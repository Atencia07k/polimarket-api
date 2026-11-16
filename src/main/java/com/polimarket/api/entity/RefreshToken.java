package com.polimarket.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Refresh token rotativo. Se guarda solo el hash SHA-256 del valor entregado al cliente.
 * Todos los tokens obtenidos a partir de un mismo login comparten la {@code familia}: si se reutiliza
 * un token ya rotado, se revoca la familia completa.
 */
@Entity
@Table(name = "refresh_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"usuario", "tokenHash"})
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_refresh_token_usuario"))
    private Usuario usuario;

    @NotBlank
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @NotNull
    @Column(name = "familia", nullable = false)
    private UUID familia;

    @NotNull
    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Builder.Default
    @Column(name = "revocado", nullable = false)
    private boolean revocado = false;

    @NotNull
    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    public boolean estaExpirado(Instant ahora) {
        return !expiraEn.isAfter(ahora);
    }
}
