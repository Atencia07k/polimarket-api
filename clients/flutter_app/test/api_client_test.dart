import 'dart:convert';
import 'dart:io';
import 'package:flutter_test/flutter_test.dart';
import 'package:polimarket_app/api_client.dart';

void main() {
  test('Rota tokens ante 401 y reintenta con el token nuevo', () async {
    final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    var refreshes = 0;
    var queries = 0;
    server.listen((request) async {
      request.response.headers.contentType = ContentType.json;
      if (request.uri.path == '/auth/refresh') {
        final body = jsonDecode(await utf8.decoder.bind(request).join());
        expect(body['refreshToken'], 'refresh-inicial');
        refreshes++;
        request.response.write(jsonEncode({'accessToken':'nuevo','refreshToken':'rotado'}));
      } else {
        queries++;
        if (request.headers.value('Authorization') == 'Bearer nuevo') {
          request.response.write('[{"id":1}]');
        } else {
          request.response.statusCode = 401;
          request.response.write('{"detail":"Expirado"}');
        }
      }
      await request.response.close();
    });
    final api = ApiClient('http://127.0.0.1:${server.port}');
    try {
      api.setTokens({'accessToken':'viejo','refreshToken':'refresh-inicial'});
      final result = await api.request('GET', '/productos');
      expect(result[0]['id'], 1);
      expect(refreshes, 1);
      expect(queries, 2);
      expect(api.refreshToken, 'rotado');
    } finally {api.close(); await server.close(force: true);}
  });
}
