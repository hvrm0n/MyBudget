package com.example.mybudget.drawersection

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.preference.MultiSelectListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import com.example.mybudget.BudgetNotificationManager
import com.example.mybudget.R
import com.example.mybudget.drawersection.finance.FinanceViewModel
import com.example.mybudget.drawersection.finance.SelectedBudgetViewModel
import com.example.mybudget.start_pages.Constants
import java.util.Calendar

class SettingsFragment : PreferenceFragmentCompat() {
    private lateinit var financeViewModel: FinanceViewModel
    private lateinit var selectedBudgetViewModel: SelectedBudgetViewModel
    private lateinit var notificationPrefs:SharedPreferences
    private lateinit var settingPrefs:SharedPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        notificationPrefs = requireContext().getSharedPreferences("NotificationPeriodAndTime", Context.MODE_PRIVATE)
        settingPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        findPreference<SwitchPreference>("notifications_enabled")?.isChecked = settingPrefs.getBoolean("notifications_enabled", NotificationManagerCompat.from(requireContext()).areNotificationsEnabled())
    }

    override fun onStart() {
        super.onStart()
        val multiSelectListPreference = findPreference<MultiSelectListPreference>("sumOfFinance")

        financeViewModel = ViewModelProvider(requireActivity())[FinanceViewModel::class.java]
        selectedBudgetViewModel = ViewModelProvider(requireActivity())[SelectedBudgetViewModel::class.java]

        financeViewModel.budgetLiveData.observe(viewLifecycleOwner) { selectedItems ->
            val entryValues = selectedItems.filter { !it.budgetItem.isDeleted }.map { it.budgetItem.name }.toTypedArray()
            multiSelectListPreference?.entries = entryValues
            multiSelectListPreference?.entryValues = selectedItems.filter { !it.budgetItem.isDeleted }.map { it.key } .toTypedArray()
        }

        findPreference<SwitchPreference>("notifications_enabled")?.setOnPreferenceChangeListener { _, newValue ->
            selectedBudgetViewModel.updateTextNotification(newValue as Boolean)
            for ((key, _) in notificationPrefs.all){
                BudgetNotificationManager.cancelAlarmManager(requireContext(), key.toString(), false)
            }
            if(newValue == true){
                restoreNotification()
            }
            true
        }

        findPreference<SwitchPreference>("transaction_enabled")?.setOnPreferenceChangeListener { _, newValue ->
            selectedBudgetViewModel.updateAutoTransactionFlag(newValue as Boolean)
            for ((key, _) in notificationPrefs.all){
                BudgetNotificationManager.cancelAutoTransaction(requireContext(), key.toString())
            }
            if(newValue == true){
                restoreTransaction()
            }
            true
        }

        findPreference<MultiSelectListPreference>("sumOfFinance")?.setOnPreferenceChangeListener { _, newValue ->
            selectedBudgetViewModel.updateSelectionData((newValue as? Set<String>)?.toList() ?: emptyList() )
            true
        }
    }

    private fun restoreNotification(){
        var channelID: String
        var placeId = ""
        var cancel = false
        val calendar = Calendar.getInstance()

        for ((key, value) in notificationPrefs.all){
            channelID = when{
                financeViewModel.planLiveData.value?.find { it.key == key } !=null -> {
                    financeViewModel.planLiveData.value?.find { it.key == key }?.let {
                        placeId = it.placeId
                        calendar.apply {
                            set(it.date.split(".")[2].toInt(),
                                it.date.split(".")[1].toInt()-1,
                                it.date.split(".")[0].toInt(),0,0,0)
                        }
                    }
                    Constants.CHANNEL_ID_PLAN
                }
                financeViewModel.goalsData.value?.find { it.key == key } !=null -> {
                    financeViewModel.goalsData.value?.find { it.key == key }?.let {
                        placeId = it.key
                        it.goalItem.date?.let { date->
                            calendar.apply {
                                set(date.split(".")[2].toInt(),
                                    date.split(".")[1].toInt()-1,
                                    date.split(".")[0].toInt(),0,0,0)
                            }
                        }
                        if (it.goalItem.isReached || it.goalItem.isDeleted) cancel = true
                    }
                    Constants.CHANNEL_ID_GOAL
                }
                financeViewModel.subLiveData.value?.find { it.key == key } !=null -> {
                    financeViewModel.subLiveData.value?.find { it.key == key }?.let {
                        placeId = it.key
                        it.subItem.date.let { date->
                            calendar.apply {
                                set(date.split(".")[2].toInt(),
                                    date.split(".")[1].toInt()-1,
                                    date.split(".")[0].toInt(),0,0,0)
                            }
                        }
                        if (it.subItem.isDeleted || it.subItem.isCancelled) cancel = true
                    }
                    Constants.CHANNEL_ID_SUB
                }
                financeViewModel.loansLiveData.value?.find { it.key == key } !=null -> {
                    financeViewModel.loansLiveData.value?.find { it.key == key }?.let {
                        placeId = it.key
                        if (!it.loanItem.dateNext.isNullOrEmpty()){
                            it.loanItem.dateNext?.let { date->
                                calendar.apply {
                                    set(date.split(".")[2].toInt(),
                                        date.split(".")[1].toInt()-1,
                                        date.split(".")[0].toInt(),0,0,0)
                                }
                            }
                        } else{
                            it.loanItem.dateOfEnd.let { date->
                                calendar.apply {
                                    set(date.split(".")[2].toInt(),
                                        date.split(".")[1].toInt()-1,
                                        date.split(".")[0].toInt(),0,0,0)
                                }
                            }
                        }
                        if (it.loanItem.isDeleted || it.loanItem.isFinished) cancel = true
                    }
                    Constants.CHANNEL_ID_LOAN
                }
                else->""
            }
            if (calendar.timeInMillis<Calendar.getInstance().timeInMillis) cancel = true
            if(channelID.isNotEmpty() && placeId.isNotEmpty() && !cancel) {
                BudgetNotificationManager.notification(
                    context = requireContext(),
                    channelID = channelID,
                    id = key.toString(),
                    placeId = placeId,
                    time = value.toString().split("|")[1],
                    dateOfExpence = calendar,
                    periodOfNotification = value.toString().split("|")[0]
                )
            }
            placeId = ""
            cancel = false
        }
    }

    private fun restoreTransaction(){
        val calendar = Calendar.getInstance()
        financeViewModel.planLiveData.value?.forEach {
            calendar.apply {
                set(it.date.split(".")[2].toInt(),
                    it.date.split(".")[1].toInt()-1,
                    it.date.split(".")[0].toInt(),0,0,0)
            }

            if (calendar.timeInMillis>=Calendar.getInstance().timeInMillis)
            {
                BudgetNotificationManager.setAutoTransaction(
                    context = requireContext(),
                    id = it.key,
                    placeId = it.placeId,
                    year = calendar.get(Calendar.YEAR),
                    month = calendar.get(Calendar.MONTH)+1,
                    dateOfExpence = calendar,
                    type =  Constants.CHANNEL_ID_PLAN)
            }
        }

        financeViewModel.subLiveData.value?.forEach{
            it.subItem.date.let { date->
                calendar.apply {
                    set(date.split(".")[2].toInt(),
                        date.split(".")[1].toInt()-1,
                        date.split(".")[0].toInt(),0,0,0)
                }
            }
            if (!it.subItem.isDeleted && !it.subItem.isCancelled
                && calendar.timeInMillis>=Calendar.getInstance().timeInMillis
                ) {
                if (!financeViewModel.budgetLiveData.value?.find {budget-> it.subItem.budgetId == budget.key }?.key.isNullOrEmpty()){
                        BudgetNotificationManager.setAutoTransaction(
                            context = requireContext(),
                            id = it.key,
                            placeId = it.key,
                            year = calendar.get(Calendar.YEAR),
                            month = calendar.get(Calendar.MONTH)+1,
                            dateOfExpence = calendar,
                            type = Constants.CHANNEL_ID_SUB)
                }
            }
        }
    }
}