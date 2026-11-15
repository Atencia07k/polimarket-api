package com.polimarket.api.repository.entregas;

import com.polimarket.api.entity.Entrega;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/**
 * Acceso a datos de {@link Entrega}. No registra movimientos de inventario:
 * esa operacion se hara desde los servicios de Bodega.
 */
public interface EntregaRepository extends JpaRepository<Entrega, Integer> {

    List<Entrega> findByEstado(String estado);

    /** La restriccion uk_entrega_venta garantiza a lo sumo una entrega por venta. */
    Optional<Entrega> findByVentaId(Integer ventaId);

    /** Rango inclusivo en ambos extremos. */
    List<Entrega> findByFechaBetween(LocalDate desde, LocalDate hasta);

    /**
     * Bloqueo pesimista (SELECT ... FOR UPDATE) para que dos confirmaciones simultaneas
     * de la misma entrega no descuenten el inventario dos veces. Requiere transaccion.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Entrega> findConBloqueoById(Integer id);
}
