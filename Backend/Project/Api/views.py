from django.shortcuts import render, get_object_or_404, redirect
from .models import *
from django.contrib.auth.decorators import permission_required, login_required
from .serializers import *
from rest_framework import viewsets, generics
from rest_framework.views import APIView
from rest_framework.decorators import api_view
from rest_framework.response import Response
from django.contrib.auth import authenticate, login
from rest_framework.authtoken.models import Token
from rest_framework import status
from rest_framework.response import Response
from rest_framework.permissions import AllowAny, IsAuthenticated

# Para poder recuperar los productos del carrito del usuario es asi:
# carrito = Carrito.objects.get(user=request.user)
# for item in carrito.items.all():
#     print(item.producto.nombre)
#     print(item.cantidad)

def Home(request):
    return render(request, "Pages/Home.html")

def Productos(request):
    productos = Producto.objects.all()
    return render(request,'Pages/Productos.html', {"prods": productos})

@permission_required('Api.add_producto')
def AnadirProducto(request):
    if request.method == 'POST':
        nombre = request.POST.get('nombre')
        descripcion = request.POST.get('descripcion')
        precio = request.POST.get('precio')
        stock = request.POST.get('stock', 0)
        imagen = request.FILES.get('imagen')
        
        producto = Producto(
            nombre=nombre,
            descripcion=descripcion,
            precio=precio,
            stock=stock,
            imagen=imagen
        )
        producto.save()
        return redirect('Productos')
    return render(request, "Pages/FormProd.html")

@permission_required('Api.change_producto')
def ModificarProducto(request, id):
    producto = get_object_or_404(Producto, id=id)
    if request.method == 'POST':
        if 'eliminar' in request.POST:
            producto.delete()
            return redirect('Productos')
        producto.nombre = request.POST.get('nombre')
        producto.descripcion = request.POST.get('descripcion')
        producto.precio = request.POST.get('precio')
        producto.stock = request.POST.get('stock', producto.stock)
        if 'imagen' in request.FILES:
            producto.imagen = request.FILES['imagen']
        producto.save()
        return redirect('Productos')
    return render(request, "Pages/FormProd.html", {"producto": producto})

@login_required
def VerCarrito(request):
    carrito, _ = Carrito.objects.get_or_create(user=request.user)
    items = carrito.items.all()
    total = sum(item.producto.precio * item.cantidad for item in items)
    return render(request, "Pages/Carrito.html", {"items": items, "total": total})

@login_required
def AgregarAlCarrito(request, id):
    producto = get_object_or_404(Producto, id=id)
    carrito, _ = Carrito.objects.get_or_create(user=request.user)
    item, created = CarritoItem.objects.get_or_create(carrito=carrito, producto=producto, defaults={'cantidad': 1})
    if not created:
        item.cantidad += 1
        item.save()
    return redirect(request.META.get('HTTP_REFERER', 'VerCarrito'))

@login_required
def EliminarDelCarrito(request, id):
    item = get_object_or_404(CarritoItem, id=id, carrito__user=request.user)
    item.delete()
    return redirect('VerCarrito')

class ViewProductos(viewsets.ModelViewSet):
    queryset = Producto.objects.all()
    serializer_class = SerializersProducto

class ViewCarrito(viewsets.ModelViewSet):
    serializer_class = SerializerCarrito
    def get_queryset(self):
        return Carrito.objects.filter(usuario=self.request.user)

class ViewPromociones(viewsets.ModelViewSet):
    queryset = Promocion.objects.all()
    serializer_class = SerializerPromocion

@api_view(['GET'])
def prueba(request):
    return Response({
        "msj": "aqui tiene profe, dejeme mimir porfa",
        "imagen": request.build_absolute_uri('/Media/prueba.jpeg')
    })

class ViewCarrito(APIView):
    permission_classes = [IsAuthenticated]
    def get(self, request):
        carrito = Carrito.objects.get(user=request.user)
        serializer = SerializerCarrito(carrito)
        return Response(serializer.data)

