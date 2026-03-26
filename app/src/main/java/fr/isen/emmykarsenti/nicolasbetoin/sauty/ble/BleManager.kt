package fr.isen.emmykarsenti.nicolasbetoin.sauty.ble
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log

@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {

    private val bluetoothManager: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bleScanner = bluetoothAdapter?.bluetoothLeScanner

    // Notre tunnel de communication
    private var bluetoothGatt: BluetoothGatt? = null

    // Un "tuyau" pour envoyer des messages à l'interface (ViewModel)
    var onStatusMessage: ((String) -> Unit)? = null

    // 1. L'ÉCOUTEUR DE CONNEXION GATT
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("SAUTY_BLE", "Connecté au STM32 !")
                onStatusMessage?.invoke("Connecté au STM32 !")

                // Étape obligatoire : demander au STM32 ce qu'il sait faire (ses "Services")
                gatt.discoverServices()

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("SAUTY_BLE", "Déconnecté.")
                onStatusMessage?.invoke("Appareil déconnecté")
                gatt.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("SAUTY_BLE", "Services découverts ! Prêt à lire les données.")
                onStatusMessage?.invoke("Prêt à recevoir les sauts")
            }
        }
    }

    // 2. LE RESTE DU SCANNER (Légèrement modifié)
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val deviceName = result.device.name ?: "Inconnu"

            // METS ICI LE NOM EXACT DE TA CARTE STM32 (ex: "SAUTY", "P2P_SERVER", etc.)
            if (deviceName == "SAUTY_STM32") {
                Log.d("SAUTY_SCAN", "BRACELET TROUVÉ ! Arrêt du scan et connexion...")
                stopScan()
                connectToDevice(result.device)
            }
        }
    }

    fun startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return
        onStatusMessage?.invoke("Recherche du bracelet en cours...")
        bleScanner?.startScan(scanCallback)
    }

    fun stopScan() {
        bleScanner?.stopScan(scanCallback)
    }

    // 3. LA NOUVELLE FONCTION DE CONNEXION
    private fun connectToDevice(device: BluetoothDevice) {
        onStatusMessage?.invoke("Connexion en cours...")
        // On lance la connexion GATT
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }
}