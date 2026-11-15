# PoliMarket API

API REST académica de **PoliMarket** (Maestría en Arquitectura de Software – Temas Avanzados en Diseño de Software). Su objetivo es demostrar la **reutilización de software mediante componentes** que integran las áreas de Recursos Humanos, Ventas, Bodega, Proveedores y Entregas.

> Etapa actual: entidades JPA, repositorios, servicios de negocio y controladores REST (`/api/v1`) implementados y probados. Seguridad temporal: endpoints públicos en el perfil `dev`; la autenticación JWT llegará en una etapa posterior.

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

### 3. Crear el archivo `.env` con las credenciales locales

El archivo `.env` **no está en el repositorio** (está en `.gitignore`); cada desarrollador crea el suyo a partir de la plantilla `.env.example`:

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

### 5. Crear las tablas (solo la primera vez)

La configuración usa `spring.jpa.hibernate.ddl-auto=none` (la aplicación no modifica el esquema). En una base nueva, cree las 14 tablas a partir de las entidades JPA ejecutando la API **una sola vez** con la opción `update`:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=dev --spring.jpa.hibernate.ddl-auto=update"
```

Cuando aparezca `Started PolimarketApiApplication`, deténgala con `Ctrl + C` (responda `S` si pregunta). En una base nueva es normal ver advertencias `constraint "..." does not exist, skipping`. Verifique las tablas:

```powershell
"\dt" | docker exec -i polimarket-postgres sh -c 'psql -U $POSTGRES_USER -d $POSTGRES_DB'
```

Deben listarse 14 tablas (`autorizacion_rrhh`, `bodega`, `cliente`, `compra_proveedor`, `detalle_compra`, `detalle_venta`, `entrega`, `inventario`, `movimiento_stock`, `producto`, `producto_proveedor`, `proveedor`, `vendedor`, `venta`). Las tablas inicialmente están vacías.

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
Invoke-RestMethod http://127.0.0.1:8080/api/v1/hola          # mensaje: Hola mundo
Invoke-RestMethod http://127.0.0.1:8080/api/v1/clientes      # [] mientras no existan datos
```

