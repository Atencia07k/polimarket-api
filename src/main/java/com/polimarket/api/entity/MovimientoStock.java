package com.polimarket.api.entity;

import jakarta.persistence.CheckConstraint;
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
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "movimiento_stock", check = {
        @CheckConstraint(name = "ck_movimiento_stock_tipo",
                constraint = "tipo IN ('ENTRADA', 'SALIDA')"),
        @CheckConstraint(name = "ck_movimiento_stock_origen",
                constraint = "(compra_proveedor_id IS NULL OR tipo = 'ENTRADA') AND (entrega_id IS NULL OR tipo = 'SALIDA')")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoStock {

    public static final String TIPO_ENTRADA = "ENTRADA";
    public static final String TIPO_SALIDA = "SALIDA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @NotBlank
    @Pattern(regexp = TIPO_ENTRADA + "|" + TIPO_SALIDA)
    @Column(name = "tipo", nullable = false)
    private String tipo;

    @Positive
    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    @NotNull
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventario_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_movimiento_stock_inventario"))
    private Inventario inventario;

    // "registra salida": opcional, solo para movimientos de tipo SALIDA.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entrega_id", foreignKey = @ForeignKey(name = "fk_movimiento_stock_entrega"))
    private Entrega entrega;

    // "registra entrada": opcional, solo para movimientos de tipo ENTRADA.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_proveedor_id",
            foreignKey = @ForeignKey(name = "fk_movimiento_stock_compra_proveedor"))
    private CompraProveedor compra;

    /**
     * Aplica el movimiento al inventario: ENTRADA suma y SALIDA resta la cantidad.
     */
    public void registrar() {
        if (inventario == null) {
            throw new IllegalStateException("El movimiento no tiene un inventario asociado");
        }
        if (esEntrada()) {
            inventario.actualizarStock(cantidad);
        } else if (TIPO_SALIDA.equals(tipo)) {
            inventario.actualizarStock(-cantidad);
        } else {
            throw new IllegalStateException("Tipo de movimiento no valido: " + tipo);
        }
    }

    public boolean esEntrada() {
        return TIPO_ENTRADA.equals(tipo);
    }

    @AssertTrue(message = "Una ENTRADA solo puede asociarse a una compra y una SALIDA solo a una entrega")
    public boolean isOrigenValido() {
        return (compra == null || esEntrada()) && (entrega == null || TIPO_SALIDA.equals(tipo));
    }
}