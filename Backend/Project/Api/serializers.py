from .models import *
from django.contrib.auth.models import User
from rest_framework import serializers

class SerializersProducto(serializers.ModelSerializer):
    class Meta:
        model=Producto
        fields='__all__'
    
class SerializersCarritoItem(serializers.ModelSerializer):
    producto = SerializersProducto(read_only=True)
    class Meta:
        model = CarritoItem
        fields = ["id", "producto", "cantidad"]

class SerializerCarrito(serializers.ModelSerializer):
    items = SerializersCarritoItem(many=True, read_only=True)

    class Meta:
        model = Carrito
        fields = "__all__"

class SerializerPromocion(serializers.ModelSerializer):
    class Meta:
        model = Promocion
        fields = '__all__'


class RegisterSerializer(serializers.ModelSerializer):
    """Registro público: solo se pide email y contraseña.

    Django exige un `username` único internamente, pero el cliente nunca lo
    envía. Se genera a partir de la parte local del correo y se asegura
    que sea único para no chocar con usuarios creados desde el panel admin.
    """
    email = serializers.EmailField(required=True)
    password = serializers.CharField(write_only=True)

    class Meta:
        model = User
        fields = ("email", "password")

    def validate_email(self, value):
        email = value.strip().lower()
        if User.objects.filter(email__iexact=email).exists():
            raise serializers.ValidationError("Ya existe un usuario registrado con ese correo.")
        return email

    @staticmethod
    def _username_from_email(email):
        base = (email.split("@", 1)[0].strip() or "usuario")[:150]
        username = base
        suffix = 1
        while User.objects.filter(username=username).exists():
            tail = str(suffix)
            username = f"{base[:150 - len(tail)]}{tail}"
            suffix += 1
        return username

    def create(self, validated_data):
        email = validated_data["email"]
        user = User.objects.create_user(
            username=self._username_from_email(email),
            email=email,
            password=validated_data["password"]
        )
        Carrito.objects.create(user=user)
        return user
from .models import PerfilSistema, SucursalComercio, CajaVenta

class AdminUserSerializer(serializers.ModelSerializer):
    password = serializers.CharField(write_only=True, min_length=8)
    role = serializers.ChoiceField(choices=["administrador", "gerente", "deposito", "cajero"], write_only=True)
    class Meta:
        model = User
        fields = ("id", "username", "email", "password", "role", "is_active")
        read_only_fields = ("id", "is_active")
    def create(self, validated_data):
        role = validated_data.pop("role")
        password = validated_data.pop("password")
        user = User.objects.create_user(password=password, **validated_data)
        user.is_staff = role == "administrador"
        user.save(update_fields=["is_staff"])
        PerfilSistema.objects.update_or_create(usuario=user, defaults={"rol": role})
        # Sincroniza un registro operativo en la tabla `usuarios` del SQL original,
        # para que las facturas puedan asociarse al cajero/administrador correcto.
        from django.db import connection
        role_names = {"administrador": "admin", "gerente": "gerencial", "deposito": "recepcion", "cajero": "caja"}
        with connection.cursor() as cursor:
            cursor.execute("SELECT id FROM roles WHERE nombre=%s AND activo=1 LIMIT 1", [role_names[role]])
            role_row = cursor.fetchone()
            cursor.execute("SELECT id FROM sucursales ORDER BY id LIMIT 1")
            branch_row = cursor.fetchone()
            if role_row and branch_row:
                cursor.execute("SELECT id FROM usuarios WHERE usuario=%s LIMIT 1", [user.username])
                existing = cursor.fetchone()
                if existing:
                    cursor.execute("UPDATE usuarios SET nombre=%s, clave=%s, estado=1, rol_id=%s, sucursal_id=%s WHERE id=%s", [user.username, user.password, role_row[0], branch_row[0], existing[0]])
                else:
                    cursor.execute("INSERT INTO usuarios (nombre,usuario,clave,estado,sucursal_id,rol_id,clave_protegida) VALUES (%s,%s,%s,1,%s,%s,1)", [user.username, user.username, user.password, branch_row[0], role_row[0]])
        return user

class SucursalSerializer(serializers.ModelSerializer):
    class Meta:
        model = SucursalComercio
        fields = "__all__"

class CajaVentaSerializer(serializers.ModelSerializer):
    class Meta:
        model = CajaVenta
        fields = "__all__"
