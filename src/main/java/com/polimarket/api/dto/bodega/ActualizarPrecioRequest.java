package com.polimarket.api.dto.bodega;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ActualizarPrecioRequest(@NotNull @PositiveOrZero Double precio) {
}
