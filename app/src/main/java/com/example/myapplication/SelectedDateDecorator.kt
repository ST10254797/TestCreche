package com.example.myapplication

import android.graphics.Color
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.spans.DotSpan

class SelectedDateDecorator(private val date: CalendarDay) : DayViewDecorator {

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return day == date
    }

    override fun decorate(view: DayViewFacade) {
        // Large purple dot with a smaller white inner dot for depth
        view.addSpan(DotSpan(14f, Color.parseColor("#7E3FF2"))) // Purple outer dot
        view.addSpan(DotSpan(6f, Color.WHITE)) // Inner white dot
    }
}
