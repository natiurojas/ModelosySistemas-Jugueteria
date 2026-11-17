package com.example.jugueteria

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InvoiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val invoiceNumber: TextView = itemView.findViewById(R.id.txtInvoiceNumber)

    val client: TextView = itemView.findViewById(R.id.txtClient)

    val date: TextView = itemView.findViewById(R.id.txtDate)

    val total: TextView = itemView.findViewById(R.id.txtTotal)

    val view: ImageButton = itemView.findViewById(R.id.btnView)
}

class InvoiceAdapter(
    private var sales: List<Sale>,
    private val onView: (Sale) -> Unit
) : RecyclerView.Adapter<InvoiceViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): InvoiceViewHolder {

        val view = LayoutInflater
            .from(parent.context)
            .inflate(
                R.layout.item_invoice,
                parent,
                false
            )

        return InvoiceViewHolder(view)
    }


    override fun onBindViewHolder(
        holder: InvoiceViewHolder,
        position: Int
    ) {

        val sale = sales[position]


        holder.invoiceNumber.text =
            "Factura #${sale.id}"


        holder.client.text =
            "Cliente: ${sale.clientName}"


        holder.date.text =
            if (sale.date > 0) SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(sale.date))
            else "Fecha no disponible"


        holder.total.text =
            "$ %.0f".format(sale.total)


        holder.view.setOnClickListener {
            onView(sale)
        }
    }


    override fun getItemCount(): Int {
        return sales.size
    }


    fun updateList(newSales: List<Sale>) {

        sales = newSales

        notifyDataSetChanged()
    }
}