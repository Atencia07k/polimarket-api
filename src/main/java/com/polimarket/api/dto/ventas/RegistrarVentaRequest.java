package com.polimarket.api.dto.ventas;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record RegistrarVentaRequest(
        @NotNull @Positive Integer idVendedor,
        @NotNull @Positive Integer idCliente,
        @NotEmpty List<@NotNull @Valid ItemVentaRequest> items) {
}
