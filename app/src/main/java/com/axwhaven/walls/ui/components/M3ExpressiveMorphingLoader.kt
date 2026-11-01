package com.axwhaven.walls.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.axwhaven.walls.ui.theme.AxWhavenColors
import com.axwhaven.walls.ui.theme.LocalAxWhavenColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Android 17 / Material 3 Expressive Contained Morphing Loader.
 * Features a squircle/circle container and an inner polygon continuously morphing
 * across squircle, 4-pointed star/diamond, pill, and octagon/circle geometries.
 */
@Composable
fun M3ExpressiveMorphingLoader(
    modifier: Modifier = Modifier,
    loaderSize: Dp = 56.dp,
    colors: AxWhavenColors = LocalAxWhavenColors.current,
    containerColor: Color? = null,
    indicatorColor: Color? = null
) {
    val actualContainerColor = containerColor ?: if (colors.isGlassmorphism) {
        colors.mintPillBg
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    val actualIndicatorColor = indicatorColor ?: if (colors.isGlassmorphism) {
        colors.mintText
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    val infiniteTransition = rememberInfiniteTransition(label = "M3MorphingLoader")

    // Continuous rotation across 360 degrees
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LoaderRotation"
    )

    // Continuous morph progress through 4 phases (0: squircle, 1: star, 2: pill, 3: octagon/circle)
    val morphProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "MorphProgress"
    )

    // Subtle breathing scale pulse
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val containerCornerRadius = (loaderSize * 0.36f).coerceAtLeast(0.dp)
    val containerShape = RoundedCornerShape(containerCornerRadius)

    Box(
        modifier = modifier
            .size(loaderSize)
            .clip(containerShape)
            .background(actualContainerColor)
            .then(
                if (colors.isGlassmorphism) {
                    Modifier.border(1.dp, Color(0x60FFFFFF), containerShape)
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(loaderSize * 0.58f)
        ) {
            val canvasWidth = this.size.width
            val canvasHeight = this.size.height
            val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
            val baseRadius = (canvasWidth / 2f) * pulseScale

            rotate(degrees = rotation, pivot = center) {
                val path = calculateMorphedPolygonPath(
                    center = center,
                    baseRadius = baseRadius,
                    progress = morphProgress
                )
                drawPath(path = path, color = actualIndicatorColor)
            }
        }
    }
}

/**
 * Computes a smooth closed bezier path for the current morph progress [0..4).
 * Phases:
 * 0 -> 1: Squircle to 4-point Diamond/Star
 * 1 -> 2: 4-point Star to Capsule/Pill
 * 2 -> 3: Capsule/Pill to Octagon/Circle
 * 3 -> 4: Octagon/Circle back to Squircle
 */
private fun calculateMorphedPolygonPath(
    center: Offset,
    baseRadius: Float,
    progress: Float
): Path {
    val phase = progress % 4f
    val segment = phase.toInt()
    val t = phase - segment // local progress [0..1) within current transition segment

    // 8 sample angular points at 45 degree intervals around the circle
    // angles: 0, 45, 90, 135, 180, 225, 270, 315 degrees
    val r0: FloatArray // Squircle
    val r1: FloatArray // Star / Diamond
    val r2: FloatArray // Pill
    val r3: FloatArray // Octagon / Circle

    r0 = floatArrayOf(0.92f, 0.72f, 0.92f, 0.72f, 0.92f, 0.72f, 0.92f, 0.72f)
    r1 = floatArrayOf(1.00f, 0.38f, 1.00f, 0.38f, 1.00f, 0.38f, 1.00f, 0.38f)
    r2 = floatArrayOf(1.00f, 0.75f, 0.52f, 0.75f, 1.00f, 0.75f, 0.52f, 0.75f)
    r3 = floatArrayOf(0.88f, 0.88f, 0.88f, 0.88f, 0.88f, 0.88f, 0.88f, 0.88f)

    val (fromR, toR) = when (segment) {
        0 -> r0 to r1
        1 -> r1 to r2
        2 -> r2 to r3
        else -> r3 to r0
    }

    val currentR = FloatArray(8) { i ->
        val easeT = 0.5f - 0.5f * cos(t * PI.toFloat()) // Smooth cosine easing
        (fromR[i] + (toR[i] - fromR[i]) * easeT) * baseRadius
    }

    val points = Array(8) { i ->
        val angle = (i * 45f) * (PI.toFloat() / 180f)
        Offset(
            x = center.x + currentR[i] * cos(angle),
            y = center.y + currentR[i] * sin(angle)
        )
    }

    val path = Path()
    // Start at midpoint of first segment for smooth closed bezier loop
    val startMid = Offset(
        (points[0].x + points[7].x) / 2f,
        (points[0].y + points[7].y) / 2f
    )
    path.moveTo(startMid.x, startMid.y)

    for (i in 0 until 8) {
        val curr = points[i]
        val next = points[(i + 1) % 8]
        val mid = Offset((curr.x + next.x) / 2f, (curr.y + next.y) / 2f)
        path.quadraticTo(curr.x, curr.y, mid.x, mid.y)
    }
    path.close()
    return path
}
