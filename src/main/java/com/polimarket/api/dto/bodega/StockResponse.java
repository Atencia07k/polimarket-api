package com.polimarket.api.dto.bodega;

public record StockResponse(Integer idProducto, Integer idBodega, int stock, boolean requiereReposicion) {
}
