-- PoliMarket: datos semilla para desarrollo y pruebas manuales (solo perfil dev).
-- Migracion repetible de Flyway: se ejecuta despues de las versionadas y vuelve a ejecutarse
-- si este archivo cambia. Es idempotente (ON CONFLICT DO NOTHING): nunca sobrescribe datos existentes.
-- No crea usuarios de la API: el primer ADMIN se define con ADMIN_USERNAME / ADMIN_PASSWORD en .env.
--
-- Escenarios incluidos:
--   RRHH        vendedores 1 y 2 autorizados; 3 autorizado y luego revocado; 4 sin autorizacion.
--   Bodega      inventarios 3, 6 y 8 por debajo del stock minimo (consulta de reposicion).
--   Compras     1 RECIBIDA (genero movimientos ENTRADA), 2 y 4 PENDIENTE, 3 CANCELADA.
--   Ventas      1 con entrega ENTREGADA (genero movimientos SALIDA), 2 con entrega PENDIENTE,
--               3 y 5 REGISTRADA sin entrega, 4 CANCELADA.

-- RRHH -----------------------------------------------------------------------------------------
INSERT INTO vendedor (id, nombre) VALUES
    (1, 'Luis Gomez'),
    (2, 'Maria Rodriguez'),
    (3, 'Carlos Perez'),
    (4, 'Laura Martinez')
ON CONFLICT DO NOTHING;

INSERT INTO autorizacion_rrhh (id, estado, fecha, vendedor_id) VALUES
    (1, 'AUTORIZADA', DATE '2026-11-01', 1),
    (2, 'AUTORIZADA', DATE '2026-11-01', 2),
    (3, 'AUTORIZADA', DATE '2026-11-01', 3),
    (4, 'REVOCADA',   DATE '2026-11-10', 3)
ON CONFLICT DO NOTHING;

-- Clientes -------------------------------------------------------------------------------------
INSERT INTO cliente (id, nombre, direccion) VALUES
    (1, 'Ana Torres',       'Calle 10 # 5-20, Bogota'),
    (2, 'Jorge Ramirez',    'Carrera 45 # 30-12, Medellin'),
    (3, 'Sofia Castro',     'Avenida 6N # 23-50, Cali'),
    (4, 'Andres Morales',   'Calle 72 # 11-40, Bogota'),
    (5, 'Valentina Rios',   'Carrera 70 # 1-15, Medellin')
ON CONFLICT DO NOTHING;

-- Proveedores ----------------------------------------------------------------------------------
INSERT INTO proveedor (nit, nombre, contacto) VALUES
    ('900123456-7', 'Distribuidora Andina S.A.S.', 'ventas@andina.co'),
    ('800987654-3', 'Lacteos del Valle Ltda.',     'pedidos@lacteosvalle.co'),
    ('901555222-1', 'Aseo Total S.A.S.',           'contacto@aseototal.co')
ON CONFLICT DO NOTHING;

-- Bodega: productos, bodegas e inventario ------------------------------------------------------
INSERT INTO producto (id, nombre, precio) VALUES
    (1, 'Arroz 1 kg',               3500),
    (2, 'Aceite de girasol 1 L',   12000),
    (3, 'Azucar 1 kg',              4200),
    (4, 'Cafe molido 500 g',       15800),
    (5, 'Leche entera 1 L',         4500),
    (6, 'Queso campesino 500 g',   11000),
    (7, 'Detergente en polvo 1 kg', 9800),
    (8, 'Jabon de loza 500 g',      5600)
ON CONFLICT DO NOTHING;

INSERT INTO producto_proveedor (producto_id, proveedor_nit) VALUES
    (1, '900123456-7'),
    (2, '900123456-7'),
    (3, '900123456-7'),
    (4, '900123456-7'),
    (5, '800987654-3'),
    (6, '800987654-3'),
    (7, '901555222-1'),
    (8, '901555222-1'),
    (8, '900123456-7')
ON CONFLICT DO NOTHING;

INSERT INTO bodega (id, ubicacion) VALUES
    (1, 'Bogota - Centro'),
    (2, 'Medellin - Belen'),
    (3, 'Cali - Norte')
ON CONFLICT DO NOTHING;

INSERT INTO inventario (id, producto_id, bodega_id, stock, stock_minimo) VALUES
    (1,  1, 1, 120, 20),
    (2,  2, 1,  60, 15),
    (3,  3, 1,   8, 10),
    (4,  4, 1,  25,  5),
    (5,  5, 2,  40, 30),
    (6,  6, 2,   4,  5),
    (7,  7, 3,  35, 10),
    (8,  8, 3,   3,  8),
    (9,  1, 2,  45, 10),
    (10, 5, 1,  70, 20)
ON CONFLICT DO NOTHING;

