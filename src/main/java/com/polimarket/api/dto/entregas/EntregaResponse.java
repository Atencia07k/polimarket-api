package com.polimarket.api.dto.entregas;

import com.polimarket.api.entity.Entrega;
import java.time.LocalDate;

public record EntregaResponse(Integer id, LocalDate fecha, String estado, String direccion, Integer idVenta) {

    public static EntregaResponse desde(Entrega entrega) {
        return new EntregaResponse(entrega.getId(), entrega.getFecha(), entrega.getEstado(), entrega.getDireccion(),
                entrega.getVenta().getId());
    }
}
