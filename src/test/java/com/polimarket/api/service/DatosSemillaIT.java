package com.polimarket.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.polimarket.api.entity.Entrega;
import com.polimarket.api.entity.Venta;
import com.polimarket.api.exception.OperacionNoPermitidaException;
import com.polimarket.api.exception.VendedorNoAutorizadoException;
import com.polimarket.api.repository.PostgresTestcontainersConfiguration;
import com.polimarket.api.service.bodega.InventarioService;
import com.polimarket.api.service.entregas.EntregaService;
import com.polimarket.api.service.rrhh.AutorizacionRRHHService;
import com.polimarket.api.service.ventas.VentaService;
import com.polimarket.api.service.ventas.VentaService.ItemVenta;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Verifica los datos semilla del perfil dev (db/seed) sobre un PostgreSQL efimero: que Flyway los aplica
 * despues de las migraciones, que Hibernate valida el esquema y que el historial es coherente con las
 * reglas de negocio. Usa su propio contenedor, por lo que no afecta a las demas pruebas.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.flyway.locations=classpath:db/migration,classpath:db/seed",
        "spring.jpa.hibernate.ddl-auto=validate",
        "polimarket.jwt.ephemeral-keys-allowed=true"})
@Import(PostgresTestcontainersConfiguration.class)
@DirtiesContext
@Tag("integration")
class DatosSemillaIT {

    @Autowired private AutorizacionRRHHService autorizacionService;
    @Autowired private InventarioService inventarioService;
    @Autowired private VentaService ventaService;
    @Autowired private EntregaService entregaService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;

    @Test
    void cargaElHistorialCoherenteConLasReglasDeNegocio() {
        assertEquals(4, contar("vendedor"));
        assertEquals(5, contar("cliente"));
        assertEquals(8, contar("producto"));
        assertEquals(10, contar("inventario"));
        assertEquals(0, contar("usuario"));

        assertTrue(autorizacionService.validarVendedor(1));
        assertTrue(autorizacionService.validarVendedor(2));
        assertFalse(autorizacionService.validarVendedor(3));
        assertFalse(autorizacionService.validarVendedor(4));

        assertEquals(List.of(3, 6, 8), inventarioService.consultarInventariosQueRequierenReposicion().stream()
                .map(inventario -> inventario.getId()).sorted().toList());
        assertEquals(165, inventarioService.consultarStockTotal(1));

        Integer totalesIncorrectos = jdbc.queryForObject("""
                SELECT count(*) FROM venta v
                WHERE v.total <> (SELECT sum(d.cantidad * d.precio) FROM detalle_venta d WHERE d.venta_id = v.id)
                """, Integer.class);
        assertEquals(0, totalesIncorrectos);
    }

    @Test
    void permiteOperarSobreLasSemillasYLasSecuenciasContinuan() {
        Venta venta = ventaService.registrarVenta(1, 3, List.of(new ItemVenta(1, 2)));
        assertEquals(6, venta.getId());
        assertEquals(7000.0, venta.getTotal(), 0.001);

        assertThrows(VendedorNoAutorizadoException.class,
                () -> ventaService.registrarVenta(3, 1, List.of(new ItemVenta(1, 1))));
        assertThrows(OperacionNoPermitidaException.class, () -> entregaService.confirmarEntrega(1, 1));

        Entrega entrega = entregaService.confirmarEntrega(2, 1);
        assertEquals("ENTREGADA", entrega.getEstado());
        assertEquals(64, inventarioService.consultarStock(5, 1));
        assertEquals(6, inventarioService.consultarStock(3, 1));
    }

    @Test
    void esIdempotente() {
        int vendedores = contar("vendedor");
        int movimientos = contar("movimiento_stock");

        new ResourceDatabasePopulator(new ClassPathResource("db/seed/R__datos_semilla.sql")).execute(dataSource);

        assertEquals(vendedores, contar("vendedor"));
        assertEquals(movimientos, contar("movimiento_stock"));
    }

    private int contar(String tabla) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabla, Integer.class);
    }
}
