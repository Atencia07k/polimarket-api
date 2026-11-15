package com.polimarket.api.repository.rrhh;

import com.polimarket.api.entity.AutorizacionRRHH;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link AutorizacionRRHH}.
 * <p>
 * El orden del historial es fecha descendente y, ante empate, id descendente.
 * El registro mas reciente no implica por si mismo una autorizacion vigente:
 * esa regla pertenece a la capa Service de Recursos Humanos.
 */
public interface AutorizacionRRHHRepository extends JpaRepository<AutorizacionRRHH, Integer> {

    List<AutorizacionRRHH> findByVendedorId(Integer vendedorId);

    List<AutorizacionRRHH> findByEstado(String estado);

    List<AutorizacionRRHH> findByVendedorIdOrderByFechaDescIdDesc(Integer vendedorId);

    Optional<AutorizacionRRHH> findFirstByVendedorIdOrderByFechaDescIdDesc(Integer vendedorId);
}
