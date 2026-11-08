package com.example.vent.util

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateUtils {

    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private val monthNamesGenitive = arrayOf(
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря"
    )

    /**
     * Форматирует ISO-строку времени (например, "2026-04-03T12:34:56.789000+00:00")
     * в локальное системное время пользователя формата "HH:mm" (например, "15:34").
     */
    fun formatTime(timestamp: String?): String {
        if (timestamp.isNullOrBlank()) return ""
        return try {
            val offsetDt = OffsetDateTime.parse(timestamp)
            offsetDt.atZoneSameInstant(ZoneId.systemDefault()).format(timeFormatter)
        } catch (_: Exception) {
            try {
                val instant = Instant.parse(timestamp)
                instant.atZone(ZoneId.systemDefault()).format(timeFormatter)
            } catch (_: Exception) {
                if (timestamp.contains("T")) {
                    timestamp.substringAfter("T").take(5)
                } else {
                    ""
                }
            }
        }
    }

    /**
     * Извлекает локальную дату (LocalDate) из строки ISO таймштампа в системном часовом поясе.
     */
    fun extractLocalDate(timestamp: String?): LocalDate? {
        if (timestamp.isNullOrBlank()) return null
        return try {
            OffsetDateTime.parse(timestamp).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate()
        } catch (_: Exception) {
            try {
                Instant.parse(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            } catch (_: Exception) {
                try {
                    if (timestamp.length >= 10) {
                        LocalDate.parse(timestamp.substring(0, 10))
                    } else {
                        null
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    /**
     * Форматирует дату для сепаратора сообщений:
     * - "Сегодня", если сообщение отправлено сегодня
     * - "Вчера", если сообщение отправлено вчера
     * - "15 октября", если сообщение отправлено в текущем году
     * - "15 октября 2025", если сообщение из прошлых лет
     */
    fun formatDateSeparator(date: LocalDate, today: LocalDate = LocalDate.now()): String {
        return when {
            date == today -> "Сегодня"
            date == today.minusDays(1) -> "Вчера"
            date.year == today.year -> {
                val monthName = monthNamesGenitive.getOrElse(date.monthValue - 1) { "" }
                "${date.dayOfMonth} $monthName"
            }
            else -> {
                val monthName = monthNamesGenitive.getOrElse(date.monthValue - 1) { "" }
                "${date.dayOfMonth} $monthName ${date.year}"
            }
        }
    }
}
