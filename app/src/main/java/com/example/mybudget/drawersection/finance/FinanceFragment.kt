package com.example.mybudget.drawersection.finance

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
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
import com.example.mybudget.drawersection.finance.category.CategoryAdapter
import com.example.mybudget.drawersection.finance.category.SwipeHelper
import com.example.mybudget.drawersection.finance.category._CategoryBegin
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import kotlinx.coroutines.launch
import java.util.Calendar

class FinanceFragment : Fragment() {

    private lateinit var binding:PageFinanceBinding
    private lateinit var vpAdapter:DateViewPagerAdapter
    private lateinit var adapterBudget: BudgetAdapter
    private lateinit var adapterCategory: CategoryAdapter

    private lateinit var auth: FirebaseAuth
    private lateinit var table: DatabaseReference

    private lateinit var financeViewModel:FinanceViewModel


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

        val sharedPreferences =
            requireContext().getSharedPreferences("preference_distribute", Context.MODE_PRIVATE)

        when (sharedPreferences.getBoolean("isDistributed", false)) {
            false -> binding.calculate.text = resources.getString(R.string.fab_calculate)
            else -> {
                val time = sharedPreferences.getString("isDistributedDay", "")
                if (time?.isNotEmpty() == true && time.split(".")[0].toInt() == Calendar.getInstance()
                        .get(Calendar.MONTH) && time.split(".")[1].toInt() == Calendar.getInstance()
                        .get(Calendar.YEAR)
                ) {
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
            if (isExpanded) {
                hideFabs {}
            } else showFabs()
            isExpanded = !isExpanded
        }

        binding.fabNewTransaction.setOnClickListener {
            findNavController().navigate(R.id.action_nav_finance_to_newTransactionFragment)
            isExpanded = false
        }

        binding.fabHistory.setOnClickListener {
            if (!financeViewModel.historyLiveData.value.isNullOrEmpty() || !financeViewModel.planLiveData.value.isNullOrEmpty()) {
                findNavController().navigate(R.id.action_nav_finance_to_historyFragment)
            } else Toast.makeText(
                requireContext(),
                getString(R.string.error_history_not_exists),
                Toast.LENGTH_LONG
            ).show()
            isExpanded = false
        }

        binding.fabCalculate.setOnClickListener {
            hideFabs {
                isExpanded = false
                binding.viewpager.currentItem = adapterCategory.getCurrentDate()
                lifecycleScope.launch {
                    financeViewModel.updateCategoryOnce(
                        vpAdapter.getDate(binding.viewpager.currentItem).first,
                        vpAdapter.getDate(binding.viewpager.currentItem).second
                    ) {
                        when (sharedPreferences.getBoolean("isDistributed", false)) {
                            false -> distributeMoney()
                            else -> cancelDistribution()
                        }
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

        activity?.let {financeViewModel = ViewModelProvider(it, FinanceViewModelFactory(
            table = table,
            auth = auth,
            context = requireContext()
        ))[FinanceViewModel::class.java] }

        adapterCategory = CategoryAdapter(requireContext(), emptyList(), viewLifecycleOwner, table, auth, requireActivity())
        adapterBudget = BudgetAdapter(requireContext(), emptyList(), lifecycleScope, table, auth)
        binding.budgetsList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.budgetsList.adapter = adapterBudget

        financeViewModel.budgetLiveData.observe(viewLifecycleOwner){
            adapterBudget.updateData(it.filter {budgetItemWithKey ->  !budgetItemWithKey.budgetItem.isDeleted })
            binding.viewpager.currentItem = adapterCategory.getCurrentDate()
            lifecycleScope.launch {
                financeViewModel.updateCategoryOnce(
                    vpAdapter.getDate(binding.viewpager.currentItem).first,
                    vpAdapter.getDate(binding.viewpager.currentItem).second
                ) {}
            }
        }

        binding.categoryList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.categoryList.adapter = adapterCategory

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
            lifecycleScope.launch {
                adapterCategory.updateData(it)
            }
        }

        vpAdapter.setData(listOf(Pair(Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.YEAR))), Pair(Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.YEAR)), binding.viewpager, adapterCategory)

        financeViewModel.categoryDate.observe(viewLifecycleOwner){ list->
            lifecycleScope.launch {
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
                            financeViewModel.updateCategoryDate(mutableList)
                        }
                        list[0].first >= Calendar.getInstance().get(Calendar.MONTH)+1
                                && list[0].second == Calendar.getInstance().get(Calendar.YEAR)->{
                            binding.leftNav.visibility = View.INVISIBLE
                            mutableList.add(Pair(Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.YEAR)))
                            financeViewModel.updateCategoryDate(mutableList)
                        }
                        list[0].second <= Calendar.getInstance().get(Calendar.YEAR)->{
                            binding.rightNav.visibility = View.INVISIBLE
                            mutableList.add(Pair(Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.YEAR)))
                            financeViewModel.updateCategoryDate(mutableList)
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
                try {
                    financeViewModel.updateDate(vpAdapter.getDate(binding.viewpager.currentItem))
                } catch (ex: IndexOutOfBoundsException){
                    binding.viewpager.currentItem = 0
                }
            }
            leftAndRightRows()
            }
        }

