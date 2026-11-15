package com.polimarket.api.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RecursosHumanosEntregasEntityTest {

    @Test
    void autorizarYRevocarCambianEstadoYFecha() {
        AutorizacionRRHH autorizacion = new AutorizacionRRHH();

        autorizacion.autorizar();
        assertThat(autorizacion.getEstado()).isEqualTo(AutorizacionRRHH.ESTADO_AUTORIZADA);
        assertThat(autorizacion.getFecha()).isEqualTo(LocalDate.now());
        assertThat(autorizacion.estaVigente()).isTrue();

        autorizacion.revocar();
        assertThat(autorizacion.getEstado()).isEqualTo(AutorizacionRRHH.ESTADO_REVOCADA);
        assertThat(autorizacion.estaVigente()).isFalse();
    }

    @Test
    void vendedorSinAutorizacionesNoEstaAutorizado() {
        assertThat(new Vendedor().estaAutorizado()).isFalse();
    }

    @Test
    void vendedorEstaAutorizadoSegunSuAutorizacionMasReciente() {
        AutorizacionRRHH antigua = autorizacion(1, LocalDate.of(2026, 1, 10), AutorizacionRRHH.ESTADO_AUTORIZADA);
        AutorizacionRRHH reciente = autorizacion(2, LocalDate.of(2026, 3, 5), AutorizacionRRHH.ESTADO_REVOCADA);

        assertThat(Vendedor.builder().autorizaciones(List.of(reciente, antigua)).build().estaAutorizado()).isFalse();
        assertThat(Vendedor.builder().autorizaciones(List.of(antigua)).build().estaAutorizado()).isTrue();
    }

    @Test
    void aIgualFechaPrevaleceLaAutorizacionDeMayorId() {
        LocalDate fecha = LocalDate.of(2026, 5, 1);
        Vendedor vendedor = Vendedor.builder().autorizaciones(List.of(
                autorizacion(8, fecha, AutorizacionRRHH.ESTADO_AUTORIZADA),
                autorizacion(7, fecha, AutorizacionRRHH.ESTADO_REVOCADA))).build();

        assertThat(vendedor.estaAutorizado()).isTrue();
    }

    @Test
    void registrarEntregaYActualizarEstado() {
        Entrega entrega = Entrega.builder().estado("PROGRAMADA").build();

        entrega.registrarEntrega();
        assertThat(entrega.getEstado()).isEqualTo(Entrega.ESTADO_ENTREGADA);

        entrega.actualizarEstado("EN_RUTA");
        assertThat(entrega.getEstado()).isEqualTo("EN_RUTA");

        assertThatIllegalArgumentException().isThrownBy(() -> entrega.actualizarEstado(""));
        assertThat(entrega.getEstado()).isEqualTo("EN_RUTA");
    }

    private static AutorizacionRRHH autorizacion(int id, LocalDate fecha, String estado) {
        return AutorizacionRRHH.builder().id(id).fecha(fecha).estado(estado).build();
    }
}