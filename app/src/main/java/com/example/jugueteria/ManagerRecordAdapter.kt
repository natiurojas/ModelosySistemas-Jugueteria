package com.example.jugueteria

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class ManagerRecord(val title: String, val subtitle: String, val amount: String = "")

class ManagerRecordAdapter(private val rows: List<ManagerRecord>) : RecyclerView.Adapter<ManagerRecordAdapter.Holder>() {
    class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.txtRecordTitle)
        val subtitle: TextView = v.findViewById(R.id.txtRecordSubtitle)
        val amount: TextView = v.findViewById(R.id.txtRecordAmount)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_manager_record, parent, false))
    override fun onBindViewHolder(h: Holder, position: Int) {
        val row = rows[position]
        h.title.text = row.title
        h.subtitle.text = row.subtitle
        h.amount.text = row.amount
        h.amount.visibility = if (row.amount.isBlank()) View.GONE else View.VISIBLE
    }
    override fun getItemCount() = rows.size
}
