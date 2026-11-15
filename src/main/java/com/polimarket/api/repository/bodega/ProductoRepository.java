package com.polimarket.api.repository.bodega;

import com.polimarket.api.entity.Producto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link Producto}. Unico repositorio de productos del sistema:
 * Ventas, Proveedores y Entregas lo reutilizan a traves de los servicios de Bodega.
 */
public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    List<Producto> findByNombreContainingIgnoreCase(String nombre);

    /** Navega la relacion muchos-a-muchos producto_proveedor. */
    List<Producto> findByProveedoresNit(String proveedorNit);
}
