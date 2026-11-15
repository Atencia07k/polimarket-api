package com.polimarket.api.repository.bodega;

import com.polimarket.api.entity.Bodega;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link Bodega}.
 */
public interface BodegaRepository extends JpaRepository<Bodega, Integer> {

    /** La ubicacion no es unica en el esquema, por eso se retorna una lista. */
    List<Bodega> findByUbicacionContainingIgnoreCase(String ubicacion);
}
