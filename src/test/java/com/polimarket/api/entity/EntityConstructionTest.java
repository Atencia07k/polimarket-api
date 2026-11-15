package com.polimarket.api.entity;

import jakarta.persistence.Entity;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que Lombok genere constructores compatibles con JPA y que las colecciones de las
 * relaciones queden inicializadas tanto con el constructor vacio como con el builder.
 */
class EntityConstructionTest {

    @ParameterizedTest
    @ValueSource(classes = {
            Vendedor.class, AutorizacionRRHH.class, Cliente.class, Venta.class, DetalleVenta.class,
            Producto.class, Bodega.class, Inventario.class, MovimientoStock.class, Proveedor.class,
            CompraProveedor.class, DetalleCompra.class, Entrega.class})
    void entidadEsConstruiblePorJpaYPorBuilder(Class<?> tipo) throws Exception {
        assertThat(tipo.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(Modifier.isFinal(tipo.getModifiers())).isFalse();

        Constructor<?> constructorVacio = tipo.getDeclaredConstructor();
        assertThat(Modifier.isPublic(constructorVacio.getModifiers())
                || Modifier.isProtected(constructorVacio.getModifiers())).isTrue();

        Object desdeConstructor = constructorVacio.newInstance();
        Object builder = tipo.getMethod("builder").invoke(null);
        Object desdeBuilder = builder.getClass().getMethod("build").invoke(builder);

        for (Field campo : tipo.getDeclaredFields()) {
            if (Collection.class.isAssignableFrom(campo.getType())) {
                campo.setAccessible(true);
                assertThat(campo.get(desdeConstructor)).as("%s.%s (constructor)", tipo.getSimpleName(), campo.getName()).isNotNull();
                assertThat(campo.get(desdeBuilder)).as("%s.%s (builder)", tipo.getSimpleName(), campo.getName()).isNotNull();
            }
        }
    }
}