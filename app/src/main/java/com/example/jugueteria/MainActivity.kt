package com.example.jugueteria

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var firebase: FirebaseConnect

    private lateinit var edtUsername: EditText
    private lateinit var edtPassword: EditText

    private lateinit var btnLogin: Button
    private lateinit var googleContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(
            R.layout.activity_main
        )

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

        edtUsername =
            findViewById(
                R.id.edtUsername
            )

        edtPassword =
            findViewById(
                R.id.edtPassword
            )

        btnLogin =
            findViewById(
                R.id.btnLogin
            )

        googleContainer =
            findViewById(
                R.id.googleContainer
            )

        // =================================
        // LOGIN EMAIL
        // =================================

        btnLogin.setOnClickListener {

            loginWithEmail()
        }

        // =================================
        // LOGIN GOOGLE
        // =================================

        googleContainer.setOnClickListener {

            loginWithGoogle()
        }
        if(firebase.getCurrentUser() != null)
        {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    // =====================================
    // LOGIN CON EMAIL
    // =====================================

    private fun loginWithEmail() {

        val email =
            edtUsername.text
                .toString()
                .trim()

        val password =
            edtPassword.text
                .toString()

        // =================================
        // VALIDACIONES
        // =================================

        if (email.isBlank()) {

            edtUsername.error =
                "Ingresá tu correo"

            edtUsername.requestFocus()

            return
        }

        if (password.isBlank()) {

            edtPassword.error =
                "Ingresá tu contraseña"

            edtPassword.requestFocus()

            return
        }

        // =================================
        // FIREBASE LOGIN
        // =================================

        btnLogin.isEnabled = false

        firebase.loginWithEmail(
            email,
            password
        ) { success, exception ->

            btnLogin.isEnabled = true

            if (success) {

                Toast.makeText(
                    this,
                    "¡Bienvenido!",
                    Toast.LENGTH_SHORT
                ).show()

                openHome()

            } else {

                Toast.makeText(
                    this,
                    getLoginErrorMessage(
                        exception
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // =====================================
    // LOGIN GOOGLE
    // =====================================

    private fun loginWithGoogle() {

        googleContainer.isEnabled = false

        firebase.loginWithGoogle {
                success,
                isNewUser,
                exception ->

            googleContainer.isEnabled = true

            if (success) {

                Toast.makeText(
                    this,
                    "¡Bienvenido!",
                    Toast.LENGTH_SHORT
                ).show()

                openHome()

            } else {

                Toast.makeText(
                    this,
                    "No se pudo iniciar sesión con Google",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // =====================================
    // ABRIR HOME
    // =====================================

    private fun openHome() {

        val intent =
            Intent(
                this,
                HomeActivity::class.java
            )

        startActivity(intent)

        finish()
    }

    // =====================================
    // MENSAJES DE ERROR
    // =====================================

    private fun getLoginErrorMessage(
        exception: Exception?
    ): String {

        return when {

            exception == null ->
                "No se pudo iniciar sesión"

            exception.message?.contains(
                "INVALID_LOGIN_CREDENTIALS"
            ) == true ->
                "Correo o contraseña incorrectos"

            exception.message?.contains(
                "INVALID_EMAIL"
            ) == true ->
                "El correo no es válido"

            exception.message?.contains(
                "USER_NOT_FOUND"
            ) == true ->
                "No existe una cuenta con ese correo"

            exception.message?.contains(
                "WRONG_PASSWORD"
            ) == true ->
                "Contraseña incorrecta"

            else ->
                exception.message
                    ?: "Error iniciando sesión"
        }
    }
}