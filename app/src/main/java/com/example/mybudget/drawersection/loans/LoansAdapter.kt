package com.example.mybudget.drawersection.loans

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
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
import com.example.mybudget.BudgetNotificationManager
import com.example.mybudget.R
import com.example.mybudget.drawersection.finance.FinanceViewModel
import com.example.mybudget.drawersection.finance.HistoryItem
import com.example.mybudget.drawersection.finance.budget.BudgetItemWithKey
import com.example.mybudget.start_pages.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LoansAdapter(private val context: Context, private var loans: List<LoanItemWithKey>, val table: DatabaseReference, val auth: FirebaseAuth, val financeViewModel: FinanceViewModel, private val parentFragment: Fragment):
    RecyclerView.Adapter<RecyclerView.ViewHolder>(){
    private val TYPE_LOAN = 2
    private val TYPE_ADD = 1
    private var finished = -1
    private var active = -1
    private var notReached = -1
    private lateinit var adapterBudget: ArrayAdapter<String>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            TYPE_LOAN->{
                val view = LayoutInflater.from(parent.context).inflate(R.layout.card_loan, parent, false)
                GoalViewHolder(view)
            }
            TYPE_ADD->{
                val view = LayoutInflater.from(parent.context).inflate(R.layout.card_add_new, parent, false)
                AddViewHolder(view)
            }
            else->throw IllegalArgumentException("Invalid view type")
        }
    }

    fun updateData(newLoan: List<LoanItemWithKey>) {
        loans = newLoan
        finished = -1
        active = -1
        notReached = -1
        notifyDataSetChanged()
    }

    fun deleteItemAtPosition(position: Int){
        BudgetNotificationManager.cancelAlarmManager(context, loans[position].key)
        table.child("Users")
            .child(auth.currentUser!!.uid)
            .child("Loans")
            .child(loans[position].key)
            .child("deleted")
            .setValue(true)
    }

    fun editItemAtPosition(position: Int){
        val bundle = Bundle()
        bundle.putString("key", loans[position].key)
        bundle.putString("type", "loan")
        parentFragment.findNavController().navigate(R.id.action_nav_loans_to_newGLSFragment, bundle)
    }

    override fun getItemCount(): Int {
        return loans.size+1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < loans.size) {
            TYPE_LOAN
        } else {
            TYPE_ADD
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is GoalViewHolder){
            holder.bind(loans[position], position)
        } else if( holder is AddViewHolder){
            holder.bind()
        }
    }

    inner class GoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val loanImage: ImageView = itemView.findViewById(R.id.loanImage)
        private val loanAmount: TextView = itemView.findViewById(R.id.loanAmount)
        private val loanName: TextView = itemView.findViewById(R.id.loanName)
        private val loanDate: TextView = itemView.findViewById(R.id.loanDate)
        private val loanDateEnd: TextView = itemView.findViewById(R.id.loanDateEnd)
        private val loanCurrency: TextView = itemView.findViewById(R.id.loanCurrency)
        private val loanFinished: TextView = itemView.findViewById(R.id.loanFinished)
        private val card: CardView = itemView.findViewById(R.id.cardLoan)

        fun bind(loanItem: LoanItemWithKey, position: Int) {
            loanName.text = loanItem.loanItem.name
            loanDateEnd.text = context.resources.getString(R.string.dateLoanEnd, loanItem.loanItem.dateOfEnd)
            when (loanItem.loanItem.period!=null){
                true-> {
                    loanDate.text = context.resources.getString(R.string.dateLoanCurrent, loanItem.loanItem.dateNext)
                    loanDate.visibility = View.VISIBLE
                }
                else -> {
                    loanDate.visibility = View.GONE
                }
            }
            loanAmount.text = loanItem.loanItem.amount
            loanCurrency.text = context.resources.getString(context.resources.getIdentifier( loanItem.loanItem.currency, "string", context.packageName))
            loanImage.setImageDrawable(ContextCompat.getDrawable(context, context.resources.getIdentifier(loanItem.loanItem.path, "drawable", context.packageName)))
            val calendar = Calendar.getInstance()
            when(loanItem.loanItem.period) {
                null -> calendar.set(
                    loanItem.loanItem.dateOfEnd.split(".")[2].toInt(),
                    loanItem.loanItem.dateOfEnd.split(".")[1].toInt() - 1,
                    loanItem.loanItem.dateOfEnd.split(".")[0].toInt() + 1,
                    0,
                    0,
                    0
                )

                else -> calendar.set(
                    loanItem.loanItem.dateNext!!.split(".")[2].toInt(),
                    loanItem.loanItem.dateNext!!.split(".")[1].toInt() - 1,
                    loanItem.loanItem.dateNext!!.split(".")[0].toInt() + 1,
                    0,
                    0,
                    0
                )
            }
            when(loanItem.loanItem.isFinished){
                true->{
                    if (finished == -1 || finished == position) {
                        loanFinished.text = context.resources.getString(R.string.loanFinished)
                        loanFinished.visibility = View.VISIBLE
                        finished = position
                    } else loanFinished.visibility = View.GONE
                    loanDate.visibility = View.GONE
                }

                false->{

                    if (Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY,0)
                            set(Calendar.MINUTE,0)
                            set(Calendar.SECOND,0)
                        }.timeInMillis > calendar.timeInMillis && !loanItem.loanItem.isDeleted) {
                        if (notReached==-1 || notReached == position){
                            loanFinished.text = "Просроченные"
                            loanFinished.visibility = View.VISIBLE
                            notReached = position
                        } else {
                            loanFinished.visibility = View.GONE
                        }
                    }
                    else if (active == -1 || active == position) {
                        loanFinished.text = context.resources.getString(R.string.activeSubs)
                        loanFinished.visibility = View.VISIBLE
                        active = position
                    }

                    else{
                        loanFinished.visibility = View.GONE
                    }

                    card.setOnClickListener {
                        openNewTransactionDialog(financeViewModel.budgetLiveData.value?.filter { !it.budgetItem.isDeleted }?: emptyList(), loanItem)
                    }
                }
            }
        }

        private fun openNewTransactionDialog(budgetList: List<BudgetItemWithKey>, loanItem: LoanItemWithKey){
            val dialogView = View.inflate(itemView.context, R.layout.card_goals_expence_income, null)
            val builder = AlertDialog.Builder(itemView.context)
            builder.setView(dialogView)

            val goalsNewValue = dialogView.findViewById<EditText>(R.id.goalsNewValue)
            val spinnerBudgetGoals = dialogView.findViewById<Spinner>(R.id.spinnerBudgetGoals)
            val currencyBudgetGoals = dialogView.findViewById<TextView>(R.id.currencyBudgetGoals)
            val currencyGoals = dialogView.findViewById<TextView>(R.id.currencyGoals)
            val equal = dialogView.findViewById<TextView>(R.id.equalGoals)
            val goalsBudgetValue = dialogView.findViewById<EditText>(R.id.goalsBudgetValue)

            goalsNewValue.isEnabled = false
            goalsBudgetValue.isEnabled = false

            goalsNewValue.setText(loanItem.loanItem.amount)
            currencyGoals.text = context.resources.getString(context.resources.getIdentifier(loanItem.loanItem.currency, "string", context.packageName))

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
                    budgetList.find { it.budgetItem.name == spinnerBudgetGoals.selectedItem.toString() }!!.budgetItem.currency.let {
                        if (it == loanItem.loanItem.currency){
                            goalsBudgetValue.visibility = View.GONE
                            equal.visibility = View.GONE
                            currencyBudgetGoals.visibility = View.GONE
                        } else{
                            goalsBudgetValue.visibility = View.VISIBLE
                            equal.visibility = View.VISIBLE
                            currencyBudgetGoals.visibility = View.VISIBLE
                            currencyBudgetGoals.text = context.resources.getString(context.resources.getIdentifier(it, "string", context.packageName))
                            goalsBudgetValue.setText(changeCurrencyAmount(
                                oldCurrency = loanItem.loanItem.currency,
                                newCurrency = it,
                                newAmount = loanItem.loanItem.amount,
                                context = context
                            ))
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    equal.visibility = View.GONE
                    goalsBudgetValue.visibility = View.GONE
                    currencyBudgetGoals.visibility = View.GONE
                }
            }

            builder.setPositiveButton("Выплатить") { dialog, _ ->
                if (spinnerBudgetGoals.selectedItemPosition == -1)  Toast.makeText(context, "Вы не выбрали счет списания", Toast.LENGTH_SHORT).show()
                else{
                    if(budgetList[spinnerBudgetGoals.selectedItemPosition].budgetItem.amount.toDouble()>=0
                        && (budgetList[spinnerBudgetGoals.selectedItemPosition].budgetItem.amount.toDouble()
                                - if(goalsBudgetValue.visibility == View.VISIBLE) goalsBudgetValue.text.toString().toDouble() else goalsNewValue.text.toString().toDouble()) <0){

                        AlertDialog.Builder(context)
                            .setTitle("Перерасход")
                            .setMessage("После совершения данной операции Вы уйдете в минус!\nПродолжить?")
                            .setPositiveButton("Да") { dialog2, _ ->
                                income(budgetList[spinnerBudgetGoals.selectedItemPosition], loanItem, goalsNewValue.text.toString().toDouble(),  if(goalsBudgetValue.visibility == View.VISIBLE) goalsBudgetValue.text.toString().toDouble() else goalsNewValue.text.toString().toDouble())
                                dialog2.dismiss()
                            }
                            .setNegativeButton("Нет") { dialog2, _ ->
                                dialog2.dismiss()
                            }.show()
                    }
                    else income(budgetList[spinnerBudgetGoals.selectedItemPosition], loanItem, goalsNewValue.text.toString().toDouble(),  if(goalsBudgetValue.visibility == View.VISIBLE) goalsBudgetValue.text.toString().toDouble() else goalsNewValue.text.toString().toDouble())
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

        private fun income(budgetItem:BudgetItemWithKey, loanItem: LoanItemWithKey, loanValue:Double, budgetValue:Double){

            if(loanItem.loanItem.period == null){
                loanItem.loanItem.isFinished = true
            } else {
                val date = loanItem.loanItem.dateNext?.let {
                    SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(it) }
                val calendar = Calendar.getInstance().apply {
                    time = date!!
                }
                calendar.add(when(loanItem.loanItem.period!!.split(" ")[1]){
                    "d"->Calendar.DAY_OF_MONTH
                    "w"->Calendar.WEEK_OF_MONTH
                    "m"->Calendar.MONTH
                    else->Calendar.YEAR
                }, loanItem.loanItem.period!!.split(" ")[0].toInt())

                val dateEnd = loanItem.loanItem.dateOfEnd.let {
                    SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(it) }
                val calendarEnd = Calendar.getInstance().apply {
                    time = dateEnd!!
                }

                val sharedPreferences = context.getSharedPreferences("NotificationPeriodAndTime", Context.MODE_PRIVATE)
                val periodBegin = sharedPreferences.getString(loanItem.key, "|")?.split("|")?.get(0)?:context.resources.getStringArray(R.array.periodicity)[0]
                val timeBegin = sharedPreferences.getString(loanItem.key, "|")?.split("|")?.get(1)?:"12:00"


                BudgetNotificationManager.cancelAlarmManager(context, loanItem.key)
                if (calendar.timeInMillis <= calendarEnd.timeInMillis){
                    if( loanItem.loanItem.period!=null){
                        BudgetNotificationManager.notification(
                            context = context,
                            channelID = Constants.CHANNEL_ID_LOAN,
                            id = loanItem.key,
                            placeId = loanItem.key,
                            time = timeBegin,
                            dateOfExpence = calendar,
                            periodOfNotification = periodBegin
                        )
                    }
                } else loanItem.loanItem.isFinished = true

                loanItem.loanItem.dateNext = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(calendar.time)
            }

            budgetItem.budgetItem.amount = "%.2f".format( budgetItem.budgetItem.amount.toDouble() - budgetValue).replace(",", ".")
            budgetItem.budgetItem.count ++

            table.child("Users").child(auth.currentUser!!.uid).child("Loans").child(loanItem.key).setValue(loanItem.loanItem)
            when(budgetItem.key){
                "Base budget" -> table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child(budgetItem.key).setValue(budgetItem.budgetItem)
                else -> table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Other budget").child(budgetItem.key).setValue(budgetItem.budgetItem)
            }
            val historyReference = table.child("Users").child(auth.currentUser!!.uid).child("History").child(
                "${Calendar.getInstance().get(Calendar.YEAR)}/${
                    Calendar.getInstance().get(Calendar.MONTH)+1}"
            ).push()
            historyReference.setValue(
                HistoryItem(
                budgetId = budgetItem.key,
                placeId = loanItem.key,
                isLoan = true,
                amount = "-%.2f".format(budgetValue).replace(",", "."),
                date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Calendar.getInstance().time),
                baseAmount = "-%.2f".format(loanValue).replace(",", "."),
                key = historyReference.key.toString())
            )
        }

        private fun changeCurrencyAmount(oldCurrency: String, newCurrency: String,newAmount:String, context: Context):String{
            val currencyConvertor = ExchangeRateManager.getExchangeRateResponse(context)
            if(currencyConvertor!=null && oldCurrency!=newCurrency){
                return when (oldCurrency){
                    currencyConvertor.baseCode->{
                        "%.2f".format(newAmount.toDouble()* currencyConvertor.conversionRates[newCurrency]!!).replace(',','.')
                    }
                    else->{
                        "%.2f".format( newAmount.toDouble()* currencyConvertor.conversionRates[newCurrency]!!/ currencyConvertor.conversionRates[oldCurrency]!!).replace(',','.')
                    }
                }
            }
            return newAmount
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
                bundle.putString("type", "loan")
                itemView.findNavController().navigate(R.id.action_nav_loans_to_newGLSFragment, bundle)
            }
        }
    }
}