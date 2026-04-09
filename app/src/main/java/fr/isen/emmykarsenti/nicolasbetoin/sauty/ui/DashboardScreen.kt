package fr.isen.emmykarsenti.nicolasbetoin.sauty.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(
    onProfileClick: () -> Unit,
    onActivityRingsClick: () -> Unit,
    onSessionClick: () -> Unit
) {
    val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRANCE)).uppercase()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // --- 1. EN-TÊTE ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Résumé", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text(text = currentDate, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                IconButton(
                    onClick = onProfileClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C1C1E))
                ) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = "Profil", tint = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- 2. ANNEAUX D'ACTIVITÉ (Style Apple) ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onActivityRingsClick() },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Anneaux Activité", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FlexibleActivityRings(size = 110.dp, strokeWidth = 14.dp, progresses = listOf(0.6f, 0.5f, 0.5f))

                        Spacer(modifier = Modifier.width(24.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppleMetricRow("Sauts", "1 250", "2 000", "SAUTS", Color(0xFFFA114F))
                            AppleMetricRow("Temps Actif", "15", "30", "MIN", Color(0xFF92E52A))
                            AppleMetricRow("Kcal", "150", "300", "KCAL", Color(0xFF00D8FE))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- 3. SESSION (Dernière) - Bulle Unique ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSessionClick() },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Sessions (Dernière)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SessionStatRow(title = "Temps d'entraînement", value = "15 min", subtitle = null)
                    HorizontalDivider(color = Color(0xFF2C2C2E), modifier = Modifier.padding(vertical = 12.dp))
                    SessionStatRow(title = "Sauts", value = "115 /min", subtitle = "1 250 total")
                    HorizontalDivider(color = Color(0xFF2C2C2E), modifier = Modifier.padding(vertical = 12.dp))
                    SessionStatRow(title = "Double sauts", value = "10 /min", subtitle = "50 total")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- 4. TENDANCES ---
        item {
            TrendsCard()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- SOUS-COMPOSANT : MÉTRIQUE STYLE APPLE ---
@Composable
fun AppleMetricRow(title: String, value: String, max: String, unit: String, color: Color) {
    Column {
        Text(text = title, color = Color.White, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = "/$max $unit", color = color.copy(alpha = 0.8f), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// --- SOUS-COMPOSANT : LIGNE DE STATISTIQUE SESSION ---
@Composable
fun SessionStatRow(title: String, value: String, subtitle: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = Color.White, fontSize = 15.sp)
        Column(horizontalAlignment = Alignment.End) {
            Text(text = value, color = Color(0xFFFFD600), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(text = subtitle, color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

// --- COMPOSANT : LA CARTE TENDANCES ---
@Composable
fun TrendsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // En-tête de la carte
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Tendances", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Voir plus", tint = Color.Gray)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Disposition sur deux colonnes
            Row(modifier = Modifier.fillMaxWidth()) {
                // Colonne de Gauche
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    TrendItem(
                        icon = Icons.Default.KeyboardArrowUp, iconColor = Color(0xFFFA114F),
                        title = "Sauts", value = "1 250 /JOUR", valueColor = Color(0xFFFA114F)
                    )
                    TrendItem(
                        icon = Icons.Default.KeyboardArrowUp, iconColor = Color(0xFF00D8FE),
                        title = "Double sauts", value = "50 /JOUR", valueColor = Color(0xFF00D8FE)
                    )
                    TrendItem(
                        icon = Icons.Default.KeyboardArrowUp, iconColor = Color(0xFFFF9800),
                        title = "Kcal", value = "220 /JOUR", valueColor = Color(0xFFFF9800)
                    )
                }

                // Colonne de Droite
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    TrendItem(
                        icon = Icons.Default.KeyboardArrowUp, iconColor = Color(0xFF92E52A),
                        title = "Temps d'entrain.", value = "15 MIN/JOUR", valueColor = Color(0xFF92E52A)
                    )
                    TrendItem(
                        icon = Icons.Default.KeyboardArrowUp, iconColor = Color(0xFFE040FB),
                        title = "Sauts / min", value = "115 /MIN", valueColor = Color(0xFFE040FB)
                    )
                    TrendItem(
                        icon = Icons.Default.Remove, iconColor = Color(0xFFFFD600),
                        title = "Double sauts / min", value = "-/- /MIN", valueColor = Color(0xFFFFD600)
                    )
                }
            }
        }
    }
}

// --- SOUS-COMPOSANT : UN ÉLÉMENT DE TENDANCE ---
@Composable
fun TrendItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    value: String,
    valueColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(Color.Transparent, CircleShape)
                .border(2.dp, iconColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(text = title, color = Color.White, fontSize = 13.sp)
            Text(text = value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FlexibleActivityRings(size: Dp, strokeWidth: Dp, progresses: List<Float>) {
    val size2 = size * 0.7f
    val size3 = size * 0.4f

    val color1 = Color(0xFFFA114F)
    val color2 = Color(0xFF92E52A)
    val color3 = Color(0xFF00D8FE)

    val trackAlpha = 0.2f

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        CircularProgressIndicator(progress = { progresses[0] }, modifier = Modifier.size(size), color = color1, strokeWidth = strokeWidth, trackColor = color1.copy(alpha = trackAlpha), strokeCap = StrokeCap.Round)
        CircularProgressIndicator(progress = { progresses[1] }, modifier = Modifier.size(size2), color = color2, strokeWidth = strokeWidth, trackColor = color2.copy(alpha = trackAlpha), strokeCap = StrokeCap.Round)
        CircularProgressIndicator(progress = { progresses[2] }, modifier = Modifier.size(size3), color = color3, strokeWidth = strokeWidth, trackColor = color3.copy(alpha = trackAlpha), strokeCap = StrokeCap.Round)
    }
}