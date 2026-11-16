package com.polimarket.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.polimarket.api.entity.Bodega;
import com.polimarket.api.entity.Cliente;
import com.polimarket.api.entity.Entrega;
import com.polimarket.api.entity.Inventario;
import com.polimarket.api.entity.Producto;
import com.polimarket.api.entity.Proveedor;
import com.polimarket.api.entity.Vendedor;
import com.polimarket.api.exception.InventarioInsuficienteException;
import com.polimarket.api.exception.OperacionNoPermitidaException;
import com.polimarket.api.exception.VendedorNoAutorizadoException;
import com.polimarket.api.repository.PostgresTestcontainersConfiguration;
import com.polimarket.api.repository.bodega.BodegaRepository;
import com.polimarket.api.repository.bodega.InventarioRepository;
import com.polimarket.api.repository.bodega.ProductoRepository;
import com.polimarket.api.repository.clientes.ClienteRepository;
import com.polimarket.api.repository.proveedores.ProveedorRepository;
import com.polimarket.api.repository.rrhh.VendedorRepository;
import com.polimarket.api.service.bodega.InventarioService;
import com.polimarket.api.service.entregas.EntregaService;
import com.polimarket.api.service.proveedores.CompraProveedorService;
import com.polimarket.api.service.proveedores.CompraProveedorService.ItemCompra;
import com.polimarket.api.service.rrhh.AutorizacionRRHHService;
import com.polimarket.api.service.ventas.VentaService;
import com.polimarket.api.service.ventas.VentaService.ItemVenta;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Valida los flujos de negocio de la capa service contra PostgreSQL real (Testcontainers).
 * Sin transaccion de prueba: cada servicio confirma o revierte su propia transaccion, igual que en ejecucion.
 * Nunca toca la base de desarrollo polimarket_db.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.jpa.hibernate.ddl-auto=validate", "polimarket.jwt.ephemeral-keys-allowed=true"})
@Import(PostgresTestcontainersConfiguration.class)
@Tag("integration")
class ServiceLayerIT {

    @Autowired private VentaService ventaService;
    @Autowired private EntregaService entregaService;
    @Autowired private CompraProveedorService compraService;
    @Autowired private InventarioService inventarioService;
    @Autowired private AutorizacionRRHHService autorizacionService;

    @Autowired private VendedorRepository vendedorRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ProveedorRepository proveedorRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private BodegaRepository bodegaRepository;
    @Autowired private InventarioRepository inventarioRepository;
    @Autowired private JdbcTemplate jdbc;

    private Integer vendedor;
    private Integer cliente;
    private Integer arroz;
    private Integer sal;
    private Integer bodega;

    @BeforeEach
    void preparar() {
        vendedor = vendedorRepository.save(Vendedor.builder().nombre("Luis").build()).getId();
        cliente = clienteRepository.save(Cliente.builder().nombre("Ana").direccion("Calle 1").build()).getId();
        Proveedor proveedor = proveedorRepository.save(
                Proveedor.builder().nit("900").nombre("Agro").contacto("agro@co").build());
        arroz = producto("Arroz", 3000, proveedor);
        sal = producto("Sal", 900, proveedor);
        Bodega principal = bodegaRepository.save(Bodega.builder().ubicacion("Centro").build());
        bodega = principal.getId();
        inventario(principal, arroz, 10);
        inventario(principal, sal, 1);
        autorizacionService.autorizarVendedor(vendedor);
    }

    @AfterEach
    void limpiar() {
        jdbc.execute("TRUNCATE vendedor, cliente, proveedor, producto, bodega RESTART IDENTITY CASCADE");
    }

    private Integer producto(String nombre, double precio, Proveedor proveedor) {
        Producto producto = Producto.builder().nombre(nombre).precio(precio).build();
        producto.agregarProveedor(proveedor);
        return productoRepository.save(producto).getId();
    }

    private void inventario(Bodega bodega, Integer idProducto, int stock) {
        inventarioRepository.save(Inventario.builder().bodega(bodega)
                .producto(productoRepository.getReferenceById(idProducto)).stock(stock).stockMinimo(2).build());
    }

