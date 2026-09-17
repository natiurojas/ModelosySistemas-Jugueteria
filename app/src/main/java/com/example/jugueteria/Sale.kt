package com.example.jugueteria

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Sale(
    val id: String = "",
    val userId: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val date: Long = 0L,
    val items: List<SaleItem> = emptyList(),
    val total: Double = 0.0
)

object SaleRepository {

    private val sales = mutableListOf<Sale>()

    private var nextId = 1


    fun addSale(sale: Sale) {
        sales.add(sale)
    }


    fun getSales(): List<Sale> {
        return sales.toList()
    }


    fun generateId(): Int {
        return nextId++
    }

    fun getSaleById(id: String): Sale? {
        return sales.find {
            it.id == id
        }
    }
}