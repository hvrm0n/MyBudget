package com.example.mybudget.drawersection.goals

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.mybudget.ExchangeRateManager
import com.example.mybudget.R
import com.example.mybudget.drawersection.finance.FinanceViewModel
import com.example.mybudget.drawersection.finance.HistoryItem
import com.example.mybudget.drawersection.finance.budget.BudgetItemWithKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GoalsAdapter(private val context: Context, private var goals: List<GoalItemWithKey>, val table: DatabaseReference, val auth: FirebaseAuth, val financeViewModel: FinanceViewModel, private val parentFragment:Fragment):
    RecyclerView.Adapter<RecyclerView.ViewHolder>(){
    private val TYPE_GOAL = 2
    private val TYPE_ADD = 1
    private var placeNotReached = false
    private var placeActive = false
    private var placeReach = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            TYPE_GOAL->{
                val view = LayoutInflater.from(parent.context).inflate(R.layout.card_goal, parent, false)
                GoalViewHolder(view)
            }
            TYPE_ADD->{
                val view = LayoutInflater.from(parent.context).inflate(R.layout.card_add_new, parent, false)
                AddViewHolder(view)
            }
            else->throw IllegalArgumentException("Invalid view type")
        }
    }

    fun updateData(newGoal: List<GoalItemWithKey>) {
        goals = newGoal
        placeActive = false
        placeNotReached = false
        placeReach = false
        notifyDataSetChanged()
    }

    fun deleteItemAtPosition(position: Int){
        table.child("Users")
            .child(auth.currentUser!!.uid)
            .child("Goals")
            .child(goals[position].key)
            .child("deleted")
            .setValue(true)
    }

    fun editItemAtPosition(position: Int){
        val bundle = Bundle()
        bundle.putString("key", goals[position].key)
        bundle.putString("type", "goal")
        parentFragment.findNavController().navigate(R.id.action_nav_goals_to_newGLSFragment, bundle)
    }

    override fun getItemCount(): Int {
        return goals.size+1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < goals.size) {
            TYPE_GOAL
        } else {
            TYPE_ADD
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is GoalViewHolder){
            holder.bind(goals[position], position)
        } else if( holder is AddViewHolder){
            holder.bind()
        }
    }

    inner class GoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: CardView = itemView.findViewById(R.id.cardGoal)
        private val textViewName: TextView = itemView.findViewById(R.id.goalName)
        private val goalNow: TextView = itemView.findViewById(R.id.goalNow)
        private val goalTarget: TextView = itemView.findViewById(R.id.goalTarget)
        private val goalCurrency: TextView = itemView.findViewById(R.id.goalCurrency)
        private val goalDate: TextView = itemView.findViewById(R.id.goalDate)
        private val progressBarGoal: ProgressBar = itemView.findViewById(R.id.progressBarGoal)
        private val goalImage: ImageView = itemView.findViewById(R.id.goalImage)
        private val reachedGoals: TextView = itemView.findViewById(R.id.reachedGoals)
        private var textWatcher: TextWatcher? = null

        private lateinit var adapterBudget: ArrayAdapter<String>
        fun bind(goalItem: GoalItemWithKey, position: Int) {
            val currencySymbol = context.resources.getString(context.resources.getIdentifier(goalItem.goalItem.currency, "string", context.packageName))
            textViewName.text = goalItem.goalItem.name
            goalNow.text = goalItem.goalItem.current
            goalTarget.text = context.resources.getString(R.string.target, "%.2f".format(goalItem.goalItem.target.toDouble()).replace(",", "."))

            goalCurrency.text = currencySymbol
            when (goalItem.goalItem.date != null){
                true-> {
                    goalDate.visibility = View.VISIBLE
                    goalDate.text = goalItem.goalItem.date
                } else -> goalDate.visibility = View.GONE
            }

            if (position!=0 && !goals[position-1].goalItem.isReached && !placeReach && goalItem.goalItem.isReached||
                goalItem.goalItem.isReached && !placeReach){
                Log.e("checkenter", "yes")
                reachedGoals.visibility = View.VISIBLE
                reachedGoals.text = "Достигнутые"
                placeReach = true
            } else if(goalItem.goalItem.date!=null && !goalItem.goalItem.isReached && !placeNotReached){
                val date = goalItem.goalItem.date!!.split(".")
                val calendar = Calendar.getInstance()
                calendar.set(date[2].toInt(), date[1].toInt()-1, date[0].toInt())
                if (Calendar.getInstance().timeInMillis > calendar.timeInMillis){
                    reachedGoals.text = "Просроченные"
                    reachedGoals.visibility = View.VISIBLE
                    placeNotReached = true
                } else if (!placeActive){
                    reachedGoals.text = "Активные"
                    reachedGoals.visibility = View.VISIBLE
                    placeActive = true
                }
            } else if (!goalItem.goalItem.isReached && !placeActive){
                reachedGoals.text = "Активные"
                reachedGoals.visibility = View.VISIBLE
                placeActive = true
            }
            else {
                reachedGoals.visibility = View.GONE
            }

            progressBarGoal.progress = (goalItem.goalItem.current.toDouble()*100.0/ goalItem.goalItem.target.toDouble()).toInt()
            goalImage.setImageDrawable(ContextCompat.getDrawable(context, context.resources.getIdentifier(goalItem.goalItem.path, "drawable", context.packageName)))

            card.setOnClickListener {
                openNewTransactionDialog(financeViewModel.budgetLiveData.value?.filter { !it.budgetItem.isDeleted }?: emptyList(), goalItem)
            }
        }

        private fun openNewTransactionDialog(budgetList: List<BudgetItemWithKey>, goalItem: GoalItemWithKey){
            val dialogView = View.inflate(itemView.context, R.layout.card_goals_expence_income, null)
            val builder = AlertDialog.Builder(itemView.context)
            builder.setView(dialogView)

            val goalsNewValue = dialogView.findViewById<EditText>(R.id.goalsNewValue)
            val spinnerBudgetGoals = dialogView.findViewById<Spinner>(R.id.spinnerBudgetGoals)
            val currencyBudgetGoals = dialogView.findViewById<TextView>(R.id.currencyBudgetGoals)
            val currencyGoals = dialogView.findViewById<TextView>(R.id.currencyGoals)
            val equal = dialogView.findViewById<TextView>(R.id.equalGoals)
            val goalsBudgetValue = dialogView.findViewById<EditText>(R.id.goalsBudgetValue)
            currencyGoals.text = context.resources.getString(context.resources.getIdentifier(goalItem.goalItem.currency, "string", context.packageName))

            adapterBudget = ArrayAdapter(context, android.R.layout.simple_spinner_item, if(budgetList.isNotEmpty()) budgetList.map { it.budgetItem.name } else emptyList())
            adapterBudget.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spinnerBudgetGoals.adapter = adapterBudget
            spinnerBudgetGoals.onItemSelectedListener = object :
                AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    textWatcher?.let { watcher->
                        goalsBudgetValue.removeTextChangedListener(watcher)
                        goalsNewValue.removeTextChangedListener(watcher)
                        goalsNewValue.text.clear()
                        goalsBudgetValue.text.clear()
                    }

                    if (budgetList[position].budgetItem.currency == goalItem.goalItem.currency){
                        equal.visibility = View.GONE
                        goalsBudgetValue.visibility = View.GONE
                        currencyBudgetGoals.visibility = View.GONE
                    } else {
                        equal.visibility = View.VISIBLE
                        goalsBudgetValue.visibility = View.VISIBLE
                        currencyBudgetGoals.visibility = View.VISIBLE
                        currencyBudgetGoals.text = context.resources.getString(context.resources.getIdentifier(budgetList[position].budgetItem.currency, "string", context.packageName))
                        goalsNewValue.clearFocus()
                        goalsBudgetValue.clearFocus()
                        convertValue(goalsNewValue, goalsBudgetValue, budgetList[position].budgetItem.currency, goalItem.goalItem.currency)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    equal.visibility = View.GONE
                    goalsBudgetValue.visibility = View.GONE
                    currencyBudgetGoals.visibility = View.GONE
                }
            }

            builder.setPositiveButton("Начисление") { dialog, _ ->
                if (goalsNewValue.text.isEmpty()) Toast.makeText(context, "Вы не ввели сумму начисления", Toast.LENGTH_SHORT).show()
                else if (spinnerBudgetGoals.selectedItemPosition == -1)  Toast.makeText(context, "Вы не выбрали счет списания", Toast.LENGTH_SHORT).show()
                else{
                    if(budgetList[spinnerBudgetGoals.selectedItemPosition].budgetItem.amount.toDouble()>=0
                        && (budgetList[spinnerBudgetGoals.selectedItemPosition].budgetItem.amount.toDouble()
                                - if(goalsBudgetValue.visibility == View.VISIBLE) goalsBudgetValue.text.toString().toDouble() else goalsNewValue.text.toString().toDouble()) <0){

                        AlertDialog.Builder(context)
                            .setTitle("Перерасход")
                            .setMessage("После совершения данной операции Вы уйдете в минус!\nПродолжить?")
                            .setPositiveButton("Да") { dialog2, _ ->
                                income(budgetList[spinnerBudgetGoals.selectedItemPosition], goalItem, goalsNewValue.text.toString().toDouble(),  if(goalsBudgetValue.visibility == View.VISIBLE) goalsBudgetValue.text.toString().toDouble() else goalsNewValue.text.toString().toDouble())
                                dialog2.dismiss()
                            }
                            .setNegativeButton("Нет") { dialog2, _ ->
                                dialog2.dismiss()
                            }.show()
                    }
                    else income(budgetList[spinnerBudgetGoals.selectedItemPosition], goalItem, goalsNewValue.text.toString().toDouble(),  if(goalsBudgetValue.visibility == View.VISIBLE) goalsBudgetValue.text.toString().toDouble() else goalsNewValue.text.toString().toDouble())
                }
                dialog.dismiss()
            }

            builder.setNegativeButton("Списание"){dialog, _ ->
                when{
                    (goalsNewValue.text.isEmpty()) -> Toast.makeText(context,"Вы не ввели сумму списания", Toast.LENGTH_SHORT).show()
                    (spinnerBudgetGoals.selectedItemPosition == -1) -> Toast.makeText(context,"Вы не выбрали счет начисления", Toast.LENGTH_LONG).show()
                    (goalItem.goalItem.current.toDouble()<goalsNewValue.text.toString().toDouble()) -> Toast.makeText(context, "Вы не можете вернуть денег больше, чем отложили!", Toast.LENGTH_SHORT).show()
                    else -> expense(budgetList[spinnerBudgetGoals.selectedItemPosition], goalItem, goalsNewValue.text.toString().toDouble(),  if(goalsBudgetValue.visibility == View.VISIBLE) goalsBudgetValue.text.toString().toDouble() else goalsNewValue.text.toString().toDouble())
                }
                dialog.dismiss()
            }

            builder.setNeutralButton("Отмена"){dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            dialog.window?.setBackgroundDrawableResource(R.drawable.listview_shadow)
            dialog.show()
        }

        private fun income(budgetItem:BudgetItemWithKey, goalItem: GoalItemWithKey, goalValue:Double, budgetValue:Double){

            goalItem.goalItem.current = "%.2f".format(goalItem.goalItem.current.toDouble() + goalValue).replace(",", ".")
            if(goalItem.goalItem.current.toDouble()>=goalItem.goalItem.target.toDouble()){
                goalItem.goalItem.isReached = true
            }
            budgetItem.budgetItem.amount = "%.2f".format( budgetItem.budgetItem.amount.toDouble() - budgetValue).replace(",", ".")
            budgetItem.budgetItem.count ++

            table.child("Users").child(auth.currentUser!!.uid).child("Goals").child(goalItem.key).setValue(goalItem.goalItem)
            when(budgetItem.key){
                "Base budget" -> table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child(budgetItem.key).setValue(budgetItem.budgetItem)
                else -> table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Other budget").child(budgetItem.key).setValue(budgetItem.budgetItem)
            }
            val historyReference = table.child("Users").child(auth.currentUser!!.uid).child("History").child(
                "${Calendar.getInstance().get(Calendar.YEAR)}/${
                    Calendar.getInstance().get(Calendar.MONTH)+1}"
            ).push()
            historyReference.setValue(HistoryItem(
                budgetId = budgetItem.key,
                placeId = goalItem.key,
                isGoal = true,
                amount = "%.2f".format(budgetValue).replace(",", "."),
                date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Calendar.getInstance().time),
                baseAmount = "%.2f".format(goalValue).replace(",", "."),
                key = historyReference.key.toString()
            ))
        }

        private fun expense(budgetItem:BudgetItemWithKey, goalItem: GoalItemWithKey, goalValue:Double, budgetValue:Double){
            goalItem.goalItem.current = "%.2f".format(goalItem.goalItem.current.toDouble() - goalValue).replace(",", ".")
            budgetItem.budgetItem.amount = "%.2f".format( budgetItem.budgetItem.amount.toDouble() + budgetValue).replace(",", ".")
            if(goalItem.goalItem.current.toDouble()<goalItem.goalItem.target.toDouble()){
                goalItem.goalItem.isReached = false
            }
            budgetItem.budgetItem.count++

            table.child("Users")
                .child(auth.currentUser!!.uid)
                .child("Goals")
                .child(goalItem.key).setValue(goalItem.goalItem)

            when(budgetItem.key){
                "Base budget" -> table.child("Users")
                    .child(auth.currentUser!!.uid)
                    .child("Budgets")
                    .child(budgetItem.key)
                    .setValue(budgetItem.budgetItem)

                else -> table.child("Users")
                    .child(auth.currentUser!!.uid)
                    .child("Budgets")
                    .child("Other budget")
                    .child(budgetItem.key)
                    .setValue(budgetItem.budgetItem)
            }
            val historyReference = table.child("Users")
                .child(auth.currentUser!!.uid)
                .child("History").child("${Calendar.getInstance().get(Calendar.YEAR)}/${Calendar.getInstance().get(Calendar.MONTH)+1}")
                .push()

            historyReference.setValue(HistoryItem(
                budgetId = budgetItem.key,
                placeId = goalItem.key,
                isGoal = true,
                amount = "-%.2f".format(budgetValue).replace(",", "."),
                date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Calendar.getInstance().time),
                baseAmount = "-%.2f".format(goalValue).replace(",", "."),
                key = historyReference.key.toString()
            ))
        }

        //goalsNewValue - 2et
        //goalsBudgetValue - 1et
        //budgetItemCurrency - валюта бюджета
        //goalItemCurrency - валюта цели
        private fun convertValue(goalsNewValue: EditText, goalsBudgetValue: EditText, budgetItemCurrency: String, goalItemCurrency: String){
            val currencyConvertor = ExchangeRateManager.getExchangeRateResponse(context)
            Log.e("convertValue_enter", currencyConvertor.toString())
            if(currencyConvertor!=null){
                when (budgetItemCurrency){
                    currencyConvertor.baseCode->{
                        textWatcher = object : TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                            override fun afterTextChanged(s: Editable?) {
                                if (s != null) {

                                    if (s.isEmpty()){
                                        goalsNewValue.text.clear()
                                        goalsBudgetValue.text.clear()
                                    }
                                    else if (s === goalsBudgetValue.text && s.isNotEmpty()) {
                                        goalsNewValue.setText(String.format("%.2f",s.toString().toDouble()*currencyConvertor.conversionRates[goalItemCurrency]!!).replace(',','.'))
                                    } else if (s === goalsNewValue.text && s.isNotEmpty()) {
                                        goalsBudgetValue.setText(String.format("%.2f", s.toString().toDouble()/currencyConvertor.conversionRates[goalItemCurrency]!!).replace(',','.'))
                                    }
                                }
                            }
                        }
                        goalsBudgetValue.setOnFocusChangeListener { _, hasFocus ->
                            if (!hasFocus) {
                                goalsBudgetValue.removeTextChangedListener(textWatcher)
                            } else {
                                goalsBudgetValue.addTextChangedListener(textWatcher)
                            }
                        }

                        goalsNewValue.setOnFocusChangeListener { _, hasFocus ->
                            if (!hasFocus) {
                                goalsNewValue.removeTextChangedListener(textWatcher)
                            } else {
                                goalsNewValue.addTextChangedListener(textWatcher)
                            }
                        }
                    }

                    else->{
                        textWatcher = object : TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                            override fun afterTextChanged(s: Editable?) {
                                if (s != null) {
                                    if (s.isEmpty()){
                                        goalsNewValue.text.clear()
                                        goalsBudgetValue.text.clear()
                                    }
                                    else if (s === goalsBudgetValue.text && s.isNotEmpty()) {
                                        val newValueToBase = s.toString().toDouble()/currencyConvertor.conversionRates[budgetItemCurrency]!!
                                        goalsNewValue.setText(String.format("%.2f", newValueToBase*currencyConvertor.conversionRates[goalItemCurrency]!!).replace(',','.'))
                                    } else if (s === goalsNewValue.text && s.isNotEmpty()) {
                                        val newValueToBase = s.toString().toDouble()/currencyConvertor.conversionRates[goalItemCurrency]!!
                                        goalsBudgetValue.setText(String.format("%.2f", newValueToBase*currencyConvertor.conversionRates[budgetItemCurrency]!!).replace(',','.'))
                                    }
                                }
                            }
                        }

                        goalsBudgetValue.setOnFocusChangeListener { _, hasFocus ->
                            if (!hasFocus) {
                                goalsBudgetValue.removeTextChangedListener(textWatcher)
                            } else {
                                goalsBudgetValue.addTextChangedListener(textWatcher)
                            }
                        }

                        goalsNewValue.setOnFocusChangeListener { _, hasFocus ->
                            if (!hasFocus) {
                                goalsNewValue.removeTextChangedListener(textWatcher)
                            } else {
                                goalsNewValue.addTextChangedListener(textWatcher)
                            }
                        }
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
                bundle.putString("type", "goal")
                itemView.findNavController().navigate(R.id.action_nav_goals_to_newGLSFragment, bundle)
            }
        }
    }
}