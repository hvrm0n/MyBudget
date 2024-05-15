package com.example.mybudget.drawersection.finance.category

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mybudget.R

class ImageAdapter(private val context: Context, private val images: Array<String>, private val adapter: InnerAdapter) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_category, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.name.visibility = View.GONE
        holder.imageView.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                context.resources.getIdentifier(images[position], "drawable", context.packageName)
            )
        )

        holder.card.setOnClickListener {
            it.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.light_green_selection))
            adapter.changeSelectedIcon(images[position], holder.card)
        }
    }

    override fun getItemCount(): Int {
        return images.size
    }

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: LinearLayout = itemView.findViewById(R.id.iconCard)
        val imageView: ImageView = itemView.findViewById(R.id.iconCategory)
        val name: TextView = itemView.findViewById(R.id.nameCategory)
    }
}