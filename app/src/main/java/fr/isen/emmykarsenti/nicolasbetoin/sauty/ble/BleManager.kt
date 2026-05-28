package fr.isen.emmykarsenti.nicolasbetoin.sauty.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {

    private val bluetoothManager: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bleScanner = bluetoothAdapter?.bluetoothLeScanner

    private val sharedPreferences = context.getSharedPreferences("SautyPrefs", Context.MODE_PRIVATE)
    private var bluetoothGatt: BluetoothGatt? = null

    // Callbacks pour l'interface utilisateur
    var onStatusMessage: ((String) -> Unit)? = null

    // CONFIGURATION UUID (Doit correspondre au .ioc du STM32)
    private val SERVICE_UUID = UUID.fromString("00000000-cc7a-482a-984a-7f2ed5b3e58f")
    private val JUMPS_CHAR_UUID = UUID.fromString("00000000-8e22-4541-9d4c-21edae82ed19")
    private val CALORIES_CHAR_UUID = UUID.fromString("00000000-8e22-4541-9d4c-21edae82ed20")
    private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // ÉTATS OBSERVABLES (UI)
    private val _foundDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val foundDevices: StateFlow<List<BluetoothDevice>> = _foundDevices

    private val _jumpsState = MutableStateFlow(0)
    val jumpsState: StateFlow<Int> = _jumpsState

    private val _caloriesState = MutableStateFlow(0)
    val caloriesState: StateFlow<Int> = _caloriesState

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    // 1. GESTION DU SCAN
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val deviceName = device.name ?: "Inconnu"

            // On filtre pour n'afficher que les appareils de ton projet
            if (deviceName.contains("SAUTY", ignoreCase = true)) {
                val currentList = _foundDevices.value.toMutableList()
                if (!currentList.any { it.address == device.address }) {
                    currentList.add(device)
                    _foundDevices.value = currentList
                    Log.d("SAUTY_SCAN", "Appareil trouvé : $deviceName (${device.address})")
                }
            }
        }
    }

    fun startScan() {
        if (bluetoothAdapter?.isEnabled == false) {
            onStatusMessage?.invoke("Activez le Bluetooth")
            return
        }
        _foundDevices.value = emptyList() // Reset de la liste
        onStatusMessage?.invoke("Recherche de bracelets...")
        bleScanner?.startScan(scanCallback)
    }

    fun stopScan() {
        bleScanner?.stopScan(scanCallback)
    }

    // 2. CONNEXION ET MÉMORISATION
    fun connectToDevice(device: BluetoothDevice) {
        stopScan()
        onStatusMessage?.invoke("Connexion à ${device.name ?: "Bracelet"}...")

        // MÉMORISATION : On sauvegarde l'adresse MAC pour la prochaine fois
        sharedPreferences.edit().putString("MAC_ADDRESS", device.address).apply()

        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    // AUTO-CONNEXION (Le mode "Apple")
    fun tryAutoConnect(): Boolean {
        val savedMacAddress = sharedPreferences.getString("MAC_ADDRESS", null)
        if (savedMacAddress != null && bluetoothAdapter?.isEnabled == true) {
            try {
                val device = bluetoothAdapter.getRemoteDevice(savedMacAddress)
                onStatusMessage?.invoke("Reconnexion automatique...")
                // autoConnect = true permet au téléphone de se connecter dès que la carte est à portée
                bluetoothGatt = device.connectGatt(context, true, gattCallback)
                return true
            } catch (e: Exception) {
                Log.e("SAUTY_BLE", "Erreur AutoConnect", e)
            }
        }
        return false
    }

    // 3. CALLBACKS GATT (COMMUNICATION)
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _isConnected.value = true
                onStatusMessage?.invoke("Connecté !")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _isConnected.value = false
                onStatusMessage?.invoke("Déconnecté")
                gatt.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                enableNotifications(gatt)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            val decodedValue = decodeUint16(value)
            when (characteristic.uuid) {
                JUMPS_CHAR_UUID -> _jumpsState.value = decodedValue
                CALORIES_CHAR_UUID -> _caloriesState.value = decodedValue
            }
        }
    }

    private fun enableNotifications(gatt: BluetoothGatt) {
        val service = gatt.getService(SERVICE_UUID) ?: return

        // Liste des caractéristiques à écouter
        val characteristics = listOf(JUMPS_CHAR_UUID, CALORIES_CHAR_UUID)

        characteristics.forEach { uuid ->
            val char = service.getCharacteristic(uuid)
            if (char != null) {
                gatt.setCharacteristicNotification(char, true)
                val descriptor = char.getDescriptor(CCCD_UUID)
                if (descriptor != null) {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
                    Thread.sleep(100)
                }
            }
        }
    }

    // 4. UTILITAIRES
    private fun decodeUint16(bytes: ByteArray): Int {
        if (bytes.size < 2) return 0
        // Little-endian : le premier octet est le poids faible
        return (bytes[1].toInt() and 0xFF shl 8) or (bytes[0].toInt() and 0xFF)
    }

    fun disconnectAndForget() {
        sharedPreferences.edit().remove("MAC_ADDRESS").apply()
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _isConnected.value = false
        onStatusMessage?.invoke("Bracelet oublié")
    }
}