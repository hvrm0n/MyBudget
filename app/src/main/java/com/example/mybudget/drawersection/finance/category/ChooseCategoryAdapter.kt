package com.example.mybudget.drawersection.finance.category

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mybudget.R
import com.example.mybudget.drawersection.finance.IconsChooserAlertDialog
import com.example.mybudget.start_pages.CategoryBegin
import com.example.mybudget.start_pages.Constants
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener


class ChooseCategoryAdapter(private val context: Context, private var categories: List<CategoryBegin>, val fragment: AddCategoryFragment, val table: DatabaseReference, val auth: FirebaseAuth, val button: MaterialButton):
    RecyclerView.Adapter<RecyclerView.ViewHolder>(){
    private val TYPE_CATEGORY = 2
    private val TYPE_ADD = 1
    private var lastClick: View? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            TYPE_CATEGORY->{
                val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_category, parent, false)
                CategoryViewHolder(view)
            }
            TYPE_ADD->{
                val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_category, parent, false)
                AddViewHolder(view)
            }
            else->throw IllegalArgumentException("Invalid view type")
        }
    }

    fun getChose() = lastClick

    fun updateData(newCategory: List<CategoryBegin>) {
        categories = newCategory
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return categories.size+1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < categories.size) {
            TYPE_CATEGORY
        } else {
            TYPE_ADD
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is CategoryViewHolder){
            holder.bind(categories[position], position)
        }
    }

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val textViewName: TextView = itemView.findViewById(R.id.nameCategory)
        private val icon: ImageView = itemView.findViewById(R.id.iconCategory)

        fun bind(category: CategoryBegin, position: Int) {
            textViewName.text = category.name
            icon.setImageDrawable(ContextCompat.getDrawable(context, context.resources.getIdentifier(category.path, "drawable", context.packageName)))

            itemView.setOnClickListener {
                button.isEnabled = true
                lastClick?.backgroundTintList = ColorStateList.valueOf( ContextCompat.getColor(context, R.color.very_light_green))
                it.backgroundTintList = ColorStateList.valueOf( ContextCompat.getColor(context, R.color.light_green))
                lastClick = it
                fragment.changeSelectedIcon(category.path)
            }
        }
    }

    inner class AddViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {


        init {
            itemView.setOnClickListener {
                showAddDialog()
            }
        }


        private fun showAddDialog() {

            val dialogView = View.inflate(itemView.context, R.layout.card_new_category, null)
            val builder = AlertDialog.Builder(itemView.context)
            builder.setView(dialogView)

            val newCategoryName = dialogView.findViewById<EditText>(R.id.categoryNewValue)
            val iconNew = dialogView.findViewById<ImageView>(R.id.imageOfCategory)

            iconNew.setOnClickListener {
                IconsChooserAlertDialog(context){ path->
                    iconNew.setImageDrawable(ContextCompat.getDrawable(context, context.resources.getIdentifier(path, "drawable", context.packageName)))
                    iconNew.tag = path
                }
            }

            builder.setPositiveButton("Добавить") { dialog, _ ->
                if(newCategoryName.text.isNotEmpty()&&iconNew.tag!=null){
                    table.child("Users").child(auth.currentUser!!.uid).child("Categories").child("Categories base").child(newCategoryName.text.toString()).addListenerForSingleValueEvent(object :
                        ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()){
                                Toast.makeText(context, "Такая категория уже существует!", Toast.LENGTH_LONG).show()
                            } else {
                                table.child("Users").child(auth.currentUser!!.uid).child("Categories")
                                    .child("Categories base").child(newCategoryName.text.toString()).setValue(CategoryBegin(newCategoryName.text.toString(), iconNew.tag.toString()))
                                dialog.dismiss()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e(Constants.TAG_USER, error.message)
                        }

                    })
                } else when{
                    newCategoryName.text.isEmpty() -> Toast.makeText(context, "Вы не ввели название категории", Toast.LENGTH_LONG).show()
                    iconNew.tag==null-> Toast.makeText(context, "Вы не выбрали изображение", Toast.LENGTH_LONG).show()
                }
            }

            builder.setNegativeButton("Отмена"){dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            dialog.window?.setBackgroundDrawableResource(R.drawable.listview_shadow)
            dialog.show()
        }
    }
}

