# Configuración Django/MySQL para Android

## Base de datos
1. Crear la base `c2850524_factura` en MySQL/MariaDB con juego de caracteres `utf8mb4`.
2. Importar el archivo `c2850524_factura modelos y sistemas.sql` con phpMyAdmin o `mysql -u root -p c2850524_factura < "c2850524_factura modelos y sistemas.sql"`.
3. Instalar dependencias: `pip install -r requirements.txt`.
4. Configurar `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_HOST`, `DB_PORT` como variables de entorno si no se usan los valores locales predeterminados.
5. Desde `Backend/Project`: ejecutar `python manage.py migrate` y luego `python manage.py runserver 0.0.0.0:8000`.
6. Crear un usuario administrador de Django con `python manage.py createsuperuser`. Para autenticarse desde Android se usa ese usuario y su contraseña. El login responde un token DRF.

En Android, colocar la IP LAN de la PC que ejecuta Django y el puerto `8000` en la pantalla inicial. En el emulador Android se puede usar `10.0.2.2:8000`; en un teléfono físico se debe usar la IP local de la PC y ambos dispositivos deben estar en la misma red.

## Endpoints incorporados
- `POST /api/login/`: usuario/contraseña, devuelve token y rol.
- `GET /api/productos/por-codigo/<codigo>/`: busca `productos.codigo_barra` en el dump SQL original.
- `GET/POST /api/admin/usuarios/`: listar/crear usuarios con rol (`administrador`, `gerente`, `deposito`, `cajero`).
- `GET/POST /api/admin/sucursales/`: listar/crear datos de comercio/sucursal.
- `GET/POST /api/admin/cajas/`: listar/crear cajas, cajero asignado y horario.

Los endpoints administrativos requieren `Authorization: Token <token>` y un usuario `is_staff`. El panel Android reutiliza automáticamente el token de la sesión iniciada.

## Endpoints Android (Retrofit)

- `POST /api/login/` recibe `username` y `password`, devuelve token DRF y rol.
- `GET /api/productos/` lista los productos activos del SQL importado; admite `?search=`.
- `GET /api/productos/por-codigo/<codigo>/` busca por código de barras.
- `POST /api/productos/`, `PATCH /api/productos/<id>/`, `DELETE /api/productos/<id>/` administran productos con token y permisos.
- `POST /api/ventas/` registra factura y detalle en `facturas` y `factura_detalle`, descuenta stock en una transacción.
- `GET /api/ventas/` y `GET /api/ventas/<id>/` consultan las facturas.
- `/api/admin/usuarios/`, `/api/admin/sucursales/` y `/api/admin/cajas/` gestionan el panel administrativo.

La app Android usa `Token <token>` en el encabezado Authorization. El inicio de sesión Google/Firebase fue deshabilitado: la autenticación para esta versión se hace con usuarios de Django.


### Nota de autenticación y ventas

El inicio de sesión móvil valida cuentas de Django (`auth_user`), no las contraseñas PHP antiguas almacenadas en `usuarios.clave`. Creá el superusuario Django con un nombre de usuario que coincida con un usuario activo del SQL original para que las ventas queden asociadas a ese operador. En un entorno de desarrollo, primero importá el dump SQL y luego ejecutá las migraciones Django. El panel administrativo administra usuarios Django y perfiles de rol, y sincroniza los nuevos usuarios con la tabla `usuarios` del SQL original cuando existen roles y sucursales importados. Las sucursales y cajas nuevas se escriben en las tablas SQL originales; los horarios se guardan en `caja_horarios`.
