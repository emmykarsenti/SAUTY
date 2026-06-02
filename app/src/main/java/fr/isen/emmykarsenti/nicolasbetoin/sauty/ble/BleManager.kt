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

/**
 * Gestionnaire Bluetooth Low Energy (BLE) pour l'application Sauty.
 * Gère la recherche d'appareils, la connexion GATT, la reconnexion automatique,
 * et la réception des notifications (sauts, calories) depuis le bracelet intelligent.
 */
@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bleScanner = bluetoothAdapter?.bluetoothLeScanner
    private val sharedPreferences = context.getSharedPreferences("SautyPrefs", Context.MODE_PRIVATE)

    private var bluetoothGatt: BluetoothGatt? = null
    var onStatusMessage: ((String) -> Unit)? = null

    // UUIDs spécifiques au bracelet SAUTY
    private val SERVICE_UUID = UUID.fromString("00000000-cc7a-482a-984a-7f2ed5b3e58f")
    private val JUMPS_CHAR_UUID = UUID.fromString("00000000-8e22-4541-9d4c-21edae82ed19")
    private val CALORIES_CHAR_UUID = UUID.fromString("00000000-8e22-4541-9d4c-21edae82ed20")
    private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // État réactif de la liste des appareils trouvés lors du scan
    private val _foundDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val foundDevices: StateFlow<List<BluetoothDevice>> = _foundDevices

    // État réactif du nombre de sauts
    private val _jumpsState = MutableStateFlow(0)
    val jumpsState: StateFlow<Int> = _jumpsState

    // État réactif des calories brûlées
    private val _caloriesState = MutableStateFlow(0)
    val caloriesState: StateFlow<Int> = _caloriesState

    // État réactif de la connexion BLE
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val charQueue = ArrayDeque<UUID>()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val currentList = _foundDevices.value.toMutableList()
            if (!currentList.any { it.address == device.address }) {
                currentList.add(device)
                _foundDevices.value = currentList
            }
        }
    }

    /**
     * Lance le scan des périphériques Bluetooth Low Energy à proximité.
     * Met à jour la liste [foundDevices] au fur et à mesure des découvertes.
     */
    fun startScan() {
        if (bluetoothAdapter?.isEnabled == false) {
            onStatusMessage?.invoke("Activez le Bluetooth")
            return
        }
        _foundDevices.value = emptyList()
        onStatusMessage?.invoke("Recherche d'appareils...")
        bleScanner?.startScan(scanCallback)
    }

    /**
     * Connecte l'application à un appareil BLE spécifique via GATT.
     * @param device L'appareil Bluetooth cible.
     * @param autoConnect Si true, sauvegarde l'adresse MAC pour de futures reconnexions.
     */
    fun connectToDevice(device: BluetoothDevice, autoConnect: Boolean) {
        bleScanner?.stopScan(scanCallback)
        if (autoConnect) {
            sharedPreferences.edit().putString("MAC_ADDRESS", device.address).apply()
        }
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    /**
     * Tente de se reconnecter automatiquement au dernier appareil sauvegardé en mémoire locale.
     */
    fun tryAutoConnect() {
        val savedMac = sharedPreferences.getString("MAC_ADDRESS", null)
        if (savedMac != null && bluetoothAdapter?.isEnabled == true) {
            onStatusMessage?.invoke("Reconnexion automatique...")
            val device = bluetoothAdapter.getRemoteDevice(savedMac)
            bluetoothGatt = device.connectGatt(context, true, gattCallback)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            android.util.Log.d("BLE_DEBUG", "onConnectionStateChange — status=$status newState=$newState")
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _isConnected.value = true
                android.util.Log.d("BLE_DEBUG", "Connecté, lancement discoverServices")
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onStatusMessage?.invoke("Connecté au SAUTY !")
                }
                try {
                    val refresh = gatt.javaClass.getMethod("refresh")
                    refresh.invoke(gatt)
                } catch (e: Exception) {
                    Log.d("BLE_DEBUG", "refresh failed: ${e.message}")
                }
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    gatt.discoverServices()
                }, 300)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _isConnected.value = false
                android.util.Log.d("BLE_DEBUG", "Déconnecté")
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onStatusMessage?.invoke("Bracelet déconnecté")
                }
                if (sharedPreferences.getString("MAC_ADDRESS", null) == null) {
                    gatt.close()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            android.util.Log.d("BLE_DEBUG", "onServicesDiscovered — status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                enableNotifications(gatt)
            } else {
                android.util.Log.d("BLE_DEBUG", "Erreur discovery, reconnexion...")
                gatt.disconnect()
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            processData(characteristic.uuid, value)
        }

        @Deprecated("Deprecated for Android 13+")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            processData(characteristic.uuid, characteristic.value)
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            android.util.Log.d("BLE_DEBUG", "onDescriptorWrite — status=$status uuid=${descriptor.characteristic.uuid}")
            enableNextNotification(gatt)
        }
    }

    /**
     * Traite les données brutes reçues depuis les caractéristiques du bracelet.
     */
    private fun processData(uuid: UUID, value: ByteArray?) {
        android.util.Log.d("BLE_DEBUG", "processData — uuid=$uuid")
        if (value == null || value.isEmpty()) return
        if (uuid == JUMPS_CHAR_UUID) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onStatusMessage?.invoke("ACTION_JUMP")
            }
        }
    }

    /**
     * Active les notifications pour la prochaine caractéristique dans la file d'attente.
     * Nécessaire car l'écriture de descripteurs BLE doit se faire séquentiellement.
     */
    private fun enableNextNotification(gatt: BluetoothGatt) {
        val uuid = charQueue.removeFirstOrNull() ?: run {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onStatusMessage?.invoke("Données synchronisées")
            }
            return
        }
        val service = gatt.getService(SERVICE_UUID) ?: return
        val char = service.getCharacteristic(uuid) ?: run {
            android.util.Log.d("BLE_DEBUG", "Caractéristique $uuid NON trouvée")
            enableNextNotification(gatt)
            return
        }
        gatt.setCharacteristicNotification(char, true)
        val desc = char.getDescriptor(CCCD_UUID) ?: run {
            android.util.Log.d("BLE_DEBUG", "CCCD NON trouvé pour $uuid")
            enableNextNotification(gatt)
            return
        }
        desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        val result = gatt.writeDescriptor(desc)
        android.util.Log.d("BLE_DEBUG", "writeDescriptor pour $uuid — résultat=$result")
    }

    /**
     * Initialise la file d'attente pour activer les notifications (sauts et calories).
     */
    private fun enableNotifications(gatt: BluetoothGatt) {
        val service = gatt.getService(SERVICE_UUID) ?: run {
            android.util.Log.d("BLE_DEBUG", "Service NON trouvé")
            return
        }
        charQueue.clear()
        charQueue.add(JUMPS_CHAR_UUID)
        charQueue.add(CALORIES_CHAR_UUID)
        enableNextNotification(gatt)
    }

    /**
     * Déconnecte l'appareil actuel et supprime son adresse MAC des préférences
     * pour empêcher la reconnexion automatique.
     */
    fun disconnectAndForget() {
        sharedPreferences.edit().remove("MAC_ADDRESS").apply()
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _isConnected.value = false
        onStatusMessage?.invoke("Bracelet oublié")
    }
}