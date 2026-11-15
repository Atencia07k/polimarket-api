package com.polimarket.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Entity
@Table(name = "vendedor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vendedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @NotBlank
    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Builder.Default
    @OneToMany(mappedBy = "vendedor")
    private List<AutorizacionRRHH> autorizaciones = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "vendedor")
    private List<Venta> ventas = new ArrayList<>();

    /**
     * Un vendedor esta autorizado cuando su autorizacion mas reciente (por fecha; a igual fecha,
     * la de mayor id) esta vigente.
     */
    public boolean estaAutorizado() {
        return autorizaciones.stream()
                .max(Comparator.comparing(AutorizacionRRHH::getFecha, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(AutorizacionRRHH::getId, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(AutorizacionRRHH::estaVigente)
                .orElse(false);
    }
}