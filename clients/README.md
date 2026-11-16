# Clientes PoliMarket

El backend Java/Spring es el proveedor de componentes; no cuenta como segundo cliente.
Se incluyen Flutter/Dart (Android y Windows) y consola Python 3, ambos con consumo HTTP real.
Flutter para dos plataformas sigue siendo un solo cliente/lenguaje.

## Ejecutar

1. En la raíz, configurar `.env` según el README principal, iniciar PostgreSQL con `docker compose up -d --wait` e iniciar la API con `.\gradlew.bat bootRun --args="--spring.profiles.active=dev"`.
2. Usar el administrador configurado en `ADMIN_USERNAME` / `ADMIN_PASSWORD`. No hay credenciales predeterminadas.
3. Flutter (requiere SDK Flutter y herramientas de la plataforma):

```powershell
cd clients/flutter_app
flutter pub get
flutter run -d windows
# Emulador Android iniciado:
flutter run -d <id-emulador> --dart-define=API_URL=http://10.0.2.2:8080/api/v1
flutter analyze
flutter test
```

La URL también se puede cambiar en el inicio de sesión. Windows usa `127.0.0.1`;
un teléfono físico requiere la IP LAN del equipo y acceso al puerto 8080.
El cliente es nativo; no se incluye Flutter Web ni se requieren cambios CORS.
El manifiesto Android admite HTTP para la demostración local. Usar HTTPS y retirar
`usesCleartextTraffic` en un despliegue real.

4. Segundo cliente (desde la raíz, Python 3 sin paquetes adicionales):

```powershell
python clients/python_cli/polimarket.py --username <usuario> --bodega 1
```

Solicita la contraseña de forma oculta, consulta clientes, catálogo e inventario y cierra sesión.

## Cinco requisitos funcionales y evidencia

| Requisito | Pantallas Flutter / consumo de API |
|---|---|
| RF1: RRHH autoriza vendedores | RRHH: listar, consultar autorización, autorizar y revocar (`/vendedores`, `/autorizaciones`) |
| RF2: Ventas consulta clientes y disponibilidad | Ventas: clientes/productos; Bodega: stock por producto y bodega (`/clientes`, `/productos`, `/inventarios/stock`) |
| RF3: Vendedor autorizado registra ventas | Ventas: registrar y consultar ventas (`POST /ventas`, `GET /ventas?vendedorId=`) |
| RF4: Bodega registra abastecimiento de proveedores | Proveedores: contacto, productos por NIT, registrar compra y recibirla (`/proveedores`, `/productos?nitProveedor=`, `/compras`, `/compras/{id}/recepcion`) |
| RF5: Entregas consulta destinatario y registra salida | Entregas: consultar venta, registrar y confirmar entrega (`/ventas/{id}`, `/entregas`, `/entregas/{id}/confirmacion`) |

Flutter consume los cinco requisitos; Python consulta clientes (Clientes), catálogo de productos e inventario por bodega (Bodega).

## Demostración con datos semilla dev

- Ingresar como ADMIN. RRHH → Vendedores; consultar vendedor 4 y autorizarlo si aún no está autorizado.
- Ventas → Clientes y Productos para identificar IDs. Bodega → Stock: producto 1, bodega 1.
- Registrar venta: vendedor 4, cliente 1, producto 1, cantidad 1. Guardar el ID devuelto.
- Entregas → consultar esa venta para ver cliente y detalles. Registrar entrega con su ID y dirección; confirmar el ID de entrega en bodega 1.
- Consultar nuevamente stock: disminuye una unidad. Una segunda confirmación debe fallar sin descontar nuevamente.
- Proveedores → proveedores/productos: NIT `900123456-7`; registrar compra del producto 1, cantidad 1, costo 2500. Recibir el ID de compra en bodega 1; comprobar el incremento del stock.
- Ejecutar Python y mostrar sus consultas contra los mismos datos.

Los formularios mínimos registran un producto por venta/compra. Los IDs se obtienen
con las consultas de la interfaz. El servidor valida stock y autorización, calcula
precios y totales de venta y controla transacciones. ADMIN ve todas las acciones;
VENDEDOR ve consultas permitidas y registro de ventas. La API no vincula usuarios
con IDs de vendedores: el ID comercial se introduce explícitamente.

Los tokens permanecen en memoria. Flutter renueva una vez ante 401 y elimina la
sesión local al salir. Los errores ProblemDetail y de validación se muestran en
pantalla; las operaciones se bloquean mientras se procesan. Si una escritura agota
el tiempo, consultar su estado antes de repetirla.

