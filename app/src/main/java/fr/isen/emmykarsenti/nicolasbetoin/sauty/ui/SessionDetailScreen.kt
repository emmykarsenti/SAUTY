package fr.isen.emmykarsenti.nicolasbetoin.sauty.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- COULEURS ---
val colorPink = Color(0xFFFF2D55)
val colorGreen = Color(0xFF30D158)
val colorCyan = Color(0xFF5AC8FA)
val colorPurple = Color(0xFFAF52DE)
val textGray = Color(0xFF8E8E93)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("jeu. 9 avril", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Action de partage */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Partager", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. En-tête Activité
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color(0xFF1A1A1A), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_today), // Change si besoin
                            contentDescription = null,
                            tint = colorGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Corde à sauter", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("14:00-14:15", color = textGray, fontSize = 14.sp)
                    }
                }
            }

            // 2. Détails de l'exercice (AVEC ANNEAU)
            item {
                Text(
                    text = "Détails de l'exercice",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth(0.65f)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                StatItem(label = "Durée", value = "15:00", unit = "", valueColor = colorGreen)
                                StatItem(label = "Sauts totaux", value = "1 250", unit = " SAUTS", valueColor = colorPink)
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                StatItem(label = "Sauts / min", value = "115", unit = " /MIN", valueColor = colorPurple)
                                StatItem(label = "Calories", value = "150", unit = " KCAL", valueColor = colorCyan)
                            }
                        }

                        // Anneau de la session actuelle
                        Box(modifier = Modifier.align(Alignment.TopEnd)) {
                            ActivityRings(
                                jumpsProgress = 0.625f,
                                timeProgress = 0.5f,
                                kcalProgress = 0.5f,
                                size = 80.dp
                            )
                        }
                    }
                }
            }

            // 3. Sessions précédentes
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Sessions précédentes",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(mockPastSessions) { session ->
                        PastSessionCard(session)
                    }
                }
            }

            // 4. Bouton Historique
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Voir tout l'historique",
                        color = colorPink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { /* Action historique */ }
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

// --- COMPOSANTS UI UTILITAIRES ---

@Composable
fun StatItem(label: String, value: String, unit: String, valueColor: Color) {
    Column {
        Text(text = label, color = textGray, fontSize = 12.sp)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = value, color = valueColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            if (unit.isNotEmpty()) {
                Text(text = unit, color = valueColor, fontSize = 10.sp, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}

@Composable
fun PastSessionCard(session: PastSession) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.width(300.dp).clickable { /* Clic session */ }
    ) {
        Box(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Column {
                Text("${session.date} • ${session.timeRange}", color = textGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(0.70f), horizontalArrangement = Arrangement.SpaceBetween) {
                    PastStatItem(label = "Durée", value = session.durationStr, valueColor = colorGreen)
                    PastStatItem(label = "Sauts", value = session.totalJumpsStr, valueColor = colorPink)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(0.70f), horizontalArrangement = Arrangement.SpaceBetween) {
                    PastStatItem(label = "Sauts/min", value = session.jumpsPerMin, valueColor = colorPurple)
                    PastStatItem(label = "Kcal", value = session.kcalStr, valueColor = colorCyan)
                }
            }

            // Anneaux de l'historique
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                ActivityRings(
                    jumpsProgress = session.jumpsProgress,
                    timeProgress = session.timeProgress,
                    kcalProgress = session.kcalProgress,
                    size = 60.dp
                )
            }
        }
    }
}

@Composable
fun PastStatItem(label: String, value: String, valueColor: Color) {
    Column {
        Text(text = label, color = textGray, fontSize = 10.sp)
        Text(text = value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

// --- DONNÉES (C'est ce qu'il te manquait pour enlever les erreurs rouges !) ---

data class PastSession(
    val date: String,
    val timeRange: String,
    val durationStr: String,
    val totalJumpsStr: String,
    val jumpsPerMin: String,
    val kcalStr: String,
    val jumpsProgress: Float,
    val timeProgress: Float,
    val kcalProgress: Float
)

val mockPastSessions = listOf(
    PastSession("mer. 8 avril", "18:30-18:45", "15:00", "1 100", "105", "135", 0.55f, 0.5f, 0.45f),
    PastSession("mar. 7 avril", "09:00-09:20", "20:00", "1 600", "110", "190", 0.8f, 0.66f, 0.63f),
    PastSession("dim. 5 avril", "10:15-10:30", "15:00", "1 200", "112", "145", 0.6f, 0.5f, 0.48f)
)