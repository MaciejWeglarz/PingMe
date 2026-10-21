package com.maciek.pingme.domain

import java.time.LocalTime

enum class Mood { HAPPY, NEUTRAL, SAD }

/**
 * Nastrój maskotki na ekranie „Dziś”.
 * Komplet zrobionych zadań wygrywa: nawet po wczorajszym pominięciu pingwinek się cieszy.
 * Smutek: wczoraj coś pominięto albo dziś minął deadline nieodhaczonego zadania.
 */
fun dayMood(today: List<DayTask>, skippedYesterday: Boolean, now: LocalTime): Mood {
    if (today.isNotEmpty() && today.all { it.done }) return Mood.HAPPY
    val deadlineMissed = today.any { item -> !item.done && item.task.deadline?.let { !now.isBefore(it) } == true }
    return if (skippedYesterday || deadlineMissed) Mood.SAD else Mood.NEUTRAL
}