class RegisterView(generics.CreateAPIView):
    serializer_class = RegisterSerializer
    permission_classes = [AllowAny]

class LoginView(APIView):
    permission_classes = [AllowAny]
    def post(self, request):
        username = request.data.get("username")
        password = request.data.get("password")
        user = authenticate(
            username=username,
            password=password
        )
        if not user:
            return Response(
                {"error":"Credenciales incorrectas"},
                status=status.HTTP_401_UNAUTHORIZED
            )
        token, _ = Token.objects.get_or_create(user=user)
        try:
            role = user.perfil_sistema.rol
        except PerfilSistema.DoesNotExist:
            role = "administrador" if user.is_staff or user.is_superuser else "cajero"
        return Response({
            "mensaje": "Login correcto",
            "usuario": user.username,
            "token": token.key,
            "role": role
        })

# API administrativa para la aplicación Android. Solo el personal administrador
# autenticado puede crear usuarios, sucursales y cajas.
from django.db import connection
from django.utils.dateparse import parse_time
from rest_framework.permissions import IsAdminUser
from rest_framework.authtoken.models import Token
from .models import PerfilSistema, SucursalComercio, CajaVenta
from .serializers import AdminUserSerializer, SucursalSerializer, CajaVentaSerializer

class AdminUsuariosView(generics.ListCreateAPIView):
    serializer_class = AdminUserSerializer
    permission_classes = [IsAdminUser]
    queryset = User.objects.all().order_by("username")
    def get(self, request, *args, **kwargs):
        rows=[]
        for user in self.get_queryset():
            perfil=getattr(user, "perfil_sistema", None)
            rows.append({"id":user.id,"username":user.username,"email":user.email,"role":perfil.rol if perfil else ("administrador" if user.is_staff or user.is_superuser else "cajero")})
        return Response(rows)

class AdminSucursalesView(generics.ListCreateAPIView):
    serializer_class = SucursalSerializer
    permission_classes = [IsAdminUser]
    queryset = SucursalComercio.objects.all().order_by("nombre")

class AdminCajasView(generics.ListCreateAPIView):
    serializer_class = CajaVentaSerializer
    permission_classes = [IsAdminUser]
    queryset = CajaVenta.objects.select_related("sucursal", "cajero").all()

@api_view(["GET"])
def ProductoPorCodigoBarras(request, codigo):
    # Usa la tabla `productos` del dump SQL original (columna `codigo_barra`).
    with connection.cursor() as cursor:
        cursor.execute("SELECT id, descripcion, precio_final, stock, codigo_barra, familia FROM productos WHERE codigo_barra = %s AND activo = 1 LIMIT 1", [codigo])
        row = cursor.fetchone()
    if not row:
        return Response({"detail": "Producto no encontrado"}, status=status.HTTP_404_NOT_FOUND)
    return Response({"id": row[0], "nombre": row[1] or "Producto", "descripcion": row[1] or "", "precio": float(row[2] or 0), "stock": int(row[3] or 0), "codigo_barras": row[4], "categoria": row[5] or ""})

# API REST para Android: las tablas de productos, facturas y factura_detalle
# pertenecen al dump SQL original, no a los modelos de ejemplo del carrito.
from django.db import transaction
from django.utils import timezone
from rest_framework.permissions import IsAuthenticated
from rest_framework.decorators import api_view, permission_classes
from rest_framework.response import Response
from rest_framework import status


def _product_dict(row):
    return {"id": int(row[0]), "nombre": row[1] or "Producto", "descripcion": row[1] or "",
            "precio": float(row[2] or 0), "stock": int(row[3] or 0), "codigo_barras": row[4],
            "categoria": row[5] or ""}

