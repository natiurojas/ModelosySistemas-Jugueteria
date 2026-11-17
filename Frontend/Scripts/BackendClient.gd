extends Node
## Cliente HTTP global para el backend Django REST Framework.
##
## Registrarlo como Autoload con el nombre `Backend`.
## Ejemplo:
##     var response := await Backend.login("usuario", "clave")
##     if response.ok:
##         print(response.data)
##
## Todas las funciones devuelven un Dictionary con esta forma:
## {
##     "ok": bool,
##     "status": int,
##     "data": Variant,
##     "error": String,
##     "headers": PackedStringArray,
##     "endpoint": String,
## }

signal request_started(method: String, endpoint: String)
signal request_completed(response: Dictionary)
signal request_failed(response: Dictionary)
signal login_completed(response: Dictionary)
signal logout_completed()
signal token_changed(token: String)

@export var base_url: String = "http://127.0.0.1:8000"
@export var request_timeout: float = 15.0
@export var save_token: bool = true
@export var config_path: String = "user://backend.cfg"

var token: String = ""
var username: String = ""
var role: String = ""

var _request_id: int = 0

func _ready() -> void:
	_load_session()

# -----------------------------------------------------------------------------
# HTTP GENÉRICO
# -----------------------------------------------------------------------------

func request(method: HTTPClient.Method, endpoint: String, body: Variant = null, query: Dictionary = {}) -> Dictionary:
	var final_endpoint := _build_endpoint(endpoint, query)
	var url := _build_url(final_endpoint)
	var method_name := _method_to_string(method)

	request_started.emit(method_name, final_endpoint)

	var http := HTTPRequest.new()
	http.timeout = request_timeout
	add_child(http)

	var headers := PackedStringArray(["Accept: application/json"])
	if body != null:
		headers.append("Content-Type: application/json")
	if not token.is_empty():
		headers.append("Authorization: Token %s" % token)

	var payload := ""
	if body != null:
		payload = JSON.stringify(body)

	var request_error := http.request(url, headers, method, payload)
	if request_error != OK:
		http.queue_free()
		var failed := _make_response(false, -1, {"detail": "No se pudo iniciar la petición: %s" % error_string(request_error)}, PackedStringArray(), final_endpoint)
		request_failed.emit(failed)
		return failed

	var result: Array = await http.request_completed
	http.queue_free()

	var transport_result: int = int(result[0])
	var response_code: int = int(result[1])
	var response_headers: PackedStringArray = result[2]
	var raw_body: PackedByteArray = result[3]

	if transport_result != HTTPRequest.RESULT_SUCCESS:
		var transport_error := _transport_error_message(transport_result)
		var failed := _make_response(false, response_code, {"detail": transport_error}, response_headers, final_endpoint)
		request_failed.emit(failed)
		return failed

	var parsed := _parse_body(raw_body)
	var ok := response_code >= 200 and response_code < 300

	var response := _make_response(ok, response_code, parsed.data, response_headers, final_endpoint)
	if not ok:
		response.error = _extract_error(parsed.data, response_code)
		request_failed.emit(response)
	else:
		request_completed.emit(response)

	return response

func get_endpoint(endpoint: String, query: Dictionary = {}) -> Dictionary:
	return await request(HTTPClient.METHOD_GET, endpoint, null, query)

func post(endpoint: String, body: Variant = null, query: Dictionary = {}) -> Dictionary:
	return await request(HTTPClient.METHOD_POST, endpoint, body, query)

func put(endpoint: String, body: Variant = null, query: Dictionary = {}) -> Dictionary:
	return await request(HTTPClient.METHOD_PUT, endpoint, body, query)

func patch(endpoint: String, body: Variant = null, query: Dictionary = {}) -> Dictionary:
	return await request(HTTPClient.METHOD_PATCH, endpoint, body, query)

func delete(endpoint: String, query: Dictionary = {}) -> Dictionary:
	return await request(HTTPClient.METHOD_DELETE, endpoint, null, query)

# -----------------------------------------------------------------------------
# AUTENTICACIÓN
# -----------------------------------------------------------------------------

func login(email: String, password: String) -> Dictionary:
	var response := await post("/api/login/", {
		"email": email,
		"password": password
	})

	if response.ok and response.data is Dictionary:
		var data: Dictionary = response.data
		token = str(data.get("token", ""))
		username = str(data.get("usuario", email))
		role = str(data.get("role", ""))
		_save_session()
		token_changed.emit(token)

	login_completed.emit(response)
	return response

func register(user_data: Dictionary) -> Dictionary:
	return await post("/api/register/", user_data)

func logout() -> void:
	token = ""
	username = ""
	role = ""
	_clear_session()
	token_changed.emit("")
	logout_completed.emit()

func is_authenticated() -> bool:
	return not token.is_empty()

func set_token(new_token: String) -> void:
	token = new_token.strip_edges()
	if save_token:
		_save_session()
	token_changed.emit(token)

func set_base_url(url: String) -> void:
	base_url = url.strip_edges().trim_suffix("/")

# -----------------------------------------------------------------------------
# PRODUCTOS
# -----------------------------------------------------------------------------

