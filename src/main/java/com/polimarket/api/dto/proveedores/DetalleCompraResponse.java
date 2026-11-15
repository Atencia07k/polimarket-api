package com.polimarket.api.dto.proveedores;

import com.polimarket.api.entity.DetalleCompra;

public record DetalleCompraResponse(Integer idProducto, String nombreProducto, int cantidad, double precio,
                                    double subtotal) {

    public static DetalleCompraResponse desde(DetalleCompra detalle) {
        return new DetalleCompraResponse(detalle.getProducto().getId(), detalle.getProducto().getNombre(),
                detalle.getCantidad(), detalle.getPrecio(), detalle.subtotal());
    }
}
