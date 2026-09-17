package com.example.jugueteria

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlin.String

data class Client(
    val id: String,
    val userId: String,
    val name: String,
    val email: String,
    val phone: String
)

class FirebaseConnect(
    private val activity: ComponentActivity
) {

    private val auth: FirebaseAuth =
        Firebase.auth

    private val credentialManager: CredentialManager = CredentialManager.create(activity)
    private val db: FirebaseFirestore = Firebase.firestore

    // =====================================================
    // AUTH
    // =====================================================

    fun getCurrentUser(): com.google.firebase.auth.FirebaseUser? {
        return auth.currentUser
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }


    fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }


    fun getCurrentUserName(): String? {
        return auth.currentUser?.displayName
    }


    fun logout() {
        auth.signOut()
    }

    fun registerWithEmail(email: String, password: String, onFinish: (Boolean, Exception?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(activity) { task ->
                if(task.isSuccessful) {
                    val user = auth.currentUser

                    Log.d(
                        "Firebase",
                        "Cuenta creada: ${user?.email}"
                    )

                    onFinish(true, null)
                } else {
                    Log.e(
                        "Firebase",
                        "Error registrando usuario",
                        task.exception
                    )

                    onFinish(false, task.exception)
                }
            }
    }

    fun loginWithEmail(email: String, password: String, onFinish: (Boolean, Exception?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(activity) { task ->

                if (task.isSuccessful) {
                    val user = auth.currentUser

                    Log.d(
                        "Firebase",
                        "Login exitoso: ${user?.email}"
                    )

                    onFinish(true, null)
                } else {
                    Log.e(
                        "Firebase",
                        "Error iniciando sesión",
                        task.exception
                    )

                    onFinish(false, task.exception)
                }
            }
    }

    fun loginWithGoogle(onFinish: (Boolean, Boolean, Exception?) -> Unit) {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(
                activity.getString(R.string.default_web_client_id)
            )
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        activity.lifecycleScope.launch {
            try
            {
                val result = credentialManager.getCredential(
                    context = activity,
                    request = request
                )
                val credential = result.credential
                if(credential is androidx.credentials.CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    firebaseAuthWithGoogle(googleCredential.idToken, onFinish)
                }
                else
                {
                    Log.e(
                        "Firebase",
                        "La credencial obtenida no es una credencial de Google"
                    )
                    onFinish(false, false, null)
                }
            }
            catch(e: GetCredentialException)
            {
                Log.e(
                    "Firebase",
                    "Error iniciando sesión con Google",
                    e
                )
                onFinish(false, false, null)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String, onFinish: (Boolean, Boolean, Exception?) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(activity) { task ->
            if(task.isSuccessful)
            {
                val user = auth.currentUser
                if(user != null)
                {
                    val username = user.displayName ?: "Jugador"
                    val email = user.email ?: ""
                    val result = task.result
                    val isNewUser = result?.additionalUserInfo?.isNewUser == true
                    Log.d(
                        "Firebase",
                        "Login Google exitoso: $username / $email"
                    )
                    onFinish(true, isNewUser, null)
                }
            }
            else
            {
                Log.e(
                    "Firebase",
                    "Error autenticando con Google",
                    task.exception
                )
                onFinish(false, false, task.exception)
            }
        }
    }

    // =====================================================
    // USERS
    // =====================================================

    fun createUserDocument(
        onFinish: (Boolean, Exception?) -> Unit
    ) {

        val user = auth.currentUser

        if (user == null) {
            onFinish(
                false,
                Exception("Usuario no autenticado")
            )
            return
        }


        val data = hashMapOf(
            "name" to (user.displayName ?: ""),
            "email" to (user.email ?: ""),
            "createdAt" to System.currentTimeMillis()
        )


        db.collection("users")
            .document(user.uid)
            .set(data)
            .addOnSuccessListener {

                Log.d(
                    "Firebase",
                    "Usuario guardado en Firestore"
                )

                onFinish(true, null)
            }
            .addOnFailureListener { exception ->

                Log.e(
                    "Firebase",
                    "Error guardando usuario",
                    exception
                )

                onFinish(false, exception)
            }
    }


    // =====================================================
    // PRODUCTS
    // =====================================================

    fun addProduct(
        product: Product,
        onFinish: (Boolean, Exception?) -> Unit
    ) {

        val document =
            db.collection("products")
                .document()


        val data = hashMapOf(
            "name" to product.name,
            "description" to product.description,
            "price" to product.price,
            "stock" to product.stock,
            "category" to product.category,
            "imageUrl" to product.imageRes,
            "createdAt" to System.currentTimeMillis()
        )


        document.set(data)
            .addOnSuccessListener {

                onFinish(true, null)
            }
            .addOnFailureListener { exception ->

                Log.e(
                    "Firebase",
                    "Error agregando producto",
                    exception
                )

                onFinish(false, exception)
            }
    }


    fun getProducts(
        onFinish: (List<Product>, Exception?) -> Unit
    ) {

        db.collection("products")
            .get()
            .addOnSuccessListener { result ->

                val products =
                    result.documents.mapNotNull { document ->

                        document.toProduct()
                    }


                onFinish(products, null)
            }
            .addOnFailureListener { exception ->

                onFinish(
                    emptyList(),
                    exception
                )
            }
    }


    fun updateProduct(
        productId: String,
        product: Product,
        onFinish: (Boolean, Exception?) -> Unit
    ) {

        val data = hashMapOf<String, Any>(
            "name" to product.name,
            "description" to product.description,
            "price" to product.price,
            "stock" to product.stock,
            "category" to product.category,
            "imageUrl" to product.imageRes
        )


        db.collection("products")
            .document(productId)
            .update(data)
            .addOnSuccessListener {

                onFinish(true, null)
            }
            .addOnFailureListener { exception ->

                onFinish(false, exception)
            }
    }


    fun deleteProduct(
        productId: String,
        onFinish: (Boolean, Exception?) -> Unit
    ) {

        db.collection("products")
            .document(productId)
            .delete()
            .addOnSuccessListener {

                onFinish(true, null)
            }
            .addOnFailureListener { exception ->

                onFinish(false, exception)
            }
    }


    // =====================================================
    // SALES
    // =====================================================

    fun addSale(
        sale: Sale,
        onFinish: (Boolean, String?, Exception?) -> Unit
    ) {

        val userId =
            auth.currentUser?.uid


        if (userId == null) {

            onFinish(
                false,
                null,
                Exception("Usuario no autenticado")
            )

            return
        }


        val document =
            db.collection("sales")
                .document()


        val items = sale.items.map { item ->

            hashMapOf(
                "productId" to item.product.id,
                "productName" to item.product.name,
                "quantity" to item.quantity,
                "unitPrice" to item.product.price,
                "subtotal" to item.subtotal
            )
        }


        val data = hashMapOf(
            "userId" to userId,
            "clientId" to sale.clientId,
            "clientName" to sale.clientName,
            "date" to System.currentTimeMillis(),
            "total" to sale.total,
            "items" to items
        )


        document.set(data)
            .addOnSuccessListener {

                Log.d(
                    "Firebase",
                    "Venta guardada: ${document.id}"
                )

                onFinish(
                    true,
                    document.id,
                    null
                )
            }
            .addOnFailureListener { exception ->

                Log.e(
                    "Firebase",
                    "Error guardando venta",
                    exception
                )

                onFinish(
                    false,
                    null,
                    exception
                )
            }
    }


    fun getSales(
        onFinish: (List<Sale>, Exception?) -> Unit
    ) {

        val userId =
            auth.currentUser?.uid


        if (userId == null) {

            onFinish(
                emptyList(),
                Exception("Usuario no autenticado")
            )

            return
        }


        db.collection("sales")
            .whereEqualTo(
                "userId",
                userId
            )
            .get()
            .addOnSuccessListener { result ->

                val sales =
                    result.documents.mapNotNull { document ->

                        document.toSale()
                    }


                onFinish(
                    sales,
                    null
                )
            }
            .addOnFailureListener { exception ->

                onFinish(
                    emptyList(),
                    exception
                )
            }
    }


    fun getSale(
        saleId: String,
        onFinish: (Sale?, Exception?) -> Unit
    ) {

        db.collection("sales")
            .document(saleId)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    onFinish(
                        document.toSale(),
                        null
                    )

                } else {

                    onFinish(
                        null,
                        Exception("Factura no encontrada")
                    )
                }
            }
            .addOnFailureListener { exception ->

                onFinish(
                    null,
                    exception
                )
            }
    }
    fun getClients(
        onFinish: (List<Client>, Exception?) -> Unit
    ) {

        val userId =
            auth.currentUser?.uid

        if (userId == null) {

            onFinish(
                emptyList(),
                Exception("No hay usuario autenticado")
            )

            return
        }

        db.collection("clients")
            .whereEqualTo(
                "userId",
                userId
            )
            .get()
            .addOnSuccessListener { result ->

                val clients =
                    result.documents.map { document ->

                        Client(
                            id = document.id,

                            userId =
                                document.getString(
                                    "userId"
                                ) ?: "",

                            name =
                                document.getString(
                                    "name"
                                ) ?: "",

                            email =
                                document.getString(
                                    "email"
                                ) ?: "",

                            phone =
                                document.getString(
                                    "phone"
                                ) ?: ""
                        )
                    }

                onFinish(
                    clients,
                    null
                )
            }
            .addOnFailureListener { exception ->

                onFinish(
                    emptyList(),
                    exception
                )
            }
    }
    fun getProductById(id: String, onFinish: (Product?, Exception?) -> Unit) {

        db.collection("products")
            .document(id)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    val product = Product(
                        id = document.getString("id") ?: "asdac",
                        imageRes = document.getString("imageUrl") ?: "",
                        name = document.getString("name") ?: "producto",
                        description = document.getString("description") ?: "descripcion",
                        category = document.getString("category") ?: "peluche",
                        price = document.getDouble("price") ?: 0.0,
                        stock = document.getLong("stock")?.toInt() ?: 0
                    )

                    onFinish(
                        product.copy(
                            id = document.id
                        ),
                        null
                    )

                } else {

                    onFinish(
                        null,
                        null
                    )
                }
            }
            .addOnFailureListener {

                onFinish(
                    null,
                    it
                )
            }
    }
}

