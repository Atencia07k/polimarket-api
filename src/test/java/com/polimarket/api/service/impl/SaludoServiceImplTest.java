package com.polimarket.api.service.impl;

import com.polimarket.api.dto.SaludoResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SaludoServiceImplTest {

    private final SaludoServiceImpl saludoService = new SaludoServiceImpl();

    @Test
    void obtenerSaludoDevuelveHolaMundo() {
        SaludoResponse respuesta = saludoService.obtenerSaludo();

        assertThat(respuesta).isNotNull();
        assertThat(respuesta.mensaje()).isEqualTo("Hola mundo");
    }
}