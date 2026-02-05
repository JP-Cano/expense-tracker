package com.expensetracker.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.expensetracker.MainViewModel
import com.expensetracker.R
import com.expensetracker.data.CardBrand
import com.expensetracker.data.Currency
import com.expensetracker.data.PaymentType
import com.expensetracker.db.ExpenseEntity
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@Composable
fun ExpenseApp(viewModel: MainViewModel) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { AppBottomBar(navController) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            NavHost(
                navController = navController,
                startDestination = "add",
                modifier = Modifier.weight(1f)
            ) {
                composable("add") { AddExpenseScreen(viewModel) }
                composable("list") { ExpenseListScreen(viewModel) }
                composable("totals") { TotalsScreen(viewModel) }
            }
        }
    }
}

@Composable
private fun AppBottomBar(navController: NavHostController) {
    val destinations = listOf(
        NavItem("add", R.string.nav_add, Icons.Filled.Add),
        NavItem("list", R.string.nav_list, Icons.Filled.List),
        NavItem("totals", R.string.nav_totals, Icons.Filled.BarChart)
    )
    NavigationBar {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        destinations.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { navController.navigate(item.route) },
                icon = { Icon(imageVector = item.icon, contentDescription = null) },
                label = { Text(text = stringResource(id = item.label)) }
            )
        }
    }
}

