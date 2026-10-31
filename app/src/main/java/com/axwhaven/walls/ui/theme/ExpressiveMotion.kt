package com.axwhaven.walls.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Centralized Material 3 Expressive motion tokens.
 * Provides literal spring specs for spatial movements and effects.
 */
object ExpressiveMotion {

    // =========================================================================
    // SPATIAL SPRINGS (Position, Size, Scale, Rotation, Corner Radius; Overshoot Allowed)
    // =========================================================================

    /**
     * FastSpatial: dampingRatio = 0.6f, stiffness = 800f
     * Used for Dice button, top segmented pill, bottom nav pill, icon pops, and interactive corner morphing.
     */
    fun <T> fastSpatial(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(
            dampingRatio = 0.6f,
            stiffness = 800f,
            visibilityThreshold = visibilityThreshold
        )

    val FastSpatialFloat: SpringSpec<Float> = fastSpatial(visibilityThreshold = 0.001f)
    val FastSpatialDp: SpringSpec<Dp> = fastSpatial(visibilityThreshold = Dp.VisibilityThreshold)
    val FastSpatialIntOffset: SpringSpec<IntOffset> = fastSpatial(visibilityThreshold = IntOffset.VisibilityThreshold)
    val FastSpatialIntSize: SpringSpec<IntSize> = fastSpatial(visibilityThreshold = IntSize.VisibilityThreshold)

    /**
     * DefaultSpatial: dampingRatio = 0.8f, stiffness = 380f
     * Used for horizontal page transitions (All Wallpapers <-> Collections; Home <-> AxW Surf <-> Settings) and header collapse.
     */
    fun <T> defaultSpatial(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(
            dampingRatio = 0.8f,
            stiffness = 380f,
            visibilityThreshold = visibilityThreshold
        )

    val DefaultSpatialFloat: SpringSpec<Float> = defaultSpatial(visibilityThreshold = 0.001f)
    val DefaultSpatialDp: SpringSpec<Dp> = defaultSpatial(visibilityThreshold = Dp.VisibilityThreshold)
    val DefaultSpatialIntOffset: SpringSpec<IntOffset> = defaultSpatial(visibilityThreshold = IntOffset.VisibilityThreshold)
    val DefaultSpatialIntSize: SpringSpec<IntSize> = defaultSpatial(visibilityThreshold = IntSize.VisibilityThreshold)

    /**
     * SlowSpatial: dampingRatio = 0.8f, stiffness = 200f
     * Used for full-screen and large container expansions.
     */
    fun <T> slowSpatial(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(
            dampingRatio = 0.8f,
            stiffness = 200f,
            visibilityThreshold = visibilityThreshold
        )

    val SlowSpatialFloat: SpringSpec<Float> = slowSpatial(visibilityThreshold = 0.001f)
    val SlowSpatialDp: SpringSpec<Dp> = slowSpatial(visibilityThreshold = Dp.VisibilityThreshold)
    val SlowSpatialIntOffset: SpringSpec<IntOffset> = slowSpatial(visibilityThreshold = IntOffset.VisibilityThreshold)
    val SlowSpatialIntSize: SpringSpec<IntSize> = slowSpatial(visibilityThreshold = IntSize.VisibilityThreshold)

    // =========================================================================
    // EFFECTS SPRINGS (Critically Damped, Zero Bounce: Damping = 1f; Alpha & Color)
    // =========================================================================

    /**
     * FastEffects: dampingRatio = 1f, stiffness = 3800f
     * Used for icon/text tint and press highlights.
     */
    fun <T> fastEffects(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(
            dampingRatio = 1f,
            stiffness = 3800f,
            visibilityThreshold = visibilityThreshold
        )

    val FastEffectsFloat: SpringSpec<Float> = fastEffects(visibilityThreshold = 0.001f)
    val FastEffectsColor: SpringSpec<Color> = fastEffects()

    /**
     * DefaultEffects: dampingRatio = 1f, stiffness = 1600f
     * Used for all fadeIn/fadeOut and container color transitions.
     */
    fun <T> defaultEffects(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(
            dampingRatio = 1f,
            stiffness = 1600f,
            visibilityThreshold = visibilityThreshold
        )

    val DefaultEffectsFloat: SpringSpec<Float> = defaultEffects(visibilityThreshold = 0.001f)
    val DefaultEffectsColor: SpringSpec<Color> = defaultEffects()
    val DefaultEffectsDp: SpringSpec<Dp> = defaultEffects(visibilityThreshold = Dp.VisibilityThreshold)
}
