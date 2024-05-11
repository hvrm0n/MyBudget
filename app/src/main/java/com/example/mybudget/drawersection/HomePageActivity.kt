package com.example.mybudget.drawersection

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.example.mybudget.ExchangeRateManager
import com.example.mybudget.R
import com.example.mybudget.databinding.HomePageBinding
import com.example.mybudget.drawersection.finance.FinanceViewModel
import com.example.mybudget.drawersection.finance.HistoryItem
import com.example.mybudget.drawersection.finance.SelectedBudgetViewModel
import com.example.mybudget.drawersection.finance.budget.BudgetItemWithKey
import com.example.mybudget.drawersection.finance.budget._BudgetItem
import com.example.mybudget.drawersection.finance.category.CategoryBeginWithKey
import com.example.mybudget.drawersection.finance.category.CategoryItemWithKey
import com.example.mybudget.drawersection.finance.category._CategoryBegin
import com.example.mybudget.drawersection.finance.category._CategoryItem
import com.example.mybudget.drawersection.goals.GoalItem
import com.example.mybudget.drawersection.goals.GoalItemWithKey
import com.example.mybudget.drawersection.loans.LoanItem
import com.example.mybudget.drawersection.loans.LoanItemWithKey
import com.example.mybudget.drawersection.subs.SubItem
import com.example.mybudget.drawersection.subs.SubItemWithKey
import com.example.mybudget.start_pages.StartActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
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

class HomePageActivity : AppCompatActivity(), Observer<List<BudgetItemWithKey>> {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: HomePageBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var table: DatabaseReference

    private lateinit var financeViewModel: FinanceViewModel
    private lateinit var selectedBudgetViewModel: SelectedBudgetViewModel
    private lateinit var navView:NavigationView
    private val combinedList = mutableListOf<BudgetItemWithKey>()
    private val baseBudget = mutableListOf<BudgetItemWithKey>()
    private val otherBudget = mutableListOf<BudgetItemWithKey>()
    private val category = mutableListOf<CategoryItemWithKey>()
    private val categoryBegin = mutableListOf<CategoryBeginWithKey>()
    private val historyList = mutableListOf<HistoryItem>()
    private val dateList = mutableListOf<Pair<Int, Int>>()
    private val planList = mutableListOf<HistoryItem>()
    private val goalList = mutableListOf<GoalItemWithKey>()
    private val subList = mutableListOf<SubItemWithKey>()
    private val loanList = mutableListOf<LoanItemWithKey>()

    private var type:String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = HomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(setOf(
            R.id.nav_finance, R.id.nav_goals, R.id.nav_loans, R.id.nav_subs, R.id.nav_charts, R.id.nav_settings), drawerLayout)

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        binding.exitButton.setOnClickListener {
            val intent = Intent(this, StartActivity::class.java)
            intent.putExtra("exit", true)
            startActivity(intent)
            this.finishAffinity()
        }

