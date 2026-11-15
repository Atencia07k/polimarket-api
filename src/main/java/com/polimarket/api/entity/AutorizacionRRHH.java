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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "autorizacion_rrhh")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutorizacionRRHH {

    public static final String ESTADO_AUTORIZADA = "AUTORIZADA";
    public static final String ESTADO_REVOCADA = "REVOCADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @NotNull
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @NotBlank
    @Column(name = "estado", nullable = false)
    private String estado;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendedor_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_autorizacion_rrhh_vendedor"))
    private Vendedor vendedor;

    public void autorizar() {
        this.estado = ESTADO_AUTORIZADA;
        this.fecha = LocalDate.now();
    }

    public void revocar() {
        this.estado = ESTADO_REVOCADA;
        this.fecha = LocalDate.now();
    }

    public boolean estaVigente() {
        return ESTADO_AUTORIZADA.equals(estado);
    }
}