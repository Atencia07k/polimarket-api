package com.polimarket.api.dto.rrhh;

import com.polimarket.api.entity.AutorizacionRRHH;
import java.time.LocalDate;

public record AutorizacionResponse(Integer id, Integer idVendedor, String estado, LocalDate fecha) {

    public static AutorizacionResponse desde(AutorizacionRRHH autorizacion) {
        return new AutorizacionResponse(autorizacion.getId(), autorizacion.getVendedor().getId(),
                autorizacion.getEstado(), autorizacion.getFecha());
    }
}
