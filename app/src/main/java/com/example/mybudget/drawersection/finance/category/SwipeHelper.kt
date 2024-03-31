package com.example.mybudget.drawersection.finance.category

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.mybudget.R
import java.util.*
import kotlin.math.abs
import kotlin.math.max

abstract class SwipeHelper(
    private val recyclerView: RecyclerView
) : ItemTouchHelper.SimpleCallback(
    ItemTouchHelper.ACTION_STATE_IDLE,
    ItemTouchHelper.LEFT
) {
    private var swipedPosition = -1
    private val buttonsBuffer: MutableMap<Int, List<UnderlayButton>> = mutableMapOf()
    private val recoverQueue = object : LinkedList<Int>() {
        override fun add(element: Int): Boolean {
            if (contains(element)) return false
            return super.add(element)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private val touchListener = View.OnTouchListener { _, event ->
        if (swipedPosition < 0) return@OnTouchListener false
        buttonsBuffer[swipedPosition]?.forEach { it.handle(event) }
        recoverQueue.add(swipedPosition)
        swipedPosition = -1
        recoverSwipedItem()
        true
    }

    init {
        recyclerView.setOnTouchListener(touchListener)
    }

    private fun recoverSwipedItem() {
        while (!recoverQueue.isEmpty()) {
            val position = recoverQueue.poll() ?: return
            recyclerView.adapter?.notifyItemChanged(position)
        }
    }

    private fun drawButtons(
        canvas: Canvas,
        buttons: List<UnderlayButton>,
        itemView: View,
        dX: Float
    ) {
        var right = itemView.right
        buttons.forEach { button ->
            val width = button.iconWidthPx / buttons.intrinsicWidth() * abs(dX)
            val left = right - width
            val rect = RectF(left, itemView.top.toFloat(), right.toFloat(), itemView.bottom.toFloat())
            //val shadowRect = RectF(left + 10, itemView.top.toFloat() + 10, right.toFloat() + 10, itemView.bottom.toFloat() + 10)
            val cornerRadius = 30f
            val paint = Paint().apply {
                color = recyclerView.context.getColor(R.color.dark_green)
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
            button.draw(canvas, rect)

            right = left.toInt()
        }
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val position = viewHolder.adapterPosition
        var maxDX = dX
        val itemView = viewHolder.itemView

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            if (dX < 0) {
                if (!buttonsBuffer.containsKey(position)) {
                    buttonsBuffer[position] = instantiateUnderlayButton(position)
                }

                val buttons = buttonsBuffer[position] ?: return
                if (buttons.isEmpty()) return
                maxDX = max(-buttons.intrinsicWidth()-80, dX)
                drawButtons(c, buttons, itemView, maxDX)
            }
        }

        super.onChildDraw(
            c,
            recyclerView,
            viewHolder,
            maxDX,
            dY,
            actionState,
            isCurrentlyActive
        )
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        if (swipedPosition != position) recoverQueue.add(swipedPosition)
        swipedPosition = position
        recoverSwipedItem()
    }

    abstract fun instantiateUnderlayButton(position: Int): List<UnderlayButton>

    interface UnderlayButtonClickListener {
        fun onClick()
    }

    override fun getSwipeDirs(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val itemCount = recyclerView.adapter?.itemCount ?: 0
        val position = viewHolder.adapterPosition
        return if (position == itemCount-1) {
            0
        } else {
            super.getSwipeDirs(recyclerView, viewHolder)
        }
    }
    class UnderlayButton(
        private val context: Context,
        private val iconResId: Int,
        @ColorRes private val colorRes: Int,
        private val clickListener: UnderlayButtonClickListener
    ) {
        private var icon: Bitmap? = null
        private var clickableRegion: RectF? = null
        var iconWidthPx: Float = 0f
        private var iconHeightPx: Float = 0f

        init {
            icon = BitmapFactory.decodeResource(context.resources, iconResId)
            val displayMetrics = context.resources.displayMetrics
            val widthInPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30f, displayMetrics)
            val heightInPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30f, displayMetrics)

            icon = Bitmap.createScaledBitmap(icon!!, widthInPx.toInt(), heightInPx.toInt(), true)
            iconWidthPx = widthInPx
            iconHeightPx = heightInPx
        }

        fun draw(canvas: Canvas, rect: RectF) {
            val cornerRadius = 30f
            val paint = Paint().apply {
                color = ContextCompat.getColor(context, R.color.light)
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRect(rect, paint)

            paint.colorFilter = PorterDuffColorFilter(ContextCompat.getColor(context, colorRes), PorterDuff.Mode.SRC_ATOP)

            val left = rect.left + (rect.width() - iconWidthPx) / 2
            val top = rect.top + (rect.height() - iconHeightPx) / 2

            icon?.let {
                canvas.drawBitmap(it, left, top, paint)
            }

            clickableRegion = rect
        }

        fun handle(event: MotionEvent) {
            clickableRegion?.let {
                if (it.contains(event.x, event.y)) {
                    clickListener.onClick()
                }
            }
        }
    }
}

private fun List<SwipeHelper.UnderlayButton>.intrinsicWidth(): Float {
    if (isEmpty()) return 0.0f
    return map { it.iconWidthPx }.sum()
}