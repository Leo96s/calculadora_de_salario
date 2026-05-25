package com.example.data

import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest

class AppRepository(private val appDao: AppDao) {

    // User Operations
    suspend fun getUserByUsername(username: String): User? {
        return appDao.getUserByUsername(username)
    }

    suspend fun getUserById(userId: Int): User? {
        return appDao.getUserById(userId)
    }

    suspend fun registerUser(username: String, passwordRaw: String, defaultHourlyRate: Double): Boolean {
        // Check if user already exists
        if (appDao.getUserByUsername(username) != null) {
            return false
        }
        val hashedPassword = hashPassword(passwordRaw)
        val newUser = User(
            username = username,
            passwordHash = hashedPassword,
            defaultHourlyRate = defaultHourlyRate
        )
        appDao.insertUser(newUser)
        return true
    }

    suspend fun authenticateUser(username: String, passwordRaw: String): User? {
        val user = appDao.getUserByUsername(username) ?: return null
        val hashedPassword = hashPassword(passwordRaw)
        return if (user.passwordHash == hashedPassword) user else null
    }

    suspend fun updateUserProfile(user: User) {
        appDao.updateUser(user)
    }

    // SalaryRecord Operations
    fun getSalaryRecordsForUser(userId: Int): Flow<List<SalaryRecord>> {
        return appDao.getSalaryRecordsForUser(userId)
    }

    suspend fun getSalaryRecordByMonth(userId: Int, monthYear: String): SalaryRecord? {
        return appDao.getSalaryRecordByMonth(userId, monthYear)
    }

    suspend fun saveSalaryRecord(record: SalaryRecord): Long {
        return appDao.insertSalaryRecord(record)
    }

    suspend fun deleteSalaryRecord(record: SalaryRecord) {
        appDao.deleteSalaryRecord(record)
    }

    suspend fun deleteSalaryRecordById(id: Int) {
        appDao.deleteSalaryRecordById(id)
    }

    // Hash Password helper using SHA-256
    private fun hashPassword(password: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            password // Fallback if hashing error occurs
        }
    }
}
