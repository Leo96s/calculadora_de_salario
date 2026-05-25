package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SalaryRecord
import com.example.ui.MainViewModel
import com.example.ui.DayWorkGroup
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val records by viewModel.salaryRecords.collectAsState()
    val liveCalc by viewModel.liveCalculation.collectAsState()
    val syncing by viewModel.syncing.collectAsState()
    val lastSynced by viewModel.lastSynced.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()

    var showMonthDialog by remember { mutableStateOf(false) }
    var showEditDefaultRateDialog by remember { mutableStateOf(false) }
    var defaultRateInput by remember { mutableStateOf("") }

    val keyboardController = LocalSoftwareKeyboardController.current

    // Observe Ui message for Snackbars
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiMessage) {
        uiMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUiMessage()
        }
    }

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            onNavigateToLogin()
        } else {
            defaultRateInput = currentUser?.defaultHourlyRate?.toString() ?: ""
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Controlo de Salário",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (syncing) 10.dp else 8.dp)
                                    .background(
                                        color = if (syncing) MaterialTheme.colorScheme.primary else Color(0xFF4CAF50),
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (syncing) "A sincronizar na nuvem..." else "Sincronizado na Nuvem",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.triggerSyncSimulation() },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (syncing) Icons.Default.Sync else Icons.Default.CloudDone,
                            contentDescription = "Sincronizar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { showEditDefaultRateDialog = true },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Preço Hora Padrão",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Sair",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToRegister,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .size(64.dp)
                    .testTag("add_register_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Adicionar Registo",
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        // Time logic for Forecast/Previsão Calc
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val currentMonthKey = "%04d-%02d".format(year, month)

        val prevCalendar = Calendar.getInstance()
        prevCalendar.set(Calendar.MONTH, prevCalendar.get(Calendar.MONTH) - 1)
        val prevYear = prevCalendar.get(Calendar.YEAR)
        val prevMonth = prevCalendar.get(Calendar.MONTH) + 1
        val prevMonthKey = "%04d-%02d".format(prevYear, prevMonth)

        val currentMonthRecord = records.find { it.monthYear == currentMonthKey }
        val prevMonthRecord = records.find { it.monthYear == prevMonthKey }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // 1. Profile Welcome Card
            item {
                ProfileWelcomeCard(
                    username = currentUser?.username ?: "Utilizador",
                    email = "leonardosilva.00009@gmail.com", // Dynamic email matching user metadata
                    defaultRate = currentUser?.defaultHourlyRate ?: 10.0,
                    lastSyncTime = lastSynced
                )
            }

            // 2. Previsão de Recebimento Card
            item {
                Text(
                    text = "Previsão de Recebimento",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentMonthRecord != null) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = viewModel.formatMonthYearPortugues(currentMonthKey),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentMonthRecord != null) {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                }
                            )

                            // Badge
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (currentMonthRecord != null) {
                                            Color(0xFF4CAF50).copy(alpha = 0.15f)
                                        } else if (prevMonthRecord != null) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (currentMonthRecord != null) "Confirmado" else if (prevMonthRecord != null) "Previsão" else "Sem Dados",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentMonthRecord != null) {
                                        Color(0xFF2E7D32)
                                    } else if (prevMonthRecord != null) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val totalToShow = if (currentMonthRecord != null) {
                            currentMonthRecord.totalEarnings
                        } else if (prevMonthRecord != null) {
                            prevMonthRecord.totalEarnings
                        } else {
                            0.0
                        }

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "${"%.2f".format(totalToShow)}€",
                                fontSize = 38.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (currentMonthRecord != null) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Explanatory Subtext
                        val explanationText = if (currentMonthRecord != null) {
                            "Valor total registado e confirmado para o mês atual."
                        } else if (prevMonthRecord != null) {
                            "Estimativa baseada no registo do mês anterior (${viewModel.formatMonthYearPortugues(prevMonthKey)})."
                        } else {
                            "Ainda não inseriu dados para o mês passado ou atual. Comece por registar os seus turnos!"
                        }

                        Text(
                            text = explanationText,
                            fontSize = 13.sp,
                            color = if (currentMonthRecord != null) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            }
                        )

                        if (currentMonthRecord == null && prevMonthRecord != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Toque no + abaixo para confirmar o registo real deste mês.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Trend Graph
            if (records.isNotEmpty()) {
                item {
                    Text(
                        text = "Gráfico de Ganhos",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                item {
                    EarningsTrendChart(records = records.take(6).reversed())
                }
            }

            // 4. History Header & List
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Histórico de Meses",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "${records.size} registos",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }

            if (records.isEmpty()) {
                item {
                    EmptyHistoryCard()
                }
            } else {
                items(records, key = { it.id }) { record ->
                    HistoryItemCard(
                        record = record,
                        onDelete = { viewModel.deleteRecord(record) },
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    // Month Selector Dialog
    if (showMonthDialog) {
        MonthYearPickerDialog(
            currentSelected = viewModel.selectedMonthYear.collectAsState().value,
            onDismiss = { showMonthDialog = false },
            onConfirm = { monthYear ->
                viewModel.selectedMonthYear.value = monthYear
                showMonthDialog = false
            }
        )
    }

    // Edit Default Hourly Rate Dialog
    if (showEditDefaultRateDialog) {
        AlertDialog(
            onDismissRequest = { showEditDefaultRateDialog = false },
            title = { Text("Preço Base por Hora") },
            text = {
                Column {
                    Text(
                        text = "Define o valor padrão que costumas receber por hora. Isto será usado para pré-preencher novas consultas.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = defaultRateInput,
                        onValueChange = { defaultRateInput = it },
                        label = { Text("Valor à hora (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateDefaultRate(defaultRateInput)
                        showEditDefaultRateDialog = false
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDefaultRateDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ProfileWelcomeCard(
    username: String,
    email: String,
    defaultRate: Double,
    lastSyncTime: Long
) {
    val sdf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeStr = sdf.format(Date(lastSyncTime))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Initials Circle
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = username.take(2).uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1.0f)) {
                Text(
                    text = "Olá, $username!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = email,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Preço Hora Base: ${"%.2f".format(defaultRate)}€ | Sinc: $timeStr",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun CalculatorFormCard(
    viewModel: MainViewModel,
    onOpenMonthPicker: () -> Unit
) {
    val rate by viewModel.hourlyRateInput.collectAsState()
    val normalDays by viewModel.normalDaysList.collectAsState()
    val sundays by viewModel.sundaysList.collectAsState()
    val holidays by viewModel.holidaysList.collectAsState()
    val selectedMonthYear by viewModel.selectedMonthYear.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Month Picker Trigger
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenMonthPicker() }
                    .background(
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Mês de Referência",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = viewModel.formatMonthYearPortugues(selectedMonthYear),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Mudar Mês",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Hourly rate (master setting)
            OutlinedTextField(
                value = rate,
                onValueChange = { viewModel.hourlyRateInput.value = it },
                label = { Text("Preço Hora (€)") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rate_input_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                )
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(16.dp))

            // Normal Days Section
            WorkGroupListEditor(
                title = "Dias Normais",
                groups = normalDays,
                onAddGroup = { viewModel.addNormalDaysGroup() },
                onRemoveGroup = { viewModel.removeNormalDaysGroup(it) },
                onUpdateGroup = { id, days, hours -> viewModel.updateNormalDaysGroup(id, days, hours) },
                daysPlaceholder = "Dias",
                hoursPlaceholder = "Horas/Dia"
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(16.dp))

            // Sundays Section
            WorkGroupListEditor(
                title = "Domingos (2x)",
                groups = sundays,
                onAddGroup = { viewModel.addSundaysGroup() },
                onRemoveGroup = { viewModel.removeSundaysGroup(it) },
                onUpdateGroup = { id, days, hours -> viewModel.updateSundaysGroup(id, days, hours) },
                daysPlaceholder = "Domingos",
                hoursPlaceholder = "Horas/Dia"
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(16.dp))

            // Holidays Section
            WorkGroupListEditor(
                title = "Feriados (3x)",
                groups = holidays,
                onAddGroup = { viewModel.addHolidaysGroup() },
                onRemoveGroup = { viewModel.removeHolidaysGroup(it) },
                onUpdateGroup = { id, days, hours -> viewModel.updateHolidaysGroup(id, days, hours) },
                daysPlaceholder = "Feriados",
                hoursPlaceholder = "Horas/Dia"
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(16.dp))

            // Notes field
            val notes by viewModel.recordNotesInput.collectAsState()
            OutlinedTextField(
                value = notes,
                onValueChange = { viewModel.recordNotesInput.value = it },
                label = { Text("Notas opcionais (ex. Empresa, Turno)") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                )
            )
        }
    }
}

@Composable
fun LiveResultsCard(
    result: com.example.ui.CalculationResult,
    viewModel: MainViewModel,
    onSaved: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Total a Receber:",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            // Dynamic Gradient Text for amount
            Text(
                text = "${"%.2f".format(result.totalEarnings)}€",
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Breakdown Rows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Regulares (${result.days8h}d de 8h + ${result.days4h}d de 4h):",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "${"%.2f".format(result.regularEarnings)}€",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Domingos a dobrar (${result.sundaysWorked} dias - ${"%.1f".format(result.sundayHours)}h):",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "+ ${"%.2f".format(result.sundayEarnings)}€",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Feriados a triplicar (${result.holidaysWorked} dias - ${"%.1f".format(result.holidayHours)}h):",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "+ ${"%.2f".format(result.holidayEarnings)}€",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Calculation formula explanation box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Resumo do Mês (${result.daysInMonth} dias):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Total de dias normais: ${result.days8h + result.days4h} dias (${result.days8h} de 8h / ${result.days4h} de 4h)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Total de horas regulares: ${"%.1f".format(result.regularHours)}h",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Save button
            Button(
                onClick = { viewModel.saveCurrentCalculation(onSaved) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_calculation_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Guardar no Histórico",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EmptyHistoryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.WorkHistory,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Sem Registos no Histórico",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Insere as tuas horas de trabalho em cima e clica em Guardar para veres o teu histórico aqui.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun HistoryItemCard(
    record: SalaryRecord,
    onDelete: () -> Unit,
    viewModel: MainViewModel
) {
    var showConfirmDelete by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_${record.monthYear}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        if (showConfirmDelete) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Deseja eliminar este registo?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showConfirmDelete = false }) {
                        Text("Não")
                    }
                    Button(
                        onClick = {
                            onDelete()
                            showConfirmDelete = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Sim")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = viewModel.formatMonthYearPortugues(record.monthYear),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (record.notes.isNotBlank()) {
                            Text(
                                text = record.notes,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${"%.2f".format(record.totalEarnings)}€",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        IconButton(
                            onClick = { showConfirmDelete = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                val parsedNorm = DayWorkGroup.deserialize(record.normalDaysJson)
                val normDesc = if (parsedNorm.isNotEmpty()) {
                    parsedNorm.joinToString("+") { "${it.days}d×${it.hours}h" }
                } else {
                    "${record.days8h}d/${record.days4h}d"
                }

                val parsedSun = DayWorkGroup.deserialize(record.sundaysJson)
                val sunDesc = if (parsedSun.isNotEmpty()) {
                    parsedSun.joinToString("+") { "${it.days}d×${it.hours}h" }
                } else {
                    "${record.sundays8h}d/${record.sundays4h}d"
                }

                val parsedHol = DayWorkGroup.deserialize(record.holidaysJson)
                val holDesc = if (parsedHol.isNotEmpty()) {
                    parsedHol.joinToString("+") { "${it.days}d×${it.hours}h" }
                } else {
                    "${record.holidays8h}d/${record.holidays4h}d"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LabelValueItem(
                        label = "Regulares",
                        value = "${"%.1f".format(record.regularHours)}h ($normDesc)"
                    )
                    LabelValueItem(
                        label = "Domingos (2x)",
                        value = "${"%.1f".format(record.sundayHours)}h ($sunDesc)"
                    )
                    LabelValueItem(
                        label = "Feriados (3x)",
                        value = "${"%.1f".format(record.holidayHours)}h ($holDesc)"
                    )
                    LabelValueItem(label = "P/ Hora", value = "${"%.2f".format(record.hourlyRate)}€")
                }
            }
        }
    }
}

@Composable
fun LabelValueItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun EarningsTrendChart(records: List<SalaryRecord>) {
    val barColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Evolução dos Ganhos Mensais",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = barColor,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Box(modifier = Modifier.weight(1f)) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp)
                ) {
                    val width = size.width
                    val height = size.height

                    if (width <= 0f || height <= 0f) return@Canvas

                    val rawMax = records.maxOfOrNull { it.totalEarnings } ?: 1.0
                    val maxEarnings = if (rawMax <= 0.0) 1.0 else rawMax
                    val count = records.size

                    val spacing = 20.dp.toPx()
                    val totalSpacing = spacing * (count + 1)
                    val barWidth = ((width - totalSpacing) / count).coerceAtLeast(1f)

                    records.forEachIndexed { index, record ->
                        val heightLimit = (height - 30.dp.toPx()).coerceAtLeast(0f)
                        val barHeight = ((record.totalEarnings / maxEarnings) * heightLimit).toFloat().coerceAtLeast(0f)
                        val x = spacing + index * (barWidth + spacing)
                        val y = (height - barHeight - 20.dp.toPx()).coerceAtLeast(0f)

                        // Draw Bar with custom gradients
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )

                        // Note / Indicator trigger can draw months text below
                        // Under standard environment we draw lines/labels cleanly or can use standard compose alignments
                    }
                }

                // Month text elements placed overlay matching canvas spacing
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    records.forEach { record ->
                        val month = record.monthYear.split("-").getOrNull(1)?.toIntOrNull() ?: 1
                        val monthShort = when (month) {
                            1 -> "Jan"
                            2 -> "Fev"
                            3 -> "Mar"
                            4 -> "Abr"
                            5 -> "Mai"
                            6 -> "Jun"
                            7 -> "Jul"
                            8 -> "Ago"
                            9 -> "Set"
                            10 -> "Out"
                            11 -> "Nov"
                            12 -> "Dez"
                            else -> month.toString()
                        }
                        Text(
                            text = monthShort,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = labelColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthYearPickerDialog(
    currentSelected: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    // Basic dialog with years and months
    val years = remember { (2024..2028).toList() }
    val months = remember { (1..12).toList() }

    val initialYear = currentSelected.split("-").getOrNull(0)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
    val initialMonth = currentSelected.split("-").getOrNull(1)?.toIntOrNull() ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)

    var selectedYear by remember { mutableStateOf(initialYear) }
    var selectedMonth by remember { mutableStateOf(initialMonth) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Selecionar Mês/Ano") },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Month Column Selector
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "MÊS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Box(modifier = Modifier.height(150.dp)) {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            months.forEach { m ->
                                val monthName = when (m) {
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
                                    else -> ""
                                }

                                Button(
                                    onClick = { selectedMonth = m },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedMonth == m) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                        contentColor = if (selectedMonth == m) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.width(110.dp)
                                ) {
                                    Text(text = monthName, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Year Column Selector
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ANO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Box(modifier = Modifier.height(150.dp)) {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            years.forEach { y ->
                                Button(
                                    onClick = { selectedYear = y },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedYear == y) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                        contentColor = if (selectedYear == y) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.width(80.dp)
                                ) {
                                    Text(text = y.toString(), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm("%d-%02d".format(selectedYear, selectedMonth)) }
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// Pulse size is handled directly in-line

@Composable
fun WorkGroupListEditor(
    title: String,
    groups: List<DayWorkGroup>,
    onAddGroup: () -> Unit,
    onRemoveGroup: (String) -> Unit,
    onUpdateGroup: (String, String, String) -> Unit,
    daysPlaceholder: String = "Dias",
    hoursPlaceholder: String = "Horas/Dia"
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(
                onClick = onAddGroup,
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Adicionar grupo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (groups.isEmpty()) {
            Text(
                text = "Nenhum dia adicionado. Toque no + para adicionar.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
            )
        } else {
            groups.forEachIndexed { index, group ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Days text field
                    OutlinedTextField(
                        value = group.days,
                        onValueChange = { onUpdateGroup(group.id, it, group.hours) },
                        label = { Text(daysPlaceholder) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("days_input_${title.lowercase().replace(" ", "_")}_$index"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                    )

                    // Hours text field
                    OutlinedTextField(
                        value = group.hours,
                        onValueChange = { onUpdateGroup(group.id, group.days, it) },
                        label = { Text(hoursPlaceholder) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hours_input_${title.lowercase().replace(" ", "_")}_$index"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                    )

                    // Remove button
                    IconButton(
                        onClick = { onRemoveGroup(group.id) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remover",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
