package com.example.jugueteria

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {


    private lateinit var edtUsername: EditText
    private lateinit var edtPassword: EditText
    private lateinit var edtServerIp: EditText

    private lateinit var btnLogin: Button

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

        ApiClient.restore(this)

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
        edtServerIp = findViewById(R.id.edtServerIp)
        edtServerIp.setText(getSharedPreferences("jugueteria", MODE_PRIVATE).getString("backend_host", "10.0.2.2:8000"))

        btnLogin =
            findViewById(
                R.id.btnLogin
            )

        // =================================
        // LOGIN EMAIL
        // =================================

        btnLogin.setOnClickListener {

            loginWithEmail()
        }

        // El inicio de sesión se realiza contra Django.
        findViewById<android.widget.LinearLayout>(R.id.googleContainer).visibility = android.view.View.GONE

        if (getSharedPreferences("jugueteria", MODE_PRIVATE).getString("token", null) != null) {
            startActivity(Intent(this, HomeActivity::class.java)); finish()
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
                "Ingresá tu usuario"

            edtUsername.requestFocus()

            return
        }

        if (password.isBlank()) {

            edtPassword.error =
                "Ingresá tu contraseña"

            edtPassword.requestFocus()

            return
        }

        // Configurar la URL de Django y autenticar con Retrofit.
        try { ApiClient.configure(this, edtServerIp.text.toString()) } catch (e: Exception) {
            edtServerIp.error = e.message ?: "IP inválida"
            return
        }
        btnLogin.isEnabled = false
        ApiClient.api.login(ApiLoginRequest(email, password)).enqueue(object : retrofit2.Callback<ApiLoginResponse> {
            override fun onResponse(call: retrofit2.Call<ApiLoginResponse>, response: retrofit2.Response<ApiLoginResponse>) {
                btnLogin.isEnabled = true
                val data = response.body()
                if (response.isSuccessful && !data?.token.isNullOrBlank()) {
                    getSharedPreferences("jugueteria", MODE_PRIVATE).edit()
                        .putString("token", data!!.token).putString("username", data.usuario ?: email).putString("role", data.role ?: "cajero").apply()
                    Toast.makeText(this@MainActivity, "¡Bienvenido!", Toast.LENGTH_SHORT).show()
                    openHome()
                } else Toast.makeText(this@MainActivity, data?.error ?: "Credenciales incorrectas (HTTP ${response.code()})", Toast.LENGTH_LONG).show()
            }
            override fun onFailure(call: retrofit2.Call<ApiLoginResponse>, t: Throwable) {
                btnLogin.isEnabled = true
                Toast.makeText(this@MainActivity, "No se pudo conectar con Django: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        })
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

    private fun isAdmin(): Boolean {
        val prefs = getSharedPreferences("jugueteria", MODE_PRIVATE)
        val role = prefs.getString("role", "cajero")
        return role == "administrador"
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