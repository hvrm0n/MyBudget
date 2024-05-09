package com.example.mybudget.drawersection.loans

data class LoanItem(var name: String="", var amount:String ="0.00",
                    var dateOfEnd: String = "", var dateNext: String? = null,
                    var path: String = "", var currency:String ="",
                    var isDeleted: Boolean = false,
                    var isFinished: Boolean = false,
                    var period:String? = null)

data class LoanItemWithKey(var key:String="", var loanItem: LoanItem)
