package com.example.mybudget.drawersection.finance.category

data class _CategoryItem(var remainder: String="0", var total:String="",
                         var priority:Int=0, var isPlanned: Boolean = false)
data class CategoryItemWithKey(val key:String="", var categoryItem: _CategoryItem)

data class _CategoryBegin(var name:String="", var path:String="")
data class CategoryBeginWithKey(var key:String="", var categoryBegin:_CategoryBegin)
