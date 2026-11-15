package com.polimarket.api.service.impl.bodega;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.polimarket.api.entity.Bodega;
import com.polimarket.api.entity.Inventario;
import com.polimarket.api.exception.RecursoNoEncontradoException;
import com.polimarket.api.repository.bodega.BodegaRepository;
import com.polimarket.api.service.bodega.InventarioService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BodegaServiceImplTest {

    @Mock
    private BodegaRepository bodegaRepository;

    @Mock
    private InventarioService inventarioService;

    @InjectMocks
    private BodegaServiceImpl bodegaService;

    @Test
    void disponibilidadUsaElCalculoDeLaEntidad() {
        Bodega bodega = Bodega.builder().id(2).ubicacion("Norte").build();
        bodega.getInventarios().add(Inventario.builder().stock(4).build());
        bodega.getInventarios().add(Inventario.builder().stock(6).build());
        when(bodegaRepository.findById(2)).thenReturn(Optional.of(bodega));

        assertEquals(10, bodegaService.consultarDisponibilidad(2));
    }

    @Test
    void productosAlmacenadosSeDelegaEnInventarioService() {
        List<Inventario> inventarios = List.of(Inventario.builder().id(1).build());
        when(inventarioService.consultarInventarioPorBodega(2)).thenReturn(inventarios);

        assertEquals(inventarios, bodegaService.consultarProductosAlmacenados(2));
        verify(inventarioService).consultarInventarioPorBodega(2);
    }

    @Test
    void bodegaInexistenteLanzaExcepcion() {
        when(bodegaRepository.findById(9)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> bodegaService.obtenerBodega(9));
    }
}
