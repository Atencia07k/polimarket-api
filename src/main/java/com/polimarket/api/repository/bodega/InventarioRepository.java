package com.polimarket.api.repository.bodega;

import com.polimarket.api.entity.Inventario;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/**
 * Acceso a datos de {@link Inventario}. Reutilizado por Ventas, Bodega, Proveedores y Entregas
 * mediante los servicios de Bodega. La actualizacion de existencias es responsabilidad de la capa Service.
 */
public interface InventarioRepository extends JpaRepository<Inventario, Integer> {

    /** La restriccion uk_inventario_bodega_producto garantiza a lo sumo un resultado. */
    Optional<Inventario> findByBodegaIdAndProductoId(Integer bodegaId, Integer productoId);

    /**
     * Igual que {@link #findByBodegaIdAndProductoId} pero con bloqueo pesimista (SELECT ... FOR UPDATE).
     * Debe invocarse dentro de una transaccion; serializa entradas y salidas sobre el mismo registro.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventario> findConBloqueoByBodegaIdAndProductoId(Integer bodegaId, Integer productoId);

    List<Inventario> findByProductoId(Integer productoId);

    List<Inventario> findByBodegaId(Integer bodegaId);

    boolean existsByBodegaIdAndProductoId(Integer bodegaId, Integer productoId);

    /** Misma regla que {@link Inventario#requiereReposicion()}: stock &lt;= stockMinimo del propio registro. */
    @Query("select i from Inventario i where i.stock <= i.stockMinimo")
    List<Inventario> findQueRequierenReposicion();
}
