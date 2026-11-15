package com.polimarket.api.dto.proveedores;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarContactoRequest(@NotBlank @Size(max = 255) String contacto) {
}
