package com.polimarket.api.dto.clientes;

import com.polimarket.api.entity.Cliente;

public record ClienteResponse(Integer id, String nombre, String direccion) {

    public static ClienteResponse desde(Cliente cliente) {
        return new ClienteResponse(cliente.getId(), cliente.getNombre(), cliente.getDireccion());
    }
}
