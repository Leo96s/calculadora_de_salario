package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    private const val TAG = "FirebaseManager"

    // Firebase lê automaticamente do google-services.json
    val auth get() = Firebase.auth
    val firestore get() = Firebase.firestore

    fun initialize(context: Context) {
        Log.d(TAG, "Firebase initialized successfully from google-services.json")
    }
    suspend fun saveRecordToCloud(userIdStr: String, record: SalaryRecord): Boolean {
        return try {
            val recordMap = hashMapOf(
                "id" to record.id,
                "userIdStr" to userIdStr,
                "monthYear" to record.monthYear,
                "hourlyRate" to record.hourlyRate,
                "hoursPerDay" to record.hoursPerDay,
                "daysOffPerWeek" to record.daysOffPerWeek,
                "sundaysWorked" to record.sundaysWorked,
                "holidaysWorked" to record.holidaysWorked,
                "days8h" to record.days8h,
                "days4h" to record.days4h,
                "sundays8h" to record.sundays8h,
                "sundays4h" to record.sundays4h,
                "holidays8h" to record.holidays8h,
                "holidays4h" to record.holidays4h,
                "regularHours" to record.regularHours,
                "sundayHours" to record.sundayHours,
                "holidayHours" to record.holidayHours,
                "regularEarnings" to record.regularEarnings,
                "sundayEarnings" to record.sundayEarnings,
                "holidayEarnings" to record.holidayEarnings,
                "totalEarnings" to record.totalEarnings,
                "normalDaysJson" to record.normalDaysJson,
                "sundaysJson" to record.sundaysJson,
                "holidaysJson" to record.holidaysJson,
                "notes" to record.notes,
                "savedAt" to record.savedAt
            )
            val docId = "${userIdStr}_${record.monthYear}"
            firestore.collection("users")
                .document(userIdStr)
                .collection("salary_records")
                .document(docId)
                .set(recordMap)
                .await()
            Log.d(TAG, "Record saved successfully: $docId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save record", e)
            false
        }
    }

    suspend fun fetchRecordsFromCloud(userIdStr: String): List<Map<String, Any>> {
        return try {
            val snapshot = firestore.collection("users")
                .document(userIdStr)
                .collection("salary_records")
                .get()
                .await()
            snapshot.documents.mapNotNull { it.data }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch records", e)
            emptyList()
        }
    }

    suspend fun deleteRecordFromCloud(userIdStr: String, monthYear: String): Boolean {
        return try {
            val docId = "${userIdStr}_$monthYear"
            firestore.collection("users")
                .document(userIdStr)
                .collection("salary_records")
                .document(docId)
                .delete()
                .await()
            Log.d(TAG, "Record deleted: $docId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete record", e)
            false
        }
    }
}