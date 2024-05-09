package com.example.mybudget.drawersection.finance.budget

data class _BudgetItem(var name: String="", var amount: String="",
                       var type:String="", var count:Int=0, var currency: String="RUB",
                       var isDeleted: Boolean = false)
data class BudgetItemWithKey(var key:String="", var budgetItem: _BudgetItem)
