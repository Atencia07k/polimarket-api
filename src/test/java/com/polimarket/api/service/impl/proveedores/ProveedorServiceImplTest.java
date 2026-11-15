package com.polimarket.api.service.impl.proveedores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.polimarket.api.entity.Proveedor;
import com.polimarket.api.exception.DatosInvalidosException;
import com.polimarket.api.exception.RecursoNoEncontradoException;
import com.polimarket.api.repository.proveedores.ProveedorRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProveedorServiceImplTest {

    @Mock
    private ProveedorRepository proveedorRepository;

    @InjectMocks
    private ProveedorServiceImpl proveedorService;

    @Test
    void actualizarContactoUsaElMetodoDeLaEntidad() {
        Proveedor proveedor = Proveedor.builder().nit("900").nombre("Agro").contacto("viejo").build();
        when(proveedorRepository.findById("900")).thenReturn(Optional.of(proveedor));
        when(proveedorRepository.save(proveedor)).thenReturn(proveedor);

        assertEquals("nuevo@agro.co", proveedorService.actualizarContacto("900", "nuevo@agro.co").getContacto());
    }

    @Test
    void contactoVacioOProveedorInexistenteSeRechazan() {
        when(proveedorRepository.findById("000")).thenReturn(Optional.empty());

        assertThrows(DatosInvalidosException.class, () -> proveedorService.actualizarContacto("900", " "));
        assertThrows(RecursoNoEncontradoException.class, () -> proveedorService.actualizarContacto("000", "x"));
        verify(proveedorRepository, never()).save(any());
    }
}
