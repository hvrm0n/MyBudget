package com.example.mybudget.drawersection.finance.category

import android.content.Context
import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mybudget.R

class InnerAdapter(private val context: Context, private val categories: List<Pair<Int, Int>>) : RecyclerView.Adapter<InnerAdapter.InnerViewHolder>() {
    private var selectedIcon = ""
    private var cardViewOld: LinearLayout? =null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InnerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_icon, parent, false)
        return InnerViewHolder(view)
    }

    override fun onBindViewHolder(holder: InnerViewHolder, position: Int) {
        holder.name.text = context.resources.getString(categories[position].first)
        holder.iconImageView.layoutManager = GridLayoutManager(context, 4)
        holder.iconImageView.adapter = ImageAdapter(
            context,
            context.resources.getStringArray(categories[position].second),
            this
        )
    }

    override fun getItemCount(): Int {
        return categories.size
    }

    fun changeSelectedIcon(newIcon:String, cardView: LinearLayout){
        Log.e("newIcon", newIcon)
        selectedIcon = newIcon
        cardViewOld?.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.very_light_green))
        cardViewOld = cardView
    }

    fun getSelectedIcon() = selectedIcon

    inner class InnerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconImageView: RecyclerView = itemView.findViewById(R.id.icons)
        val name: TextView = itemView.findViewById(R.id.nameOfIconCategory)
    }
}