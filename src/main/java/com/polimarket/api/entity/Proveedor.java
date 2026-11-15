package com.polimarket.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "proveedor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proveedor {

    // Identificador natural definido en el UML.
    @Id
    @NotBlank
    @Column(name = "nit", nullable = false)
    private String nit;

    @NotBlank
    @Column(name = "nombre", nullable = false)
    private String nombre;

    @NotBlank
    @Column(name = "contacto", nullable = false)
    private String contacto;

    @Builder.Default
    @ManyToMany(mappedBy = "proveedores")
    private Set<Producto> productos = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "proveedor")
    private List<CompraProveedor> compras = new ArrayList<>();

    public void actualizarContacto(String datos) {
        if (datos == null || datos.isBlank()) {
            throw new IllegalArgumentException("Los datos de contacto son obligatorios");
        }
        this.contacto = datos;
    }
}