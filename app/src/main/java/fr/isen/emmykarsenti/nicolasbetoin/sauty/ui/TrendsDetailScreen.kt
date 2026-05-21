package fr.isen.emmykarsenti.nicolasbetoin.sauty.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.isen.emmykarsenti.nicolasbetoin.sauty.model.DailyTrendData
import fr.isen.emmykarsenti.nicolasbetoin.sauty.viewmodel.SautyViewModel

@Composable
fun TrendsDetailScreen(
    viewModel: SautyViewModel = viewModel(),
    onBackClick: () -> Unit
) {
    val weeklyTrends by viewModel.weeklyTrends.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
            }
            Text(
                text = "Détail des Tendances",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Courbe des Sauts
        TrendChartCard(
            title = "Sauts",
            data = weeklyTrends,
            color = Color(0xFFE91E63),
            valueSelector = { it.jumps }
        )

        // Courbe Temps d'entraînement
        TrendChartCard(
            title = "Temps d'entraînement (min)",
            data = weeklyTrends,
            color = Color(0xFF8CE825),
            valueSelector = { it.durationMin }
        )

        // Courbe Kcal
        TrendChartCard(
            title = "Énergie dépensée (kcal)",
            data = weeklyTrends,
            color = Color(0xFF00E5FF),
            valueSelector = { it.kcal }
        )

        // Courbe Cadence (Sauts/min)
        TrendChartCard(
            title = "Cadence moyenne (sauts/min)",
            data = weeklyTrends,
            color = Color(0xFFBA68C8),
            valueSelector = { it.cadence }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun TrendChartCard(
    title: String,
    data: List<DailyTrendData>,
    color: Color,
    valueSelector: (DailyTrendData) -> Float
) {
    val maxValue = data.maxOfOrNull { valueSelector(it) }?.takeIf { it > 0 } ?: 1f

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                val stepX = size.width / (data.size - 1).coerceAtLeast(1)

                val points = data.mapIndexed { index, item ->
                    val value = valueSelector(item)
                    val x = index * stepX
                    val y = size.height - ((value / maxValue) * size.height)
                    Offset(x, y)
                }

                // Dessin de la ligne
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = color,
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                }

                // Dessin des points
                points.forEach { point ->
                    drawCircle(color = Color.White, radius = 8f, center = point)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Labels de l'axe X (Jours)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                data.forEach {
                    Text(text = it.dayLabel, color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
    }
}