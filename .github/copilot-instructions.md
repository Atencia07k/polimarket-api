# Instrucciones de desarrollo para GitHub Copilot – PoliMarket API

Proyecto académico (Maestría en Arquitectura de Software – Temas Avanzados en Diseño de Software) cuyo objetivo es demostrar la **reutilización de software mediante componentes** que integran Recursos Humanos, Ventas, Bodega, Proveedores y Entregas. Los diagramas UML de clases y componentes son la fuente de verdad del modelo.

## Stack obligatorio
- Java 21 (toolchain de Gradle).
- Spring Boot 4.1.2 (Spring Framework 7). Usa APIs y paquetes vigentes de Boot 4; evita anotaciones o clases deprecadas.
- Gradle con **Groovy DSL** mediante el wrapper (`.\gradlew.bat`). Nunca Maven ni `pom.xml`.
- PostgreSQL (Docker, `compose.yaml`), Hibernate ORM y Spring Data JPA. No usar H2 ni otra base de datos.
- Lombok para evitar código repetitivo.
- Spring Security con autenticación **JWT** (OAuth2 Resource Server, RS256) y Flyway para migraciones (dependencias autorizadas en la etapa 6).
- Pruebas con JUnit Jupiter y Mockito; Testcontainers (PostgreSQL) para pruebas de repositorios.
- No agregar tecnologías ni dependencias sin autorización; usar versiones administradas por el BOM de Spring Boot.

## Arquitectura por capas (paquete base `com.polimarket.api`)
- `entity`: entidades JPA.
- `repository`: interfaces que extienden `JpaRepository`, sin reglas de negocio.
- `service`: interfaces (contratos) de las funcionalidades.
- `service.impl`: implementaciones con la lógica de negocio.
- `controller`: controladores REST; solo reciben la petición HTTP y delegan en servicios. Sin lógica de negocio.
- `dto`: objetos de entrada y respuesta. Nunca exponer entidades JPA en la API.
- `config`: configuración general.
- `security`: configuración de Spring Security y JWT.
- `exception`: excepciones personalizadas y manejo centralizado de errores.
- Los módulos funcionales (autenticación, recursos humanos, ventas, bodega, proveedores, entregas) se incorporarán respetando estas capas.

## Organización por área funcional (regla permanente)
Todas las capas, **excepto `entity`**, se organizan en subpaquetes por área funcional: `clientes`, `ventas`, `rrhh`, `bodega`, `proveedores`, `entregas` y `autenticacion`.

```text
repository/{clientes,ventas,rrhh,bodega,proveedores,entregas,autenticacion}/
service/{clientes,ventas,rrhh,bodega,proveedores,entregas,autenticacion}/
service/impl/{clientes,ventas,rrhh,bodega,proveedores,entregas,autenticacion}/
controller/{clientes,ventas,rrhh,bodega,proveedores,entregas,autenticacion}/
dto/{clientes,ventas,rrhh,bodega,proveedores,entregas,autenticacion}/
```

- `entity` permanece plano (`com.polimarket.api.entity`) y no se reorganiza.
- Las pruebas replican la misma organización por área (por ejemplo, `src/test/java/com/polimarket/api/repository/ventas/`).
- Cada tabla tiene **un único repositorio**, ubicado en el área propietaria (por ejemplo, `ProductoRepository` solo existe en `repository.bodega`). Otro componente que necesite esos datos los obtiene mediante los **servicios** del área propietaria, nunca creando un repositorio duplicado:
  - Ventas usa los servicios de Bodega para consultar productos y existencias.
  - Entregas usa los servicios de Bodega para registrar salidas de inventario.
  - Proveedores usa los servicios de Bodega para registrar compras y entradas.
  - Recursos Humanos ofrece servicios para validar vendedores.
- No se reorganizan las clases existentes de `config`, `security`, `exception` ni el endpoint Hola mundo (`SaludoController`, `SaludoService`, `SaludoServiceImpl`, `SaludoResponse`).

