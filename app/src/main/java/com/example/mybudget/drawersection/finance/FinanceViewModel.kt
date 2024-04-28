package com.example.mybudget.drawersection.finance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mybudget.drawersection.finance.budget.BudgetItemWithKey
import com.example.mybudget.drawersection.finance.category.CategoryItemWithKey
import com.example.mybudget.drawersection.goals.GoalItemWithKey
import com.example.mybudget.start_pages.CategoryBeginWithKey

class FinanceViewModel:ViewModel() {
    private val _budgetLiveData = MutableLiveData<List<BudgetItemWithKey>>()
    val budgetLiveData: LiveData<List<BudgetItemWithKey>> get() = _budgetLiveData

    private val _categoryLiveData = MutableLiveData<List<CategoryItemWithKey>>()
    val categoryLiveData: LiveData<List<CategoryItemWithKey>> get() = _categoryLiveData

    private val _categoryBeginLiveData = MutableLiveData<List<CategoryBeginWithKey>>()
    val categoryBeginLiveData: LiveData<List<CategoryBeginWithKey>> get() = _categoryBeginLiveData

    private val _historyLiveData = MutableLiveData<List<HistoryItem>>()
    val historyLiveData: LiveData<List<HistoryItem>> get() = _historyLiveData

    private val _planLiveData = MutableLiveData<List<HistoryItem>>()
    val planLiveData: LiveData<List<HistoryItem>> get() = _planLiveData

    private val _financeDate = MutableLiveData<Pair<Int,Int>>()
    val financeDate: LiveData<Pair<Int,Int>> get() = _financeDate


    private val _goalsLiveData = MutableLiveData<List<GoalItemWithKey>>()
    val goalsData: LiveData<List<GoalItemWithKey>> get() = _goalsLiveData

    fun updateBudgetData(newData: List<BudgetItemWithKey>) {
        _budgetLiveData.value = newData
    }

    fun updateCategoryData(newData: List<CategoryItemWithKey>) {
        _categoryLiveData.value = newData
    }

    fun updateHistoryData(newData: List<HistoryItem>) {
        _historyLiveData.value = newData
    }

    fun updatePlanData(newData: List<HistoryItem>) {
        _planLiveData.value = newData
    }

    fun updateCategoryBeginData(newData: List<CategoryBeginWithKey>) {
        _categoryBeginLiveData.value = newData
    }

    fun updateDate(newData: Pair<Int, Int>) {
        _financeDate.value = newData
    }

    fun updateGoalsData(newData: List<GoalItemWithKey>) {
        _goalsLiveData.value = newData
    }
}