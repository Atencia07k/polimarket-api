package com.polimarket.api.dto.autenticacion;

import com.polimarket.api.entity.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearUsuarioRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Rol rol) {

    @Override
    public String toString() {
        return "CrearUsuarioRequest[username=" + username + ", password=****, rol=" + rol + "]";
    }
}