        val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                leftAndRightRows()
                adapterCategory.updateNewDate(binding.viewpager.currentItem)
                financeViewModel.updateDate(vpAdapter.getDate(binding.viewpager.currentItem))
                lifecycleScope.launch {
                    financeViewModel.updateCategoryOnce(vpAdapter.getDate(binding.viewpager.currentItem).first, vpAdapter.getDate(binding.viewpager.currentItem).second){}
                }
            }
        }

        binding.viewpager.registerOnPageChangeCallback(onPageChangeCallback)
        binding.leftNav.setOnClickListener {
            binding.viewpager.currentItem -= 1
        }

        binding.rightNav.setOnClickListener {
            binding.viewpager.currentItem += 1
        }
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
                        .setTitle(getString(R.string.delete_category))
                        .setMessage(getString(R.string.delete_category_sure))
                        .setPositiveButton(getString(R.string.agree)) { dialog, _ ->
                            adapterCategory.deleteItemAtPosition(position, vpAdapter.getDate(binding.viewpager.currentItem))
                            dialog.dismiss()
                        }
                        .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
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

        builder.setPositiveButton(getString(R.string.apply)) {dialog, _ ->

            val newName = etName.text.toString()
            val newPriority = spinnerType.selectedItemPosition
            val newPath = imageChoose.tag.toString()
            if (etName.text.toString() != financeViewModel.categoryBeginLiveData.value!!.filter {  it.key == category?.key}[0].categoryBegin.path) {
                    if (financeViewModel.categoryLiveData.value?.all { financeViewModel.categoryBeginLiveData.value!!.filter {begin->  begin.key == it.key}[0].categoryBegin.path != etName.text.toString() } == false) Toast.makeText(
                        context,
                        getString(R.string.error_category_exist),
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
            } else Toast.makeText(context, getString(R.string.error_category_name), Toast.LENGTH_LONG).show()

            dialog.dismiss()
        }

        builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
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
        if (financeViewModel.cancelDistribution()) binding.calculate.text = resources.getString(R.string.fab_calculate)
        else Toast.makeText(context, resources.getString(R.string.error_category_not_choosen), Toast.LENGTH_LONG).show()
    }

    private fun distributeMoney(){
        showBudgetSelectionDialog {
            if(financeViewModel.distributeMoney(selectedBudget = it)) binding.calculate.text = resources.getString(R.string.fab_cancel_calculate)
            else Toast.makeText(context, getString(R.string.error_category_not_exists), Toast.LENGTH_LONG).show()
        }
    }

    private fun showBudgetSelectionDialog(callback:(List<BudgetItemWithKey>)->Unit) {
        val alertDialogBuilder = AlertDialog.Builder(requireContext())
        alertDialogBuilder.setTitle(getString(R.string.select_budget))
        val budgetNames = financeViewModel.budgetLiveData.value!!.filter { !it.budgetItem.isDeleted }.map { it.budgetItem.name }.toTypedArray()
        val checkedItems = BooleanArray(budgetNames.size) { false }
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        val selectAllCheckbox = CheckBox(requireContext())
        selectAllCheckbox.text = getString(R.string.select_all)
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
        alertDialogBuilder.setPositiveButton(getString(R.string.distribute)) { dialog, _ ->
            val selectedBudgets = mutableListOf<BudgetItemWithKey>()
            val checkedItem = listView.checkedItemPositions
            for (i in 0 until checkedItem.size()) {
                if (checkedItem.valueAt(i)) {
                    val budget = (financeViewModel.budgetLiveData.value!!)[checkedItem.keyAt(i)]
                    selectedBudgets.add(budget)
                }
            }
            callback(selectedBudgets)
            dialog.dismiss()
        }
        alertDialogBuilder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = alertDialogBuilder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.listview_shadow)
        dialog.show()
    }
}