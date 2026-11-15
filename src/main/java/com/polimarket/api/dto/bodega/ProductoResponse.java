package com.polimarket.api.dto.bodega;

import com.polimarket.api.entity.Producto;
import com.polimarket.api.entity.Proveedor;
import java.util.List;

public record ProductoResponse(Integer id, String nombre, double precio, List<String> nitsProveedores) {

    public static ProductoResponse desde(Producto producto) {
        List<String> nits = producto.getProveedores().stream().map(Proveedor::getNit).sorted().toList();
        return new ProductoResponse(producto.getId(), producto.getNombre(), producto.getPrecio(), nits);
    }
}
