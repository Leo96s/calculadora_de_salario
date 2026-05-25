package com.example.ui

import java.util.UUID

data class DayWorkGroup(
    val id: String = UUID.randomUUID().toString(),
    val days: String,
    val hours: String
) {
    companion object {
        fun serialize(list: List<DayWorkGroup>): String {
            return list.filter { it.days.isNotBlank() && it.hours.isNotBlank() }
                .joinToString(";") { "${it.days.trim()},${it.hours.trim()}" }
        }

        fun deserialize(str: String?): List<DayWorkGroup> {
            if (str.isNullOrBlank()) return emptyList()
            return str.split(";").mapNotNull { item ->
                val parts = item.split(",")
                if (parts.size == 2) {
                    val daysVal = parts[0].trim()
                    val hoursVal = parts[1].trim()
                    if (daysVal.isNotEmpty() || hoursVal.isNotEmpty()) {
                        DayWorkGroup(days = daysVal, hours = hoursVal)
                    } else null
                } else null
            }
        }
    }
}
