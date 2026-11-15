package com.polimarket.api.entity;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Valida el mapeo JPA construyendo el metamodelo de Hibernate con el dialecto PostgreSQL,
 * sin conexion a base de datos. El DDL resultante solo se escribe como script en
 * build/generated/ddl para revision; no se aplica en PostgreSQL.
 */
class JpaMappingTest {

    private static final List<Class<?>> ENTIDADES = List.of(
            Vendedor.class, AutorizacionRRHH.class, Cliente.class, Venta.class, DetalleVenta.class,
            Producto.class, Bodega.class, Inventario.class, MovimientoStock.class, Proveedor.class,
            CompraProveedor.class, DetalleCompra.class, Entrega.class);

    private static StandardServiceRegistry registry;
    private static Metadata metadata;
    private static String ddl;

    @BeforeAll
    static void construirMetamodelo() throws Exception {
        Path script = Path.of("build", "generated", "ddl", "polimarket-schema.sql").toAbsolutePath();
        Files.createDirectories(script.getParent());
        Files.deleteIfExists(script);

        // Solo genera script: la accion sobre la base de datos queda en "none".
        Map<String, Object> settings = Map.of(
                "hibernate.dialect", PostgreSQLDialect.class.getName(),
                "hibernate.boot.allow_jdbc_metadata_access", "false",
                "hibernate.format_sql", "true",
                "hibernate.hbm2ddl.delimiter", ";",
                "jakarta.persistence.schema-generation.database.action", "none",
                "jakarta.persistence.schema-generation.scripts.action", "create",
                "jakarta.persistence.schema-generation.scripts.create-target", script.toString());
        registry = new StandardServiceRegistryBuilder().applySettings(settings).build();
        MetadataSources sources = new MetadataSources(registry);
        ENTIDADES.forEach(sources::addAnnotatedClass);
        metadata = sources.buildMetadata();
        ((MetadataImplementor) metadata).validate();

        SchemaManagementToolCoordinator.process(metadata, registry, settings, action -> { });
        ddl = Files.readString(script).toLowerCase();
    }

    @AfterAll
    static void cerrar() {
        StandardServiceRegistryBuilder.destroy(registry);
    }

    @Test
    void lasTreceEntidadesEstanMapeadasConSusTablas() {
        assertThat(metadata.getEntityBindings().stream()
                .map(PersistentClass::getTable)
                .map(t -> t.getName())
                .collect(Collectors.toSet()))
                .containsExactlyInAnyOrder("vendedor", "autorizacion_rrhh", "cliente", "venta", "detalle_venta",
                        "producto", "bodega", "inventario", "movimiento_stock", "proveedor", "compra_proveedor",
                        "detalle_compra", "entrega");
    }

    @Test
    void elEsquemaIncluyeTablaIntermediaRestriccionesYLlavesForaneas() {
        assertThat(ddl).contains(
                "create table producto_proveedor",
                "uk_inventario_bodega_producto",
                "ck_movimiento_stock_tipo",
                "ck_movimiento_stock_origen",
                "fk_venta_vendedor",
                "fk_venta_cliente",
                "fk_detalle_venta_venta",
                "fk_detalle_venta_producto",
                "fk_entrega_venta",
                "fk_movimiento_stock_inventario",
                "fk_movimiento_stock_entrega",
                "fk_movimiento_stock_compra_proveedor",
                "fk_inventario_bodega",
                "fk_inventario_producto",
                "fk_producto_proveedor_producto",
                "fk_producto_proveedor_proveedor",
                "fk_compra_proveedor_proveedor",
                "fk_detalle_compra_compra_proveedor",
                "fk_detalle_compra_producto",
                "fk_autorizacion_rrhh_vendedor");
    }

    @Test
    void unaVentaTieneComoMaximoUnaEntrega() {
        // Hibernate 7 emite la unicidad del @OneToOne como restriccion de columna.
        assertThat(ddl).containsPattern("venta_id integer not null unique");
    }

    @Test
    void proveedorUsaElNitComoIdentificador() {
        assertThat(metadata.getEntityBinding(Proveedor.class.getName()).getIdentifierProperty().getName())
                .isEqualTo("nit");
    }
}