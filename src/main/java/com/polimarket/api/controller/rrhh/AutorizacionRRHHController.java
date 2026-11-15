package com.polimarket.api.controller.rrhh;

import com.polimarket.api.dto.rrhh.AutorizacionResponse;
import com.polimarket.api.dto.rrhh.AutorizarVendedorRequest;
import com.polimarket.api.dto.rrhh.EstadoAutorizacionResponse;
import com.polimarket.api.service.rrhh.AutorizacionRRHHService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autorizacion comercial de Recursos Humanos (regla de negocio, no seguridad tecnica).
 */
@RestController
@RequestMapping(path = "/api/v1/autorizaciones", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AutorizacionRRHHController {

    private final AutorizacionRRHHService autorizacionService;

    @GetMapping("/vendedores/{idVendedor}")
    public ResponseEntity<EstadoAutorizacionResponse> verificar(@PathVariable Integer idVendedor) {
        return ResponseEntity.ok(new EstadoAutorizacionResponse(idVendedor,
                autorizacionService.validarVendedor(idVendedor)));
    }

    @GetMapping("/vendedores/{idVendedor}/historial")
    public ResponseEntity<List<AutorizacionResponse>> historial(@PathVariable Integer idVendedor) {
        return ResponseEntity.ok(autorizacionService.consultarHistorial(idVendedor).stream()
                .map(AutorizacionResponse::desde).toList());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<AutorizacionResponse> autorizar(@Valid @RequestBody AutorizarVendedorRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                AutorizacionResponse.desde(autorizacionService.autorizarVendedor(solicitud.idVendedor())));
    }

    @PatchMapping("/vendedores/{idVendedor}/revocacion")
    @Transactional
    public ResponseEntity<AutorizacionResponse> revocar(@PathVariable Integer idVendedor) {
        return ResponseEntity.ok(AutorizacionResponse.desde(autorizacionService.revocarAutorizacion(idVendedor)));
    }
}
