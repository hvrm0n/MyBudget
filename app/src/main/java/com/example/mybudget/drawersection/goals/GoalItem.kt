package com.example.mybudget.drawersection.goals

data class GoalItem(var name: String="", var current:String ="0.00",
                    var target: String="0.00", var currency: String="RUB",
                    var date: String? = null, var path: String = "",
                    var isDeleted: Boolean = false, var isReached: Boolean = false)
data class GoalItemWithKey(var key:String="", var goalItem: GoalItem)