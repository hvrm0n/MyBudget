package com.example.mybudget.drawersection.finance

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.mybudget.R
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

class BudgetEditDialogFragment:DialogFragment() {

    private var selection: Triple<String, String, String>? = Triple("","", "")
    private lateinit var auth: FirebaseAuth
    private lateinit var table: DatabaseReference
    private lateinit var currency: TextView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.card_add_budget, null)
        val etName: EditText = dialogView.findViewById(R.id.nameBudgetNew)
        val tvName: TextView = dialogView.findViewById(R.id.titleAddBudget)
        val etAmount: EditText = dialogView.findViewById(R.id.amountNew)
        val tvAmount: TextView = dialogView.findViewById(R.id.amountAddBudget)
        val spinnerType: Spinner = dialogView.findViewById(R.id.typeNewBudget)
        currency = dialogView.findViewById(R.id.currencyNewBudget)

        val checkBox: CheckBox = dialogView.findViewById(R.id.checkBoxBasic)
        auth = Firebase.auth
        table = Firebase.database.reference
        builder.setView(dialogView)

        val adapterType = ArrayAdapter.createFromResource(requireContext(), R.array.budget_types, android.R.layout.simple_spinner_item)
        adapterType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = adapterType

        val sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]
        sharedViewModel.dataToPass.observe(this, Observer { data ->
            if (data.first.isNotEmpty()){
                currency.text = data.third
                selection = data
                sharedViewModel.dataToPass.value = Triple("","","")
            }
        })

        if(arguments?.getString("basicCurrency") == null) {
            val name = requireArguments().getString("name")
            val amount = requireArguments().getString("amount")
            val type = requireArguments().getString("type")
            val base = requireArguments().getBoolean("base")
            val currencySymbol = requireArguments().getString("symbol")
            val currencyShort = requireArguments().getString("currency")

            tvName.text = "Название"
            tvAmount.text = "Накопления"
            currency.text = currencySymbol
            checkBox.isChecked = base
            etName.setText(name)
            etAmount.setText(amount)

            currency.setOnClickListener { findNavController().navigate(R.id.action_budgetEditDialogFragment_to_currencyDialogFragment) }

            spinnerType.setSelection(adapterType.getPosition(type))

            builder.setPositiveButton("Сохранить") { dialog, _ ->
                if (etName.text.isNotEmpty() && etAmount.text.isNotEmpty()){
                    if (!base){
                        if (etName.text.toString() != name){
                            if(!checkBox.isChecked){
                                table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Other budget").child(name!!).removeValue().addOnCompleteListener {
                                    table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Other budget").child(etName.text.toString())
                                        .setValue(BudgetItem(etName.text.toString(), etAmount.text.toString(),  spinnerType.selectedItem.toString(), 0, selection?.first?.takeIf { it.isNotEmpty() } ?: currencyShort!!))
                                        //ПОТОМ КОГДА БУДЕТ ИНФОРМАЦИЯ ОБ ОПЕРАЦИЯХ НУЖНО ОТТУДА ПЕРЕЗАПИСЫВАТЬ
                                        // table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Other budget").child(etName.text.toString()).setValue(snapshot.value)
                                }
                            } else{
                                    table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Other budget").child(name!!).removeValue().addOnCompleteListener {
                                        table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Base budget")
                                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                                override fun onDataChange(snapshot: DataSnapshot) {
                                                    val baseOld = snapshot.getValue(BudgetItem::class.java)
                                                    if (baseOld!=null){
                                                        table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Base budget").setValue(
                                                            BudgetItem(etName.text.toString(), etAmount.text.toString(),  spinnerType.selectedItem.toString(), 0, selection?.first?.takeIf { it.isNotEmpty() } ?: currencyShort!!))
                                                            .addOnCompleteListener {
                                                                table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Other budget").child(baseOld.name).setValue(baseOld).addOnCompleteListener {
                                                                    findNavController().popBackStack()
                                                                    dialog.dismiss()
                                                                }
                                                            }
                                                    }
                                                }
                                                override fun onCancelled(error: DatabaseError) {}
                                            })
                                    }
                            }
                        } else{
                            if (!checkBox.isChecked){
                                table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Other budget").child(etName.text.toString()).setValue(
                                    BudgetItem(etName.text.toString(), etAmount.text.toString(),  spinnerType.selectedItem.toString(), 0, selection?.first?.takeIf { it.isNotEmpty() } ?: currencyShort!!)
                                ).addOnCompleteListener {
                                    dialog.dismiss()
                                }
                            } else{
                                lifecycleScope.launch(Dispatchers.IO){
                                    table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Other budget").child(name).removeValue().addOnCompleteListener {
                                        table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Base budget")
                                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                                override fun onDataChange(snapshot: DataSnapshot) {
                                                    val baseOld = snapshot.getValue(BudgetItem::class.java)
                                                    if (baseOld!=null){
                                                        table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Base budget").setValue(
                                                            BudgetItem(etName.text.toString(), etAmount.text.toString(),  spinnerType.selectedItem.toString(), 0, selection?.first?.takeIf { it.isNotEmpty() } ?: currencyShort!!))
                                                            .addOnCompleteListener {
                                                                table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Other budget").child(baseOld.name).setValue(baseOld).addOnCompleteListener {
                                                                    dialog.dismiss()
                                                                }
                                                            }
                                                    }
                                                }
                                                override fun onCancelled(error: DatabaseError) {
                                                    Log.e("EROORBASE", error.message)
                                                }

                                            })
                                    }
                                }
                            }
                        }
                    }
                    else{
                        table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Base budget")
                            .setValue(BudgetItem(etName.text.toString(), etAmount.text.toString(),  spinnerType.selectedItem.toString(), 0, selection?.first?.takeIf { it.isNotEmpty() } ?: currencyShort!!)
                            ).addOnCompleteListener {
                                dialog.dismiss()
                            }
                    }
                } else Toast.makeText(context, "Вы заполнили не все данные", Toast.LENGTH_LONG).show()
            }

            builder.setNegativeButton("Удалить") { dialog, _ ->
                if (base) Toast.makeText(context, "Нельзя удалить основной бюджет!", Toast.LENGTH_LONG).show()
                else {
                    if (name == etName.text.toString() && etName.text.isNotEmpty() && etAmount.text.isNotEmpty()){
                        table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Other budget").child(name).removeValue().addOnCompleteListener {
                            dialog.dismiss()
                        }
                    } else when{
                        name != etName.text.toString() -> Toast.makeText(context, "Вы собираетесь удалить отредактированный бюджет", Toast.LENGTH_LONG).show()
                        etName.text.isEmpty() || etAmount.text.isEmpty() -> Toast.makeText(context, "Вы ввели не все данные", Toast.LENGTH_LONG).show()}
                }
            }

            builder.setNeutralButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
        }
        else {
            val currencyShort = requireArguments().getString("currency")
            val basicCurrency = requireArguments().getString("basicCurrency")
            val budgetsJson = requireArguments().getString("budgets")
            val budgets = Gson().fromJson(budgetsJson, Array<BudgetItem>::class.java).asList()

            currency.text = basicCurrency

            currency.setOnClickListener {
                findNavController().navigate(R.id.action_budgetEditDialogFragment_to_currencyDialogFragment)
            }
            builder.setPositiveButton("Добавить") { dialog, _ ->
                if (etName.text.isNotEmpty() && etAmount.text.isNotEmpty() && budgets.none {it.name == etName.text.toString()}){
                    if (!checkBox.isChecked){
                        table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Other budget").child(etName.text.toString()).setValue(
                        BudgetItem(etName.text.toString(), etAmount.text.toString(),  spinnerType.selectedItem.toString(), 0, selection?.first?.takeIf { it.isNotEmpty() } ?: currencyShort!!)).
                    addOnCompleteListener {
                        dialog.dismiss()}
                    }
                    else{
                        lifecycleScope.launch(Dispatchers.IO){
                            table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Base budget")
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        val baseOld = snapshot.getValue(BudgetItem::class.java)
                                        if (baseOld!=null){
                                            table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Base budget").setValue(
                                                BudgetItem(etName.text.toString(), etAmount.text.toString(),  spinnerType.selectedItem.toString(), 0, selection?.first?.takeIf { it.isNotEmpty() } ?: currencyShort!!))
                                                .addOnCompleteListener {
                                                    table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Other budget").child(baseOld.name).setValue(baseOld).addOnCompleteListener {
                                                        dialog.dismiss()}
                                                }
                                        }
                                    }
                                    override fun onCancelled(error: DatabaseError) {}

                                })
                        }
                    }
                } else when{
                    !budgets.none {it.name == etName.text.toString()}->Toast.makeText(context, "Счет с таким названием уже существует!", Toast.LENGTH_LONG).show()
                    etName.text.isEmpty() || etAmount.text.isEmpty() -> Toast.makeText(context, "Вы ввели не все данные", Toast.LENGTH_LONG).show()}

            }
            builder.setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
        }
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.listview_shadow)
        dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        return dialog

    }
}

