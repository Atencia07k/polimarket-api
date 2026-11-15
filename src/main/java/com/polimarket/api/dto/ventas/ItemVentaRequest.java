package com.polimarket.api.dto.ventas;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Solo producto y cantidad: el precio (catalogo vigente) y el total los define el servidor.
 */
public record ItemVentaRequest(@NotNull @Positive Integer idProducto, @NotNull @Positive Integer cantidad) {
}
