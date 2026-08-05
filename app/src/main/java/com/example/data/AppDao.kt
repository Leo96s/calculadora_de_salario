package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // User related queries
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE uid = :userId LIMIT 1")
    suspend fun getUserById(userId: Int): User?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User): Long

    // @Upsert atualiza a linha existente em vez de a apagar e reinserir: um
    // REPLACE via @Insert apagaria a linha antiga e, por causa do
    // onDelete = CASCADE em SalaryRecord.userId, apagava também em catadupa
    // todos os registos salariais desse utilizador.
    @Upsert
    suspend fun upsertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    // SalaryRecord related queries
    @Query("SELECT * FROM salary_records WHERE userId = :userId ORDER BY monthYear DESC, savedAt DESC")
    fun getSalaryRecordsForUser(userId: String): Flow<List<SalaryRecord>>

    @Query("SELECT * FROM salary_records WHERE userId = :userId AND monthYear = :monthYear LIMIT 1")
    suspend fun getSalaryRecordByMonth(userId: String, monthYear: String): SalaryRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalaryRecord(record: SalaryRecord): Long

    @Delete
    suspend fun deleteSalaryRecord(record: SalaryRecord)

    @Query("DELETE FROM salary_records WHERE id = :id")
    suspend fun deleteSalaryRecordById(id: Int)
}
