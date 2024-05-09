package com.example.mybudget.drawersection.subs

data class SubItem(var name: String="", var amount:String ="0.00", var date: String = "",
                   var path: String = "", var isDeleted: Boolean = false, val budgetId:String="",
                   var isCancelled: Boolean = false, var period:String = "")
data class SubItemWithKey(var key:String="", var subItem: SubItem)
