package com.maciek.pingme.ui

import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

val PolishLocale: Locale = Locale.forLanguageTag("pl")

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dayFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", PolishLocale)
private val dateFormatter = DateTimeFormatter.ofPattern("d.MM.yyyy")
// LLLL to nazwa miesiąca w mianowniku („październik”), a nie w dopełniaczu („października”).
private val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", PolishLocale)

/** 08:05 */
fun LocalTime.formatTime(): String = format(timeFormatter)

/** środa, 21 października */
fun LocalDate.formatDay(): String = format(dayFormatter)

/** 21.10.2026 */
fun LocalDate.formatDate(): String = format(dateFormatter)

/** Październik 2026 */
fun YearMonth.formatMonth(): String = format(monthFormatter).replaceFirstChar { it.titlecase(PolishLocale) }
