package com.polimarket.api.repository.autenticacion;

import com.polimarket.api.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * Acceso a datos de {@link RefreshToken}. Las busquedas se hacen siempre por el hash del token.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /** Bloquea la fila para que dos peticiones concurrentes no roten el mismo token. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findConBloqueoByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshToken r set r.revocado = true where r.familia = :familia and r.revocado = false")
    int revocarFamilia(UUID familia);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshToken r set r.revocado = true where r.usuario.id = :idUsuario and r.revocado = false")
    int revocarPorUsuario(Integer idUsuario);
}
