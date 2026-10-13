package com.example.jugueteria

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

object ApiClient {
    @Volatile private var baseUrl = "http://10.0.2.2:8000/"
    @Volatile private var retrofit: Retrofit? = null
    fun configure(context: Context, host: String) {
        val clean = host.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
        require(clean.isNotBlank()) { "Ingresá la IP del servidor" }
        baseUrl = "http://$clean/"
        context.getSharedPreferences("jugueteria", Context.MODE_PRIVATE).edit().putString("backend_host", clean).apply()
        retrofit = null
    }
    fun restore(context: Context) {
        val host = context.getSharedPreferences("jugueteria", Context.MODE_PRIVATE).getString("backend_host", null)
        if (!host.isNullOrBlank()) baseUrl = "http://${host.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')}/"
    }
    val api: JugueteriaApi get() = (retrofit ?: synchronized(this) {
        retrofit ?: Retrofit.Builder().baseUrl(baseUrl)
            .client(OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build())
            .addConverterFactory(GsonConverterFactory.create()).build().also { retrofit = it }
    }).create(JugueteriaApi::class.java)
}

data class ApiLoginRequest(val username: String, val password: String)
data class ApiLoginResponse(val token: String? = null, val usuario: String? = null, val role: String? = null, val error: String? = null)
data class ApiProduct(val id: Int, val nombre: String, val descripcion: String = "", val precio: Double, val stock: Int, val codigo_barras: String? = null, val categoria: String? = null)
data class ApiMessage(val detail: String? = null, val error: String? = null, val id: Int? = null, val mensaje: String? = null)
data class ApiSaleItem(val productId: Int, val quantity: Int, val unitPrice: Double)
data class ApiSaleRequest(val items: List<ApiSaleItem>, val total: Double, val usuario: String? = null)
data class ApiSaleItemResponse(val producto_id: Int, val nombre: String, val cantidad: Int, val precio_unitario: Double, val subtotal: Double)
data class ApiSaleResponse(val id: Int, val date: String? = null, val total: Double, val userId: String? = null, val clientName: String? = "", val items: List<ApiSaleItemResponse> = emptyList())
data class AdminUserRequest(val username: String, val email: String, val password: String, val role: String)
data class BranchRequest(val nombre: String, val domicilio: String, val razon_social: String)
data class CashRegisterRequest(val sucursal: Int, val nombre: String, val cajero: Int? = null, val hora_apertura: String = "09:00", val hora_cierre: String = "18:00")
data class AdminUserItem(val id: Int, val username: String, val email: String = "", val role: String = "cajero")
data class OfficeItem(val id: Int, val nombre: String, val domicilio: String, val razon_social: String)
data class CashboxItem(val id: Int, val sucursal: Int, val nombre: String, val cajero: Int? = null, val cajero_nombre: String? = null, val hora_apertura: String = "09:00:00", val hora_cierre: String = "18:00:00")

interface JugueteriaApi {
    @POST("api/register/") fun register(@Body body: Map<String, @JvmSuppressWildcards String>): Call<Map<String, Any>>
    @POST("api/login/") fun login(@Body body: ApiLoginRequest): Call<ApiLoginResponse>
    @GET("api/productos/") fun products(@Query("search") search: String? = null): Call<List<ApiProduct>>
    @GET("api/productos/por-codigo/{codigo}/") fun productByBarcode(@Path("codigo") barcode: String): Call<ApiProduct>
    @POST("api/ventas/") fun createSale(@Header("Authorization") token: String, @Body body: ApiSaleRequest): Call<ApiSaleResponse>
    @GET("api/ventas/") fun sales(@Header("Authorization") token: String): Call<List<ApiSaleResponse>>
    @GET("api/ventas/{id}/") fun sale(@Header("Authorization") token: String, @Path("id") id: String): Call<ApiSaleResponse>
    @POST("api/productos/") fun createProduct(@Header("Authorization") token: String, @Body body: Map<String, @JvmSuppressWildcards Any>): Call<ApiProduct>
    @PATCH("api/productos/{id}/") fun updateProduct(@Header("Authorization") token: String, @Path("id") id: String, @Body body: Map<String, @JvmSuppressWildcards Any>): Call<ApiProduct>
    @DELETE("api/productos/{id}/") fun deleteProduct(@Header("Authorization") token: String, @Path("id") id: String): Call<Void>
    @POST("api/admin/usuarios/") fun createUser(@Header("Authorization") token: String, @Body body: AdminUserRequest): Call<ApiMessage>
    @GET("api/admin/usuarios/") fun users(@Header("Authorization") token: String): Call<List<AdminUserItem>>
    @PATCH("api/admin/usuarios/{id}/") fun updateUser(@Header("Authorization") token: String, @Path("id") id: Int, @Body body: Map<String, @JvmSuppressWildcards Any>): Call<ApiMessage>
    @DELETE("api/admin/usuarios/{id}/") fun deleteUser(@Header("Authorization") token: String, @Path("id") id: Int): Call<Void>
    @GET("api/admin/sucursales/") fun offices(@Header("Authorization") token: String): Call<List<OfficeItem>>
    @PATCH("api/admin/sucursales/{id}/") fun updateOffice(@Header("Authorization") token: String, @Path("id") id: Int, @Body body: BranchRequest): Call<ApiMessage>
    @DELETE("api/admin/sucursales/{id}/") fun deleteOffice(@Header("Authorization") token: String, @Path("id") id: Int): Call<Void>
    @GET("api/admin/cajas/") fun cashboxes(@Header("Authorization") token: String): Call<List<CashboxItem>>
    @PATCH("api/admin/cajas/{id}/") fun updateCashbox(@Header("Authorization") token: String, @Path("id") id: Int, @Body body: CashRegisterRequest): Call<ApiMessage>
    @DELETE("api/admin/cajas/{id}/") fun deleteCashbox(@Header("Authorization") token: String, @Path("id") id: Int): Call<Void>
    @POST("api/admin/sucursales/") fun createBranch(@Header("Authorization") token: String, @Body body: BranchRequest): Call<ApiMessage>
    @POST("api/admin/cajas/") fun createCashRegister(@Header("Authorization") token: String, @Body body: CashRegisterRequest): Call<ApiMessage>
}
