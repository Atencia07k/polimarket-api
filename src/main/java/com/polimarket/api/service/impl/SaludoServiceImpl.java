package com.polimarket.api.service.impl;

import com.polimarket.api.dto.SaludoResponse;
import com.polimarket.api.service.SaludoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SaludoServiceImpl implements SaludoService {

    static final String MENSAJE_SALUDO = "Hola mundo";

    @Override
    public SaludoResponse obtenerSaludo() {
        log.debug("Generando mensaje de saludo");
        return new SaludoResponse(MENSAJE_SALUDO);
    }
}