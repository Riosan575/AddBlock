package bendev.addblock.core.ui

import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date

fun Int.formatted(): String = NumberFormat.getIntegerInstance().format(this)

fun Long.asDateTime(): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(this))

fun Long.asTime(): String = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(this))
