package com.polimarket.api.service.impl.bodega;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.polimarket.api.entity.Producto;
import com.polimarket.api.exception.DatosInvalidosException;
import com.polimarket.api.exception.RecursoNoEncontradoException;
import com.polimarket.api.repository.bodega.ProductoRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductoServiceImplTest {

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private ProductoServiceImpl productoService;

    private final Producto producto = Producto.builder().id(1).nombre("Arroz").precio(3000).build();

    @Test
    void consultasDeProductos() {
        when(productoRepository.findAll()).thenReturn(List.of(producto));
        when(productoRepository.findByProveedoresNit("900")).thenReturn(List.of(producto));

        assertEquals(List.of(producto), productoService.listarProductos());
        assertEquals(List.of(producto), productoService.consultarProductosPorProveedor("900"));
    }

    @Test
    void productoInexistenteLanzaExcepcion() {
        when(productoRepository.findById(9)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> productoService.obtenerProducto(9));
    }

    @Test
    void actualizarPrecioUsaElMetodoDeLaEntidad() {
        when(productoRepository.findById(1)).thenReturn(Optional.of(producto));
        when(productoRepository.save(producto)).thenReturn(producto);

        assertEquals(3500.0, productoService.actualizarPrecio(1, 3500).getPrecio());
    }

    @Test
    void precioNegativoONoNumericoSeRechaza() {
        assertThrows(DatosInvalidosException.class, () -> productoService.actualizarPrecio(1, -1));
        assertThrows(DatosInvalidosException.class, () -> productoService.actualizarPrecio(1, Double.NaN));
        verify(productoRepository, never()).save(any());
    }
}
