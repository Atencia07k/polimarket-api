# PoliMarket API

API REST académica de **PoliMarket** (Maestría en Arquitectura de Software – Temas Avanzados en Diseño de Software). Su objetivo es demostrar la **reutilización de software mediante componentes** que integran las áreas de Recursos Humanos, Ventas, Bodega, Proveedores y Entregas.

> Etapa actual: entidades JPA, repositorios, servicios de negocio, controladores REST (`/api/v1`) y **seguridad con JWT** (access token RS256 + refresh token rotativo, roles `ADMIN`/`VENDEDOR`) implementados y probados. El esquema de base de datos se gestiona con migraciones **Flyway**.

## Ejecución local en Windows (paso a paso)

Guía para levantar la API desde cero en cualquier equipo Windows 10/11. Todos los comandos se ejecutan en **PowerShell** desde la raíz del proyecto.

### 1. Instalar los requisitos (una sola vez)

| Herramienta | Para qué | Instalación sugerida | Verificación |
|---|---|---|---|
| Git | Clonar el repositorio | `winget install Git.Git` | `git --version` |
| JDK 21 | Compilar y ejecutar (obligatorio: Gradle no lo descarga) | `winget install Microsoft.OpenJDK.21` | `java -version` → `21.x` |
| Docker Desktop | PostgreSQL 18.7 en contenedor | `winget install Docker.DockerDesktop` (requiere WSL 2) | `docker version` y `docker compose version` |
| IntelliJ IDEA (opcional) | IDE | `winget install JetBrains.IntelliJIDEA.Community` | — |
| DBeaver (opcional) | Explorar la base de datos | `winget install dbeaver.dbeaver` | — |

No hace falta instalar Gradle: el proyecto incluye el wrapper `gradlew.bat`. Después de instalar, **cierre y abra PowerShell** para refrescar el `PATH` e **inicie Docker Desktop** (espere a que indique *Engine running*).

### 2. Clonar el repositorio

```powershell
git clone https://github.com/Atencia07k/polimarket-api.git
cd polimarket-api
git switch develop        # rama de trabajo (master es la rama principal/estable)
```

> Recomendación: clone en una ruta corta y fuera de OneDrive (por ejemplo `C:\dev\polimarket-api`) para evitar bloqueos de archivos durante la compilación.

### 3. Copiar los archivos de configuración que no están en el repositorio

Por seguridad, estos archivos **no están en GitHub** (están en `.gitignore`):

| Archivo / carpeta | Contenido |
|---|---|
| `.env` | Credenciales de PostgreSQL, primer administrador (`ADMIN_USERNAME`/`ADMIN_PASSWORD`) y ubicación de las llaves JWT |
| `secrets/jwt-private.pem` | Llave privada RSA que **firma** los tokens JWT |
| `secrets/jwt-public.pem` | Llave pública RSA que **valida** los tokens JWT |

**Integrantes del equipo:** estos archivos **ya se les compartieron** por un canal privado en el paquete **`polimarket-api-config-local.zip`**. Descomprímalo en la **raíz del proyecto** (la carpeta que contiene `build.gradle`) y verifique:

```powershell
Expand-Archive ..\polimarket-api-config-local.zip -DestinationPath . -Force   # ajuste la ruta del .zip
Test-Path .env, secrets\jwt-private.pem, secrets\jwt-public.pem              # debe imprimir True True True
git status --short                                                            # no debe listar .env ni secrets/
```

Con el paquete, el `.env` ya trae la contraseña de PostgreSQL, el administrador inicial y las llaves: **continúe en el paso 4** (en el paso 5 no necesita generar el administrador; las credenciales para iniciar sesión están en `ADMIN_USERNAME`/`ADMIN_PASSWORD` del `.env`). Nunca suba el `.zip` ni su contenido a un repositorio ni lo comparta por canales públicos.

