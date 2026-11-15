package com.polimarket.api.dto.rrhh;

import com.polimarket.api.entity.Vendedor;

public record VendedorResponse(Integer id, String nombre) {

    public static VendedorResponse desde(Vendedor vendedor) {
        return new VendedorResponse(vendedor.getId(), vendedor.getNombre());
    }
}
