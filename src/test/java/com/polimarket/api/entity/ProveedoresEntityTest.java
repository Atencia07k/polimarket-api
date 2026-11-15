package com.polimarket.api.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ProveedoresEntityTest {

    @Test
    void detalleCompraSubtotalEsCantidadPorPrecio() {
        DetalleCompra detalle = DetalleCompra.builder().cantidad(10).precio(1200.0).build();

        assertThat(detalle.subtotal()).isEqualTo(12000.0);
    }

    @Test
    void calcularTotalDeCompraSumaSubtotales() {
        CompraProveedor compra = new CompraProveedor();
        compra.agregarDetalle(DetalleCompra.builder().cantidad(10).precio(1200.0).build());
        compra.agregarDetalle(DetalleCompra.builder().cantidad(4).precio(250.5).build());

        assertThat(compra.calcularTotal()).isEqualTo(13002.0);
        assertThat(compra.getDetalles()).allSatisfy(d -> assertThat(d.getCompra()).isSameAs(compra));
    }

    @Test
    void cancelarCompraCambiaElEstadoACancelada() {
        CompraProveedor compra = CompraProveedor.builder().estado("REGISTRADA").build();

        compra.cancelar();

        assertThat(compra.getEstado()).isEqualTo(CompraProveedor.ESTADO_CANCELADA);
    }

    @Test
    void actualizarContactoReemplazaElContacto() {
        Proveedor proveedor = Proveedor.builder().nit("900123456-1").contacto("anterior").build();

        proveedor.actualizarContacto("compras@proveedor.com");

        assertThat(proveedor.getContacto()).isEqualTo("compras@proveedor.com");
    }

    @Test
    void actualizarContactoRechazaDatosVacios() {
        Proveedor proveedor = Proveedor.builder().contacto("actual").build();

        assertThatIllegalArgumentException().isThrownBy(() -> proveedor.actualizarContacto(" "));
        assertThat(proveedor.getContacto()).isEqualTo("actual");
    }

    @Test
    void agregarProveedorMantieneAmbosLadosDeLaRelacion() {
        Producto producto = new Producto();
        Proveedor proveedor = new Proveedor();

        producto.agregarProveedor(proveedor);

        assertThat(producto.getProveedores()).containsExactly(proveedor);
        assertThat(proveedor.getProductos()).containsExactly(producto);
    }
}