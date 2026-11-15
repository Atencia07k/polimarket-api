package com.polimarket.api.service.impl.rrhh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.polimarket.api.entity.Vendedor;
import com.polimarket.api.exception.RecursoNoEncontradoException;
import com.polimarket.api.repository.rrhh.VendedorRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VendedorServiceImplTest {

    @Mock
    private VendedorRepository vendedorRepository;

    @InjectMocks
    private VendedorServiceImpl vendedorService;

    @Test
    void consultasDeVendedores() {
        Vendedor vendedor = Vendedor.builder().id(7).nombre("Luis").build();
        when(vendedorRepository.findById(7)).thenReturn(Optional.of(vendedor));
        when(vendedorRepository.findByNombreContainingIgnoreCase("lu")).thenReturn(List.of(vendedor));

        assertEquals("Luis", vendedorService.obtenerVendedor(7).getNombre());
        assertEquals(List.of(vendedor), vendedorService.buscarVendedoresPorNombre("lu"));
    }

    @Test
    void vendedorInexistenteLanzaExcepcion() {
        when(vendedorRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> vendedorService.obtenerVendedor(99));
    }
}
