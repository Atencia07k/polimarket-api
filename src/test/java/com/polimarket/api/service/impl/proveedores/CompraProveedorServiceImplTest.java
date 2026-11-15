package com.polimarket.api.service.impl.proveedores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.polimarket.api.entity.CompraProveedor;
import com.polimarket.api.entity.DetalleCompra;
import com.polimarket.api.entity.Producto;
import com.polimarket.api.entity.Proveedor;
import com.polimarket.api.exception.DatosInvalidosException;
import com.polimarket.api.exception.OperacionNoPermitidaException;
import com.polimarket.api.exception.RecursoNoEncontradoException;
import com.polimarket.api.repository.proveedores.CompraProveedorRepository;
import com.polimarket.api.service.bodega.InventarioService;
import com.polimarket.api.service.bodega.ProductoService;
import com.polimarket.api.service.proveedores.CompraProveedorService;
import com.polimarket.api.service.proveedores.CompraProveedorService.ItemCompra;
import com.polimarket.api.service.proveedores.ProveedorService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompraProveedorServiceImplTest {

    @Mock
    private CompraProveedorRepository compraProveedorRepository;

    @Mock
    private ProveedorService proveedorService;

    @Mock
    private ProductoService productoService;

    @Mock
    private InventarioService inventarioService;

    @InjectMocks
    private CompraProveedorServiceImpl compraService;

    private Proveedor proveedor;
    private Producto arroz;

    @BeforeEach
    void preparar() {
        proveedor = Proveedor.builder().nit("900").nombre("Agro").contacto("agro@co").build();
        arroz = Producto.builder().id(1).nombre("Arroz").precio(3000).build();
        arroz.agregarProveedor(proveedor);
    }

    @Test
    void registrarCompraConstruyeDetallesEnEstadoPendiente() {
        when(proveedorService.obtenerProveedor("900")).thenReturn(proveedor);
        when(productoService.obtenerProducto(1)).thenReturn(arroz);
        when(compraProveedorRepository.save(any(CompraProveedor.class))).thenAnswer(inv -> inv.getArgument(0));

        CompraProveedor compra = compraService.registrarCompra("900",
                List.of(new ItemCompra(1, 10, 2000), new ItemCompra(1, 5, 1800)));

        assertEquals(CompraProveedorService.ESTADO_PENDIENTE, compra.getEstado());
        assertSame(proveedor, compra.getProveedor());
        assertEquals(2, compra.getDetalles().size());
        assertEquals(29000.0, compra.calcularTotal());
        verify(inventarioService, never()).registrarEntrada(anyInt(), anyInt(), anyInt(), any());
    }

    @Test
    void proveedorInexistenteLanzaExcepcion() {
        when(proveedorService.obtenerProveedor("000")).thenThrow(new RecursoNoEncontradoException("Proveedor", "000"));

        assertThrows(RecursoNoEncontradoException.class,
                () -> compraService.registrarCompra("000", List.of(new ItemCompra(1, 1, 100))));
        verify(compraProveedorRepository, never()).save(any());
    }

    @Test
    void productoInexistenteLanzaExcepcion() {
        when(proveedorService.obtenerProveedor("900")).thenReturn(proveedor);
        when(productoService.obtenerProducto(9)).thenThrow(new RecursoNoEncontradoException("Producto", 9));

        assertThrows(RecursoNoEncontradoException.class,
                () -> compraService.registrarCompra("900", List.of(new ItemCompra(9, 1, 100))));
        verify(compraProveedorRepository, never()).save(any());
    }

    @Test
    void productoNoSuministradoPorElProveedorSeRechaza() {
        Producto ajeno = Producto.builder().id(2).nombre("Sal").precio(900).build();
        ajeno.agregarProveedor(Proveedor.builder().nit("800").nombre("Otro").contacto("x").build());
        when(proveedorService.obtenerProveedor("900")).thenReturn(proveedor);
        when(productoService.obtenerProducto(2)).thenReturn(ajeno);

        assertThrows(DatosInvalidosException.class,
                () -> compraService.registrarCompra("900", List.of(new ItemCompra(2, 1, 100))));
        verify(compraProveedorRepository, never()).save(any());
    }

    @Test
    void itemsInvalidosSeRechazan() {
        assertThrows(DatosInvalidosException.class, () -> compraService.registrarCompra("900", List.of()));
        assertThrows(DatosInvalidosException.class,
                () -> compraService.registrarCompra("900", List.of(new ItemCompra(1, 0, 100))));
        assertThrows(DatosInvalidosException.class,
                () -> compraService.registrarCompra("900", List.of(new ItemCompra(1, 1, -5))));
        verify(compraProveedorRepository, never()).save(any());
    }

    @Test
    void recibirCompraRegistraEntradasAgrupadasYMarcaRecibida() {
        CompraProveedor compra = CompraProveedor.builder().id(5).proveedor(proveedor)
                .estado(CompraProveedorService.ESTADO_PENDIENTE).build();
        compra.agregarDetalle(DetalleCompra.builder().producto(arroz).cantidad(10).precio(2000).build());
        compra.agregarDetalle(DetalleCompra.builder().producto(arroz).cantidad(5).precio(1800).build());
        when(compraProveedorRepository.findConBloqueoById(5)).thenReturn(Optional.of(compra));
        when(compraProveedorRepository.save(compra)).thenReturn(compra);

        CompraProveedor recibida = compraService.recibirCompra(5, 2);

        assertEquals(CompraProveedorService.ESTADO_RECIBIDA, recibida.getEstado());
        verify(inventarioService).registrarEntrada(1, 2, 15, compra);
    }

    @Test
    void compraYaRecibidaNoIngresaInventarioDeNuevo() {
        CompraProveedor compra = CompraProveedor.builder().id(5)
                .estado(CompraProveedorService.ESTADO_RECIBIDA).build();
        when(compraProveedorRepository.findConBloqueoById(5)).thenReturn(Optional.of(compra));

        assertThrows(OperacionNoPermitidaException.class, () -> compraService.recibirCompra(5, 2));
        assertThrows(OperacionNoPermitidaException.class, () -> compraService.cancelarCompra(5));
        verify(inventarioService, never()).registrarEntrada(anyInt(), anyInt(), anyInt(), any());
    }

    @Test
    void cancelarCompraPendiente() {
        CompraProveedor compra = CompraProveedor.builder().id(5)
                .estado(CompraProveedorService.ESTADO_PENDIENTE).build();
        when(compraProveedorRepository.findConBloqueoById(5)).thenReturn(Optional.of(compra));
        when(compraProveedorRepository.save(compra)).thenReturn(compra);

        assertEquals(CompraProveedor.ESTADO_CANCELADA, compraService.cancelarCompra(5).getEstado());
    }
}