-- Compras a proveedores ------------------------------------------------------------------------
INSERT INTO compra_proveedor (id, estado, fecha, proveedor_nit) VALUES
    (1, 'RECIBIDA',  DATE '2026-11-03', '900123456-7'),
    (2, 'PENDIENTE', DATE '2026-11-12', '800987654-3'),
    (3, 'CANCELADA', DATE '2026-11-08', '901555222-1'),
    (4, 'PENDIENTE', DATE '2026-11-14', '901555222-1')
ON CONFLICT DO NOTHING;

INSERT INTO detalle_compra (id, compra_proveedor_id, producto_id, cantidad, precio) VALUES
    (1, 1, 1,  50, 2800),
    (2, 1, 2,  30, 9500),
    (3, 2, 5, 100, 3200),
    (4, 2, 6,  40, 8000),
    (5, 3, 7,  20, 7000),
    (6, 4, 8,  30, 4000)
ON CONFLICT DO NOTHING;

-- Ventas (el total es la suma de cantidad * precio de sus detalles) ----------------------------
INSERT INTO venta (id, estado, fecha, total, vendedor_id, cliente_id) VALUES
    (1, 'REGISTRADA', DATE '2026-11-05', 34500, 1, 1),
    (2, 'REGISTRADA', DATE '2026-11-11', 35400, 2, 2),
    (3, 'REGISTRADA', DATE '2026-11-13', 15800, 1, 3),
    (4, 'CANCELADA',  DATE '2026-11-09', 19600, 2, 4),
    (5, 'REGISTRADA', DATE '2026-11-14', 22400, 2, 5)
ON CONFLICT DO NOTHING;

INSERT INTO detalle_venta (id, venta_id, producto_id, cantidad, precio) VALUES
    (1, 1, 1, 3,  3500),
    (2, 1, 2, 2, 12000),
    (3, 2, 5, 6,  4500),
    (4, 2, 3, 2,  4200),
    (5, 3, 4, 1, 15800),
    (6, 4, 7, 2,  9800),
    (7, 5, 8, 4,  5600)
ON CONFLICT DO NOTHING;

-- Entregas -------------------------------------------------------------------------------------
INSERT INTO entrega (id, estado, fecha, direccion, venta_id) VALUES
    (1, 'ENTREGADA', DATE '2026-11-06', 'Calle 10 # 5-20, Bogota',      1),
    (2, 'PENDIENTE', DATE '2026-11-12', 'Carrera 45 # 30-12, Medellin', 2)
ON CONFLICT DO NOTHING;

-- Movimientos de stock: ENTRADA por la compra 1 recibida en la bodega 1, SALIDA por la entrega 1
INSERT INTO movimiento_stock (id, tipo, cantidad, fecha, inventario_id, compra_proveedor_id, entrega_id) VALUES
    (1, 'ENTRADA', 50, DATE '2026-11-04', 1, 1, NULL),
    (2, 'ENTRADA', 30, DATE '2026-11-04', 2, 1, NULL),
    (3, 'SALIDA',   3, DATE '2026-11-06', 1, NULL, 1),
    (4, 'SALIDA',   2, DATE '2026-11-06', 2, NULL, 1)
ON CONFLICT DO NOTHING;

-- Las columnas identity deben continuar despues de los ids insertados explicitamente ------------
SELECT setval(pg_get_serial_sequence('vendedor', 'id'),          (SELECT COALESCE(MAX(id), 0) + 1 FROM vendedor), false);
SELECT setval(pg_get_serial_sequence('autorizacion_rrhh', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM autorizacion_rrhh), false);
SELECT setval(pg_get_serial_sequence('cliente', 'id'),           (SELECT COALESCE(MAX(id), 0) + 1 FROM cliente), false);
SELECT setval(pg_get_serial_sequence('producto', 'id'),          (SELECT COALESCE(MAX(id), 0) + 1 FROM producto), false);
SELECT setval(pg_get_serial_sequence('bodega', 'id'),            (SELECT COALESCE(MAX(id), 0) + 1 FROM bodega), false);
SELECT setval(pg_get_serial_sequence('inventario', 'id'),        (SELECT COALESCE(MAX(id), 0) + 1 FROM inventario), false);
SELECT setval(pg_get_serial_sequence('compra_proveedor', 'id'),  (SELECT COALESCE(MAX(id), 0) + 1 FROM compra_proveedor), false);
SELECT setval(pg_get_serial_sequence('detalle_compra', 'id'),    (SELECT COALESCE(MAX(id), 0) + 1 FROM detalle_compra), false);
SELECT setval(pg_get_serial_sequence('venta', 'id'),             (SELECT COALESCE(MAX(id), 0) + 1 FROM venta), false);
SELECT setval(pg_get_serial_sequence('detalle_venta', 'id'),     (SELECT COALESCE(MAX(id), 0) + 1 FROM detalle_venta), false);
SELECT setval(pg_get_serial_sequence('entrega', 'id'),           (SELECT COALESCE(MAX(id), 0) + 1 FROM entrega), false);
SELECT setval(pg_get_serial_sequence('movimiento_stock', 'id'),  (SELECT COALESCE(MAX(id), 0) + 1 FROM movimiento_stock), false);
