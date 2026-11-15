package com.polimarket.api.repository.ventas;

import com.polimarket.api.entity.Venta;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link Venta}. Los detalles se persisten en cascada desde la venta.
 */
public interface VentaRepository extends JpaRepository<Venta, Integer> {

    List<Venta> findByVendedorId(Integer vendedorId);

    List<Venta> findByClienteId(Integer clienteId);

    List<Venta> findByEstado(String estado);

    /** Rango inclusivo en ambos extremos. */
    List<Venta> findByFechaBetween(LocalDate desde, LocalDate hasta);
}