@api_view(["GET", "POST"])
def AndroidProductos(request):
    if request.method == "GET":
        q = (request.query_params.get("search") or "").strip()
        with connection.cursor() as cursor:
            if q:
                like = f"%{q}%"
                cursor.execute("SELECT id, descripcion, precio_final, stock, codigo_barra, familia FROM productos WHERE activo=1 AND (descripcion LIKE %s OR codigo LIKE %s OR codigo_barra LIKE %s OR CAST(id AS CHAR) LIKE %s) ORDER BY descripcion LIMIT 1000", [like, like, like, like])
            else:
                cursor.execute("SELECT id, descripcion, precio_final, stock, codigo_barra, familia FROM productos WHERE activo=1 ORDER BY descripcion LIMIT 2000")
            return Response([_product_dict(r) for r in cursor.fetchall()])
    if not request.user.is_authenticated:
        return Response({"detail":"Autenticación requerida"}, status=401)
    if not (request.user.is_staff or request.user.is_superuser or getattr(getattr(request.user, "perfil_sistema", None), "rol", "") in ["administrador", "gerente", "deposito"]):
        return Response({"detail":"No tenés permisos para administrar productos"}, status=403)
    data = request.data
    nombre = str(data.get("descripcion", data.get("nombre", ""))).strip()
    try: precio = float(data.get("precio_final", data.get("precio", 0)))
    except (TypeError, ValueError): return Response({"detail":"Precio inválido"}, status=400)
    try: stock = int(data.get("stock", 0))
    except (TypeError, ValueError): return Response({"detail":"Stock inválido"}, status=400)
    if not nombre or precio < 0 or stock < 0: return Response({"detail":"Nombre, precio y stock válidos son obligatorios"}, status=400)
    with connection.cursor() as cursor:
        cursor.execute("INSERT INTO productos (descripcion, precio_final, stock, familia, activo) VALUES (%s,%s,%s,%s,1)", [nombre, precio, stock, data.get("familia", "JUGUETE")])
        pid = cursor.lastrowid
        cursor.execute("SELECT id, descripcion, precio_final, stock, codigo_barra, familia FROM productos WHERE id=%s", [pid])
        return Response(_product_dict(cursor.fetchone()), status=201)

@api_view(["PATCH", "PUT", "DELETE"])
@permission_classes([IsAuthenticated])
def AndroidProductoDetalle(request, pk):
    if not (request.user.is_staff or request.user.is_superuser or getattr(getattr(request.user, "perfil_sistema", None), "rol", "") in ["administrador", "gerente", "deposito"]):
        return Response({"detail":"No tenés permisos para administrar productos"}, status=403)
    with connection.cursor() as cursor:
        cursor.execute("SELECT id, descripcion, precio_final, stock, codigo_barra, familia FROM productos WHERE id=%s AND activo=1", [pk]); row=cursor.fetchone()
        if not row: return Response({"detail":"Producto no encontrado"}, status=404)
        if request.method == "DELETE":
            cursor.execute("UPDATE productos SET activo=0 WHERE id=%s", [pk]); return Response(status=204)
        data=request.data
        nombre=str(data.get("descripcion", data.get("nombre", row[1] or ""))).strip()
        try: precio=float(data.get("precio_final", data.get("precio", row[2] or 0))); stock=int(data.get("stock", row[3] or 0))
        except (TypeError, ValueError): return Response({"detail":"Precio o stock inválido"}, status=400)
        cursor.execute("UPDATE productos SET descripcion=%s, precio_final=%s, stock=%s, familia=%s WHERE id=%s", [nombre, precio, stock, data.get("familia", row[5] or ""), pk])
        cursor.execute("SELECT id, descripcion, precio_final, stock, codigo_barra, familia FROM productos WHERE id=%s", [pk]); return Response(_product_dict(cursor.fetchone()))

