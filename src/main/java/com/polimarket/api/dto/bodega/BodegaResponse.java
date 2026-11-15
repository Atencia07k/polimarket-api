package com.polimarket.api.dto.bodega;

import com.polimarket.api.entity.Bodega;

public record BodegaResponse(Integer id, String ubicacion) {

    public static BodegaResponse desde(Bodega bodega) {
        return new BodegaResponse(bodega.getId(), bodega.getUbicacion());
    }
}
