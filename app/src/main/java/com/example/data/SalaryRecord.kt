package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "salary_records",
    indices = [Index(value = ["userId"])],
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["uid"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SalaryRecord(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val userId: String,

    val monthYear: String,
    val hourlyRate: Double,
    val hoursPerDay: Double,
    val daysOffPerWeek: Double,
    val sundaysWorked: Int,
    val holidaysWorked: Int,

    val days8h: Int = 0,
    val days4h: Int = 0,
    val sundays8h: Int = 0,
    val sundays4h: Int = 0,
    val holidays8h: Int = 0,
    val holidays4h: Int = 0,

    val regularHours: Double,
    val sundayHours: Double,
    val holidayHours: Double,

    val regularEarnings: Double,
    val sundayEarnings: Double,
    val holidayEarnings: Double,
    val totalEarnings: Double,

    val normalDaysJson: String = "",
    val sundaysJson: String = "",
    val holidaysJson: String = "",

    val notes: String = "",

    val savedAt: Long = System.currentTimeMillis(),

    val isSynced: Boolean = false
)