@api_view(["POST", "GET"])
@permission_classes([IsAuthenticated])
def AndroidVentas(request):
    if request.method == "POST":
        items=request.data.get("items", [])
        if not items: return Response({"detail":"La venta debe incluir productos"}, status=400)
        username=request.user.username
        with transaction.atomic():
            with connection.cursor() as cursor:
                cursor.execute("SELECT id FROM usuarios WHERE usuario=%s AND estado=1 LIMIT 1", [username]); userrow=cursor.fetchone()
                if not userrow: cursor.execute("SELECT id FROM usuarios WHERE estado=1 ORDER BY id LIMIT 1"); userrow=cursor.fetchone()
                cursor.execute("SELECT id FROM sucursales ORDER BY id LIMIT 1"); branch=cursor.fetchone()
                cursor.execute("SELECT id FROM cajas ORDER BY id LIMIT 1"); cash=cursor.fetchone()
                if not userrow or not branch or not cash: return Response({"detail":"El SQL debe contener al menos un usuario, una sucursal y una caja"}, status=400)
                normalized=[]; total=0.0
                for item in items:
                    try: pid=int(item.get("productId")); qty=int(item.get("quantity"))
                    except (TypeError, ValueError): return Response({"detail":"Producto o cantidad inválida"}, status=400)
                    if qty < 1: return Response({"detail":"La cantidad debe ser mayor a cero"}, status=400)
                    cursor.execute("SELECT id, descripcion, precio_final, stock FROM productos WHERE id=%s AND activo=1 FOR UPDATE", [pid]); p=cursor.fetchone()
                    if not p: return Response({"detail":f"Producto {pid} no encontrado"}, status=400)
                    if int(p[3] or 0) < qty: return Response({"detail":f"Stock insuficiente para {p[1]}"}, status=400)
                    unit=float(p[2] or 0); subtotal=unit*qty; total+=subtotal; normalized.append((p,qty,unit,subtotal))
                cursor.execute("INSERT INTO facturas (tipo, fecha, usuario_id, sucursal_id, caja_id, total, total_sin_ajuste, cobrada, cliente_nombre) VALUES ('PV',%s,%s,%s,%s,%s,%s,1,NULL)", [timezone.now().replace(tzinfo=None), userrow[0], branch[0], cash[0], total, total])
                invoice_id=cursor.lastrowid
                for p,qty,unit,subtotal in normalized:
                    cursor.execute("INSERT INTO factura_detalle (factura_id, producto_id, cantidad, precio_unitario, subtotal) VALUES (%s,%s,%s,%s,%s)", [invoice_id,p[0],qty,unit,subtotal])
                    cursor.execute("UPDATE productos SET stock=stock-%s WHERE id=%s", [qty,p[0]])
                return Response({"id":invoice_id,"date":timezone.now().isoformat(),"total":total,"userId":username,"clientName":"","items":[{"producto_id":p[0],"nombre":p[1],"cantidad":qty,"precio_unitario":unit,"subtotal":subtotal} for p,qty,unit,subtotal in normalized]}, status=201)
    with connection.cursor() as cursor:
        cursor.execute("SELECT id, fecha, total, usuario_id FROM facturas ORDER BY fecha DESC, id DESC LIMIT 500")
        rows=cursor.fetchall()
        result=[]
        for row in rows:
            cursor.execute("SELECT d.producto_id, COALESCE(p.descripcion,'Producto'), d.cantidad, d.precio_unitario, d.subtotal FROM factura_detalle d LEFT JOIN productos p ON p.id=d.producto_id WHERE d.factura_id=%s", [row[0]])
            items=[{"producto_id":int(i[0]),"nombre":i[1],"cantidad":int(i[2]),"precio_unitario":float(i[3]),"subtotal":float(i[4])} for i in cursor.fetchall()]
            result.append({"id":int(row[0]),"date":row[1].isoformat() if row[1] else None,"total":float(row[2]),"userId":str(row[3]),"clientName":"","items":items})
        return Response(result)

