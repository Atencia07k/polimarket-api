package com.polimarket.api.service.clientes;

import com.polimarket.api.entity.Cliente;
import java.util.List;

/**
 * Componente Clientes: consulta y actualizacion de clientes. No crea ni elimina clientes.
 */
public interface ClienteService {

    List<Cliente> listarClientes();

    /** @throws com.polimarket.api.exception.RecursoNoEncontradoException si no existe */
    Cliente obtenerCliente(Integer idCliente);

    List<Cliente> buscarClientesPorNombre(String nombre);

    /**
     * Actualiza nombre y direccion de un cliente existente; nunca crea uno nuevo.
     *
     * @throws com.polimarket.api.exception.RecursoNoEncontradoException si no existe
     * @throws com.polimarket.api.exception.DatosInvalidosException si algun dato esta vacio
     */
    Cliente actualizarDatos(Integer idCliente, String nombre, String direccion);

    boolean existeCliente(Integer idCliente);
}
