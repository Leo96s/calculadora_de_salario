package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // User related queries
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Int): User?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    // SalaryRecord related queries
    @Query("SELECT * FROM salary_records WHERE userId = :userId ORDER BY monthYear DESC, savedAt DESC")
    fun getSalaryRecordsForUser(userId: Int): Flow<List<SalaryRecord>>

    @Query("SELECT * FROM salary_records WHERE userId = :userId AND monthYear = :monthYear LIMIT 1")
    suspend fun getSalaryRecordByMonth(userId: Int, monthYear: String): SalaryRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalaryRecord(record: SalaryRecord): Long

    @Delete
    suspend fun deleteSalaryRecord(record: SalaryRecord)

    @Query("DELETE FROM salary_records WHERE id = :id")
    suspend fun deleteSalaryRecordById(id: Int)
}