func get_productos(search: String = "", limit: int = 50, offset: int = 0) -> Dictionary:
	var query := {"limit": limit, "offset": offset}
	if not search.strip_edges().is_empty():
		query["search"] = search.strip_edges()
	return await get_endpoint("/api/productos/", query)

func get_producto_por_codigo(codigo: String) -> Dictionary:
	return await get_endpoint("/api/productos/por-codigo/%s/" % codigo.uri_encode())

func crear_producto(producto: Dictionary) -> Dictionary:
	return await post("/api/productos/", producto)

func actualizar_producto(id: int, producto: Dictionary) -> Dictionary:
	return await patch("/api/productos/%d/" % id, producto)

func reemplazar_producto(id: int, producto: Dictionary) -> Dictionary:
	return await put("/api/productos/%d/" % id, producto)

func eliminar_producto(id: int) -> Dictionary:
	return await delete("/api/productos/%d/" % id)

# -----------------------------------------------------------------------------
# VENTAS
# -----------------------------------------------------------------------------

func get_ventas(periodo: String = "", fecha: String = "", mes: String = "", anio: String = "") -> Dictionary:
	var query := {}
	if not periodo.is_empty():
		query["periodo"] = periodo
	if not fecha.is_empty():
		query["fecha"] = fecha
	if not mes.is_empty():
		query["mes"] = mes
	if not anio.is_empty():
		query["anio"] = anio
	return await get_endpoint("/api/ventas/", query)

func get_venta(id: int) -> Dictionary:
	return await get_endpoint("/api/ventas/%d/" % id)

func crear_venta(items: Array) -> Dictionary:
	return await post("/api/ventas/", {"items": items})

# -----------------------------------------------------------------------------
# ADMINISTRACIÓN - USUARIOS
# -----------------------------------------------------------------------------

func get_usuarios() -> Dictionary:
	return await get_endpoint("/api/admin/usuarios/")

func crear_usuario(usuario_data: Dictionary) -> Dictionary:
	return await post("/api/admin/usuarios/", usuario_data)

func actualizar_usuario(id: int, usuario_data: Dictionary) -> Dictionary:
	return await patch("/api/admin/usuarios/%d/" % id, usuario_data)

func eliminar_usuario(id: int) -> Dictionary:
	return await delete("/api/admin/usuarios/%d/" % id)

# -----------------------------------------------------------------------------
# ADMINISTRACIÓN - SUCURSALES
# -----------------------------------------------------------------------------

func get_sucursales() -> Dictionary:
	return await get_endpoint("/api/admin/sucursales/")

func crear_sucursal(sucursal_data: Dictionary) -> Dictionary:
	return await post("/api/admin/sucursales/", sucursal_data)

func actualizar_sucursal(id: int, sucursal_data: Dictionary) -> Dictionary:
	return await patch("/api/admin/sucursales/%d/" % id, sucursal_data)

func eliminar_sucursal(id: int) -> Dictionary:
	return await delete("/api/admin/sucursales/%d/" % id)

# -----------------------------------------------------------------------------
# ADMINISTRACIÓN - CAJAS
# -----------------------------------------------------------------------------

func get_cajas() -> Dictionary:
	return await get_endpoint("/api/admin/cajas/")

func crear_caja(caja_data: Dictionary) -> Dictionary:
	return await post("/api/admin/cajas/", caja_data)

func actualizar_caja(id: int, caja_data: Dictionary) -> Dictionary:
	return await patch("/api/admin/cajas/%d/" % id, caja_data)

func eliminar_caja(id: int) -> Dictionary:
	return await delete("/api/admin/cajas/%d/" % id)

# -----------------------------------------------------------------------------
# GERENTE - CONFIGURACIÓN
# -----------------------------------------------------------------------------

func get_configuracion_gerente() -> Dictionary:
	return await get_endpoint("/api/gerente/configuracion/")

func actualizar_configuracion_gerente(configuracion: Dictionary) -> Dictionary:
	return await patch("/api/gerente/configuracion/", configuracion)

# -----------------------------------------------------------------------------
# GERENTE - REPORTES
# -----------------------------------------------------------------------------

func get_reporte_cajas(desde: String = "", hasta: String = "") -> Dictionary:
	return await _get_reporte("cajas", desde, hasta)

func get_reporte_productos(desde: String = "", hasta: String = "") -> Dictionary:
	return await _get_reporte("productos", desde, hasta)

func get_reporte_recaudacion(fecha: String) -> Dictionary:
	return await get_endpoint("/api/gerente/reportes/", {
		"tipo": "recaudacion",
		"fecha": fecha
	})

func get_reporte_ganancia(fecha: String) -> Dictionary:
	return await get_endpoint("/api/gerente/reportes/", {
		"tipo": "ganancia",
		"fecha": fecha
	})

func _get_reporte(tipo: String, desde: String = "", hasta: String = "") -> Dictionary:
	var query := {"tipo": tipo}
	if not desde.strip_edges().is_empty():
		query["desde"] = desde
	if not hasta.strip_edges().is_empty():
		query["hasta"] = hasta
	return await get_endpoint("/api/gerente/reportes/", query)

