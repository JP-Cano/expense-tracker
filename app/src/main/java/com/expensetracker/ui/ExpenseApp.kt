package com.expensetracker.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.expensetracker.RateUiState
import com.expensetracker.data.CardBrand
import com.expensetracker.data.Currency
import com.expensetracker.data.PaymentType
import com.expensetracker.data.PocketForm
import com.expensetracker.data.UNCATEGORIZED_POCKET_ID
import com.expensetracker.db.ExpenseEntity
import com.expensetracker.db.PocketEntity
import com.expensetracker.repository.ExpenseRepository.DeletePocketMode
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@Composable
fun ExpenseApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val message by viewModel.message.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(resolveMessage(context, message!!))
            viewModel.clearMessage()
        }
    }

    Scaffold(
        bottomBar = { AppBottomBar(navController) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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
                startDestination = "home",
                modifier = Modifier.weight(1f)
            ) {
                composable("home") { HomeScreen(viewModel, navController) }
                composable("add") { AddExpenseScreen(viewModel) }
                composable("list") { ExpenseListScreen(viewModel) }
                composable("pocket/{id}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: UNCATEGORIZED_POCKET_ID
                    PocketDetailScreen(viewModel, navController, id)
                }
            }
        }
    }
}

@Composable
private fun AppBottomBar(navController: NavHostController) {
    val destinations = listOf(
        NavItem("home", R.string.nav_home, Icons.Filled.BarChart),
        NavItem("add", R.string.nav_add, Icons.Filled.Add),
        NavItem("list", R.string.nav_list, Icons.AutoMirrored.Filled.List)
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
    val icon: ImageVector
)

@Composable
private fun HomeScreen(viewModel: MainViewModel, navController: NavHostController) {
    val pockets by viewModel.pockets.collectAsState()
    val totals by viewModel.pocketTotals.collectAsState()
    val globalTotal by viewModel.globalMonthlyTotal.collectAsState()
    val globalBudget by viewModel.globalBudget.collectAsState()
    val error by viewModel.errorMessage.collectAsState()

    var showPocketDialog by remember { mutableStateOf(false) }
    var globalBudgetInput by remember { mutableStateOf(globalBudget?.monthlyBudget?.toString() ?: "") }

    LaunchedEffect(globalBudget?.monthlyBudget) {
        globalBudgetInput = globalBudget?.monthlyBudget?.toString() ?: ""
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(text = stringResource(id = R.string.home_title), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = stringResource(id = R.string.global_total_month), fontWeight = FontWeight.SemiBold)
                    Text(text = formatCop(globalTotal), fontSize = 22.sp)

                    OutlinedTextField(
                        value = globalBudgetInput,
                        onValueChange = { globalBudgetInput = it },
                        label = { Text(stringResource(id = R.string.global_budget)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.saveGlobalBudget(globalBudgetInput) }) {
                            Text(text = stringResource(id = R.string.save))
                        }
                        TextButton(onClick = { globalBudgetInput = ""; viewModel.saveGlobalBudget(null) }) {
                            Text(text = stringResource(id = R.string.clear_budget))
                        }
                    }

                    val remaining = (globalBudget?.monthlyBudget ?: 0.0) - globalTotal
                    if (globalBudget?.monthlyBudget != null) {
                        Text(
                            text = stringResource(id = R.string.remaining_budget, formatCop(remaining)),
                            color = if (remaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.startCreatePocket(); showPocketDialog = true }) {
                    Text(text = stringResource(id = R.string.add_pocket))
                }
            }
        }

        item {
            if (error != null) {
                Text(text = error ?: "", color = MaterialTheme.colorScheme.error)
            }
        }

        items(pockets) { pocket ->
            val total = totals[pocket.id] ?: 0.0
            PocketRow(
                pocket = pocket,
                total = total,
                onOpen = { navController.navigate("pocket/${pocket.id}") },
                onEdit = { viewModel.startEditPocket(pocket); showPocketDialog = true }
            )
        }
    }

    if (showPocketDialog) {
        PocketDialog(
            form = viewModel.pocketForm.collectAsState().value,
            onDismiss = { showPocketDialog = false },
            onSave = {
                val globalBudgetValue = globalBudget?.monthlyBudget
                viewModel.savePocket(globalBudgetValue)
                showPocketDialog = false
            },
            onNameChange = viewModel::updatePocketName,
            onBudgetChange = viewModel::updatePocketBudget,
            onColorChange = viewModel::updatePocketColor,
            onIconChange = viewModel::updatePocketIcon
        )
    }
}