**Sin el paquete (equipo nuevo o externo):** cree su propio `.env` a partir de la plantilla `.env.example` como se indica a continuación, y opcionalmente sus llaves en `secrets/` ([Llaves RSA](#llaves-rsa-para-firmar-los-tokens)):

```powershell
Copy-Item .env.example .env
# Generar una contraseña aleatoria alfanumérica de 32 caracteres y guardarla en .env
$pgPass = -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 32 | ForEach-Object { [char]$_ })
(Get-Content .env) -replace '^POSTGRES_PASSWORD=.*', "POSTGRES_PASSWORD=$pgPass" | Set-Content .env
Get-Content .env          # verificar que POSTGRES_PASSWORD tiene valor
```

### 4. Levantar PostgreSQL en Docker

```powershell
docker compose up -d --wait     # descarga postgres:18.7, crea el volumen y espera a "healthy"
docker compose ps               # STATUS debe mostrar (healthy)
```

La base `polimarket_db` y el usuario `polimarket` se crean automáticamente la primera vez con la contraseña de `.env`. Si el puerto 5432 está ocupado, cambie `POSTGRES_PORT` en `.env` (por ejemplo `5433`) y repita el comando.

### 5. Definir el administrador inicial y el esquema (automático con Flyway)

**Las tablas se crean solas**: al arrancar, Flyway aplica las migraciones de `src/main/resources/db/migration` (`V1__esquema_inicial.sql`: 14 tablas del negocio; `V2__seguridad_usuarios.sql`: `usuario` y `refresh_token`) y Hibernate solo **valida** que las entidades coincidan (`ddl-auto=validate`). Si la base ya tenía las 14 tablas de una versión anterior, Flyway la marca como versión 1 (*baseline*) y aplica solo la V2.

Todos los endpoints de negocio exigen un token. Si usó el paquete compartido del paso 3, el administrador ya está definido en `.env`; si no, defina en `.env` las credenciales del **primer administrador** (se crea al arrancar solo si no existe ningún `ADMIN` activo; después puede borrar estas dos líneas):

```powershell
$adminPass = -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 20 | ForEach-Object { [char]$_ })
(Get-Content .env) -replace '^ADMIN_USERNAME=.*', 'ADMIN_USERNAME=admin' -replace '^ADMIN_PASSWORD=.*', "ADMIN_PASSWORD=$adminPass" | Set-Content .env
Get-Content .env          # anote ADMIN_PASSWORD: la necesitará para iniciar sesión
```

Reglas: usuario de 3 a 50 caracteres (`a-z 0-9 . _ -`, se guarda en minúsculas) y contraseña de 8 a 72 caracteres. La contraseña se almacena con BCrypt; nunca en texto plano.

Tras el primer arranque (paso 7) puede verificar el esquema:

```powershell
"SELECT version, description, success FROM flyway_schema_history; \dt" | docker exec -i polimarket-postgres sh -c 'psql -U $POSTGRES_USER -d $POSTGRES_DB'
```

Deben listarse 17 tablas: las 14 del negocio, `usuario`, `refresh_token` y `flyway_schema_history`.

**Datos semilla:** con el perfil `dev` Flyway también carga `src/main/resources/db/seed/R__datos_semilla.sql` (4 vendedores, 5 clientes, 3 proveedores, 8 productos, 3 bodegas con inventario, compras, ventas, entregas y movimientos), de modo que la API tiene datos para probar desde el primer arranque. Es idempotente, no crea usuarios y no se carga en las pruebas ni fuera de `dev`. Detalle de los datos y cómo restaurarlos: [`docs/endpoints.md`](docs/endpoints.md#datos-semilla-perfil-dev).

### 6. Compilar y ejecutar las pruebas

```powershell
.\gradlew.bat clean build          # compila y ejecuta las pruebas unitarias (no requieren base de datos)
.\gradlew.bat integrationTest      # opcional: pruebas de integración (requieren Docker en ejecución)
```

La primera ejecución descarga Gradle y las dependencias (varios minutos). Debe terminar con `BUILD SUCCESSFUL`.

### 7. Ejecutar la API

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

o desde IntelliJ con la configuración **PolimarketApi (dev)** (ver [Ejecución desde IntelliJ IDEA](#ejecución-desde-intellij-idea)). La API queda disponible en `http://127.0.0.1:8080` cuando aparece `Started PolimarketApiApplication`.

### 8. Verificar que funciona

```powershell
Invoke-RestMethod http://127.0.0.1:8080/actuator/health      # status: UP (incluye la base de datos)
Invoke-RestMethod http://127.0.0.1:8080/api/v1/hola          # mensaje: Hola mundo (público)
Invoke-WebRequest http://127.0.0.1:8080/api/v1/clientes -SkipHttpErrorCheck | Select-Object StatusCode   # 401: requiere token

# Iniciar sesión con el administrador del paso 5 y usar el access token
$login = Invoke-RestMethod http://127.0.0.1:8080/api/v1/auth/login -Method Post -ContentType 'application/json' `
    -Body (@{ username = 'admin'; password = '<ADMIN_PASSWORD de su .env>' } | ConvertTo-Json)
$auth = @{ Authorization = "Bearer $($login.accessToken)" }
Invoke-RestMethod http://127.0.0.1:8080/api/v1/auth/me -Headers $auth          # datos del usuario autenticado
Invoke-RestMethod http://127.0.0.1:8080/api/v1/clientes -Headers $auth         # 5 clientes de los datos semilla
```

(`-SkipHttpErrorCheck` requiere PowerShell 7; en Windows PowerShell 5.1 la petición sin token muestra el error 401.) Cómo funcionan los tokens: [Autenticación y seguridad (JWT)](#autenticación-y-seguridad-jwt). La referencia completa de endpoints está en [`docs/endpoints.md`](docs/endpoints.md).

### 9. Conectarse a la base de datos con DBeaver (opcional)

*Database > New Database Connection > PostgreSQL*:

| Campo | Valor |
|---|---|
| Host | `localhost` |
| Port | `5432` (o el valor de `POSTGRES_PORT`) |
| Database | `polimarket_db` |
| Username | `polimarket` |
| Password | el valor de `POSTGRES_PASSWORD` en su `.env` |

### 10. Detener el entorno

```powershell
# API: Ctrl + C en la consola de bootRun (o Stop en IntelliJ)
docker compose stop        # detiene PostgreSQL conservando los datos
# docker compose down -v   # CUIDADO: elimina el contenedor y el volumen (se pierden todos los datos)
```

## Flujo de trabajo con Git

| Rama | Propósito |
|---|---|
| `master` | Rama principal y estable. Solo recibe cambios integrados desde `develop`. |
| `develop` | Rama de desarrollo: **todas las modificaciones nuevas se suben aquí**. |

```powershell
git switch develop
git pull
# ... cambios ...
git add .
git commit -m "Descripción del cambio"
git push origin develop
```

Antes de cada commit revise `git status`: nunca debe aparecer `.env` ni otros archivos con credenciales (están excluidos en `.gitignore`).

## Tecnologías

| Tecnología | Versión |
|---|---|
| Java | 21 (Gradle toolchain) |
| Spring Boot | 4.1.2 (Spring Framework 7) |
| Gradle (wrapper, Groovy DSL) | 9.9.1 |
| PostgreSQL (imagen Docker oficial) | 18.7 |
| Hibernate ORM / Spring Data JPA | administradas por Spring Boot |
| Spring Security + OAuth2 Resource Server (JWT RS256, Nimbus) | administradas por Spring Boot |
| Flyway (migraciones del esquema) | administrado por Spring Boot |
| Jakarta Bean Validation, Actuator, Lombok | administradas por Spring Boot |
| JUnit Jupiter, Mockito | administradas por Spring Boot |

## Requisitos previos

- JDK 21 instalado (`java -version`).
- Docker Desktop iniciado (`docker version`, `docker compose version`).
- IntelliJ IDEA con el plugin de Lombok (incluido por defecto) y **Annotation Processing** habilitado.
- No se requiere Gradle instalado: se usa el wrapper `gradlew.bat`.

## Estructura de carpetas

```text
polimarket-api/
├── .github/copilot-instructions.md   Reglas de desarrollo para GitHub Copilot
├── .run/                             Configuración de ejecución para IntelliJ (perfil dev)
├── gradle/wrapper/                   Gradle Wrapper 9.9.1
├── src/main/java/com/polimarket/api/
│   ├── entity/          15 entidades JPA (13 del modelo UML + Usuario y RefreshToken)
│   ├── repository/      Interfaces JpaRepository por área funcional:
│   │                    clientes/ ventas/ rrhh/ bodega/ proveedores/ entregas/ autenticacion/
│   ├── service/         Contratos de servicio por área
│   │   └── impl/        Implementaciones de los servicios de negocio
│   ├── controller/      Controladores REST por área (/api/v1)
│   ├── dto/             DTO de entrada/respuesta por área (records)
│   ├── config/          Configuración general (creación del administrador inicial)
│   ├── security/        Spring Security: SecurityConfig (matriz de acceso), JwtConfig (llaves RSA, encoder/decoder)
│   ├── exception/       Excepciones y manejo centralizado de errores
│   └── PolimarketApiApplication.java
├── src/main/resources/
│   ├── application.properties        Configuración común
│   ├── application-dev.properties    PostgreSQL en Docker
│   ├── application-test.properties   Pruebas sin base de datos
│   ├── application-prod.properties   Variables de entorno de producción
│   ├── db/migration/                 Migraciones Flyway (V1 esquema de negocio, V2 seguridad)
│   └── db/seed/                      Datos semilla (R__datos_semilla.sql, solo perfil dev)
├── src/test/java/com/polimarket/api/   Pruebas unitarias e integración
├── docs/endpoints.md                   Referencia de endpoints REST
├── docs/peticiones-api.md              Guía de las 68 peticiones de prueba con resultados esperados
├── docs/postman/                       Colección de Postman (login, tokens y todos los endpoints)
├── build.gradle · settings.gradle · gradle.properties · lombok.config
├── gradlew · gradlew.bat
├── compose.yaml · .env.example · .gitignore
└── README.md
```

## Variables de entorno

Las credenciales nunca se versionan. Los integrantes del equipo reciben el `.env` y la carpeta `secrets/` en el paquete privado `polimarket-api-config-local.zip` ([paso 3](#3-copiar-los-archivos-de-configuración-que-no-están-en-el-repositorio)). El archivo `.env` (ignorado por Git) es el único lugar donde se guarda la contraseña local y lo usan **ambos** procesos:

- **Docker Compose** lo carga automáticamente desde la raíz del proyecto.
- **Spring Boot (perfil `dev`)** lo importa con `spring.config.import=optional:file:.env[.properties]`, relativo al directorio de trabajo (raíz del proyecto en IntelliJ y en `bootRun`). Las variables de entorno del sistema tienen prioridad sobre `.env`.

Crear el archivo a partir de la plantilla y definir una contraseña alfanumérica:

```powershell
Copy-Item .env.example .env
# Generar una contraseña aleatoria de 32 caracteres y guardarla en .env
$pgPass = -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 32 | ForEach-Object { [char]$_ })
(Get-Content .env) -replace '^POSTGRES_PASSWORD=.*', "POSTGRES_PASSWORD=$pgPass" | Set-Content .env
```

| Variable | Uso | Valor por defecto |
|---|---|---|
| `POSTGRES_DB` | Base de datos | `polimarket_db` |
| `POSTGRES_USER` | Usuario | `polimarket` |
| `POSTGRES_PASSWORD` | Contraseña (obligatoria) | — |
| `POSTGRES_HOST` | Host para Spring Boot | `localhost` |
| `POSTGRES_PORT` | Puerto publicado / JDBC | `5432` |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | Primer administrador; se crea al arrancar solo si no existe ningún `ADMIN` activo | — (vacías: no se crea) |
| `JWT_PRIVATE_KEY_LOCATION` / `JWT_PUBLIC_KEY_LOCATION` | Llaves RSA (PEM) para firmar/validar tokens. Opcionales en `dev`, **obligatorias en `prod`** | — (dev: llaves efímeras) |

Perfil `prod`: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_PRIVATE_KEY_LOCATION`, `JWT_PUBLIC_KEY_LOCATION` (obligatorias) y `DB_POOL_MAX_SIZE` (opcional), proporcionadas por el entorno de despliegue.

## Docker y PostgreSQL

`compose.yaml` define el servicio `postgres` (contenedor `polimarket-postgres`, imagen `postgres:18.7`) publicado solo en `127.0.0.1:5432`, con healthcheck `pg_isready`, reinicio `unless-stopped` y volumen persistente `polimarket_postgres_data` montado en `/var/lib/postgresql` (estructura recomendada para PostgreSQL 18+, `PGDATA=/var/lib/postgresql/18/docker`).

```powershell
docker compose config --quiet          # validar compose.yaml
docker compose up -d --wait            # descargar imagen, crear e iniciar (espera a "healthy")
docker compose ps                      # estado
"SELECT current_database(), 1;" | docker exec -i polimarket-postgres sh -c 'psql -U $POSTGRES_USER -d $POSTGRES_DB'
docker compose stop                    # detener (conserva los datos)
```

La base de datos `polimarket_db` se crea automáticamente en el primer arranque del volumen. Las tablas las crea y versiona **Flyway** al arrancar la aplicación (`db/migration`); Hibernate solo valida el esquema (`ddl-auto=validate`). Todo cambio de esquema futuro se hace con una nueva migración `V3__descripcion.sql`, nunca editando una migración ya aplicada.

## Ejecución desde IntelliJ IDEA

1. `File > Open` y seleccionar la carpeta `polimarket-api` (se importa como proyecto Gradle).
2. `File > Project Structure > Project SDK`: JDK 21.
3. `Settings > Build, Execution, Deployment > Compiler > Annotation Processors`: activar **Enable annotation processing** (Lombok).
4. Iniciar PostgreSQL: `docker compose up -d --wait`.
5. Ejecutar la configuración **PolimarketApi (dev)** (incluida en `.run/`). Alternativamente, crear una configuración *Application* con clase principal `com.polimarket.api.PolimarketApiApplication`, argumentos `--spring.profiles.active=dev` y directorio de trabajo en la raíz del proyecto (necesario para leer `.env`).

## Compilación y pruebas con Gradle (PowerShell)

```powershell
.\gradlew.bat --version
.\gradlew.bat clean build          # compila y ejecuta las pruebas unitarias
.\gradlew.bat test                 # solo pruebas unitarias (no requieren PostgreSQL)
.\gradlew.bat integrationTest      # pruebas de integración (requieren Docker y el contenedor activo)
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

Pruebas incluidas:

- `SaludoServiceImplTest`: el servicio devuelve `Hola mundo`.
- `SaludoControllerTest`: controlador con `SaludoService` simulado mediante Mockito.
- `SaludoControllerWebMvcTest`: `@WebMvcTest` + `@MockitoBean`; verifica HTTP 200, JSON y `mensaje`.
- `controller/**/*ControllerWebMvcTest`: `@WebMvcTest` + `@MockitoBean` + MockMvc (perfil `test`, sin PostgreSQL) para los 11 controladores de negocio, autenticados como `ADMIN` con `@WithMockUser`: 200/201 (`Location`), 400 (validación con `errores`, tipos y filtros), 404, 409, 500 genérico, JSON de los DTO e invocación del servicio.
- `controller/autenticacion/AutenticacionControllerWebMvcTest`: login (200 / 401 genérico con `WWW-Authenticate`), refresh, logout (204), creación de usuarios (201, 400, 409) y desactivación.
- `security/SecurityMatrixWebMvcTest`: tokens **reales** firmados con RS256: sin token, token alterado, expirado, de otro emisor o de otra audiencia → 401; `VENDEDOR` en rutas de `ADMIN` → 403; matriz de roles y `GET /api/v1/hola` público.
- `service/impl/autenticacion/*ServiceImplTest`: login sin enumeración de usuarios, refresh guardado solo como hash SHA-256, rotación, detección de reutilización (revoca la familia), logout idempotente, política de usuarios y contraseñas, administrador inicial.
- `controller/ControllerHttpIT` (`@Tag("integration")`): HTTP real (puerto aleatorio) contra PostgreSQL efímero creado por Flyway, con **login real**: autorizar → vender → entregar → confirmar (stock descontado una sola vez), vendedor sin autorización (409), stock insuficiente (409), compra → recepción, actualización de cliente; flujo completo de tokens (login → me → refresh → reutilización detectada → logout) y matriz `ADMIN`/`VENDEDOR`; verifica lo persistido con `JdbcTemplate`.
- `PolimarketApiApplicationTests`: el contexto completo (servicios incluidos) arranca con el perfil `test` sin base de datos; los repositorios se simulan con `@MockitoBean`.
- `service/impl/**/*ServiceImplTest`: pruebas unitarias Mockito de los 11 servicios de negocio (Clientes, RRHH, Bodega, Proveedores, Ventas, Entregas).
- `PostgresConnectionIT` (`@Tag("integration")`): `SELECT 1` contra `polimarket_db` y petición HTTP real al endpoint.
- `service/DatosSemillaIT` (`@Tag("integration")`): aplica las migraciones **y** los datos semilla sobre un PostgreSQL efímero y verifica conteos, reglas de negocio (vendedor revocado, inventarios bajo el mínimo, totales de venta), que se puede operar sobre ellos y que el script es idempotente.
- `repository/**/*RepositoryIT` (`@Tag("integration")`): `@DataJpaTest` contra un PostgreSQL 18.7 **efímero** levantado con Testcontainers (no usa `polimarket_db`), con el esquema creado por las migraciones Flyway y validado contra las entidades; valida las consultas derivadas y JPQL de los 15 repositorios.
- `service/ServiceLayerIT` (`@Tag("integration")`): flujos reales contra PostgreSQL efímero: venta → entrega → confirmación descuenta stock una sola vez, reversión atómica por stock insuficiente, confirmaciones concurrentes, recepción de compras y vendedor revocado.

## Capa de servicios

| Área | Servicios | Operaciones principales |
|---|---|---|
| Clientes | `ClienteService` | consultar, buscar, actualizar datos |
| RRHH | `VendedorService`, `AutorizacionRRHHService` | validar, autorizar, revocar, historial |
| Bodega | `ProductoService`, `BodegaService`, `InventarioService`, `MovimientoStockService` | catálogo, disponibilidad, stock, entradas/salidas, reposición |
| Proveedores | `ProveedorService`, `CompraProveedorService` | registrar (`PENDIENTE`), recibir (`RECIBIDA`), cancelar compras |
| Ventas | `VentaService` | registrar (`REGISTRADA`) y cancelar ventas; no descuenta stock |
| Entregas | `EntregaService` | registrar (`PENDIENTE`) y confirmar (`ENTREGADA`, descuenta stock) |

Las reglas de negocio aprobadas están en `.github/copilot-instructions.md`.

## Primer endpoint

```powershell
Invoke-WebRequest http://localhost:8080/api/v1/hola -UseBasicParsing | Select-Object StatusCode, Content
# StatusCode: 200   Content: {"mensaje":"Hola mundo"}
Invoke-RestMethod http://localhost:8080/actuator/health
```

## Endpoints REST

Los 13 controladores (`controller/{clientes,rrhh,bodega,proveedores,ventas,entregas,autenticacion}`) exponen la API bajo `/api/v1`; la referencia completa (rutas, DTO, códigos, errores y ejemplos) está en [`docs/endpoints.md`](docs/endpoints.md).

## Autenticación y seguridad (JWT)

La API es un **OAuth 2.0 Resource Server sin estado**: no hay sesiones HTTP ni cookies. Cada petición a un endpoint privado debe llevar la cabecera `Authorization: Bearer <access token>` y Spring Security verifica en **cada petición** la firma RS256, `exp`/`nbf` (tolerancia de 30 s), `iss = polimarket-api`, `aud = polimarket-api` y `sub`. La misma cadena de seguridad aplica a todos los perfiles.

| Token | Formato | Vigencia | Dónde se guarda |
|---|---|---|---|
| Access token | JWT firmado con RSA 2048 (RS256). Claims: `sub` (usuario), `uid`, `roles`, `iat`, `exp`, `jti` | 15 min | Solo en el cliente |
| Refresh token | Valor aleatorio opaco de 256 bits | 7 días | Cliente; en la base solo su **hash SHA-256** (`refresh_token`) |

**Flujo:**

1. `POST /api/v1/auth/login` `{"username","password"}` → `{accessToken, tokenType:"Bearer", expiresIn, refreshToken, refreshExpiresIn}`. Un error de credenciales responde siempre `401` con el mismo mensaje genérico (no revela si el usuario existe).
2. Llamar a los endpoints con `Authorization: Bearer <accessToken>`.
3. Cuando el access token expira (`401`), `POST /api/v1/auth/refresh` `{"refreshToken"}` devuelve un **par nuevo**; el refresh token usado queda revocado (**rotación**).
4. Si se presenta un refresh token ya rotado (posible robo), se revoca **toda la familia** de esa sesión y se responde `401`: el usuario debe iniciar sesión de nuevo.
5. `POST /api/v1/auth/logout` `{"refreshToken"}` → `204`; revoca la sesión. El access token vigente sigue siendo válido hasta su expiración (máximo 15 min): es el compromiso de un diseño sin estado.

**Matriz de autorización:**

| Ruta | Acceso |
|---|---|
| `GET /api/v1/hola`, `GET /actuator/health`, `/error` | Público |
| `POST /api/v1/auth/login`, `/refresh`, `/logout` | Público (el refresh token es la credencial) |
| `GET /api/v1/auth/me` | `ADMIN` o `VENDEDOR` |
| `GET` de `/clientes`, `/productos`, `/inventarios`, `/ventas`, `/entregas` | `ADMIN` o `VENDEDOR` |
| `POST /api/v1/ventas` y `PATCH /api/v1/ventas/{id}/cancelacion` | `ADMIN` o `VENDEDOR` |
| Resto de `/api/v1/**` (escrituras de catálogo, bodega, proveedores, RRHH, entregas y `/usuarios`) | Solo `ADMIN` |
| Cualquier otra ruta | Denegada |

Sin token o con token inválido/expirado: `401` (`urn:polimarket:error:no-autenticado`, cabecera `WWW-Authenticate: Bearer`). Con token válido pero sin el rol: `403` (`urn:polimarket:error:acceso-denegado`). La autorización comercial de Recursos Humanos sigue aplicándose siempre (un vendedor sin autorización vigente recibe `409`).

**Usuarios:** el primer `ADMIN` se crea con `ADMIN_USERNAME`/`ADMIN_PASSWORD` ([paso 5](#5-definir-el-administrador-inicial-y-el-esquema-automático-con-flyway)); luego un `ADMIN` gestiona usuarios con `POST /api/v1/usuarios`, `GET /api/v1/usuarios[/{id}]` y `PATCH /api/v1/usuarios/{id}/desactivacion` (revoca sus refresh tokens; no puede desactivarse a sí mismo).

### Llaves RSA para firmar los tokens

- **`dev` y `test`**: si no se configuran llaves, la API genera un par **efímero** en memoria al arrancar (aparece un `WARN`). Funciona, pero los tokens emitidos dejan de ser válidos al reiniciar la API.
- **`prod`**: las llaves son **obligatorias**; si faltan, la aplicación no arranca.

El equipo ya recibió un par de llaves persistentes en `secrets/` dentro del paquete compartido ([paso 3](#3-copiar-los-archivos-de-configuración-que-no-están-en-el-repositorio)). Para generar un par propio (recomendado también en dev si no quiere volver a iniciar sesión tras cada reinicio), genérelas con OpenSSL (incluido con Git para Windows) en la carpeta `secrets/`, que está excluida de Git:

```powershell
New-Item -ItemType Directory -Force secrets | Out-Null
$openssl = "C:\Program Files\Git\usr\bin\openssl.exe"
& $openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out secrets\jwt-private.pem   # PKCS#8
& $openssl pkey -in secrets\jwt-private.pem -pubout -out secrets\jwt-public.pem
Add-Content .env "JWT_PRIVATE_KEY_LOCATION=secrets/jwt-private.pem"
Add-Content .env "JWT_PUBLIC_KEY_LOCATION=secrets/jwt-public.pem"
```

Nunca versione `secrets/`, `*.pem` ni `.env` (ya están en `.gitignore`). Si una llave privada se filtra, genere un par nuevo: todos los tokens emitidos con la anterior quedan invalidados.

## Resolución de problemas

| Síntoma | Causa probable / solución |
|---|---|
| `Connection to localhost:5432 refused` | El contenedor no está iniciado: `docker compose up -d --wait`; revisar `docker compose ps`. |
| `password authentication failed for user "polimarket"` | La contraseña de `.env` no coincide con la del volumen (PostgreSQL solo la aplica al crear el volumen). Restablecer la contraseña original en `.env`, o cambiarla dentro del contenedor con `ALTER USER`. |
| `Could not resolve placeholder 'POSTGRES_PASSWORD'` | No existe `.env` o el directorio de trabajo no es la raíz del proyecto. |
| `relation "..." does not exist` o `Schema-validation: missing table` | Flyway no se ejecutó o la base tiene un esquema distinto: revisar `flyway_schema_history` y el log de arranque (`Migrating schema`). |
| `Validate failed: Migration checksum mismatch` | Se editó una migración ya aplicada. Revertir el cambio y crear una nueva `V<n>__...sql`. |
| `401` en todos los endpoints | Falta la cabecera `Authorization: Bearer <token>`, el token expiró (15 min: use `/auth/refresh`) o la API se reinició con llaves efímeras (inicie sesión de nuevo o configure llaves persistentes). |
| `403` con un token válido | El rol del usuario no permite la operación (ver la matriz de autorización). |
| `401` al iniciar sesión con el administrador | No se definieron `ADMIN_USERNAME`/`ADMIN_PASSWORD` antes del primer arranque (el log muestra un `WARN`) o la contraseña es otra. Defínalas en `.env` y reinicie: el admin se crea si no existe ningún `ADMIN` activo. |
| `Faltan las llaves JWT` al arrancar con `prod` | Definir `JWT_PRIVATE_KEY_LOCATION` y `JWT_PUBLIC_KEY_LOCATION` (ver [Llaves RSA](#llaves-rsa-para-firmar-los-tokens)). |
| `java -version` no muestra 21 o Gradle indica que no encuentra el toolchain 21 | Instalar el JDK 21 y abrir una nueva consola; si hay varios JDK, definir `JAVA_HOME` apuntando al 21. |
| `Failed to configure a DataSource: 'url' attribute is not specified` | No se activó un perfil: usar `--spring.profiles.active=dev`. |
| `port is already allocated` al iniciar Docker | Otro servicio usa el 5432: `Get-NetTCPConnection -LocalPort 5432`; cambiar `POSTGRES_PORT` en `.env`. |
| `Port 8080 was already in use` | Detener el proceso que usa el puerto o ejecutar con `--server.port=8081`. |
| Docker no responde | Iniciar Docker Desktop y verificar con `docker version`. |
| Errores de Lombok en IntelliJ | Activar *Annotation Processing* y recargar el proyecto Gradle. |
| Bloqueos de archivos en `build/` | El proyecto está en OneDrive: pausar la sincronización durante la compilación si aparecen errores de acceso. |
## Clientes Flutter y Python

Consultar [instrucciones y demostraci�n de los cinco requisitos](clients/README.md).
