package com.polimarket.api.controller;

import com.polimarket.api.dto.SaludoResponse;
import com.polimarket.api.service.SaludoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SaludoControllerTest {

    @Mock
    private SaludoService saludoService;

    @InjectMocks
    private SaludoController saludoController;

    @Test
    void holaDelegaEnElServicioYDevuelveSuRespuesta() {
        given(saludoService.obtenerSaludo()).willReturn(new SaludoResponse("Hola mundo"));

        ResponseEntity<SaludoResponse> respuesta = saludoController.hola();

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().mensaje()).isEqualTo("Hola mundo");
        verify(saludoService).obtenerSaludo();
    }
}