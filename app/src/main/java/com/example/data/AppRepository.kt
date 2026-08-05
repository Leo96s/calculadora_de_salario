package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val appDao: AppDao) {

    // User Operations
    suspend fun updateUserProfile(user: User) {
        appDao.updateUser(user)
    }

    suspend fun upsertUser(user: User) {
        appDao.upsertUser(user)
    }

    // SalaryRecord Operations
    fun getSalaryRecordsForUser(userId: String): Flow<List<SalaryRecord>> {
        return appDao.getSalaryRecordsForUser(userId)
    }

    suspend fun getSalaryRecordByMonth(userId: String, monthYear: String): SalaryRecord? {
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
}
