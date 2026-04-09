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

    // NOUVEAU : Mémoire pour l'auto-connexion
    private val sharedPreferences = context.getSharedPreferences("SautyPrefs", Context.MODE_PRIVATE)

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

                // NOUVEAU : SAUVEGARDE DE L'ADRESSE MAC POUR L'AUTO-CONNEXION
                val deviceAddress = gatt.device?.address
                if (deviceAddress != null) {
                    sharedPreferences.edit().putString("MAC_ADDRESS", deviceAddress).apply()
                }

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

    // 2. LE RESTE DU SCANNER
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

    // 3. LA FONCTION DE CONNEXION CLASSIQUE (Suite au scan manuel)
    private fun connectToDevice(device: BluetoothDevice) {
        onStatusMessage?.invoke("Connexion en cours...")
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    // 4. NOUVEAU : AUTO-CONNEXION AU DÉMARRAGE
    fun tryAutoConnect(): Boolean {
        val savedMacAddress = sharedPreferences.getString("MAC_ADDRESS", null)
        if (savedMacAddress != null && bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
            try {
                val device = bluetoothAdapter.getRemoteDevice(savedMacAddress)
                // autoConnect = true -> le téléphone se connectera automatiquement dès que le STM32 sera allumé et à portée
                bluetoothGatt = device.connectGatt(context, true, gattCallback)
                onStatusMessage?.invoke("En attente du bracelet connu...")
                return true
            } catch (e: Exception) {
                Log.e("SAUTY_BLE", "Erreur AutoConnect", e)
            }
        }
        return false // Aucun bracelet en mémoire
    }

    // 5. NOUVEAU : DÉCONNEXION ET OUBLI DU BRACELET (Bouton manuel)
    fun disconnectAndForget() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
        } catch (e: Exception) {
            Log.e("SAUTY_BLE", "Erreur lors de la déconnexion", e)
        }

        // On efface la mémoire pour empêcher la reconnexion automatique
        sharedPreferences.edit().remove("MAC_ADDRESS").apply()
        onStatusMessage?.invoke("Bracelet oublié. Prêt à scanner.")
    }
}