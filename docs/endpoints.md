# PoliMarket API – Endpoints REST (v1)

> **Aviso de seguridad temporal.** Con el perfil **`dev`** (y en las pruebas automatizadas con el perfil `test`) todos los endpoints son **públicos, sin autenticación**, y la aplicación escucha solo en `127.0.0.1`. Esta configuración (`DevSecurityConfig`) es exclusiva para desarrollo local y pruebas funcionales.
> Con el perfil **`prod`** o sin perfil, `SecurityConfig` **deniega por defecto** todo lo que no sea `GET /api/v1/hola`, `GET /actuator/health` y `/error` (respuesta `403`).
> La autenticación con **JWT** (login, emisión y validación de tokens, roles) se implementará en una etapa posterior en `controller/autenticacion` y `dto/autenticacion`; reemplazará la configuración temporal sin cambiar las rutas.
> La **autorización comercial de Recursos Humanos** (un vendedor solo puede vender si su último registro es `AUTORIZADA`) es una regla de negocio y **sigue activa** aunque los endpoints sean públicos.

## Convenciones

- URL base: `http://127.0.0.1:8080/api/v1`. JSON en UTF-8; fechas ISO-8601 (`2026-11-14`).
- Las respuestas siempre son **DTO** (nunca entidades). Los campos nulos se omiten.
- Los controladores solo dependen de interfaces de servicio; toda regla de negocio vive en la capa service.
- El cliente HTTP **nunca envía precios ni totales de venta**: el precio de cada línea es el precio vigente del producto y el total lo calcula `VentaService`.
- `POST` que crean un recurso responden `201 Created` con encabezado `Location` (excepto `POST /autorizaciones`, que agrega un registro al historial).
- Cambios de estado (recepción, cancelación, confirmación, revocación) usan `PATCH` sobre un sub-recurso con nombre de acción.

### Códigos HTTP y formato de error

Los errores usan **ProblemDetail (RFC 9457)**, `Content-Type: application/problem+json`. Nunca se exponen trazas, SQL ni detalles de Hibernate.

| Código | Cuándo | `type` |
|---|---|---|
| 200 | Consulta o actualización correcta | – |
| 201 | Recurso creado | – |
| 400 | Validación de campos (incluye `errores` campo → mensaje), JSON mal formado, parámetro faltante o de tipo inválido, filtros incompatibles, `DatosInvalidosException` | `urn:polimarket:error:validacion` / `datos-invalidos` |
| 401 / 403 | Reservados para la etapa JWT (`AuthenticationException` / `AccessDeniedException`); en `prod` las rutas no públicas responden 403 | `no-autenticado` / `acceso-denegado` |
| 404 | `RecursoNoEncontradoException` o ruta inexistente | `urn:polimarket:error:recurso-no-encontrado` |
| 405 | Método HTTP no soportado en la ruta | – |
| 409 | Conflicto con el estado del negocio: `VendedorNoAutorizadoException`, `InventarioInsuficienteException`, `OperacionNoPermitidaException`, violación de integridad o bloqueo concurrente | `vendedor-no-autorizado`, `inventario-insuficiente`, `operacion-no-permitida`, `conflicto-datos`, `conflicto-concurrencia` |
| 500 | Error inesperado (mensaje genérico; el detalle solo queda en el log) | `urn:polimarket:error:error-interno` |

> El proyecto no usa `422`: los conflictos de reglas de negocio se reportan con `409` y los datos inválidos con `400`.

Ejemplo de error de validación:

```json
{
  "type": "urn:polimarket:error:validacion",
  "title": "Solicitud invalida",
  "status": 400,
  "detail": "Uno o mas campos no son validos",
  "instance": "/api/v1/ventas",
  "errores": { "idVendedor": "must not be null", "items": "must not be empty" }
}
```

Ejemplo de regla de negocio:

```json
{
  "type": "urn:polimarket:error:vendedor-no-autorizado",
  "title": "Vendedor no autorizado por Recursos Humanos",
  "status": 409,
  "detail": "El vendedor 2 no tiene una autorizacion vigente de Recursos Humanos",
  "instance": "/api/v1/ventas"
}
```

