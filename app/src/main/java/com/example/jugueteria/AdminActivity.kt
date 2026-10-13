package com.example.jugueteria

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class AdminActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.restore(this)
        setContentView(R.layout.activity_admin)
        findViewById<Button>(R.id.btnManageUsers).setOnClickListener { startActivity(Intent(this, UsersActivity::class.java)) }
        findViewById<Button>(R.id.btnManageOffices).setOnClickListener { startActivity(Intent(this, OfficeActivity::class.java)) }
        findViewById<Button>(R.id.btnManageCashboxes).setOnClickListener { startActivity(Intent(this, CashboxActivity::class.java)) }
        findViewById<android.widget.ImageButton>(R.id.btnAdminBack).setOnClickListener { finish() }
    }
}
