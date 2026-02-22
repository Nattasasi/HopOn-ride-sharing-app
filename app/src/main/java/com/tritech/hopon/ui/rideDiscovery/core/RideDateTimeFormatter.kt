package com.tritech.hopon.ui.rideDiscovery.core

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object RideDateTimeFormatter {
    private val parserPatterns = listOf(
        "MMM dd, yyyy",
        "MMM dd",
        "yyyy-MM-dd",
        "MM/dd/yyyy",
        "dd/MM/yyyy"
    )

    fun splitMeetupDateTimeLabel(label: String): Pair<String, String> {
        val separatorIndex = label.lastIndexOf(',')
        if (separatorIndex <= 0 || separatorIndex >= label.lastIndex) {
            return label.trim() to ""
        }

        val datePart = label.substring(0, separatorIndex).trim()
        val timePart = label.substring(separatorIndex + 1).trim()
        return datePart to timePart
    }

    fun formatMeetupDateTimeLabel(dateLabel: String, timeLabel: String): String {
        val normalizedDate = dateLabel.trim()
        val normalizedTime = timeLabel.trim()
        return when {
            normalizedDate.isEmpty() -> normalizedTime
            normalizedTime.isEmpty() -> normalizedDate
            else -> "$normalizedDate, $normalizedTime"
        }
    }

    fun canonicalDateLabelForNow(now: Calendar = Calendar.getInstance()): String {
        return SimpleDateFormat("MMM dd, yyyy", Locale.US).format(now.time)
    }

    fun canonicalTimeLabelForNow(now: Calendar = Calendar.getInstance()): String {
        return SimpleDateFormat("HH:mm", Locale.US).format(now.time)
    }

    fun seedMeetupDateTimeLabel(
        dayOffsetFromToday: Int,
        time24h: String,
        now: Calendar = Calendar.getInstance()
    ): String {
        val seedDate = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, dayOffsetFromToday)
        }
        val dateLabel = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(seedDate.time)
        return "$dateLabel, ${time24h.trim()}"
    }

    fun formatDateLabelForDisplay(dateLabel: String, now: Calendar = Calendar.getInstance()): String {
        val parsedCalendar = parseDateLabelToCalendar(dateLabel, now) ?: return dateLabel.trim()
        if (isSameDay(parsedCalendar, now)) {
            return "Today"
        }

        val tomorrow = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        if (isSameDay(parsedCalendar, tomorrow)) {
            return "Tomorrow"
        }

        return SimpleDateFormat("MMM dd", Locale.US).format(parsedCalendar.time)
    }

    fun parseMeetupDateTimeToEpochMillis(label: String, now: Calendar = Calendar.getInstance()): Long? {
        val (datePart, timePart) = splitMeetupDateTimeLabel(label)
        if (timePart.isBlank()) {
            return null
        }
        return parseSubmissionMeetupToEpochMillis(datePart, timePart, now)
    }

    fun parseSubmissionMeetupToEpochMillis(
        dateLabel: String,
        timeLabel: String,
        now: Calendar = Calendar.getInstance()
    ): Long? {
        val time = parseTimeParts(timeLabel) ?: return null
        val baseCalendar = parseDateLabelToCalendar(dateLabel, now) ?: return null

        baseCalendar.set(Calendar.HOUR_OF_DAY, time.first)
        baseCalendar.set(Calendar.MINUTE, time.second)
        baseCalendar.set(Calendar.SECOND, 0)
        baseCalendar.set(Calendar.MILLISECOND, 0)
        return baseCalendar.timeInMillis
    }

    private fun parseDateLabelToCalendar(
        dateLabel: String,
        now: Calendar = Calendar.getInstance()
    ): Calendar? {
        val trimmed = dateLabel.trim()
        when {
            trimmed.isBlank() || trimmed.equals("Today", ignoreCase = true) -> {
                return now.clone() as Calendar
            }

            trimmed.equals("Tomorrow", ignoreCase = true) -> {
                return (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
            }
        }

        val currentYear = now.get(Calendar.YEAR)
        parserPatterns.forEach { pattern ->
            val parser = SimpleDateFormat(pattern, Locale.US)
            val parsedDate = runCatching {
                if (pattern == "MMM dd") parser.parse("$trimmed $currentYear") else parser.parse(trimmed)
            }.getOrNull() ?: return@forEach

            return Calendar.getInstance().apply { time = parsedDate }
        }

        return null
    }

    private fun parseTimeParts(timeLabel: String): Pair<Int, Int>? {
        val tokens = timeLabel.split(":")
        if (tokens.size != 2) return null

        val hour = tokens[0].trim().toIntOrNull() ?: return null
        val minute = tokens[1].trim().toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null

        return hour to minute
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }
}