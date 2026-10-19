from django.db import migrations

# Entidades del DER (Tarea 7) que faltaban en el esquema del sistema. Se crean
# sobre las tablas heredadas del dump SQL: facturas, cajas, usuarios, vouchers.

FORWARD = [
    """
    CREATE TABLE IF NOT EXISTS `ventas` (
      `id` int(11) NOT NULL AUTO_INCREMENT,
      `factura_id` int(11) NOT NULL,
      `monto_pagado` decimal(12,2) NOT NULL DEFAULT 0.00,
      `vuelto` decimal(12,2) NOT NULL DEFAULT 0.00,
      `fecha` datetime DEFAULT current_timestamp(),
      PRIMARY KEY (`id`),
      KEY `fk_ventas_factura` (`factura_id`),
      CONSTRAINT `fk_ventas_factura` FOREIGN KEY (`factura_id`) REFERENCES `facturas` (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
    """,
    """
    CREATE TABLE IF NOT EXISTS `prepagas` (
      `id` int(11) NOT NULL AUTO_INCREMENT,
      `numero` varchar(30) DEFAULT NULL,
      `monto` decimal(12,2) NOT NULL DEFAULT 0.00,
      `estado` enum('EMITIDO','USADO','ANULADO') NOT NULL DEFAULT 'EMITIDO',
      `fecha` datetime DEFAULT current_timestamp(),
      PRIMARY KEY (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
    """,
    """
    CREATE TABLE IF NOT EXISTS `detalle_prepaga` (
      `id` int(11) NOT NULL AUTO_INCREMENT,
      `prepaga_id` int(11) NOT NULL,
      `venta_id` int(11) NOT NULL,
      `monto_usado` decimal(12,2) NOT NULL DEFAULT 0.00,
      PRIMARY KEY (`id`),
      KEY `fk_detprepaga_prepaga` (`prepaga_id`),
      KEY `fk_detprepaga_venta` (`venta_id`),
      CONSTRAINT `fk_detprepaga_prepaga` FOREIGN KEY (`prepaga_id`) REFERENCES `prepagas` (`id`),
      CONSTRAINT `fk_detprepaga_venta` FOREIGN KEY (`venta_id`) REFERENCES `ventas` (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
    """,
    """
    CREATE TABLE IF NOT EXISTS `detalle_voucher` (
      `id` int(11) NOT NULL AUTO_INCREMENT,
      `voucher_id` int(11) NOT NULL,
      `venta_id` int(11) NOT NULL,
      `monto` decimal(12,2) NOT NULL DEFAULT 0.00,
      `fecha` datetime DEFAULT current_timestamp(),
      `tipo` enum('emision','aplicacion') NOT NULL DEFAULT 'emision',
      `diferencia` decimal(12,2) NOT NULL DEFAULT 0.00,
      `observaciones` varchar(255) DEFAULT NULL,
      PRIMARY KEY (`id`),
      KEY `fk_detvoucher_voucher` (`voucher_id`),
      KEY `fk_detvoucher_venta` (`venta_id`),
      CONSTRAINT `fk_detvoucher_voucher` FOREIGN KEY (`voucher_id`) REFERENCES `vouchers` (`id`),
      CONSTRAINT `fk_detvoucher_venta` FOREIGN KEY (`venta_id`) REFERENCES `ventas` (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
    """,
    """
    CREATE TABLE IF NOT EXISTS `asignacion_cajas` (
      `id` int(11) NOT NULL AUTO_INCREMENT,
      `caja_id` int(11) NOT NULL,
      `usuario_id` int(11) NOT NULL,
      `fecha_asignacion` datetime DEFAULT current_timestamp(),
      PRIMARY KEY (`id`),
      KEY `fk_asigcajas_caja` (`caja_id`),
      KEY `fk_asigcajas_usuario` (`usuario_id`),
      CONSTRAINT `fk_asigcajas_caja` FOREIGN KEY (`caja_id`) REFERENCES `cajas` (`id`),
      CONSTRAINT `fk_asigcajas_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
    """,
    """
    CREATE TABLE IF NOT EXISTS `autorizacion_pagos` (
      `id` int(11) NOT NULL AUTO_INCREMENT,
      `caja_id` int(11) NOT NULL,
      `venta_id` int(11) NOT NULL,
      `monto_inicial` decimal(12,2) NOT NULL DEFAULT 0.00,
      `monto_final` decimal(12,2) NOT NULL DEFAULT 0.00,
      PRIMARY KEY (`id`),
      KEY `fk_autopagos_caja` (`caja_id`),
      KEY `fk_autopagos_venta` (`venta_id`),
      CONSTRAINT `fk_autopagos_caja` FOREIGN KEY (`caja_id`) REFERENCES `cajas` (`id`),
      CONSTRAINT `fk_autopagos_venta` FOREIGN KEY (`venta_id`) REFERENCES `ventas` (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
    """,
    """
    CREATE TABLE IF NOT EXISTS `eliminacion_facturas` (
      `id` int(11) NOT NULL AUTO_INCREMENT,
      `factura_id` int(11) NOT NULL,
      `usuario_id` int(11) NOT NULL,
      `fecha` datetime DEFAULT current_timestamp(),
      PRIMARY KEY (`id`),
      KEY `fk_elimfact_factura` (`factura_id`),
      KEY `fk_elimfact_usuario` (`usuario_id`),
      CONSTRAINT `fk_elimfact_factura` FOREIGN KEY (`factura_id`) REFERENCES `facturas` (`id`),
      CONSTRAINT `fk_elimfact_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
    """,
    """
    CREATE TABLE IF NOT EXISTS `informe_ganancias` (
      `id` int(11) NOT NULL AUTO_INCREMENT,
      `fecha_desde` date DEFAULT NULL,
      `fecha_hasta` date DEFAULT NULL,
      `ganancia_neta` decimal(12,2) NOT NULL DEFAULT 0.00,
      PRIMARY KEY (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
    """,
    """
    CREATE TABLE IF NOT EXISTS `notas_credito` (
      `id` int(11) NOT NULL AUTO_INCREMENT,
      `factura_id` int(11) NOT NULL,
      `usuario_id` int(11) NOT NULL,
      `monto` decimal(12,2) NOT NULL DEFAULT 0.00,
      `fecha` datetime DEFAULT current_timestamp(),
      `observaciones` varchar(255) DEFAULT NULL,
      PRIMARY KEY (`id`),
      KEY `fk_notacredito_factura` (`factura_id`),
      KEY `fk_notacredito_usuario` (`usuario_id`),
      CONSTRAINT `fk_notacredito_factura` FOREIGN KEY (`factura_id`) REFERENCES `facturas` (`id`),
      CONSTRAINT `fk_notacredito_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
    """,
    """
    CREATE TABLE IF NOT EXISTS `api_conexiones` (
      `id` int(11) NOT NULL AUTO_INCREMENT,
      `estado` varchar(50) DEFAULT NULL,
      `observaciones` varchar(255) DEFAULT NULL,
      `fecha` datetime DEFAULT current_timestamp(),
      PRIMARY KEY (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
    """,
]

REVERSE = [
    "DROP TABLE IF EXISTS `api_conexiones`",
    "DROP TABLE IF EXISTS `notas_credito`",
    "DROP TABLE IF EXISTS `informe_ganancias`",
    "DROP TABLE IF EXISTS `eliminacion_facturas`",
    "DROP TABLE IF EXISTS `autorizacion_pagos`",
    "DROP TABLE IF EXISTS `asignacion_cajas`",
    "DROP TABLE IF EXISTS `detalle_voucher`",
    "DROP TABLE IF EXISTS `detalle_prepaga`",
    "DROP TABLE IF EXISTS `prepagas`",
    "DROP TABLE IF EXISTS `ventas`",
]


class Migration(migrations.Migration):
    dependencies = [("Api", "0004_admin_panel")]

    operations = [
        migrations.RunSQL(sql=FORWARD, reverse_sql=REVERSE),
    ]
