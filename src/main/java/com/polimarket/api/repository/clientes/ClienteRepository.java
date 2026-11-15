package com.polimarket.api.repository.clientes;

import com.polimarket.api.entity.Cliente;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link Cliente}. Lo reutiliza Ventas a traves de la capa Service.
 */
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    List<Cliente> findByNombreContainingIgnoreCase(String nombre);
}
