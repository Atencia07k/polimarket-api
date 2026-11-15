package com.polimarket.api.service.impl.clientes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.polimarket.api.entity.Cliente;
import com.polimarket.api.exception.DatosInvalidosException;
import com.polimarket.api.exception.RecursoNoEncontradoException;
import com.polimarket.api.repository.clientes.ClienteRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClienteServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    private final Cliente cliente = Cliente.builder().id(1).nombre("Ana").direccion("Calle 1").build();

    @Test
    void listarYBuscarUsanElRepositorio() {
        when(clienteRepository.findAll()).thenReturn(List.of(cliente));
        when(clienteRepository.findByNombreContainingIgnoreCase("an")).thenReturn(List.of(cliente));

        assertEquals(List.of(cliente), clienteService.listarClientes());
        assertEquals(List.of(cliente), clienteService.buscarClientesPorNombre("  an "));
    }

    @Test
    void obtenerClienteExistente() {
        when(clienteRepository.findById(1)).thenReturn(Optional.of(cliente));

        assertEquals("Ana", clienteService.obtenerCliente(1).getNombre());
    }

    @Test
    void obtenerClienteInexistenteLanzaExcepcion() {
        when(clienteRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> clienteService.obtenerCliente(99));
    }

    @Test
    void actualizarDatosModificaNombreYDireccion() {
        when(clienteRepository.findById(1)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(cliente)).thenReturn(cliente);

        Cliente actualizado = clienteService.actualizarDatos(1, " Ana Maria ", "Carrera 9");

        assertEquals("Ana Maria", actualizado.getNombre());
        assertEquals("Carrera 9", actualizado.getDireccion());
        verify(clienteRepository).save(cliente);
    }

    @Test
    void actualizarClienteInexistenteNoCreaUnoNuevo() {
        when(clienteRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> clienteService.actualizarDatos(99, "Ana", "Calle 1"));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void actualizarConDatosVaciosLanzaExcepcion() {
        assertThrows(DatosInvalidosException.class, () -> clienteService.actualizarDatos(1, " ", "Calle 1"));
        assertThrows(DatosInvalidosException.class, () -> clienteService.actualizarDatos(1, "Ana", null));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void existeClienteConIdentificadorNuloEsFalso() {
        assertFalse(clienteService.existeCliente(null));
    }
}
