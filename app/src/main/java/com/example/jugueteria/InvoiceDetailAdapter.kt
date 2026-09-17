package com.example.jugueteria

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class InvoiceDetailAdapter(
    private val items: List<SaleItem>
) : RecyclerView.Adapter<InvoiceDetailAdapter.ViewHolder>() {


    class ViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        val productName: TextView =
            itemView.findViewById(
                R.id.txtProductName
            )

        val quantity: TextView =
            itemView.findViewById(
                R.id.txtQuantity
            )

        val unitPrice: TextView =
            itemView.findViewById(
                R.id.txtUnitPrice
            )

        val subtotal: TextView =
            itemView.findViewById(
                R.id.txtSubtotal
            )
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view = LayoutInflater
            .from(parent.context)
            .inflate(
                R.layout.item_invoice_detail,
                parent,
                false
            )

        return ViewHolder(view)
    }


    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val item = items[position]


        holder.productName.text =
            item.product.name


        holder.quantity.text =
            if (item.quantity == 1) {
                "1 unidad"
            } else {
                "${item.quantity} unidades"
            }


        holder.unitPrice.text =
            "$ %.0f".format(
                item.product.price
            )


        holder.subtotal.text =
            "$ %.0f".format(
                item.subtotal
            )
    }


    override fun getItemCount(): Int {
        return items.size
    }
}