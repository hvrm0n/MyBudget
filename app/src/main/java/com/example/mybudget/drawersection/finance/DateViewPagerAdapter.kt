package com.example.mybudget.drawersection.finance

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.doOnPreDraw
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.mybudget.R
import com.example.mybudget.drawersection.finance.category.CategoryAdapter
import java.util.Calendar

class DateViewPagerAdapter(val context: Context) : RecyclerView.Adapter<DateViewPagerAdapter.ViewHolder>() {

    private var dataList: List<Pair<Int, Int>> = listOf()

    fun setData(data: List<Pair<Int, Int>> = listOf(Pair(Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.YEAR))), pos:Pair<Int, Int>, viewPager2: ViewPager2, categoryAdapter: CategoryAdapter) {
        dataList = data
        notifyDataSetChanged()
        viewPager2.post {
            viewPager2.setCurrentItem(dataList.indexOf(pos), true)
            categoryAdapter.updateCurrentDate(viewPager2.currentItem)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        view.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        view.findViewById<TextView>(android.R.id.text1).gravity = Gravity.CENTER
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataList[position])
    }

    fun getDate(i:Int):Pair<Int, Int> {
        return dataList[i]
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(android.R.id.text1)
        fun bind(date: Pair<Int, Int>) {
            textView.text = "${context.resources.getStringArray(R.array.months)[date.first-1]} ${date.second}"
            Log.e("CheckList2",textView.text.toString())
        }
    }
}