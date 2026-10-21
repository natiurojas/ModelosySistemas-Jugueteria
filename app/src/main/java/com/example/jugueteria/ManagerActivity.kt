package com.example.jugueteria

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class ManagerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.restore(this)
        setContentView(R.layout.activity_manager)
        findViewById<ImageButton>(R.id.btnManagerBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnManagerHours).setOnClickListener { open(ManagerSettingsActivity::class.java) }
        findViewById<Button>(R.id.btnManagerStock).setOnClickListener { open(ManagerStockActivity::class.java) }
        findViewById<Button>(R.id.btnManagerCashHistory).setOnClickListener { open(ManagerCashHistoryActivity::class.java) }
        findViewById<Button>(R.id.btnManagerProductHistory).setOnClickListener { open(ManagerProductHistoryActivity::class.java) }
        findViewById<Button>(R.id.btnManagerRevenue).setOnClickListener { open(ManagerRevenueActivity::class.java) }
        findViewById<Button>(R.id.btnManagerProfit).setOnClickListener { open(ManagerProfitActivity::class.java) }
    }
    private fun open(c: Class<*>) {
        startActivity(Intent(this, c))
    }
}
