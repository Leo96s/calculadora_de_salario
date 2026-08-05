package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class User(
    @PrimaryKey
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val passwordHash: String = "",
    val hourlyRate: Double = 10.0
)
