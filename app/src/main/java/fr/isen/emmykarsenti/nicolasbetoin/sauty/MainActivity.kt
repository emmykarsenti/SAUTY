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
import fr.isen.emmykarsenti.nicolasbetoin.sauty.ui.ActivityDetailsScreen
import fr.isen.emmykarsenti.nicolasbetoin.sauty.ui.DashboardScreen
import fr.isen.emmykarsenti.nicolasbetoin.sauty.ui.LoginScreen
import fr.isen.emmykarsenti.nicolasbetoin.sauty.ui.ProfileScreen
import fr.isen.emmykarsenti.nicolasbetoin.sauty.ui.RegisterScreen
import fr.isen.emmykarsenti.nicolasbetoin.sauty.ui.SessionDetailScreen // <-- IMPORT AJOUTÉ
import fr.isen.emmykarsenti.nicolasbetoin.sauty.viewmodel.SautyViewModel

class MainActivity : ComponentActivity() {

    private lateinit var bleManager: BleManager

    // Le lanceur pour les permissions.
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.entries.all { it.value }) {
            Toast.makeText(this, "Permissions BLE accordées ! \uD83D\uDE80", Toast.LENGTH_SHORT).show()

            // On tente l'auto-connexion silencieuse dès que les permissions sont OK
            bleManager.tryAutoConnect()

        } else {
            Toast.makeText(this, "Erreur : Le Bluetooth est obligatoire.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bleManager = BleManager(this)
        checkAndRequestBluetoothPermissions()

        // --- LOGIQUE RESTER CONNECTÉ ---
        val user = FirebaseAuth.getInstance().currentUser
        val initialStartDestination = if (user != null) "dashboard" else "login"

        setContent {
            val viewModel: SautyViewModel = viewModel()

            // ON BRANCHE LE TUYAU : Quand le BleManager envoie un message, on le donne au ViewModel.
            bleManager.onStatusMessage = { message ->
                viewModel.updateStatus(message)
            }

            // Forçage du Mode Sombre sur toute l'application
            MaterialTheme(colorScheme = darkColorScheme()) {
                // 1. CRÉATION DE L'AIGUILLEUR (NavController)
                val navController = rememberNavController()

                // On observe sur quel écran on se trouve actuellement
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // On gère les écrans qui ont le droit d'afficher la barre du bas (Profil retiré)
                val showBottomBar = currentRoute in listOf("dashboard", "scan")

                // 2. LE SCAFFOLD : La structure principale qui contient la barre de navigation
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
                    // 3. DÉFINITION DU CHEMIN DE NAVIGATION
                    NavHost(
                        navController = navController,
                        startDestination = initialStartDestination,
                        modifier = Modifier.padding(innerPadding)
                    ) {

                        // ÉCRAN 1 : CONNEXION
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

                        // ÉCRAN 2 : INSCRIPTION
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

                        // ÉCRAN 3 : LE DASHBOARD (RÉSUMÉ)
                        composable("dashboard") {
                            DashboardScreen(
                                onProfileClick = { navController.navigate("profile") },
                                onActivityRingsClick = { navController.navigate("activityDetails") },
                                onSessionClick = { navController.navigate("sessionDetail") }
                            )
                        }

                        // ÉCRAN 4 : SCANNER BLUETOOTH
                        composable("scan") {
                            SautyScanScreen(
                                onStartScanClick = {
                                    bleManager.startScan()
                                },
                                onDisconnectClick = {
                                    bleManager.disconnectAndForget() // Déclenche l'oubli
                                },
                                viewModel = viewModel
                            )
                        }

                        // ÉCRAN 5 : PROFIL
                        composable("profile") {
                            ProfileScreen(
                                onLogout = {
                                    navController.navigate("login") {
                                        // On efface tout l'historique pour empêcher le retour en arrière
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // ÉCRAN 6 : DÉTAIL DES ANNEAUX (Nom de route corrigé)
                        composable("activityDetails") {
                            ActivityDetailsScreen(
                                onBackClick = {
                                    navController.popBackStack() // Retour en arrière
                                }
                            )
                        }

                        // ÉCRAN 7 : DÉTAIL DE LA SESSION (Route ajoutée)
                        composable("sessionDetail") {
                            SessionDetailScreen(
                                onBackClick = {
                                    navController.popBackStack() // Retour en arrière
                                }
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

// COMPOSANT SCANNER MIS À JOUR
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