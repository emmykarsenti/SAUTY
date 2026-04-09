package fr.isen.emmykarsenti.nicolasbetoin.sauty.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- 1. MODÈLE DE DONNÉES ---
data class JumpSession(
    val id: String,
    val dateLabel: String,
    val timeRange: String,
    val duration: String,
    val totalJumps: String,
    val jumpsPerMin: String,
    val doubleJumpsTotal: String,
    val doubleJumpsPerMin: String,
    val kcal: String
)

// --- 2. DONNÉES FACTICES (Triées de la plus récente à la plus ancienne). ---
val mockSessionsHistory = listOf(
    JumpSession("1", "jeu. 9 avril", "14:00–14:15", "15:00", "1 250", "115", "50", "10", "150"),
    JumpSession("2", "mer. 8 avril", "18:30–18:45", "15:00", "1 100", "105", "30", "5", "135"),
    JumpSession("3", "mar. 7 avril", "09:00–09:20", "20:00", "1 600", "110", "45", "8", "190"),
    JumpSession("4", "dim. 5 avril", "10:15–10:30", "15:00", "1 200", "112", "40", "9", "145"),
    JumpSession("5", "ven. 3 avril", "19:00–19:10", "10:00", "850", "115", "20", "6", "100"),
    JumpSession("6", "mer. 1 avril", "14:00–14:20", "20:00", "1 550", "108", "35", "7", "185"),
    JumpSession("7", "lun. 30 mars", "17:45–18:00", "15:00", "1 150", "109", "25", "4", "140")
)

@Composable
fun SessionDetailScreen(
    onBackClick: () -> Unit
) {
    val currentSession = mockSessionsHistory.first()
    var showAllSessions by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // --- EN-TÊTE ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                }
                Text(
                    text = currentSession.dateLabel,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = { /* Action de partage */ }) {
                    Icon(imageVector = Icons.Outlined.IosShare, contentDescription = "Partager", tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- TITRE DE L'EXERCICE (Avec espace pour ton icône) ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2E3A18)), // Vert olive foncé du design
                    contentAlignment = Alignment.Center
                ) {
                    // ----> REMPLACE CE BLOC PAR TON ICÔNE <----
                    // Décommente la ligne suivante et supprime l'icône FitnessCenter :
                    // Icon(painter = painterResource(id = R.drawable.nom_de_ton_icone_corde), contentDescription = null, tint = Color(0xFF92E52A), modifier = Modifier.size(32.dp))

                    Icon(imageVector = Icons.Default.FitnessCenter, contentDescription = null, tint = Color(0xFF92E52A), modifier = Modifier.size(32.dp))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text("Corde à sauter", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                    Text(currentSession.timeRange, color = Color.Gray, fontSize = 15.sp)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // --- CARTE DÉTAILS (Session Actuelle, SANS flèche) ---
        item {
            SessionDetailsCard(
                session = currentSession,
                showTitle = true,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        // --- SECTION : AUTRES SESSIONS ---
        item {
            Text(
                text = "Sessions précédentes",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)
            )
        }

        // --- CARROUSEL ---
        item {
            AnimatedVisibility(visible = !showAllSessions) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val recentSessions = mockSessionsHistory.drop(1).take(5)
                    items(recentSessions) { session ->
                        SessionDetailsCard(
                            session = session,
                            showTitle = false,
                            modifier = Modifier.width(300.dp)
                        )
                    }
                }
            }
        }

        // --- BOUTON VOIR PLUS ---
        item {
            Text(
                text = if (showAllSessions) "Voir moins" else "Voir tout l'historique",
                color = Color(0xFFFA114F), // Rouge Sauty
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
                    .clickable { showAllSessions = !showAllSessions }
            )
        }

        // --- LISTE COMPLÈTE ---
        if (showAllSessions) {
            items(mockSessionsHistory.drop(1)) { session ->
                SessionDetailsCard(
                    session = session,
                    showTitle = false,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                )
            }
        }
    }
}

// --- SOUS-COMPOSANT : CARTE DE DÉTAILS D'UNE SESSION ---
@Composable
fun SessionDetailsCard(
    session: JumpSession,
    showTitle: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            if (showTitle) {
                // TITRE DE LA CARTE, SANS LA FLÈCHE ">"
                Text(
                    text = "Détails de l'exercice",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            } else {
                Text(
                    text = "${session.dateLabel} • ${session.timeRange}",
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF2C2C2E))
            }

            Spacer(modifier = Modifier.height(if (showTitle) 24.dp else 16.dp))

            // GRILLE DES STATS
            Row(modifier = Modifier.fillMaxWidth()) {
                // Colonne de Gauche
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    StatItem(label = "Durée de l'exercice", value = session.duration, unit = "", valueColor = Color(0xFFFFD600))
                    StatItem(label = "Sauts / min moy.", value = session.jumpsPerMin, unit = " /MIN", valueColor = Color(0xFFE040FB))
                    StatItem(label = "Double sauts / min", value = session.doubleJumpsPerMin, unit = " /MIN", valueColor = Color(0xFFFF9800))
                }

                // Colonne de Droite
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    StatItem(label = "Sauts totaux", value = session.totalJumps, unit = " SAUTS", valueColor = Color(0xFFFA114F))
                    StatItem(label = "Double sauts totaux", value = session.doubleJumpsTotal, unit = " SAUTS", valueColor = Color(0xFFFF9800))
                    StatItem(label = "Kcal en activité", value = session.kcal, unit = " KCAL", valueColor = Color(0xFF00D8FE))
                }
            }
        }
    }
}

// --- SOUS-COMPOSANT : UN ÉLÉMENT DE STATISTIQUE ---
@Composable
fun StatItem(label: String, value: String, unit: String, valueColor: Color) {
    Column {
        Text(text = label, color = Color.White, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = value, color = valueColor, fontWeight = FontWeight.SemiBold, fontSize = 28.sp)
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    color = valueColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}