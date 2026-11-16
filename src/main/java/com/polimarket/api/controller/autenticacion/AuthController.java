package com.polimarket.api.controller.autenticacion;

import com.polimarket.api.dto.autenticacion.LoginRequest;
import com.polimarket.api.dto.autenticacion.RefreshTokenRequest;
import com.polimarket.api.dto.autenticacion.TokenResponse;
import com.polimarket.api.dto.autenticacion.UsuarioResponse;
import com.polimarket.api.service.autenticacion.AuthService;
import com.polimarket.api.service.autenticacion.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autenticacion: login, renovacion (refresh token rotativo), logout y datos del usuario autenticado.
 */
@RestController
@RequestMapping(path = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest solicitud) {
        return ResponseEntity.ok(TokenResponse.desde(
                authService.iniciarSesion(solicitud.username(), solicitud.password())));
    }

    @PostMapping(path = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TokenResponse> refrescar(@Valid @RequestBody RefreshTokenRequest solicitud) {
        return ResponseEntity.ok(TokenResponse.desde(authService.refrescar(solicitud.refreshToken())));
    }

    @PostMapping(path = "/logout", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest solicitud) {
        authService.cerrarSesion(solicitud.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> usuarioActual(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(UsuarioResponse.desde(usuarioService.obtenerPorUsername(jwt.getSubject())));
    }
}
