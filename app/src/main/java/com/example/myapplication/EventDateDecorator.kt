package com.example.myapplication

import android.graphics.Color
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.spans.DotSpan

class EventDateDecorator(private val eventDates: Set<CalendarDay>) : DayViewDecorator {

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return eventDates.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        // Cluster of small dots for events
        view.addSpan(DotSpan(6f, Color.parseColor("#00FF7F"))) // Green main dot
        view.addSpan(DotSpan(4f, Color.parseColor("#32CD32"))) // Smaller lighter green dot
    }
}
