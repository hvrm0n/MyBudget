package com.example.mybudget.drawersection.finance.category

import android.content.Context
import android.content.res.ColorStateList
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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.mybudget.R
import com.example.mybudget.drawersection.finance.DateViewPagerAdapter
import com.example.mybudget.drawersection.finance.FinanceViewModel
import com.example.mybudget.drawersection.finance.HistoryItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import java.util.Calendar


class CategoryAdapter(private val context: Context, private var categories: List<CategoryItemWithKey>, val scope: CoroutineScope, val table: DatabaseReference, val auth: FirebaseAuth, val activity: ViewModelStoreOwner,
    vpAdapter: DateViewPagerAdapter):
    RecyclerView.Adapter<RecyclerView.ViewHolder>(){
    private val TYPE_CATEGORY = 2
    private val TYPE_ADD = 1
    private var baseCurrency: String? = null
    private var currentDate: Int = 2
    private var newDate: Int = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            TYPE_CATEGORY->{
                val view = LayoutInflater.from(parent.context).inflate(R.layout.card_expence, parent, false)
                CategoryViewHolder(view)
            }
            else->{
                val view = LayoutInflater.from(parent.context).inflate(R.layout.card_add_new, parent, false)
                AddViewHolder(view)
            }
        }
    }

    fun getCurrentDate() = currentDate

    fun updateData(newCategory: List<CategoryItemWithKey>) {
        categories = newCategory
        Log.e("CategoryDataUpdate", newCategory.toString())
        notifyDataSetChanged()
    }

    fun updateCurrentDate(newDate:Int){
        currentDate = newDate
    }

    fun updateNewDate(new:Int){
        newDate = new

    }

    fun updateCurrency(newCurrency: String) {
        baseCurrency = newCurrency
        notifyDataSetChanged()
    }

    fun deleteItemAtPosition(position:Int, data:Pair<Int, Int>){
        table.child("Users").child(auth.currentUser!!.uid).child("Plan")
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach { years ->
                        years.children.forEach { months->
                            months.children.forEach {snap->
                                snap.getValue(HistoryItem::class.java)?.let {
                                    if (it.placeId == categories[position].key && months.key!!.toInt() >= data.first && years.key!!.toInt() >= data.second){
                                        table.child("Users").child(auth.currentUser!!.uid).child("Plan")
                                            .child("${years.key}/${months.key}").child(it.key).removeValue()

                                        table.child("Users").child(auth.currentUser!!.uid).child("Categories")
                                            .child("${years.key}/${months.key}")
                                            .child("ExpenseCategories").child(categories[position].key).removeValue()
                                    }
                                }
                            }
                        }
                    }
                    table.child("Users").child(auth.currentUser!!.uid).child("Categories")
                        .child("${data.second}/${data.first}")
                        .child("ExpenseCategories").child(categories[position].key).removeValue()
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }

    override fun getItemCount(): Int {
        return categories.size+1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < categories.size) {
            TYPE_CATEGORY
        } else {
            if (position == categories.size) {
                TYPE_ADD
            } else {
                -1
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is CategoryViewHolder){
            holder.bind(categories[position], position)
        } else if (holder is AddViewHolder){
            holder.create()
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


        fun bind(categoryItem: CategoryItemWithKey, position: Int) {
            val financeViewModel = ViewModelProvider(activity)[FinanceViewModel::class.java]
            baseCurrency = financeViewModel.budgetLiveData.value?.get(0)?.budgetItem?.currency
            textViewName.text = financeViewModel.categoryBeginLiveData.value!!.filter {  it.key == categoryItem.key}[0].categoryBegin.name
            priority.text = context.resources.getString(
                R.string.priority, when (categoryItem.categoryItem.priority) {
                    0 -> "Низкий"
                    1 -> "Средний"
                    else -> "Высокий"
                }
            )
            icon.setImageDrawable(ContextCompat.getDrawable(context, context.resources.getIdentifier(financeViewModel.categoryBeginLiveData.value!!.filter {  it.key == categoryItem.key}[0].categoryBegin.path, "drawable", context.packageName)))
            currency.text = context.resources.getString(context.resources.getIdentifier(baseCurrency, "string", context.packageName))
            if (categoryItem.categoryItem.remainder=="0"){
                categoryRemainder.visibility = View.GONE
                progress.visibility = View.GONE
                categoryTotal.visibility = View.VISIBLE
                left.visibility = View.GONE
            } else {
                categoryRemainder.visibility = View.VISIBLE
                progress.visibility = View.VISIBLE
                left.visibility=View.VISIBLE
                if(categoryItem.categoryItem.remainder.toDouble()<0.0){
                    progress.progressTintList = ColorStateList.valueOf(context.resources.getColor(R.color.dark_orange, context.theme))
                    //categoryTotal.visibility = View.GONE
                    left.text = "перерасход из"
                    categoryRemainder.text = categoryItem.categoryItem.remainder.subSequence(1, categoryItem.categoryItem.remainder.length)
                } else {
                    progress.progressTintList = ColorStateList.valueOf(
                        context.resources.getColor(
                            R.color.dark_green,
                            context.theme
                        )
                    )
                    left.text = context.resources.getString(R.string.card_expence_rem)
                    categoryTotal.visibility = View.VISIBLE
                    categoryRemainder.text = categoryItem.categoryItem.remainder
                }
                progress.progress = ((categoryItem.categoryItem.total.toDouble()-categoryItem.categoryItem.remainder.toDouble())*100.0/(categoryItem.categoryItem.total.toDouble())).toInt()
            }

            if(categoryItem.categoryItem.isPlanned){
                categoryTotal.visibility = View.VISIBLE
                currency.text ="В планах потратить "  + currency.text.toString()
            }
            categoryTotal.text = categoryItem.categoryItem.total
        }
    }

    inner class AddViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val addNew: CardView = itemView.findViewById(R.id.addNew)

        fun create() {
            if(newDate < currentDate){
                addNew.visibility = View.GONE
            } else {
                addNew.visibility = View.VISIBLE
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
                                itemView.findNavController().navigate(R.id.action_nav_finance_to_addCategoryFragment)
                            }
                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
            }
        }
    }
}