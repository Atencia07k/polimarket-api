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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inventario", uniqueConstraints = @UniqueConstraint(
        name = "uk_inventario_bodega_producto", columnNames = {"bodega_id", "producto_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventario {

    // Identificador tecnico requerido por JPA (no definido en el UML).
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @PositiveOrZero
    @Column(name = "stock", nullable = false)
    private int stock;

    @PositiveOrZero
    @Column(name = "stock_minimo", nullable = false)
    private int stockMinimo;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bodega_id", nullable = false, foreignKey = @ForeignKey(name = "fk_inventario_bodega"))
    private Bodega bodega;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false, foreignKey = @ForeignKey(name = "fk_inventario_producto"))
    private Producto producto;

    @Builder.Default
    @OneToMany(mappedBy = "inventario")
    private List<MovimientoStock> movimientos = new ArrayList<>();

    /**
     * Ajusta el stock: cantidad positiva suma (ingreso) y negativa resta (salida).
     *
     * @throws IllegalArgumentException si el stock resultante seria negativo
     */
    public void actualizarStock(int cantidad) {
        int nuevoStock = Math.addExact(stock, cantidad);
        if (nuevoStock < 0) {
            throw new IllegalArgumentException(
                    "Stock insuficiente: existencia " + stock + ", ajuste solicitado " + cantidad);
        }
        this.stock = nuevoStock;
    }

    public int consultarExistencia() {
        return stock;
    }

    public boolean requiereReposicion() {
        return stock <= stockMinimo;
    }
}