        val headerView = navView.getHeaderView(0)
        val user = Firebase.auth.currentUser
        if (user != null && user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }) {
            headerView.findViewById<TextView>(R.id.userNameHeader).text = Firebase.auth.currentUser?.displayName
            val imageView: ImageView = headerView.findViewById(R.id.imageView)
            val photoUrl = user.photoUrl
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.personiconpng)
                .error(R.drawable.personiconpng)
                .into(imageView)
        } else{
            headerView.findViewById<TextView>(R.id.userNameHeader).text = Firebase.auth.currentUser?.email
        }

        if (PreferenceManager.getDefaultSharedPreferences(this).all.isEmpty()){
            getNotificationPermission()
            PreferenceManager.getDefaultSharedPreferences(this).edit().putStringSet("sumOfFinance", setOf("Base budget")).apply()
        }
    }


    override fun onStart() {
        super.onStart()
        auth = Firebase.auth
        table = Firebase.database.reference
        financeViewModel = ViewModelProvider(this)[FinanceViewModel::class.java]
        updateBudget()
        updateBeginCategory()
        updateDate()
        beginCheckUpCategories()
        updateCategory()
        updateHistory()
        updatePlan()
        updateGoals()
        updateLoans()
        updateSubs()

        selectedBudgetViewModel = ViewModelProvider(this)[SelectedBudgetViewModel::class.java]
        selectedBudgetViewModel.updateSelectionData(PreferenceManager.getDefaultSharedPreferences(this).getStringSet("sumOfFinance", setOf("Base budget"))?.toList()?:listOf("Base budget") )
        financeViewModel.budgetLiveData.observe(this){
            selectedBudgetViewModel.selectedBudget.observe(this) {
                updateHeaderSum()
            }
            financeViewModel.budgetLiveData.removeObserver(this)
        }
    }


    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("notifications_enabled",  isGranted)
        }

    private fun getNotificationPermission() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        val listener = NavController.OnDestinationChangedListener { _, destination, arguments ->
            if (destination.id == R.id.newGLSFragment) {
                type = arguments?.getString("type")
                title(arguments?.getString("key"), arguments?.getString("type"))
            } else type = null
        }
        navController.addOnDestinationChangedListener(listener)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun title(key:String?, type:String?){
        if (key == null){
            supportActionBar?.title = when(type){
                "loan"-> "Новый обязательный платеж"
                "goal"-> "Новая цель"
                else -> "Новая подписка"
            }
        } else {
           supportActionBar?.title = when(type){
                "loan"-> "Редактирования обязательного платежа"
                "goal"-> "Редактирование цели"
                else -> "Редактирование подписки"
            }
        }
    }

    private fun beginCheckUpCategories(){
        val categoryReference = table.child("Users").child(auth.currentUser!!.uid).child("Categories")
            .child("${Calendar.getInstance().get(Calendar.YEAR)}/${Calendar.getInstance().get(Calendar.MONTH)+1}").child("ExpenseCategories")

        categoryReference.addListenerForSingleValueEvent(object : ValueEventListener{
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

    private fun updateBudget(){
        table.child("Users").child(auth.currentUser!!.uid).child("Budgets").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                baseBudget.clear()
                otherBudget.clear()
                combinedList.clear()
                for (budget in snapshot.children){
                    budget.getValue(_BudgetItem::class.java)?.let {
                        if(budget.key=="Base budget") {
                            baseBudget.add(BudgetItemWithKey(budget.key.toString(), it))
                            }
                        else {
                            for(other in budget.children){
                                other.getValue(_BudgetItem::class.java)?.let { data ->
                                    otherBudget.add(BudgetItemWithKey(other.key.toString(), data))
                                }
                            }
                        }
                    }
                }

                combinedList.run {
                    add(baseBudget[0])
                    addAll(otherBudget)
                }
                financeViewModel.updateBudgetData(combinedList)
                updateHeaderSum()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("errorLog", error.toException().toString())
            }
        })
    }

    fun updateHeaderSum(){
        navView.getHeaderView(0).apply {
            findViewById<TextView>(R.id.currentBalance).text = totalMoney(combinedList) +  getString(resources.getIdentifier(combinedList[0].budgetItem.currency, "string", packageName))
        }
    }

    private fun totalMoney(budgetList: List<BudgetItemWithKey>):String {
        var total = 0.0
        val baseCurrency = budgetList.find {budget-> budget.key == "Base budget" }!!.budgetItem.currency
        val budgets =  selectedBudgetViewModel.selectedBudget.value?: listOf("Base budget")
        Log.e("CheckPrefs", budgets.toString())
        budgetList.filter { budgets.contains(it.key) }.forEach{ budgetItem->
                when(budgetItem.budgetItem.currency){
                    baseCurrency -> {
                        total += budgetItem.budgetItem.amount.toDouble()
                    }
                    else ->{
                        val currencyConvertor = ExchangeRateManager.getExchangeRateResponse(this)
                        if(currencyConvertor!=null){
                            total += when(currencyConvertor.baseCode){
                                baseCurrency -> (budgetItem.budgetItem.amount.toDouble())/currencyConvertor.conversionRates[budgetItem.budgetItem.currency]!!
                                else->{
                                    val newValueToBase = ((budgetItem.budgetItem.amount.toDouble())/currencyConvertor.conversionRates[budgetItem.budgetItem.currency]!!)
                                    newValueToBase*currencyConvertor.conversionRates[baseCurrency]!!
                                }
                            }
                        }
                    }
                }
            }
        return "%.2f".format(total).replace(",", ".")
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
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("errorLog", error.toException().toString())
                }
            })
    }

    private fun updateCategory(){
        table.child("Users").child(auth.currentUser!!.uid).child("Categories")
            .child("${Calendar.getInstance().get(Calendar.YEAR)}/${
                Calendar.getInstance().get(
                    Calendar.MONTH)+1}").child("ExpenseCategories")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    category.clear()
                    for (expenseCategory in snapshot.children){
                        expenseCategory.getValue(_CategoryItem::class.java)?.let {
                            category.add(CategoryItemWithKey(expenseCategory.key.toString(), it))
                        }
                    }
                    financeViewModel.updateCategoryData(category.sortedByDescending { it.categoryItem.priority })
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("errorLog", error.toException().toString())
                }
            })
    }

    private fun updateDate(){
        table.child("Users").child(auth.currentUser!!.uid).child("Categories").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                dateList.clear()
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
                financeViewModel.updateCategoryDate(dateList)
            }
            override fun onCancelled(error: DatabaseError) {}
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

    private fun updatePlan() {
        table.child("Users").child(auth.currentUser!!.uid).child("Plan")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    planList.clear()
                    for (years in snapshot.children) {
                        for (months in years.children) {
                            for (histories in months.children)
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

    private fun updateGoals(){
        table.child("Users").child(auth.currentUser!!.uid).child("Goals").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for (goal in snapshot.children){
                    goalList.clear()
                    goal.getValue(GoalItem::class.java)?.let { gi->
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
                    sub.getValue(SubItem::class.java)?.let { si->
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
                    loan.getValue(LoanItem::class.java)?.let { loanItem->
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

    override fun onChanged(value: List<BudgetItemWithKey>) {
        TODO("Not yet implemented")
    }

}