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
        return try {
            // Cria utilizador no Firebase Auth
            val result = FirebaseManager.auth?.createUserWithEmailAndPassword(email, password)?.await()

            if (result?.user != null) {
                val userId = result.user!!.uid

                // Guarda os dados do utilizador no Firestore
                val userData = hashMapOf(
                    "uid" to userId,
                    "email" to email,
                    "username" to username,
                    "hourlyRate" to hourlyRate,
                    "createdAt" to System.currentTimeMillis()
                )

                FirebaseManager.firestore?.collection("users")
                    ?.document(userId)
                    ?.set(userData)
                    ?.await()

                _isUserLoggedIn.value = true
                _currentUserId.value = userId
                Log.d(TAG, "User registered successfully: $email")
                true
            } else {
                Log.e(TAG, "Registration failed")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Registration error", e)
            false
        }
    }

    suspend fun loginWithEmail(emailOrUsername: String, password: String): Boolean {
        return try {
            // Se for email, usa diretamente
            val emailToUse = if (emailOrUsername.contains("@")) {
                emailOrUsername
            } else {
                // Se for username, procura o email correspondente
                val user = FirebaseManager.firestore?.collection("users")
                    ?.whereEqualTo("username", emailOrUsername)
                    ?.get()
                    ?.await()
                    ?.documents
                    ?.firstOrNull()

                user?.getString("email") ?: emailOrUsername
            }

            // Faz login com o email
            val result = FirebaseManager.auth?.signInWithEmailAndPassword(emailToUse, password)?.await()

            if (result?.user != null) {
                _isUserLoggedIn.value = true
                _currentUserId.value = result.user!!.uid
                Log.d(TAG, "User logged in: $emailToUse")
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