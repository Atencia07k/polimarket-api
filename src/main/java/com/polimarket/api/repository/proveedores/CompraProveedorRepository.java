package com.polimarket.api.repository.proveedores;

import com.polimarket.api.entity.CompraProveedor;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/**
 * Acceso a datos de {@link CompraProveedor}. Los detalles se persisten en cascada desde la compra.
 */
public interface CompraProveedorRepository extends JpaRepository<CompraProveedor, Integer> {

    List<CompraProveedor> findByProveedorNit(String proveedorNit);

    List<CompraProveedor> findByEstado(String estado);

    /** Rango inclusivo en ambos extremos. */
    List<CompraProveedor> findByFechaBetween(LocalDate desde, LocalDate hasta);

    /**
     * Bloqueo pesimista (SELECT ... FOR UPDATE) para que dos recepciones simultaneas
     * de la misma compra no ingresen el inventario dos veces. Requiere transaccion.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CompraProveedor> findConBloqueoById(Integer id);
}