@api_view(["GET"])
@permission_classes([IsAuthenticated])
def AndroidVentaDetalle(request, pk):
    with connection.cursor() as cursor:
        cursor.execute("SELECT id, fecha, total, usuario_id FROM facturas WHERE id=%s", [pk]); row=cursor.fetchone()
        if not row: return Response({"detail":"Factura no encontrada"}, status=404)
        cursor.execute("SELECT d.producto_id, COALESCE(p.descripcion,'Producto'), d.cantidad, d.precio_unitario, d.subtotal FROM factura_detalle d LEFT JOIN productos p ON p.id=d.producto_id WHERE d.factura_id=%s", [pk])
        items=[{"producto_id":int(i[0]),"nombre":i[1],"cantidad":int(i[2]),"precio_unitario":float(i[3]),"subtotal":float(i[4])} for i in cursor.fetchall()]
        return Response({"id":int(row[0]),"date":row[1].isoformat() if row[1] else None,"total":float(row[2]),"userId":str(row[3]),"clientName":"","items":items})

# Override admin branch/cash endpoints to write into the original POS tables,
# so newly configured locations and registers are usable by Android sales.
class AdminSucursalesView(APIView):
    permission_classes = [IsAdminUser]
    def get(self, request):
        with connection.cursor() as cursor:
            cursor.execute("SELECT id, nombre, direccion, rs FROM sucursales ORDER BY id")
            return Response([{"id":r[0],"nombre":r[1] or "","domicilio":r[2] or "","razon_social":r[3] or ""} for r in cursor.fetchall()])
    def post(self, request):
        nombre=str(request.data.get("nombre", "")).strip(); domicilio=str(request.data.get("domicilio", "")).strip(); razon=str(request.data.get("razon_social", "")).strip()
        if not nombre or not domicilio or not razon: return Response({"detail":"Nombre, domicilio y razón social son obligatorios"}, status=400)
        with connection.cursor() as cursor:
            cursor.execute("INSERT INTO sucursales (nombre, direccion, rs) VALUES (%s,%s,%s)", [nombre, domicilio, razon]); sid=cursor.lastrowid
        return Response({"id":sid,"nombre":nombre,"domicilio":domicilio,"razon_social":razon}, status=201)

class AdminCajasView(APIView):
    permission_classes = [IsAdminUser]
    def get(self, request):
        with connection.cursor() as cursor:
            cursor.execute("CREATE TABLE IF NOT EXISTS caja_horarios (caja_id INT NOT NULL PRIMARY KEY, hora_apertura TIME NOT NULL DEFAULT '09:00:00', hora_cierre TIME NOT NULL DEFAULT '18:00:00') ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")
            cursor.execute("""
                SELECT
                    c.id,
                    c.sucursal_id,
                    c.nombre,
                    c.usuario_id,
                    COALESCE(h.hora_apertura, '09:00:00'),
                    COALESCE(h.hora_cierre, '18:00:00'),
                    u.nombre,
                    au.id
                FROM cajas c
                LEFT JOIN caja_horarios h ON h.caja_id = c.id
                LEFT JOIN usuarios u ON u.id = c.usuario_id
                LEFT JOIN auth_user au
                    ON au.username COLLATE utf8mb4_general_ci = u.usuario
                ORDER BY c.id
            """)
            return Response([{"id":r[0],"sucursal":r[1],"nombre":r[2] or "","cajero":r[7] or r[3],"cajero_legacy_id":r[3],"cajero_nombre":r[6] or "","hora_apertura":str(r[4]),"hora_cierre":str(r[5])} for r in cursor.fetchall()])
    def post(self, request):
        try: sucursal=int(request.data.get("sucursal"))
        except (TypeError,ValueError): return Response({"detail":"ID de sucursal inválido"}, status=400)
        nombre=str(request.data.get("nombre", "")).strip(); apertura=str(request.data.get("hora_apertura", "09:00")); cierre=str(request.data.get("hora_cierre", "18:00")); cashier=request.data.get("cajero")
        if not nombre: return Response({"detail":"El nombre de la caja es obligatorio"}, status=400)
        legacy_cashier=None
        if cashier not in (None, "", 0, "0"):
            try: django_user=User.objects.get(pk=int(cashier))
            except (ValueError, User.DoesNotExist): return Response({"detail":"El usuario cajero no existe"}, status=400)
            with connection.cursor() as cursor:
                cursor.execute("SELECT id FROM usuarios WHERE usuario=%s AND estado=1 LIMIT 1", [django_user.username]); found=cursor.fetchone()
            if not found: return Response({"detail":"Creá primero el operador con el mismo nombre de usuario en la tabla usuarios del SQL original"}, status=400)
            legacy_cashier=found[0]
        with connection.cursor() as cursor:
            cursor.execute("SELECT id FROM sucursales WHERE id=%s", [sucursal])
            if not cursor.fetchone(): return Response({"detail":"La sucursal no existe en la base SQL"}, status=400)
            cursor.execute("CREATE TABLE IF NOT EXISTS caja_horarios (caja_id INT NOT NULL PRIMARY KEY, hora_apertura TIME NOT NULL DEFAULT '09:00:00', hora_cierre TIME NOT NULL DEFAULT '18:00:00') ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")
            cursor.execute("INSERT INTO cajas (sucursal_id,nombre,usuario_id) VALUES (%s,%s,%s)", [sucursal,nombre,legacy_cashier]); cid=cursor.lastrowid
            cursor.execute("INSERT INTO caja_horarios (caja_id,hora_apertura,hora_cierre) VALUES (%s,%s,%s)", [cid,apertura,cierre])
        return Response({"id":cid,"sucursal":sucursal,"nombre":nombre,"cajero":legacy_cashier,"hora_apertura":apertura,"hora_cierre":cierre}, status=201)

