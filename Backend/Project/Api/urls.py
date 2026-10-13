from django.contrib import admin
from django.urls import path, include
from .models import *
from .views import *
from rest_framework import routers

router = routers.DefaultRouter()
router.register('carrito',ViewCarrito)
router.register("promociones", ViewPromociones)

urlpatterns = [
    path('', Home, name="Home"),
    path('Productos/', Productos, name="Productos"),
    path('AnadirProd/', AnadirProducto, name="AnadirProd"),
    path('ModificarProd/<id>/', ModificarProducto, name="ModificarProd"),
    path('Carrito/', VerCarrito, name="VerCarrito"),
    path('AgregarAlCarrito/<id>/', AgregarAlCarrito, name="AgregarAlCarrito"),
    path('EliminarDelCarrito/<id>/', EliminarDelCarrito, name="EliminarDelCarrito"),
    path('api/', include(router.urls)),
    path('api/prueba/', prueba, name="prueba"),
    path("api/login/", LoginView.as_view()),
    path("api/register/", RegisterView.as_view()),
    path("api/carrito/", ViewCarrito.as_view()),
    path("api/productos/por-codigo/<str:codigo>/", ProductoPorCodigoBarras, name="producto_por_codigo_barras"),
    path("api/productos/", AndroidProductos, name="android_productos"),
    path("api/productos/<int:pk>/", AndroidProductoDetalle, name="android_producto_detalle"),
    path("api/ventas/", AndroidVentas, name="android_ventas"),
    path("api/ventas/<int:pk>/", AndroidVentaDetalle, name="android_venta_detalle"),
    path("api/admin/usuarios/", AdminUsuariosView.as_view(), name="admin_usuarios"),
    path("api/admin/sucursales/", AdminSucursalesView.as_view(), name="admin_sucursales"),
    path("api/admin/cajas/", AdminCajasView.as_view(), name="admin_cajas"),
]