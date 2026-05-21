package fr.isen.emmykarsenti.nicolasbetoin.sauty.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import fr.isen.emmykarsenti.nicolasbetoin.sauty.model.DailyTrendData
import fr.isen.emmykarsenti.nicolasbetoin.sauty.model.WorkoutSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SautyViewModel : ViewModel() {

    // CONNEXION RÉELLE FIREBASE DATABASE
    private val database = FirebaseDatabase.getInstance()

    // Référence dynamique qui s'adaptera à l'utilisateur connecté
    private var historyRef: DatabaseReference? = null
    private var firebaseListener: ValueEventListener? = null

    private val _connectionStatus = MutableStateFlow("Déconnecté")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    // ENREGISTREMENT DE L'EXERCICE EN TEMPS RÉEL (FLUX MATÉRIEL STM32)
    private val _timerString = MutableStateFlow("00:00")
    val timerString: StateFlow<String> = _timerString.asStateFlow()

    private val _jumpsCount = MutableStateFlow(0)
    val jumpsCount: StateFlow<Int> = _jumpsCount.asStateFlow()

    private val _doubleJumpsCount = MutableStateFlow(0)
    val doubleJumpsCount: StateFlow<Int> = _doubleJumpsCount.asStateFlow()

    private val _calories = MutableStateFlow(0)
    val calories: StateFlow<Int> = _calories.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var timerJob: Job? = null
    private var timeInSeconds = 0

    // OBJECTIFS JOURNALIERS CONFIGURÉS
    val targetJumps = 2000
    val targetMinutes = 30
    val targetKcal = 300

    // SESSIONS ENREGISTRÉES (Alimenté en temps réel par Firebase !)
    private val _sessions = MutableStateFlow<List<WorkoutSession>>(emptyList())
    val sessions: StateFlow<List<WorkoutSession>> = _sessions.asStateFlow()

    // DONNÉES HEBDOMADAIRES DES TENDANCES
    private val _weeklyTrends = MutableStateFlow(
        listOf(
            DailyTrendData("lun.", 0f, 0f, 0f, 0f),
            DailyTrendData("mar.", 0f, 0f, 0f, 0f),
            DailyTrendData("mer.", 0f, 0f, 0f, 0f),
            DailyTrendData("jeu.", 0f, 0f, 0f, 0f),
            DailyTrendData("ven.", 0f, 0f, 0f, 0f),
            DailyTrendData("sam.", 0f, 0f, 0f, 0f),
            DailyTrendData("dim.", 0f, 0f, 0f, 0f)
        )
    )
    val weeklyTrends: StateFlow<List<DailyTrendData>> = _weeklyTrends.asStateFlow()

    init {
        // Récupération automatique de l'UID de l'utilisateur actuellement connecté sur le téléphone
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUid != null) {
            // On initialise le chemin Firebase avec son vrai UID unique
            historyRef = database.getReference("users/$currentUid/history")
            listenToFirebaseHistory()
        } else {
            // Fallback temporaire si aucun utilisateur n'est connecté (ex: phase de dev)
            historyRef = database.getReference("users/invite/history")
            listenToFirebaseHistory()
        }
    }

    // ÉCOUTE EN TEMPS RÉEL DE L'HISTORIQUE FIREBASE
    private fun listenToFirebaseHistory() {
        val ref = historyRef ?: return

        // Supprime l'ancien écouteur si la fonction est rappelée
        firebaseListener?.let { ref.removeEventListener(it) }

        firebaseListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = mutableListOf<WorkoutSession>()
                for (child in snapshot.children) {
                    val session = child.getValue(WorkoutSession::class.java)
                    if (session != null) {
                        tempList.add(session)
                    }
                }
                _sessions.value = tempList.reversed()
            }

            override fun onCancelled(error: DatabaseError) {
                println("Erreur Firebase History: ${error.message}")
            }
        }

        ref.addValueEventListener(firebaseListener!!)
    }

    // TRAITEMENT DES TRAMES DE L'EDGE IA PAR COMPORTEMENT
    fun updateStatus(message: String) {
        _connectionStatus.value = message

        if (_isRunning.value) {
            when {
                message.contains("SAUT_SIMPLE") || message.contains("JUMP_SIMPLE") -> {
                    _jumpsCount.value += 1
                    recalculateCalories()
                }
                message.contains("SAUT_DOUBLE") || message.contains("JUMP_DOUBLE") -> {
                    _jumpsCount.value += 1
                    _doubleJumpsCount.value += 1
                    recalculateCalories()
                }
            }
        }
    }

    private fun recalculateCalories() {
        _calories.value = (_jumpsCount.value * 0.12).toInt()
    }

    fun startScanning() {
        _connectionStatus.value = "Recherche en cours..."
    }

    fun toggleWorkout() {
        if (_isRunning.value) pauseWorkout() else startWorkout()
    }

    private fun startWorkout() {
        _isRunning.value = true
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                timeInSeconds++
                updateTimerDisplay()
            }
        }
    }

    private fun pauseWorkout() {
        _isRunning.value = false
        timerJob?.cancel()
    }

    fun stopWorkout() {
        _isRunning.value = false
        timerJob?.cancel()

        if (timeInSeconds > 0 || _jumpsCount.value > 0) {
            val currentMin = timeInSeconds / 60f
            val avgCadenceCalculated = if (currentMin > 0) (_jumpsCount.value / currentMin).toInt() else 0
            val doubleJumpsMinCalculated = if (currentMin > 0) (_doubleJumpsCount.value / currentMin).toInt() else 0

            val sdfDate = SimpleDateFormat("EEE dd MMM", Locale.FRANCE)
            val sdfTime = SimpleDateFormat("HH:mm", Locale.FRANCE)
            val currentDate = Date()

            val newSession = WorkoutSession(
                date = sdfDate.format(currentDate),
                timeRange = sdfTime.format(currentDate),
                durationSeconds = timeInSeconds,
                jumpsTotal = _jumpsCount.value,
                doubleJumpsTotal = _doubleJumpsCount.value,
                calories = _calories.value,
                avgCadence = avgCadenceCalculated,
                doubleJumpsMin = doubleJumpsMinCalculated
            )

            // --- SAUVEGARDE RÉELLE DANS LE DOSSIER DE L'UTILISATEUR ACTIF ---
            historyRef?.let { ref ->
                val key = ref.push().key
                if (key != null) {
                    ref.child(key).setValue(newSession)
                }
            }

            // Détermination du jour actuel (ex: "lun.", "mar.")
            val dayFormat = SimpleDateFormat("EEE", Locale.FRANCE)
            val currentDayLabel = dayFormat.format(currentDate).replace(".", "") + "."

            // Mise à jour des courbes
            _weeklyTrends.value = _weeklyTrends.value.map {
                if (it.dayLabel.equals(currentDayLabel, ignoreCase = true) || it.dayLabel == "jeu.") {
                    DailyTrendData(
                        dayLabel = it.dayLabel,
                        jumps = it.jumps + _jumpsCount.value.toFloat(),
                        durationMin = it.durationMin + currentMin,
                        kcal = it.kcal + _calories.value.toFloat(),
                        cadence = if (it.cadence == 0f) avgCadenceCalculated.toFloat() else (it.cadence + avgCadenceCalculated) / 2f
                    )
                } else it
            }
        }

        // Remise à zéro
        timeInSeconds = 0
        _jumpsCount.value = 0
        _doubleJumpsCount.value = 0
        _calories.value = 0
        updateTimerDisplay()
    }

    private fun updateTimerDisplay() {
        val minutes = timeInSeconds / 60
        val seconds = timeInSeconds % 60
        _timerString.value = String.format("%02d:%02d", minutes, seconds)
    }
}