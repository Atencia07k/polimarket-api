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
import java.util.List;

// registrarIngreso() y registrarSalida() del UML quedan pendientes hasta definir sus parametros.
@Entity
@Table(name = "bodega")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bodega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @NotBlank
    @Column(name = "ubicacion", nullable = false)
    private String ubicacion;

    // Agregacion: los registros de inventario no se eliminan en cascada.
    @Builder.Default
    @OneToMany(mappedBy = "bodega")
    private List<Inventario> inventarios = new ArrayList<>();

    /**
     * Total de unidades disponibles en la bodega (suma del stock de sus inventarios).
     */
    public int consultarDisponibilidad() {
        return inventarios.stream().mapToInt(Inventario::getStock).sum();
    }
}