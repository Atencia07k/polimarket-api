package com.polimarket.api.service.impl.rrhh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.polimarket.api.entity.AutorizacionRRHH;
import com.polimarket.api.entity.Vendedor;
import com.polimarket.api.exception.OperacionNoPermitidaException;
import com.polimarket.api.exception.RecursoNoEncontradoException;
import com.polimarket.api.repository.rrhh.AutorizacionRRHHRepository;
import com.polimarket.api.service.rrhh.VendedorService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AutorizacionRRHHServiceImplTest {

    @Mock
    private AutorizacionRRHHRepository autorizacionRepository;

    @Mock
    private VendedorService vendedorService;

    @InjectMocks
    private AutorizacionRRHHServiceImpl autorizacionService;

    private final Vendedor vendedor = Vendedor.builder().id(7).nombre("Luis").build();

    private AutorizacionRRHH registro(String estado) {
        return AutorizacionRRHH.builder().id(1).vendedor(vendedor).estado(estado).fecha(LocalDate.now()).build();
    }

    @Test
    void vendedorConUltimaAutorizacionVigenteEsValido() {
        when(vendedorService.obtenerVendedor(7)).thenReturn(vendedor);
        when(autorizacionRepository.findFirstByVendedorIdOrderByFechaDescIdDesc(7))
                .thenReturn(Optional.of(registro(AutorizacionRRHH.ESTADO_AUTORIZADA)));

        assertTrue(autorizacionService.validarVendedor(7));
    }

    @Test
    void vendedorConUltimoRegistroRevocadoNoEsValido() {
        when(vendedorService.obtenerVendedor(7)).thenReturn(vendedor);
        when(autorizacionRepository.findFirstByVendedorIdOrderByFechaDescIdDesc(7))
                .thenReturn(Optional.of(registro(AutorizacionRRHH.ESTADO_REVOCADA)));

        assertFalse(autorizacionService.validarVendedor(7));
    }

    @Test
    void vendedorSinRegistrosNoEsValido() {
        when(vendedorService.obtenerVendedor(7)).thenReturn(vendedor);
        when(autorizacionRepository.findFirstByVendedorIdOrderByFechaDescIdDesc(7)).thenReturn(Optional.empty());

        assertFalse(autorizacionService.validarVendedor(7));
    }

    @Test
    void validarVendedorInexistenteLanzaExcepcion() {
        when(vendedorService.obtenerVendedor(99)).thenThrow(new RecursoNoEncontradoException("Vendedor", 99));

        assertThrows(RecursoNoEncontradoException.class, () -> autorizacionService.validarVendedor(99));
    }

    @Test
    void autorizarCreaUnNuevoRegistroAutorizado() {
        when(vendedorService.obtenerVendedor(7)).thenReturn(vendedor);
        when(autorizacionRepository.findFirstByVendedorIdOrderByFechaDescIdDesc(7))
                .thenReturn(Optional.of(registro(AutorizacionRRHH.ESTADO_REVOCADA)));
        when(autorizacionRepository.save(any(AutorizacionRRHH.class))).thenAnswer(inv -> inv.getArgument(0));

        AutorizacionRRHH nueva = autorizacionService.autorizarVendedor(7);

        assertEquals(AutorizacionRRHH.ESTADO_AUTORIZADA, nueva.getEstado());
        assertEquals(LocalDate.now(), nueva.getFecha());
        assertSame(vendedor, nueva.getVendedor());
        verify(autorizacionRepository).save(nueva);
    }

    @Test
    void autorizarVendedorYaAutorizadoSeRechaza() {
        when(vendedorService.obtenerVendedor(7)).thenReturn(vendedor);
        when(autorizacionRepository.findFirstByVendedorIdOrderByFechaDescIdDesc(7))
                .thenReturn(Optional.of(registro(AutorizacionRRHH.ESTADO_AUTORIZADA)));

        assertThrows(OperacionNoPermitidaException.class, () -> autorizacionService.autorizarVendedor(7));
        verify(autorizacionRepository, never()).save(any());
    }

    @Test
    void revocarCreaUnNuevoRegistroRevocado() {
        when(vendedorService.obtenerVendedor(7)).thenReturn(vendedor);
        when(autorizacionRepository.findFirstByVendedorIdOrderByFechaDescIdDesc(7))
                .thenReturn(Optional.of(registro(AutorizacionRRHH.ESTADO_AUTORIZADA)));
        when(autorizacionRepository.save(any(AutorizacionRRHH.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(AutorizacionRRHH.ESTADO_REVOCADA, autorizacionService.revocarAutorizacion(7).getEstado());
    }

    @Test
    void revocarVendedorSinAutorizacionVigenteSeRechaza() {
        when(vendedorService.obtenerVendedor(7)).thenReturn(vendedor);
        when(autorizacionRepository.findFirstByVendedorIdOrderByFechaDescIdDesc(7)).thenReturn(Optional.empty());

        assertThrows(OperacionNoPermitidaException.class, () -> autorizacionService.revocarAutorizacion(7));
        verify(autorizacionRepository, never()).save(any());
    }

    @Test
    void consultarHistorialUsaElOrdenDelRepositorio() {
        List<AutorizacionRRHH> historial = List.of(registro(AutorizacionRRHH.ESTADO_REVOCADA));
        when(vendedorService.obtenerVendedor(7)).thenReturn(vendedor);
        when(autorizacionRepository.findByVendedorIdOrderByFechaDescIdDesc(7)).thenReturn(historial);

        assertEquals(historial, autorizacionService.consultarHistorial(7));
    }
}
