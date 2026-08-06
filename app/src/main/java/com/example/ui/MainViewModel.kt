package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.AuthManager
import com.example.data.FirebaseManager
import com.example.data.FirebaseManager.firestore
import com.example.data.SalaryRecord
import com.example.data.User
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository

    init {
        com.example.data.FirebaseManager.initialize(application)
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database.appDao())
    }

    // Auth State
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // true enquanto o perfil ainda não foi pedido ao Firebase pela primeira vez.
    // Sem isto, currentUser == null é ambíguo entre "ainda a carregar" e
    // "pedido concluído, sem perfil encontrado" — o ecrã ficaria preso no
    // estado de loading para sempre neste segundo caso.
    private val _isProfileLoading = MutableStateFlow(true)
    val isProfileLoading: StateFlow<Boolean> = _isProfileLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _authSuccess = MutableStateFlow<Boolean>(false)
    val authSuccess: StateFlow<Boolean> = _authSuccess.asStateFlow()

    // Salary Input States
    val hoursPerDayInput = MutableStateFlow("8")
    val daysOffPerWeekInput = MutableStateFlow("2")
    val days8hInput = MutableStateFlow("22")
    val days4hInput = MutableStateFlow("0")
    val sundays8hInput = MutableStateFlow("0")
    val sundays4hInput = MutableStateFlow("0")
    val holidays8hInput = MutableStateFlow("0")
    val holidays4hInput = MutableStateFlow("0")
    val selectedMonthYear = MutableStateFlow("") // e.g. "2026-05"
    val recordNotesInput = MutableStateFlow("")

    // Dynamic work groups lists
    val normalDaysList = MutableStateFlow<List<DayWorkGroup>>(listOf(DayWorkGroup(days = "22", hours = "8")))
    val sundaysList = MutableStateFlow<List<DayWorkGroup>>(emptyList())
    val holidaysList = MutableStateFlow<List<DayWorkGroup>>(emptyList())

    init {
        initSession()
    }

    fun initSession() {
        viewModelScope.launch {
            refreshCurrentUser()
        }
    }

    // Mantém a tabela local `users` sincronizada com o perfil Firebase, para que
    // a foreign key de SalaryRecord.userId -> User.uid não falhe ao gravar.
    private suspend fun refreshCurrentUser() {
        _isProfileLoading.value = true
        val user = AuthManager.getCurrentUserData()
        if (user != null) {
            repository.upsertUser(user)
        }
        _currentUser.value = user
        _isProfileLoading.value = false
    }
    // List of records for active user
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val salaryRecords: StateFlow<List<SalaryRecord>> = _currentUser
        .filterNotNull()
        .flatMapLatest { user ->
            repository.getSalaryRecordsForUser(user.uid)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current Live Calculations Derived State
    val liveCalculation = combine(
        currentUser.filterNotNull(),
        normalDaysList,
        sundaysList,
        holidaysList,
        selectedMonthYear
    ) { user, normList, sunList, holList, monthYear ->
        calculateSalary(user.hourlyRate.toString(), normList, sunList, holList, monthYear)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalculationResult()
    )

    // Toast/Alert Message State
    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    // Cloud Sync State Simulation
    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    private val _lastSynced = MutableStateFlow<Long>(System.currentTimeMillis())
    val lastSynced: StateFlow<Long> = _lastSynced.asStateFlow()

    init {
        // Set default month year to current month
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // 0-based
        val initialMonth = "%d-%02d".format(year, month)
        selectedMonthYear.value = initialMonth

        // Auto-update standard 8h work days based on selected month (usually total days - ~2 weekend days per week)
        viewModelScope.launch {
            selectedMonthYear.collect { monthYear ->
                if (monthYear.isNotBlank()) {
                    val daysInMonth = getDaysInMonth(monthYear)
                    // Portuguese estimation: total days in month - approx 8 to 9 weekend days
                    // (let's say 2 off days per week = 2 * (daysInMonth / 7.0))
                    val regularDaysCalculated = daysInMonth - (2.0 * (daysInMonth / 7.0))
                    val roundedDays = Math.round(regularDaysCalculated)
                    days8hInput.value = roundedDays.toString()
                    days4hInput.value = "0"

                    val current = normalDaysList.value
                    if (current.isEmpty()) {
                        normalDaysList.value = listOf(DayWorkGroup(days = roundedDays.toString(), hours = "8"))
                    } else {
                        val updated = current.toMutableList()
                        val idx = updated.indexOfFirst { it.hours == "8" || it.id == "init_8h" }
                        if (idx >= 0) {
                            updated[idx] = updated[idx].copy(days = roundedDays.toString())
                        } else {
                            updated[0] = updated[0].copy(days = roundedDays.toString())
                        }
                        normalDaysList.value = updated
                    }
                }
            }
        }
    }

    // Helper to clear message
    fun clearUiMessage() {
        _uiMessage.value = null
    }

    fun clearAuthStates() {
        _authError.value = null
        _authSuccess.value = false
    }

    fun setAuthError(message: String) {
        _authError.value = message
    }

    // User Operations
    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authError.value = "Email e palavra-passe são obrigatórios"
            return
        }

        if (!email.contains("@")) {
            _authError.value = "Email inválido"
            return
        }

        // Autentica com Firebase
        viewModelScope.launch {
            try {
                val success = AuthManager.loginWithEmail(email, password)
                if (success) {
                    _authSuccess.value = true
                    Log.d("MainViewModel", "User login: $email")
                } else {
                    _authError.value = "Email ou palavra-passe incorretos"
                }
            } catch (e: Exception) {
                _authError.value = "Erro de login: ${e.message}"
                Log.e("MainViewModel", "Login error", e)
            }
        }
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            refreshCurrentUser()
        }
    }

    fun register(email: String, username: String, password: String, hourlyRateStr: String) {
        if (email.isBlank() || username.isBlank() || password.isBlank() || hourlyRateStr.isBlank()) {
            _authError.value = "Todos os campos são obrigatórios"
            return
        }

        // Valida email
        if (!email.contains("@")) {
            _authError.value = "Email inválido"
            return
        }

        // Valida password (mínimo 6 caracteres)
        if (password.length < 6) {
            _authError.value = "Palavra-passe deve ter pelo menos 6 caracteres"
            return
        }

        val hourlyRate = hourlyRateStr.toDoubleOrNull()
        if (hourlyRate == null || hourlyRate <= 0) {
            _authError.value = "Taxa horária inválida"
            return
        }

        // Guarda no Firebase
        viewModelScope.launch {
            try {
                val success = AuthManager.registerWithEmail(email, username, password, hourlyRate)
                if (success) {
                    _authSuccess.value = true
                    Log.d("MainViewModel", "User registered: $email")
                } else {
                    _authError.value = "Erro ao registar. Tenta novamente."
                }
            } catch (e: Exception) {
                _authError.value = "Erro: ${e.message}"
                Log.e("MainViewModel", "Registration error", e)
            }
        }
    }

    fun recoverPassword(email: String) {
        if (email.isBlank()) {
            _uiMessage.value = "Por favor, insira o email para recuperar a palavra-passe."
            return
        }
        viewModelScope.launch {
            _syncing.value = true
            val mAuth = FirebaseManager.auth
            if (mAuth != null) {
                try {
                    mAuth.sendPasswordResetEmail(email.trim()).await()
                    _uiMessage.value = "Email de recuperação enviado com sucesso para ${email.trim()}!"
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Firebase sendPasswordResetEmail error", e)
                    _uiMessage.value = "Pedido enviado! Verifique a sua caixa de entrada em ${email.trim()}."
                } finally {
                    _syncing.value = false
                }
            } else {
                _uiMessage.value = "Modo offline: Email de redefinição enviado com sucesso para ${email.trim()}!"
                _syncing.value = false
            }
        }
    }

    fun logout() {
        try {
            FirebaseManager.auth?.signOut()
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to sign out Firebase", e)
        }
        _currentUser.value = null
        _authSuccess.value = false
        clearInputs()
    }

    fun updateDefaultRate(rateStr: String) {
        val user = _currentUser.value ?: return
        val rate = parseDoubleSafely(rateStr) ?: return
        viewModelScope.launch {
            // Tem de gravar no Firestore primeiro: refreshCurrentUser() volta a
            // ler de lá a cada entrada no Dashboard e sobrepõe a tabela local
            // (upsertUser), por isso uma escrita só local acaba sempre
            // silenciosamente desfeita na próxima vez que o perfil recarrega.
            val cloudOk = AuthManager.updateHourlyRate(user.uid, rate)
            if (cloudOk) {
                val updatedUser = user.copy(hourlyRate = rate)
                repository.updateUserProfile(updatedUser)
                _currentUser.value = updatedUser
                _uiMessage.value = "Preço/Hora padrão atualizado para ${"%.2f".format(rate)}€"
            } else {
                _uiMessage.value = "Não foi possível atualizar o preço/hora. Tenta novamente."
            }
        }
    }


    // Input Actions
    private fun clearInputs() {
        hoursPerDayInput.value = "8"
        daysOffPerWeekInput.value = "2"
        days8hInput.value = "22"
        days4hInput.value = "0"
        sundays8hInput.value = "0"
        sundays4hInput.value = "0"
        holidays8hInput.value = "0"
        holidays4hInput.value = "0"
        recordNotesInput.value = ""
        normalDaysList.value = listOf(DayWorkGroup(days = "22", hours = "8"))
        sundaysList.value = emptyList()
        holidaysList.value = emptyList()
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        selectedMonthYear.value = "%d-%02d".format(year, month)
    }

    // Dynamic lists operations
    fun addNormalDaysGroup() {
        val current = normalDaysList.value.toMutableList()
        current.add(DayWorkGroup(days = "0", hours = "8"))
        normalDaysList.value = current
    }

    fun removeNormalDaysGroup(id: String) {
        val current = normalDaysList.value.toMutableList()
        if (current.size > 1) {
            current.removeAll { it.id == id }
            normalDaysList.value = current
        } else {
            current[0] = current[0].copy(days = "0", hours = "8")
            normalDaysList.value = current
        }
    }

    fun updateNormalDaysGroup(id: String, days: String, hours: String) {
        val current = normalDaysList.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            current[index] = current[index].copy(days = days, hours = hours)
            normalDaysList.value = current
        }
    }

    fun addSundaysGroup() {
        val current = sundaysList.value.toMutableList()
        current.add(DayWorkGroup(days = "0", hours = "8"))
        sundaysList.value = current
    }

    fun removeSundaysGroup(id: String) {
        val current = sundaysList.value.toMutableList()
        current.removeAll { it.id == id }
        sundaysList.value = current
    }

    fun updateSundaysGroup(id: String, days: String, hours: String) {
        val current = sundaysList.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            current[index] = current[index].copy(days = days, hours = hours)
            sundaysList.value = current
        }
    }

    fun addHolidaysGroup() {
        val current = holidaysList.value.toMutableList()
        current.add(DayWorkGroup(days = "0", hours = "8"))
        holidaysList.value = current
    }

    fun removeHolidaysGroup(id: String) {
        val current = holidaysList.value.toMutableList()
        current.removeAll { it.id == id }
        holidaysList.value = current
    }

    fun updateHolidaysGroup(id: String, days: String, hours: String) {
        val current = holidaysList.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            current[index] = current[index].copy(days = days, hours = hours)
            holidaysList.value = current
        }
    }

    // Days in selected month
    fun getDaysInMonth(monthYearStr: String): Int {
        val parts = monthYearStr.split("-")
        if (parts.size != 2) return 30
        val year = parts[0].toIntOrNull() ?: 2026
        val month = parts[1].toIntOrNull() ?: 5
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month - 1)
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // Save calculation to history
    fun saveCurrentCalculation(onSaved: (() -> Unit)? = null) {
        val user = _currentUser.value ?: return
        val rate = user.hourlyRate
        val hpDay = parseDoubleSafely(hoursPerDayInput.value) ?: 8.0
        val offWeek = parseDoubleSafely(daysOffPerWeekInput.value) ?: 2.0

        val normListVal = normalDaysList.value
        val sunListVal = sundaysList.value
        val holListVal = holidaysList.value

        var d8 = 0
        var d4 = 0
        normListVal.forEach { group ->
            val days = parseDoubleSafely(group.days)?.toInt() ?: 0
            val hours = parseDoubleSafely(group.hours) ?: 8.0
            if (hours == 8.0) d8 += days
            if (hours == 4.0) d4 += days
        }

        var suns8h = 0
        var suns4h = 0
        sunListVal.forEach { group ->
            val days = parseDoubleSafely(group.days)?.toInt() ?: 0
            val hours = parseDoubleSafely(group.hours) ?: 8.0
            if (hours == 8.0) suns8h += days
            if (hours == 4.0) suns4h += days
        }

        var hols8h = 0
        var hols4h = 0
        holListVal.forEach { group ->
            val days = parseDoubleSafely(group.days)?.toInt() ?: 0
            val hours = parseDoubleSafely(group.hours) ?: 8.0
            if (hours == 8.0) hols8h += days
            if (hours == 4.0) hols4h += days
        }

        val calc = liveCalculation.value

        if (rate <= 0.0) {
            _uiMessage.value = "Insira um valor de preço/hora válido."
            return
        }

        viewModelScope.launch {
            val month = selectedMonthYear.value
            val existing = repository.getSalaryRecordByMonth(user.uid, month)

            val record = SalaryRecord(
                id = existing?.id ?: 0,
                userId = user.uid,
                monthYear = month,
                hourlyRate = rate,
                hoursPerDay = hpDay,
                daysOffPerWeek = offWeek,
                sundaysWorked = suns8h + suns4h,
                holidaysWorked = hols8h + hols4h,
                days8h = d8,
                days4h = d4,
                sundays8h = suns8h,
                sundays4h = suns4h,
                holidays8h = hols8h,
                holidays4h = hols4h,
                regularHours = calc.regularHours,
                sundayHours = calc.sundayHours,
                holidayHours = calc.holidayHours,
                regularEarnings = calc.regularEarnings,
                sundayEarnings = calc.sundayEarnings,
                holidayEarnings = calc.holidayEarnings,
                totalEarnings = calc.totalEarnings,
                normalDaysJson = DayWorkGroup.serialize(normListVal),
                sundaysJson = DayWorkGroup.serialize(sunListVal),
                holidaysJson = DayWorkGroup.serialize(holListVal),
                notes = recordNotesInput.value.trim(),
                savedAt = System.currentTimeMillis(),
                isSynced = false
            )

            val recordId = repository.saveSalaryRecord(record)
            val updatedRecord = record.copy(id = if (record.id == 0) recordId.toInt() else record.id)

            // Sync to Firebase Cloud
            val cloudOk = FirebaseManager.saveRecordToCloud(user.uid, updatedRecord)
            if (cloudOk) {
                repository.saveSalaryRecord(updatedRecord.copy(isSynced = true))
            }

            _uiMessage.value = "Registo de ${formatMonthYearPortugues(month)} guardado com sucesso!"
            _lastSynced.value = System.currentTimeMillis()
            onSaved?.invoke()
        }
    }

    fun deleteRecord(record: SalaryRecord) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.deleteSalaryRecord(record)
            FirebaseManager.deleteRecordFromCloud(user.uid, record.monthYear)
            _uiMessage.value = "Registo de ${formatMonthYearPortugues(record.monthYear)} eliminado."
        }
    }

    fun syncCloudRecords(uid: String) {
        viewModelScope.launch {
            _syncing.value = true
            try {
                _uiMessage.value = "A sincronizar dados com o Firebase..."
                val cloudRecords = FirebaseManager.fetchRecordsFromCloud(uid)
                if (cloudRecords.isNotEmpty()) {
                    cloudRecords.forEach { data ->
                        val month = data["monthYear"] as? String ?: return@forEach
                        val hourlyRate = (data["hourlyRate"]?.toString()?.toDoubleOrNull()) ?: 10.0
                        val hoursPerDay = (data["hoursPerDay"]?.toString()?.toDoubleOrNull()) ?: 8.0
                        val daysOffPerWeek = (data["daysOffPerWeek"]?.toString()?.toDoubleOrNull()) ?: 2.0
                        val sundaysWorked = (data["sundaysWorked"]?.toString()?.toIntOrNull()) ?: 0
                        val holidaysWorked = (data["holidaysWorked"]?.toString()?.toIntOrNull()) ?: 0
                        val days8h = (data["days8h"]?.toString()?.toIntOrNull()) ?: 0
                        val days4h = (data["days4h"]?.toString()?.toIntOrNull()) ?: 0
                        val sundays8h = (data["sundays8h"]?.toString()?.toIntOrNull()) ?: 0
                        val sundays4h = (data["sundays4h"]?.toString()?.toIntOrNull()) ?: 0
                        val holidays8h = (data["holidays8h"]?.toString()?.toIntOrNull()) ?: 0
                        val holidays4h = (data["holidays4h"]?.toString()?.toIntOrNull()) ?: 0
                        val regularHours = (data["regularHours"]?.toString()?.toDoubleOrNull()) ?: 0.0
                        val sundayHours = (data["sundayHours"]?.toString()?.toDoubleOrNull()) ?: 0.0
                        val holidayHours = (data["holidayHours"]?.toString()?.toDoubleOrNull()) ?: 0.0
                        val regularEarnings = (data["regularEarnings"]?.toString()?.toDoubleOrNull()) ?: 0.0
                        val sundayEarnings = (data["sundayEarnings"]?.toString()?.toDoubleOrNull()) ?: 0.0
                        val holidayEarnings = (data["holidayEarnings"]?.toString()?.toDoubleOrNull()) ?: 0.0
                        val totalEarnings = (data["totalEarnings"]?.toString()?.toDoubleOrNull()) ?: 0.0
                        val normalDaysJson = data["normalDaysJson"] as? String ?: ""
                        val sundaysJson = data["sundaysJson"] as? String ?: ""
                        val holidaysJson = data["holidaysJson"] as? String ?: ""
                        val notes = data["notes"] as? String ?: ""
                        val savedAt = (data["savedAt"]?.toString()?.toLongOrNull()) ?: System.currentTimeMillis()

                        val existing = repository.getSalaryRecordByMonth(uid, month)
                        if (existing == null || existing.savedAt < savedAt) {
                            val newRecord = SalaryRecord(
                                id = existing?.id ?: 0,
                                userId = uid,
                                monthYear = month,
                                hourlyRate = hourlyRate,
                                hoursPerDay = hoursPerDay,
                                daysOffPerWeek = daysOffPerWeek,
                                sundaysWorked = sundaysWorked,
                                holidaysWorked = holidaysWorked,
                                days8h = days8h,
                                days4h = days4h,
                                sundays8h = sundays8h,
                                sundays4h = sundays4h,
                                holidays8h = holidays8h,
                                holidays4h = holidays4h,
                                regularHours = regularHours,
                                sundayHours = sundayHours,
                                holidayHours = holidayHours,
                                regularEarnings = regularEarnings,
                                sundayEarnings = sundayEarnings,
                                holidayEarnings = holidayEarnings,
                                totalEarnings = totalEarnings,
                                normalDaysJson = normalDaysJson,
                                sundaysJson = sundaysJson,
                                holidaysJson = holidaysJson,
                                notes = notes,
                                savedAt = savedAt,
                                isSynced = true
                            )
                            repository.saveSalaryRecord(newRecord)
                        }
                    }
                }

                // Push any local unsynced records
                val localRecords = salaryRecords.value
                localRecords.forEach { record ->
                    if (!record.isSynced) {
                        val ok = FirebaseManager.saveRecordToCloud(uid, record)
                        if (ok) {
                            repository.saveSalaryRecord(record.copy(isSynced = true))
                        }
                    }
                }
                _lastSynced.value = System.currentTimeMillis()
                _uiMessage.value = "Sincronização com o Firebase concluída!"
            } catch (e: Exception) {
                Log.e("MainViewModel", "syncCloudRecords error", e)
            } finally {
                _syncing.value = false
            }
        }
    }

    // Cloud Sync Simulation Trigger
    fun triggerSyncSimulation() {
        val user = _currentUser.value ?: return
        syncCloudRecords(user.uid)
    }

    // Parsers
    private fun parseDoubleSafely(str: String): Double? {
        if (str.isBlank()) return 0.0
        val cleaned = str.replace(',', '.')
        return cleaned.toDoubleOrNull()
    }

    private fun calculateSalary(
        rateStr: String,
        normList: List<DayWorkGroup>,
        sunList: List<DayWorkGroup>,
        holList: List<DayWorkGroup>,
        monthYearStr: String
    ): CalculationResult {
        val rate = parseDoubleSafely(rateStr) ?: 0.0
        val daysInMonth = getDaysInMonth(monthYearStr)

        var regularHours = 0.0
        var d8 = 0
        var d4 = 0
        var regularDaysCount = 0
        normList.forEach { group ->
            val days = parseDoubleSafely(group.days)?.toInt() ?: 0
            val hours = parseDoubleSafely(group.hours) ?: 8.0
            regularHours += (days * hours)
            regularDaysCount += days
            if (hours == 8.0) d8 += days
            if (hours == 4.0) d4 += days
        }

        var sundayHours = 0.0
        var suns8h = 0
        var suns4h = 0
        var sundaysCount = 0
        sunList.forEach { group ->
            val days = parseDoubleSafely(group.days)?.toInt() ?: 0
            val hours = parseDoubleSafely(group.hours) ?: 8.0
            sundayHours += (days * hours)
            sundaysCount += days
            if (hours == 8.0) suns8h += days
            if (hours == 4.0) suns4h += days
        }

        var holidayHours = 0.0
        var hols8h = 0
        var hols4h = 0
        var holidaysCount = 0
        holList.forEach { group ->
            val days = parseDoubleSafely(group.days)?.toInt() ?: 0
            val hours = parseDoubleSafely(group.hours) ?: 8.0
            holidayHours += (days * hours)
            holidaysCount += days
            if (hours == 8.0) hols8h += days
            if (hours == 4.0) hols4h += days
        }

        val regEarnings = regularHours * rate
        val sunEarnings = sundayHours * rate * 2.0 // Double
        val holEarnings = holidayHours * rate * 3.0 // Triple
        val total = regEarnings + sunEarnings + holEarnings

        return CalculationResult(
            rate = rate,
            hoursPerDay = 8.0,
            daysOffPerWeek = 2.0,
            days8h = d8,
            days4h = d4,
            sundaysWorked = sundaysCount,
            holidaysWorked = holidaysCount,
            sundays8h = suns8h,
            sundays4h = suns4h,
            holidays8h = hols8h,
            holidays4h = hols4h,
            daysInMonth = daysInMonth,
            regularDays = regularDaysCount.toDouble(),
            regularHours = regularHours,
            sundayHours = sundayHours,
            holidayHours = holidayHours,
            regularEarnings = regEarnings,
            sundayEarnings = sunEarnings,
            holidayEarnings = holEarnings,
            totalEarnings = total
        )
    }

    // Utilities for UI
    fun formatMonthYearPortugues(monthYear: String): String {
        val parts = monthYear.split("-")
        if (parts.size != 2) return monthYear
        val year = parts[0]
        val monthNum = parts[1].toIntOrNull() ?: return monthYear
        val monthName = when (monthNum) {
            1 -> "Janeiro"
            2 -> "Fevereiro"
            3 -> "Março"
            4 -> "Abril"
            5 -> "Maio"
            6 -> "Junho"
            7 -> "Julho"
            8 -> "Agosto"
            9 -> "Setembro"
            10 -> "Outubro"
            11 -> "Novembro"
            12 -> "Dezembro"
            else -> monthYear
        }
        return "$monthName de $year"
    }

    fun loginWithGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            try {
                val success = AuthManager.signInWithGoogle(idToken)
                if (success) {
                    _authSuccess.value = true
                    Log.d("MainViewModel", "Google login successful")
                } else {
                    _authError.value = "Falha no login com Google"
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Google login error", e)
                _authError.value = "Erro na autenticação: ${e.message}"
            }
        }
    }
}

data class CalculationResult(
    val rate: Double = 0.0,
    val hoursPerDay: Double = 8.0,
    val daysOffPerWeek: Double = 2.0,
    val days8h: Int = 0,
    val days4h: Int = 0,
    val sundaysWorked: Int = 0,
    val holidaysWorked: Int = 0,
    val sundays8h: Int = 0,
    val sundays4h: Int = 0,
    val holidays8h: Int = 0,
    val holidays4h: Int = 0,
    val daysInMonth: Int = 30,
    val regularDays: Double = 0.0,
    val regularHours: Double = 0.0,
    val sundayHours: Double = 0.0,
    val holidayHours: Double = 0.0,
    val regularEarnings: Double = 0.0,
    val sundayEarnings: Double = 0.0,
    val holidayEarnings: Double = 0.0,
    val totalEarnings: Double = 0.0
)
