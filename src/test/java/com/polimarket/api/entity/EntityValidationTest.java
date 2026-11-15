package com.polimarket.api.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EntityValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void iniciar() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrar() {
        factory.close();
    }

    @Test
    void ventaSinDetallesIncumpleLaMultiplicidadUnoAMuchos() {
        Venta venta = Venta.builder().fecha(LocalDate.now()).estado("REGISTRADA")
                .vendedor(new Vendedor()).cliente(new Cliente()).build();

        assertThat(propiedadesInvalidas(venta)).containsExactly("detalles");

        venta.agregarDetalle(new DetalleVenta());
        assertThat(validator.validate(venta)).isEmpty();
    }

    @Test
    void productoRequiereAlMenosUnProveedor() {
        Producto producto = Producto.builder().nombre("Arroz").precio(3500).build();

        assertThat(propiedadesInvalidas(producto)).containsExactly("proveedores");
    }

    @Test
    void cantidadesYExistenciasNoAceptanNegativos() {
        Inventario inventario = Inventario.builder().stock(-1).stockMinimo(-2)
                .bodega(new Bodega()).producto(new Producto()).build();
        DetalleCompra detalle = DetalleCompra.builder().cantidad(0).precio(-5)
                .compra(new CompraProveedor()).producto(new Producto()).build();

        assertThat(propiedadesInvalidas(inventario)).containsExactlyInAnyOrder("stock", "stockMinimo");
        assertThat(propiedadesInvalidas(detalle)).containsExactlyInAnyOrder("cantidad", "precio");
    }

    @Test
    void movimientoDeEntradaPuedeAsociarseAUnaCompra() {
        MovimientoStock entrada = movimiento(MovimientoStock.TIPO_ENTRADA);
        entrada.setCompra(new CompraProveedor());

        assertThat(validator.validate(entrada)).isEmpty();
    }

    @Test
    void movimientoSinOrigenEsValido() {
        assertThat(validator.validate(movimiento(MovimientoStock.TIPO_SALIDA))).isEmpty();
    }

    @Test
    void movimientoDeEntradaNoPuedeAsociarseAUnaEntrega() {
        MovimientoStock entrada = movimiento(MovimientoStock.TIPO_ENTRADA);
        entrada.setEntrega(new Entrega());

        assertThat(propiedadesInvalidas(entrada)).containsExactly("origenValido");
    }

    @Test
    void movimientoDeSalidaNoPuedeAsociarseAUnaCompra() {
        MovimientoStock salida = movimiento(MovimientoStock.TIPO_SALIDA);
        salida.setCompra(new CompraProveedor());

        assertThat(propiedadesInvalidas(salida)).containsExactly("origenValido");
    }

    @Test
    void tipoDeMovimientoDebeSerEntradaOSalida() {
        assertThat(propiedadesInvalidas(movimiento("AJUSTE"))).containsExactly("tipo");
    }

    private static MovimientoStock movimiento(String tipo) {
        return MovimientoStock.builder().tipo(tipo).cantidad(1).fecha(LocalDate.now())
                .inventario(new Inventario()).build();
    }

    private static <T> Set<String> propiedadesInvalidas(T entidad) {
        Set<ConstraintViolation<T>> violaciones = validator.validate(entidad);
        return violaciones.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }
}