package com.example.mybudget.drawersection.finance.budget

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.mybudget.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope

class BudgetAdapter(private val context: Context, private var budgets: List<BudgetItemWithKey>, val scope: CoroutineScope, val table: DatabaseReference, val auth: FirebaseAuth):
    RecyclerView.Adapter<RecyclerView.ViewHolder>(){
    private val TYPE_BUDGET = 2
    private val TYPE_ADD = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            TYPE_BUDGET->{
                val view = LayoutInflater.from(parent.context).inflate(R.layout.card_budget, parent, false)
                BudgetViewHolder(view)
            }
            TYPE_ADD->{
                val view = LayoutInflater.from(parent.context).inflate(R.layout.card_add_new, parent, false)
                AddViewHolder(view)
            }
            else->throw IllegalArgumentException("Invalid view type")
        }
    }

    fun updateData(newPurchases: List<BudgetItemWithKey>) {
        budgets = newPurchases
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return budgets.size+1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < budgets.size) {
            TYPE_BUDGET
        } else {
            TYPE_ADD
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is BudgetViewHolder){
            holder.bind(budgets[position], position)
        } else if( holder is AddViewHolder){
            holder.bind()
        }
    }

    inner class BudgetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: CardView = itemView.findViewById(R.id.cardBudget)
        private val textViewName: TextView = itemView.findViewById(R.id.budgetName)
        private val budgetRemainder: TextView = itemView.findViewById(R.id.budgetRemainder)
        private val budgetCount: TextView = itemView.findViewById(R.id.budgetCount)
        private val budgetType: TextView = itemView.findViewById(R.id.budgetType)

        fun bind(budgetItem: BudgetItemWithKey, position: Int) {
            textViewName.text = budgetItem.budgetItem.name
            budgetRemainder.text = context.resources.getString(R.string.remaind,budgetItem.budgetItem.amount, context.resources.getString(context.resources.getIdentifier(budgetItem.budgetItem.currency, "string", context.packageName)))
            budgetType.text = if(position==0) context.resources.getString(R.string.base, budgetItem.budgetItem.type) else budgetItem.budgetItem.type
            budgetCount.text = context.resources.getString(R.string.transaction, budgetItem.budgetItem.count)

            card.setOnClickListener {
                val bundle = Bundle()
                bundle.putString("key",budgetItem.key)
                bundle.putString("name",budgetItem.budgetItem.name)
                bundle.putString("amount",budgetItem.budgetItem.amount)
                bundle.putString("type",budgetItem.budgetItem.type)
                bundle.putString("transaction",budgetItem.budgetItem.count.toString())
                bundle.putString("symbol",context.resources.getString(context.resources.getIdentifier(budgetItem.budgetItem.currency, "string", context.packageName)))
                bundle.putString("currency",budgetItem.budgetItem.currency)
                bundle.putBoolean("base", position==0)
                itemView.findNavController().navigate(R.id.action_nav_finance_to_budgetEditDialogFragment, bundle)
            }
        }

    }

    inner class AddViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val addNew: CardView = itemView.findViewById(R.id.addNew)

        fun bind() {
            val layoutParams = addNew.layoutParams
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            addNew.layoutParams = layoutParams

            addNew.setOnClickListener {
                    table.child("Users").child(auth.currentUser?.uid.toString()).child("Budgets")
                        .child("Base budget").child("currency").get()
                        .addOnSuccessListener { dataSnapshot ->
                            if (dataSnapshot.exists()) {
                                val bundle = Bundle()
                                bundle.putString(
                                    "basicCurrency",
                                    context.resources.getString(
                                        context.resources.getIdentifier(
                                            dataSnapshot.value.toString(),
                                            "string",
                                            context.packageName
                                        )
                                    )
                                )

                                bundle.putString("currency", dataSnapshot.value.toString())
                                val gson = Gson()
                                val budgetJson = gson.toJson(budgets)
                                bundle.putString("budgets", budgetJson)
                                itemView.findNavController().navigate(R.id.action_nav_finance_to_budgetEditDialogFragment, bundle)
                            }
                        }

            }
        }
    }
}