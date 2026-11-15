package com.polimarket.api.dto.entregas;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ConfirmarEntregaRequest(@NotNull @Positive Integer idBodega) {
}
