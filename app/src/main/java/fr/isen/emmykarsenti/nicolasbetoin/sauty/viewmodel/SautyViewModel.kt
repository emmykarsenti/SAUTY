package fr.isen.emmykarsenti.nicolasbetoin.sauty.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SautyViewModel : ViewModel() {

    // On crée une variable "réactive". Dès qu'elle change, l'interface se mettra à jour toute seule !
    private val _connectionStatus = MutableStateFlow("Prêt à scanner")
    val connectionStatus: StateFlow<String> = _connectionStatus

    // Fonction appelée quand on clique sur le bouton
    fun startScanning() {
        _connectionStatus.value = "Recherche du STM32 en cours..."
    }

    // Fonction qui sera appelée quand le BleManager trouvera la carte
    fun deviceFound(deviceName: String) {
        _connectionStatus.value = "Appareil trouvé : $deviceName !"
    }
    // Cette fonction sera appelée directement par le BleManager
    fun updateStatus(newStatus: String) {
        _connectionStatus.value = newStatus
    }
}
