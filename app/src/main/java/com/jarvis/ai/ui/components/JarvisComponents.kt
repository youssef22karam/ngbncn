package com.jarvis.ai.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.jarvis.ai.ui.theme.*
import kotlin.math.*

// ── Arc Reactor / Pulse Ring ────────────────────────────────────────────────
@Composable
fun PulseRing(
    isActive: Boolean,
    amplitude: Float = 0f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue  = 1.05f + amplitude * 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue  = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )

    val ringColor = if (isActive) JarvisBlue else JarvisTextSub.copy(alpha = 0.3f)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Outer glow ring
        if (isActive) {
            Box(
                Modifier
                    .size(120.dp)
                    .scale(scale)
                    .alpha(alpha * 0.3f)
                    .background(JarvisGlowBlue, CircleShape)
            )
            Box(
                Modifier
                    .size(100.dp)
                    .scale(scale)
                    .alpha(alpha * 0.5f)
                    .background(JarvisGlowBlue, CircleShape)
            )
        }
        // Main ring
        Box(
            Modifier
                .size(80.dp)
                .border(2.dp, ringColor, CircleShape)
        )
        // Inner arc reactor hex
        ArcReactorIcon(isActive = isActive, size = 56.dp)
    }
}

@Composable
fun ArcReactorIcon(isActive: Boolean, size: Dp = 56.dp) {
    val color = if (isActive) JarvisBlue else JarvisTextSub.copy(alpha = 0.5f)
    val infiniteTransition = rememberInfiniteTransition(label = "reactor")
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue  = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "glow"
    )

    Box(
        modifier = Modifier
            .size(size)
            .alpha(if (isActive) glow else 0.5f)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        if (isActive) JarvisBlue.copy(alpha = 0.4f) else Color.Transparent,
                        Color.Transparent
                    )
                ),
                CircleShape
            )
            .border(1.5.dp, color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Inner dot
        Box(
            Modifier
                .size(size * 0.3f)
                .background(color, CircleShape)
        )
    }
}

// ── Waveform bars for voice visualization ───────────────────────────────────
@Composable
fun VoiceWaveform(
    amplitude: Float,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 16
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(barCount) { i ->
            val phase = (i.toFloat() / barCount) * PI.toFloat() * 2
            val animValue by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue  = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 400 + (i * 30),
                        easing = EaseInOutSine
                    ),
                    repeatMode = RepeatMode.Reverse
                ), label = "bar$i"
            )
            val height = if (isActive) {
                val base   = sin(phase + animValue * PI.toFloat()).absoluteValue
                val amp    = amplitude.coerceIn(0f, 1f)
                (8 + (base * 32 * amp) + (animValue * 16 * amp)).dp
            } else {
                4.dp
            }
            Box(
                Modifier
                    .width(3.dp)
                    .height(height.coerceAtLeast(4.dp))
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isActive) JarvisBlue else JarvisTextSub.copy(alpha = 0.3f)
                    )
            )
        }
    }
}

// ── Glowing header text ──────────────────────────────────────────────────────
@Composable
fun GlowText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 28.sp,
    color: Color = JarvisBlue,
    letterSpacing: TextUnit = 6.sp
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.headlineLarge.copy(
            fontSize     = fontSize,
            color        = color,
            letterSpacing = letterSpacing,
            fontWeight   = FontWeight.Light,
            shadow = Shadow(
                color  = color.copy(alpha = 0.8f),
                offset = Offset(0f, 0f),
                blurRadius = 20f
            )
        )
    )
}

// ── Status badge ─────────────────────────────────────────────────────────────
@Composable
fun StatusBadge(
    label: String,
    color: Color = JarvisBlue,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .border(0.5.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text  = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontSize = 8.sp,
            letterSpacing = 1.sp
        )
    }
}

// ── HUD border card ───────────────────────────────────────────────────────────
@Composable
fun HudCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(JarvisCardBg, RoundedCornerShape(12.dp))
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        JarvisBlue.copy(alpha = 0.6f),
                        JarvisDivider,
                        JarvisBlue.copy(alpha = 0.3f)
                    )
                ),
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        content = content
    )
}

// ── Loading dots ─────────────────────────────────────────────────────────────
@Composable
fun ThinkingDots(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { i ->
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue  = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, delayMillis = i * 150, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ), label = "dot$i"
            )
            Box(
                Modifier
                    .size(6.dp)
                    .scale(scale)
                    .background(JarvisBlue, CircleShape)
            )
        }
    }
}

// ── Scanline overlay ─────────────────────────────────────────────────────────
@Composable
fun ScanlineOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "scanline"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .alpha(0.15f)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, JarvisBlue, Color.Transparent)
                )
            )
    )
}
