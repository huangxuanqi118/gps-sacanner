package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class RadarBlip(
    val id: String,
    val distanceRatio: Float, // 0.15f (close) .. 0.88f (far)
    val isAlert: Boolean,
    val isWarning: Boolean
)

@Composable
fun RadarCanvas(
    isScanning: Boolean,
    accentColor: Color,
    blips: List<RadarBlip>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_sweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )

    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
    val alertColor = MaterialTheme.colorScheme.error
    val warnColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = modifier
            .size(188.dp)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f - 6.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)

            // Subtle radial background glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.16f),
                        accentColor.copy(alpha = 0.03f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // Concentric range rings
            listOf(0.33f, 0.66f, 1.0f).forEach { ratio ->
                drawCircle(
                    color = gridColor,
                    radius = radius * ratio,
                    center = center,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }

            // Crosshair lines
            drawLine(
                color = gridColor.copy(alpha = 0.45f),
                start = Offset(center.x - radius, center.y),
                end = Offset(center.x + radius, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = gridColor.copy(alpha = 0.45f),
                start = Offset(center.x, center.y - radius),
                end = Offset(center.x, center.y + radius),
                strokeWidth = 1.dp.toPx()
            )

            if (isScanning) {
                // Expanding pulse ring
                drawCircle(
                    color = accentColor.copy(alpha = (1f - pulseScale) * 0.5f),
                    radius = radius * pulseScale,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Rotating sweep sector
                rotate(degrees = sweepAngle, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            0.0f to Color.Transparent,
                            0.75f to Color.Transparent,
                            0.96f to accentColor.copy(alpha = 0.32f),
                            1.0f to accentColor.copy(alpha = 0.65f),
                            center = center
                        ),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = true,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f)
                    )
                    drawLine(
                        color = accentColor,
                        start = center,
                        end = Offset(center.x + radius, center.y),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Signal blips positioned deterministically from ID hash and strength
            blips.take(18).forEach { blip ->
                val angleRad = ((blip.id.hashCode() and 0xFFFF) % 360) * (PI / 180.0)
                val dist = radius * blip.distanceRatio.coerceIn(0.18f, 0.9f)
                val bx = center.x + (dist * cos(angleRad)).toFloat()
                val by = center.y + (dist * sin(angleRad)).toFloat()
                val dotColor = when {
                    blip.isAlert -> alertColor
                    blip.isWarning -> warnColor
                    else -> accentColor
                }
                drawCircle(
                    color = dotColor.copy(alpha = 0.28f),
                    radius = 9.dp.toPx(),
                    center = Offset(bx, by)
                )
                drawCircle(
                    color = dotColor,
                    radius = 4.dp.toPx(),
                    center = Offset(bx, by)
                )
            }

            // Center emitter dot
            drawCircle(
                color = accentColor.copy(alpha = 0.35f),
                radius = 10.dp.toPx(),
                center = center
            )
            drawCircle(
                color = accentColor,
                radius = 5.dp.toPx(),
                center = center
            )
        }
    }
}
