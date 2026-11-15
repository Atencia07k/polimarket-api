package com.polimarket.api.repository;

import com.polimarket.api.entity.AutorizacionRRHH;
import com.polimarket.api.entity.Bodega;
import com.polimarket.api.entity.Cliente;
import com.polimarket.api.entity.CompraProveedor;
import com.polimarket.api.entity.DetalleCompra;
import com.polimarket.api.entity.DetalleVenta;
import com.polimarket.api.entity.Entrega;
import com.polimarket.api.entity.Inventario;
import com.polimarket.api.entity.MovimientoStock;
import com.polimarket.api.entity.Producto;
import com.polimarket.api.entity.Proveedor;
import com.polimarket.api.entity.Vendedor;
import com.polimarket.api.entity.Venta;
import java.time.LocalDate;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * Base de las pruebas de repositorios: PostgreSQL real en Testcontainers, esquema creado
 * desde las entidades (create-drop) y rollback automatico al final de cada prueba.
 * Requiere Docker en ejecucion; por eso se etiqueta como integration.
 * <p>
 * Los ayudantes respetan las validaciones de las entidades: un producto tiene al menos
 * un proveedor y una venta o compra tiene al menos un detalle.
 */
@DataJpaTest(showSql = false, properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresTestcontainersConfiguration.class)
@Tag("integration")
public abstract class AbstractRepositoryIT {

    /** Valor usado solo como dato de contraste en pruebas; no es un estado de negocio. */
    protected static final String ESTADO_PRUEBA = "ESTADO_PRUEBA";

    protected static final LocalDate HOY = LocalDate.of(2026, 11, 14);

    @Autowired
    protected TestEntityManager em;

    /** Sincroniza con PostgreSQL y vacia el contexto para que las consultas lean de la base. */
    protected void flushAndClear() {
        em.flush();
        em.clear();
    }

    protected Vendedor vendedor(String nombre) {
        return em.persist(Vendedor.builder().nombre(nombre).build());
    }

    protected Cliente cliente(String nombre) {
        return em.persist(Cliente.builder().nombre(nombre).direccion("Calle 1 # 2-3").build());
    }

    protected Proveedor proveedor(String nit, String nombre) {
        return em.persist(Proveedor.builder().nit(nit).nombre(nombre).contacto("contacto@proveedor.co").build());
    }

    protected Producto producto(String nombre, Proveedor proveedor) {
        Producto producto = Producto.builder().nombre(nombre).precio(1000.0).build();
        producto.agregarProveedor(proveedor);
        return em.persist(producto);
    }

    protected Bodega bodega(String ubicacion) {
        return em.persist(Bodega.builder().ubicacion(ubicacion).build());
    }

    protected Inventario inventario(Bodega bodega, Producto producto, int stock, int stockMinimo) {
        return em.persist(Inventario.builder()
                .bodega(bodega).producto(producto).stock(stock).stockMinimo(stockMinimo).build());
    }

    /** Venta con un detalle de una unidad del producto indicado (persistido en cascada). */
    protected Venta venta(Vendedor vendedor, Cliente cliente, String estado, LocalDate fecha, Producto producto) {
        Venta venta = Venta.builder().vendedor(vendedor).cliente(cliente).estado(estado).fecha(fecha).build();
        venta.agregarDetalle(DetalleVenta.builder().producto(producto).cantidad(1).precio(producto.getPrecio()).build());
        venta.calcularTotal();
        return em.persist(venta);
    }

    protected Entrega entrega(Venta venta, String estado, LocalDate fecha) {
        return em.persist(Entrega.builder()
                .venta(venta).estado(estado).fecha(fecha).direccion("Carrera 7 # 8-9").build());
    }

    /** Compra con un detalle de una unidad del producto indicado (persistido en cascada). */
    protected CompraProveedor compra(Proveedor proveedor, String estado, LocalDate fecha, Producto producto) {
        CompraProveedor compra = CompraProveedor.builder().proveedor(proveedor).estado(estado).fecha(fecha).build();
        compra.agregarDetalle(DetalleCompra.builder().producto(producto).cantidad(1).precio(producto.getPrecio()).build());
        return em.persist(compra);
    }

    protected AutorizacionRRHH autorizacion(Vendedor vendedor, String estado, LocalDate fecha) {
        return em.persist(AutorizacionRRHH.builder().vendedor(vendedor).estado(estado).fecha(fecha).build());
    }

    protected MovimientoStock movimiento(Inventario inventario, String tipo, LocalDate fecha,
                                         Entrega entrega, CompraProveedor compra) {
        return em.persist(MovimientoStock.builder()
                .inventario(inventario).tipo(tipo).cantidad(1).fecha(fecha)
                .entrega(entrega).compra(compra).build());
    }
}
