package com.demo.hiifi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SettingsAdapter(private val items: List<SettingsItem>) :
    RecyclerView.Adapter<SettingsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.item_icon)
        val title: TextView = view.findViewById(R.id.item_title)
        val subtitle: TextView = view.findViewById(R.id.item_subtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_settings, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.icon.setImageResource(item.icon)
        holder.title.text = item.title
        holder.subtitle.text = item.subtitle
    }

    override fun getItemCount() = items.size
}
