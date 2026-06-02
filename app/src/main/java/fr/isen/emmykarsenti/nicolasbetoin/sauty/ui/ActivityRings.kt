package fr.isen.emmykarsenti.nicolasbetoin.sauty.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Composant graphique personnalisé dessinant trois anneaux concentriques.
 * Représente visuellement la progression de l'utilisateur sur trois métriques :
 * sauts (externe), temps (intermédiaire) et calories (interne).
 */
@Composable
fun ActivityRings(
    jumpsProgress: Float,
    timeProgress: Float,
    kcalProgress: Float,
    size: Dp = 140.dp
) {
    Canvas(modifier = Modifier.size(size)) {
        val strokeWidth = size.toPx() * 0.11f
        val spacing = size.toPx() * 0.02f
        val center = this.center // On récupère le centre exact du Canvas

        // Couleurs style Apple Fitness
        val orangeColor = Color(0xFFFA9E1E)
        val redColor = Color(0xFFFF521E)
        val cyanColor = Color(0xFF5AC8FA)

        // 1. Cercle des Sauts (Orange) - Externe
        val radiusOuter = (size.toPx() / 2) - (strokeWidth / 2)
        drawCircle(
            color = orangeColor.copy(alpha = 0.15f),
            radius = radiusOuter,
            style = Stroke(strokeWidth)
        )
        // On force l'arc à épouser exactement le cercle
        drawArc(
            color = orangeColor,
            startAngle = -90f,
            sweepAngle = (jumpsProgress * 360f).coerceAtMost(360f),
            useCenter = false,
            topLeft = Offset(center.x - radiusOuter, center.y - radiusOuter),
            size = Size(radiusOuter * 2, radiusOuter * 2),
            style = Stroke(strokeWidth, cap = StrokeCap.Round)
        )

        // 2. Cercle du Temps Actif (Rouge) - Intermédiaire
        val radiusMedium = radiusOuter - strokeWidth - spacing
        drawCircle(
            color = redColor.copy(alpha = 0.15f),
            radius = radiusMedium,
            style = Stroke(strokeWidth)
        )
        drawArc(
            color = redColor,
            startAngle = -90f,
            sweepAngle = (timeProgress * 360f).coerceAtMost(360f),
            useCenter = false,
            topLeft = Offset(center.x - radiusMedium, center.y - radiusMedium),
            size = Size(radiusMedium * 2, radiusMedium * 2),
            style = Stroke(strokeWidth, cap = StrokeCap.Round)
        )

        // 3. Cercle des Calories (Cyan) - Interne
        val radiusInner = radiusMedium - strokeWidth - spacing
        drawCircle(
            color = cyanColor.copy(alpha = 0.15f),
            radius = radiusInner,
            style = Stroke(strokeWidth)
        )
        drawArc(
            color = cyanColor,
            startAngle = -90f,
            sweepAngle = (kcalProgress * 360f).coerceAtMost(360f),
            useCenter = false,
            topLeft = Offset(center.x - radiusInner, center.y - radiusInner),
            size = Size(radiusInner * 2, radiusInner * 2),
            style = Stroke(strokeWidth, cap = StrokeCap.Round)
        )
    }
}