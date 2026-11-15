package com.polimarket.api.dto.ventas;

import com.polimarket.api.entity.DetalleVenta;

public record DetalleVentaResponse(Integer idProducto, String nombreProducto, int cantidad, double precio,
                                   double subtotal) {

    public static DetalleVentaResponse desde(DetalleVenta detalle) {
        return new DetalleVentaResponse(detalle.getProducto().getId(), detalle.getProducto().getNombre(),
                detalle.getCantidad(), detalle.getPrecio(), detalle.subtotal());
    }
}
