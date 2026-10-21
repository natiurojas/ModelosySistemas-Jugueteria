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

## Panel administrativo Android

El panel se divide en `AdminActivity`, `UsersActivity` / `UserEditActivity`, `OfficeActivity` / `OfficeEditActivity` y `CashboxActivity` / `CashboxEditActivity`. Las operaciones usan los endpoints protegidos por token de Django:

- `GET/POST api/admin/usuarios/`, `PATCH/DELETE api/admin/usuarios/<id>/`
- `GET/POST api/admin/sucursales/`, `PATCH/DELETE api/admin/sucursales/<id>/`
- `GET/POST api/admin/cajas/`, `PATCH/DELETE api/admin/cajas/<id>/`

Para acceder a estas rutas, el token debe pertenecer a un usuario administrador (`is_staff` / `is_superuser`). Las cajas asocian el usuario de Django con el usuario operativo de la base SQL mediante el nombre de usuario.


## Módulo Gerente

El módulo Gerente se abre al iniciar sesión con un usuario cuyo perfil Django tenga el rol `gerente` (o con un administrador). Incluye:

- `GET/PATCH /api/gerente/configuracion/`: consulta sucursales, horarios de cajas y límite de ventas; permite modificar el horario de una caja o el límite de ventas de la sucursal.
- `GET /api/gerente/reportes/?tipo=cajas`: histórico de ventas por caja.
- `GET /api/gerente/reportes/?tipo=productos`: histórico de productos vendidos y cantidades.
- `GET /api/gerente/reportes/?tipo=recaudacion&fecha=AAAA-MM-DD`: recaudación del día.
- `GET /api/gerente/reportes/?tipo=ganancia&fecha=AAAA-MM-DD`: ganancia estimada del día, calculada como subtotal vendido menos el costo registrado de los productos.

Los informes de históricos admiten `desde` y `hasta` con formato `AAAA-MM-DD`. Los endpoints requieren `Authorization: Token <token>` y el rol `gerente` o `administrador`. Las fechas de los informes y las validaciones horarias se interpretan en `America/Argentina/Buenos_Aires`. Las ventas nuevas respetan el horario de la caja asignada al usuario y la hora límite de ventas configurada para su sucursal.

Después de actualizar el proyecto, ejecutar `python manage.py migrate` para crear las tablas `caja_horarios` y `gerente_configuracion`.
