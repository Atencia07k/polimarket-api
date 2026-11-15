package com.polimarket.api.dto.rrhh;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AutorizarVendedorRequest(@NotNull @Positive Integer idVendedor) {
}
