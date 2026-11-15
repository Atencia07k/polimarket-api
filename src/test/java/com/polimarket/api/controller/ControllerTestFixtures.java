package com.polimarket.api.controller;

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

/** Entidades en memoria para las pruebas de controladores (sin base de datos). */
public final class ControllerTestFixtures {

    public static final LocalDate FECHA = LocalDate.of(2026, 11, 14);

    private ControllerTestFixtures() {
    }

    public static Vendedor vendedor() {
        return Vendedor.builder().id(1).nombre("Ana Vendedora").build();
    }

    public static Cliente cliente() {
        return Cliente.builder().id(2).nombre("Carlos Cliente").direccion("Calle 1 # 2-3").build();
    }

    public static Proveedor proveedor() {
        return Proveedor.builder().nit("900123456-7").nombre("Distribuidora Andina").contacto("ventas@andina.co").build();
    }

    public static Producto producto() {
        Producto producto = Producto.builder().id(3).nombre("Cuaderno").precio(5000.0).build();
        producto.getProveedores().add(proveedor());
        return producto;
    }

    public static Bodega bodega() {
        return Bodega.builder().id(4).ubicacion("Bogota Norte").build();
    }

    public static Inventario inventario() {
        return Inventario.builder().id(5).producto(producto()).bodega(bodega()).stock(8).stockMinimo(10).build();
    }

    public static AutorizacionRRHH autorizacion(String estado) {
        return AutorizacionRRHH.builder().id(6).vendedor(vendedor()).estado(estado).fecha(FECHA).build();
    }

    public static Venta venta(String estado) {
        Venta venta = Venta.builder().id(7).fecha(FECHA).estado(estado).total(10000.0)
                .vendedor(vendedor()).cliente(cliente()).build();
        venta.agregarDetalle(DetalleVenta.builder().id(1).producto(producto()).cantidad(2).precio(5000.0).build());
        return venta;
    }

    public static Entrega entrega(String estado) {
        Venta venta = venta("REGISTRADA");
        Entrega entrega = Entrega.builder().id(8).fecha(FECHA).estado(estado).direccion("Calle 1 # 2-3")
                .venta(venta).build();
        venta.setEntrega(entrega);
        return entrega;
    }

    public static CompraProveedor compra(String estado) {
        CompraProveedor compra = CompraProveedor.builder().id(9).fecha(FECHA).estado(estado).proveedor(proveedor())
                .build();
        DetalleCompra detalle = DetalleCompra.builder().id(1).producto(producto()).cantidad(10).precio(3000.0)
                .compra(compra).build();
        compra.getDetalles().add(detalle);
        return compra;
    }

    public static MovimientoStock movimiento(String tipo) {
        return MovimientoStock.builder().id(10).tipo(tipo).cantidad(10).fecha(FECHA).inventario(inventario())
                .compra(compra("RECIBIDA")).build();
    }
}
