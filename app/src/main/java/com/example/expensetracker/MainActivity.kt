package com.example.expensetracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.expensetracker.domain.BiometricLockManager
import com.example.expensetracker.ui.AuthScreen
import com.example.expensetracker.ui.DashboardScreen
import com.example.expensetracker.ui.MainViewModel
import com.example.expensetracker.ui.components.SpendSyncLogo
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var biometricLockManager: BiometricLockManager
    private var isBiometricUnlocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        biometricLockManager = BiometricLockManager(this)

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
            val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()

            // Trigger biometric prompt on startup if enabled
            LaunchedEffect(isBiometricEnabled) {
                if (isBiometricEnabled && !isBiometricUnlocked) {
                    biometricLockManager.promptBiometricUnlock(
                        activity = this@MainActivity,
                        onSuccess = { isBiometricUnlocked = true },
                        onError = { /* Keep locked */ }
                    )
                } else if (!isBiometricEnabled) {
                    isBiometricUnlocked = true
                }
            }

            ExpenseTrackerTheme(userDarkModePreference = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isAuthenticated by remember {
                        mutableStateOf(isInitiallyAuthenticated)
                    }

                    if (isBiometricEnabled && !isBiometricUnlocked) {
                        BiometricLockScreen(
                            onUnlockClick = {
                                biometricLockManager.promptBiometricUnlock(
                                    activity = this@MainActivity,
                                    onSuccess = { isBiometricUnlocked = true },
                                    onError = { /* Keep locked */ }
                                )
                            }
                        )
                    } else if (isAuthenticated) {
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

@Composable
fun BiometricLockScreen(
    onUnlockClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SpendSyncLogo(size = 72.dp)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "SpendSync is Locked",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Authenticate to access your financial records",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onUnlockClick,
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("Unlock SpendSync", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}