## Capa service (reglas aprobadas)
- Interfaces en `service.{area}` e implementaciones en `service.impl.{area}`: `@Service`, constructor, `@Transactional(readOnly = true)` a nivel de clase y `@Transactional` en operaciones de escritura. Sin código HTTP ni DTO.
- Errores de negocio con las excepciones de `exception` (`RecursoNoEncontradoException`, `VendedorNoAutorizadoException`, `InventarioInsuficienteException`, `OperacionNoPermitidaException`, `DatosInvalidosException`).
- Dependencias sin ciclos: Ventas → Clientes, RRHH, Producto, Inventario; Entregas → Ventas, Inventario; Compras → Proveedor, Producto, Inventario. `InventarioService` usa `BodegaRepository` (no `BodegaService`, que depende de él).
- **`InventarioService` es el único que modifica stock y crea `MovimientoStock`**: entrada solo con compra persistida, salida solo con entrega persistida; el registro producto+bodega debe existir; bloqueo pesimista (`findConBloqueo…`).
- Venta: nace `REGISTRADA`, precio = precio actual del producto, valida vendedor autorizado (último registro RRHH = `AUTORIZADA`) y stock total en todas las bodegas; **no descuenta inventario**. Se cancela solo si está `REGISTRADA` y su entrega no está `ENTREGADA`.
- Entrega: `registrarEntrega` crea `PENDIENTE` sin mover stock; `confirmarEntrega(entrega, bodega)` bloquea la entrega, descuenta de esa bodega y pasa a `ENTREGADA`.
- Compra: `registrarCompra` crea `PENDIENTE` (productos suministrados por ese proveedor, precio = costo indicado); `recibirCompra(compra, bodega)` ingresa stock y pasa a `RECIBIDA`; se cancela solo si está `PENDIENTE`.
- RRHH: autorizar o revocar agrega un registro al historial; se rechaza autorizar a un autorizado o revocar a quien no lo está.
- Los servicios devuelven entidades con relaciones LAZY (`open-in-view=false`): los controladores deben mapear a DTO dentro de la transacción del servicio.

## Capa controller (reglas aprobadas)
- Controladores en `controller.{area}` y DTO en `dto.{area}` (`record` con validación Jakarta y método estático `desde(entidad)`); `controller.autenticacion` y `dto.autenticacion` contienen login/refresh/logout y gestión de usuarios.
- `@RestController`, `@RequestMapping("/api/v1/...")`, `ResponseEntity`, constructor (`@RequiredArgsConstructor`), `@Valid` en los cuerpos. Solo dependen de **interfaces de servicio**; sin repositorios ni reglas de negocio.
- Nunca devolver entidades. Para leer relaciones LAZY con `open-in-view=false`, el controlador usa `@Transactional(readOnly = true)` de clase y `@Transactional` en escrituras, y mapea a DTO dentro de esa transacción.
- `POST` de creación → `201` + `Location`; cambios de estado con `PATCH /{id}/{accion}` (`recepcion`, `cancelacion`, `confirmacion`, `revocacion`).
- El cliente no envía precios ni totales de venta. No hay ajustes manuales de inventario: entradas solo por `PATCH /compras/{id}/recepcion` y salidas solo por `PATCH /entregas/{id}/confirmacion`.
- Errores centralizados en `GlobalExceptionHandler` (ProblemDetail, `type = urn:polimarket:error:*`): 404 no encontrado; 400 validación (`errores`), `DatosInvalidosException` y filtros incompatibles; 409 vendedor no autorizado, inventario insuficiente, operación no permitida, integridad y bloqueo; 401 `no-autenticado` (token ausente/inválido) y `autenticacion-fallida` (login o refresh); 403 `acceso-denegado` (rol); 500 genérico sin trazas, SQL ni Hibernate. No se usa 422.
- Documentar todo endpoint nuevo en `docs/endpoints.md`.

