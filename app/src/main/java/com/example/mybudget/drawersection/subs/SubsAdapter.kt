package com.example.mybudget.drawersection.subs

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.mybudget.R
import com.example.mybudget.drawersection.finance.FinanceViewModel
import com.example.mybudget.drawersection.finance.HistoryItem
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SubsAdapter(private val context: Context, private var subs: List<SubItemWithKey>, val table: DatabaseReference, val auth: FirebaseAuth, val financeViewModel: FinanceViewModel, private val parentFragment:Fragment):
    RecyclerView.Adapter<RecyclerView.ViewHolder>(){
    private val TYPE_GOAL = 2
    private val TYPE_ADD = 1
    private var cancelled = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            TYPE_GOAL->{
                val view = LayoutInflater.from(parent.context).inflate(R.layout.card_subs, parent, false)
                GoalViewHolder(view)
            }
            TYPE_ADD->{
                val view = LayoutInflater.from(parent.context).inflate(R.layout.card_add_new, parent, false)
                AddViewHolder(view)
            }
            else->throw IllegalArgumentException("Invalid view type")
        }
    }

    fun updateData(newSub: List<SubItemWithKey>) {
        subs = newSub
        cancelled = false
        notifyDataSetChanged()
    }

    fun deleteItemAtPosition(position: Int){
        table.child("Users")
            .child(auth.currentUser!!.uid)
            .child("Subs")
            .child(subs[position].key)
            .child("deleted")
            .setValue(true)
    }

    fun editItemAtPosition(position: Int){
        val bundle = Bundle()
        bundle.putString("key", subs[position].key)
        bundle.putString("type", "sub")
        parentFragment.findNavController().navigate(R.id.action_nav_goals_to_newGLSFragment, bundle)
    }

    override fun getItemCount(): Int {
        return subs.size+1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < subs.size) {
            TYPE_GOAL
        } else {
            TYPE_ADD
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is GoalViewHolder){
            holder.bind(subs[position], position)
        } else if( holder is AddViewHolder){
            holder.bind()
        }
    }

    inner class GoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageSub: ImageView = itemView.findViewById(R.id.subImage)
        private val subCost: TextView = itemView.findViewById(R.id.subCost)
        private val subName: TextView = itemView.findViewById(R.id.subName)
        private val subBudget: TextView = itemView.findViewById(R.id.subBudget)
        private val subDate: TextView = itemView.findViewById(R.id.subDate)
        private val paidSub: MaterialButton = itemView.findViewById(R.id.paidSub)
        private val cancelSub: MaterialButton = itemView.findViewById(R.id.cancelSub)
        private val cancelledSub: TextView = itemView.findViewById(R.id.subCancelled)

        fun bind(subItem: SubItemWithKey, position: Int) {
            subName.text = subItem.subItem.name
            subBudget.text = context.resources.getString(R.string.nameBudgetSubs, financeViewModel.budgetLiveData.value?.find { it.key == subItem.subItem.budgetId }?.budgetItem?.name)
            subDate.text = subItem.subItem.date
            subCost.text = subItem.subItem.amount+ context.resources.getString(context.resources.getIdentifier( financeViewModel.budgetLiveData.value?.find { it.key == subItem.subItem.budgetId }?.budgetItem?.currency, "string", context.packageName))
            imageSub.setImageDrawable(ContextCompat.getDrawable(context, context.resources.getIdentifier(subItem.subItem.path, "drawable", context.packageName)))

            when(subItem.subItem.isCancelled){
                true->{
                    if (!cancelled) {
                        cancelledSub.visibility = View.VISIBLE
                        cancelled = true
                    } else cancelledSub.visibility = View.GONE
                    cancelSub.visibility = View.GONE
                    paidSub.text = context.resources.getString(R.string.reSubs)
                    paidSub.setOnClickListener {
                        table.child("Users")
                        .child(auth.currentUser!!.uid)
                        .child("Subs")
                        .child(subItem.key)
                        .child("cancelled")
                        .setValue(false)
                    }
                }

                false->{
                    cancelledSub.visibility = View.GONE
                    cancelSub.visibility = View.VISIBLE
                    paidSub.text = context.resources.getString(R.string.card_sub_paid)
                    cancelSub.setOnClickListener {
                        table.child("Users")
                            .child(auth.currentUser!!.uid)
                            .child("Subs")
                            .child(subItem.key)
                            .child("cancelled")
                            .setValue(true)
                    }

                    paidSub.setOnClickListener {
                        payOnce(subItem)
                    }
                }
            }
        }

        private fun payOnce(subItem: SubItemWithKey){
            financeViewModel.budgetLiveData.value?.find { it.key == subItem.subItem.budgetId }?.let { budgetItem->
                val budgetReference = when (subItem.subItem.budgetId){
                    "Base budget"->{
                        table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Base budget")
                    }
                    else->{
                        table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Other budget").child(subItem.subItem.budgetId)
                    }
                }

                budgetItem.budgetItem.amount =  "%.2f".format(budgetItem.budgetItem.amount.toDouble() - subItem.subItem.amount.toDouble()).replace(",", ".")
                budgetItem.budgetItem.count++

                budgetReference.setValue(budgetItem.budgetItem).addOnSuccessListener {
                    val date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(subItem.subItem.date)
                    val calendar = Calendar.getInstance().apply {
                        time = date!!
                        timeInMillis += subItem.subItem.period
                    }
                    subItem.subItem.date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(calendar.time)
                    table.child("Users").child(auth.currentUser!!.uid).child("Subs").child(subItem.key).setValue(subItem.subItem).addOnSuccessListener {
                        val historyItem = table.child("Users").child(auth.currentUser!!.uid).child("History")
                            .child("${Calendar.getInstance().get(Calendar.YEAR)}/${Calendar.getInstance().get(Calendar.MONTH)+1}").push()

                        historyItem.setValue(
                            HistoryItem(
                                budgetId = subItem.subItem.budgetId,
                                placeId = subItem.key,
                                isSub = true,
                                amount = "-${subItem.subItem.amount}",
                                date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Calendar.getInstance().time),
                                baseAmount = "-${subItem.subItem.amount}",
                                key = historyItem.key.toString()
                            )
                        )
                    }
                }
            }
        }
    }

    inner class AddViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val addNew: CardView = itemView.findViewById(R.id.addNew)

        fun bind() {
            val layoutParams = addNew.layoutParams
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            addNew.layoutParams = layoutParams


            addNew.setOnClickListener {
                val bundle = Bundle()
                bundle.putString("type", "sub")
                itemView.findNavController().navigate(R.id.action_nav_subs_to_newGLSFragment, bundle)
            }
        }
    }
}