package com.example.mybudget.drawersection.finance

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
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
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mybudget.R
import com.example.mybudget.drawersection.finance.category.CategoryAdapter
import com.example.mybudget.drawersection.finance.category.CategoryItem
import com.example.mybudget.drawersection.finance.category.InnerAdapter
import com.example.mybudget.drawersection.finance.category.SwipeHelper
import com.example.mybudget.start_pages.CategoryBegin
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class FinanceFragment : Fragment() {

    private lateinit var layout: LinearLayout
    private lateinit var recyclerViewBudget: RecyclerView
    private lateinit var recyclerViewCategory: RecyclerView
    private lateinit var adapterBudget: BudgetAdapter
    private lateinit var adapterCategory: CategoryAdapter

    private lateinit var fab:FloatingActionButton
    private lateinit var fab_calculate:FloatingActionButton
    private lateinit var fab_transaction:FloatingActionButton
    private lateinit var calculate:TextView
    private lateinit var transaction:TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var table: DatabaseReference
    private lateinit var budgetView:View

    private val baseBudget = mutableListOf<BudgetItem>()
    private val otherBudget = mutableListOf<BudgetItem>()
    private val budgetLiveData: MutableLiveData<List<BudgetItem>> = MutableLiveData()

    private val category = mutableListOf<CategoryItem>()
    private val categoryLiveData: MutableLiveData<List<CategoryItem>> = MutableLiveData()

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
        budgetView = view
        super.onViewCreated(view, savedInstanceState)
        layout = budgetView.findViewById(R.id.linearLayoutFinance)
        fab = budgetView.findViewById(R.id.floatingActionButton)
        fab_calculate = budgetView.findViewById(R.id.fabCalculate)
        fab_transaction = budgetView.findViewById(R.id.fabNewTransaction)
        calculate = budgetView.findViewById(R.id.calculate)
        transaction = budgetView.findViewById(R.id.transaction)

        fab.setOnClickListener {
            if (isExpanded){
                hideFabs()
            } else showFabs()
            isExpanded = !isExpanded
        }

        fab_transaction.setOnClickListener {
            budgetView.findNavController().navigate(R.id.action_nav_finance_to_newTransactionFragment)
        }
    }

    private fun hideFabs(){
        calculate.startAnimation(toBottom)
        fab_calculate.startAnimation(toBottom)
        transaction.startAnimation(toBottom)
        fab_transaction.startAnimation(toBottom)
        fab.startAnimation(toBottomRotate)
        layout.startAnimation(toBG)
    }

    private fun showFabs(){
        calculate.startAnimation(fromBottom)
        fab_calculate.startAnimation(fromBottom)
        transaction.startAnimation(fromBottom)
        fab_transaction.startAnimation(fromBottom)
        fab.startAnimation(fromBottomRotate)
        layout.startAnimation(fromBG)
    }


    override fun onStart() {
        super.onStart()
        auth = Firebase.auth
        table = Firebase.database.reference


        adapterBudget = BudgetAdapter(requireContext(), emptyList(), lifecycleScope, table, auth)
        recyclerViewBudget = budgetView.findViewById(R.id.budgetsList)
        recyclerViewBudget.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerViewBudget.adapter = adapterBudget
        budgetLiveData.observe(viewLifecycleOwner){
            adapterBudget.updateData(it)
        }
        updateBudget()

        adapterCategory = CategoryAdapter(requireContext(), emptyList(), lifecycleScope, table, auth)
        recyclerViewCategory = budgetView.findViewById(R.id.categoryList)
        recyclerViewCategory.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        recyclerViewCategory.adapter = adapterCategory

        val itemTouchHelper = ItemTouchHelper(object : SwipeHelper(recyclerViewCategory) {
            override fun instantiateUnderlayButton(position: Int): List<UnderlayButton> {
                if(position==adapterCategory.itemCount-1){return emptyList() }
                val deleteButton = deleteButton(position)
                val editButton = editButton(position)
                return listOf(deleteButton, editButton)
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerViewCategory)

        categoryLiveData.observe(viewLifecycleOwner){
            adapterCategory.updateData(it)
        }
        updateCategory()
    }

    private fun deleteButton(position: Int) : SwipeHelper.UnderlayButton {
        return SwipeHelper.UnderlayButton(
            requireContext(),
            R.drawable.trash,
            R.color.dark_orange,
            object : SwipeHelper.UnderlayButtonClickListener {
                override fun onClick() {
                    adapterCategory.deleteItemAtPosition(position)
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

        val category = categoryLiveData.value?.get(position)
        val adapterPriority = ArrayAdapter.createFromResource(requireContext(), R.array.category_priority, android.R.layout.simple_spinner_item)
        adapterPriority.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = adapterPriority

        spinnerType.setSelection(category?.priority ?: 0)
        etName.setText(category?.name)
        imageChoose.setImageDrawable(ContextCompat.getDrawable(requireContext(), requireContext().resources.getIdentifier(category?.path, "drawable", requireContext().packageName)))
        imageChoose.tag = category?.path

        imageChoose.setOnClickListener {
            IconsChooserAlertDialog(requireContext()){ path->
                imageChoose.setImageDrawable(ContextCompat.getDrawable(requireContext(), requireContext().resources.getIdentifier(path, "drawable", requireContext().packageName)))
                imageChoose.tag = path
            }
        }

        builder.setPositiveButton("Применить") {dialog, _ ->
            if(etName.text.toString()!=category?.name){
                table.child("Users").child(auth.currentUser!!.uid).child("Categories")
                    .child("${Calendar.getInstance().get(Calendar.YEAR)}/${Calendar.getInstance().get(Calendar.MONTH)+1}")
                    .child("ExpenseCategories").child(category!!.name).removeValue().addOnSuccessListener {
                        table.child("Users").child(auth.currentUser!!.uid).child("Categories")
                            .child("${Calendar.getInstance().get(Calendar.YEAR)}/${Calendar.getInstance().get(Calendar.MONTH)+1}")
                            .child("ExpenseCategories").child(etName.text.toString()).setValue(CategoryItem(etName.text.toString(),"0","0", spinnerType.selectedItemPosition,imageChoose.tag.toString()))
                    }

            } else {
                table.child("Users").child(auth.currentUser!!.uid).child("Categories")
                    .child("${Calendar.getInstance().get(Calendar.YEAR)}/${Calendar.getInstance().get(Calendar.MONTH)+1}")
                    .child("ExpenseCategories").child(etName.text.toString()).setValue(CategoryItem(etName.text.toString(),"0","0", spinnerType.selectedItemPosition,imageChoose.tag.toString()))
            }

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

    private fun updateBudget(){
        table.child("Users").child(auth.currentUser!!.uid).child("Budgets").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                    baseBudget.clear()
                    otherBudget.clear()
                    for (budget in snapshot.children){
                        budget.getValue(BudgetItem::class.java)?.let {
                            if(budget.key=="Base budget") {
                                baseBudget.add(it)
                            Log.e(com.example.mybudget.start_pages.Constants.TAG_USER, it.toString())}
                            else {
                                for(other in budget.children){
                                    other.getValue(BudgetItem::class.java)?.let { data ->
                                        otherBudget.add(data)
                                        Log.e(com.example.mybudget.start_pages.Constants.TAG_USER+"other", data.toString())
                                    }
                                }
                            }
                        }
                    }
                    val combinedList = mutableListOf<BudgetItem>()

                    combinedList.run {
                        add(baseBudget[0])
                        addAll(otherBudget)
                    }

                Log.e(com.example.mybudget.start_pages.Constants.TAG_USER, combinedList.toString())
                budgetLiveData.value = combinedList
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("errorLog", error.toException().toString())
            }
        })
    }

    private fun addExpense(){}

    private fun updateCategory(){
        table.child("Users").child(auth.currentUser!!.uid).child("Categories").child("${
            Calendar.getInstance().get(Calendar.YEAR)}/${Calendar.getInstance().get(Calendar.MONTH)+1}").child("ExpenseCategories").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                category.clear()
                for (expenseCategory in snapshot.children){
                    expenseCategory.getValue(CategoryItem::class.java)?.let {
                        category.add(it)
                    }
                }
                categoryLiveData.value = category.sortedByDescending { it.priority }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("errorLog", error.toException().toString())
            }

        })
    }

}