## Reglas de código
- Inyección de dependencias **por constructor** (`@RequiredArgsConstructor` con campos `final`). No usar `@Autowired` en campos.
- Validar entradas con Jakarta Bean Validation en los DTO.
- Tipos del modelo UML: usar **`double`** para precios y totales, e **`int`** para identificadores y cantidades definidos en el modelo.
- Entidades JPA con Lombok: usar `@Getter`, `@Setter`, `@NoArgsConstructor` (y `@AllArgsConstructor`/`@Builder` solo cuando aporte). **No** usar `@Data`, `@EqualsAndHashCode` ni `@ToString` sobre entidades; excluir relaciones de `toString`, y definir `equals`/`hashCode` basados en el identificador si son necesarios. Relaciones `LAZY` por defecto.
- El esquema de base de datos lo gestiona **Flyway** (`src/main/resources/db/migration`, `V{n}__descripcion.sql`); Hibernate solo valida (`spring.jpa.hibernate.ddl-auto=validate`). Nunca modificar una migración ya aplicada: crear una nueva.
- Registrar logs con SLF4J (`@Slf4j`). No registrar credenciales ni datos sensibles.
- Configuración en archivos `application*.properties` (no YAML).
- Credenciales siempre mediante variables de entorno o `.env` local (no versionado).

## Seguridad
- Una sola cadena `SecurityConfig` para todos los perfiles: OAuth2 Resource Server con JWT **RS256** (llaves RSA ≥ 2048 bits vía `JWT_PRIVATE_KEY_LOCATION`/`JWT_PUBLIC_KEY_LOCATION`; llaves efímeras solo en `dev`/`test`), STATELESS, CSRF/basic/form deshabilitados, errores 401/403 en ProblemDetail.
- Públicos: `GET /api/v1/hola`, `GET /actuator/health`, `/error` y `POST /api/v1/auth/{login,refresh,logout}`. Roles `ADMIN` y `VENDEDOR`; lo no listado se deniega (`denyAll`). Toda ruta nueva debe agregarse explícitamente a la matriz de `SecurityConfig` y a `SecurityMatrixWebMvcTest`.
- Access token de 15 min; refresh token opaco de 7 días guardado solo como hash SHA-256, con rotación y detección de reutilización (revoca la familia). Contraseñas con BCrypt.
- La autenticación (login, emisión y validación de JWT) es un componente independiente de la autorización adicional de vendedores realizada por Recursos Humanos.
- No configurar usuarios o contraseñas predeterminados: el primer `ADMIN` se crea desde `ADMIN_USERNAME`/`ADMIN_PASSWORD` solo si no existe ninguno activo.

## Pruebas
- Pruebas unitarias con JUnit Jupiter y Mockito (`@ExtendWith(MockitoExtension.class)`), y `@WebMvcTest` + `@MockitoBean` para controladores.
- Las pruebas unitarias no deben requerir PostgreSQL (perfil `test`).
- Las pruebas que requieren Docker/PostgreSQL se etiquetan con `@Tag("integration")` y se ejecutan con `.\gradlew.bat integrationTest`.
- Repositorios: extender `AbstractRepositoryIT` (`@DataJpaTest` + PostgreSQL 18.7 efímero en **Testcontainers** con `@ServiceConnection`, esquema creado por las migraciones Flyway con `ddl-auto=validate` y rollback por prueba). Nunca usar la base de desarrollo `polimarket_db` para pruebas, ni Mockito para simular consultas JPA.
- Servicios: pruebas unitarias Mockito en `src/test/java/.../service/impl/{area}`; flujos completos en `ServiceLayerIT` (`@SpringBootTest` + Testcontainers, sin transacción de prueba, limpieza con `TRUNCATE … CASCADE`).
- Controladores: `@WebMvcTest(XController.class)` + `@Import(SecurityConfig.class)` + `@ActiveProfiles("test")` + `@WithMockUser(roles = "ADMIN")` + `@MockitoBean` del servicio, en `src/test/java/.../controller/{area}` (entidades de prueba en `ControllerTestFixtures`). Flujos HTTP reales en `ControllerHttpIT` (Testcontainers, verificando lo persistido).
- `PolimarketApiApplicationTests` reemplaza los repositorios con `@MockitoBean(types = …)`; al crear un repositorio nuevo, agregarlo a esa lista.

## Principios
- Evitar duplicar funcionalidades entre componentes; reutilizar servicios existentes.
- No modificar la arquitectura, paquetes ni dependencias sin revisar antes su impacto en los demás componentes.
- No implementar funcionalidades de negocio que no estén definidas en el UML o solicitadas explícitamente.