from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion

class Migration(migrations.Migration):
    dependencies = [('Api', '0003_alter_carritoitem_cantidad'), migrations.swappable_dependency(settings.AUTH_USER_MODEL)]
    operations = [
        migrations.CreateModel(name='PerfilSistema', fields=[('id', models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')), ('rol', models.CharField(choices=[('administrador','Administrador'),('gerente','Gerente'),('deposito','Depósito'),('cajero','Cajero')], default='cajero', max_length=20)), ('usuario', models.OneToOneField(on_delete=django.db.models.deletion.CASCADE, related_name='perfil_sistema', to=settings.AUTH_USER_MODEL))]),
        migrations.CreateModel(name='SucursalComercio', fields=[('id', models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')), ('nombre', models.CharField(max_length=100)), ('domicilio', models.CharField(max_length=255)), ('razon_social', models.CharField(max_length=150)), ('activa', models.BooleanField(default=True))]),
        migrations.CreateModel(name='CajaVenta', fields=[('id', models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')), ('nombre', models.CharField(max_length=80)), ('hora_apertura', models.TimeField(default='09:00')), ('hora_cierre', models.TimeField(default='18:00')), ('activa', models.BooleanField(default=True)), ('cajero', models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.SET_NULL, related_name='cajas_asignadas', to=settings.AUTH_USER_MODEL)), ('sucursal', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name='cajas', to='Api.sucursalcomercio'))]),
    ]
