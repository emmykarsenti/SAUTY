package fr.isen.emmykarsenti.nicolasbetoin.sauty.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.isen.emmykarsenti.nicolasbetoin.sauty.model.DailyTrendData
import fr.isen.emmykarsenti.nicolasbetoin.sauty.viewmodel.SautyViewModel
import java.util.Locale

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
                text = "Détail des Tendances (60 jrs)",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (weeklyTrends.isNotEmpty()) {
            TrendChartCard(
                title = "Sauts",
                data = weeklyTrends,
                color = Color(0xFFFA9E1E),
                targetValue = viewModel.targetJumps.toFloat(),
                valueSelector = { it.jumps }
            )

            TrendChartCard(
                title = "Temps d'entraînement (min)",
                data = weeklyTrends,
                color = Color(0xFFFF521E),
                targetValue = viewModel.targetMinutes.toFloat(),
                valueSelector = { it.durationMin }
            )

            TrendChartCard(
                title = "Énergie dépensée (kcal)",
                data = weeklyTrends,
                color = Color(0xFF00E5FF),
                targetValue = viewModel.targetKcal.toFloat(),
                valueSelector = { it.kcal }
            )

            TrendChartCard(
                title = "Cadence moyenne (sauts/min)",
                data = weeklyTrends,
                color = Color(0xFF3902FF),
                targetValue = 150f,
                valueSelector = { it.cadence }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun TrendChartCard(
    title: String,
    data: List<DailyTrendData>,
    color: Color,
    targetValue: Float,
    valueSelector: (DailyTrendData) -> Float
) {
    val actualMax = data.maxOfOrNull { valueSelector(it) } ?: 0f
    val displayMax = if (actualMax > targetValue) actualMax * 1.1f else targetValue
    val safeDisplayMax = if (displayMax > 0f) displayMax else 10f

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val horizontalScrollState = rememberScrollState()

    // Configuration de l'écran pour calculer la largeur exacte (Solution au bug d'écrasement)
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // Largeur de l'écran moins les paddings (32dp) et l'axe Y (50dp)
    val availableWidthDp = configuration.screenWidthDp.dp - 32.dp - 50.dp
    // On veut afficher exactement 7 colonnes (jours) dans cet espace
    val columnWidthDp = availableWidthDp / 7f
    // La largeur totale de la zone défilante est proportionnelle aux 60 jours
    val canvasWidthDp = columnWidthDp * data.size

    LaunchedEffect(horizontalScrollState.maxValue) {
        if (horizontalScrollState.maxValue > 0) {
            horizontalScrollState.scrollTo(horizontalScrollState.maxValue)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Faites glisser et touchez un point", color = Color.DarkGray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth().height(220.dp)) {

                // AXE Y (Fixe)
                Canvas(modifier = Modifier.width(50.dp).fillMaxHeight()) {
                    val paddingTop = 60f
                    val paddingBottom = 60f
                    val chartHeight = size.height - paddingBottom - paddingTop

                    val textPaintY = Paint().apply {
                        this.color = android.graphics.Color.LTGRAY
                        textSize = 26f
                        textAlign = Paint.Align.RIGHT
                    }

                    val steps = 4
                    for (i in 0..steps) {
                        val y = paddingTop + chartHeight - (i * chartHeight / steps)
                        val value = (safeDisplayMax * i / steps).toInt()
                        drawContext.canvas.nativeCanvas.drawText(
                            value.toString(),
                            size.width - 10f,
                            y + 10f,
                            textPaintY
                        )
                    }
                }

                // GRAPHIQUE (Défilant)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(horizontalScrollState)
                ) {
                    Canvas(
                        modifier = Modifier
                            .width(canvasWidthDp) // C'est ICI que l'on empêche l'écrasement !
                            .fillMaxHeight()
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val pointWidthPx = with(density) { columnWidthDp.toPx() }
                                    val index = (offset.x / pointWidthPx).toInt()
                                    if (index in data.indices) {
                                        selectedIndex = if (selectedIndex == index) null else index
                                    }
                                }
                            }
                    ) {
                        val paddingTop = 60f
                        val paddingBottom = 60f
                        val chartHeight = size.height - paddingBottom - paddingTop
                        val pointWidthPx = size.width / data.size

                        val steps = 4
                        for (i in 0..steps) {
                            val y = paddingTop + chartHeight - (i * chartHeight / steps)
                            drawLine(
                                color = Color.DarkGray.copy(alpha = 0.5f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        }

                        val textPaintX = Paint().apply {
                            this.color = android.graphics.Color.GRAY
                            textSize = 24f
                            textAlign = Paint.Align.CENTER
                        }

                        val points = data.mapIndexed { index, item ->
                            val value = valueSelector(item)
                            val x = (index * pointWidthPx) + (pointWidthPx / 2f)
                            val y = paddingTop + chartHeight - ((value / safeDisplayMax) * chartHeight)

                            val dateParts = item.dayLabel.split(" ")
                            if (dateParts.isNotEmpty()) {
                                drawContext.canvas.nativeCanvas.drawText(
                                    dateParts[0], x, size.height - 25f, textPaintX
                                )
                                if (dateParts.size > 1) {
                                    drawContext.canvas.nativeCanvas.drawText(
                                        dateParts[1], x, size.height, textPaintX
                                    )
                                }
                            }
                            Offset(x, y)
                        }

                        for (i in 0 until points.size - 1) {
                            drawLine(
                                color = color,
                                start = points[i],
                                end = points[i + 1],
                                strokeWidth = 6f,
                                cap = StrokeCap.Round
                            )
                        }

                        points.forEach { point ->
                            drawCircle(color = Color.White, radius = 8f, center = point)
                        }

                        // BULLE D'INFORMATION AU CLIC
                        selectedIndex?.let { index ->
                            if (index in points.indices) {
                                val point = points[index]
                                val value = valueSelector(data[index])

                                val tooltipText = if (value % 1f == 0f) value.toInt().toString() else String.format(Locale.FRANCE, "%.1f", value)

                                val tooltipPaint = Paint().apply {
                                    this.color = android.graphics.Color.WHITE
                                    textSize = 30f
                                    textAlign = Paint.Align.CENTER
                                    isFakeBoldText = true
                                }

                                val bgRectWidth = 100f
                                val bgRectHeight = 50f
                                drawRoundRect(
                                    color = Color(0xFF2C2C2E),
                                    topLeft = Offset(point.x - (bgRectWidth / 2), point.y - 70f),
                                    size = Size(bgRectWidth, bgRectHeight),
                                    cornerRadius = CornerRadius(12f, 12f)
                                )

                                drawContext.canvas.nativeCanvas.drawText(
                                    tooltipText,
                                    point.x,
                                    point.y - 35f,
                                    tooltipPaint
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}