## Resumen de endpoints

| Componente | Método | Ruta | Función | Servicio | Éxito | Errores |
|---|---|---|---|---|---|---|
| Hola mundo | GET | `/hola` | Saludo de prueba (público en todos los perfiles) | `SaludoService.obtenerSaludo` | 200 | – |
| Clientes | GET | `/clientes[?nombre=]` | Listar o buscar clientes | `ClienteService.listarClientes` / `buscarClientesPorNombre` | 200 | 400 |
| Clientes | GET | `/clientes/{id}` | Consultar cliente | `ClienteService.obtenerCliente` | 200 | 400, 404 |
| Clientes | PUT | `/clientes/{id}` | Actualizar nombre y dirección | `ClienteService.actualizarDatos` | 200 | 400, 404 |
| RRHH | GET | `/vendedores[?nombre=]` | Listar o buscar vendedores | `VendedorService.listarVendedores` / `buscarVendedoresPorNombre` | 200 | 400 |
| RRHH | GET | `/vendedores/{id}` | Consultar vendedor | `VendedorService.obtenerVendedor` | 200 | 400, 404 |
| RRHH | GET | `/autorizaciones/vendedores/{idVendedor}` | ¿Vendedor autorizado? | `AutorizacionRRHHService.validarVendedor` | 200 | 400, 404 |
| RRHH | GET | `/autorizaciones/vendedores/{idVendedor}/historial` | Historial de autorizaciones | `AutorizacionRRHHService.consultarHistorial` | 200 | 400, 404 |
| RRHH | POST | `/autorizaciones` | Autorizar vendedor | `AutorizacionRRHHService.autorizarVendedor` | 201 | 400, 404, 409 |
| RRHH | PATCH | `/autorizaciones/vendedores/{idVendedor}/revocacion` | Revocar autorización | `AutorizacionRRHHService.revocarAutorizacion` | 200 | 400, 404, 409 |
| Bodega | GET | `/productos[?nombre=\|?nitProveedor=]` | Catálogo (un solo filtro opcional) | `ProductoService.listarProductos` / `buscarProductosPorNombre` / `consultarProductosPorProveedor` | 200 | 400, 404 |
| Bodega | GET | `/productos/{id}` | Consultar producto | `ProductoService.obtenerProducto` | 200 | 400, 404 |
| Bodega | PUT | `/productos/{id}/precio` | Actualizar precio | `ProductoService.actualizarPrecio` | 200 | 400, 404 |
| Bodega | GET | `/bodegas[?ubicacion=]` | Listar o buscar bodegas | `BodegaService.listarBodegas` / `buscarBodegasPorUbicacion` | 200 | 400 |
| Bodega | GET | `/bodegas/{id}` | Consultar bodega | `BodegaService.obtenerBodega` | 200 | 400, 404 |
| Bodega | GET | `/bodegas/{id}/disponibilidad` | Unidades disponibles en la bodega | `BodegaService.consultarDisponibilidad` | 200 | 400, 404 |
| Bodega | GET | `/bodegas/{id}/inventarios` | Productos almacenados | `BodegaService.consultarProductosAlmacenados` | 200 | 400, 404 |
| Bodega | GET | `/inventarios/stock?productoId=&bodegaId=` | Stock de un producto en una bodega | `InventarioService.consultarStock` + `requiereReposicion` | 200 | 400, 404 |
| Bodega | GET | `/inventarios/productos/{productoId}` | Inventario del producto por bodega | `InventarioService.consultarInventarioPorProducto` | 200 | 400, 404 |
| Bodega | GET | `/inventarios/productos/{productoId}/stock-total` | Stock total del producto | `InventarioService.consultarStockTotal` | 200 | 400, 404 |
| Bodega | GET | `/inventarios/bodegas/{bodegaId}` | Inventario de una bodega | `InventarioService.consultarInventarioPorBodega` | 200 | 400, 404 |
| Bodega | GET | `/inventarios/reposicion` | Registros con stock ≤ mínimo | `InventarioService.consultarInventariosQueRequierenReposicion` | 200 | – |
| Bodega | GET | `/movimientos-stock[?tipo=\|?desde=&hasta=]` | Listar movimientos (un solo filtro opcional) | `MovimientoStockService.listarMovimientos` / `consultarPorTipo` / `consultarPorFechas` | 200 | 400 |
| Bodega | GET | `/movimientos-stock/{id}` | Consultar movimiento | `MovimientoStockService.obtenerMovimiento` | 200 | 400, 404 |
| Bodega | GET | `/movimientos-stock/{inventarios\|productos\|compras\|entregas}/{id}` | Movimientos por relación | `MovimientoStockService.consultarPor…` | 200 | 400, 404 |
| Proveedores | GET | `/proveedores[?nombre=\|?productoId=]` | Listar proveedores (un solo filtro opcional) | `ProveedorService.listarProveedores` / `buscarProveedoresPorNombre` / `consultarProveedoresPorProducto` | 200 | 400, 404 |
| Proveedores | GET | `/proveedores/{nit}` | Consultar proveedor por NIT | `ProveedorService.obtenerProveedor` | 200 | 404 |
| Proveedores | PUT | `/proveedores/{nit}/contacto` | Actualizar contacto | `ProveedorService.actualizarContacto` | 200 | 400, 404 |
| Proveedores | GET | `/compras?nitProveedor=` | Compras de un proveedor (filtro obligatorio) | `CompraProveedorService.consultarComprasPorProveedor` | 200 | 400, 404 |
| Proveedores | GET | `/compras/pendientes` | Compras `PENDIENTE` | `CompraProveedorService.consultarComprasPendientes` | 200 | – |
| Proveedores | GET | `/compras/{id}` | Consultar compra | `CompraProveedorService.consultarCompra` | 200 | 400, 404 |
| Proveedores | POST | `/compras` | Registrar compra (`PENDIENTE`, no mueve stock) | `CompraProveedorService.registrarCompra` | 201 | 400, 404 |
| Proveedores | PATCH | `/compras/{id}/recepcion` | Recibir compra en una bodega (ingresa stock, `RECIBIDA`) | `CompraProveedorService.recibirCompra` | 200 | 400, 404, 409 |
| Proveedores | PATCH | `/compras/{id}/cancelacion` | Cancelar compra `PENDIENTE` | `CompraProveedorService.cancelarCompra` | 200 | 400, 404, 409 |
| Ventas | GET | `/ventas?vendedorId=` o `?clienteId=` | Ventas por vendedor o por cliente (exactamente un filtro) | `VentaService.consultarVentasPorVendedor` / `consultarVentasPorCliente` | 200 | 400, 404 |
| Ventas | GET | `/ventas/{id}` | Consultar venta | `VentaService.consultarVenta` | 200 | 400, 404 |
| Ventas | POST | `/ventas` | Registrar venta (`REGISTRADA`, no descuenta stock) | `VentaService.registrarVenta` | 201 | 400, 404, 409 |
| Ventas | PATCH | `/ventas/{id}/cancelacion` | Cancelar venta | `VentaService.cancelarVenta` | 200 | 400, 404, 409 |
| Entregas | GET | `/entregas?estado=` o `?desde=&hasta=` | Entregas por estado o rango de fechas (exactamente un filtro) | `EntregaService.consultarPorEstado` / `consultarPorRangoFechas` | 200 | 400 |
| Entregas | GET | `/entregas/pendientes` | Entregas `PENDIENTE` | `EntregaService.consultarPendientes` | 200 | – |
| Entregas | GET | `/entregas/{id}` | Consultar entrega | `EntregaService.consultarEntrega` | 200 | 400, 404 |
| Entregas | GET | `/entregas/ventas/{idVenta}` | Entrega de una venta | `EntregaService.consultarEntregaPorVenta` | 200 | 400, 404 |
| Entregas | POST | `/entregas` | Registrar entrega (`PENDIENTE`) | `EntregaService.registrarEntrega` | 201 | 400, 404, 409 |
| Entregas | PATCH | `/entregas/{id}/confirmacion` | Confirmar entrega desde una bodega (descuenta stock una sola vez, `ENTREGADA`) | `EntregaService.confirmarEntrega` | 200 | 400, 404, 409 |
| Actuator | GET | `/actuator/health` | Estado de la aplicación y de PostgreSQL | Spring Boot Actuator | 200 | – |

