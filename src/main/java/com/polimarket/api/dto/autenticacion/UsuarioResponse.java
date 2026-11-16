package com.polimarket.api.dto.autenticacion;

import com.polimarket.api.entity.Rol;
import com.polimarket.api.entity.Usuario;
import java.time.Instant;

/** Datos publicos de un usuario: nunca incluye el hash de la contrasena. */
public record UsuarioResponse(Integer id, String username, Rol rol, boolean activo, Instant fechaCreacion) {

    public static UsuarioResponse desde(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getUsername(), usuario.getRol(), usuario.isActivo(),
                usuario.getFechaCreacion());
    }
}
