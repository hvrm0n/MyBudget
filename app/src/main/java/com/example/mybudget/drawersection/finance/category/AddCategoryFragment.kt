package com.example.mybudget.drawersection.finance.category

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mybudget.R
import com.example.mybudget.start_pages.CategoryBegin
import com.google.android.material.button.MaterialButton
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar


class AddCategoryFragment : Fragment() {

    private lateinit var recyclerCategoryChooser:RecyclerView
    private lateinit var spinnerPriority:Spinner
    private lateinit var button: MaterialButton
    private lateinit var auth: FirebaseAuth
    private lateinit var table: DatabaseReference
    private val categoryBase = mutableListOf<CategoryBegin>()
    private val categoryBaseLiveData: MutableLiveData<List<CategoryBegin>> = MutableLiveData()
    private lateinit var categoriesAlready: Array<String>
    private var selectedIcon = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.card_default_category, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerCategoryChooser = view.findViewById(R.id.chooseCategory)
        spinnerPriority = view.findViewById(R.id.spinnerPrority)
        button = view.findViewById(R.id.chooseNewCategory)
    }

    override fun onStart() {
        super.onStart()
        auth = Firebase.auth
        table = Firebase.database.reference
        val adapterPriority = ArrayAdapter.createFromResource(requireContext(), R.array.category_priority, android.R.layout.simple_spinner_item)
        adapterPriority.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPriority.adapter = adapterPriority
        val categoriesJson = arguments?.getString("categories")
        categoriesAlready = Gson().fromJson(categoriesJson, Array<String>::class.java)

        val layoutManager = GridLayoutManager(context, 4)
        recyclerCategoryChooser.layoutManager = layoutManager

        val adapter = ChooseCategoryAdapter(requireContext(), categoryBase, this, table, auth, button)
        recyclerCategoryChooser.adapter = adapter

        categoryBaseLiveData.observe(viewLifecycleOwner){
            adapter.updateData(it)
        }

        updateBaseCategory()

        button.setOnClickListener {
            adapter.getChose()?.let{last->
                val name = last.findViewById<TextView>(R.id.nameCategory)?.text.toString()
                table.child("Users").child(auth.currentUser!!.uid).child("Categories").child("${Calendar.getInstance().get(Calendar.YEAR)}/${Calendar.getInstance().get(Calendar.MONTH)+1}")
                .child("ExpenseCategories").child(name).setValue(CategoryItem(name, "0", "0", spinnerPriority.selectedItemPosition, selectedIcon)).addOnSuccessListener {
                    view?.findNavController()?.popBackStack()
                }.addOnFailureListener { ex->
                    Log.e("SaveError", ex.message.toString())
                }
            }
        }
    }

    fun changeSelectedIcon(newIcon:String){
        Log.e("newIcon", newIcon)
        selectedIcon = newIcon
    }

    private fun updateBaseCategory(){
        table.child("Users").child(auth.currentUser!!.uid).child("Categories").child("Categories base").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                categoryBase.clear()
                lifecycleScope.launch(Dispatchers.IO){
                    for (category in snapshot.children){
                        category.getValue(CategoryBegin::class.java)?.let {categoryNew ->
                            if (categoriesAlready.all { it !=  categoryNew.name}) {
                                categoryBase.add(categoryNew)
                            }
                        }
                    }
                    withContext(Dispatchers.Main){
                        categoryBaseLiveData.value = categoryBase
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}