No existen endpoints de alta/eliminación de clientes, vendedores, productos, bodegas o proveedores, ni de ajuste manual de inventario: los servicios aprobados no los definen. Las entradas de stock ocurren solo al **recibir una compra** y las salidas solo al **confirmar una entrega**.

## DTO por componente

| Componente | Request (validación) | Response |
|---|---|---|
| Clientes | `ActualizarClienteRequest(nombre @NotBlank ≤255, direccion ≤255)` | `ClienteResponse(id, nombre, direccion)` |
| RRHH | `AutorizarVendedorRequest(idVendedor @NotNull @Positive)` | `VendedorResponse(id, nombre)`, `AutorizacionResponse(id, idVendedor, estado, fecha)`, `EstadoAutorizacionResponse(idVendedor, autorizado)` |
| Bodega | `ActualizarPrecioRequest(precio @NotNull ≥0)` | `ProductoResponse(id, nombre, precio, nitsProveedores)`, `BodegaResponse(id, ubicacion)`, `DisponibilidadBodegaResponse(idBodega, unidadesDisponibles)`, `InventarioResponse(id, idProducto, nombreProducto, idBodega, ubicacionBodega, stock, stockMinimo, requiereReposicion)`, `StockResponse(idProducto, idBodega, stock, requiereReposicion)`, `StockTotalResponse(idProducto, stockTotal)`, `MovimientoStockResponse(id, tipo, cantidad, fecha, idInventario, idProducto, idBodega, idCompra?, idEntrega?)` |
| Proveedores | `ActualizarContactoRequest(contacto @NotBlank ≤255)`, `RegistrarCompraRequest(nitProveedor @NotBlank, items @NotEmpty[ItemCompraRequest(idProducto @Positive, cantidad @Positive, precio ≥0)])`, `RecibirCompraRequest(idBodega @NotNull @Positive)` | `ProveedorResponse(nit, nombre, contacto)`, `CompraResponse(id, fecha, estado, nitProveedor, nombreProveedor, total, detalles[DetalleCompraResponse(idProducto, nombreProducto, cantidad, precio, subtotal)])` |
| Ventas | `RegistrarVentaRequest(idVendedor @NotNull @Positive, idCliente @NotNull @Positive, items @NotEmpty[ItemVentaRequest(idProducto @Positive, cantidad @Positive)])` | `VentaResponse(id, fecha, estado, total, idVendedor, nombreVendedor, idCliente, nombreCliente, detalles[DetalleVentaResponse(idProducto, nombreProducto, cantidad, precio, subtotal)], idEntrega?)` |
| Entregas | `RegistrarEntregaRequest(idVenta @NotNull @Positive, direccion @NotBlank ≤255)`, `ConfirmarEntregaRequest(idBodega @NotNull @Positive)` | `EntregaResponse(id, fecha, estado, direccion, idVenta)` |

