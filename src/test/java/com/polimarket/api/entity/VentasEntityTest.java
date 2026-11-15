package com.polimarket.api.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VentasEntityTest {

    @Test
    void detalleVentaSubtotalEsCantidadPorPrecio() {
        DetalleVenta detalle = DetalleVenta.builder().cantidad(3).precio(2500.5).build();

        assertThat(detalle.subtotal()).isEqualTo(7501.5);
    }

    @Test
    void calcularTotalSumaSubtotalesYActualizaElAtributoTotal() {
        Venta venta = new Venta();
        venta.agregarDetalle(DetalleVenta.builder().cantidad(2).precio(1000.0).build());
        venta.agregarDetalle(DetalleVenta.builder().cantidad(1).precio(500.25).build());

        assertThat(venta.calcularTotal()).isEqualTo(2500.25);
        assertThat(venta.getTotal()).isEqualTo(2500.25);
    }

    @Test
    void calcularTotalSinDetallesEsCero() {
        assertThat(new Venta().calcularTotal()).isZero();
    }

    @Test
    void agregarDetalleEstableceLaVentaDelDetalle() {
        Venta venta = new Venta();
        DetalleVenta detalle = new DetalleVenta();

        venta.agregarDetalle(detalle);

        assertThat(venta.getDetalles()).containsExactly(detalle);
        assertThat(detalle.getVenta()).isSameAs(venta);
    }

    @Test
    void cancelarCambiaElEstadoACancelada() {
        Venta venta = Venta.builder().estado("REGISTRADA").build();

        venta.cancelar();

        assertThat(venta.getEstado()).isEqualTo(Venta.ESTADO_CANCELADA);
    }
}