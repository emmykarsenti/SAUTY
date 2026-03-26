package fr.isen.emmykarsenti.nicolasbetoin.sauty

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.isen.emmykarsenti.nicolasbetoin.sauty.ble.BleManager
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.isen.emmykarsenti.nicolasbetoin.sauty.viewmodel.SautyViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState

class MainActivity : ComponentActivity() {

    private lateinit var bleManager: BleManager

    // Le lanceur pour les permissions (comme avant)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.entries.all { it.value }) {
            Toast.makeText(this, "Permissions BLE accordées ! \uD83D\uDE80", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Erreur : Le Bluetooth est obligatoire.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bleManager = BleManager(this)
        checkAndRequestBluetoothPermissions()

        setContent {
            // On récupère ou on crée le ViewModel ici, au plus haut niveau
            val viewModel: SautyViewModel = viewModel()

            // ON BRANCHE LE TUYAU : Quand le BleManager envoie un message, on le donne au ViewModel
            bleManager.onStatusMessage = { message ->
                viewModel.updateStatus(message)
            }

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SautyScanScreen(
                        onStartScanClick = {
                            bleManager.startScan()
                        },
                        viewModel = viewModel // On passe le viewModel à l'écran
                    )
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
    viewModel: SautyViewModel = viewModel()
) {
    // On "observe" le statut en temps réel
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

        // On affiche le statut dynamique géré par le ViewModel
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                viewModel.startScanning() // On met à jour le texte
                onStartScanClick()        // On lance le VRAI scan Bluetooth
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "SCANNER", style = MaterialTheme.typography.titleMedium)
        }
    }
}