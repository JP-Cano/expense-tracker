package com.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expensetracker.ui.ExpenseApp
import com.expensetracker.ui.theme.ExpenseTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = ServiceLocator.provideRepository(this)
        val factory = MainViewModel.Factory(repository)

        setContent {
            val vm: MainViewModel = viewModel(factory = factory)
            ExpenseTrackerTheme {
                ExpenseApp(vm)
            }
        }
    }
}
