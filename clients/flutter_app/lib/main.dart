import 'package:material_ui/material_ui.dart';
import 'api_client.dart';

void main() => runApp(const PoliMarketApp());

class PoliMarketApp extends StatelessWidget {
  const PoliMarketApp({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp(
    title: 'PoliMarket', debugShowCheckedModeBanner: false,
    theme: ThemeData(useMaterial3: true, colorSchemeSeed: const Color(0xff087f68),
      inputDecorationTheme: const InputDecorationTheme(border: OutlineInputBorder())),
    home: const MarketHome());
}

class Operation {
  const Operation(this.title, this.method, this.path, [this.fields = const {}]);
  final String title, method, path;
  // Un * identifica un entero positivo; $ identifica un precio no negativo.
  final Map<String, String> fields;
}

const modules = <String, List<Operation>>{
  'Ventas': [
    Operation('Clientes disponibles', 'GET', '/clientes'),
    Operation('Productos disponibles', 'GET', '/productos'),
    Operation('Registrar venta', 'POST', '/ventas', {'idVendedor':'Vendedor *','idCliente':'Cliente *','idProducto':'Producto *','cantidad':'Cantidad *'}),
    Operation('Ventas del vendedor', 'GET', '/ventas?vendedorId={idVendedor}', {'idVendedor':'Vendedor *'}),
  ],
  'Bodega': [
    Operation('Catálogo de productos', 'GET', '/productos'),
    Operation('Inventario por bodega', 'GET', '/inventarios/bodegas/{idBodega}', {'idBodega':'Bodega *'}),
    Operation('Stock de producto', 'GET', '/inventarios/stock?productoId={idProducto}&bodegaId={idBodega}', {'idProducto':'Producto *','idBodega':'Bodega *'}),
  ],
  'RRHH': [
    Operation('Vendedores', 'GET', '/vendedores'),
    Operation('Consultar autorización', 'GET', '/autorizaciones/vendedores/{idVendedor}', {'idVendedor':'Vendedor *'}),
    Operation('Autorizar vendedor', 'POST', '/autorizaciones', {'idVendedor':'Vendedor *'}),
    Operation('Revocar autorización', 'PATCH', '/autorizaciones/vendedores/{idVendedor}/revocacion', {'idVendedor':'Vendedor *'}),
  ],
  'Proveedores': [
    Operation('Proveedores y contacto', 'GET', '/proveedores'),
    Operation('Productos del proveedor', 'GET', '/productos?nitProveedor={nitProveedor}', {'nitProveedor':'NIT del proveedor'}),
    Operation('Compras pendientes', 'GET', '/compras/pendientes'),
    Operation('Registrar compra', 'POST', '/compras', {'nitProveedor':'NIT del proveedor','idProducto':'Producto *','cantidad':'Cantidad *','precio':'Costo unitario \$'}),
    Operation('Recibir compra', 'PATCH', '/compras/{idCompra}/recepcion', {'idCompra':'Compra *','idBodega':'Bodega *'}),
    Operation('Bodegas disponibles', 'GET', '/bodegas'),
  ],
  'Entregas': [
    Operation('Entregas pendientes', 'GET', '/entregas/pendientes'),
    Operation('Consultar venta y destinatario', 'GET', '/ventas/{idVenta}', {'idVenta':'Venta *'}),
    Operation('Registrar entrega', 'POST', '/entregas', {'idVenta':'Venta *','direccion':'Dirección de entrega'}),
    Operation('Confirmar entrega', 'PATCH', '/entregas/{idEntrega}/confirmacion', {'idEntrega':'Entrega *','idBodega':'Bodega *'}),
  ],
};

class MarketHome extends StatefulWidget {
  const MarketHome({super.key});
  @override
  State<MarketHome> createState() => _MarketHomeState();
}
class _MarketHomeState extends State<MarketHome> {
  final url = TextEditingController(text: const String.fromEnvironment('API_URL', defaultValue: 'http://127.0.0.1:8080/api/v1'));
  final username = TextEditingController(), password = TextEditingController();
  ApiClient? api;
  String? role;
  bool busy = false;
  String? error;
  String module = 'Ventas';
  @override
  void dispose() { url.dispose(); username.dispose(); password.dispose(); api?.close(); super.dispose(); }
  Future<void> login() async {
    final uri = Uri.tryParse(url.text.trim());
    if (uri == null || !['http','https'].contains(uri.scheme) || uri.host.isEmpty || username.text.trim().isEmpty || password.text.isEmpty) {
      setState(() => error = 'Ingresa una URL válida, usuario y contraseña.'); return;
    }
    setState(() {busy = true; error = null;});
    final client = ApiClient(url.text.trim());
    try {
      await client.login(username.text.trim(), password.text);
      final user = await client.request('GET', '/auth/me');
      if (!mounted) {client.close(); return;}
      setState(() { api = client; role = user['rol'] as String; password.clear(); });
    } catch (e) {client.close(); if (mounted) setState(() => error = '$e');}
    finally {if (mounted) setState(() => busy = false);}
  }
  Future<void> logout() async {
    setState(() => busy = true);
    try {await api!.logout();} catch (_) { /* La sesión local siempre se elimina. */ }
    api!.close();
    if (mounted) setState(() {api = null; role = null; busy = false; module = 'Ventas';});
  }
  @override
  Widget build(BuildContext context) {
    final visible = modules.keys.where((m) => role == 'ADMIN' || !['RRHH','Proveedores'].contains(m)).toList();
    return Scaffold(
      appBar: AppBar(title: const Text('PoliMarket'), actions: [if (api != null) TextButton.icon(onPressed: busy ? null : logout, icon: const Icon(Icons.logout), label: const Text('Salir'))]),
      body: api == null ? Center(child: SingleChildScrollView(padding: const EdgeInsets.all(24), child: ConstrainedBox(constraints: const BoxConstraints(maxWidth: 420), child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        const Icon(Icons.storefront, size: 64, color: Color(0xff087f68)),
        const Text('Conecta las áreas de tu negocio', textAlign: TextAlign.center, style: TextStyle(fontSize: 24)),
        const SizedBox(height: 24),
        TextField(controller: url, decoration: const InputDecoration(labelText: 'URL de la API')),
        const SizedBox(height: 16), TextField(controller: username, decoration: const InputDecoration(labelText: 'Usuario')),
        const SizedBox(height: 16), TextField(controller: password, obscureText: true, onSubmitted: (_) {if (!busy) login();}, decoration: const InputDecoration(labelText: 'Contraseña')),
        const SizedBox(height: 16), if (error != null) Text(error!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
        FilledButton(onPressed: busy ? null : login, child: Text(busy ? 'Conectando…' : 'Ingresar')),
        const Text('Usa las credenciales configuradas en la API. En el emulador Android, utiliza 10.0.2.2 en lugar de 127.0.0.1.', textAlign: TextAlign.center),
      ])))) : Column(children: [
        Padding(padding: const EdgeInsets.symmetric(horizontal: 16), child: Row(children: [Expanded(child: Text('${username.text} · $role')), const Text('Operación comercial')])),
        SingleChildScrollView(scrollDirection: Axis.horizontal, padding: const EdgeInsets.all(12), child: Row(children: visible.map((m) => Padding(padding: const EdgeInsets.only(right: 8), child: ChoiceChip(label: Text(m), selected: module == m, onSelected: (_) => setState(() => module = m)))).toList())),
        Expanded(child: OperationPanel(key: ValueKey(module), api: api!, operations: modules[module]!.where((o) => role == 'ADMIN' || o.method == 'GET' || o.path == '/ventas').toList())),
      ]),
    );
  }
}

class OperationPanel extends StatefulWidget {
  const OperationPanel({super.key, required this.api, required this.operations});
  final ApiClient api;
  final List<Operation> operations;
  @override
  State<OperationPanel> createState() => _OperationPanelState();
}
class _OperationPanelState extends State<OperationPanel> {
  int selected = 0;
  final form = GlobalKey<FormState>();
  final Map<String, TextEditingController> inputs = {};
  bool busy = false;
  dynamic result;
  String? error;
  Operation get op => widget.operations[selected];
  @override
  void dispose() {for (final c in inputs.values) {c.dispose();} super.dispose();}
  Future<void> execute() async {
    if (!form.currentState!.validate()) return;
    setState(() {busy = true; error = null; result = null;});
    try {
      var path = op.path;
      final body = <String, dynamic>{};
      for (final entry in op.fields.entries) {
        final text = inputs[entry.key]!.text.trim();
        final placeholder = '{${entry.key}}';
        if (path.contains(placeholder)) {path = path.replaceAll(placeholder, Uri.encodeComponent(text));}
        else {body[entry.key] = entry.value.endsWith('*') ? int.parse(text) : entry.value.endsWith('\$') ? double.parse(text) : text;}
      }
      if (op.path == '/ventas' && op.method == 'POST' || op.path == '/compras' && op.method == 'POST') {
        final item = <String, dynamic>{'idProducto': body.remove('idProducto'), 'cantidad': body.remove('cantidad')};
        if (body.containsKey('precio')) item['precio'] = body.remove('precio');
        body['items'] = [item];
      }
      final data = await widget.api.request(op.method, path, body.isEmpty ? null : body);
      if (mounted) setState(() => result = data ?? {'resultado':'Operación realizada'});
    } catch (e) {if (mounted) setState(() => error = '$e');}
    finally {if (mounted) setState(() => busy = false);}
  }
  Widget render(dynamic data) {
    if (data is List) {
      if (data.isEmpty) return const Text('No hay registros.');
      return Column(children: data.map<Widget>((row) => Card(child: Padding(padding: const EdgeInsets.all(16), child: render(row)))).toList());
    }
    if (data is Map) return Column(crossAxisAlignment: CrossAxisAlignment.start, children: data.entries.map<Widget>((e) => Padding(padding: const EdgeInsets.symmetric(vertical: 3), child: e.value is List || e.value is Map ? Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('${e.key}', style: const TextStyle(fontWeight: FontWeight.bold)), render(e.value)]) : SelectableText('${e.key}: ${e.value}'))).toList());
    return SelectableText('$data');
  }
  @override
  Widget build(BuildContext context) => SingleChildScrollView(padding: const EdgeInsets.all(16), child: Center(child: ConstrainedBox(constraints: const BoxConstraints(maxWidth: 760), child: Form(key: form, child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
    DropdownButtonFormField<int>(initialValue: selected, decoration: const InputDecoration(labelText: 'Acción'), items: List.generate(widget.operations.length, (i) => DropdownMenuItem(value: i, child: Text(widget.operations[i].title))), onChanged: busy ? null : (i) => setState(() {selected = i!; result = null; error = null; form.currentState?.reset();})),
    const SizedBox(height: 16),
    if (op.title == 'Registrar venta') const Text('La API valida la autorización de RRHH y el stock. El precio y total los calcula el servidor.'),
    if (op.title == 'Confirmar entrega') const Text('Confirmar descuenta el stock de la bodega indicada.'),
    if (op.title == 'Recibir compra') const Text('Recibir ingresa los productos al inventario de la bodega indicada.'),
    ...op.fields.entries.map((e) => Padding(padding: const EdgeInsets.only(bottom: 12), child: TextFormField(key: ValueKey('${op.title}${e.key}'), controller: inputs.putIfAbsent(e.key, TextEditingController.new), decoration: InputDecoration(labelText: e.value.replaceAll(RegExp(r' [*\$]$'), '')), keyboardType: e.value.endsWith('*') || e.value.endsWith('\$') ? const TextInputType.numberWithOptions(decimal: true) : TextInputType.text, validator: (v) {
      final value = v?.trim() ?? '';
      if (value.isEmpty) return 'Campo obligatorio';
      if (e.value.endsWith('*') && (int.tryParse(value) == null || int.parse(value) <= 0)) return 'Ingresa un entero positivo';
      if (e.value.endsWith('\$')) {final n = double.tryParse(value); if (n == null || !n.isFinite || n < 0) return 'Ingresa un precio válido, con punto decimal';}
      if (e.key == 'direccion' && value.length > 255) return 'Máximo 255 caracteres';
      return null;
    }))),
    FilledButton.icon(onPressed: busy ? null : execute, icon: const Icon(Icons.play_arrow), label: Text(busy ? 'Procesando…' : op.title)),
    if (busy) const LinearProgressIndicator(),
    const SizedBox(height: 16),
    if (error != null) SelectableText(error!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
    if (result != null) render(result),
  ])))));
}