fun DocumentSnapshot.toProduct(): Product? {

    return try {

        Product(
            id = id,
            name = getString("name") ?: "",
            description =
                getString("description") ?: "",
            price =
                getDouble("price") ?: 0.0,
            stock =
                getLong("stock")?.toInt() ?: 0,
            category =
                getString("category") ?: "",
            imageRes =
                getString("imageUrl") ?: ""
        )

    } catch (e: Exception) {
        null
    }
}

fun DocumentSnapshot.toSale(): Sale? {

    return try {

        val rawItems =
            get("items")
                    as? List<Map<String, Any>>
                ?: emptyList()


        val items =
            rawItems.map { item ->

                val product =
                    Product(
                        id =
                            item["productId"]
                                    as? String
                                ?: "",

                        name =
                            item["productName"]
                                    as? String
                                ?: "",
                        description = "",
                        price =
                            (item["unitPrice"]
                                    as? Number)
                                ?.toDouble()
                                ?: 0.0,
                        category = "",
                        imageRes = "",
                        stock = 0
                    )

                SaleItem(
                    product = product,

                    quantity =
                        (item["quantity"]
                                as? Number)
                            ?.toInt()
                            ?: 0
                )
            }


        Sale(
            id = id,

            userId =
                getString("userId")
                    ?: "",

            clientId =
                getString("clientId")
                    ?: "",

            clientName =
                getString("clientName")
                    ?: "",

            date =
                getLong("date")
                    ?: 0L,

            items = items,

            total =
                getDouble("total")
                    ?: 0.0
        )

    } catch (e: Exception) {

        Log.e(
            "Firebase",
            "Error convirtiendo venta",
            e
        )

        null
    }
}

