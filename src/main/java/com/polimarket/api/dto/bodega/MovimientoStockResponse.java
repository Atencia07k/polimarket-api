package com.polimarket.api.dto.bodega;

import com.polimarket.api.entity.MovimientoStock;
import java.time.LocalDate;

public record MovimientoStockResponse(Integer id, String tipo, int cantidad, LocalDate fecha, Integer idInventario,
                                      Integer idProducto, Integer idBodega, Integer idCompra, Integer idEntrega) {

    public static MovimientoStockResponse desde(MovimientoStock movimiento) {
        return new MovimientoStockResponse(movimiento.getId(), movimiento.getTipo(), movimiento.getCantidad(),
                movimiento.getFecha(), movimiento.getInventario().getId(),
                movimiento.getInventario().getProducto().getId(), movimiento.getInventario().getBodega().getId(),
                movimiento.getCompra() == null ? null : movimiento.getCompra().getId(),
                movimiento.getEntrega() == null ? null : movimiento.getEntrega().getId());
    }
}
