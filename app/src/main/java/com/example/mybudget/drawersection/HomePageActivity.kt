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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.example.mybudget.ExchangeRateManager
import com.example.mybudget.R
import com.example.mybudget.databinding.HomePageBinding
import com.example.mybudget.drawersection.finance.FinanceViewModel
import com.example.mybudget.drawersection.finance.FinanceViewModelFactory
import com.example.mybudget.drawersection.finance.SelectedBudgetViewModel
import com.example.mybudget.drawersection.finance.budget.BudgetItemWithKey
import com.example.mybudget.start_pages.StartActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database

class HomePageActivity : AppCompatActivity(), Observer<List<BudgetItemWithKey>> {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: HomePageBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var table: DatabaseReference

    private lateinit var financeViewModel: FinanceViewModel
    private lateinit var selectedBudgetViewModel: SelectedBudgetViewModel
    private lateinit var navView:NavigationView


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
            cacheDir.deleteRecursively()
            filesDir.deleteRecursively()
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
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("transaction_enabled",  true).apply()
        }
    }


    override fun onStart() {
        super.onStart()
        auth = Firebase.auth
        table = Firebase.database.reference
        financeViewModel = ViewModelProvider(this, FinanceViewModelFactory(
            table = table,
            auth = auth,
            context = this
        ))[FinanceViewModel::class.java]
        financeViewModel.apply {
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
        }

        selectedBudgetViewModel = ViewModelProvider(this)[SelectedBudgetViewModel::class.java]
        selectedBudgetViewModel.updateSelectionData(PreferenceManager.getDefaultSharedPreferences(this).getStringSet("sumOfFinance", setOf("Base budget"))?.toList()?:listOf("Base budget") )
        financeViewModel.budgetLiveData.observe(this){
            selectedBudgetViewModel.selectedBudget.observe(this) {
                updateHeaderSum()
            }
            financeViewModel.budgetLiveData.removeObserver(this)
        }
        financeViewModel.headerSum.observe(this){
            if (!financeViewModel.budgetLiveData.value.isNullOrEmpty()) {
                updateHeaderSum()
            }
        }
    }


    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("notifications_enabled",  isGranted).apply()
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
        } else {
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("notifications_enabled",  true).apply()
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



    private fun updateHeaderSum(){
        navView.getHeaderView(0).apply {
            findViewById<TextView>(R.id.currentBalance).text = totalMoney(financeViewModel.budgetLiveData.value!!) +  getString(resources.getIdentifier(financeViewModel.budgetLiveData.value!![0].budgetItem.currency, "string", packageName))
        }
    }

    private fun totalMoney(budgetList: List<BudgetItemWithKey>):String {
        var total = 0.0
        val baseCurrency = budgetList.find {budget-> budget.key == "Base budget" }!!.budgetItem.currency
        val budgets = selectedBudgetViewModel.selectedBudget.value?: listOf("Base budget")

        budgetList.filter { budgets.contains(it.key) && !it.budgetItem.isDeleted }.forEach{ budgetItem->
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



    override fun onChanged(value: List<BudgetItemWithKey>) {}
}