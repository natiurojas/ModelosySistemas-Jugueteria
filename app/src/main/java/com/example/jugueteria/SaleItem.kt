package com.example.jugueteria

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class SaleItem(
    val product: Product,
    var quantity: Int
) {
    val subtotal: Double get() = product.price * quantity
}

class ProductSaleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val name: TextView = itemView.findViewById(R.id.txtProductName)
    val price: TextView = itemView.findViewById(R.id.txtProductPrice)
    val add: ImageButton = itemView.findViewById(R.id.btnAddProduct)
}

class SaleProductAdapter(
    private val products: List<Product>,
    private val onAddProduct: (Product) -> Unit
) : RecyclerView.Adapter<ProductSaleViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProductSaleViewHolder {

        val view = LayoutInflater
            .from(parent.context)
            .inflate(
                R.layout.item_sale_product,
                parent,
                false
            )

        return ProductSaleViewHolder(view)
    }


    override fun onBindViewHolder(holder: ProductSaleViewHolder, position: Int) {
        val product = products[position]
        holder.name.text = product.name
        holder.price.text = "$%.0f".format(product.price)
        holder.add.setOnClickListener {
            onAddProduct(product)
        }
    }


    override fun getItemCount(): Int {
        return products.size
    }
}

class DetailViewHolder(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {
    val productName: TextView = itemView.findViewById(R.id.txtDetailProduct)
    val quantityPrice: TextView = itemView.findViewById(R.id.txtQuantityPrice)
    val subtotal: TextView = itemView.findViewById(R.id.txtSubtotal)
    val remove: ImageButton = itemView.findViewById(R.id.btnRemove)
}

class SaleDetailAdapter(
    private val items: MutableList<SaleItem>,
    private val onRemove: (SaleItem) -> Unit
) : RecyclerView.Adapter<DetailViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DetailViewHolder {

        val view = LayoutInflater
            .from(parent.context)
            .inflate(
                R.layout.item_sale_detail,
                parent,
                false
            )

        return DetailViewHolder(view)
    }


    override fun onBindViewHolder(
        holder: DetailViewHolder,
        position: Int
    ) {

        val item = items[position]

        holder.productName.text =
            item.product.name

        holder.quantityPrice.text =
            "${item.quantity} x $ %.0f"
                .format(item.product.price)

        holder.subtotal.text =
            "$ %.0f".format(item.subtotal)


        holder.remove.setOnClickListener {
            onRemove(item)
        }
    }


    override fun getItemCount(): Int {
        return items.size
    }
}