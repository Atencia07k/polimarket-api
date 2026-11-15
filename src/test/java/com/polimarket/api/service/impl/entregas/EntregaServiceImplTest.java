package com.polimarket.api.service.impl.entregas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.polimarket.api.entity.DetalleVenta;
import com.polimarket.api.entity.Entrega;
import com.polimarket.api.entity.Producto;
import com.polimarket.api.entity.Venta;
import com.polimarket.api.exception.InventarioInsuficienteException;
import com.polimarket.api.exception.OperacionNoPermitidaException;
import com.polimarket.api.repository.entregas.EntregaRepository;
import com.polimarket.api.service.bodega.InventarioService;
import com.polimarket.api.service.entregas.EntregaService;
import com.polimarket.api.service.ventas.VentaService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EntregaServiceImplTest {

    @Mock
    private EntregaRepository entregaRepository;

    @Mock
    private VentaService ventaService;

    @Mock
    private InventarioService inventarioService;

    @InjectMocks
    private EntregaServiceImpl entregaService;

    private Venta venta;

    @BeforeEach
    void preparar() {
        Producto arroz = Producto.builder().id(1).nombre("Arroz").precio(3000).build();
        Producto sal = Producto.builder().id(2).nombre("Sal").precio(900).build();
        venta = Venta.builder().id(4).estado(VentaService.ESTADO_REGISTRADA).build();
        venta.agregarDetalle(DetalleVenta.builder().producto(sal).cantidad(1).precio(900).build());
        venta.agregarDetalle(DetalleVenta.builder().producto(arroz).cantidad(2).precio(3000).build());
        venta.agregarDetalle(DetalleVenta.builder().producto(arroz).cantidad(3).precio(3000).build());
    }

    private Entrega entrega(String estado) {
        return Entrega.builder().id(8).venta(venta).estado(estado).direccion("Calle 1").build();
    }

    @Test
    void consultarPendientesUsaElEstadoPendiente() {
        List<Entrega> pendientes = List.of(entrega(EntregaService.ESTADO_PENDIENTE));
        when(entregaRepository.findByEstado(EntregaService.ESTADO_PENDIENTE)).thenReturn(pendientes);

        assertEquals(pendientes, entregaService.consultarPendientes());
    }

    @Test
    void registrarEntregaQuedaPendienteSinMoverInventario() {
        when(ventaService.consultarVenta(4)).thenReturn(venta);
        when(entregaRepository.findByVentaId(4)).thenReturn(Optional.empty());
        when(entregaRepository.save(any(Entrega.class))).thenAnswer(inv -> inv.getArgument(0));

        Entrega entrega = entregaService.registrarEntrega(4, " Calle 1 ");

        assertEquals(EntregaService.ESTADO_PENDIENTE, entrega.getEstado());
        assertEquals("Calle 1", entrega.getDireccion());
        verify(inventarioService, never()).registrarSalida(anyInt(), anyInt(), anyInt(), any());
    }

    @Test
    void ventaConEntregaExistenteOCanceladaNoAdmiteOtraEntrega() {
        when(ventaService.consultarVenta(4)).thenReturn(venta);
        when(entregaRepository.findByVentaId(4)).thenReturn(Optional.of(entrega(EntregaService.ESTADO_PENDIENTE)));

        assertThrows(OperacionNoPermitidaException.class, () -> entregaService.registrarEntrega(4, "Calle 1"));

        venta.cancelar();
        assertThrows(OperacionNoPermitidaException.class, () -> entregaService.registrarEntrega(4, "Calle 1"));
        verify(entregaRepository, never()).save(any());
    }

    @Test
    void confirmarEntregaDescuentaCadaProductoUnaVezYMarcaEntregada() {
        Entrega entrega = entrega(EntregaService.ESTADO_PENDIENTE);
        when(entregaRepository.findConBloqueoById(8)).thenReturn(Optional.of(entrega));
        when(entregaRepository.save(entrega)).thenReturn(entrega);

        Entrega confirmada = entregaService.confirmarEntrega(8, 2);

        assertEquals(Entrega.ESTADO_ENTREGADA, confirmada.getEstado());
        verify(inventarioService).registrarSalida(1, 2, 5, entrega);
        verify(inventarioService).registrarSalida(2, 2, 1, entrega);
    }

    @Test
    void entregaYaProcesadaNoDescuentaDeNuevo() {
        when(entregaRepository.findConBloqueoById(8)).thenReturn(Optional.of(entrega(Entrega.ESTADO_ENTREGADA)));

        assertThrows(OperacionNoPermitidaException.class, () -> entregaService.confirmarEntrega(8, 2));
        verify(inventarioService, never()).registrarSalida(anyInt(), anyInt(), anyInt(), any());
        verify(entregaRepository, never()).save(any());
    }

    @Test
    void entregaDeVentaCanceladaNoSeConfirma() {
        venta.cancelar();
        when(entregaRepository.findConBloqueoById(8)).thenReturn(Optional.of(entrega(EntregaService.ESTADO_PENDIENTE)));

        assertThrows(OperacionNoPermitidaException.class, () -> entregaService.confirmarEntrega(8, 2));
        verify(inventarioService, never()).registrarSalida(anyInt(), anyInt(), anyInt(), any());
    }

    @Test
    void inventarioInsuficienteDejaLaEntregaPendiente() {
        Entrega entrega = entrega(EntregaService.ESTADO_PENDIENTE);
        when(entregaRepository.findConBloqueoById(8)).thenReturn(Optional.of(entrega));
        when(inventarioService.registrarSalida(1, 2, 5, entrega))
                .thenThrow(new InventarioInsuficienteException("sin stock"));

        assertThrows(InventarioInsuficienteException.class, () -> entregaService.confirmarEntrega(8, 2));
        assertEquals(EntregaService.ESTADO_PENDIENTE, entrega.getEstado());
        verify(entregaRepository, never()).save(any());
    }
}
