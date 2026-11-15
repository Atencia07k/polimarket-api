package com.polimarket.api.dto.proveedores;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RecibirCompraRequest(@NotNull @Positive Integer idBodega) {
}
