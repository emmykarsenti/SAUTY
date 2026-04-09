package fr.isen.emmykarsenti.nicolasbetoin.sauty.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Composable
fun ActivityDetailsScreen(onBackClick: () -> Unit) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var offsetX by remember { mutableStateOf(0f) }

    val today = LocalDate.now()
    val isFuture = selectedDate.isAfter(today)

    val monday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekDates = (0..6).map { monday.plusDays(it.toLong()) }
    val dayInitials = listOf("L", "M", "M", "J", "V", "S", "D")

    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRANCE)
    val dateText = when {
        selectedDate.isEqual(today) -> "aujourd'hui ${selectedDate.format(formatter)}"
        selectedDate.isAfter(today) -> "À venir (${selectedDate.format(formatter)})"
        else -> selectedDate.format(formatter)
    }

    val randomFactor = selectedDate.dayOfYear % 10

    val ring1Progress = if (isFuture) 0f else 0.6f + (randomFactor * 0.03f)
    val ring2Progress = if (isFuture) 0f else 0.5f + (randomFactor * 0.04f)
    val ring3Progress = if (isFuture) 0f else 0.5f + (randomFactor * 0.02f)

    // Valeurs simulées alignées avec le Dashboard
    val sautsValue = if (isFuture) "0" else (1250 + randomFactor * 50).toString()
    val tempsActifValue = if (isFuture) "0" else (15 + randomFactor * 2).toString()
    val kcalValue = if (isFuture) "0" else (150 + randomFactor * 10).toString()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // --- 1. BARRE DE NAVIGATION SUPÉRIEURE ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "Retour",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onBackClick() }
                )

                Text(
                    text = dateText,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(24.dp))
            }
        }

        // --- 2. SEMAINE (PETITS ANNEAUX CLIQUABLES + SWIPE) ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (offsetX > 50) {
                                    selectedDate = selectedDate.minusWeeks(1)
                                } else if (offsetX < -50) {
                                    selectedDate = selectedDate.plusWeeks(1)
                                }
                                offsetX = 0f
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount
                            }
                        )
                    },
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDates.forEachIndexed { index, date ->
                    val isSelected = date.isEqual(selectedDate)
                    val dayIsFuture = date.isAfter(today)
                    val initial = dayInitials[index]

                    val dayFactor = date.dayOfYear % 10
                    val p1 = if (dayIsFuture) 0f else 0.4f + (dayFactor * 0.05f)
                    val p2 = if (dayIsFuture) 0f else 0.3f + (dayFactor * 0.04f)
                    val p3 = if (dayIsFuture) 0f else 0.2f + (dayFactor * 0.03f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedDate = date }
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = initial,
                            color = if (dayIsFuture) Color.DarkGray else if (isSelected) Color.White else Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        if (isSelected) {
                            Box(contentAlignment = Alignment.Center) {
                                Box(modifier = Modifier.size(32.dp).background(Color(0xFF1C1C1E), CircleShape))
                                FlexibleActivityRings(size = 24.dp, strokeWidth = 3.dp, progresses = listOf(ring1Progress, ring2Progress, ring3Progress), isFuture = dayIsFuture)
                            }
                        } else {
                            FlexibleActivityRings(
                                size = 24.dp,
                                strokeWidth = 3.dp,
                                progresses = listOf(p1, p2, p3),
                                isFuture = dayIsFuture
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- 3. GRANDS ANNEAUX CENTRAUX ---
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                FlexibleActivityRings(
                    size = 220.dp,
                    strokeWidth = 28.dp,
                    progresses = listOf(ring1Progress, ring2Progress, ring3Progress),
                    isFuture = isFuture
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // --- 4. SECTION SAUTS (ROUGE) ---
        item {
            ActivityDetailChartSection(
                title = "Sauts",
                value = sautsValue,
                unit = "", // Pas d'unité texte, juste le chiffre
                goal = "/ 2 000",
                color = Color(0xFFFA114F),
                maxChartValue = "150 SAUTS",
                isFuture = isFuture
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- 5. SECTION TEMPS ACTIF (VERT) ---
        item {
            ActivityDetailChartSection(
                title = "Temps Actif",
                value = tempsActifValue,
                unit = "MN",
                goal = " / 30",
                color = Color(0xFF92E52A),
                maxChartValue = "6 MN",
                isFuture = isFuture
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- 6. SECTION KCAL (BLEU) ---
        item {
            ActivityDetailChartSection(
                title = "Kcal",
                value = kcalValue,
                unit = "", // Pas d'unité texte
                goal = "/ 300",
                color = Color(0xFF00D8FE),
                maxChartValue = "40 KCAL",
                isFuture = isFuture
            )
        }
    }
}

// --- SOUS-COMPOSANTS ---

@Composable
fun FlexibleActivityRings(size: Dp, strokeWidth: Dp, progresses: List<Float>, isFuture: Boolean = false) {
    val size1 = size
    val size2 = size * 0.7f
    val size3 = size * 0.4f

    val color1 = Color(0xFFFA114F) // Rouge : Sauts
    val color2 = Color(0xFF92E52A) // Vert : Temps Actif
    val color3 = Color(0xFF00D8FE) // Bleu : Kcal

    val trackAlpha = if (isFuture) 0.05f else 0.2f

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        CircularProgressIndicator(progress = progresses[0], modifier = Modifier.size(size1), color = color1, strokeWidth = strokeWidth, trackColor = color1.copy(alpha = trackAlpha), strokeCap = StrokeCap.Round)
        CircularProgressIndicator(progress = progresses[1], modifier = Modifier.size(size2), color = color2, strokeWidth = strokeWidth, trackColor = color2.copy(alpha = trackAlpha), strokeCap = StrokeCap.Round)
        CircularProgressIndicator(progress = progresses[2], modifier = Modifier.size(size3), color = color3, strokeWidth = strokeWidth, trackColor = color3.copy(alpha = trackAlpha), strokeCap = StrokeCap.Round)
    }
}

@Composable
fun ActivityDetailChartSection(
    title: String,
    value: String,
    unit: String,
    goal: String,
    color: Color,
    maxChartValue: String,
    isFuture: Boolean = false
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(text = title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = value, color = color, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            if (unit.isNotEmpty()) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = unit, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
            }
            Text(text = goal, color = color.copy(alpha = 0.7f), fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = maxChartValue, color = Color.Gray, fontSize = 10.sp)
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            val barHeights = if (isFuture) {
                List(15) { 0f }
            } else {
                listOf(0.1f, 0.0f, 0.0f, 0.2f, 0.1f, 0.8f, 0.4f, 0.9f, 0.2f, 0.1f, 0.1f, 0.0f, 0.0f, 0.0f, 0.0f)
            }

            barHeights.forEach { height ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(if (height > 0) height else 0.05f)
                        .background(if (height > 0) color else color.copy(alpha = 0.3f), RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(color.copy(alpha = 0.5f)))
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("00:00", color = Color.Gray, fontSize = 10.sp)
            Text("06:00", color = Color.Gray, fontSize = 10.sp)
            Text("12:00", color = Color.Gray, fontSize = 10.sp)
            Text("18:00", color = Color.Gray, fontSize = 10.sp)
        }
    }
}