Los campos desconocidos del JSON (por ejemplo un `precio` o `total` enviado en una venta) se ignoran.

## Ejemplos de flujo (PowerShell)

```powershell
$api = "http://127.0.0.1:8080/api/v1"

# 1. RRHH autoriza al vendedor 1  -> 201
Invoke-RestMethod -Method Post "$api/autorizaciones" -ContentType "application/json" -Body '{"idVendedor":1}'
```

```json
{ "id": 1, "idVendedor": 1, "estado": "AUTORIZADA", "fecha": "2026-11-14" }
```

```powershell
# 2. Registrar venta -> 201, Location: /api/v1/ventas/1 (no descuenta stock)
Invoke-RestMethod -Method Post "$api/ventas" -ContentType "application/json" `
  -Body '{"idVendedor":1,"idCliente":1,"items":[{"idProducto":1,"cantidad":3}]}'
```

```json
{
  "id": 1, "fecha": "2026-11-14", "estado": "REGISTRADA", "total": 9000.0,
  "idVendedor": 1, "nombreVendedor": "Luis", "idCliente": 1, "nombreCliente": "Ana",
  "detalles": [ { "idProducto": 1, "nombreProducto": "Arroz", "cantidad": 3, "precio": 3000.0, "subtotal": 9000.0 } ]
}
```

```powershell
# 3. Registrar entrega -> 201 (PENDIENTE)
Invoke-RestMethod -Method Post "$api/entregas" -ContentType "application/json" -Body '{"idVenta":1,"direccion":"Carrera 7"}'

