package com.polimarket.api.service.impl.bodega;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.polimarket.api.entity.Inventario;
import com.polimarket.api.entity.MovimientoStock;
import com.polimarket.api.exception.DatosInvalidosException;
import com.polimarket.api.repository.bodega.MovimientoStockRepository;
import com.polimarket.api.service.bodega.InventarioService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MovimientoStockServiceImplTest {

    @Mock
    private MovimientoStockRepository movimientoStockRepository;

    @Mock
    private InventarioService inventarioService;

    @InjectMocks
    private MovimientoStockServiceImpl movimientoStockService;

    @Test
    void consultarPorProductoReuneLosMovimientosDeSusInventarios() {
        MovimientoStock m1 = MovimientoStock.builder().id(1).build();
        MovimientoStock m2 = MovimientoStock.builder().id(2).build();
        when(inventarioService.consultarInventarioPorProducto(1)).thenReturn(List.of(
                Inventario.builder().id(10).build(), Inventario.builder().id(11).build()));
        when(movimientoStockRepository.findByInventarioId(10)).thenReturn(List.of(m1));
        when(movimientoStockRepository.findByInventarioId(11)).thenReturn(List.of(m2));

        assertEquals(List.of(m1, m2), movimientoStockService.consultarPorProducto(1));
    }

    @Test
    void consultarPorTipoSoloAceptaTiposDefinidos() {
        when(movimientoStockRepository.findByTipo(MovimientoStock.TIPO_SALIDA)).thenReturn(List.of());

        assertEquals(List.of(), movimientoStockService.consultarPorTipo(MovimientoStock.TIPO_SALIDA));
        assertThrows(DatosInvalidosException.class, () -> movimientoStockService.consultarPorTipo("AJUSTE"));
    }

    @Test
    void rangoDeFechasInvertidoSeRechaza() {
        LocalDate hoy = LocalDate.now();

        assertThrows(DatosInvalidosException.class, () -> movimientoStockService.consultarPorFechas(hoy, hoy.minusDays(1)));
    }
}
