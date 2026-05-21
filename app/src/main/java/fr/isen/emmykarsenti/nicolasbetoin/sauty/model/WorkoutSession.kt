package fr.isen.emmykarsenti.nicolasbetoin.sauty.model

data class WorkoutSession(
    val date: String,
    val timeRange: String,
    val durationSeconds: Int,
    val jumpsTotal: Int,
    val doubleJumpsTotal: Int,
    val calories: Int,
    val avgCadence: Int,
    val doubleJumpsMin: Int
)

data class DailyTrendData(
    val dayLabel: String,
    val jumps: Float,
    val durationMin: Float,
    val kcal: Float,
    val cadence: Float
)