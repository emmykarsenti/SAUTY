package fr.isen.emmykarsenti.nicolasbetoin.sauty.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Interface d'entraînement actif.
 * Affiche les statistiques en temps réel et permet de contrôler le déroulement de la session.
 */
@Composable
fun WorkoutScreen(
    jumpsCount: Int,
    timerString: String,
    calories: Int,
    onStartPauseClick: () -> Unit,
    onStopClick: () -> Unit,
    isRunning: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 40.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = "$jumpsCount", color = Color.White, fontSize = 80.sp, fontWeight = FontWeight.Light)
                Text(text = " SAUTS", color = Color.Gray, fontSize = 16.sp, modifier = Modifier.padding(bottom = 16.dp, start = 8.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = "$calories", color = Color.White, fontSize = 60.sp, fontWeight = FontWeight.Light)
                Text(text = " KCAL", color = Color.Gray, fontSize = 16.sp, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = timerString,
                    color = Color(0xFFE5D52A),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FloatingActionButton(
                        onClick = onStopClick,
                        containerColor = Color.DarkGray,
                        contentColor = Color.Red,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(32.dp))
                    }

                    FloatingActionButton(
                        onClick = onStartPauseClick,
                        containerColor = if (isRunning) Color(0xFFE5D52A) else Color(0xFFFF521E),
                        contentColor = Color.Black,
                        shape = CircleShape,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }
    }
}