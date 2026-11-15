package com.polimarket.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "entrega", uniqueConstraints = @UniqueConstraint(name = "uk_entrega_venta", columnNames = "venta_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Entrega {

    public static final String ESTADO_ENTREGADA = "ENTREGADA";

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

    @NotBlank
    @Column(name = "direccion", nullable = false)
    private String direccion;

    // Lado propietario de la relacion uno a uno con Venta (cada entrega corresponde a una venta).
    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false, foreignKey = @ForeignKey(name = "fk_entrega_venta"))
    private Venta venta;

    @Builder.Default
    @OneToMany(mappedBy = "entrega")
    private List<MovimientoStock> movimientos = new ArrayList<>();

    public void registrarEntrega() {
        this.estado = ESTADO_ENTREGADA;
    }

    public void actualizarEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            throw new IllegalArgumentException("El estado es obligatorio");
        }
        this.estado = estado;
    }
}