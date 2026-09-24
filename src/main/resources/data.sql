-- Clientes de prueba
INSERT INTO clientes (id, tipo_cliente, nit) VALUES (1, 'VIP', '900123456');
INSERT INTO clientes (id, tipo_cliente, nit) VALUES (2, 'FRECUENTE', '900654321');
INSERT INTO clientes (id, tipo_cliente, nit) VALUES (3, 'MOROSO', NULL);
INSERT INTO clientes (id, tipo_cliente, nit) VALUES (4, 'ESTANDAR', NULL);

-- Inventario y productos
INSERT INTO productos (id, precio) VALUES (101, 50000.0);
INSERT INTO inventario (producto_id, stock) VALUES (101, 100);

INSERT INTO productos (id, precio) VALUES (102, 200000.0);
INSERT INTO inventario (producto_id, stock) VALUES (102, 5);

-- Factura pendiente para el cliente moroso (ID 3)
INSERT INTO facturas (cliente_id, monto, pagada) VALUES (3, 150000.0, false);

-- Historial para el cliente frecuente (ID 2): 4 pedidos previos para activar descuento del 4%
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (2, 50000, 0, 9500, 59500, CURRENT_TIMESTAMP, 'CONFIRMADO');
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (2, 50000, 0, 9500, 59500, CURRENT_TIMESTAMP, 'CONFIRMADO');
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (2, 50000, 0, 9500, 59500, CURRENT_TIMESTAMP, 'CONFIRMADO');
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (2, 50000, 0, 9500, 59500, CURRENT_TIMESTAMP, 'CONFIRMADO');