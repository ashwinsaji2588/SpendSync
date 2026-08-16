package com.example.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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

        // Safely initialize Firebase without crashing if google-services.json is missing
        val isFirebaseReady = try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            true
        } catch (e: Exception) {
            false
        }

        val currentUser = try {
            if (isFirebaseReady) FirebaseAuth.getInstance().currentUser else null
        } catch (e: Exception) {
            null
        }

        setContent {
            ExpenseTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isAuthenticated by remember {
                        mutableStateOf(currentUser != null || !isFirebaseReady)
                    }

                    if (isAuthenticated) {
                        DashboardScreen(
                            viewModel = viewModel,
                            onSignOut = {
                                try {
                                    FirebaseAuth.getInstance().signOut()
                                } catch (e: Exception) {
                                    // Ignore sign-out errors when running in offline mode
                                }
                                isAuthenticated = false
                            }
                        )
                    } else {
                        AuthScreen(
                            onAuthSuccess = { isAuthenticated = true }
                        )
                    }
                }
            }
        }
    }
}