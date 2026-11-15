package com.polimarket.api.dto.proveedores;

import com.polimarket.api.entity.CompraProveedor;
import java.time.LocalDate;
import java.util.List;

public record CompraResponse(Integer id, LocalDate fecha, String estado, String nitProveedor, String nombreProveedor,
                             double total, List<DetalleCompraResponse> detalles) {

    public static CompraResponse desde(CompraProveedor compra) {
        return new CompraResponse(compra.getId(), compra.getFecha(), compra.getEstado(),
                compra.getProveedor().getNit(), compra.getProveedor().getNombre(), compra.calcularTotal(),
                compra.getDetalles().stream().map(DetalleCompraResponse::desde).toList());
    }
}