# Operaciones de detalle para el panel administrativo Android.
class AdminUsuarioDetalleView(APIView):
    permission_classes = [IsAdminUser]
    def patch(self, request, pk):
        try: user = User.objects.get(pk=pk)
        except User.DoesNotExist: return Response({"detail":"Usuario no encontrado"}, status=404)
        username = request.data.get("username", user.username)
        email = request.data.get("email", user.email)
        role = request.data.get("role")
        if role and role not in ["administrador", "gerente", "deposito", "cajero"]:
            return Response({"detail":"Rol inválido"}, status=400)
        user.username = str(username).strip(); user.email = str(email).strip()
        if not user.username or not user.email: return Response({"detail":"Nombre y correo son obligatorios"}, status=400)
        if role == "administrador": user.is_staff = True
        elif role: user.is_staff = False
        user.save()
        if role:
            PerfilSistema.objects.update_or_create(usuario=user, defaults={"rol":role})
            role_names = {"administrador":"admin", "gerente":"gerencial", "deposito":"recepcion", "cajero":"caja"}
            with connection.cursor() as cursor:
                cursor.execute("SELECT id FROM roles WHERE nombre=%s AND activo=1 LIMIT 1", [role_names[role]])
                role_row=cursor.fetchone()
                cursor.execute("SELECT id FROM usuarios WHERE usuario=%s OR usuario=%s LIMIT 1", [user.username, request.data.get("username", user.username)])
                legacy=cursor.fetchone()
                if role_row and legacy:
                    cursor.execute("UPDATE usuarios SET nombre=%s,usuario=%s,rol_id=%s WHERE id=%s", [user.username,user.username,role_row[0],legacy[0]])
        return Response({"id":user.id,"username":user.username,"email":user.email,"role":role or getattr(getattr(user,"perfil_sistema",None),"rol","cajero")})
    def delete(self, request, pk):
        try: user = User.objects.get(pk=pk)
        except User.DoesNotExist: return Response({"detail":"Usuario no encontrado"}, status=404)
        if user.pk == request.user.pk: return Response({"detail":"No podés eliminar tu propio usuario"}, status=400)
        user.delete(); return Response(status=204)

