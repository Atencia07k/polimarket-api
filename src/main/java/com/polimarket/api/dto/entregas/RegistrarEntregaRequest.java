package com.polimarket.api.dto.entregas;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RegistrarEntregaRequest(@NotNull @Positive Integer idVenta, @NotBlank @Size(max = 255) String direccion) {
}
