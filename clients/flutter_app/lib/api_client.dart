import 'dart:async';
import 'dart:convert';
import 'dart:io';

/// Cliente nativo: los tokens se conservan solamente durante la sesión.
class ApiClient {
  ApiClient(this.baseUrl);
  final String baseUrl;
  final HttpClient _http = HttpClient();
  String? accessToken;
  String? refreshToken;
  Future<dynamic> request(String method, String path,
      [Map<String, dynamic>? body, bool retry = true]) async {
    final uri = Uri.parse('${baseUrl.replaceFirst(RegExp(r"/+$"), "")}$path');
    try {
      final req = await _http.openUrl(method, uri).timeout(const Duration(seconds: 15));
      req.headers.contentType = ContentType.json;
      if (accessToken != null) req.headers.set('Authorization', 'Bearer $accessToken');
      if (body != null) req.write(jsonEncode(body));
      final response = await req.close().timeout(const Duration(seconds: 20));
      final raw = await response.transform(utf8.decoder).join().timeout(const Duration(seconds: 20));
      dynamic data;
      try { data = raw.isEmpty ? null : jsonDecode(raw); }
      on FormatException { throw Exception('Respuesta inesperada del servidor (${response.statusCode}).'); }
      if (response.statusCode == 401 && retry && refreshToken != null && !path.startsWith('/auth/')) {
        try {
          final tokens = await request('POST', '/auth/refresh', {'refreshToken': refreshToken}, false);
          setTokens(tokens);
        } catch (_) {
          accessToken = null;
          refreshToken = null;
          throw Exception('La sesión expiró. Cierra sesión e ingresa nuevamente.');
        }
        return await request(method, path, body, false);
      }
      if (response.statusCode >= 400) {
        final detail = data is Map ? data['detail'] ?? data['title'] : 'Error del servidor';
        final fields = data is Map && data['errores'] is Map ? '\n${(data['errores'] as Map).values.join('\n')}' : '';
        throw Exception('${response.statusCode}: $detail$fields');
      }
      return data;
    } on SocketException {
      throw Exception('No se pudo conectar. Revisa la URL, la API y la conexión de red.');
    } on TimeoutException {
      throw Exception('La API no respondió a tiempo. Consulta el estado antes de repetir una operación.');
    }
  }
  void setTokens(dynamic tokens) {
    accessToken = tokens['accessToken'] as String;
    refreshToken = tokens['refreshToken'] as String;
  }
  Future<void> login(String username, String password) async {
    setTokens(await request('POST', '/auth/login', {'username': username, 'password': password}));
  }
  Future<void> logout() async {
    try {
      if (refreshToken != null) await request('POST', '/auth/logout', {'refreshToken': refreshToken});
    } finally { accessToken = null; refreshToken = null; }
  }
  void close() => _http.close(force: true);
}