La referencia completa de endpoints está en [`docs/endpoints.md`](docs/endpoints.md).

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
| Spring Security + OAuth2 Resource Server (JWT futuro) | administradas por Spring Boot |
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
│   ├── entity/          13 entidades JPA del modelo UML
│   ├── repository/      Interfaces JpaRepository por área funcional:
│   │                    clientes/ ventas/ rrhh/ bodega/ proveedores/ entregas/ autenticacion/
│   ├── service/         Contratos de servicio por área
│   │   └── impl/        Implementaciones de los servicios de negocio
│   ├── controller/      Controladores REST por área (/api/v1)
│   ├── dto/             DTO de entrada/respuesta por área (records)
│   ├── config/          Configuración general
│   ├── security/        Spring Security (SecurityConfig) y JWT futuro
│   ├── exception/       Excepciones y manejo centralizado de errores
│   └── PolimarketApiApplication.java
├── src/main/resources/
│   ├── application.properties        Configuración común
│   ├── application-dev.properties    PostgreSQL en Docker
│   ├── application-test.properties   Pruebas sin base de datos
│   └── application-prod.properties   Variables de entorno de producción
├── src/test/java/com/polimarket/api/   Pruebas unitarias e integración
├── docs/endpoints.md                   Referencia de endpoints REST
├── build.gradle · settings.gradle · gradle.properties · lombok.config
├── gradlew · gradlew.bat
├── compose.yaml · .env.example · .gitignore
└── README.md
```

## Variables de entorno

Las credenciales nunca se versionan. El archivo `.env` (ignorado por Git) es el único lugar donde se guarda la contraseña local y lo usan **ambos** procesos:

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

Perfil `prod`: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (obligatorias) y `DB_POOL_MAX_SIZE` (opcional), proporcionadas por el entorno de despliegue.

## Docker y PostgreSQL

`compose.yaml` define el servicio `postgres` (contenedor `polimarket-postgres`, imagen `postgres:18.7`) publicado solo en `127.0.0.1:5432`, con healthcheck `pg_isready`, reinicio `unless-stopped` y volumen persistente `polimarket_postgres_data` montado en `/var/lib/postgresql` (estructura recomendada para PostgreSQL 18+, `PGDATA=/var/lib/postgresql/18/docker`).

```powershell
docker compose config --quiet          # validar compose.yaml
docker compose up -d --wait            # descargar imagen, crear e iniciar (espera a "healthy")
docker compose ps                      # estado
"SELECT current_database(), 1;" | docker exec -i polimarket-postgres sh -c 'psql -U $POSTGRES_USER -d $POSTGRES_DB'
docker compose stop                    # detener (conserva los datos)
```

La base de datos `polimarket_db` se crea automáticamente en el primer arranque del volumen. La aplicación usa `spring.jpa.hibernate.ddl-auto=none` y no modifica el esquema; en una base nueva las tablas se crean una única vez según el [paso 5](#5-crear-las-tablas-solo-la-primera-vez). Más adelante el esquema se gestionará con migraciones controladas.

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
- `controller/**/*ControllerWebMvcTest`: `@WebMvcTest` + `@MockitoBean` + MockMvc (perfil `test`, sin PostgreSQL) para los 11 controladores: 200/201 (`Location`), 400 (validación con `errores`, tipos y filtros), 404, 409, 500 genérico, JSON de los DTO e invocación del servicio.
- `security/SecurityProfilesWebMvcTest`: con `prod` las rutas comerciales responden 403 y `hola` sigue público; con `dev` son públicas sin credenciales ni CSRF.
- `controller/ControllerHttpIT` (`@Tag("integration")`): HTTP real (puerto aleatorio) contra PostgreSQL efímero; autorizar → vender → entregar → confirmar (stock descontado una sola vez), vendedor sin autorización (409), stock insuficiente (409), compra → recepción (stock incrementado), actualización de cliente; verifica lo persistido con `JdbcTemplate`.
- `PolimarketApiApplicationTests`: el contexto completo (servicios incluidos) arranca con el perfil `test` sin base de datos; los repositorios se simulan con `@MockitoBean`.
- `service/impl/**/*ServiceImplTest`: pruebas unitarias Mockito de los 11 servicios de negocio (Clientes, RRHH, Bodega, Proveedores, Ventas, Entregas).
- `PostgresConnectionIT` (`@Tag("integration")`): `SELECT 1` contra `polimarket_db` y petición HTTP real al endpoint.
- `repository/**/*RepositoryIT` (`@Tag("integration")`): `@DataJpaTest` contra un PostgreSQL 18.7 **efímero** levantado con Testcontainers (no usa `polimarket_db`); valida las consultas derivadas y JPQL de los 13 repositorios.
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

## Endpoints REST y seguridad temporal

Los 11 controladores (`controller/{clientes,rrhh,bodega,proveedores,ventas,entregas}`) exponen la API bajo `/api/v1`; la referencia completa (rutas, DTO, códigos, errores y ejemplos) está en [`docs/endpoints.md`](docs/endpoints.md).

| Perfil | Cadena de seguridad | Acceso |
|---|---|---|
| `dev` | `DevSecurityConfig` (temporal) | **Todos los endpoints públicos**, sin autenticación; la app escucha solo en `127.0.0.1` |
| `test` | `DevSecurityConfig` (temporal) | Público para las pruebas automatizadas |
| `prod` o sin perfil | `SecurityConfig` | Solo `GET /api/v1/hola`, `GET /actuator/health` y `/error`; el resto responde `403` |

La autenticación JWT llegará en una etapa posterior. La autorización comercial de Recursos Humanos sigue aplicándose siempre (un vendedor sin autorización vigente recibe `409`).

## Resolución de problemas

| Síntoma | Causa probable / solución |
|---|---|
| `Connection to localhost:5432 refused` | El contenedor no está iniciado: `docker compose up -d --wait`; revisar `docker compose ps`. |
| `password authentication failed for user "polimarket"` | La contraseña de `.env` no coincide con la del volumen (PostgreSQL solo la aplica al crear el volumen). Restablecer la contraseña original en `.env`, o cambiarla dentro del contenedor con `ALTER USER`. |
| `Could not resolve placeholder 'POSTGRES_PASSWORD'` | No existe `.env` o el directorio de trabajo no es la raíz del proyecto. |
| `relation "..." does not exist` al consultar endpoints | La base es nueva y no tiene tablas: ejecutar el [paso 5](#5-crear-las-tablas-solo-la-primera-vez). |
| `java -version` no muestra 21 o Gradle indica que no encuentra el toolchain 21 | Instalar el JDK 21 y abrir una nueva consola; si hay varios JDK, definir `JAVA_HOME` apuntando al 21. |
| `Failed to configure a DataSource: 'url' attribute is not specified` | No se activó un perfil: usar `--spring.profiles.active=dev`. |
| `port is already allocated` al iniciar Docker | Otro servicio usa el 5432: `Get-NetTCPConnection -LocalPort 5432`; cambiar `POSTGRES_PORT` en `.env`. |
| `Port 8080 was already in use` | Detener el proceso que usa el puerto o ejecutar con `--server.port=8081`. |
| Docker no responde | Iniciar Docker Desktop y verificar con `docker version`. |
| Errores de Lombok en IntelliJ | Activar *Annotation Processing* y recargar el proyecto Gradle. |
| Bloqueos de archivos en `build/` | El proyecto está en OneDrive: pausar la sincronización durante la compilación si aparecen errores de acceso. |