package fr.isen.emmykarsenti.nicolasbetoin.sauty.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
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

    var onStatusMessage: ((String) -> Unit)? = null

    // LES VRAIS UUIDS EXTRAITS DE LA CARTE STM32
    private val SERVICE_UUID = UUID.fromString("00000000-cc7a-482a-984a-7f2ed5b3e58f")

    // UUID de la caractéristique Sauts
    private val JUMPS_CHAR_UUID = UUID.fromString("00000000-8e22-4541-9d4c-21edae82ed19")

    // UUID de la caractéristique Calories (Actuellement identique aux Sauts, à modifier quand ton binôme aura corrigé la carte)
    private val CALORIES_CHAR_UUID = UUID.fromString("00000000-8e22-4541-9d4c-21edae82ed19")

    // UUID standard universel pour activer les notifications (CCCD - ne pas modifier)
    private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // Flux de données observables pour Jetpack Compose
    private val _jumpsState = MutableStateFlow(0)
    val jumpsState: StateFlow<Int> = _jumpsState

    private val _caloriesState = MutableStateFlow(0)
    val caloriesState: StateFlow<Int> = _caloriesState

    // 1. L'ÉCOUTEUR DE CONNEXION GATT
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("SAUTY_BLE", "Connecté au STM32 !")
                onStatusMessage?.invoke("Connecté au STM32 !")

                val deviceAddress = gatt.device?.address
                if (deviceAddress != null) {
                    sharedPreferences.edit().putString("MAC_ADDRESS", deviceAddress).apply()
                }

                // Découverte des services
                gatt.discoverServices()

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("SAUTY_BLE", "Déconnecté.")
                onStatusMessage?.invoke("Appareil déconnecté")
                gatt.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("SAUTY_BLE", "Services découverts !")
                onStatusMessage?.invoke("Prêt à recevoir les données")

                // Active les notifications dès que la carte est prête
                enableNotifications(gatt)
            }
        }

        // Réception des données pour les versions Android 12 et inférieures
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val dataBytes = characteristic.value ?: return
            processCharacteristicData(characteristic.uuid, dataBytes)
        }

        // Réception des données pour les versions Android 13 et supérieures
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            processCharacteristicData(characteristic.uuid, value)
        }
    }

    // Fonction commune pour traiter les données reçues
    private fun processCharacteristicData(uuid: UUID, bytes: ByteArray) {
        val decodedValue = decodeUint16(bytes)
        when (uuid) {
            JUMPS_CHAR_UUID -> {
                _jumpsState.value = decodedValue
                Log.d("SAUTY_DATA", "Sauts : $decodedValue")
            }
            // 🚨 Tant que l'UUID est identique, ce bloc ne sera jamais appelé correctement.
            // Les calories s'afficheront dans les sauts.
            CALORIES_CHAR_UUID -> {
                _caloriesState.value = decodedValue
                Log.d("SAUTY_DATA", "Calories : $decodedValue")
            }
        }
    }

    // 2. LE RESTE DU SCANNER
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val deviceName = result.device.name ?: "Inconnu"

            if (deviceName == "SAUTY_STM32") {
                Log.d("SAUTY_SCAN", "BRACELET TROUVÉ ! Arrêt du scan et connexion...")
                stopScan()
                connectToDevice(result.device)
            }
        }
    }

    fun startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            onStatusMessage?.invoke("Veuillez activer le Bluetooth")
            return
        }
        onStatusMessage?.invoke("Recherche du bracelet en cours...")
        bleScanner?.startScan(scanCallback)
    }

    fun stopScan() {
        bleScanner?.stopScan(scanCallback)
    }

    private fun connectToDevice(device: BluetoothDevice) {
        onStatusMessage?.invoke("Connexion en cours...")
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    // 3. FONCTIONS UTILITAIRES POUR LE TRAITEMENT DES DONNÉES
    private fun enableNotifications(gatt: BluetoothGatt) {
        val service = gatt.getService(SERVICE_UUID) ?: return

        // Abonnement aux sauts
        val jumpsChar = service.getCharacteristic(JUMPS_CHAR_UUID)
        if (jumpsChar != null) {
            gatt.setCharacteristicNotification(jumpsChar, true)
            val descriptor = jumpsChar.getDescriptor(CCCD_UUID)
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
        }

        // Note : Sous Android, il est recommandé de s'abonner aux différentes caractéristiques
        // les unes après les autres (via onDescriptorWrite) plutôt qu'en même temps,
        // mais nous pourrons optimiser cela quand l'UUID des calories sera corrigé.
    }

    // Convertit les octets envoyés par la carte (Little-Endian) en nombre entier
    private fun decodeUint16(bytes: ByteArray): Int {
        if (bytes.size < 2) return 0
        return (bytes[1].toInt() and 0xFF shl 8) or (bytes[0].toInt() and 0xFF)
    }

    // 4. AUTO-CONNEXION AU DÉMARRAGE
    fun tryAutoConnect(): Boolean {
        val savedMacAddress = sharedPreferences.getString("MAC_ADDRESS", null)
        if (savedMacAddress != null && bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
            try {
                val device = bluetoothAdapter.getRemoteDevice(savedMacAddress)
                bluetoothGatt = device.connectGatt(context, true, gattCallback)
                onStatusMessage?.invoke("En attente du bracelet connu...")
                return true
            } catch (e: Exception) {
                Log.e("SAUTY_BLE", "Erreur AutoConnect", e)
            }
        }
        return false
    }

    // 5. DÉCONNEXION ET OUBLI DU BRACELET
    fun disconnectAndForget() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
        } catch (e: Exception) {
            Log.e("SAUTY_BLE", "Erreur lors de la déconnexion", e)
        }

        sharedPreferences.edit().remove("MAC_ADDRESS").apply()
        onStatusMessage?.invoke("Bracelet oublié. Prêt à scanner.")
    }
}