# PoliMarket API – Endpoints REST (v1)

> **Seguridad.** Todos los endpoints de negocio requieren un **access token JWT** en la cabecera `Authorization: Bearer <token>` (en todos los perfiles). Solo son públicos `GET /hola`, `GET /actuator/health`, `/error` y `POST /auth/{login,refresh,logout}`. Los roles son `ADMIN` (todo) y `VENDEDOR` (consultas de clientes, productos, inventarios, ventas y entregas; registrar y cancelar ventas). La matriz completa y el ciclo de vida de los tokens están en el [README](../README.md#autenticación-y-seguridad-jwt).
> La **autorización comercial de Recursos Humanos** (un vendedor solo puede vender si su último registro es `AUTORIZADA`) es una regla de negocio independiente de la autenticación y **sigue activa** aunque el usuario tenga un token válido.

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
| 401 | Sin token, token inválido, alterado o expirado (`WWW-Authenticate: Bearer`) | `urn:polimarket:error:no-autenticado` |
| 401 | Login con credenciales incorrectas o usuario inactivo (mensaje genérico); refresh token inválido, expirado, revocado o reutilizado | `urn:polimarket:error:autenticacion-fallida` |
| 403 | Token válido, pero el rol no permite la operación | `urn:polimarket:error:acceso-denegado` |
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
| Hola mundo | GET | `/hola` | Saludo de prueba (público) | `SaludoService.obtenerSaludo` | 200 | – |
| Autenticación | POST | `/auth/login` | Iniciar sesión: access token (15 min) + refresh token (7 días) (público) | `AuthService.iniciarSesion` | 200 | 400, 401 |
| Autenticación | POST | `/auth/refresh` | Rotar el refresh token y obtener un par nuevo (público) | `AuthService.refrescar` | 200 | 400, 401 |
| Autenticación | POST | `/auth/logout` | Revocar la sesión del refresh token; idempotente (público) | `AuthService.cerrarSesion` | 204 | 400 |
| Autenticación | GET | `/auth/me` | Usuario autenticado (`ADMIN`/`VENDEDOR`) | `UsuarioService.obtenerPorUsername` | 200 | 401 |
| Usuarios | POST | `/usuarios` | Crear usuario `ADMIN` o `VENDEDOR` (solo `ADMIN`) | `UsuarioService.crearUsuario` | 201 | 400, 401, 403, 409 |
| Usuarios | GET | `/usuarios` | Listar usuarios (solo `ADMIN`) | `UsuarioService.listarUsuarios` | 200 | 401, 403 |
| Usuarios | GET | `/usuarios/{id}` | Consultar usuario (solo `ADMIN`) | `UsuarioService.obtenerUsuario` | 200 | 400, 401, 403, 404 |
| Usuarios | PATCH | `/usuarios/{id}/desactivacion` | Desactivar usuario y revocar sus refresh tokens; no aplica a uno mismo (solo `ADMIN`) | `UsuarioService.desactivarUsuario` | 200 | 400, 401, 403, 404, 409 |
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
| Actuator | GET | `/actuator/health` | Estado de la aplicación y de PostgreSQL (público) | Spring Boot Actuator | 200 | – |

Todos los endpoints de negocio de la tabla pueden responder además `401` (sin token válido) y `403` (rol insuficiente). Acceso por rol: los `GET` de `/clientes`, `/productos`, `/inventarios`, `/ventas` y `/entregas`, más `POST /ventas` y `PATCH /ventas/{id}/cancelacion`, admiten `ADMIN` y `VENDEDOR`; el resto exige `ADMIN`.

No existen endpoints de alta/eliminación de clientes, vendedores, productos, bodegas o proveedores, ni de ajuste manual de inventario: los servicios aprobados no los definen. Las entradas de stock ocurren solo al **recibir una compra** y las salidas solo al **confirmar una entrega**.

## DTO por componente

| Componente | Request (validación) | Response |
|---|---|---|
| Autenticación | `LoginRequest(username @NotBlank ≤50, password @NotBlank ≤72)`, `RefreshTokenRequest(refreshToken @NotBlank ≤200)`, `CrearUsuarioRequest(username @NotBlank 3–50, password @NotBlank 8–72, rol @NotNull ADMIN\|VENDEDOR)` | `TokenResponse(accessToken, tokenType="Bearer", expiresIn, refreshToken, refreshExpiresIn)`, `UsuarioResponse(id, username, rol, activo, fechaCreacion)` (nunca incluye la contraseña ni su hash) |
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

# 0. Iniciar sesion (usuario ADMIN definido en .env) y preparar la cabecera Bearer
$login = Invoke-RestMethod -Method Post "$api/auth/login" -ContentType "application/json" `
  -Body (@{ username = $env:ADMIN_USERNAME; password = $env:ADMIN_PASSWORD } | ConvertTo-Json)
$PSDefaultParameterValues["Invoke-RestMethod:Headers"] = @{ Authorization = "Bearer $($login.accessToken)" }
# El access token expira en 15 min: renovar con POST $api/auth/refresh y {"refreshToken":"..."}

# 1. RRHH autoriza a la vendedora 4 (sin autorizacion en las semillas) -> 201
Invoke-RestMethod -Method Post "$api/autorizaciones" -ContentType "application/json" -Body '{"idVendedor":4}'
```

```json
{ "id": 5, "idVendedor": 4, "estado": "AUTORIZADA", "fecha": "2026-11-14" }
```

```powershell
# 2. Registrar venta -> 201, Location: /api/v1/ventas/6 (no descuenta stock)
Invoke-RestMethod -Method Post "$api/ventas" -ContentType "application/json" `
  -Body '{"idVendedor":4,"idCliente":1,"items":[{"idProducto":1,"cantidad":3}]}'
```

```json
{
  "id": 6, "fecha": "2026-11-14", "estado": "REGISTRADA", "total": 10500.0,
  "idVendedor": 4, "nombreVendedor": "Laura Martinez", "idCliente": 1, "nombreCliente": "Ana Torres",
  "detalles": [ { "idProducto": 1, "nombreProducto": "Arroz 1 kg", "cantidad": 3, "precio": 3500.0, "subtotal": 10500.0 } ]
}
```

```powershell
# 3. Registrar entrega -> 201 (PENDIENTE, id 3)
Invoke-RestMethod -Method Post "$api/entregas" -ContentType "application/json" -Body '{"idVenta":6,"direccion":"Calle 10 # 5-20, Bogota"}'

# 4. Confirmar entrega desde la bodega 1 -> 200 (ENTREGADA, descuenta 3 unidades y crea un movimiento SALIDA)
Invoke-RestMethod -Method Patch "$api/entregas/3/confirmacion" -ContentType "application/json" -Body '{"idBodega":1}'
# Repetir la confirmacion -> 409 operacion-no-permitida (el stock no se descuenta dos veces)

# 5. Compra a proveedor y recepcion -> 201 PENDIENTE (id 5), luego 200 RECIBIDA (+5 unidades, movimiento ENTRADA)
Invoke-RestMethod -Method Post "$api/compras" -ContentType "application/json" `
  -Body '{"nitProveedor":"900123456-7","items":[{"idProducto":1,"cantidad":5,"precio":2500.0}]}'
Invoke-RestMethod -Method Patch "$api/compras/5/recepcion" -ContentType "application/json" -Body '{"idBodega":1}'

# 6. Consultas
Invoke-RestMethod "$api/inventarios/stock?productoId=1&bodegaId=1"
Invoke-RestMethod "$api/movimientos-stock?tipo=SALIDA"
Invoke-RestMethod "$api/entregas?estado=ENTREGADA"
```

## Datos semilla (perfil `dev`)

Con el perfil `dev`, Flyway carga además `src/main/resources/db/seed/R__datos_semilla.sql` (migración *repetible*), así que cualquiera que clone el proyecto y cree la base tiene datos mínimos para probar todos los endpoints. Es idempotente (IDs explícitos con `ON CONFLICT DO NOTHING`), no modifica datos existentes, **no crea usuarios** (el administrador sale de `.env`) y no se carga en las pruebas ni fuera de `dev`.

| Datos | Contenido y escenarios cubiertos |
|---|---|
| Vendedores | 1 Luis Gomez y 2 Maria Rodriguez (**autorizados**), 3 Carlos Perez (autorizado y luego **revocado** → venta 409), 4 Laura Martinez (**sin autorización** → para probar `POST /autorizaciones`) |
| Clientes | 1 a 5 (Bogotá, Medellín, Cali) |
| Proveedores | `900123456-7` Distribuidora Andina, `800987654-3` Lacteos del Valle, `901555222-1` Aseo Total |
| Productos | 1 Arroz, 2 Aceite, 3 Azucar, 4 Cafe (Andina); 5 Leche, 6 Queso (Valle); 7 Detergente (Aseo); 8 Jabon (Aseo y Andina) |
| Bodegas | 1 Bogota - Centro, 2 Medellin - Belen, 3 Cali - Norte |
| Inventario | 10 registros; los inventarios **3, 6 y 8 están bajo el mínimo** (`GET /inventarios/reposicion`) |
| Compras | 1 RECIBIDA (con movimientos ENTRADA), 2 y 4 **PENDIENTE** (recibir / cancelar), 3 CANCELADA |
| Ventas | 1 a 3 y 5 REGISTRADA, 4 CANCELADA; la venta 5 no tiene entrega (se puede cancelar) y la 3 permite registrar entrega |
| Entregas | 1 ENTREGADA (venta 1, con movimientos SALIDA), 2 **PENDIENTE** (venta 2, confirmar desde la bodega 1) |

Para **restaurar las semillas** tras pruebas manuales (borra los datos de negocio, conserva usuarios):

```powershell
"TRUNCATE vendedor, autorizacion_rrhh, cliente, proveedor, producto, producto_proveedor, bodega, inventario, compra_proveedor, detalle_compra, venta, detalle_venta, entrega, movimiento_stock RESTART IDENTITY CASCADE; DELETE FROM flyway_schema_history WHERE version IS NULL;" | docker exec -i polimarket-postgres sh -c 'psql -U $POSTGRES_USER -d $POSTGRES_DB'
```

y reiniciar la API con el perfil `dev`. Si modifica el script de semillas, Flyway lo vuelve a ejecutar en el siguiente arranque (solo inserta lo que falte).
## Pendientes

- **Listados sin filtro** de ventas, entregas y compras: los servicios aprobados solo ofrecen consultas filtradas, por lo que `GET /ventas`, `GET /entregas` y `GET /compras` exigen un filtro. Agregar `listarVentas()`, `listarEntregas()` y `listarCompras()` requiere autorizar cambios en la capa service.
- **Revocación inmediata de access tokens** (lista de `jti` revocados): hoy un access token sigue siendo válido hasta su expiración (máximo 15 min) tras un logout o una desactivación.
- **Límite de intentos de login** (rate limiting / bloqueo temporal) para mitigar fuerza bruta.
- **Ajustes manuales de inventario** y altas de catálogo: no definidos en el UML ni en los servicios.
- **Paginación** de listados: no requerida en esta etapa.
