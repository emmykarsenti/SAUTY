package fr.isen.emmykarsenti.nicolasbetoin.sauty.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    // FIREBASE
    private val database = FirebaseDatabase.getInstance("https://sauty-ekarsenti-nbetoin-default-rtdb.europe-west1.firebasedatabase.app/")
    private var historyRef: DatabaseReference? = null
    private var profileRef: DatabaseReference? = null
    private var firebaseListener: ValueEventListener? = null

    // ÉTAT CONNEXION
    private val _connectionStatus = MutableStateFlow("Déconnecté")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    // EXERCICE EN TEMPS RÉEL
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

    // OBJECTIFS (Synchronisés avec le Dashboard)
    var userFirstName by mutableStateOf("")
        private set
    var targetJumps by mutableIntStateOf(2000)
        private set
    var targetMinutes by mutableIntStateOf(30)
        private set
    var targetKcal by mutableIntStateOf(300)
        private set

    // DONNÉES HISTORIQUE & TENDANCES
    private val _sessions = MutableStateFlow<List<WorkoutSession>>(emptyList())
    val sessions: StateFlow<List<WorkoutSession>> = _sessions.asStateFlow()

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
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid != null) {
            historyRef = database.getReference("users/$currentUid/history")
            profileRef = database.getReference("users/$currentUid/profil")
            listenToFirebaseHistory()
            listenToProfileTargets()
        }
    }

    // RÉCUPÉRATION DES OBJECTIFS DEPUIS LE PROFIL
    private fun listenToProfileTargets() {
        profileRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Récupération du prénom pour le "Bonjour {Prénom}"
                userFirstName = snapshot.child("prenom").getValue(String::class.java) ?: ""

                // Récupération des objectifs (déjà existant dans ton code)
                val objectifs = snapshot.child("objectifs")
                val jumps = snapshot.child("sauts").getValue(Int::class.java) ?: 2000
                val minutes = snapshot.child("minutes").getValue(Int::class.java) ?: 30
                val kcal = snapshot.child("kcal").getValue(Int::class.java) ?: 300
                updateTargets(jumps, minutes, kcal)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // Fonction appelée par le ProfileScreen pour mettre à jour l'UI instantanément
    fun updateTargets(jumps: Int, minutes: Int, kcal: Int) {
        targetJumps = jumps
        targetMinutes = minutes
        targetKcal = kcal
    }

    // GESTION HISTORIQUE
    private fun listenToFirebaseHistory() {
        val ref = historyRef ?: return
        firebaseListener?.let { ref.removeEventListener(it) }

        firebaseListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = mutableListOf<WorkoutSession>()
                for (child in snapshot.children) {
                    child.getValue(WorkoutSession::class.java)?.let { tempList.add(it) }
                }
                _sessions.value = tempList.reversed()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(firebaseListener!!)
    }

    // LOGIQUE EXERCICE
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
                avgCadence = avgCadenceCalculated
            )

            historyRef?.push()?.setValue(newSession)

            // Mise à jour locale des tendances (courbes)
            val dayFormat = SimpleDateFormat("EEE", Locale.FRANCE)
            val currentDayLabel = dayFormat.format(currentDate).lowercase().replace(".", "") + "."

            _weeklyTrends.value = _weeklyTrends.value.map {
                if (it.dayLabel.equals(currentDayLabel, ignoreCase = true)) {
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

        // Reset
        timeInSeconds = 0
        _jumpsCount.value = 0
        _doubleJumpsCount.value = 0
        _calories.value = 0
        updateTimerDisplay()
    }

    private fun updateTimerDisplay() {
        val minutes = timeInSeconds / 60
        val seconds = timeInSeconds % 60
        _timerString.value = String.format(Locale.FRANCE, "%02d:%02d", minutes, seconds)
    }

    fun startScanning() {
        _connectionStatus.value = "Recherche en cours..."
    }
}