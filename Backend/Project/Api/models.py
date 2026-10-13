from django.db import models
from django.contrib.auth.models import User

class Producto(models.Model):
    id = models.AutoField(primary_key=True)
    nombre = models.TextField()
    descripcion = models.TextField()
    precio = models.FloatField()
    imagen = models.ImageField(upload_to='movies', null=True)
    stock = models.IntegerField(default=0)
    def __int__(self):
        return self.id

class Carrito(models.Model):
    id = models.AutoField(primary_key=True)
    user = models.OneToOneField(
        User,
        on_delete=models.CASCADE
    )

class CarritoItem(models.Model):
    id = models.AutoField(primary_key=True)
    carrito = models.ForeignKey(Carrito, on_delete=models.CASCADE, related_name="items")
    producto = models.ForeignKey(Producto, on_delete=models.CASCADE, related_name="carritos")
    cantidad = models.IntegerField(default=1)

class Promocion(models.Model):
    id = models.AutoField(primary_key=True)
    nombre = models.CharField(max_length=255)
    descripcion = models.TextField(blank=True, null=True)

    descuento_porcentaje = models.FloatField(null=True, blank=True)
    descuento_fijo = models.FloatField(null=True, blank=True)

    fecha_inicio = models.DateTimeField()
    fecha_fin = models.DateTimeField()

    activa = models.BooleanField(default=True)

    def __str__(self):
        return self.nombre

class PromocionProducto(models.Model):
    promocion = models.ForeignKey(Promocion, on_delete=models.CASCADE)
    producto = models.ForeignKey('Producto', on_delete=models.CASCADE)

    def __str__(self):
        return f"{self.promocion.nombre} - {self.producto.nombre}"
# Entidades del panel administrativo. Se mantienen separadas de las tablas del
# sistema de facturación heredado para no alterar su estructura al migrar Django.
class PerfilSistema(models.Model):
    ROLES = [("administrador", "Administrador"), ("gerente", "Gerente"), ("deposito", "Depósito"), ("cajero", "Cajero")]
    usuario = models.OneToOneField(User, on_delete=models.CASCADE, related_name="perfil_sistema")
    rol = models.CharField(max_length=20, choices=ROLES, default="cajero")
    def __str__(self): return f"{self.usuario.username} ({self.rol})"

class SucursalComercio(models.Model):
    nombre = models.CharField(max_length=100)
    domicilio = models.CharField(max_length=255)
    razon_social = models.CharField(max_length=150)
    activa = models.BooleanField(default=True)
    def __str__(self): return self.nombre

class CajaVenta(models.Model):
    sucursal = models.ForeignKey(SucursalComercio, on_delete=models.CASCADE, related_name="cajas")
    nombre = models.CharField(max_length=80)
    cajero = models.ForeignKey(User, null=True, blank=True, on_delete=models.SET_NULL, related_name="cajas_asignadas")
    hora_apertura = models.TimeField(default="09:00")
    hora_cierre = models.TimeField(default="18:00")
    activa = models.BooleanField(default=True)
    def __str__(self): return f"{self.sucursal.nombre} - {self.nombre}"