class AdminSucursalDetalleView(APIView):
    permission_classes = [IsAdminUser]
    def patch(self, request, pk):
        nombre=str(request.data.get("nombre", "")).strip(); domicilio=str(request.data.get("domicilio", "")).strip(); razon=str(request.data.get("razon_social", "")).strip()
        if not nombre or not domicilio or not razon: return Response({"detail":"Todos los campos son obligatorios"},status=400)
        with connection.cursor() as cursor:
            cursor.execute("UPDATE sucursales SET nombre=%s,direccion=%s,rs=%s WHERE id=%s",[nombre,domicilio,razon,pk])
            if cursor.rowcount == 0:
                cursor.execute("SELECT id FROM sucursales WHERE id=%s",[pk])
                if not cursor.fetchone(): return Response({"detail":"Sucursal no encontrada"},status=404)
        return Response({"id":int(pk),"nombre":nombre,"domicilio":domicilio,"razon_social":razon})
    def delete(self, request, pk):
        with connection.cursor() as cursor:
            cursor.execute("SELECT id FROM cajas WHERE sucursal_id=%s LIMIT 1",[pk])
            if cursor.fetchone(): return Response({"detail":"No se puede eliminar una sucursal que tiene cajas asociadas"},status=400)
            cursor.execute("DELETE FROM sucursales WHERE id=%s",[pk])
            if cursor.rowcount == 0: return Response({"detail":"Sucursal no encontrada"},status=404)
        return Response(status=204)

class AdminCajaDetalleView(APIView):
    permission_classes = [IsAdminUser]
    def patch(self, request, pk):
        try: sucursal=int(request.data.get("sucursal")); nombre=str(request.data.get("nombre", "")).strip()
        except (TypeError,ValueError): return Response({"detail":"Sucursal inválida"},status=400)
        cajero=request.data.get("cajero")
        with connection.cursor() as cursor:
            cursor.execute("SELECT id FROM sucursales WHERE id=%s",[sucursal])
            if not cursor.fetchone(): return Response({"detail":"La sucursal no existe"},status=400)
            legacy_cashier=None
            if cajero not in (None,"",0,"0"):
                try: u=User.objects.get(pk=int(cajero))
                except (ValueError,User.DoesNotExist): return Response({"detail":"Cajero inválido"},status=400)
                cursor.execute("SELECT id FROM usuarios WHERE usuario=%s AND estado=1 LIMIT 1",[u.username]); found=cursor.fetchone()
                if not found: return Response({"detail":"El usuario no está sincronizado con la tabla operativa usuarios"},status=400)
                legacy_cashier=found[0]
            cursor.execute("UPDATE cajas SET sucursal_id=%s,nombre=%s,usuario_id=%s WHERE id=%s",[sucursal,nombre,legacy_cashier,pk])
            if cursor.rowcount == 0:
                cursor.execute("SELECT id FROM cajas WHERE id=%s",[pk])
                if not cursor.fetchone(): return Response({"detail":"Caja no encontrada"},status=404)
            apertura=str(request.data.get("hora_apertura","09:00")); cierre=str(request.data.get("hora_cierre","18:00"))
            if parse_time(apertura) is None or parse_time(cierre) is None: return Response({"detail":"Las horas deben tener formato HH:mm"},status=400)
            cursor.execute("CREATE TABLE IF NOT EXISTS caja_horarios (caja_id INT NOT NULL PRIMARY KEY, hora_apertura TIME NOT NULL DEFAULT '09:00:00', hora_cierre TIME NOT NULL DEFAULT '18:00:00') ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")
            cursor.execute("INSERT INTO caja_horarios(caja_id,hora_apertura,hora_cierre) VALUES(%s,%s,%s) ON DUPLICATE KEY UPDATE hora_apertura=VALUES(hora_apertura),hora_cierre=VALUES(hora_cierre)",[pk,apertura,cierre])
        return Response({"id":int(pk),"sucursal":sucursal,"nombre":nombre,"cajero":legacy_cashier,"hora_apertura":apertura,"hora_cierre":cierre})
    def delete(self, request, pk):
        with connection.cursor() as cursor:
            cursor.execute("DELETE FROM caja_horarios WHERE caja_id=%s",[pk]); cursor.execute("DELETE FROM cajas WHERE id=%s",[pk])
            if cursor.rowcount == 0: return Response({"detail":"Caja no encontrada"},status=404)
        return Response(status=204)