@Composable
private fun PocketRow(
    pocket: PocketEntity,
    total: Double,
    onOpen: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = iconForName(pocket.icon),
                contentDescription = null,
                tint = Color(pocket.color),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = pocket.name, fontWeight = FontWeight.SemiBold)
                Text(text = formatCop(total), color = MaterialTheme.colorScheme.onSurfaceVariant)
                pocket.monthlyBudget?.let { budget ->
                    val remaining = budget - total
                    Text(
                        text = stringResource(id = R.string.remaining_budget, formatCop(remaining)),
                        color = if (remaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            IconButton(onClick = onOpen) {
                Icon(imageVector = Icons.Filled.BarChart, contentDescription = null)
            }
            if (!pocket.isSystem) {
                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Filled.Edit, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun PocketDetailScreen(viewModel: MainViewModel, navController: NavHostController, pocketId: Long) {
    val pockets by viewModel.pockets.collectAsState()
    val pocket = pockets.firstOrNull { it.id == pocketId }
    val globalTotal by viewModel.globalMonthlyTotal.collectAsState()
    val pocketTotal by viewModel.pocketMonthlyTotal(pocketId).collectAsState(0.0)
    val expenses by viewModel.expensesByPocket(pocketId).collectAsState(initial = emptyList())

    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteMode by remember { mutableStateOf(DeletePocketMode.MOVE_TO_UNCATEGORIZED) }
    var reassignPocketId by remember { mutableStateOf(UNCATEGORIZED_POCKET_ID) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = pocket?.name ?: stringResource(id = R.string.uncategorized), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(id = R.string.pocket_total_month), fontWeight = FontWeight.SemiBold)
                Text(text = formatCop(pocketTotal), fontSize = 22.sp)
                pocket?.monthlyBudget?.let { budget ->
                    val remaining = budget - pocketTotal
                    Text(
                        text = stringResource(id = R.string.remaining_budget, formatCop(remaining)),
                        color = if (remaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(text = stringResource(id = R.string.global_total_month), fontWeight = FontWeight.SemiBold)
                Text(text = formatCop(globalTotal))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                viewModel.startNewExpenseForPocket(pocketId)
                navController.navigate("add")
            }) {
                Text(text = stringResource(id = R.string.add_expense))
            }
        }

        if (pocket != null && !pocket.isSystem) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showDeleteDialog = true }) {
                    Text(text = stringResource(id = R.string.delete_pocket))
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(expenses) { expense ->
                ExpenseCard(expense = expense, onEdit = { viewModel.startEdit(expense) }, onDelete = { viewModel.deleteExpense(expense.id) })
            }
        }
    }

    if (showDeleteDialog && pocket != null) {
        DeletePocketDialog(
            pocketName = pocket.name,
            pockets = pockets.filter { it.id != pocket.id },
            mode = deleteMode,
            onModeChange = { deleteMode = it },
            reassignPocketId = reassignPocketId,
            onReassignChange = { reassignPocketId = it },
            onConfirm = {
                viewModel.deletePocket(pocket.id, deleteMode, reassignPocketId)
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    val editingId by viewModel.editingId.collectAsState()
    val editForm by viewModel.editForm.collectAsState()
    if (editingId != null) {
        EditExpenseDialog(
            form = editForm,
            pockets = pockets,
            onDismiss = viewModel::cancelEdit,
            onSave = viewModel::saveEdit,
            onPaymentChange = viewModel::updateEditPaymentType,
            onCardBrandChange = viewModel::updateEditCardBrand,
            onAmountChange = viewModel::updateEditAmount,
            onCurrencyChange = viewModel::updateEditCurrency,
            onPlaceChange = viewModel::updateEditPlace,
            onDateChange = viewModel::updateEditDate,
            onDescriptionChange = viewModel::updateEditDescription,
            onManualRateChange = viewModel::updateEditManualRate,
            onPocketChange = viewModel::updateEditPocketId
        )
    }
}

@Composable
private fun AddExpenseScreen(viewModel: MainViewModel) {
    val form by viewModel.form.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val rateState by viewModel.usdRateState.collectAsState()
    val pockets by viewModel.pockets.collectAsState()

    var paymentExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var cardBrandExpanded by remember { mutableStateOf(false) }
    var pocketExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(form.currency, form.date) {
        viewModel.refreshRateIfNeeded()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(text = stringResource(id = R.string.add_expense), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        }

        item {
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
                        when (rateState) {
                            RateUiState.Loading -> Text(
                                text = stringResource(id = R.string.rate_loading),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            is RateUiState.Success -> {
                                val rate = (rateState as RateUiState.Success).rate
                                Text(
                                    text = stringResource(id = R.string.rate_label, rate),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(id = R.string.rate_attribution),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                            is RateUiState.Error -> {
                                Text(
                                    text = stringResource(id = R.string.rate_error, (rateState as RateUiState.Error).message),
                                    color = MaterialTheme.colorScheme.error
                                )
                                OutlinedTextField(
                                    value = form.manualRate,
                                    onValueChange = viewModel::updateManualRate,
                                    label = { Text(stringResource(id = R.string.manual_rate)) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = stringResource(id = R.string.rate_attribution),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                            RateUiState.Idle -> Unit
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(id = R.string.pocket), modifier = Modifier.width(120.dp))
                        TextButton(onClick = { pocketExpanded = true }) {
                            val pocketName = pockets.firstOrNull { it.id == form.pocketId }?.name
                                ?: stringResource(id = R.string.uncategorized)
                            Text(text = pocketName)
                        }
                        DropdownMenu(expanded = pocketExpanded, onDismissRequest = { pocketExpanded = false }) {
                            pockets.forEach { pocket ->
                                DropdownMenuItem(
                                    text = { Text(pocket.name) },
                                    onClick = {
                                        viewModel.updatePocketId(pocket.id)
                                        pocketExpanded = false
                                    }
                                )
                            }
                        }
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
        }

        item {
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
        }

        item {
            if (error != null) {
                Text(text = error ?: "", color = MaterialTheme.colorScheme.error)
            }
        }

        item {
            Button(onClick = { viewModel.saveExpense() }, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(id = R.string.save))
            }
        }
    }
}

@Composable
private fun ExpenseListScreen(viewModel: MainViewModel) {
    val expenses by viewModel.expenses.collectAsState()
    val editingId by viewModel.editingId.collectAsState()
    val editForm by viewModel.editForm.collectAsState()
    val pockets by viewModel.pockets.collectAsState()
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
            pockets = pockets,
            onDismiss = viewModel::cancelEdit,
            onSave = viewModel::saveEdit,
            onPaymentChange = viewModel::updateEditPaymentType,
            onCardBrandChange = viewModel::updateEditCardBrand,
            onAmountChange = viewModel::updateEditAmount,
            onCurrencyChange = viewModel::updateEditCurrency,
            onPlaceChange = viewModel::updateEditPlace,
            onDateChange = viewModel::updateEditDate,
            onDescriptionChange = viewModel::updateEditDescription,
            onManualRateChange = viewModel::updateEditManualRate,
            onPocketChange = viewModel::updateEditPocketId
        )
    }
}

@Composable
private fun ExpenseCard(
    expense: ExpenseEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val copFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }
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
private fun EditExpenseDialog(
    form: com.expensetracker.data.ExpenseForm,
    pockets: List<PocketEntity>,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onPaymentChange: (PaymentType) -> Unit,
    onCardBrandChange: (CardBrand) -> Unit,
    onAmountChange: (String) -> Unit,
    onCurrencyChange: (Currency) -> Unit,
    onPlaceChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onManualRateChange: (String) -> Unit,
    onPocketChange: (Long) -> Unit
) {
    var paymentExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var cardBrandExpanded by remember { mutableStateOf(false) }
    var pocketExpanded by remember { mutableStateOf(false) }

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
                    Text(
                        text = stringResource(id = R.string.rate_attribution),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = stringResource(id = R.string.pocket), modifier = Modifier.width(120.dp))
                    TextButton(onClick = { pocketExpanded = true }) {
                        val pocketName = pockets.firstOrNull { it.id == form.pocketId }?.name
                            ?: stringResource(id = R.string.uncategorized)
                        Text(text = pocketName)
                    }
                    DropdownMenu(expanded = pocketExpanded, onDismissRequest = { pocketExpanded = false }) {
                        pockets.forEach { pocket ->
                            DropdownMenuItem(
                                text = { Text(pocket.name) },
                                onClick = {
                                    onPocketChange(pocket.id)
                                    pocketExpanded = false
                                }
                            )
                        }
                    }
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

@Composable
private fun PocketDialog(
    form: PocketForm,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onNameChange: (String) -> Unit,
    onBudgetChange: (String) -> Unit,
    onColorChange: (Int) -> Unit,
    onIconChange: (String) -> Unit
) {
    var iconExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.pocket_editor)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(id = R.string.pocket_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = form.monthlyBudget,
                    onValueChange = onBudgetChange,
                    label = { Text(stringResource(id = R.string.pocket_budget)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = stringResource(id = R.string.pocket_icon), modifier = Modifier.width(120.dp))
                    TextButton(onClick = { iconExpanded = true }) {
                        Text(text = form.icon)
                    }
                    DropdownMenu(expanded = iconExpanded, onDismissRequest = { iconExpanded = false }) {
                        pocketIconOptions().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onIconChange(option)
                                    iconExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColorOption(0xFF1F2937.toInt(), form.color, onColorChange)
                    ColorOption(0xFF0F766E.toInt(), form.color, onColorChange)
                    ColorOption(0xFF7C3AED.toInt(), form.color, onColorChange)
                    ColorOption(0xFFB45309.toInt(), form.color, onColorChange)
                    ColorOption(0xFF991B1B.toInt(), form.color, onColorChange)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) { Text(stringResource(id = R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) }
        }
    )
}

@Composable
private fun ColorOption(color: Int, selected: Int, onSelect: (Int) -> Unit) {
    val border = if (selected == color) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else null
    Card(
        modifier = Modifier.size(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(color)),
        border = border,
        onClick = { onSelect(color) }
    ) {}
}

@Composable
private fun DeletePocketDialog(
    pocketName: String,
    pockets: List<PocketEntity>,
    mode: DeletePocketMode,
    onModeChange: (DeletePocketMode) -> Unit,
    reassignPocketId: Long,
    onReassignChange: (Long) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var reassignExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.delete_pocket)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(id = R.string.delete_pocket_confirm, pocketName))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = mode == DeletePocketMode.DELETE_EXPENSES,
                        onClick = { onModeChange(DeletePocketMode.DELETE_EXPENSES) }
                    )
                    Text(text = stringResource(id = R.string.delete_pocket_expenses))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = mode == DeletePocketMode.MOVE_TO_UNCATEGORIZED,
                        onClick = { onModeChange(DeletePocketMode.MOVE_TO_UNCATEGORIZED) }
                    )
                    Text(text = stringResource(id = R.string.delete_pocket_uncategorized))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = mode == DeletePocketMode.REASSIGN,
                        onClick = { onModeChange(DeletePocketMode.REASSIGN) }
                    )
                    Text(text = stringResource(id = R.string.delete_pocket_reassign))
                }

                if (mode == DeletePocketMode.REASSIGN) {
                    TextButton(onClick = { reassignExpanded = true }) {
                        val pocketNameLabel = pockets.firstOrNull { it.id == reassignPocketId }?.name
                            ?: stringResource(id = R.string.uncategorized)
                        Text(text = pocketNameLabel)
                    }
                    DropdownMenu(expanded = reassignExpanded, onDismissRequest = { reassignExpanded = false }) {
                        pockets.forEach { pocket ->
                            DropdownMenuItem(
                                text = { Text(pocket.name) },
                                onClick = {
                                    onReassignChange(pocket.id)
                                    reassignExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(id = R.string.delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) }
        }
    )
}

@Composable
private fun paymentLabel(type: PaymentType): String {
    return when (type) {
        PaymentType.CASH -> stringResource(id = R.string.payment_cash)
        PaymentType.CARD -> stringResource(id = R.string.payment_card)
        PaymentType.TRANSFER -> stringResource(id = R.string.payment_transfer)
        PaymentType.QR -> stringResource(id = R.string.payment_QR)
        PaymentType.OTHER -> stringResource(id = R.string.payment_other)
    }
}

@Composable
private fun cardBrandLabel(brand: CardBrand?): String {
    return when (brand) {
        CardBrand.VISA -> stringResource(id = R.string.card_brand_visa)
        CardBrand.MASTERCARD -> stringResource(id = R.string.card_brand_mastercard)
        CardBrand.AMEX -> stringResource(id = R.string.card_brand_amex)
        CardBrand.RAPPI -> stringResource(id = R.string.card_brand_rappi)
        CardBrand.GLOBAL66 -> stringResource(id = R.string.card_brand_global66)
        CardBrand.OTHER -> stringResource(id = R.string.card_brand_other)
        null -> stringResource(id = R.string.card_brand_select)
    }
}

private fun iconForName(name: String): ImageVector {
    return when (name) {
        "Pets" -> Icons.Filled.Pets
        "Restaurant" -> Icons.Filled.Restaurant
        "ShoppingBag" -> Icons.Filled.ShoppingBag
        "LocalPharmacy" -> Icons.Filled.LocalPharmacy
        "FitnessCenter" -> Icons.Filled.FitnessCenter
        else -> Icons.Filled.Category
    }
}

private fun pocketIconOptions(): List<String> {
    return listOf("Category", "Restaurant", "ShoppingBag", "Pets", "LocalPharmacy", "FitnessCenter")
}

private fun formatCop(value: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    format.maximumFractionDigits = 0
    format.minimumFractionDigits = 0
    return format.format(value)
}

private fun resolveMessage(context: android.content.Context, key: String): String {
    val res = context.resources
    return when (key) {
        "expense_saved" -> res.getString(R.string.expense_saved)
        "expense_updated" -> res.getString(R.string.expense_updated)
        "expense_deleted" -> res.getString(R.string.expense_deleted)
        "pocket_created" -> res.getString(R.string.pocket_created)
        "pocket_updated" -> res.getString(R.string.pocket_updated)
        "pocket_deleted" -> res.getString(R.string.pocket_deleted)
        else -> key
    }
}

private fun LocalDate.toEpochMillis(): Long {
    return atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
}
