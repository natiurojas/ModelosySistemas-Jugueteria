from django.db import migrations

FORWARD = [
    """CREATE TABLE IF NOT EXISTS caja_horarios (
        caja_id INT NOT NULL PRIMARY KEY,
        hora_apertura TIME NOT NULL DEFAULT '09:00:00',
        hora_cierre TIME NOT NULL DEFAULT '18:00:00'
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",
    """CREATE TABLE IF NOT EXISTS gerente_configuracion (
        sucursal_id INT NOT NULL PRIMARY KEY,
        hora_limite_ventas TIME NOT NULL DEFAULT '18:00:00'
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",
]

class Migration(migrations.Migration):
    dependencies = [("Api", "0005_der_coherencia")]
    operations = [migrations.RunSQL(sql=FORWARD, reverse_sql=migrations.RunSQL.noop)]
