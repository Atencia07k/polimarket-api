package com.polimarket.api.controller.autenticacion;

import com.polimarket.api.dto.autenticacion.CrearUsuarioRequest;
import com.polimarket.api.dto.autenticacion.UsuarioResponse;
import com.polimarket.api.entity.Usuario;
import com.polimarket.api.service.autenticacion.UsuarioService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Administracion de usuarios de la API (solo ADMIN, segun SecurityConfig).
 */
@RestController
@RequestMapping(path = "/api/v1/usuarios", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioRequest solicitud) {
        Usuario usuario = usuarioService.crearUsuario(solicitud.username(), solicitud.password(), solicitud.rol());
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}").buildAndExpand(usuario.getId()).toUri();
        return ResponseEntity.created(ubicacion).body(UsuarioResponse.desde(usuario));
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listarUsuarios().stream().map(UsuarioResponse::desde).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(UsuarioResponse.desde(usuarioService.obtenerUsuario(id)));
    }

    @PatchMapping("/{id}/desactivacion")
    public ResponseEntity<UsuarioResponse> desactivar(@PathVariable Integer id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(UsuarioResponse.desde(usuarioService.desactivarUsuario(id, jwt.getSubject())));
    }
}
