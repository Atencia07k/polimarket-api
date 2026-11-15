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
import com.polimarket.api.entity.Vendedor;
import com.polimarket.api.repository.PostgresTestcontainersConfiguration;
import com.polimarket.api.repository.bodega.BodegaRepository;
import com.polimarket.api.repository.bodega.InventarioRepository;
import com.polimarket.api.repository.bodega.ProductoRepository;
import com.polimarket.api.repository.clientes.ClienteRepository;
import com.polimarket.api.repository.proveedores.ProveedorRepository;
import com.polimarket.api.repository.rrhh.VendedorRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

/**
 * Flujos completos por HTTP real (Tomcat en puerto aleatorio) contra PostgreSQL efimero en Testcontainers.
 * Usa el perfil test (seguridad publica temporal) y verifica que lo persistido coincide con lo respondido.
 * Nunca toca la base de desarrollo polimarket_db.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
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

    private RestClient http;
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
    }

    @AfterEach
    void limpiar() {
        jdbc.execute("TRUNCATE vendedor, cliente, proveedor, producto, bodega RESTART IDENTITY CASCADE");
    }

    private ResponseEntity<String> enviar(HttpMethod metodo, String ruta, String json) {
        RestClient.RequestBodySpec solicitud = http.method(metodo).uri(ruta).accept(MediaType.APPLICATION_JSON);
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
    void consultasPublicasDevuelvenDatosPersistidos() {
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
}
