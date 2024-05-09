package com.example.mybudget.drawersection.finance

data class HistoryItem(var budgetId:String = "",
                       var placeId: String = "",
                       var isCategory:Boolean?=false,
                       var isGoal:Boolean?=false,
                       var isLoan:Boolean?=false,
                       var isSub:Boolean?=false,
                       var isTransfer:Boolean?=false,
                       var amount:String="",
                       var date:String = "",
                       var baseAmount:String = "",
                       var key:String = "")