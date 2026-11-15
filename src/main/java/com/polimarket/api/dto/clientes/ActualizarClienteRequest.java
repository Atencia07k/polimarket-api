package com.polimarket.api.dto.clientes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarClienteRequest(
        @NotBlank @Size(max = 255) String nombre,
        @NotBlank @Size(max = 255) String direccion) {
}
