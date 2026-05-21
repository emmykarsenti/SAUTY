package fr.isen.emmykarsenti.nicolasbetoin.sauty

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import fr.isen.emmykarsenti.nicolasbetoin.sauty.ble.BleManager
import fr.isen.emmykarsenti.nicolasbetoin.sauty.ui.DashboardScreen
import fr.isen.emmykarsenti.nicolasbetoin.sauty.ui.LoginScreen
import fr.isen.emmykarsenti.nicolasbetoin.sauty.ui.ProfileScreen
import fr.isen.emmykarsenti.nicolasbetoin.sauty.ui.RegisterScreen
import fr.isen.emmykarsenti.nicolasbetoin.sauty.ui.SessionDetailScreen
import fr.isen.emmykarsenti.nicolasbetoin.sauty.ui.TrendsDetailScreen
import fr.isen.emmykarsenti.nicolasbetoin.sauty.ui.WorkoutScreen
import fr.isen.emmykarsenti.nicolasbetoin.sauty.viewmodel.SautyViewModel

class MainActivity : ComponentActivity() {

    private lateinit var bleManager: BleManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.entries.all { it.value }) {
            Toast.makeText(this, "Permissions BLE accordées ! \uD83D\uDE80", Toast.LENGTH_SHORT).show()
            bleManager.tryAutoConnect()
        } else {
            Toast.makeText(this, "Erreur : Le Bluetooth est obligatoire.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bleManager = BleManager(this)
        checkAndRequestBluetoothPermissions()

        val user = FirebaseAuth.getInstance().currentUser
        val initialStartDestination = if (user != null) "dashboard" else "login"

        setContent {
            val viewModel: SautyViewModel = viewModel()

            bleManager.onStatusMessage = { message ->
                viewModel.updateStatus(message)
            }

            MaterialTheme(colorScheme = darkColorScheme()) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val showBottomBar = currentRoute in listOf("dashboard", "workout", "scan")

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Résumé") },
                                    label = { Text("Résumé") },
                                    selected = currentRoute == "dashboard",
                                    onClick = {
                                        navController.navigate("dashboard") {
                                            popUpTo("dashboard") { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Exercice") },
                                    label = { Text("Exercice") },
                                    selected = currentRoute == "workout",
                                    onClick = {
                                        navController.navigate("workout") {
                                            popUpTo("dashboard") { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Bluetooth, contentDescription = "Scanner") },
                                    label = { Text("Scanner") },
                                    selected = currentRoute == "scan",
                                    onClick = {
                                        navController.navigate("scan") {
                                            popUpTo("scan") { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = initialStartDestination,
                        modifier = Modifier.padding(innerPadding)
                    ) {

                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("dashboard") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onNavigateToRegister = {
                                    navController.navigate("register")
                                }
                            )
                        }

                        composable("register") {
                            RegisterScreen(
                                onRegisterSuccess = {
                                    navController.navigate("dashboard") {
                                        popUpTo("register") { inclusive = true }
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onBackToLogin = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("dashboard") {
                            DashboardScreen(
                                bleManager = bleManager,
                                viewModel = viewModel,
                                onProfileClick = { navController.navigate("profile") },
                                onSessionClick = { navController.navigate("sessionDetail") },
                                onTrendsClick = { navController.navigate("trendsDetail") }
                            )
                        }

                        composable("workout") {
                            val timerString by viewModel.timerString.collectAsState()
                            val jumpsCount by viewModel.jumpsCount.collectAsState()
                            val calories by viewModel.calories.collectAsState()
                            val isRunning by viewModel.isRunning.collectAsState()

                            WorkoutScreen(
                                timerString = timerString,
                                jumpsCount = jumpsCount,
                                calories = calories,
                                isRunning = isRunning,
                                onStartPauseClick = { viewModel.toggleWorkout() },
                                onStopClick = { viewModel.stopWorkout() }
                            )
                        }

                        composable("scan") {
                            SautyScanScreen(
                                onStartScanClick = { bleManager.startScan() },
                                onDisconnectClick = { bleManager.disconnectAndForget() },
                                viewModel = viewModel
                            )
                        }

                        composable("profile") {
                            ProfileScreen(
                                onLogout = {
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("trendsDetail") {
                            TrendsDetailScreen(
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        // CORRECTION ICI : On ne passe plus le viewModel car c'est un écran de test visuel
                        composable("sessionDetail") {
                            SessionDetailScreen(
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestBluetoothPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        requestPermissionLauncher.launch(permissions)
    }
}

@Composable
fun SautyScanScreen(
    onStartScanClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    viewModel: SautyViewModel = viewModel()
) {
    val statusText by viewModel.connectionStatus.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Bracelet SAUTY",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = {
                viewModel.startScanning()
                onStartScanClick()
            }) {
                Text(text = "SCANNER", style = MaterialTheme.typography.titleMedium)
            }
            Button(
                onClick = { onDisconnectClick() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(text = "OUBLIER", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}