package com.polimarket.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
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
@Table(name = "producto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @NotBlank
    @Column(name = "nombre", nullable = false)
    private String nombre;

    @PositiveOrZero
    @Column(name = "precio", nullable = false)
    private double precio;

    // Lado propietario de la relacion muchos a muchos "suministra" (Proveedor 1..*).
    @NotEmpty
    @Builder.Default
    @ManyToMany
    @JoinTable(name = "producto_proveedor",
            joinColumns = @JoinColumn(name = "producto_id",
                    foreignKey = @ForeignKey(name = "fk_producto_proveedor_producto")),
            inverseJoinColumns = @JoinColumn(name = "proveedor_nit",
                    foreignKey = @ForeignKey(name = "fk_producto_proveedor_proveedor")))
    private Set<Proveedor> proveedores = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "producto")
    private List<Inventario> inventarios = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "producto")
    private List<DetalleVenta> detallesVenta = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "producto")
    private List<DetalleCompra> detallesCompra = new ArrayList<>();

    public void agregarProveedor(Proveedor proveedor) {
        proveedores.add(proveedor);
        proveedor.getProductos().add(this);
    }

    public void actualizarPrecio(double precio) {
        if (Double.isNaN(precio) || precio < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
        this.precio = precio;
    }
}