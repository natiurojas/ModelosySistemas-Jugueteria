package com.example.jugueteria

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Product(
    val id: String,
    val imageRes: String,
    val name: String,
    val description: String,
    val category: String,
    val price: Double,
    val stock: Int
)

class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val imgProduct: ImageView = itemView.findViewById(R.id.imgProduct)
    val txtProductName: TextView = itemView.findViewById(R.id.txtProductName)
    val txtCategory: TextView = itemView.findViewById(R.id.txtCategory)
    val txtPrice: TextView = itemView.findViewById(R.id.txtPrice)
    val txtStock: TextView = itemView.findViewById(R.id.txtStock)
    val btnView: ImageButton = itemView.findViewById(R.id.btnView)
    val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
    val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
}

class ProductAdapter(
    private val products: List<Product>,
    private val onView: (Product) -> Unit,
    private val onEdit: (Product) -> Unit,
    private val onDelete: (Product) -> Unit
) : RecyclerView.Adapter<ProductViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(
                R.layout.item_products,
                parent,
                false
            )
        return ProductViewHolder(view)
    }


    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {

        val product = products[position]


        holder.txtProductName.text = product.name

        holder.txtCategory.text = product.category

        holder.txtPrice.text = "$ %.0f".format(product.price)

        holder.txtStock.text = product.stock.toString()

        // holder.imgProduct.setImageResource(product.imageRes)


        holder.btnView.setOnClickListener {
            onView(product)
        }


        holder.btnEdit.setOnClickListener {
            onEdit(product)
        }


        holder.btnDelete.setOnClickListener {
            onDelete(product)
        }
    }


    override fun getItemCount(): Int {
        return products.size
    }
}