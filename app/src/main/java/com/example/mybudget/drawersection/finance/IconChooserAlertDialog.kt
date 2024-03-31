package com.example.mybudget.drawersection.finance

import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mybudget.R
import com.example.mybudget.drawersection.finance.category.InnerAdapter

class IconsChooserAlertDialog(context: Context,  onIconSelected: (String) -> Unit) : AlertDialog(context) {
    init{
        val dialogView = View.inflate(context, R.layout.card_icon_chooser, null)
        val builder = AlertDialog.Builder(context)
        builder.setView(dialogView)
        val iconsRecycler = dialogView.findViewById<RecyclerView>(R.id.recyclerIconChooser)

        val stringResourceMap = listOf(
            R.string.car to R.array.icons_car,
            R.string.family to R.array.icons_family,
            R.string.graduation to R.array.icons_graduation,
            R.string.gym to R.array.icons_gym,
            R.string.heart to R.array.icons_heart,
            R.string.home to R.array.icons_home,
            R.string.marker to R.array.icons_marker,
            R.string.nature to R.array.icons_nature,
            R.string.pet to R.array.icons_pet,
            R.string.restaurants to R.array.icons_restaurant,
            R.string.smile to R.array.icons_smile,
            R.string.work to R.array.icons_work,
            R.string.other to R.array.icons_other)

        val adapter = InnerAdapter(context, stringResourceMap)
        iconsRecycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        iconsRecycler.adapter = adapter

        builder.setPositiveButton("Выбрать") { dialog, _ ->
            if(adapter.getSelectedIcon()!=""){
                onIconSelected(adapter.getSelectedIcon())
                dialog.dismiss()
            } else Toast.makeText(context, "Вы не выбрали иконку.", Toast.LENGTH_LONG).show()
        }

        builder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(R.drawable.listview_shadow)
        dialog.show()
    }
}