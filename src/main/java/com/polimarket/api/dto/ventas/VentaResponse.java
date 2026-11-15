package com.polimarket.api.dto.ventas;

import com.polimarket.api.entity.Venta;
import java.time.LocalDate;
import java.util.List;

public record VentaResponse(Integer id, LocalDate fecha, String estado, double total, Integer idVendedor,
                            String nombreVendedor, Integer idCliente, String nombreCliente,
                            List<DetalleVentaResponse> detalles, Integer idEntrega) {

    public static VentaResponse desde(Venta venta) {
        return new VentaResponse(venta.getId(), venta.getFecha(), venta.getEstado(), venta.getTotal(),
                venta.getVendedor().getId(), venta.getVendedor().getNombre(),
                venta.getCliente().getId(), venta.getCliente().getNombre(),
                venta.getDetalles().stream().map(DetalleVentaResponse::desde).toList(),
                venta.getEntrega() == null ? null : venta.getEntrega().getId());
    }
}
