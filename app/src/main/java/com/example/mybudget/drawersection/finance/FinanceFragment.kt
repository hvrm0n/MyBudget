package com.example.mybudget.drawersection.finance

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.mybudget.ExchangeRateManager
import com.example.mybudget.R
import com.example.mybudget.databinding.PageFinanceBinding
import com.example.mybudget.drawersection.finance.budget.BudgetAdapter
import com.example.mybudget.drawersection.finance.budget.BudgetItemWithKey
import com.example.mybudget.drawersection.finance.budget._BudgetItem
import com.example.mybudget.drawersection.finance.category.CategoryAdapter
import com.example.mybudget.drawersection.finance.category.CategoryBeginWithKey
import com.example.mybudget.drawersection.finance.category.CategoryItemWithKey
import com.example.mybudget.drawersection.finance.category.SwipeHelper
import com.example.mybudget.drawersection.finance.category._CategoryBegin
import com.example.mybudget.drawersection.finance.category._CategoryItem
import com.example.mybudget.drawersection.goals.GoalItem
import com.example.mybudget.drawersection.goals.GoalItemWithKey
import com.example.mybudget.drawersection.loans.LoanItem
import com.example.mybudget.drawersection.loans.LoanItemWithKey
import com.example.mybudget.drawersection.subs.SubItem
import com.example.mybudget.drawersection.subs.SubItemWithKey
import com.example.mybudget.start_pages.Constants
import com.google.android.material.navigation.NavigationView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Calendar
import kotlin.math.abs

class FinanceFragment : Fragment() {

    private lateinit var binding:PageFinanceBinding
    private lateinit var vpAdapter:DateViewPagerAdapter
    private lateinit var adapterBudget: BudgetAdapter
    private lateinit var adapterCategory: CategoryAdapter

    private lateinit var auth: FirebaseAuth
    private lateinit var table: DatabaseReference

