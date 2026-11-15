package com.polimarket.api.repository.ventas;

import com.polimarket.api.entity.DetalleVenta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link DetalleVenta}.
 */
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Integer> {

    List<DetalleVenta> findByVentaId(Integer ventaId);

    List<DetalleVenta> findByProductoId(Integer productoId);
}
