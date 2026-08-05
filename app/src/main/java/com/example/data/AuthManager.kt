package com.example.data

import android.util.Log
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

object AuthManager {
    private const val TAG = "AuthManager"

    private val _isUserLoggedIn = MutableStateFlow(false)
    val isUserLoggedIn: Flow<Boolean> = _isUserLoggedIn.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: Flow<String?> = _currentUserId.asStateFlow()

    init {
        // Verifica se há utilizador já autenticado
        val currentUser = FirebaseManager.auth?.currentUser
        if (currentUser != null) {
            _isUserLoggedIn.value = true
            _currentUserId.value = currentUser.uid
            Log.d(TAG, "User already logged in: ${currentUser.email}")
        }
    }

    suspend fun signInWithGoogle(idToken: String): Boolean {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = FirebaseManager.auth?.signInWithCredential(credential)?.await()

            if (result?.user != null) {
                _isUserLoggedIn.value = true
                _currentUserId.value = result.user!!.uid
                Log.d(TAG, "Google sign-in successful: ${result.user?.email}")
                true
            } else {
                Log.e(TAG, "Sign-in result is null")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in failed", e)
            _isUserLoggedIn.value = false
            false
        }
    }

    suspend fun signOut(): Boolean {
        return try {
            FirebaseManager.auth?.signOut()
            _isUserLoggedIn.value = false
            _currentUserId.value = null
            Log.d(TAG, "User signed out")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Sign-out failed", e)
            false
        }
    }

    fun isUserAuthenticated(): Boolean {
        return FirebaseManager.auth?.currentUser != null
    }

    fun getCurrentUserEmail(): String? {
        return FirebaseManager.auth?.currentUser?.email
    }

    fun getCurrentUserUid(): String? {
        return FirebaseManager.auth?.currentUser?.uid
    }

    suspend fun getCurrentUserData(): User? {
        return try {
            val uid = FirebaseManager.auth?.currentUser?.uid ?: return null

            val document = FirebaseManager.firestore
                ?.collection("users")
                ?.document(uid)
                ?.get()
                ?.await()

            document?.toObject(User::class.java)

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user data", e)
            null
        }
    }

    suspend fun registerWithEmail(email: String, username: String, password: String, hourlyRate: Double): Boolean {
        // Cria utilizador no Firebase Auth
        val user = try {
            FirebaseManager.auth?.createUserWithEmailAndPassword(email, password)?.await()?.user
        } catch (e: Exception) {
            Log.e(TAG, "Registration error", e)
            null
        }

        if (user == null) {
            Log.e(TAG, "Registration failed")
            return false
        }

        // Guarda os dados do utilizador no Firestore
        val userData = hashMapOf(
            "uid" to user.uid,
            "email" to email,
            "username" to username,
            "hourlyRate" to hourlyRate,
            "createdAt" to System.currentTimeMillis()
        )

        return try {
            FirebaseManager.firestore?.collection("users")
                ?.document(user.uid)
                ?.set(userData)
                ?.await()

            _isUserLoggedIn.value = true
            _currentUserId.value = user.uid
            Log.d(TAG, "User registered successfully: $email")
            true
        } catch (e: Exception) {
            // Sem isto, a conta Auth ficava órfã (sem perfil), e o próximo
            // registo com o mesmo email falhava com "email already in use".
            Log.e(TAG, "Failed to save profile, rolling back auth account", e)
            try {
                user.delete().await()
            } catch (deleteError: Exception) {
                Log.e(TAG, "Failed to rollback auth account after profile write failure", deleteError)
            }
            false
        }
    }

    suspend fun updateHourlyRate(uid: String, hourlyRate: Double): Boolean {
        return try {
            FirebaseManager.firestore?.collection("users")
                ?.document(uid)
                ?.update("hourlyRate", hourlyRate)
                ?.await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update hourly rate", e)
            false
        }
    }

    suspend fun loginWithEmail(email: String, password: String): Boolean {
        return try {
            val result = FirebaseManager.auth?.signInWithEmailAndPassword(email, password)?.await()

            if (result?.user != null) {
                _isUserLoggedIn.value = true
                _currentUserId.value = result.user!!.uid
                Log.d(TAG, "User logged in: $email")
                true
            } else {
                Log.e(TAG, "Login failed")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login error: ${e.message}", e)
            false
        }
    }
}