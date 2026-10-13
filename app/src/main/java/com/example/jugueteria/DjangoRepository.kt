package com.example.jugueteria

import androidx.activity.ComponentActivity
import java.time.OffsetDateTime
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/** Repositorio de acceso al backend Django. Todas las operaciones HTTP usan Retrofit. */
data class Client(val id: String, val userId: String, val name: String, val email: String, val phone: String)

class DjangoRepository(private val activity: ComponentActivity) {
    private fun token(): String = activity.getSharedPreferences("jugueteria", 0).getString("token", "") ?: ""
    private fun authHeader(): String = "Token ${token()}"
    fun getCurrentUserId(): String? = activity.getSharedPreferences("jugueteria", 0).getString("username", null)
    fun getCurrentUserEmail(): String? = null
    fun getCurrentUserName(): String? = getCurrentUserId()
    fun logout() { activity.getSharedPreferences("jugueteria", 0).edit().remove("token").remove("username").remove("role").apply() }
    fun getCurrentUser(): Any? = if (token().isBlank()) null else getCurrentUserId()

    fun registerWithEmail(email: String, password: String, onFinish: (Boolean, Exception?) -> Unit) {
        ApiClient.api.register(mapOf("username" to email, "email" to email, "password" to password)).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) { onFinish(response.isSuccessful, if (response.isSuccessful) null else Exception("No se pudo registrar el usuario (${response.code()})")) }
            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) = onFinish(false, Exception(t.message ?: "Error de conexión"))
        })
    }
    fun loginWithEmail(email: String, password: String, onFinish: (Boolean, Exception?) -> Unit) {
        ApiClient.api.login(ApiLoginRequest(email, password)).enqueue(object : Callback<ApiLoginResponse> {
            override fun onResponse(call: Call<ApiLoginResponse>, response: Response<ApiLoginResponse>) {
                val b = response.body(); if (response.isSuccessful && !b?.token.isNullOrBlank()) {
                    activity.getSharedPreferences("jugueteria", 0).edit().putString("token", b!!.token).putString("username", b.usuario ?: email).putString("role", b.role ?: "cajero").apply(); onFinish(true, null)
                } else onFinish(false, Exception(b?.error ?: "Credenciales incorrectas"))
            }
            override fun onFailure(call: Call<ApiLoginResponse>, t: Throwable) = onFinish(false, Exception(t.message ?: "Error de conexión"))
        })
    }
    fun createUserDocument(onFinish: (Boolean, Exception?) -> Unit) = onFinish(true, null)

    private fun mapProduct(p: ApiProduct) = Product(p.id.toString(), "", p.nombre, p.descripcion, p.categoria ?: "", p.precio, p.stock)
    fun getProducts(onFinish: (List<Product>, Exception?) -> Unit) {
        ApiClient.api.products().enqueue(object : Callback<List<ApiProduct>> {
            override fun onResponse(call: Call<List<ApiProduct>>, response: Response<List<ApiProduct>>) { if (response.isSuccessful) onFinish(response.body().orEmpty().map(::mapProduct), null) else onFinish(emptyList(), Exception("Error consultando productos (${response.code()})")) }
            override fun onFailure(call: Call<List<ApiProduct>>, t: Throwable) = onFinish(emptyList(), Exception(t.message ?: "Error de conexión"))
        })
    }
    fun getProductById(id: String, onFinish: (Product?, Exception?) -> Unit) {
        ApiClient.api.products().enqueue(object : Callback<List<ApiProduct>> {
            override fun onResponse(call: Call<List<ApiProduct>>, response: Response<List<ApiProduct>>) { val p = response.body().orEmpty().firstOrNull { it.id.toString() == id }; onFinish(p?.let(::mapProduct), if (response.isSuccessful) null else Exception("Error consultando producto")) }
            override fun onFailure(call: Call<List<ApiProduct>>, t: Throwable) = onFinish(null, Exception(t.message ?: "Error de conexión"))
        })
    }
    fun addProduct(product: Product, onFinish: (Boolean, Exception?) -> Unit) {
        val body = mapOf("descripcion" to product.name, "precio_final" to product.price, "stock" to product.stock, "familia" to product.category, "activo" to 1)
        ApiClient.api.createProduct(authHeader(), body).enqueue(object : Callback<ApiProduct> {
            override fun onResponse(call: Call<ApiProduct>, response: Response<ApiProduct>) = onFinish(response.isSuccessful, if (response.isSuccessful) null else Exception("Error creando producto (${response.code()})"))
            override fun onFailure(call: Call<ApiProduct>, t: Throwable) = onFinish(false, Exception(t.message ?: "Error de conexión"))
        })
    }
    fun updateProduct(productId: String, product: Product, onFinish: (Boolean, Exception?) -> Unit) {
        val body = mapOf("descripcion" to product.name, "precio_final" to product.price, "stock" to product.stock, "familia" to product.category)
        ApiClient.api.updateProduct(authHeader(), productId, body).enqueue(object : Callback<ApiProduct> {
            override fun onResponse(call: Call<ApiProduct>, response: Response<ApiProduct>) = onFinish(response.isSuccessful, if (response.isSuccessful) null else Exception("Error actualizando producto (${response.code()})"))
            override fun onFailure(call: Call<ApiProduct>, t: Throwable) = onFinish(false, Exception(t.message ?: "Error de conexión"))
        })
    }
    fun deleteProduct(productId: String, onFinish: (Boolean, Exception?) -> Unit) {
        ApiClient.api.deleteProduct(authHeader(), productId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) = onFinish(response.isSuccessful, if (response.isSuccessful) null else Exception("Error eliminando producto (${response.code()})"))
            override fun onFailure(call: Call<Void>, t: Throwable) = onFinish(false, Exception(t.message ?: "Error de conexión"))
        })
    }
    fun addSale(sale: Sale, onFinish: (Boolean, String?, Exception?) -> Unit) {
        val body = ApiSaleRequest(sale.items.map { ApiSaleItem(it.product.id.toIntOrNull() ?: -1, it.quantity, it.product.price) }, sale.total, getCurrentUserId())
        ApiClient.api.createSale(authHeader(), body).enqueue(object : Callback<ApiSaleResponse> {
            override fun onResponse(call: Call<ApiSaleResponse>, response: Response<ApiSaleResponse>) { val b=response.body(); onFinish(response.isSuccessful, b?.id?.toString(), if(response.isSuccessful) null else Exception("Error registrando venta (${response.code()})")) }
            override fun onFailure(call: Call<ApiSaleResponse>, t: Throwable) = onFinish(false, null, Exception(t.message ?: "Error de conexión"))
        })
    }
    private fun mapSale(s: ApiSaleResponse): Sale {
        val items = s.items.map { i -> SaleItem(Product(i.producto_id.toString(), "", i.nombre, "", "", i.precio_unitario, 0), i.cantidad) }
        val timestamp = try { s.date?.let { OffsetDateTime.parse(it).toInstant().toEpochMilli() } ?: 0L } catch (_: Exception) { 0L }
        return Sale(s.id.toString(), s.userId ?: "", "", s.clientName ?: "", timestamp, items, s.total)
    }
    fun getSales(onFinish: (List<Sale>, Exception?) -> Unit) {
        ApiClient.api.sales(authHeader()).enqueue(object : Callback<List<ApiSaleResponse>> {
            override fun onResponse(call: Call<List<ApiSaleResponse>>, response: Response<List<ApiSaleResponse>>) { onFinish(if(response.isSuccessful) response.body().orEmpty().map(::mapSale) else emptyList(), if(response.isSuccessful) null else Exception("Error consultando facturas (${response.code()})")) }
            override fun onFailure(call: Call<List<ApiSaleResponse>>, t: Throwable) = onFinish(emptyList(), Exception(t.message ?: "Error de conexión"))
        })
    }
    fun getSale(saleId: String, onFinish: (Sale?, Exception?) -> Unit) {
        ApiClient.api.sale(authHeader(), saleId).enqueue(object : Callback<ApiSaleResponse> {
            override fun onResponse(call: Call<ApiSaleResponse>, response: Response<ApiSaleResponse>) { onFinish(if(response.isSuccessful) response.body()?.let(::mapSale) else null, if(response.isSuccessful) null else Exception("Factura no encontrada")) }
            override fun onFailure(call: Call<ApiSaleResponse>, t: Throwable) = onFinish(null, Exception(t.message ?: "Error de conexión"))
        })
    }
    fun getClients(onFinish: (List<Client>, Exception?) -> Unit) = onFinish(emptyList(), null)
}
