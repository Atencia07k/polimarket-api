package com.polimarket.api.repository.rrhh;

import com.polimarket.api.entity.Vendedor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link Vendedor}. La autorizacion se consulta en {@link AutorizacionRRHHRepository}.
 */
public interface VendedorRepository extends JpaRepository<Vendedor, Integer> {

    List<Vendedor> findByNombreContainingIgnoreCase(String nombre);
}
