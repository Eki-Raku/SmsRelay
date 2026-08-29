package com.raku.smsrelay.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import com.raku.smsrelay.onboarding.OnboardingStep

@Stable
class TourTargetRegistry {
    private val targets = mutableStateMapOf<OnboardingStep, Rect>()

    fun bounds(step: OnboardingStep): Rect? = targets[step]

    internal fun update(step: OnboardingStep, bounds: Rect) {
        if (bounds.width > 0f && bounds.height > 0f) targets[step] = bounds
    }
}

val LocalTourTargetRegistry = staticCompositionLocalOf { TourTargetRegistry() }

@Composable
fun rememberTourTargetRegistry(): TourTargetRegistry = remember { TourTargetRegistry() }

fun Modifier.tourTarget(
    step: OnboardingStep,
    registry: TourTargetRegistry,
): Modifier = onGloballyPositioned { coordinates ->
    registry.update(step, coordinates.boundsInWindow())
}
