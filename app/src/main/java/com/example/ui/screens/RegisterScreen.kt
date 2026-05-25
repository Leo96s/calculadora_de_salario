package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val liveCalc by viewModel.liveCalculation.collectAsState()
    var showMonthDialog by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Registar Turnos",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            keyboardController?.hide()
                            onNavigateBack()
                        },
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Text(
                    text = "Insira as horas e dias trabalhados para calcular e guardar os seus ganhos deste mês.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                CalculatorFormCard(
                    viewModel = viewModel,
                    onOpenMonthPicker = { showMonthDialog = true }
                )
            }

            item {
                LiveResultsCard(
                    result = liveCalc,
                    viewModel = viewModel,
                    onSaved = {
                        keyboardController?.hide()
                        onNavigateBack()
                    }
                )
            }
        }
    }

    // Month Selector Dialog reuse
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
}
