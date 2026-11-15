package com.polimarket.api.repository.proveedores;

import com.polimarket.api.entity.Proveedor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link Proveedor}. El identificador es el NIT asignado manualmente,
 * por lo que {@code save()} sobre un NIT existente actualiza (merge) en lugar de insertar.
 */
public interface ProveedorRepository extends JpaRepository<Proveedor, String> {

    List<Proveedor> findByNombreContainingIgnoreCase(String nombre);

    /** Navega el lado inverso de la relacion muchos-a-muchos producto_proveedor. */
    List<Proveedor> findByProductosId(Integer productoId);
}
