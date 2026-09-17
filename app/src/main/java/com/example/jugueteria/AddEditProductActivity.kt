package com.example.jugueteria

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AddEditProductActivity : AppCompatActivity() {

    private lateinit var firebase: FirebaseConnect

    private lateinit var txtTitle: TextView

    private lateinit var edtName: EditText
    private lateinit var edtDescription: EditText
    private lateinit var edtCategory: EditText
    private lateinit var edtPrice: EditText
    private lateinit var edtStock: EditText

    private lateinit var btnSave: Button
    private lateinit var btnBack: ImageButton

    // Si es null → producto nuevo
    // Si tiene valor → estamos editando
    private var productId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(
            R.layout.activity_add_edit_product
        )

        // =================================
        // INSETS
        // =================================

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { v, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        // =================================
        // FIREBASE
        // =================================

        firebase =
            FirebaseConnect(this)

        // =================================
        // VIEWS
        // =================================

        txtTitle =
            findViewById(R.id.txtTitle)

        edtName =
            findViewById(R.id.edtName)

        edtDescription =
            findViewById(R.id.edtDescription)

        edtCategory =
            findViewById(R.id.edtCategory)

        edtPrice =
            findViewById(R.id.edtPrice)

        edtStock =
            findViewById(R.id.edtStock)

        btnSave =
            findViewById(R.id.btnSave)

        btnBack =
            findViewById(R.id.btnBack)

        // =================================
        // OBTENER PRODUCT ID
        // =================================

        productId =
            intent.getStringExtra("PRODUCT_ID")

        // =================================
        // MODO
        // =================================

        if (productId == null) {

            // -----------------------------
            // NUEVO PRODUCTO
            // -----------------------------

            txtTitle.text =
                "Nuevo producto"

            btnSave.text =
                "Guardar producto"

        } else {

            // -----------------------------
            // EDITAR PRODUCTO
            // -----------------------------

            txtTitle.text =
                "Editar producto"

            btnSave.text =
                "Guardar cambios"

            cargarProducto(
                productId!!
            )
        }

        // =================================
        // VOLVER
        // =================================

        btnBack.setOnClickListener {

            finish()
        }

        // =================================
        // GUARDAR
        // =================================

        btnSave.setOnClickListener {

            guardarProducto()
        }
    }

    // =====================================
    // CARGAR PRODUCTO
    // =====================================

    private fun cargarProducto(
        id: String
    ) {

        firebase.getProductById(
            id
        ) { product, error ->

            if (error != null) {

                Toast.makeText(
                    this,
                    "Error cargando producto",
                    Toast.LENGTH_LONG
                ).show()

                finish()

                return@getProductById
            }

            if (product == null) {

                Toast.makeText(
                    this,
                    "Producto no encontrado",
                    Toast.LENGTH_LONG
                ).show()

                finish()

                return@getProductById
            }

            // =================================
            // MOSTRAR DATOS
            // =================================

            edtName.setText(
                product.name
            )

            edtDescription.setText(
                product.description
            )

            edtCategory.setText(
                product.category
            )

            edtPrice.setText(
                product.price.toString()
            )

            edtStock.setText(
                product.stock.toString()
            )
        }
    }

    // =====================================
    // GUARDAR PRODUCTO
    // =====================================

    private fun guardarProducto() {

        val name =
            edtName.text
                .toString()
                .trim()

        val description =
            edtDescription.text
                .toString()
                .trim()

        val category =
            edtCategory.text
                .toString()
                .trim()

        val priceText =
            edtPrice.text
                .toString()
                .trim()

        val stockText =
            edtStock.text
                .toString()
                .trim()

        // =================================
        // VALIDACIONES
        // =================================

        if (name.isBlank()) {

            edtName.error =
                "Ingresá el nombre"

            edtName.requestFocus()

            return
        }

        if (category.isBlank()) {

            edtCategory.error =
                "Ingresá una categoría"

            edtCategory.requestFocus()

            return
        }

        if (priceText.isBlank()) {

            edtPrice.error =
                "Ingresá el precio"

            edtPrice.requestFocus()

            return
        }

        if (stockText.isBlank()) {

            edtStock.error =
                "Ingresá el stock"

            edtStock.requestFocus()

            return
        }

        val price =
            priceText.toDoubleOrNull()

        if (price == null || price < 0) {

            edtPrice.error =
                "Precio inválido"

            edtPrice.requestFocus()

            return
        }

        val stock =
            stockText.toIntOrNull()

        if (stock == null || stock < 0) {

            edtStock.error =
                "Stock inválido"

            edtStock.requestFocus()

            return
        }

        // =================================
        // CREAR OBJETO
        // =================================

        val product =
            Product(
                id = productId ?: "",
                name = name,
                description = description,
                price = price,
                stock = stock,
                category = category,
                imageRes = ""
            )

        btnSave.isEnabled = false

        // =================================
        // NUEVO
        // =================================

        if (productId == null) {

            firebase.addProduct(
                product
            ) { success, error ->

                btnSave.isEnabled = true

                if (success) {

                    Toast.makeText(
                        this,
                        "Producto agregado",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()

                } else {

                    Toast.makeText(
                        this,
                        "Error guardando producto",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        } else {

            // =================================
            // EDITAR
            // =================================

            firebase.updateProduct(
                product.id,
                product
            ) { success, error ->

                btnSave.isEnabled = true

                if (success) {

                    Toast.makeText(
                        this,
                        "Producto actualizado",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()

                } else {

                    Toast.makeText(
                        this,
                        "Error actualizando producto",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}