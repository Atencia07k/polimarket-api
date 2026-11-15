package com.polimarket.api.controller;

import com.polimarket.api.dto.SaludoResponse;
import com.polimarket.api.service.SaludoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SaludoController {

    private final SaludoService saludoService;

    @GetMapping(path = "/hola", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SaludoResponse> hola() {
        return ResponseEntity.ok(saludoService.obtenerSaludo());
    }
}