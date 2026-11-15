package com.polimarket.api.service.impl.bodega;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.polimarket.api.entity.Bodega;
import com.polimarket.api.entity.CompraProveedor;
import com.polimarket.api.entity.Entrega;
import com.polimarket.api.entity.Inventario;
import com.polimarket.api.entity.MovimientoStock;
import com.polimarket.api.entity.Producto;
import com.polimarket.api.exception.DatosInvalidosException;
import com.polimarket.api.exception.InventarioInsuficienteException;
import com.polimarket.api.exception.RecursoNoEncontradoException;
import com.polimarket.api.repository.bodega.BodegaRepository;
import com.polimarket.api.repository.bodega.InventarioRepository;
import com.polimarket.api.repository.bodega.MovimientoStockRepository;
import com.polimarket.api.service.bodega.ProductoService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventarioServiceImplTest {

    @Mock
    private InventarioRepository inventarioRepository;

    @Mock
    private MovimientoStockRepository movimientoStockRepository;

    @Mock
    private BodegaRepository bodegaRepository;

    @Mock
    private ProductoService productoService;

    @InjectMocks
    private InventarioServiceImpl inventarioService;

    private Producto producto;
    private Inventario inventario;

    @BeforeEach
    void preparar() {
        producto = Producto.builder().id(1).nombre("Arroz").precio(3000).build();
        Bodega bodega = Bodega.builder().id(2).ubicacion("Norte").build();
        inventario = Inventario.builder().id(10).producto(producto).bodega(bodega).stock(10).stockMinimo(3).build();
    }

    private void productoYBodegaExisten() {
        when(productoService.obtenerProducto(1)).thenReturn(producto);
        when(bodegaRepository.existsById(2)).thenReturn(true);
    }

    @Test
    void consultarStockYReposicionUsanLaEntidad() {
        productoYBodegaExisten();
        when(inventarioRepository.findByBodegaIdAndProductoId(2, 1)).thenReturn(Optional.of(inventario));

        assertEquals(10, inventarioService.consultarStock(1, 2));
        assertEquals(false, inventarioService.requiereReposicion(1, 2));
    }

    @Test
    void consultarStockTotalSumaTodasLasBodegas() {
        Inventario otro = Inventario.builder().id(11).producto(producto).stock(5).build();
        when(productoService.obtenerProducto(1)).thenReturn(producto);
        when(inventarioRepository.findByProductoId(1)).thenReturn(List.of(inventario, otro));

        assertEquals(15, inventarioService.consultarStockTotal(1));
    }

    @Test
    void consultarInventarioDeBodegaInexistenteLanzaExcepcion() {
        when(bodegaRepository.existsById(99)).thenReturn(false);

        assertThrows(RecursoNoEncontradoException.class, () -> inventarioService.consultarInventarioPorBodega(99));
    }

    @Test
    void registrarEntradaAumentaStockYGuardaMovimientoDeLaCompra() {
        productoYBodegaExisten();
        CompraProveedor compra = CompraProveedor.builder().id(5).build();
        when(inventarioRepository.findConBloqueoByBodegaIdAndProductoId(2, 1)).thenReturn(Optional.of(inventario));
        when(movimientoStockRepository.save(any(MovimientoStock.class))).thenAnswer(inv -> inv.getArgument(0));

        MovimientoStock movimiento = inventarioService.registrarEntrada(1, 2, 4, compra);

        assertEquals(14, inventario.getStock());
        assertEquals(MovimientoStock.TIPO_ENTRADA, movimiento.getTipo());
        assertEquals(4, movimiento.getCantidad());
        assertEquals(LocalDate.now(), movimiento.getFecha());
        assertSame(compra, movimiento.getCompra());
        assertSame(inventario, movimiento.getInventario());
        verify(movimientoStockRepository).save(movimiento);
    }

    @Test
    void registrarSalidaDisminuyeStockYGuardaMovimientoDeLaEntrega() {
        productoYBodegaExisten();
        Entrega entrega = Entrega.builder().id(8).build();
        when(inventarioRepository.findConBloqueoByBodegaIdAndProductoId(2, 1)).thenReturn(Optional.of(inventario));
        when(movimientoStockRepository.save(any(MovimientoStock.class))).thenAnswer(inv -> inv.getArgument(0));

        MovimientoStock movimiento = inventarioService.registrarSalida(1, 2, 10, entrega);

        assertEquals(0, inventario.getStock());
        assertEquals(MovimientoStock.TIPO_SALIDA, movimiento.getTipo());
        assertSame(entrega, movimiento.getEntrega());
        assertTrue(inventario.requiereReposicion());
    }

    @Test
    void salidaQueProduciriaStockNegativoSeRechazaSinGuardar() {
        productoYBodegaExisten();
        Entrega entrega = Entrega.builder().id(8).build();
        when(inventarioRepository.findConBloqueoByBodegaIdAndProductoId(2, 1)).thenReturn(Optional.of(inventario));

        assertThrows(InventarioInsuficienteException.class, () -> inventarioService.registrarSalida(1, 2, 11, entrega));
        assertEquals(10, inventario.getStock());
        verify(movimientoStockRepository, never()).save(any());
    }

    @Test
    void cantidadesNoPositivasSeRechazan() {
        CompraProveedor compra = CompraProveedor.builder().id(5).build();
        Entrega entrega = Entrega.builder().id(8).build();

        assertThrows(DatosInvalidosException.class, () -> inventarioService.registrarEntrada(1, 2, 0, compra));
        assertThrows(DatosInvalidosException.class, () -> inventarioService.registrarSalida(1, 2, -3, entrega));
        verify(movimientoStockRepository, never()).save(any());
    }

    @Test
    void movimientosSinOrigenRegistradoSeRechazan() {
        assertThrows(DatosInvalidosException.class, () -> inventarioService.registrarEntrada(1, 2, 1, null));
        assertThrows(DatosInvalidosException.class,
                () -> inventarioService.registrarSalida(1, 2, 1, Entrega.builder().build()));
    }

    @Test
    void entradaSinRegistroDeInventarioSeRechaza() {
        productoYBodegaExisten();
        when(inventarioRepository.findConBloqueoByBodegaIdAndProductoId(2, 1)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> inventarioService.registrarEntrada(1, 2, 1, CompraProveedor.builder().id(5).build()));
        verify(movimientoStockRepository, never()).save(any());
    }
}
