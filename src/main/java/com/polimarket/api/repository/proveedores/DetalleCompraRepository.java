package com.polimarket.api.repository.proveedores;

import com.polimarket.api.entity.DetalleCompra;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link DetalleCompra}.
 */
public interface DetalleCompraRepository extends JpaRepository<DetalleCompra, Integer> {

    List<DetalleCompra> findByCompraId(Integer compraId);

    List<DetalleCompra> findByProductoId(Integer productoId);
}