    private lateinit var financeViewModel:FinanceViewModel
    private val baseBudget = mutableListOf<BudgetItemWithKey>()
    private val otherBudget = mutableListOf<BudgetItemWithKey>()
    private val category = mutableListOf<CategoryItemWithKey>()
    private val categoryBegin = mutableListOf<CategoryBeginWithKey>()
    private val historyList = mutableListOf<HistoryItem>()
    private val dateList = mutableListOf<Pair<Int, Int>>()
    private val categoryDateLive =  MutableLiveData<List<Pair<Int, Int>>>(mutableListOf(Pair(Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.YEAR))))
    private val planList = mutableListOf<HistoryItem>()
    private val goalList = mutableListOf<GoalItemWithKey>()
    private val subList = mutableListOf<SubItemWithKey>()
    private val loanList = mutableListOf<LoanItemWithKey>()

    private var isExpanded = false

    private val fromBottom:Animation by lazy {
        AnimationUtils.loadAnimation(context, R.anim.from_bottom_fab)
    }
    private val toBottom:Animation by lazy {
        AnimationUtils.loadAnimation(context, R.anim.to_bottom_fab)
    }
    private val fromBottomRotate:Animation by lazy {
        AnimationUtils.loadAnimation(context, R.anim.rotate_from)
    }
    private val toBottomRotate:Animation by lazy {
        AnimationUtils.loadAnimation(context, R.anim.rotate_to)
    }
    private val toBG:Animation by lazy {
        AnimationUtils.loadAnimation(context, R.anim.to_bg)
    }
    private val fromBG:Animation by lazy {
        AnimationUtils.loadAnimation(context, R.anim.from_bg)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.page_finance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = PageFinanceBinding.bind(view)

        vpAdapter = DateViewPagerAdapter(requireContext())
        binding.viewpager.adapter = vpAdapter

        val sharedPreferences = requireContext().getSharedPreferences("preference_distribute", Context.MODE_PRIVATE)

        when(sharedPreferences.getBoolean("isDistributed", false)){
            false -> binding.calculate.text = resources.getString(R.string.fab_calculate)
            else -> {
                val time = sharedPreferences.getString("isDistributedDay", "")
                if (time?.isNotEmpty() == true && time.split(".")[0].toInt() == Calendar.getInstance().get(Calendar.MONTH) && time.split(".")[1].toInt() == Calendar.getInstance().get(Calendar.YEAR) ) {
                    binding.calculate.text = resources.getString(R.string.fab_cancel_calculate)
                } else {
                    binding.calculate.text = resources.getString(R.string.fab_calculate)
                    val editor = sharedPreferences.edit()
                    editor.putBoolean("isDistributed", false)
                    editor.putString("isDistributedDay", "")
                    editor.apply()
                }
            }
        }

        binding.floatingActionButton.setOnClickListener {
            if (isExpanded){
                hideFabs {}
            } else showFabs()
            isExpanded = !isExpanded
        }

        binding.fabNewTransaction.setOnClickListener {
            findNavController().navigate(R.id.action_nav_finance_to_newTransactionFragment)
            isExpanded = false
        }

        binding.fabHistory.setOnClickListener {
            openHistory()
            isExpanded = false
        }

        binding.fabCalculate.setOnClickListener {
            hideFabs{
                isExpanded = false
                binding.viewpager.currentItem = adapterCategory.getCurrentDate()
                updateCategoryOnce(vpAdapter.getDate(binding.viewpager.currentItem).first, vpAdapter.getDate(binding.viewpager.currentItem).second){
                    when(sharedPreferences.getBoolean("isDistributed", false)){
                        false -> {
                            binding.viewpager.currentItem = adapterCategory.getCurrentDate()
                            updateCategoryOnce(vpAdapter.getDate(binding.viewpager.currentItem).first, vpAdapter.getDate(binding.viewpager.currentItem).second){}
                            distributeMoney()
                        }
                        else -> cancelDistribution()
                    }
                }
            }
        }
    }

    private fun hideFabs(callback: (Unit) -> Unit){
        binding.calculate.startAnimation(toBottom)
        binding.fabCalculate.startAnimation(toBottom)
        binding.transaction.startAnimation(toBottom)
        binding.history.startAnimation(toBottom)
        binding.fabNewTransaction.startAnimation(toBottom)
        binding.fabHistory.startAnimation(toBottom)
        binding.floatingActionButton.startAnimation(toBottomRotate)
        binding.linearLayoutFinance.startAnimation(toBG)

        binding.fabHistory.isEnabled = false
        binding.fabNewTransaction.isEnabled = false
        binding.fabCalculate.isEnabled = false
        callback(Unit)
    }

    private fun showFabs(){
        binding.fabHistory.isEnabled = true
        binding.fabNewTransaction.isEnabled = true
        binding.fabCalculate.isEnabled = true

        binding.calculate.startAnimation(fromBottom)
        binding.fabCalculate.startAnimation(fromBottom)
        binding.transaction.startAnimation(fromBottom)
        binding.history.startAnimation(fromBottom)
        binding.fabNewTransaction.startAnimation(fromBottom)
        binding.fabHistory.startAnimation(fromBottom)
        binding.floatingActionButton.startAnimation(fromBottomRotate)
        binding.linearLayoutFinance.startAnimation(fromBG)
    }


    override fun onStart() {
        super.onStart()
        auth = Firebase.auth
        table = Firebase.database.reference

        ExchangeRateManager.request(table, auth, requireContext(), lifecycleScope, requireView(), requireActivity(), false)

        activity?.let {financeViewModel = ViewModelProvider(it)[FinanceViewModel::class.java] }

        adapterCategory = CategoryAdapter(requireContext(), emptyList(), viewLifecycleOwner, table, auth, requireActivity())
        adapterBudget = BudgetAdapter(requireContext(), emptyList(), lifecycleScope, table, auth)
        binding.budgetsList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.budgetsList.adapter = adapterBudget
        financeViewModel.budgetLiveData.observe(viewLifecycleOwner){
            adapterBudget.updateData(it.filter {budgetItemWithKey ->  !budgetItemWithKey.budgetItem.isDeleted })
            activity?.let {activity->
                activity.findViewById<NavigationView>(R.id.nav_view).getHeaderView(0).apply {
                    findViewById<TextView>(R.id.currentBalance).text = it[0].budgetItem.amount +  requireContext().resources.getString(requireContext().resources.getIdentifier(it[0].budgetItem.currency, "string", requireContext().packageName))

                }
            }
        }

        beginCheckUpCategories()
        updateBeginCategory()
        updateBudget()

        binding.categoryList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.categoryList.adapter = adapterCategory

        updateCategory()

        val itemTouchHelper = ItemTouchHelper(object : SwipeHelper(binding.categoryList, adapterCategory, "category") {
            override fun instantiateUnderlayButton(position: Int): List<UnderlayButton> {
                if(position==adapterCategory.itemCount-1){return emptyList() }
                val deleteButton = deleteButton(position)
                val editButton = editButton(position)
                return listOf(deleteButton, editButton)
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.categoryList)

        financeViewModel.categoryLiveData.observe(viewLifecycleOwner){
            adapterCategory.updateData(it)
        }

        vpAdapter.setData(listOf(Pair(Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.YEAR))), Pair(Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.YEAR)), binding.viewpager, adapterCategory)
        updateDate()
        categoryDateLive.observe(viewLifecycleOwner){ list->
            if(list.isEmpty()){
                binding.leftNav.visibility = View.INVISIBLE
                binding.rightNav.visibility = View.INVISIBLE
            }

            else {
                val mutableList = mutableListOf<Pair<Int, Int>>()
                mutableList.addAll(list)
                if(list.size == 1) {
                    when{
                        list[0].first == Calendar.getInstance().get(Calendar.MONTH)+1
                                && list[0].second == Calendar.getInstance().get(Calendar.YEAR)->{
                                    binding.leftNav.visibility = View.INVISIBLE
                                    binding.rightNav.visibility = View.INVISIBLE
                                }
                        list[0].first <= Calendar.getInstance().get(Calendar.MONTH)+1
                                && list[0].second == Calendar.getInstance().get(Calendar.YEAR)->{
                            binding.rightNav.visibility = View.INVISIBLE
                            mutableList.add(Pair(Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.YEAR)))
                            categoryDateLive.value = mutableList
                        }
                        list[0].first >= Calendar.getInstance().get(Calendar.MONTH)+1
                                && list[0].second == Calendar.getInstance().get(Calendar.YEAR)->{
                            binding.leftNav.visibility = View.INVISIBLE
                            mutableList.add(Pair(Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.YEAR)))
                            categoryDateLive.value = mutableList
                        }
                        list[0].second <= Calendar.getInstance().get(Calendar.YEAR)->{
                            binding.rightNav.visibility = View.INVISIBLE
                            mutableList.add(Pair(Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.YEAR)))
                            categoryDateLive.value = mutableList
                        }
                    }
                }

                binding.leftNav.visibility = View.VISIBLE
                binding.rightNav.visibility = View.VISIBLE
                var position = Pair(0,0)
                vpAdapter.setData(list.asSequence().sortedBy { it.second }.sortedBy {it.first}
                    .map{
                        if(Calendar.getInstance().get(Calendar.MONTH)+1 == it.first &&
                            Calendar.getInstance().get(Calendar.YEAR) == it.second){
                            position = it
                        }
                        it
                    }.toSet().toList(), position, binding.viewpager, adapterCategory)
                financeViewModel.updateDate(vpAdapter.getDate(binding.viewpager.currentItem))
            }
            leftAndRightRows()
        }

        updateHistory()
        updatePlan()

        val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                leftAndRightRows()
                adapterCategory.updateNewDate(binding.viewpager.currentItem)
                financeViewModel.updateDate(vpAdapter.getDate(binding.viewpager.currentItem))
                updateCategoryOnce(vpAdapter.getDate(binding.viewpager.currentItem).first, vpAdapter.getDate(binding.viewpager.currentItem).second){}
            }
        }

        binding.viewpager.registerOnPageChangeCallback(onPageChangeCallback)
        binding.leftNav.setOnClickListener {
            binding.viewpager.currentItem -= 1
        }

        binding.rightNav.setOnClickListener {
            binding.viewpager.currentItem += 1
        }

        updateGoals()
        updateSubs()
        updateLoans()
    }

    private fun leftAndRightRows(){
        when (binding.viewpager.currentItem){
            0 -> {
                binding.leftNav.visibility = View.INVISIBLE
                if(vpAdapter.itemCount>1){
                    binding.rightNav.visibility = View.VISIBLE
                } else  binding.rightNav.visibility = View.INVISIBLE
            }
            vpAdapter.itemCount-1 -> {
                binding.rightNav.visibility = View.INVISIBLE
                if(vpAdapter.itemCount>1){
                    binding.leftNav.visibility = View.VISIBLE
                } else  binding.leftNav.visibility = View.INVISIBLE
            }
            else -> {
                binding.leftNav.visibility = View.VISIBLE
                binding.rightNav.visibility = View.VISIBLE
            }
        }
    }
    private fun deleteButton(position: Int) : SwipeHelper.UnderlayButton {
        return SwipeHelper.UnderlayButton(
            requireContext(),
            R.drawable.trash,
            R.color.dark_orange,
            object : SwipeHelper.UnderlayButtonClickListener {
                override fun onClick() {
                    AlertDialog.Builder(context)
                        .setTitle("Удаление категории")
                        .setMessage("Вы уверены, что хотите удалить категорию?")
                        .setPositiveButton("Подтвердить") { dialog, _ ->
                            adapterCategory.deleteItemAtPosition(position, vpAdapter.getDate(binding.viewpager.currentItem))
                            dialog.dismiss()
                        }
                        .setNegativeButton("Отмена") { dialog, _ ->
                            dialog.dismiss()
                        }.show()
                }
            })
    }

    private fun editButton(position: Int) : SwipeHelper.UnderlayButton {
        return SwipeHelper.UnderlayButton(
            requireContext(),
            R.drawable.pencil,
            R.color.dark_green,
            object : SwipeHelper.UnderlayButtonClickListener {
                override fun onClick() {
                    editCategory(position)
                }
            })
    }

    private fun updateDate(){
        dateList.clear()
        table.child("Users").child(auth.currentUser!!.uid).child("Categories").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {years->
                    years.children.forEach { months->
                        if(years.key!=null&&months.key!=null&&years.key.toString().isDigitsOnly()){
                            dateList.add(Pair(months.key.toString().toInt(), years.key.toString().toInt()))
                        }
                    }
                }
                val current = Pair(Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.YEAR))
                if(dateList.isEmpty() || !dateList.contains(current)){
                    dateList.add(Pair(Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.YEAR)))
                }
                categoryDateLive.value = dateList
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun editCategory(position: Int){
        val dialogView = View.inflate(requireContext(), R.layout.card_new_category, null)
        val builder = AlertDialog.Builder(requireContext())
        builder.setView(dialogView)

        val etName: EditText = dialogView.findViewById(R.id.categoryNewValue)
        val tvPriority:TextView = dialogView.findViewById(R.id.textChooseIcon)
        val spinnerType: Spinner = dialogView.findViewById(R.id.spinnerProrityEdit)
        val imageChoose: ImageView = dialogView.findViewById(R.id.imageOfCategory)
        tvPriority.visibility = View.VISIBLE
        spinnerType.visibility = View.VISIBLE

        val category = financeViewModel.categoryLiveData.value?.get(position)
        val adapterPriority = ArrayAdapter.createFromResource(requireContext(), R.array.category_priority, android.R.layout.simple_spinner_item)
        adapterPriority.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = adapterPriority
        spinnerType.setSelection(category?.categoryItem?.priority ?: 0)
        etName.setText(financeViewModel.categoryBeginLiveData.value!!.filter {  it.key == category?.key}[0].categoryBegin.name)
        imageChoose.setImageDrawable(ContextCompat.getDrawable(requireContext(), requireContext().resources.getIdentifier(financeViewModel.categoryBeginLiveData.value!!.filter {  it.key == category?.key}[0].categoryBegin.path, "drawable", requireContext().packageName)))
        imageChoose.tag = financeViewModel.categoryBeginLiveData.value!!.filter {  it.key == category?.key}[0].categoryBegin.path

        imageChoose.setOnClickListener {
            IconsChooserAlertDialog(requireContext()){ path->
                imageChoose.setImageDrawable(ContextCompat.getDrawable(requireContext(), requireContext().resources.getIdentifier(path, "drawable", requireContext().packageName)))
                imageChoose.tag = path
            }
        }

        builder.setPositiveButton("Применить") {dialog, _ ->

            val newName = etName.text.toString()
            val newPriority = spinnerType.selectedItemPosition
            val newPath = imageChoose.tag.toString()
            if (etName.text.toString() != financeViewModel.categoryBeginLiveData.value!!.filter {  it.key == category?.key}[0].categoryBegin.path) {
                    if (financeViewModel.categoryLiveData.value?.all { financeViewModel.categoryBeginLiveData.value!!.filter {begin->  begin.key == it.key}[0].categoryBegin.path != etName.text.toString() } == false) Toast.makeText(
                        context,
                        "Такая категория уже существует!",
                        Toast.LENGTH_LONG
                    ).show()
                    else {
                        table.child("Users").child(auth.currentUser!!.uid).child("Categories")
                            .child("Categories base").child(category!!.key).setValue(_CategoryBegin(newName, newPath))
                        category.categoryItem.priority = newPriority
                        table.child("Users").child(auth.currentUser!!.uid).child("Categories")
                            .child(
                                "${vpAdapter.getDate(binding.viewpager.currentItem).second}/${
                                    vpAdapter.getDate(
                                        binding.viewpager.currentItem
                                    ).first
                                }"
                            ).child("ExpenseCategories").child(category.key).setValue(category.categoryItem)
                    }
            } else Toast.makeText(context, "Вы не заполнили название категории.", Toast.LENGTH_LONG).show()

            dialog.dismiss()
        }

        builder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(R.drawable.listview_shadow)
        dialog.show()
    }

    private fun cancelDistribution(){
        if(category.size!=0){
                for (categoryItem in category){
                    categoryItem.categoryItem.total = "%.2f".format(categoryItem.categoryItem.total.toDouble() - categoryItem.categoryItem.remainder.toDouble()).replace(',','.')
                    categoryItem.categoryItem.remainder = "0"

                    table.child("Users").child(auth.currentUser!!.uid)
                        .child("Categories").child("${Calendar.getInstance().get(Calendar.YEAR)}/${Calendar.getInstance().get(Calendar.MONTH)+1}")
                        .child("ExpenseCategories").child(categoryItem.key).setValue(categoryItem.categoryItem)

                    val sharedPreferences = requireContext().getSharedPreferences("preference_distribute", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putBoolean("isDistributed", false)
                    editor.putString("isDistributedDay", "")
                    editor.apply()
                    binding.calculate.text = resources.getString(R.string.fab_calculate)
                }
        } else Toast.makeText(context, "Вы еще не выбрали категории расходов на этот месяц!", Toast.LENGTH_LONG).show()
    }

    private fun distributeMoney(){
            if(category.size!=0){
                val highPriorityCategories = category.filter { it.categoryItem.priority == 2 }
                val mediumPriorityCategories = category.filter { it.categoryItem.priority == 1 }
                val lowPriorityCategories = category.filter { it.categoryItem.priority == 0 }

                val totalHighPriorityCategories = highPriorityCategories.size
                val totalMediumPriorityCategories = mediumPriorityCategories.size
                val totalLowPriorityCategories = lowPriorityCategories.size

                var highPriorityCoefficient =0.0
                var mediumPriorityCoefficient = 0.0
                var lowPriorityCoefficient = 0.0


                if(totalHighPriorityCategories !=0){
                    if(totalMediumPriorityCategories!=0 && totalLowPriorityCategories!=0){
                        mediumPriorityCoefficient = totalMediumPriorityCategories/category.size.toDouble()
                        highPriorityCoefficient = (1.0-mediumPriorityCoefficient) * ((totalMediumPriorityCategories+totalHighPriorityCategories)/category.size.toDouble())
                        lowPriorityCoefficient = 1.0-highPriorityCoefficient-mediumPriorityCoefficient
                    } else if (totalLowPriorityCategories!=0){
                        lowPriorityCoefficient = 0.33*totalLowPriorityCategories/category.size.toDouble()
                        highPriorityCoefficient = 1.0-lowPriorityCoefficient
                    } else if (totalMediumPriorityCategories!=0){
                        mediumPriorityCoefficient = 0.66*totalMediumPriorityCategories/category.size.toDouble()
                        highPriorityCoefficient = 1.0-mediumPriorityCoefficient
                    } else highPriorityCoefficient = 1.0

                } else if (totalMediumPriorityCategories!=0){
                    if(totalLowPriorityCategories!=0){
                        lowPriorityCoefficient = 0.66 * totalLowPriorityCategories/category.size.toDouble()
                        mediumPriorityCoefficient = 1.0 - lowPriorityCoefficient
                    }
                    else{
                        mediumPriorityCoefficient = 1.0
                    }

                }
                else if(totalLowPriorityCategories!=0){
                    lowPriorityCoefficient = 1.0
                }

                totalMoney{ totalMoney->
                    val highPriorityBudget:Double = totalMoney * highPriorityCoefficient / totalHighPriorityCategories
                    val mediumPriorityBudget:Double = totalMoney * mediumPriorityCoefficient/ totalMediumPriorityCategories
                    val lowPriorityBudget:Double = totalMoney * lowPriorityCoefficient / totalLowPriorityCategories
                    var totalCheck = 0.0
                    for (categoryItem in category){
                        val expence = if(categoryItem.categoryItem.remainder.toDouble() == 0.0) categoryItem.categoryItem.total.toDouble() else categoryItem.categoryItem.total.toDouble()-categoryItem.categoryItem.remainder.toDouble()
                        categoryItem.categoryItem.total = "%.2f".format(when(categoryItem.categoryItem.priority){
                            0  -> lowPriorityBudget
                            1 -> mediumPriorityBudget
                            else -> highPriorityBudget
                        }).replace(',','.')
                        categoryItem.categoryItem.remainder = "%.2f".format(categoryItem.categoryItem.total.toDouble()-expence).replace(',','.')
                        totalCheck+=categoryItem.categoryItem.total.toDouble()

                        table.child("Users").child(auth.currentUser!!.uid)
                            .child("Categories").child("${Calendar.getInstance().get(Calendar.YEAR)}/${Calendar.getInstance().get(Calendar.MONTH)+1}")
                            .child("ExpenseCategories").child(categoryItem.key).setValue(categoryItem.categoryItem)

                        val sharedPreferences = requireContext().getSharedPreferences("preference_distribute", Context.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.putBoolean("isDistributed", true)
                        editor.putString("isDistributedDay", "${Calendar.getInstance().get(Calendar.MONTH)}.${Calendar.getInstance().get(Calendar.YEAR)}")
                        editor.apply()
                        binding.calculate.text = resources.getString(R.string.fab_cancel_calculate)

                    }
                }
            } else Toast.makeText(context, "Вы еще не выбрали категории расходов на этот месяц!", Toast.LENGTH_LONG).show()
        }

    private fun totalMoney(callback: (Double) -> Unit) {
        var total = 0.0
        showBudgetSelectionDialog{
           it.forEach{budgetItem->
               when(budgetItem.budgetItem.currency){
                   baseBudget[0].budgetItem.currency -> {
                       total += budgetItem.budgetItem.amount.toDouble() - withSub(budgetItem.key)
                   }
                   else ->{
                       val currencyConvertor = ExchangeRateManager.getExchangeRateResponse(requireContext())
                       if(currencyConvertor!=null){
                           total += when(currencyConvertor.baseCode){
                               baseBudget[0].budgetItem.currency -> (budgetItem.budgetItem.amount.toDouble() - withSub(budgetItem.key))/currencyConvertor.conversionRates[budgetItem.budgetItem.currency]!!
                               else->{
                                   val newValueToBase = ((budgetItem.budgetItem.amount.toDouble() - withSub(budgetItem.key))/currencyConvertor.conversionRates[budgetItem.budgetItem.currency]!!)
                                   newValueToBase*currencyConvertor.conversionRates[baseBudget[0].budgetItem.currency]!!
                               }
                           }
                       }
                   }
               }
           }
           callback(total - withLoan())
        }
    }

    private fun withSub(budgetItemKey:String) = subList.filter { !it.subItem.isCancelled
            && !it.subItem.isDeleted
            && it.subItem.budgetId == budgetItemKey
            && it.subItem.date.split(".")[1].toInt() == Calendar.getInstance().get(Calendar.MONTH)+1
            && it.subItem.date.split(".")[2].toInt() == Calendar.getInstance().get(Calendar.YEAR)}
        .sumOf { it.subItem.amount.toDouble() }

    private fun withLoan() = loanList.filter {
        if(it.loanItem.period!=null) {
            !it.loanItem.isFinished
                    && !it.loanItem.isDeleted
                    && it.loanItem.dateNext!!.split(".")[1].toInt() == Calendar.getInstance().get(Calendar.MONTH)+1
        }
        else{
            !it.loanItem.isFinished
                    && !it.loanItem.isDeleted
                    && it.loanItem.dateOfEnd.split(".")[1].toInt() == Calendar.getInstance().get(Calendar.MONTH)+1
        }
    }.sumOf {
        when(it.loanItem.currency){
            baseBudget[0].budgetItem.currency -> {
                it.loanItem.amount.toDouble()
            }
            else ->{
                val currencyConvertor = ExchangeRateManager.getExchangeRateResponse(requireContext())
                if(currencyConvertor!=null){
                    when(currencyConvertor.baseCode){
                        baseBudget[0].budgetItem.currency ->
                            it.loanItem.amount.toDouble()/currencyConvertor.conversionRates[it.loanItem.currency]!!
                        else->{
                            val newValueToBase = ((it.loanItem.amount.toDouble())/currencyConvertor.conversionRates[it.loanItem.currency]!!)
                            newValueToBase*currencyConvertor.conversionRates[baseBudget[0].budgetItem.currency]!!
                        }
                    }
                } else {
                    0.0
                }
            }
        }
    }


    private fun showBudgetSelectionDialog(callback:(List<BudgetItemWithKey>)->Unit) {
        val alertDialogBuilder = AlertDialog.Builder(requireContext())
        alertDialogBuilder.setTitle("Выберите счет")
        val budgetNames = (baseBudget+otherBudget).filter { !it.budgetItem.isDeleted }.map { it.budgetItem.name }.toTypedArray()
        val checkedItems = BooleanArray(budgetNames.size) { false }
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        val selectAllCheckbox = CheckBox(requireContext())
        selectAllCheckbox.text = "Выбрать все"
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        params.gravity = Gravity.CENTER
        params.setMargins(16, 16, 16, 16)
        selectAllCheckbox.layoutParams = params
        layout.addView(selectAllCheckbox)
        val listView = ListView(requireContext())
        listView.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_multiple_choice, budgetNames)
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        listView.setOnItemClickListener { _, _, position, _ ->
            checkedItems[position] = !checkedItems[position]
            selectAllCheckbox.isChecked = checkedItems.all { it }
        }

        layout.addView(listView)

        selectAllCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked){
                for (i in 0 until listView.count) {
                    listView.setItemChecked(i, true)
                    checkedItems[i] = true
                }
            }
        }

        selectAllCheckbox.setOnClickListener {
            if(!selectAllCheckbox.isChecked){
                for (i in 0 until listView.count) {
                    listView.setItemChecked(i, false)
                    checkedItems[i] = false
                }
            }
        }

        alertDialogBuilder.setView(layout)
        alertDialogBuilder.setPositiveButton("Распределить") { dialog, _ ->
            val selectedBudgets = mutableListOf<BudgetItemWithKey>()
            val checkedItem = listView.checkedItemPositions
            for (i in 0 until checkedItem.size()) {
                if (checkedItem.valueAt(i)) {
                    val budget = (baseBudget+otherBudget)[checkedItem.keyAt(i)]
                    selectedBudgets.add(budget)
                }
            }
            callback(selectedBudgets)
            dialog.dismiss()
        }
        alertDialogBuilder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = alertDialogBuilder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.listview_shadow)
        dialog.show()
    }

    private fun beginCheckUpCategories(){
        val categoryReference = table.child("Users").child(auth.currentUser!!.uid).child("Categories")
            .child("${Calendar.getInstance().get(Calendar.YEAR)}/${Calendar.getInstance().get(Calendar.MONTH)+1}").child("ExpenseCategories")

        categoryReference.addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (expenseCategory in snapshot.children){
                        expenseCategory.getValue(_CategoryItem::class.java)?.let {category->
                           if(category.isPlanned){
                               categoryReference.child(expenseCategory.key.toString()).child("planned").setValue(false).addOnCompleteListener {
                                   table.child("Users").child(auth.currentUser!!.uid).child("History")
                                       .child("${Calendar.getInstance().get(Calendar.YEAR)}/${Calendar.getInstance().get(Calendar.MONTH)+1}").addListenerForSingleValueEvent(
                                           object :ValueEventListener{
                                               override fun onDataChange(snapshot: DataSnapshot) {
                                                   var sum = 0.0
                                                   for (historyItem in snapshot.children){
                                                       historyItem.getValue(HistoryItem::class.java)?.let {
                                                           sum += if (it.placeId == expenseCategory.key) abs(it.baseAmount.toDouble()) else 0.0
                                                       }
                                                   }
                                                   categoryReference.child(expenseCategory.key.toString()).child("total").setValue(if (sum==0.0) "0" else "%.2f".format(sum).replace(",", "."))
                                               }

                                               override fun onCancelled(error: DatabaseError) {
                                               }

                                           }
                                       )
                               }
                           }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("errorLog", error.toException().toString())
                }
            })
    }

    private fun updateHistory(){
        table.child("Users").child(auth.currentUser!!.uid).child("History")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    historyList.clear()
                    for (years in snapshot.children) {
                        for(months in years.children){
                            for(histories in months.children)
                                histories.getValue(HistoryItem::class.java)?.let {
                                    historyList.add(it)
                                }
                        }
                    }
                    financeViewModel.updateHistoryData(historyList)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("errorLog", error.toException().toString())
                }
            })
    }

    private fun updatePlan(){
        table.child("Users").child(auth.currentUser!!.uid).child("Plan")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    planList.clear()
                    for (years in snapshot.children) {
                        for(months in years.children){
                            for(histories in months.children)
                                histories.getValue(HistoryItem::class.java)?.let {
                                    planList.add(it)
                                }
                        }
                    }
                    financeViewModel.updatePlanData(planList)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("errorLog", error.toException().toString())
                }
            })
    }

    private fun updateBudget(){
        table.child("Users").child(auth.currentUser!!.uid).child("Budgets").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                    baseBudget.clear()
                    otherBudget.clear()
                    for (budget in snapshot.children){
                        budget.getValue(_BudgetItem::class.java)?.let {
                            if(budget.key=="Base budget") {
                                baseBudget.add(BudgetItemWithKey(budget.key.toString(), it))
                            Log.e(Constants.TAG_USER, it.toString())}
                            else {
                                for(other in budget.children){
                                    other.getValue(_BudgetItem::class.java)?.let { data ->
                                        otherBudget.add(BudgetItemWithKey(other.key.toString(), data))
                                        Log.e(Constants.TAG_USER+"other", data.toString())
                                    }
                                }
                            }
                        }
                    }
                    val combinedList = mutableListOf<BudgetItemWithKey>()

                    combinedList.run {
                        add(baseBudget[0])
                        addAll(otherBudget)
                    }

                Log.e(Constants.TAG_USER, combinedList.toString())
                financeViewModel.updateBudgetData(combinedList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("errorLog", error.toException().toString())
            }
        })
    }

    private fun updateBeginCategory(){
        table.child("Users").child(auth.currentUser!!.uid).child("Categories").child("Categories base")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    categoryBegin.clear()
                    for(category in snapshot.children) {
                        category.getValue(_CategoryBegin::class.java)?.let {
                            categoryBegin.add(CategoryBeginWithKey(category.key.toString(), it))
                        }
                    }
                    financeViewModel.updateCategoryBeginData(categoryBegin)
                    adapterCategory.notifyDataSetChanged()
                    }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("errorLog", error.toException().toString())
                }
            })
    }

    private fun openHistory(){
        table.child("Users").child(auth.currentUser!!.uid).child("History")
            .addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                historyList.clear()

                for (years in snapshot.children) {
                    for(months in years.children){
                        for(histories in months.children)
                            histories.getValue(HistoryItem::class.java)?.let {
                            historyList.add(it)
                        }
                    }
                }
                findNavController().navigate(R.id.action_nav_finance_to_historyFragment)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("errorLog", error.toException().toString())
            }
        })
    }

    private fun updateCategory(/*month:Int, year:Int*/){
        table.child("Users").child(auth.currentUser!!.uid).child("Categories")
            .child("${Calendar.getInstance().get(Calendar.YEAR)}/${Calendar.getInstance().get(Calendar.MONTH)+1}").child("ExpenseCategories").addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    category.clear()
                    for (expenseCategory in snapshot.children){
                        expenseCategory.getValue(_CategoryItem::class.java)?.let {
                            category.add(CategoryItemWithKey(expenseCategory.key.toString(), it))
                        }
                    }
                    Log.e("UPDATE_CATEGORY", "YES")

                    financeViewModel.updateCategoryData(category.sortedByDescending { it.categoryItem.priority })
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("errorLog", error.toException().toString())
                }
            })
    }

    private fun updateCategoryOnce(month:Int, year:Int, doAfter: (Unit)->Unit){
        table.child("Users").child(auth.currentUser!!.uid).child("Categories")
            .child("$year/${month}").child("ExpenseCategories").addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    category.clear()
                    for (expenseCategory in snapshot.children){
                        expenseCategory.getValue(_CategoryItem::class.java)?.let {
                            category.add(CategoryItemWithKey(expenseCategory.key.toString(), it))
                        }
                    }
                    financeViewModel.updateCategoryData(category.sortedByDescending { it.categoryItem.priority })
                    doAfter(Unit)
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("errorLog", error.toException().toString())
                }
            })
    }

    private fun updateGoals(){
        table.child("Users").child(auth.currentUser!!.uid).child("Goals").addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    goalList.clear()
                    for (goal in snapshot.children){
                        goal.getValue(GoalItem::class.java)?.let {gi->
                            goalList.add(GoalItemWithKey(goal.key.toString(), gi))
                        }
                    }

                    financeViewModel.updateGoalsData(

                        goalList.asSequence()
                            .filter { it.goalItem.date!=null && if (it.goalItem.date!=null){
                                Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY,0)
                                    set(Calendar.MINUTE,0)
                                    set(Calendar.SECOND,0)
                                }.timeInMillis <= Calendar.getInstance().apply {
                                    set(
                                        it.goalItem.date!!.split(".")[2].toInt(),
                                        it.goalItem.date!!.split(".")[1].toInt()-1,
                                        it.goalItem.date!!.split(".")[0].toInt(), 0,0,0)}.timeInMillis
                            } else true}
                            .sortedByDescending { it.goalItem.target.toDouble() - it.goalItem.current.toDouble() }
                            .toList()
                                +
                                goalList
                                    .asSequence() .filter { it.goalItem.date==null }
                                    .sortedByDescending { it.goalItem.target.toDouble() - it.goalItem.current.toDouble() }
                                    .toList()

                                + goalList.asSequence()
                            .filter { it.goalItem.date!=null && if (it.goalItem.date!=null){
                                Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY,0)
                                    set(Calendar.MINUTE,0)
                                    set(Calendar.SECOND,0)
                                }.timeInMillis > Calendar.getInstance().apply {
                                    set(
                                        it.goalItem.date!!.split(".")[2].toInt(),
                                        it.goalItem.date!!.split(".")[1].toInt()-1,
                                        it.goalItem.date!!.split(".")[0].toInt(), 0,0,0)
                                        }.timeInMillis
                            } else true}
                            .sortedByDescending { it.goalItem.target.toDouble() - it.goalItem.current.toDouble() }
                            .toList()
                    )
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("errorLog", error.toException().toString())
                }
            })
    }

    private fun updateSubs(){
        table.child("Users").child(auth.currentUser!!.uid).child("Subs").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                subList.clear()
                for (sub in snapshot.children){
                    sub.getValue(SubItem::class.java)?.let {si->
                        subList.add(SubItemWithKey(sub.key.toString(), si))
                    }
                }

                financeViewModel.updateSubsData(
                    subList.asSequence()
                        .filter { !it.subItem.isCancelled  && !it.subItem.isDeleted  } .toList()
                            + subList.asSequence() .filter { it.subItem.isCancelled }.toList()
                + subList.filter { it.subItem.isDeleted }.toList())
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("errorLog", error.toException().toString())
            }
        })
    }


    private fun updateLoans(){
        table.child("Users").child(auth.currentUser!!.uid).child("Loans").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                loanList.clear()
                for (loan in snapshot.children){
                    loan.getValue(LoanItem::class.java)?.let {loanItem->
                        loanList.add(LoanItemWithKey(loan.key.toString(), loanItem))
                    }
                }

                financeViewModel.updateLoansData(
                    //активные
                    loanList.asSequence()
                        .filter { !it.loanItem.isFinished  && !it.loanItem.isDeleted && if (it.loanItem.dateNext!=null){
                            Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY,0)
                                set(Calendar.MINUTE,0)
                                set(Calendar.SECOND,0)
                            }.timeInMillis <= Calendar.getInstance().apply {
                                set(
                                    it.loanItem.dateNext!!.split(".")[2].toInt(),
                                    it.loanItem.dateNext!!.split(".")[1].toInt()-1,
                                    it.loanItem.dateNext!!.split(".")[0].toInt(), 0,0,0)}.timeInMillis
                        } else
                            Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY,0)
                                set(Calendar.MINUTE,0)
                                set(Calendar.SECOND,0)
                            }.timeInMillis <= Calendar.getInstance().apply {
                                set(
                                    it.loanItem.dateOfEnd.split(".")[2].toInt(),
                                    it.loanItem.dateOfEnd.split(".")[1].toInt()-1,
                                    it.loanItem.dateOfEnd.split(".")[0].toInt(), 0,0,0)}.timeInMillis
                        } .toList().sortedBy {loan->
                            when (loan.loanItem.period){
                                null->loan.loanItem.dateOfEnd.split('.').let {
                                    ChronoUnit.DAYS.between(
                                        LocalDate.of(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.DAY_OF_MONTH)),
                                        LocalDate.of(it[2].toInt(),it[1].toInt(), it[0].toInt()))
                                }
                                else->loan.loanItem.dateNext?.split('.')?.let {
                                    ChronoUnit.DAYS.between(
                                        LocalDate.of(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.DAY_OF_MONTH)),
                                        LocalDate.of(it[2].toInt(),it[1].toInt(), it[0].toInt()))
                                }
                            }
                        }
                            +
                            //просроченные
                            loanList.asSequence()
                                .filter { !it.loanItem.isFinished  && !it.loanItem.isDeleted && if (it.loanItem.dateNext!=null){
                            Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY,0)
                                set(Calendar.MINUTE,0)
                                set(Calendar.SECOND,0)
                            }.timeInMillis > Calendar.getInstance().apply {
                                set(
                                    it.loanItem.dateNext!!.split(".")[2].toInt(),
                                    it.loanItem.dateNext!!.split(".")[1].toInt()-1,
                                    it.loanItem.dateNext!!.split(".")[0].toInt(), 0,0,0)}.timeInMillis
                            } else
                            Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY,0)
                                set(Calendar.MINUTE,0)
                                set(Calendar.SECOND,0)
                            }.timeInMillis > Calendar.getInstance().apply {
                                set(
                                    it.loanItem.dateOfEnd.split(".")[2].toInt(),
                                    it.loanItem.dateOfEnd.split(".")[1].toInt()-1,
                                    it.loanItem.dateOfEnd.split(".")[0].toInt(), 0,0,0)}.timeInMillis
                        } .toList().sortedBy {loan->
                                    when (loan.loanItem.period){
                                        null->loan.loanItem.dateOfEnd.split('.').let {
                                            ChronoUnit.DAYS.between(
                                                LocalDate.of(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.DAY_OF_MONTH)),
                                                LocalDate.of(it[2].toInt(),it[1].toInt(), it[0].toInt()))
                                        }
                                        else->loan.loanItem.dateNext?.split('.')?.let {
                                            ChronoUnit.DAYS.between(
                                                LocalDate.of(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.DAY_OF_MONTH)),
                                                LocalDate.of(it[2].toInt(),it[1].toInt(), it[0].toInt()))
                                        }
                                    }
                                }
                            //завершенные
                            + loanList.asSequence()
                                .filter { it.loanItem.isFinished }.toList()
                        .sortedBy {loan->
                            when (loan.loanItem.period){
                                null->loan.loanItem.dateOfEnd.split('.').let {
                                    ChronoUnit.DAYS.between(
                                        LocalDate.of(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.DAY_OF_MONTH)),
                                        LocalDate.of(it[2].toInt(),it[1].toInt(), it[0].toInt()))
                                }
                                else->loan.loanItem.dateNext?.split('.')?.let {
                                    ChronoUnit.DAYS.between(
                                        LocalDate.of(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.DAY_OF_MONTH)),
                                        LocalDate.of(it[2].toInt(),it[1].toInt(), it[0].toInt()))
                                }
                            }
                        }
                            + loanList.filter { it.loanItem.isDeleted }.toList())
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("errorLog", error.toException().toString())
            }
        })
    }
}