# -----------------------------------------------------------------------------
# CARRITO / ENDPOINT DE PRUEBA
# -----------------------------------------------------------------------------

func get_carrito() -> Dictionary:
	return await get_endpoint("/api/carrito/")

func get_prueba() -> Dictionary:
	return await get_endpoint("/api/prueba/")

# -----------------------------------------------------------------------------
# UTILIDADES
# -----------------------------------------------------------------------------

func _build_url(endpoint: String) -> String:
	var clean_base := base_url.strip_edges().trim_suffix("/")
	var clean_endpoint := endpoint.strip_edges()
	if not clean_endpoint.begins_with("/"):
		clean_endpoint = "/" + clean_endpoint
	return clean_base + clean_endpoint

func _build_endpoint(endpoint: String, query: Dictionary) -> String:
	if query.is_empty():
		return endpoint

	var parts: Array[String] = []
	for key in query.keys():
		var value = query[key]
		if value == null:
			continue
		parts.append("%s=%s" % [str(key).uri_encode(), str(value).uri_encode()])

	if parts.is_empty():
		return endpoint

	return "%s%s%s" % [endpoint, "?" if not endpoint.contains("?") else "&", "&".join(parts)]

func _parse_body(body: PackedByteArray) -> Dictionary:
	if body.is_empty():
		return {"data": {}}

	var text_body := body.get_string_from_utf8().strip_edges()
	if text_body.is_empty():
		return {"data": {}}

	var json := JSON.new()
	var parse_error := json.parse(text_body)
	if parse_error != OK:
		return {
			"data": {
				"detail": text_body
			}
		}

	return {"data": json.data}

func _make_response(ok: bool, status: int, data: Variant, headers: PackedStringArray, endpoint: String) -> Dictionary:
	return {
		"ok": ok,
		"status": status,
		"data": data,
		"error": "" if ok else _extract_error(data, status),
		"headers": headers,
		"endpoint": endpoint
	}

func _extract_error(data: Variant, status: int) -> String:
	if data is Dictionary:
		var dictionary: Dictionary = data
		if dictionary.has("detail"):
			return str(dictionary["detail"])
		if dictionary.has("error"):
			return str(dictionary["error"])
		if dictionary.has("message"):
			return str(dictionary["message"])
		return JSON.stringify(dictionary)

	if data is String and not str(data).is_empty():
		return str(data)

	return "Error HTTP %d" % status

func _method_to_string(method: HTTPClient.Method) -> String:
	match method:
		HTTPClient.METHOD_GET: return "GET"
		HTTPClient.METHOD_POST: return "POST"
		HTTPClient.METHOD_PUT: return "PUT"
		HTTPClient.METHOD_PATCH: return "PATCH"
		HTTPClient.METHOD_DELETE: return "DELETE"
		_:
			return str(method)

func _transport_error_message(result: int) -> String:
	match result:
		HTTPRequest.RESULT_CHUNKED_BODY_SIZE_MISMATCH:
			return "El tamaño del cuerpo recibido no coincide."
		HTTPRequest.RESULT_CANT_CONNECT:
			return "No se pudo conectar con el backend."
		HTTPRequest.RESULT_CANT_RESOLVE:
			return "No se pudo resolver la dirección del backend."
		HTTPRequest.RESULT_CONNECTION_ERROR:
			return "Se produjo un error de conexión."
		HTTPRequest.RESULT_TLS_HANDSHAKE_ERROR:
			return "Falló el handshake TLS/HTTPS."
		HTTPRequest.RESULT_NO_RESPONSE:
			return "El backend no respondió."
		HTTPRequest.RESULT_BODY_SIZE_LIMIT_EXCEEDED:
			return "La respuesta del backend excede el tamaño permitido."
		HTTPRequest.RESULT_REQUEST_FAILED:
			return "La petición HTTP falló."
		HTTPRequest.RESULT_DOWNLOAD_FILE_CANT_OPEN:
			return "No se pudo abrir el archivo de descarga."
		HTTPRequest.RESULT_DOWNLOAD_FILE_WRITE_ERROR:
			return "No se pudo escribir la respuesta."
		_:
			return "Error de transporte HTTP (código %d)." % result

# -----------------------------------------------------------------------------
# SESIÓN LOCAL
# -----------------------------------------------------------------------------

func _save_session() -> void:
	if not save_token:
		return

	var config := ConfigFile.new()
	config.set_value("auth", "token", token)
	config.set_value("auth", "username", username)
	config.set_value("auth", "role", role)
	config.save(config_path)

func _load_session() -> void:
	if not save_token:
		return

	var config := ConfigFile.new()
	if config.load(config_path) != OK:
		return

	token = str(config.get_value("auth", "token", ""))
	username = str(config.get_value("auth", "username", ""))
	role = str(config.get_value("auth", "role", ""))

func _clear_session() -> void:
	var config := ConfigFile.new()
	config.set_value("auth", "token", "")
	config.set_value("auth", "username", "")
	config.set_value("auth", "role", "")
	config.save(config_path)
