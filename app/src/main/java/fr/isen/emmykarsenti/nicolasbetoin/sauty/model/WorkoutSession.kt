package fr.isen.emmykarsenti.nicolasbetoin.sauty.model

/**
 * Modèle de données représentant une session d'entraînement unique.
 */
data class WorkoutSession(
    val date: String = "",
    val timeRange: String = "",
    val durationSeconds: Int = 0,
    val jumpsTotal: Int = 0,
    val doubleJumpsTotal: Int = 0,
    val calories: Int = 0,
    val avgCadence: Int = 0,
    val doubleJumpsMin: Int = 0
)

/**
 * Modèle de données utilisé pour l'agrégation et l'affichage
 * des statistiques journalières dans les graphiques de tendances.
 */
data class DailyTrendData(
    val dayLabel: String = "",
    val jumps: Float = 0f,
    val durationMin: Float = 0f,
    val kcal: Float = 0f,
    val cadence: Float = 0f
)