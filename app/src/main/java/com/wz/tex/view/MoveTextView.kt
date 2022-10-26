package com.wz.tex.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.abs

class MoveTextView(
    context: Context,
    attrs: AttributeSet? = null,
    def: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, def) {

    private var x = 0
    private var y = 0
    private var firstX = 0
    private var firstY = 0
    private var lp: ConstraintLayout.LayoutParams? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (layoutParams is ConstraintLayout.LayoutParams) {
            lp = layoutParams as ConstraintLayout.LayoutParams
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (lp == null) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                x = event.rawX.toInt()
                y = event.rawY.toInt()
                firstX = event.rawX.toInt()
                firstY = event.rawY.toInt()
            }
            MotionEvent.ACTION_MOVE -> {
                val nowx = event.rawX.toInt()
                val nowy = event.rawY.toInt()
                val movedx = nowx - x
                val movedy = nowy - y
                x = nowx
                y = nowy

                lp!!.marginStart = lp!!.marginStart - movedx
                lp!!.topMargin = lp!!.topMargin - movedy
                // 更新悬浮窗控件布局
            }
            MotionEvent.ACTION_UP -> {
                val minSlop = 3
                if (abs(event.rawX.toInt() - firstX)  < minSlop && abs(event.rawY.toInt() - firstY) < minSlop){
                    performClick()
                }
            }
        }
        return false
    }
}