package com.example.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.expensetracker.ui.AuthScreen
import com.example.expensetracker.ui.DashboardScreen
import com.example.expensetracker.ui.MainViewModel
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Safely initialize Firebase if available
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            // Ignore Firebase initialization error in offline development mode
        }

        val hasFirebaseUser = try {
            FirebaseAuth.getInstance().currentUser != null
        } catch (e: Exception) {
            false
        }

        // Persistent Auth check: Firebase user OR persisted session
        val isInitiallyAuthenticated = hasFirebaseUser || viewModel.isPersistedLoggedIn()

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            ExpenseTrackerTheme(userDarkModePreference = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isAuthenticated by remember {
                        mutableStateOf(isInitiallyAuthenticated)
                    }

                    if (isAuthenticated) {
                        DashboardScreen(
                            viewModel = viewModel,
                            onSignOut = {
                                viewModel.signOut {
                                    isAuthenticated = false
                                }
                            }
                        )
                    } else {
                        AuthScreen(
                            onAuthSuccess = {
                                viewModel.setPersistedLoggedIn(true)
                                isAuthenticated = true
                            }
                        )
                    }
                }
            }
        }
    }
}