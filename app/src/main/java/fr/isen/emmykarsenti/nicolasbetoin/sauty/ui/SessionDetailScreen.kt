package fr.isen.emmykarsenti.nicolasbetoin.sauty.ui

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.io.FileOutputStream

// COULEURS
val colorPink = Color(0xFFFF2D55)
val colorGreen = Color(0xFF30D158)
val colorCyan = Color(0xFF5AC8FA)
val colorPurple = Color(0xFFAF52DE)
val textGray = Color(0xFF8E8E93)

// MODÈLE DE DONNÉES (Prêt pour Firebase & STM32)
data class SessionData(
    val id: String = "", // Pour identifier la session dans Firebase
    val date: String = "--",
    val timeRange: String = "--:--",
    val durationStr: String = "00:00",
    val totalJumps: Int = 0,
    val jumpsPerMin: Int = 0,
    val kcal: Int = 0,
    val jumpsProgress: Float = 0f,
    val timeProgress: Float = 0f,
    val kcalProgress: Float = 0f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    currentSession: SessionData?, // La session en cours (récupérée de la base/STM32)
    pastSessions: List<SessionData>, // L'historique Firebase
    onBackClick: () -> Unit
) {
    // Récupération du contexte pour générer et partager le PDF
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = currentSession?.date ?: "Chargement...",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Appel de la fonction de partage PDF ici
                        shareSessionAsPdf(context, currentSession)
                    }) {
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

            // Si currentSession est null (données pas encore chargées), on peut afficher un loader ou rien
            if (currentSession != null) {
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
                                painter = painterResource(id = android.R.drawable.ic_menu_today),
                                contentDescription = null,
                                tint = colorGreen,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Corde à sauter", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(currentSession.timeRange, color = textGray, fontSize = 14.sp)
                        }
                    }
                }

                // 2. Détails de l'exercice
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
                                    StatItem(label = "Durée", value = currentSession.durationStr, unit = "", valueColor = colorGreen)
                                    // Utilisation de %,d pour formater les milliers (ex: 1250 -> 1 250)
                                    StatItem(label = "Sauts totaux", value = String.format("%,d", currentSession.totalJumps), unit = " SAUTS", valueColor = colorPink)
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    StatItem(label = "Sauts / min", value = currentSession.jumpsPerMin.toString(), unit = " /MIN", valueColor = colorPurple)
                                    StatItem(label = "Calories", value = currentSession.kcal.toString(), unit = " KCAL", valueColor = colorCyan)
                                }
                            }

                            // Anneau dynamique connecté aux données
                            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                                ActivityRings(
                                    jumpsProgress = currentSession.jumpsProgress,
                                    timeProgress = currentSession.timeProgress,
                                    kcalProgress = currentSession.kcalProgress,
                                    size = 80.dp
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colorPink)
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

                if (pastSessions.isEmpty()) {
                    Text(
                        text = "Aucune session précédente trouvée.",
                        color = textGray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(pastSessions) { session ->
                            PastSessionCard(session)
                        }
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
                            .clickable { /* Action historique vers un autre écran */ }
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

// COMPOSANTS UI UTILITAIRES

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
fun PastSessionCard(session: SessionData) {
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
                    PastStatItem(label = "Sauts", value = String.format("%,d", session.totalJumps), valueColor = colorPink)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(0.70f), horizontalArrangement = Arrangement.SpaceBetween) {
                    PastStatItem(label = "Sauts/min", value = session.jumpsPerMin.toString(), valueColor = colorPurple)
                    PastStatItem(label = "Kcal", value = session.kcal.toString(), valueColor = colorCyan)
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

// FONCTION DE PARTAGE PDF

fun shareSessionAsPdf(context: Context, session: SessionData?) {
    if (session == null) {
        Toast.makeText(context, "Aucune donnée à partager", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        // 1. Création du document PDF (Format A4 standard)
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        // 2. Dessiner le contenu (Titre)
        paint.textSize = 28f
        paint.isFakeBoldText = true
        canvas.drawText("Résumé de la Session - Bracelet Sauty", 50f, 80f, paint)

        // 3. Dessiner les données
        paint.textSize = 20f
        paint.isFakeBoldText = false
        canvas.drawText("Date : ${session.date} (${session.timeRange})", 50f, 150f, paint)

        paint.textSize = 24f
        canvas.drawText("Durée : ${session.durationStr}", 50f, 220f, paint)
        canvas.drawText("Calories : ${session.kcal} kcal", 50f, 270f, paint)
        canvas.drawText("Sauts totaux : ${String.format("%,d", session.totalJumps)}", 50f, 320f, paint)
        canvas.drawText("Cadence moy. : ${session.jumpsPerMin} sauts/min", 50f, 370f, paint)

        // Terminer la page
        pdfDocument.finishPage(page)

        // 4. Créer le dossier et le fichier dans le cache
        val pdfFolder = File(context.cacheDir, "pdfs")
        pdfFolder.mkdirs() // Créer le dossier s'il n'existe pas
        val file = File(pdfFolder, "Session_Sauty.pdf")

        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
        outputStream.close()

        // 5. Générer l'URI sécurisée via le FileProvider
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

        // 6. Lancer l'Intent de partage
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Ma session de corde à sauter")
            putExtra(Intent.EXTRA_TEXT, "Voici les résultats de ma dernière session avec le bracelet Sauty !")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Partager le PDF via..."))

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Erreur lors de la création du PDF", Toast.LENGTH_SHORT).show()
    }
}