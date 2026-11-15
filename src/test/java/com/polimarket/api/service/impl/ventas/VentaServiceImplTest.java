package com.polimarket.api.service.impl.ventas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.polimarket.api.entity.Cliente;
import com.polimarket.api.entity.Entrega;
import com.polimarket.api.entity.Producto;
import com.polimarket.api.entity.Vendedor;
import com.polimarket.api.entity.Venta;
import com.polimarket.api.exception.DatosInvalidosException;
import com.polimarket.api.exception.InventarioInsuficienteException;
import com.polimarket.api.exception.OperacionNoPermitidaException;
import com.polimarket.api.exception.RecursoNoEncontradoException;
import com.polimarket.api.exception.VendedorNoAutorizadoException;
import com.polimarket.api.repository.ventas.VentaRepository;
import com.polimarket.api.service.bodega.InventarioService;
import com.polimarket.api.service.bodega.ProductoService;
import com.polimarket.api.service.clientes.ClienteService;
import com.polimarket.api.service.rrhh.AutorizacionRRHHService;
import com.polimarket.api.service.rrhh.VendedorService;
import com.polimarket.api.service.ventas.VentaService;
import com.polimarket.api.service.ventas.VentaService.ItemVenta;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VentaServiceImplTest {

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private ClienteService clienteService;

    @Mock
    private VendedorService vendedorService;

    @Mock
    private AutorizacionRRHHService autorizacionRRHHService;

    @Mock
    private ProductoService productoService;

    @Mock
    private InventarioService inventarioService;

    @InjectMocks
    private VentaServiceImpl ventaService;

    private final Vendedor vendedor = Vendedor.builder().id(7).nombre("Luis").build();
    private final Cliente cliente = Cliente.builder().id(3).nombre("Ana").direccion("Calle 1").build();
    private final Producto arroz = Producto.builder().id(1).nombre("Arroz").precio(3000).build();

    @Test
    void listarProductosYClientesReutilizanOtrosComponentes() {
        when(productoService.listarProductos()).thenReturn(List.of(arroz));
        when(clienteService.listarClientes()).thenReturn(List.of(cliente));

        assertEquals(List.of(arroz), ventaService.listarProductos());
        assertEquals(List.of(cliente), ventaService.listarClientes());
    }

    @Test
    void vendedorAutorizadoRegistraVentaSinDescontarInventario() {
        when(vendedorService.obtenerVendedor(7)).thenReturn(vendedor);
        when(autorizacionRRHHService.validarVendedor(7)).thenReturn(true);
        when(clienteService.obtenerCliente(3)).thenReturn(cliente);
        when(productoService.obtenerProducto(1)).thenReturn(arroz);
        when(inventarioService.consultarStockTotal(1)).thenReturn(5);
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));

        Venta venta = ventaService.registrarVenta(7, 3, List.of(new ItemVenta(1, 2), new ItemVenta(1, 3)));

        assertEquals(VentaService.ESTADO_REGISTRADA, venta.getEstado());
        assertEquals(2, venta.getDetalles().size());
        assertEquals(3000.0, venta.getDetalles().get(0).getPrecio());
        assertEquals(15000.0, venta.getTotal());
        verify(ventaRepository).save(venta);
        verify(inventarioService, never()).registrarSalida(anyInt(), anyInt(), anyInt(), any());
    }

    @Test
    void vendedorNoAutorizadoNoPuedeVender() {
        when(vendedorService.obtenerVendedor(7)).thenReturn(vendedor);
        when(autorizacionRRHHService.validarVendedor(7)).thenReturn(false);

        assertThrows(VendedorNoAutorizadoException.class,
                () -> ventaService.registrarVenta(7, 3, List.of(new ItemVenta(1, 1))));
        verify(ventaRepository, never()).save(any());
        verifyNoInteractions(clienteService, inventarioService);
    }

    @Test
    void productoInexistenteImpideLaVenta() {
        when(vendedorService.obtenerVendedor(7)).thenReturn(vendedor);
        when(autorizacionRRHHService.validarVendedor(7)).thenReturn(true);
        when(clienteService.obtenerCliente(3)).thenReturn(cliente);
        when(productoService.obtenerProducto(9)).thenThrow(new RecursoNoEncontradoException("Producto", 9));

        assertThrows(RecursoNoEncontradoException.class,
                () -> ventaService.registrarVenta(7, 3, List.of(new ItemVenta(9, 1))));
        verify(ventaRepository, never()).save(any());
    }

    @Test
    void cantidadesInvalidasSeRechazanAntesDeConsultarOtrosComponentes() {
        assertThrows(DatosInvalidosException.class, () -> ventaService.registrarVenta(7, 3, List.of(new ItemVenta(1, 0))));
        assertThrows(DatosInvalidosException.class, () -> ventaService.registrarVenta(7, 3, List.of(new ItemVenta(1, -2))));
        assertThrows(DatosInvalidosException.class, () -> ventaService.registrarVenta(7, 3, List.of()));
        verifyNoInteractions(vendedorService, autorizacionRRHHService, ventaRepository);
    }

    @Test
    void existenciaTotalInsuficienteImpideLaVenta() {
        when(vendedorService.obtenerVendedor(7)).thenReturn(vendedor);
        when(autorizacionRRHHService.validarVendedor(7)).thenReturn(true);
        when(clienteService.obtenerCliente(3)).thenReturn(cliente);
        when(productoService.obtenerProducto(1)).thenReturn(arroz);
        when(inventarioService.consultarStockTotal(1)).thenReturn(4);

        assertThrows(InventarioInsuficienteException.class,
                () -> ventaService.registrarVenta(7, 3, List.of(new ItemVenta(1, 3), new ItemVenta(1, 2))));
        verify(ventaRepository, never()).save(any());
    }

    @Test
    void ventaEntregadaNoPuedeCancelarse() {
        Venta venta = Venta.builder().id(4).estado(VentaService.ESTADO_REGISTRADA)
                .entrega(Entrega.builder().estado(Entrega.ESTADO_ENTREGADA).build()).build();
        when(ventaRepository.findById(4)).thenReturn(Optional.of(venta));

        assertThrows(OperacionNoPermitidaException.class, () -> ventaService.cancelarVenta(4));
        verify(ventaRepository, never()).save(any());
    }

    @Test
    void ventaRegistradaSinEntregaSeCancela() {
        Venta venta = Venta.builder().id(4).estado(VentaService.ESTADO_REGISTRADA).build();
        when(ventaRepository.findById(4)).thenReturn(Optional.of(venta));
        when(ventaRepository.save(venta)).thenReturn(venta);

        assertEquals(Venta.ESTADO_CANCELADA, ventaService.cancelarVenta(4).getEstado());
    }
}
