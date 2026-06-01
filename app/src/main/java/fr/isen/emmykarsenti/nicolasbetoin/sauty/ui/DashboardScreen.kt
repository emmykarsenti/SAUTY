package fr.isen.emmykarsenti.nicolasbetoin.sauty.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import fr.isen.emmykarsenti.nicolasbetoin.sauty.ble.BleManager
import fr.isen.emmykarsenti.nicolasbetoin.sauty.viewmodel.SautyViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    bleManager: BleManager,
    viewModel: SautyViewModel,
    onProfileClick: () -> Unit,
    onTrendsClick: () -> Unit,
    onSessionClick: () -> Unit
) {
    val sessions by viewModel.sessions.collectAsState()
    val lastSession = sessions.firstOrNull()

    // CUMULS DE LA JOURNÉE
    val todayJumps by viewModel.todayJumps.collectAsState()
    val todayMinutes by viewModel.todayMinutes.collectAsState()
    val todayCalories by viewModel.todayCalories.collectAsState()

    // DONNÉES DE LA DERNIÈRE SESSION (Pour la carte centrale uniquement)
    val lastMinutes = (lastSession?.durationSeconds ?: 0) / 60
    val lastCadence = lastSession?.avgCadence ?: 0

    // OBJECTIFS
    val targetJumps = viewModel.targetJumps
    val targetMinutes = viewModel.targetMinutes
    val targetKcal = viewModel.targetKcal

    val currentDate = remember {
        val sdf = SimpleDateFormat("d MMMM yyyy", Locale.FRANCE)
        sdf.format(Date()).uppercase(Locale.FRANCE)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (viewModel.userFirstName.isNotEmpty()) "Bonjour ${viewModel.userFirstName}" else "Bonjour !",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(currentDate, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            IconButton(onClick = onProfileClick) {
                Icon(Icons.Default.AccountCircle, contentDescription = "Profil", modifier = Modifier.size(36.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bloc des Objectifs du Jour (Utilise les totaux cumulative "today")
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                ActivityRings(
                    jumpsProgress = todayJumps.toFloat() / targetJumps.coerceAtLeast(1),
                    timeProgress = todayMinutes.toFloat() / targetMinutes.coerceAtLeast(1),
                    kcalProgress = todayCalories.toFloat() / targetKcal.coerceAtLeast(1),
                    size = 120.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Sauts", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text("$todayJumps / $targetJumps SAUTS", color = Color(0xFFFA9E1E), style = MaterialTheme.typography.titleMedium)

                    Text("Temps d'entrainement", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text("$todayMinutes / $targetMinutes MIN", color = Color(0xFFFF521E), style = MaterialTheme.typography.titleMedium)

                    Text("Kcal", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text("$todayCalories / $targetKcal KCAL", color = Color(0xFF00E5FF), style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bloc de la Dernière Session Enregistrée (Garde les infos de la dernière séance)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            modifier = Modifier.fillMaxWidth().clickable { onSessionClick() }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Sessions (Dernière)", style = MaterialTheme.typography.titleMedium)
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Détails")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Temps d'entraînement", color = Color.Gray)
                    Text("$lastMinutes MIN", color = Color(0xFFFF521E))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Sauts", color = Color.Gray)
                    Text("$lastCadence /MIN", color = Color(0xFF3902FF))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Redirection vers les Histogrammes de Tendances (Affiche le cumul du jour)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            modifier = Modifier.fillMaxWidth().clickable { onTrendsClick() }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tendances", style = MaterialTheme.typography.titleMedium)
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Graphiques")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Sauts", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        Text("$todayJumps /JOUR", color = Color(0xFFFA9E1E))
                    }
                    Column {
                        Text("Temps d'entrain.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        Text("$todayMinutes MIN/JOUR", color = Color(0xFFFF521E))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("KCAL", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        Text("$todayCalories /JOUR", color = Color(0xFF00E5FF))
                    }
                    Column {
                        Text("Sauts / MIN", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        Text("$lastCadence /MIN", color = Color(0xFF3902FF))
                    }
                }
            }
        }
    }
}