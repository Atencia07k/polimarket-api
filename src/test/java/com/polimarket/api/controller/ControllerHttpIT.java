package com.polimarket.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jayway.jsonpath.JsonPath;
import com.polimarket.api.entity.Bodega;
import com.polimarket.api.entity.Cliente;
import com.polimarket.api.entity.Inventario;
import com.polimarket.api.entity.Producto;
import com.polimarket.api.entity.Proveedor;
import com.polimarket.api.entity.Rol;
import com.polimarket.api.entity.Vendedor;
import com.polimarket.api.repository.PostgresTestcontainersConfiguration;
import com.polimarket.api.repository.bodega.BodegaRepository;
import com.polimarket.api.repository.bodega.InventarioRepository;
import com.polimarket.api.repository.bodega.ProductoRepository;
import com.polimarket.api.repository.clientes.ClienteRepository;
import com.polimarket.api.repository.proveedores.ProveedorRepository;
import com.polimarket.api.repository.rrhh.VendedorRepository;
import com.polimarket.api.service.autenticacion.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

/**
 * Flujos completos por HTTP real (Tomcat en puerto aleatorio) contra PostgreSQL efimero en Testcontainers.
 * El esquema lo crean las migraciones Flyway y cada peticion lleva un access token obtenido con un login real.
 * Verifica que lo persistido coincide con lo respondido. Nunca toca la base de desarrollo polimarket_db.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.autoconfigure.exclude="
})
@Import(PostgresTestcontainersConfiguration.class)
@ActiveProfiles("test")
@Tag("integration")
class ControllerHttpIT {

    @Value("${local.server.port}")
    private int puerto;

    @Autowired private VendedorRepository vendedorRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ProveedorRepository proveedorRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private BodegaRepository bodegaRepository;
    @Autowired private InventarioRepository inventarioRepository;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UsuarioService usuarioService;

    private static final String CLAVE = "Clave.Segura1";

    private RestClient http;
    private String tokenAdmin;
    private Integer vendedor;
    private Integer vendedorSinAutorizar;
    private Integer cliente;
    private Integer arroz;
    private Integer bodega;

    @BeforeEach
    void preparar() {
        http = RestClient.builder()
                .baseUrl("http://localhost:" + puerto + "/api/v1")
                .defaultStatusHandler(estado -> true, (solicitud, respuesta) -> { })
                .build();
        vendedor = vendedorRepository.save(Vendedor.builder().nombre("Luis").build()).getId();
        vendedorSinAutorizar = vendedorRepository.save(Vendedor.builder().nombre("Pedro").build()).getId();
        cliente = clienteRepository.save(Cliente.builder().nombre("Ana").direccion("Calle 1").build()).getId();
        Proveedor proveedor = proveedorRepository.save(
                Proveedor.builder().nit("900").nombre("Agro").contacto("agro@co").build());
        Producto producto = Producto.builder().nombre("Arroz").precio(3000).build();
        producto.agregarProveedor(proveedor);
        arroz = productoRepository.save(producto).getId();
        Bodega principal = bodegaRepository.save(Bodega.builder().ubicacion("Centro").build());
        bodega = principal.getId();
        inventarioRepository.save(Inventario.builder().bodega(principal)
                .producto(productoRepository.getReferenceById(arroz)).stock(10).stockMinimo(2).build());
        usuarioService.crearUsuario("admin", CLAVE, Rol.ADMIN);
        usuarioService.crearUsuario("vendedor1", CLAVE, Rol.VENDEDOR);
        tokenAdmin = leer(login("admin", CLAVE), "$.accessToken");
    }

    @AfterEach
    void limpiar() {
        jdbc.execute("TRUNCATE vendedor, cliente, proveedor, producto, bodega, usuario, refresh_token "
                + "RESTART IDENTITY CASCADE");
    }

    private ResponseEntity<String> login(String username, String password) {
        ResponseEntity<String> respuesta = enviarCon(null, HttpMethod.POST, "/auth/login",
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
        assertEquals(200, respuesta.getStatusCode().value(), respuesta.getBody());
        return respuesta;
    }

    private ResponseEntity<String> enviar(HttpMethod metodo, String ruta, String json) {
        return enviarCon(tokenAdmin, metodo, ruta, json);
    }

    private ResponseEntity<String> enviarCon(String token, HttpMethod metodo, String ruta, String json) {
        RestClient.RequestBodySpec solicitud = http.method(metodo).uri(ruta).accept(MediaType.APPLICATION_JSON);
        if (token != null) {
            solicitud.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (json != null) {
            solicitud.contentType(MediaType.APPLICATION_JSON).body(json);
        }
        return solicitud.retrieve().toEntity(String.class);
    }

    private static <T> T leer(ResponseEntity<String> respuesta, String ruta) {
        return JsonPath.read(respuesta.getBody(), ruta);
    }

    private int stockArroz() {
        return jdbc.queryForObject("SELECT stock FROM inventario WHERE producto_id = ?", Integer.class, arroz);
    }

    private int contar(String tabla) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabla, Integer.class);
    }

    private void autorizar(Integer idVendedor) {
        ResponseEntity<String> respuesta = enviar(HttpMethod.POST, "/autorizaciones",
                "{\"idVendedor\":" + idVendedor + "}");
        assertEquals(201, respuesta.getStatusCode().value());
    }

    private String ventaJson(Integer idVendedor, int cantidad) {
        return "{\"idVendedor\":" + idVendedor + ",\"idCliente\":" + cliente
                + ",\"items\":[{\"idProducto\":" + arroz + ",\"cantidad\":" + cantidad + "}]}";
    }

    @Test
    void autorizacionPorHttpSePersisteYSeConsulta() {
        autorizar(vendedor);

        assertEquals("AUTORIZADA", jdbc.queryForObject(
                "SELECT estado FROM autorizacion_rrhh WHERE vendedor_id = ?", String.class, vendedor));
        ResponseEntity<String> estado = enviar(HttpMethod.GET, "/autorizaciones/vendedores/" + vendedor, null);
        assertEquals(200, estado.getStatusCode().value());
        assertTrue((Boolean) leer(estado, "$.autorizado"));

        assertEquals(409, enviar(HttpMethod.POST, "/autorizaciones", "{\"idVendedor\":" + vendedor + "}")
                .getStatusCode().value(), "autorizar dos veces es un conflicto");
    }

    @Test
    void ventaEntregaYConfirmacionPorHttpDescuentanStockUnaSolaVez() {
        autorizar(vendedor);

        ResponseEntity<String> venta = enviar(HttpMethod.POST, "/ventas", ventaJson(vendedor, 3));
        assertEquals(201, venta.getStatusCode().value());
        Integer idVenta = leer(venta, "$.id");
        assertNotNull(venta.getHeaders().getLocation());
        assertTrue(venta.getHeaders().getLocation().getPath().endsWith("/api/v1/ventas/" + idVenta));
        assertEquals(9000.0, ((Number) leer(venta, "$.total")).doubleValue());
        assertEquals("REGISTRADA", jdbc.queryForObject("SELECT estado FROM venta WHERE id = ?", String.class, idVenta));
        assertEquals(9000.0, jdbc.queryForObject("SELECT total FROM venta WHERE id = ?", Double.class, idVenta));
        assertEquals(10, stockArroz(), "registrar la venta no descuenta stock");

        ResponseEntity<String> entrega = enviar(HttpMethod.POST, "/entregas",
                "{\"idVenta\":" + idVenta + ",\"direccion\":\"Carrera 7\"}");
        assertEquals(201, entrega.getStatusCode().value());
        Integer idEntrega = leer(entrega, "$.id");
        assertEquals("PENDIENTE", leer(entrega, "$.estado"));

        ResponseEntity<String> confirmada = enviar(HttpMethod.PATCH, "/entregas/" + idEntrega + "/confirmacion",
                "{\"idBodega\":" + bodega + "}");
        assertEquals(200, confirmada.getStatusCode().value());
        assertEquals("ENTREGADA", leer(confirmada, "$.estado"));
        assertEquals("ENTREGADA", jdbc.queryForObject("SELECT estado FROM entrega WHERE id = ?", String.class, idEntrega));
        assertEquals(7, stockArroz());
        assertEquals(1, contar("movimiento_stock"));
        assertEquals("SALIDA", jdbc.queryForObject("SELECT tipo FROM movimiento_stock WHERE entrega_id = ?",
                String.class, idEntrega));

        ResponseEntity<String> repetida = enviar(HttpMethod.PATCH, "/entregas/" + idEntrega + "/confirmacion",
                "{\"idBodega\":" + bodega + "}");
        assertEquals(409, repetida.getStatusCode().value());
        assertEquals(7, stockArroz(), "la segunda confirmacion no descuenta de nuevo");
        assertEquals(1, contar("movimiento_stock"));

        ResponseEntity<String> consultada = enviar(HttpMethod.GET, "/ventas/" + idVenta, null);
        assertEquals(idEntrega, leer(consultada, "$.idEntrega"));
    }

    @Test
    void vendedorSinAutorizacionDeRrhhNoPuedeVender() {
        ResponseEntity<String> respuesta = enviar(HttpMethod.POST, "/ventas", ventaJson(vendedorSinAutorizar, 1));

        assertEquals(409, respuesta.getStatusCode().value());
        assertEquals("urn:polimarket:error:vendedor-no-autorizado", leer(respuesta, "$.type"));
        assertEquals(0, contar("venta"));
    }

    @Test
    void ventaSinExistenciasSuficientesEsRechazada() {
        autorizar(vendedor);

        ResponseEntity<String> respuesta = enviar(HttpMethod.POST, "/ventas", ventaJson(vendedor, 11));

        assertEquals(409, respuesta.getStatusCode().value());
        assertEquals("urn:polimarket:error:inventario-insuficiente", leer(respuesta, "$.type"));
        assertEquals(0, contar("venta"));
        assertEquals(10, stockArroz());
    }

    @Test
    void compraRecibidaPorHttpIncrementaStock() {
        ResponseEntity<String> compra = enviar(HttpMethod.POST, "/compras", "{\"nitProveedor\":\"900\",\"items\":"
                + "[{\"idProducto\":" + arroz + ",\"cantidad\":5,\"precio\":2500.0}]}");
        assertEquals(201, compra.getStatusCode().value());
        Integer idCompra = leer(compra, "$.id");
        assertEquals("PENDIENTE", leer(compra, "$.estado"));
        assertEquals(12500.0, ((Number) leer(compra, "$.total")).doubleValue());
        assertEquals(10, stockArroz(), "registrar la compra no mueve inventario");

        ResponseEntity<String> recibida = enviar(HttpMethod.PATCH, "/compras/" + idCompra + "/recepcion",
                "{\"idBodega\":" + bodega + "}");
        assertEquals(200, recibida.getStatusCode().value());
        assertEquals("RECIBIDA", jdbc.queryForObject("SELECT estado FROM compra_proveedor WHERE id = ?",
                String.class, idCompra));
        assertEquals(15, stockArroz());
        assertEquals("ENTRADA", jdbc.queryForObject("SELECT tipo FROM movimiento_stock WHERE compra_proveedor_id = ?",
                String.class, idCompra));

        assertEquals(409, enviar(HttpMethod.PATCH, "/compras/" + idCompra + "/recepcion",
                "{\"idBodega\":" + bodega + "}").getStatusCode().value());
        assertEquals(15, stockArroz());
    }

    @Test
    void actualizarClientePorHttpSePersiste() {
        ResponseEntity<String> respuesta = enviar(HttpMethod.PUT, "/clientes/" + cliente,
                "{\"nombre\":\"Ana Maria\",\"direccion\":\"Calle 99\"}");

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals("Ana Maria", leer(respuesta, "$.nombre"));
        assertEquals("Ana Maria", jdbc.queryForObject("SELECT nombre FROM cliente WHERE id = ?", String.class, cliente));
        assertEquals("Calle 99", jdbc.queryForObject("SELECT direccion FROM cliente WHERE id = ?", String.class, cliente));
    }

    @Test
    void erroresHttpUsanProblemDetailSinDetallesInternos() {
        ResponseEntity<String> noExiste = enviar(HttpMethod.GET, "/ventas/999", null);
        assertEquals(404, noExiste.getStatusCode().value());
        assertEquals("urn:polimarket:error:recurso-no-encontrado", leer(noExiste, "$.type"));

        ResponseEntity<String> invalida = enviar(HttpMethod.POST, "/ventas", "{\"items\":[]}");
        assertEquals(400, invalida.getStatusCode().value());
        assertNotNull(leer(invalida, "$.errores.idVendedor"));

        ResponseEntity<String> tipoInvalido = enviar(HttpMethod.GET, "/clientes/abc", null);
        assertEquals(400, tipoInvalido.getStatusCode().value());

        for (ResponseEntity<String> respuesta : new ResponseEntity[] {noExiste, invalida, tipoInvalido}) {
            String cuerpo = respuesta.getBody();
            assertFalse(cuerpo.contains("trace") || cuerpo.contains("Exception") || cuerpo.contains("SQL")
                    || cuerpo.contains("hibernate"), cuerpo);
        }
    }

    @Test
    void consultasAutenticadasDevuelvenDatosPersistidos() {
        ResponseEntity<String> productos = enviar(HttpMethod.GET, "/productos", null);
        assertEquals(200, productos.getStatusCode().value());
        assertEquals("900", leer(productos, "$[0].nitsProveedores[0]"));

        ResponseEntity<String> stock = enviar(HttpMethod.GET,
                "/inventarios/stock?productoId=" + arroz + "&bodegaId=" + bodega, null);
        assertEquals(10, (Integer) leer(stock, "$.stock"));
        assertFalse((Boolean) leer(stock, "$.requiereReposicion"));

        assertEquals(200, enviar(HttpMethod.GET, "/proveedores/900", null).getStatusCode().value());
        assertEquals(200, enviar(HttpMethod.GET, "/bodegas/" + bodega + "/inventarios", null).getStatusCode().value());
    }

    @Test
    void flujoCompletoDeAutenticacionConRotacionYDeteccionDeReutilizacion() {
        assertEquals(401, enviarCon(null, HttpMethod.GET, "/productos", null).getStatusCode().value());
        assertEquals(401, enviarCon("token-falso", HttpMethod.GET, "/productos", null).getStatusCode().value());
        assertEquals(401, enviarCon(null, HttpMethod.POST, "/auth/login",
                "{\"username\":\"admin\",\"password\":\"incorrecta\"}").getStatusCode().value());

        ResponseEntity<String> sesion = login("Vendedor1", CLAVE);
        String access = leer(sesion, "$.accessToken");
        String refresh1 = leer(sesion, "$.refreshToken");
        assertEquals("Bearer", leer(sesion, "$.tokenType"));
        assertEquals(900, (Integer) leer(sesion, "$.expiresIn"));
        assertFalse(contarRefreshConValor(refresh1), "el refresh token se guarda solo como hash");

        ResponseEntity<String> yo = enviarCon(access, HttpMethod.GET, "/auth/me", null);
        assertEquals(200, yo.getStatusCode().value());
        assertEquals("vendedor1", leer(yo, "$.username"));
        assertEquals("VENDEDOR", leer(yo, "$.rol"));

        ResponseEntity<String> rotado = enviarCon(null, HttpMethod.POST, "/auth/refresh",
                "{\"refreshToken\":\"" + refresh1 + "\"}");
        assertEquals(200, rotado.getStatusCode().value());
        String refresh2 = leer(rotado, "$.refreshToken");
        assertEquals(200, enviarCon((String) leer(rotado, "$.accessToken"), HttpMethod.GET, "/productos", null)
                .getStatusCode().value());

        // Reutilizar refresh1 (ya rotado) se trata como robo: se revoca toda la familia, incluido refresh2
        assertEquals(401, enviarCon(null, HttpMethod.POST, "/auth/refresh",
                "{\"refreshToken\":\"" + refresh1 + "\"}").getStatusCode().value());
        assertEquals(401, enviarCon(null, HttpMethod.POST, "/auth/refresh",
                "{\"refreshToken\":\"" + refresh2 + "\"}").getStatusCode().value());

        String refresh3 = leer(login("vendedor1", CLAVE), "$.refreshToken");
        assertEquals(204, enviarCon(null, HttpMethod.POST, "/auth/logout",
                "{\"refreshToken\":\"" + refresh3 + "\"}").getStatusCode().value());
        assertEquals(401, enviarCon(null, HttpMethod.POST, "/auth/refresh",
                "{\"refreshToken\":\"" + refresh3 + "\"}").getStatusCode().value());
    }

    @Test
    void vendedorSoloAccedeASusOperacionesYAdminGestionaUsuarios() {
        String tokenVendedor = leer(login("vendedor1", CLAVE), "$.accessToken");

        assertEquals(200, enviarCon(tokenVendedor, HttpMethod.GET, "/productos", null).getStatusCode().value());
        ResponseEntity<String> prohibido = enviarCon(tokenVendedor, HttpMethod.POST, "/clientes",
                "{\"nombre\":\"Eva\",\"direccion\":\"Calle 2\"}");
        assertEquals(403, prohibido.getStatusCode().value());
        assertEquals("urn:polimarket:error:acceso-denegado", leer(prohibido, "$.type"));
        assertEquals(403, enviarCon(tokenVendedor, HttpMethod.GET, "/usuarios", null).getStatusCode().value());

        ResponseEntity<String> creado = enviar(HttpMethod.POST, "/usuarios",
                "{\"username\":\"vendedor2\",\"password\":\"" + CLAVE + "\",\"rol\":\"VENDEDOR\"}");
        assertEquals(201, creado.getStatusCode().value());
        assertFalse(creado.getBody().contains("password"), creado.getBody());
        assertTrue(jdbc.queryForObject("SELECT password_hash FROM usuario WHERE username = 'vendedor2'",
                String.class).startsWith("{bcrypt}"));
        assertEquals(409, enviar(HttpMethod.POST, "/usuarios",
                "{\"username\":\"VENDEDOR2\",\"password\":\"" + CLAVE + "\",\"rol\":\"VENDEDOR\"}")
                .getStatusCode().value());

        String refreshVendedor = leer(login("vendedor1", CLAVE), "$.refreshToken");
        Integer idVendedor1 = jdbc.queryForObject("SELECT id FROM usuario WHERE username = 'vendedor1'", Integer.class);
        assertEquals(200, enviar(HttpMethod.PATCH, "/usuarios/" + idVendedor1 + "/desactivacion", null)
                .getStatusCode().value());
        assertEquals(401, enviarCon(null, HttpMethod.POST, "/auth/login",
                "{\"username\":\"vendedor1\",\"password\":\"" + CLAVE + "\"}").getStatusCode().value());
        assertEquals(401, enviarCon(null, HttpMethod.POST, "/auth/refresh",
                "{\"refreshToken\":\"" + refreshVendedor + "\"}").getStatusCode().value());
    }

    private boolean contarRefreshConValor(String valor) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM refresh_token WHERE token_hash = ?", Integer.class, valor) > 0;
    }
}