private data class NavItem(
    val route: String,
    val label: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
private fun AddExpenseScreen(viewModel: MainViewModel) {
    val form by viewModel.form.collectAsState()
    val error by viewModel.errorMessage.collectAsState()

    var paymentExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var cardBrandExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = stringResource(id = R.string.add_expense), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(id = R.string.section_payment), fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = stringResource(id = R.string.payment_type), modifier = Modifier.width(120.dp))
                    TextButton(onClick = { paymentExpanded = true }) {
                        Text(text = paymentLabel(form.paymentType))
                    }
                    DropdownMenu(expanded = paymentExpanded, onDismissRequest = { paymentExpanded = false }) {
                        PaymentType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(paymentLabel(type)) },
                                onClick = {
                                    viewModel.updatePaymentType(type)
                                    paymentExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = form.amount,
                    onValueChange = viewModel::updateAmount,
                    label = { Text(stringResource(id = R.string.value)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = stringResource(id = R.string.currency), modifier = Modifier.width(120.dp))
                    TextButton(onClick = { currencyExpanded = true }) {
                        Text(text = form.currency.name)
                    }
                    DropdownMenu(expanded = currencyExpanded, onDismissRequest = { currencyExpanded = false }) {
                        Currency.values().forEach { currency ->
                            DropdownMenuItem(
                                text = { Text(currency.name) },
                                onClick = {
                                    viewModel.updateCurrency(currency)
                                    currencyExpanded = false
                                }
                            )
                        }
                    }
                }

                if (form.currency == Currency.USD) {
                    OutlinedTextField(
                        value = form.manualRate,
                        onValueChange = viewModel::updateManualRate,
                        label = { Text(stringResource(id = R.string.manual_rate)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (form.paymentType == PaymentType.CARD) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(id = R.string.card_brand), modifier = Modifier.width(120.dp))
                        TextButton(onClick = { cardBrandExpanded = true }) {
                            Text(text = cardBrandLabel(form.cardBrand))
                        }
                        DropdownMenu(expanded = cardBrandExpanded, onDismissRequest = { cardBrandExpanded = false }) {
                            CardBrand.values().forEach { brand ->
                                DropdownMenuItem(
                                    text = { Text(cardBrandLabel(brand)) },
                                    onClick = {
                                        viewModel.updateCardBrand(brand)
                                        cardBrandExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(id = R.string.section_details), fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = form.place,
                    onValueChange = viewModel::updatePlace,
                    label = { Text(stringResource(id = R.string.place)) },
                    modifier = Modifier.fillMaxWidth()
                )

                DatePickerField(
                    label = stringResource(id = R.string.date),
                    date = form.date,
                    onDateSelected = viewModel::updateDate
                )

                OutlinedTextField(
                    value = form.description,
                    onValueChange = viewModel::updateDescription,
                    label = { Text(stringResource(id = R.string.description)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (error != null) {
            Text(text = error ?: "", color = MaterialTheme.colorScheme.error)
            LaunchedEffect(error) {
                viewModel.clearError()
            }
        }

        Button(onClick = { viewModel.saveExpense() }, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(id = R.string.save))
        }
    }
}

@Composable
private fun ExpenseListScreen(viewModel: MainViewModel) {
    val expenses by viewModel.expenses.collectAsState()
    val message by viewModel.message.collectAsState()
    val editingId by viewModel.editingId.collectAsState()
    val editForm by viewModel.editForm.collectAsState()
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportToUri(context, uri)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importFromUri(context, uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = stringResource(id = R.string.expenses), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { exportLauncher.launch("expenses.json") }) {
                Text(text = stringResource(id = R.string.export_json))
            }
            Button(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                Text(text = stringResource(id = R.string.import_json))
            }
        }

        if (message != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message ?: "", color = MaterialTheme.colorScheme.primary)
            LaunchedEffect(message) {
                viewModel.clearMessage()
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        if (expenses.isEmpty()) {
            Text(text = stringResource(id = R.string.no_expenses))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(expenses) { expense ->
                    ExpenseCard(
                        expense = expense,
                        onEdit = { viewModel.startEdit(expense) },
                        onDelete = { viewModel.deleteExpense(expense.id) }
                    )
                }
            }
        }
    }

    if (editingId != null) {
        EditExpenseDialog(
            form = editForm,
            onDismiss = viewModel::cancelEdit,
            onSave = viewModel::saveEdit,
            onPaymentChange = viewModel::updateEditPaymentType,
            onCardBrandChange = viewModel::updateEditCardBrand,
            onAmountChange = viewModel::updateEditAmount,
            onCurrencyChange = viewModel::updateEditCurrency,
            onPlaceChange = viewModel::updateEditPlace,
            onDateChange = viewModel::updateEditDate,
            onDescriptionChange = viewModel::updateEditDescription,
            onManualRateChange = viewModel::updateEditManualRate
        )
    }
}

@Composable
private fun ExpenseCard(
    expense: ExpenseEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val copFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    val usdFormat = NumberFormat.getCurrencyInstance(Locale.US)

    var confirmDelete by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.place.ifBlank { "-" },
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = expense.description.ifBlank { "-" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Filled.Edit, contentDescription = null)
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = null)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            val original = if (expense.currency == Currency.USD.name) {
                usdFormat.format(expense.amount)
            } else {
                copFormat.format(expense.amount)
            }
            Text(text = "${expense.date} • $original")
            if (!expense.cardBrand.isNullOrBlank()) {
                Text(text = stringResource(id = R.string.card_brand_label, expense.cardBrand))
            }
            Text(text = copFormat.format(expense.amountCop), fontWeight = FontWeight.SemiBold)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(id = R.string.delete_title)) },
            text = { Text(stringResource(id = R.string.delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text(stringResource(id = R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(id = R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun TotalsScreen(viewModel: MainViewModel) {
    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()
    val total by viewModel.totalBetween.collectAsState()

    val copFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = stringResource(id = R.string.totals), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DatePickerField(
                    label = stringResource(id = R.string.from),
                    date = startDate,
                    onDateSelected = { viewModel.setDateRange(it, endDate) }
                )

                DatePickerField(
                    label = stringResource(id = R.string.to),
                    date = endDate,
                    onDateSelected = { viewModel.setDateRange(startDate, it) }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = viewModel::quickToday) { Text(stringResource(id = R.string.quick_today)) }
                    Button(onClick = viewModel::quickMonth) { Text(stringResource(id = R.string.quick_month)) }
                    Button(onClick = viewModel::quickYear) { Text(stringResource(id = R.string.quick_year)) }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(id = R.string.total_cop), fontWeight = FontWeight.SemiBold)
        Text(text = copFormat.format(total), fontSize = 24.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    label: String,
    date: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    val dateText = date.toString()

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, modifier = Modifier.width(120.dp))
        TextButton(onClick = { open = true }) { Text(text = dateText) }
    }

    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.toEpochMillis())
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        onDateSelected(millis.toLocalDate())
                    }
                    open = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun paymentLabel(type: PaymentType): String {
    return when (type) {
        PaymentType.CASH -> stringResource(id = R.string.payment_cash)
        PaymentType.CARD -> stringResource(id = R.string.payment_card)
        PaymentType.TRANSFER -> stringResource(id = R.string.payment_transfer)
        PaymentType.OTHER -> stringResource(id = R.string.payment_other)
    }
}

@Composable
private fun cardBrandLabel(brand: CardBrand?): String {
    return when (brand) {
        CardBrand.VISA -> stringResource(id = R.string.card_brand_visa)
        CardBrand.MASTERCARD -> stringResource(id = R.string.card_brand_mastercard)
        CardBrand.AMEX -> stringResource(id = R.string.card_brand_amex)
        CardBrand.OTHER -> stringResource(id = R.string.card_brand_other)
        null -> stringResource(id = R.string.card_brand_select)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditExpenseDialog(
    form: com.expensetracker.data.ExpenseForm,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onPaymentChange: (PaymentType) -> Unit,
    onCardBrandChange: (CardBrand) -> Unit,
    onAmountChange: (String) -> Unit,
    onCurrencyChange: (Currency) -> Unit,
    onPlaceChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onManualRateChange: (String) -> Unit
) {
    var paymentExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var cardBrandExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.edit_expense)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = stringResource(id = R.string.payment_type), modifier = Modifier.width(120.dp))
                    TextButton(onClick = { paymentExpanded = true }) {
                        Text(text = paymentLabel(form.paymentType))
                    }
                    DropdownMenu(expanded = paymentExpanded, onDismissRequest = { paymentExpanded = false }) {
                        PaymentType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(paymentLabel(type)) },
                                onClick = {
                                    onPaymentChange(type)
                                    paymentExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = form.amount,
                    onValueChange = onAmountChange,
                    label = { Text(stringResource(id = R.string.value)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = stringResource(id = R.string.currency), modifier = Modifier.width(120.dp))
                    TextButton(onClick = { currencyExpanded = true }) {
                        Text(text = form.currency.name)
                    }
                    DropdownMenu(expanded = currencyExpanded, onDismissRequest = { currencyExpanded = false }) {
                        Currency.values().forEach { currency ->
                            DropdownMenuItem(
                                text = { Text(currency.name) },
                                onClick = {
                                    onCurrencyChange(currency)
                                    currencyExpanded = false
                                }
                            )
                        }
                    }
                }

                if (form.currency == Currency.USD) {
                    OutlinedTextField(
                        value = form.manualRate,
                        onValueChange = onManualRateChange,
                        label = { Text(stringResource(id = R.string.manual_rate)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (form.paymentType == PaymentType.CARD) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(id = R.string.card_brand), modifier = Modifier.width(120.dp))
                        TextButton(onClick = { cardBrandExpanded = true }) {
                            Text(text = cardBrandLabel(form.cardBrand))
                        }
                        DropdownMenu(expanded = cardBrandExpanded, onDismissRequest = { cardBrandExpanded = false }) {
                            CardBrand.values().forEach { brand ->
                                DropdownMenuItem(
                                    text = { Text(cardBrandLabel(brand)) },
                                    onClick = {
                                        onCardBrandChange(brand)
                                        cardBrandExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = form.place,
                    onValueChange = onPlaceChange,
                    label = { Text(stringResource(id = R.string.place)) },
                    modifier = Modifier.fillMaxWidth()
                )

                DatePickerField(
                    label = stringResource(id = R.string.date),
                    date = form.date,
                    onDateSelected = onDateChange
                )

                OutlinedTextField(
                    value = form.description,
                    onValueChange = onDescriptionChange,
                    label = { Text(stringResource(id = R.string.description)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) { Text(stringResource(id = R.string.update)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) }
        }
    )
}

private fun LocalDate.toEpochMillis(): Long {
    return atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
}
