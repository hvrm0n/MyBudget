package com.example.mybudget.drawersection.finance.category

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.mybudget.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import java.util.Calendar


class CategoryAdapter(private val context: Context, private var categories: List<CategoryItem>, val scope: CoroutineScope, val table: DatabaseReference, val auth: FirebaseAuth):
    RecyclerView.Adapter<RecyclerView.ViewHolder>(){
    private val TYPE_CATEGORY = 2
    private val TYPE_ADD = 1
    private var baseCurrency = "RUB"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            TYPE_CATEGORY->{
                val view = LayoutInflater.from(parent.context).inflate(R.layout.card_expence, parent, false)
                CategoryViewHolder(view)
            }
            TYPE_ADD->{
                val view = LayoutInflater.from(parent.context).inflate(R.layout.card_add_new, parent, false)
                AddViewHolder(view)
            }
            else->throw IllegalArgumentException("Invalid view type")
        }
    }

    fun updateData(newCategory: List<CategoryItem>) {
        categories = newCategory
        notifyDataSetChanged()
    }

    fun updateCurrency(newCurrency: String) {
        baseCurrency = newCurrency
        notifyDataSetChanged()
    }

    fun deleteItemAtPosition(position:Int){
        table.child("Users").child(auth.currentUser!!.uid).child("Categories")
            .child("${Calendar.getInstance().get(Calendar.YEAR)}/${Calendar.getInstance().get(Calendar.MONTH)+1}")
            .child("ExpenseCategories").child(categories[position].name).removeValue()
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

        private val card: CardView = itemView.findViewById(R.id.cardExpense)
        private val textViewName: TextView = itemView.findViewById(R.id.categoryName)
        private val priority: TextView = itemView.findViewById(R.id.categoryPriority)
        private val currency: TextView = itemView.findViewById(R.id.expenceVal)
        private val categoryRemainder: TextView = itemView.findViewById(R.id.categoryRemainder)
        private val categoryTotal: TextView = itemView.findViewById(R.id.categoryTotal)
        private val progress: ProgressBar = itemView.findViewById(R.id.progressBarCategory)
        private val left:TextView = itemView.findViewById(R.id.left)
        private val icon: ImageView = itemView.findViewById(R.id.categoryImage)


        fun bind(categoryItem: CategoryItem, position: Int) {
            Log.e("checkbind", categoryItem.path)
            textViewName.text = categoryItem.name
            priority.text = context.resources.getString(
                R.string.priority, when (categoryItem.priority) {
                    0 -> "Низкий"
                    1 -> "Средний"
                    else -> "Высокий"
                }
            )
            icon.setImageDrawable(ContextCompat.getDrawable(context, context.resources.getIdentifier(categoryItem.path, "drawable", context.packageName)))
            currency.text = context.resources.getString(context.resources.getIdentifier(baseCurrency, "string", context.packageName))
            if (categoryItem.remainder=="0"){
                categoryRemainder.visibility = View.GONE
                progress.visibility = View.GONE
                left.visibility = View.GONE
            } else {
                categoryRemainder.visibility = View.VISIBLE
                progress.visibility = View.VISIBLE
                left.visibility=View.VISIBLE
                categoryRemainder.text = categoryItem.remainder
                progress.progress = ((categoryItem.remainder.toDouble()*100.0)/(categoryItem.total.toDouble())).toInt()
            }

            categoryTotal.text = categoryItem.total
        }
    }

    inner class AddViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val addNew: CardView = itemView.findViewById(R.id.addNew)

        init {
            val layoutParams = addNew.layoutParams
            layoutParams.width = MATCH_PARENT
            addNew.layoutParams = layoutParams
            addNew.setOnClickListener {
                val categoriesAlready = mutableListOf<String>()
                table.child("Users").child(auth.currentUser!!.uid).child("Categories")
                    .child("${Calendar.getInstance().get(Calendar.YEAR)}/${Calendar.getInstance().get(Calendar.MONTH)+1}").child("ExpenseCategories")
                    .addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (category in snapshot.children){
                                categoriesAlready.add(category.key.toString())
                            }
                            val bundle = Bundle()
                            val gson = Gson()
                            val categoryJson = gson.toJson(categoriesAlready)
                            bundle.putString("categories", categoryJson)
                            itemView.findNavController().navigate(R.id.action_nav_finance_to_addCategoryFragment, bundle)
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
            }
        }
    }
}