package com.polimarket.api.dto.proveedores;

import com.polimarket.api.entity.Proveedor;

public record ProveedorResponse(String nit, String nombre, String contacto) {

    public static ProveedorResponse desde(Proveedor proveedor) {
        return new ProveedorResponse(proveedor.getNit(), proveedor.getNombre(), proveedor.getContacto());
    }
}
