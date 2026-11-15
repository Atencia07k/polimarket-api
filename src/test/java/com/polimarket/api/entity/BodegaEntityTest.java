package com.polimarket.api.entity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class BodegaEntityTest {

    @Test
    void actualizarStockSumaYRestaLaCantidad() {
        Inventario inventario = Inventario.builder().stock(10).stockMinimo(3).build();

        inventario.actualizarStock(5);
        assertThat(inventario.consultarExistencia()).isEqualTo(15);

        inventario.actualizarStock(-12);
        assertThat(inventario.consultarExistencia()).isEqualTo(3);
    }

    @Test
    void actualizarStockNoPermiteStockNegativo() {
        Inventario inventario = Inventario.builder().stock(2).build();

        assertThatIllegalArgumentException().isThrownBy(() -> inventario.actualizarStock(-3));
        assertThat(inventario.getStock()).isEqualTo(2);
    }

    @Test
    void requiereReposicionCuandoElStockNoSuperaElMinimo() {
        assertThat(Inventario.builder().stock(2).stockMinimo(3).build().requiereReposicion()).isTrue();
        assertThat(Inventario.builder().stock(3).stockMinimo(3).build().requiereReposicion()).isTrue();
        assertThat(Inventario.builder().stock(4).stockMinimo(3).build().requiereReposicion()).isFalse();
    }

    @Test
    void consultarDisponibilidadSumaElStockDeLosInventarios() {
        Bodega bodega = Bodega.builder()
                .inventarios(List.of(
                        Inventario.builder().stock(7).build(),
                        Inventario.builder().stock(5).build()))
                .build();

        assertThat(bodega.consultarDisponibilidad()).isEqualTo(12);
        assertThat(new Bodega().consultarDisponibilidad()).isZero();
    }

    @Test
    void registrarEntradaSumaAlInventario() {
        Inventario inventario = Inventario.builder().stock(10).build();
        MovimientoStock movimiento = MovimientoStock.builder()
                .tipo(MovimientoStock.TIPO_ENTRADA).cantidad(4).inventario(inventario).build();

        movimiento.registrar();

        assertThat(movimiento.esEntrada()).isTrue();
        assertThat(inventario.getStock()).isEqualTo(14);
    }

    @Test
    void registrarSalidaRestaDelInventario() {
        Inventario inventario = Inventario.builder().stock(10).build();
        MovimientoStock movimiento = MovimientoStock.builder()
                .tipo(MovimientoStock.TIPO_SALIDA).cantidad(4).inventario(inventario).build();

        movimiento.registrar();

        assertThat(movimiento.esEntrada()).isFalse();
        assertThat(inventario.getStock()).isEqualTo(6);
    }

    @Test
    void registrarSalidaSinStockSuficienteNoModificaElInventario() {
        Inventario inventario = Inventario.builder().stock(1).build();
        MovimientoStock movimiento = MovimientoStock.builder()
                .tipo(MovimientoStock.TIPO_SALIDA).cantidad(4).inventario(inventario).build();

        assertThatIllegalArgumentException().isThrownBy(movimiento::registrar);
        assertThat(inventario.getStock()).isEqualTo(1);
    }

    @Test
    void actualizarPrecioDeProducto() {
        Producto producto = Producto.builder().precio(1000.0).build();

        producto.actualizarPrecio(1250.75);
        assertThat(producto.getPrecio()).isEqualTo(1250.75);

        assertThatIllegalArgumentException().isThrownBy(() -> producto.actualizarPrecio(-1));
        assertThat(producto.getPrecio()).isEqualTo(1250.75);
    }
}