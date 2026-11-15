package com.polimarket.api.dto.proveedores;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * El precio es el costo acordado con el proveedor (regla aprobada); el total lo calcula el servidor.
 */
public record ItemCompraRequest(
        @NotNull @Positive Integer idProducto,
        @NotNull @Positive Integer cantidad,
        @NotNull @PositiveOrZero Double precio) {
}
