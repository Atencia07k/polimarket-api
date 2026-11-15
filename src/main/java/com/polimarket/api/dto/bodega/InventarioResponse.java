package com.polimarket.api.dto.bodega;

import com.polimarket.api.entity.Inventario;

public record InventarioResponse(Integer id, Integer idProducto, String nombreProducto, Integer idBodega,
                                 String ubicacionBodega, int stock, int stockMinimo, boolean requiereReposicion) {

    public static InventarioResponse desde(Inventario inventario) {
        return new InventarioResponse(inventario.getId(), inventario.getProducto().getId(),
                inventario.getProducto().getNombre(), inventario.getBodega().getId(),
                inventario.getBodega().getUbicacion(), inventario.getStock(), inventario.getStockMinimo(),
                inventario.requiereReposicion());
    }
}
