package com.polimarket.api.repository.bodega;

import com.polimarket.api.entity.MovimientoStock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link MovimientoStock}. Las relaciones con Entrega y CompraProveedor son opcionales.
 */
public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Integer> {

    List<MovimientoStock> findByInventarioId(Integer inventarioId);

    List<MovimientoStock> findByTipo(String tipo);

    /** Rango inclusivo en ambos extremos. */
    List<MovimientoStock> findByFechaBetween(LocalDate desde, LocalDate hasta);

    List<MovimientoStock> findByEntregaId(Integer entregaId);

    List<MovimientoStock> findByCompraId(Integer compraId);
}
