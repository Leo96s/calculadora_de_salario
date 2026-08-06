package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.MaritalStatus
import com.example.domain.TaxRegion
import com.example.ui.MainViewModel

private val maritalStatusLabels = linkedMapOf(
    MaritalStatus.NAO_CASADO to "Não casado",
    MaritalStatus.CASADO_UNICO_TITULAR to "Casado — Único titular",
    MaritalStatus.CASADO_DOIS_TITULARES to "Casado — Dois titulares"
)

private val regionLabels = linkedMapOf(
    TaxRegion.CONTINENTE to "Continente",
    TaxRegion.ACORES to "Açores",
    TaxRegion.MADEIRA to "Madeira"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()

    var rateInput by remember { mutableStateOf("") }
    var dependentsInput by remember { mutableStateOf("") }
    var maritalStatus by remember { mutableStateOf(MaritalStatus.NAO_CASADO) }
    var region by remember { mutableStateOf(TaxRegion.CONTINENTE) }

    // Preenche os campos a partir do perfil só uma vez, quando este fica
    // disponível — sem isto, cada recomposição (ex. depois de gravar)
    // reporia os campos com o valor antigo antes do StateFlow atualizar.
    LaunchedEffect(currentUser?.uid) {
        currentUser?.let { user ->
            rateInput = "%.2f".format(user.hourlyRate)
            dependentsInput = user.dependents.toString()
            maritalStatus = runCatching { MaritalStatus.valueOf(user.maritalStatus) }
                .getOrDefault(MaritalStatus.NAO_CASADO)
            region = runCatching { TaxRegion.valueOf(user.region) }
                .getOrDefault(TaxRegion.CONTINENTE)
        }
    }

    var maritalStatusExpanded by remember { mutableStateOf(false) }
    var regionExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Definições",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Taxa/hora e situação fiscal usadas para calcular o valor líquido",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Text(
                        text = "Preço base por hora",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rateInput,
                        onValueChange = { rateInput = it },
                        label = { Text("Valor à hora (€)") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Euro, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Situação fiscal",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = maritalStatusExpanded,
                        onExpandedChange = { maritalStatusExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = maritalStatusLabels.getValue(maritalStatus),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Estado civil") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = maritalStatusExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = maritalStatusExpanded,
                            onDismissRequest = { maritalStatusExpanded = false }
                        ) {
                            maritalStatusLabels.forEach { (status, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        maritalStatus = status
                                        maritalStatusExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = dependentsInput,
                        onValueChange = { dependentsInput = it },
                        label = { Text("Número de dependentes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ExposedDropdownMenuBox(
                        expanded = regionExpanded,
                        onExpandedChange = { regionExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = regionLabels.getValue(region),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Região") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = regionExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = regionExpanded,
                            onDismissRequest = { regionExpanded = false }
                        ) {
                            regionLabels.forEach { (r, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        region = r
                                        regionExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Os valores de IRS são uma estimativa baseada nas tabelas oficiais " +
                                "de retenção na fonte em vigor; para a tua situação fiscal exata, " +
                                "confirma com o teu TOC.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.updateDefaultRate(rateInput)
                            viewModel.updateFiscalSettings(maritalStatus.name, dependentsInput, region.name)
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(text = "Guardar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
