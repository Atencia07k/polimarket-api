# PoliMarket API – Guía de peticiones para pruebas

Listado de las 68 peticiones incluidas en la colección [`postman/PoliMarket-API.postman_collection.json`](postman/PoliMarket-API.postman_collection.json), con el resultado esperado sobre los [datos semilla](endpoints.md#datos-semilla-perfil-dev) del perfil `dev`. La referencia técnica (DTO, códigos de error, reglas) está en [`endpoints.md`](endpoints.md).

## Preparación

1. Levantar PostgreSQL (`docker compose up -d`) y la API con el perfil `dev` (IntelliJ: **PolimarketApi (dev)**). URL base: `http://127.0.0.1:8080`.
2. En Postman: **Import** → `docs/postman/PoliMarket-API.postman_collection.json`.
3. En la pestaña **Variables** de la colección, escribir la contraseña del administrador (`ADMIN_PASSWORD` del `.env`) en `password` y guardar.
4. Ejecutar **1. Autenticación › Login**: guarda `accessToken` y `refreshToken`; las demás peticiones envían `Authorization: Bearer {{accessToken}}` automáticamente.
5. El access token dura **15 minutos**: al recibir `401`, ejecutar **Refresh**.

## Roles

| Rol | Permisos |
|---|---|
| `ADMIN` | Todas las peticiones. |
| `VENDEDOR` | `GET` de clientes, productos, inventarios, ventas y entregas; registrar y cancelar ventas; `auth/me`. El resto responde `403`. |
| Sin token | Solo login, refresh, logout, `GET /api/v1/hola` y `GET /actuator/health`. El resto responde `401`. |

## 1. Autenticación

| # | Acción | Método y ruta | Body | Resultado esperado |
|---|---|---|---|---|
| 1 | Login | `POST /api/v1/auth/login` | `{"username":"admin","password":"..."}` | `200` con `accessToken`, `refreshToken`, `expiresIn` |
| 2 | Renovar tokens | `POST /api/v1/auth/refresh` | `{"refreshToken":"{{refreshToken}}"}` | `200` con un par nuevo; reutilizar el refresh anterior → `401` y se revoca la sesión |
| 3 | Usuario actual | `GET /api/v1/auth/me` | — | `200` con usuario y rol |
| 4 | Cerrar sesión | `POST /api/v1/auth/logout` | `{"refreshToken":"{{refreshToken}}"}` | `204` |

## 2. Usuarios (solo ADMIN)

Los usuarios se guardan en la tabla `usuario` de PostgreSQL (contraseña con BCrypt).

| # | Acción | Método y ruta | Body | Resultado esperado |
|---|---|---|---|---|
| 5 | Crear vendedor | `POST /api/v1/usuarios` | `{"username":"vendedor1","password":"Vendedor-2026!","rol":"VENDEDOR"}` | `201`; repetir → `409` |
| 6 | Crear administrador | `POST /api/v1/usuarios` | `{"username":"admin2","password":"Admin2-2026!","rol":"ADMIN"}` | `201` |
| 7 | Listar usuarios | `GET /api/v1/usuarios` | — | `200` |
| 8 | Consultar usuario | `GET /api/v1/usuarios/{id}` | — | `200`; id inexistente → `404` |
| 9 | Desactivar usuario | `PATCH /api/v1/usuarios/{id}/desactivacion` | — | `200`; revoca sus refresh tokens; desactivarse a sí mismo → `409` |

## 3. Clientes

| # | Acción | Método y ruta | Body | Resultado esperado |
|---|---|---|---|---|
| 10 | Listar clientes | `GET /api/v1/clientes` | — | `200`, 5 clientes |
| 11 | Buscar por nombre | `GET /api/v1/clientes?nombre=Ana` | — | `200`, Ana Torres |
| 12 | Consultar cliente | `GET /api/v1/clientes/1` | — | `200` |
| 13 | Actualizar cliente | `PUT /api/v1/clientes/1` | `{"nombre":"Ana Cliente","direccion":"Calle 1 # 2-3"}` | `200` |

## 4. Recursos Humanos (vendedores y autorizaciones)

| # | Acción | Método y ruta | Body | Resultado esperado |
|---|---|---|---|---|
| 14 | Listar vendedores | `GET /api/v1/vendedores` | — | `200`, 4 vendedores |
| 15 | Buscar por nombre | `GET /api/v1/vendedores?nombre=Luis` | — | `200` |
| 16 | Consultar vendedor | `GET /api/v1/vendedores/1` | — | `200` |
| 17 | Autorizar vendedor | `POST /api/v1/autorizaciones` | `{"idVendedor":4}` | `201` (estado `AUTORIZADA`) |
| 18 | ¿Está autorizado? | `GET /api/v1/autorizaciones/vendedores/{id}` | — | 1 y 2 → `true`; 3 y 4 → `false` |
| 19 | Historial | `GET /api/v1/autorizaciones/vendedores/3/historial` | — | `200`, AUTORIZADA y luego REVOCADA |
| 20 | Revocar autorización | `PATCH /api/v1/autorizaciones/vendedores/2/revocacion` | — | `200` (estado `REVOCADA`) |

## 5. Bodega (productos, bodegas, inventario y movimientos)

| # | Acción | Método y ruta | Resultado esperado |
|---|---|---|---|
| 21 | Listar productos | `GET /api/v1/productos` | `200`, 8 productos |
| 22 | Buscar productos por nombre | `GET /api/v1/productos?nombre=Arroz` | `200` |
| 23 | Productos por proveedor | `GET /api/v1/productos?nitProveedor=900123456-7` | `200`, productos 1–4 y 8 |
| 24 | Consultar producto | `GET /api/v1/productos/1` | `200` |
| 25 | Actualizar precio | `PUT /api/v1/productos/1/precio` — body `{"precio":3200.0}` | `200` |
| 26 | Listar bodegas | `GET /api/v1/bodegas` | `200`, 3 bodegas |
| 27 | Buscar bodegas por ubicación | `GET /api/v1/bodegas?ubicacion=Bogota` | `200` |
| 28 | Consultar bodega | `GET /api/v1/bodegas/1` | `200` |
| 29 | Disponibilidad de bodega | `GET /api/v1/bodegas/1/disponibilidad` | `200`, unidades almacenadas |
| 30 | Inventarios de una bodega | `GET /api/v1/bodegas/1/inventarios` | `200` |
| 31 | Stock de un producto en una bodega | `GET /api/v1/inventarios/stock?productoId=1&bodegaId=1` | `200` |
| 32 | Inventario por producto | `GET /api/v1/inventarios/productos/1` | `200`, bodegas 1 y 2 |
| 33 | Stock total de un producto | `GET /api/v1/inventarios/productos/1/stock-total` | `200`, 165 |
| 34 | Inventario por bodega | `GET /api/v1/inventarios/bodegas/1` | `200` |
| 35 | Requieren reposición | `GET /api/v1/inventarios/reposicion` | `200`, inventarios 3, 6 y 8 |
| 36 | Listar movimientos | `GET /api/v1/movimientos-stock` | `200` |
| 37 | Movimientos por tipo | `GET /api/v1/movimientos-stock?tipo=SALIDA` | `200` |
| 38 | Movimientos por fechas | `GET /api/v1/movimientos-stock?desde=2026-11-01&hasta=2026-11-30` | `200` |
| 39 | Consultar movimiento | `GET /api/v1/movimientos-stock/1` | `200` |
| 40 | Movimientos por inventario | `GET /api/v1/movimientos-stock/inventarios/1` | `200` |
| 41 | Movimientos por producto | `GET /api/v1/movimientos-stock/productos/1` | `200` |
| 42 | Movimientos por compra | `GET /api/v1/movimientos-stock/compras/1` | `200`, 2 entradas |
| 43 | Movimientos por entrega | `GET /api/v1/movimientos-stock/entregas/1` | `200`, 2 salidas |

## 6. Proveedores y compras

| # | Acción | Método y ruta | Body | Resultado esperado |
|---|---|---|---|---|
| 44 | Listar proveedores | `GET /api/v1/proveedores` | — | `200`, 3 proveedores |
| 45 | Buscar por nombre | `GET /api/v1/proveedores?nombre=Andina` | — | `200` |
| 46 | Proveedores de un producto | `GET /api/v1/proveedores?productoId=8` | — | `200`, Andina y Aseo Total |
| 47 | Consultar proveedor | `GET /api/v1/proveedores/900123456-7` | — | `200` |
| 48 | Actualizar contacto | `PUT /api/v1/proveedores/900123456-7/contacto` | `{"contacto":"compras@andina.co"}` | `200` |
| 49 | Registrar compra | `POST /api/v1/compras` | `{"nitProveedor":"900123456-7","items":[{"idProducto":1,"cantidad":5,"precio":2500.0}]}` | `201` (`PENDIENTE`) |
| 50 | Compras por proveedor | `GET /api/v1/compras?nitProveedor=900123456-7` | — | `200` |
| 51 | Compras pendientes | `GET /api/v1/compras/pendientes` | — | `200`, compras 2 y 4 |
| 52 | Consultar compra | `GET /api/v1/compras/1` | — | `200` |
| 53 | Recibir compra | `PATCH /api/v1/compras/2/recepcion` | `{"idBodega":2}` | `200` (`RECIBIDA`), suma stock y crea movimientos `ENTRADA`; repetir → `409` |
| 54 | Cancelar compra | `PATCH /api/v1/compras/4/cancelacion` | — | `200` (`CANCELADA`) |

## 7. Ventas

| # | Acción | Método y ruta | Body | Resultado esperado |
|---|---|---|---|---|
| 55 | Registrar venta | `POST /api/v1/ventas` | `{"idVendedor":1,"idCliente":1,"items":[{"idProducto":1,"cantidad":3}]}` | `201` (venta 6, no descuenta stock); con vendedor 3 o 4 → `409` (no autorizado) |
| 56 | Ventas por vendedor | `GET /api/v1/ventas?vendedorId=1` | — | `200` |
| 57 | Ventas por cliente | `GET /api/v1/ventas?clienteId=1` | — | `200` |
| 58 | Consultar venta | `GET /api/v1/ventas/1` | — | `200`, total 34500 con detalles |
| 59 | Cancelar venta | `PATCH /api/v1/ventas/5/cancelacion` | — | `200` (`CANCELADA`); la venta 1 (ya entregada) → `409` |

## 8. Entregas

| # | Acción | Método y ruta | Body | Resultado esperado |
|---|---|---|---|---|
| 60 | Registrar entrega | `POST /api/v1/entregas` | `{"idVenta":3,"direccion":"Carrera 7 # 10-20"}` | `201` (`PENDIENTE`); venta con entrega existente → `409` |
| 61 | Entregas por estado | `GET /api/v1/entregas?estado=PENDIENTE` | — | `200` |
| 62 | Entregas por fechas | `GET /api/v1/entregas?desde=2026-11-01&hasta=2026-11-30` | — | `200` |
| 63 | Entregas pendientes | `GET /api/v1/entregas/pendientes` | — | `200`, entrega 2 |
| 64 | Consultar entrega | `GET /api/v1/entregas/1` | — | `200` |
| 65 | Entrega de una venta | `GET /api/v1/entregas/ventas/1` | — | `200` |
| 66 | Confirmar entrega | `PATCH /api/v1/entregas/2/confirmacion` | `{"idBodega":1}` | `200` (`ENTREGADA`), descuenta stock y crea movimientos `SALIDA`; repetir → `409` |

## 9. Públicos

| # | Acción | Método y ruta | Resultado esperado |
|---|---|---|---|
| 67 | Saludo | `GET /api/v1/hola` | `200`, `{"mensaje":"Hola mundo"}` |
| 68 | Estado de la API | `GET /actuator/health` | `200`, `UP` (incluye la base de datos) |

## Flujo de prueba completo sugerido

1. **Login** (1).
2. **¿Está autorizado?** vendedor 3 (18) → `false`; **Registrar venta** con `idVendedor` 3 (55) → `409`.
3. **Registrar venta** con vendedor 1 (55) → `201`, venta 6.
4. **Registrar entrega** con `idVenta` 6 (60) → `201`.
5. **Stock** producto 1, bodega 1 (31) → anotar el valor.
6. **Confirmar entrega** creada, desde la bodega 1 (66) → `200`; el stock (31) baja 3 unidades.
7. Repetir la confirmación → `409` (el stock no se descuenta dos veces).
8. **Recibir compra** 2 en la bodega 2 (53) y revisar **Movimientos por compra** 2 (42).
9. **Requieren reposición** (35) para ver los productos bajo el mínimo.
10. **Crear vendedor** (5), hacer login con ese usuario y comprobar que `GET /api/v1/usuarios` responde `403`.

Para volver a los datos iniciales, ver *Restaurar las semillas* en [`endpoints.md`](endpoints.md#datos-semilla-perfil-dev).
