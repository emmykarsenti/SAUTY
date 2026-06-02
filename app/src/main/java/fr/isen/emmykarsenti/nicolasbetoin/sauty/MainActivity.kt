package fr.isen.emmykarsenti.nicolasbetoin.sauty

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import fr.isen.emmykarsenti.nicolasbetoin.sauty.ui.SessionData
import fr.isen.emmykarsenti.nicolasbetoin.sauty.ui.SessionDetailScreen
import fr.isen.emmykarsenti.nicolasbetoin.sauty.ui.TrendsDetailScreen
import fr.isen.emmykarsenti.nicolasbetoin.sauty.ui.WorkoutScreen
import fr.isen.emmykarsenti.nicolasbetoin.sauty.viewmodel.SautyViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Point d'entrée principal de l'application
// Gère le cycle de vie de base, l'initialisation du Bluetooth et la structure de navigation entre les écrans
class MainActivity : ComponentActivity() {

    private lateinit var bleManager: BleManager

    // Gestionnaire des retours de demandes de permissions
    // S'assure que l'utilisateur a bien accordé l'accès au Bluetooth avant de continuer
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.entries.all { it.value }) {
            Toast.makeText(this, "Permissions BLE accordées ! 🚀", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Erreur : Le Bluetooth est obligatoire.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bleManager = BleManager(this)
        checkAndRequestBluetoothPermissions()

        // Vérifie si une session Firebase est déjà active pour rediriger l'utilisateur
        // directement vers le tableau de bord au lieu de l'écran de connexion
        val user = FirebaseAuth.getInstance().currentUser
        val initialStartDestination = if (user != null) "dashboard" else "login"

        setContent {
            val viewModel: SautyViewModel = viewModel()
            val bleJumps by bleManager.jumpsState.collectAsState()
            val bleCalories by bleManager.caloriesState.collectAsState()

            // Pont de communication entre le gestionnaire Bluetooth (hardware) et le ViewModel (logique métier)
            // Ce bloc écoute les changements d'état du module BLE et transmet les messages au ViewModel
            LaunchedEffect(Unit) {
                bleManager.onStatusMessage = { message ->
                    Log.d("BLE_DEBUG", "onStatusMessage reçu : $message")
                    viewModel.updateStatus(message)
                }
                bleManager.tryAutoConnect()
            }

            MaterialTheme(colorScheme = darkColorScheme()) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // La barre de navigation du bas ne doit être visible que sur certains écrans principaux
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
                    // Définition du graphe de navigation de l'application
                    // Associe chaque route texte à son composant graphique correspondant
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
                                bleManager = bleManager,
                                onStartScanClick = { bleManager.startScan() },
                                onDisconnectClick = { bleManager.disconnectAndForget() },
                                viewModel = viewModel
                            )
                        }

                        composable("profile") {
                            ProfileScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
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

                        // Écran affichant les détails d'une séance spécifique
                        // Traite les données brutes pour les adapter à l'affichage
                        composable("sessionDetail") {
                            val timerString by viewModel.timerString.collectAsState()
                            val jumpsCount by viewModel.jumpsCount.collectAsState()
                            val calories by viewModel.calories.collectAsState()
                            val isRunning by viewModel.isRunning.collectAsState()
                            val firebaseSessions by viewModel.sessions.collectAsState()

                            // Détermine la session principale à afficher en haut de l'écran
                            // Priorité : la session en cours d'enregistrement, sinon la dernière session enregistrée en base
                            val currentSession = if (isRunning || jumpsCount > 0) {
                                fr.isen.emmykarsenti.nicolasbetoin.sauty.ui.SessionData(
                                    id = "live",
                                    date = java.text.SimpleDateFormat("EEE dd MMM", Locale.FRANCE).format(java.util.Date()),
                                    timeRange = "Session en cours",
                                    durationStr = timerString,
                                    totalJumps = jumpsCount,
                                    jumpsPerMin = 0,
                                    kcal = calories,
                                    jumpsProgress = jumpsCount.toFloat() / viewModel.targetJumps.coerceAtLeast(1),
                                    timeProgress = 0f,
                                    kcalProgress = calories.toFloat() / viewModel.targetKcal.coerceAtLeast(1)
                                )
                            } else if (firebaseSessions.isNotEmpty()) {
                                val mostRecent = firebaseSessions.first()
                                fr.isen.emmykarsenti.nicolasbetoin.sauty.ui.SessionData(
                                    id = mostRecent.date + mostRecent.timeRange,
                                    date = mostRecent.date,
                                    timeRange = mostRecent.timeRange,
                                    durationStr = String.format(Locale.FRANCE, "%02d:%02d", mostRecent.durationSeconds / 60, mostRecent.durationSeconds % 60),
                                    totalJumps = mostRecent.jumpsTotal,
                                    jumpsPerMin = mostRecent.avgCadence,
                                    kcal = mostRecent.calories,
                                    jumpsProgress = mostRecent.jumpsTotal.toFloat() / viewModel.targetJumps.coerceAtLeast(1),
                                    timeProgress = (mostRecent.durationSeconds / 60f) / viewModel.targetMinutes.coerceAtLeast(1).toFloat(),
                                    kcalProgress = mostRecent.calories.toFloat() / viewModel.targetKcal.coerceAtLeast(1)
                                )
                            } else null

                            // Prépare la liste de l'historique sous forme de carrousel
                            // On ignore le premier élément s'il est déjà affiché en tant que session principale
                            val listToMap = if (!isRunning && jumpsCount == 0 && firebaseSessions.isNotEmpty()) {
                                firebaseSessions.drop(1)
                            } else {
                                firebaseSessions
                            }

                            val pastSessions = listToMap.map { workout ->
                                fr.isen.emmykarsenti.nicolasbetoin.sauty.ui.SessionData(
                                    id = workout.date + workout.timeRange,
                                    date = workout.date,
                                    timeRange = workout.timeRange,
                                    durationStr = String.format(Locale.FRANCE, "%02d:%02d", workout.durationSeconds / 60, workout.durationSeconds % 60),
                                    totalJumps = workout.jumpsTotal,
                                    jumpsPerMin = workout.avgCadence,
                                    kcal = workout.calories,
                                    jumpsProgress = workout.jumpsTotal.toFloat() / viewModel.targetJumps.coerceAtLeast(1),
                                    timeProgress = (workout.durationSeconds / 60f) / viewModel.targetMinutes.coerceAtLeast(1).toFloat(),
                                    kcalProgress = workout.calories.toFloat() / viewModel.targetKcal.coerceAtLeast(1)
                                )
                            }
                            SessionDetailScreen(
                                currentSession = currentSession,
                                pastSessions = pastSessions.take(5),
                                onBackClick = { navController.popBackStack() },
                                onHistoryClick = { navController.navigate("fullHistory") }
                            )
                        }

                        // Écran listant l'intégralité des sessions passées de l'utilisateur
                        composable("fullHistory") {
                            val firebaseSessions by viewModel.sessions.collectAsState()

                            val allSessionsData = firebaseSessions.map { workout ->
                                fr.isen.emmykarsenti.nicolasbetoin.sauty.ui.SessionData(
                                    id = workout.date + workout.timeRange,
                                    date = workout.date,
                                    timeRange = workout.timeRange,
                                    durationStr = String.format(Locale.FRANCE, "%02d:%02d", workout.durationSeconds / 60, workout.durationSeconds % 60),
                                    totalJumps = workout.jumpsTotal,
                                    jumpsPerMin = workout.avgCadence,
                                    kcal = workout.calories,
                                    jumpsProgress = workout.jumpsTotal.toFloat() / viewModel.targetJumps.coerceAtLeast(1),
                                    timeProgress = (workout.durationSeconds / 60f) / viewModel.targetMinutes.coerceAtLeast(1).toFloat(),
                                    kcalProgress = workout.calories.toFloat() / viewModel.targetKcal.coerceAtLeast(1)
                                )
                            }

                            Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                                    }
                                    Text("Historique Complet", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    items(allSessionsData) { session ->
                                        fr.isen.emmykarsenti.nicolasbetoin.sauty.ui.PastSessionCard(session = session)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Gère la complexité des permissions Bluetooth introduite par les différentes versions d'Android
    // Android 12+ nécessite des permissions spécifiques au Bluetooth, tandis que les versions antérieures lient cela à la localisation
    private fun checkAndRequestBluetoothPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        requestPermissionLauncher.launch(permissions)
    }
}

// Interface utilisateur dédiée au scan et à la gestion des appareils Bluetooth
@SuppressLint("MissingPermission")
@Composable
fun SautyScanScreen(
    bleManager: BleManager,
    onStartScanClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    viewModel: SautyViewModel = viewModel()
) {
    val statusText by viewModel.connectionStatus.collectAsState()
    val scannedDevices by bleManager.foundDevices.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    var autoConnectChecked by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.White)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_sauty),
                contentDescription = "Logo Sauty",
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "SAUTY CONNECT",
            color = Color(0xFFFA9E1E),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(text = statusText, color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    viewModel.startScanning()
                    onStartScanClick()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF521E))
            ) {
                Text("SCANNER")
            }
            Button(
                onClick = { onDisconnectClick() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
            ) {
                Text("OUBLIER")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Liste dynamique mettant à jour les appareils détectés en temps réel
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(scannedDevices) { device ->
                DeviceItemSecure(
                    device = device,
                    onClick = {
                        selectedDevice = device
                        showDialog = true
                    }
                )
            }
        }
    }

    // Boîte de dialogue de confirmation qui s'affiche lors de la sélection d'un appareil dans la liste
    // Permet de valider la connexion et d'activer/désactiver la mémorisation pour les sessions futures
    if (showDialog && selectedDevice != null) {
        val safeName = try {
            selectedDevice?.name ?: "Appareil Inconnu"
        } catch (e: Exception) {
            "Inconnu"
        }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Connexion au bracelet", color = Color.White) },
            text = {
                Column {
                    Text(
                        text = "Voulez-vous connecter : $safeName ?",
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { autoConnectChecked = !autoConnectChecked }
                    ) {
                        Checkbox(
                            checked = autoConnectChecked,
                            onCheckedChange = { autoConnectChecked = it },
                            colors = CheckboxDefaults.colors(
                                checkmarkColor = Color.Black,
                                checkedColor = Color(0xFFFA9E1E),
                                uncheckedColor = Color.Gray
                            )
                        )
                        Text("Connexion automatique", color = Color.White)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        selectedDevice?.let {
                            bleManager.connectToDevice(it, autoConnectChecked)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFA9E1E))
                ) {
                    Text("SE CONNECTER")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("ANNULER", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }
}

// Composant d'interface pour afficher un appareil dans la liste de résultats du scan
// Intègre une gestion d'erreurs (SecurityException) pour éviter un crash si l'accès au nom de l'appareil est restreint
@Composable
fun DeviceItemSecure(device: BluetoothDevice, onClick: () -> Unit) {
    val safeName = remember(device) {
        try {
            device.name ?: "Appareil inconnu"
        } catch (e: SecurityException) {
            "Accès refusé (Permission)"
        } catch (e: Exception) {
            "Erreur lecture nom"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = safeName, color = Color.White, fontWeight = FontWeight.Bold)
            Text(text = device.address, color = Color.Gray, fontSize = 12.sp)
        }
    }
}