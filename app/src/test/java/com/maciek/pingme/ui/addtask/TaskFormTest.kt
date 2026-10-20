package com.maciek.pingme.ui.addtask

import com.maciek.pingme.domain.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek.TUESDAY
import java.time.LocalDate
import java.time.LocalTime

class TaskFormTest {

    private val today = LocalDate.of(2026, 10, 21)
    private val valid = TaskForm(name = "Suplementy", startDate = today)

    @Test
    fun validForm_hasNoErrors() {
        assertTrue(valid.validate(today).isEmpty())
    }

    @Test
    fun blankName_isError() {
        assertEquals(setOf(FormError.EMPTY_NAME), valid.copy(name = "   ").validate(today))
    }

    @Test
    fun deadlineNotAfterReminder_isError() {
        val sameTime = valid.copy(hasDeadline = true, reminderTime = LocalTime.of(9, 0), deadline = LocalTime.of(9, 0))
        val earlier = sameTime.copy(deadline = LocalTime.of(8, 30))
        assertEquals(setOf(FormError.DEADLINE_NOT_AFTER_REMINDER), sameTime.validate(today))
        assertEquals(setOf(FormError.DEADLINE_NOT_AFTER_REMINDER), earlier.validate(today))
    }

    @Test
    fun earlierDeadline_ignoredWhenDeadlineOff() {
        val form = valid.copy(hasDeadline = false, reminderTime = LocalTime.of(9, 0), deadline = LocalTime.of(8, 0))
        assertTrue(form.validate(today).isEmpty())
    }

    @Test
    fun customWithoutDays_isError() {
        assertEquals(setOf(FormError.NO_DAYS_SELECTED), valid.copy(scheduleOption = ScheduleOption.CUSTOM).validate(today))
    }

    @Test
    fun pastDate_isError() {
        assertEquals(setOf(FormError.DATE_IN_PAST), valid.copy(startDate = today.minusDays(1)).validate(today))
    }

    @Test
    fun toTask_mapsFields() {
        val task = valid.copy(
            name = "  Leki  ",
            scheduleOption = ScheduleOption.CUSTOM,
            customDays = setOf(TUESDAY),
            startDate = today.plusDays(3),
        ).toTask()
        assertEquals("Leki", task.name)
        assertNull(task.deadline)
        assertEquals(Schedule.CustomDays(setOf(TUESDAY)), task.schedule)
        assertEquals(today.plusDays(2), task.checkedUntil)
    }

    @Test
    fun toTask_keepsDeadlineWhenOn() {
        val task = valid.copy(hasDeadline = true, deadline = LocalTime.of(10, 0)).toTask()
        assertEquals(LocalTime.of(10, 0), task.deadline)
    }
}
