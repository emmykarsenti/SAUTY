package fr.isen.emmykarsenti.nicolasbetoin.sauty.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
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

    private val database = FirebaseDatabase.getInstance("https://sauty-ekarsenti-nbetoin-default-rtdb.europe-west1.firebasedatabase.app/")
    private var historyRef: DatabaseReference? = null
    private var profileRef: DatabaseReference? = null
    private var firebaseListener: ValueEventListener? = null

    private val _connectionStatus = MutableStateFlow("Déconnecté")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

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

    private var initialJumpsOffset = -1
    private var initialCalOffset = -1

    var userFirstName by mutableStateOf("")
    var userPoids by mutableStateOf("70")
    var userTaille by mutableStateOf("170")
    var targetJumps by mutableIntStateOf(2000)
    var targetMinutes by mutableIntStateOf(30)
    var targetKcal by mutableIntStateOf(300)

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

    private fun listenToProfileTargets() {
        profileRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userFirstName = snapshot.child("prenom").getValue(String::class.java) ?: ""
                userPoids = snapshot.child("poids").getValue(String::class.java) ?: "70"
                userTaille = snapshot.child("taille").getValue(String::class.java) ?: "170"

                val objectifs = snapshot.child("objectifs")
                updateTargets(
                    jumps = objectifs.child("sauts").getValue(Int::class.java) ?: 2000,
                    minutes = objectifs.child("minutes").getValue(Int::class.java) ?: 30,
                    kcal = objectifs.child("kcal").getValue(Int::class.java) ?: 300
                )
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // Calcul des calories basé sur le poids et la taille réels de l'utilisateur
    // Formule : MET (11.8 pour corde à sauter) × poids(kg) × heures
    // Le MET est ensuite ajusté selon l'IMC pour tenir compte de la morphologie
    private fun calculerCalories(): Int {
        val poids = userPoids.toFloatOrNull() ?: 70f
        val taille = userTaille.toFloatOrNull() ?: 170f
        val tailleM = taille / 100f
        val imc = poids / (tailleM * tailleM)

        // Ajustement du MET selon l'IMC
        // IMC normal (18.5-25) : MET 11.8
        // IMC élevé (>25) : effort légèrement plus important → MET augmenté
        // IMC faible (<18.5) : effort moindre → MET réduit
        val met = when {
            imc < 18.5f -> 10.5f
            imc < 25f   -> 11.8f
            imc < 30f   -> 12.5f
            else        -> 13.2f
        }

        val heures = timeInSeconds / 3600f
        return (met * poids * heures).toInt()
    }

    fun updateTargets(jumps: Int, minutes: Int, kcal: Int) {
        targetJumps = jumps
        targetMinutes = minutes
        targetKcal = kcal
    }

    private fun listenToFirebaseHistory() {
        historyRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = mutableListOf<WorkoutSession>()
                for (child in snapshot.children) {
                    child.getValue(WorkoutSession::class.java)?.let { tempList.add(it) }
                }
                val reversedList = tempList.reversed()
                _sessions.value = reversedList
                updateWeeklyTrends(reversedList)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateWeeklyTrends(sessions: List<WorkoutSession>) {
        val dayLabels = listOf("lun.", "mar.", "mer.", "jeu.", "ven.", "sam.", "dim.")
        val trendsMap = dayLabels.associateWith { DailyTrendData(it, 0f, 0f, 0f, 0f) }.toMutableMap()

        for (session in sessions) {
            val dateStr = session.date.lowercase(Locale.FRANCE)
            val matchedDay = dayLabels.find { dateStr.startsWith(it.replace(".", "")) }

            if (matchedDay != null) {
                val current = trendsMap[matchedDay]!!
                val durationMin = session.durationSeconds / 60f
                val totalMin = current.durationMin + durationMin
                val totalJumps = current.jumps + session.jumpsTotal

                trendsMap[matchedDay] = current.copy(
                    durationMin = totalMin,
                    jumps = totalJumps,
                    kcal = current.kcal + session.calories,
                    cadence = if (totalMin > 0) totalJumps / totalMin else 0f
                )
            }
        }
        _weeklyTrends.value = dayLabels.map { trendsMap[it]!! }
    }

    fun updateFromBle(jumpsFromDevice: Int, caloriesFromDevice: Int) {
        if (_isRunning.value) {
            if (initialJumpsOffset == -1) {
                initialJumpsOffset = jumpsFromDevice
                initialCalOffset = caloriesFromDevice
            }
            _jumpsCount.value = maxOf(0, jumpsFromDevice - initialJumpsOffset)
            _calories.value = maxOf(0, caloriesFromDevice - initialCalOffset)
        }
    }

    fun updateStatus(message: String) {
        val cleanMessage = message.trim()
        android.util.Log.d("BLE_DEBUG", "updateStatus reçu : '$cleanMessage' — isRunning=${_isRunning.value}")
        if (cleanMessage.contains("ACTION_JUMP", ignoreCase = true)) {
            if (_isRunning.value) {
                _jumpsCount.value += 1
                _calories.value = calculerCalories()
            }
        } else {
            _connectionStatus.value = cleanMessage
        }
    }

    fun toggleWorkout() {
        if (_isRunning.value) pauseWorkout() else startWorkout()
    }

    private fun startWorkout() {
        _isRunning.value = true
        initialJumpsOffset = -1
        initialCalOffset = -1
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                timeInSeconds++
                updateTimerDisplay()
                // Mise à jour des calories chaque seconde si session active
                if (_jumpsCount.value > 0) {
                    _calories.value = calculerCalories()
                }
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
            saveSessionToFirebase()
        }
        resetStats()
    }

    private fun saveSessionToFirebase() {
        val currentMin = timeInSeconds / 60f
        val avgCadence = if (currentMin > 0) (_jumpsCount.value / currentMin).toInt() else 0
        val sdfDate = SimpleDateFormat("EEE dd MMM", Locale.FRANCE)
        val sdfTime = SimpleDateFormat("HH:mm", Locale.FRANCE)
        val now = Date()

        val session = WorkoutSession(
            date = sdfDate.format(now),
            timeRange = sdfTime.format(now),
            durationSeconds = timeInSeconds,
            jumpsTotal = _jumpsCount.value,
            doubleJumpsTotal = _doubleJumpsCount.value,
            calories = _calories.value,
            avgCadence = avgCadence
        )
        historyRef?.push()?.setValue(session)
    }

    private fun resetStats() {
        timeInSeconds = 0
        _jumpsCount.value = 0
        _doubleJumpsCount.value = 0
        _calories.value = 0
        initialJumpsOffset = -1
        initialCalOffset = -1
        updateTimerDisplay()
    }

    private fun updateTimerDisplay() {
        val mins = timeInSeconds / 60
        val secs = timeInSeconds % 60
        _timerString.value = String.format(Locale.FRANCE, "%02d:%02d", mins, secs)
    }

    fun startScanning() {
        _connectionStatus.value = "Recherche en cours..."
    }
}