# 4. Confirmar entrega desde la bodega 1 -> 200 (ENTREGADA, descuenta 3 unidades y crea un movimiento SALIDA)
Invoke-RestMethod -Method Patch "$api/entregas/1/confirmacion" -ContentType "application/json" -Body '{"idBodega":1}'
# Repetir la confirmacion -> 409 operacion-no-permitida (el stock no se descuenta dos veces)

# 5. Compra a proveedor y recepcion -> 201 PENDIENTE, luego 200 RECIBIDA (+5 unidades, movimiento ENTRADA)
Invoke-RestMethod -Method Post "$api/compras" -ContentType "application/json" `
  -Body '{"nitProveedor":"900","items":[{"idProducto":1,"cantidad":5,"precio":2500.0}]}'
Invoke-RestMethod -Method Patch "$api/compras/1/recepcion" -ContentType "application/json" -Body '{"idBodega":1}'

# 6. Consultas
Invoke-RestMethod "$api/inventarios/stock?productoId=1&bodegaId=1"
Invoke-RestMethod "$api/movimientos-stock?tipo=SALIDA"
Invoke-RestMethod "$api/entregas?estado=ENTREGADA"
```

## Datos de ejemplo para pruebas manuales (pendiente de aprobación)

La base de desarrollo `polimarket_db` está **vacía** y la API no tiene endpoints para crear vendedores, clientes, productos, bodegas, proveedores ni registros de inventario. Para probar los flujos de escritura manualmente se propone el siguiente script; **no se ha ejecutado** y solo debe aplicarse con aprobación explícita:

```sql
BEGIN;
INSERT INTO vendedor (nombre) VALUES ('Luis Vendedor');
INSERT INTO cliente (nombre, direccion) VALUES ('Ana Cliente', 'Calle 1 # 2-3');
INSERT INTO proveedor (nit, nombre, contacto) VALUES ('900123456-7', 'Distribuidora Andina', 'ventas@andina.co');
INSERT INTO producto (nombre, precio) VALUES ('Arroz 1 kg', 3000);
INSERT INTO producto_proveedor (producto_id, proveedor_nit)
  SELECT id, '900123456-7' FROM producto WHERE nombre = 'Arroz 1 kg';
INSERT INTO bodega (ubicacion) VALUES ('Bogota Centro');
INSERT INTO inventario (producto_id, bodega_id, stock, stock_minimo)
  SELECT p.id, b.id, 10, 2 FROM producto p, bodega b WHERE p.nombre = 'Arroz 1 kg' AND b.ubicacion = 'Bogota Centro';
COMMIT;
```

## Pendientes

- **Listados sin filtro** de ventas, entregas y compras: los servicios aprobados solo ofrecen consultas filtradas, por lo que `GET /ventas`, `GET /entregas` y `GET /compras` exigen un filtro. Agregar `listarVentas()`, `listarEntregas()` y `listarCompras()` requiere autorizar cambios en la capa service.
- **Seguridad definitiva** con JWT y roles (reemplaza `DevSecurityConfig`).
- **Ajustes manuales de inventario** y altas de catálogo: no definidos en el UML ni en los servicios.
- **Paginación** de listados: no requerida en esta etapa.