    private int movimientos() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM movimiento_stock", Integer.class);
    }

    private Integer entregaPendiente(List<ItemVenta> items) {
        Integer venta = ventaService.registrarVenta(vendedor, cliente, items).getId();
        return entregaService.registrarEntrega(venta, "Carrera 7").getId();
    }

    @Test
    void ventaEntregaYConfirmacionDescuentanInventarioUnaSolaVez() {
        Integer entrega = entregaPendiente(List.of(new ItemVenta(arroz, 3), new ItemVenta(arroz, 1)));
        assertEquals(10, inventarioService.consultarStock(arroz, bodega), "registrar venta no descuenta");

        entregaService.confirmarEntrega(entrega, bodega);

        assertEquals(6, inventarioService.consultarStock(arroz, bodega));
        assertEquals(1, movimientos());
        assertEquals(Entrega.ESTADO_ENTREGADA, entregaService.consultarEntrega(entrega).getEstado());
        assertThrows(OperacionNoPermitidaException.class, () -> entregaService.confirmarEntrega(entrega, bodega));
        assertEquals(6, inventarioService.consultarStock(arroz, bodega));
        assertEquals(1, movimientos());
    }

    @Test
    void confirmacionConUnProductoInsuficienteRevierteTodo() {
        Integer entrega = entregaPendiente(List.of(new ItemVenta(arroz, 2), new ItemVenta(sal, 1)));
        jdbc.update("UPDATE inventario SET stock = 0 WHERE producto_id = ?", sal);

        assertThrows(InventarioInsuficienteException.class, () -> entregaService.confirmarEntrega(entrega, bodega));

        assertEquals(10, inventarioService.consultarStock(arroz, bodega));
        assertEquals(0, movimientos());
        assertEquals(EntregaService.ESTADO_PENDIENTE, entregaService.consultarEntrega(entrega).getEstado());
    }

    @Test
    void confirmacionesConcurrentesSoloDescuentanUnaVez() throws Exception {
        Integer entrega = entregaPendiente(List.of(new ItemVenta(arroz, 4)));
        CountDownLatch salida = new CountDownLatch(1);
        Callable<Boolean> confirmar = () -> {
            salida.await();
            try {
                entregaService.confirmarEntrega(entrega, bodega);
                return true;
            } catch (OperacionNoPermitidaException e) {
                return false;
            }
        };
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        List<Future<Boolean>> resultados = new ArrayList<>();
        try {
            resultados.add(hilos.submit(confirmar));
            resultados.add(hilos.submit(confirmar));
            salida.countDown();
            int exitosas = 0;
            for (Future<Boolean> resultado : resultados) {
                exitosas += obtener(resultado) ? 1 : 0;
            }
            assertEquals(1, exitosas);
        } finally {
            hilos.shutdownNow();
        }
        assertEquals(6, inventarioService.consultarStock(arroz, bodega));
        assertEquals(1, movimientos());
    }

    private static boolean obtener(Future<Boolean> resultado) throws InterruptedException {
        try {
            return resultado.get();
        } catch (ExecutionException e) {
            throw new AssertionError("Fallo inesperado en confirmacion concurrente", e.getCause());
        }
    }

    @Test
    void vendedorRevocadoNoPuedeVender() {
        autorizacionService.revocarAutorizacion(vendedor);

        assertFalse(autorizacionService.validarVendedor(vendedor));
        assertThrows(VendedorNoAutorizadoException.class,
                () -> ventaService.registrarVenta(vendedor, cliente, List.of(new ItemVenta(arroz, 1))));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM venta", Integer.class));
    }

    @Test
    void recibirCompraIncrementaInventarioUnaSolaVez() {
        Integer compra = compraService.registrarCompra("900",
                List.of(new ItemCompra(arroz, 5, 2000), new ItemCompra(sal, 4, 500))).getId();
        assertEquals(10, inventarioService.consultarStock(arroz, bodega), "registrar compra no ingresa stock");
        assertEquals(12000.0, compraService.calcularTotal(compra));

        compraService.recibirCompra(compra, bodega);

        assertEquals(15, inventarioService.consultarStock(arroz, bodega));
        assertEquals(5, inventarioService.consultarStock(sal, bodega));
        assertEquals(2, movimientos());
        assertTrue(compraService.consultarComprasPendientes().isEmpty());
        assertThrows(OperacionNoPermitidaException.class, () -> compraService.recibirCompra(compra, bodega));
        assertEquals(15, inventarioService.consultarStock(arroz, bodega));
    }
}
