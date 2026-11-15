package com.polimarket.api.repository.entregas;

import static org.assertj.core.api.Assertions.assertThat;

import com.polimarket.api.entity.Entrega;
import com.polimarket.api.entity.Inventario;
import com.polimarket.api.entity.MovimientoStock;
import com.polimarket.api.entity.Producto;
import com.polimarket.api.entity.Venta;
import com.polimarket.api.repository.AbstractRepositoryIT;
import com.polimarket.api.repository.bodega.MovimientoStockRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EntregaRepositoryIT extends AbstractRepositoryIT {

    @Autowired
    private EntregaRepository entregaRepository;

    @Autowired
    private MovimientoStockRepository movimientoRepository;

    @Test
    void consultaEntregasPorEstadoVentaYFechas() {
        Producto arroz = producto("Arroz", proveedor("900123456-1", "Alimentos SA"));
        Venta v1 = venta(vendedor("Laura"), cliente("Ana"), ESTADO_PRUEBA, HOY.minusDays(4), arroz);
        Venta v2 = venta(vendedor("Jorge"), cliente("Pedro"), ESTADO_PRUEBA, HOY.minusDays(1), arroz);
        Venta sinEntrega = venta(vendedor("Marta"), cliente("Luis"), ESTADO_PRUEBA, HOY, arroz);
        Entrega e1 = entrega(v1, Entrega.ESTADO_ENTREGADA, HOY.minusDays(2));
        Entrega e2 = entregaRepository.save(Entrega.builder()
                .venta(v2).estado(ESTADO_PRUEBA).fecha(HOY).direccion("Calle 50 # 10-10").build());
        flushAndClear();

        assertThat(entregaRepository.findByEstado(Entrega.ESTADO_ENTREGADA))
                .extracting(Entrega::getId).containsExactly(e1.getId());
        assertThat(entregaRepository.findByVentaId(v2.getId()))
                .get().extracting(Entrega::getId).isEqualTo(e2.getId());
        assertThat(entregaRepository.findByVentaId(sinEntrega.getId())).isEmpty();
        assertThat(entregaRepository.findByFechaBetween(HOY.minusDays(2), HOY))
                .extracting(Entrega::getId).containsExactlyInAnyOrder(e1.getId(), e2.getId());
        assertThat(entregaRepository.findById(e1.getId())).isPresent();
    }

    @Test
    void entregaConservaSusMovimientosDeSalidaAsociados() {
        Producto arroz = producto("Arroz", proveedor("900123456-1", "Alimentos SA"));
        Inventario inv = inventario(bodega("Norte"), arroz, 30, 5);
        Entrega entrega = entrega(venta(vendedor("Laura"), cliente("Ana"), ESTADO_PRUEBA, HOY, arroz),
                Entrega.ESTADO_ENTREGADA, HOY);
        MovimientoStock salida = movimiento(inv, MovimientoStock.TIPO_SALIDA, HOY, entrega, null);
        flushAndClear();

        Entrega recuperada = entregaRepository.findById(entrega.getId()).orElseThrow();
        assertThat(recuperada.getMovimientos())
                .extracting(MovimientoStock::getId).containsExactly(salida.getId());
        assertThat(movimientoRepository.findByEntregaId(entrega.getId()))
                .singleElement().extracting(MovimientoStock::getTipo).isEqualTo(MovimientoStock.TIPO_SALIDA);
    }
}
