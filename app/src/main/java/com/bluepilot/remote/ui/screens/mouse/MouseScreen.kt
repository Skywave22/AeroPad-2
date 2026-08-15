package com.bluepilot.remote.ui.screens.mouse

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bluepilot.remote.model.MouseButton
import com.bluepilot.remote.ui.components.GlassCard
import com.bluepilot.remote.ui.components.KeyCard
import com.bluepilot.remote.ui.components.HintBar
import com.bluepilot.remote.ui.components.NotConnectedBanner
import com.bluepilot.remote.ui.components.rememberHaptic
import com.bluepilot.remote.ui.theme.LocalAppTheme
import com.bluepilot.remote.viewmodel.RemoteControlViewModel

/**
 * Mouse screen: trackpad (drag = move, tap = left click, double-tap =
 * double click, long-press = right click), scroll strip, and L/M/R buttons.
 * All motion runs through PointerMath with the user's settings applied.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MouseScreen(
    onBack: () -> Unit,
    viewModel: RemoteControlViewModel = hiltViewModel()
) {
    val isConnected by viewModel.isConnected.collectAsState()
    val vibration by viewModel.vibrationsEnabled.collectAsState()
    val haptic = rememberHaptic(vibration)
    val spec = LocalAppTheme.current

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(if (spec.monoFont) "MOUSE" else "Mouse") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            NotConnectedBanner(!isConnected)
            HintBar("2-finger tap = right-click • 3-finger swipe switches apps • pinch zooms")

            // AEROPAD v1.0 — #19 Precision Mode + #22 Drag Lock chips.
            run {
                val precision by viewModel.precisionMode.collectAsState()
                val dragLock by viewModel.dragLock.collectAsState()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.FilterChip(
                        selected = precision,
                        onClick = { viewModel.setPrecisionMode(!precision) },
                        label = { Text(if (precision) "Precision ON (slow)" else "Precision mode") }
                    )
                    androidx.compose.material3.FilterChip(
                        selected = dragLock,
                        onClick = { viewModel.toggleDragLock() },
                        label = { Text(if (dragLock) "Drag locked — tap to release" else "Drag lock") }
                    )
                }
            }

            Row(modifier = Modifier.weight(1f)) {
                // ---------- Trackpad ----------
                GlassCard(
                    modifier = Modifier
                        .weight(1f).shadow3DPad()
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { haptic(); viewModel.onTrackpadTap() },
                                onDoubleTap = { haptic(); viewModel.onTrackpadDoubleTap() },
                                onLongPress = { haptic(); viewModel.onTrackpadLongPress() }
                            )
                        }
                        .pointerInput(viewModel) {
                            // FEATURE: real multi-touch trackpad.
                            // 1 finger = move pointer, 2 fingers = scroll
                            // (the hint always promised two-finger scroll —
                            // now it actually works).
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                viewModel.onTrackpadGestureStart()
                                var scrollAccum = 0f
                                // BLEK-PRO v3 — gesture classification state:
                                // peak finger count + total travel decide, on
                                // release, between 2-finger tap (right-click)
                                // and 3-finger swipe (app switch / desktop /
                                // task view). Travel gates prevent misfires.
                                var maxFingers = 1
                                var travel = 0f
                                var threeX = 0f
                                var threeY = 0f
                                val gestureStart = System.currentTimeMillis()
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val pressed = event.changes.filter { it.pressed }
                                    if (pressed.isEmpty()) break
                                    if (pressed.size > maxFingers) maxFingers = pressed.size
                                    travel += pressed.maxOf {
                                        (it.position - it.previousPosition).getDistance()
                                    }
                                    if (pressed.size >= 3) {
                                        // Accumulate common 3-finger motion.
                                        threeX += pressed.map { it.position.x - it.previousPosition.x }
                                            .average().toFloat()
                                        threeY += pressed.map { it.position.y - it.previousPosition.y }
                                            .average().toFloat()
                                        event.changes.forEach { it.consume() }
                                        continue
                                    }
                                    if (pressed.size >= 2) {
                                        // FEATURE: pinch-to-zoom vs two-finger scroll.
                                        // Compare finger-distance change (pinch)
                                        // against common Y motion (scroll).
                                        val a = pressed[0]; val b = pressed[1]
                                        val distNow = (a.position - b.position).getDistance()
                                        val distPrev = (a.previousPosition - b.previousPosition).getDistance()
                                        val pinchDelta = distNow - distPrev
                                        val dy = pressed
                                            .map { it.position.y - it.previousPosition.y }
                                            .average().toFloat()
                                        if (kotlin.math.abs(pinchDelta) > kotlin.math.abs(dy) * 1.5f &&
                                            distPrev > 0f
                                        ) {
                                            viewModel.onPinchZoom(distNow / distPrev)
                                        } else {
                                            scrollAccum += dy
                                            if (kotlin.math.abs(scrollAccum) > 8f) {
                                                viewModel.onScrollDelta(scrollAccum)
                                                scrollAccum = 0f
                                            }
                                        }
                                    } else {
                                        // Don't jerk the pointer with the last
                                        // lingering finger of a multi-touch
                                        // gesture lifting off.
                                        if (maxFingers == 1) {
                                            val c = pressed.first()
                                            val d = c.position - c.previousPosition
                                            viewModel.onTrackpadDelta(d.x, d.y)
                                        }
                                    }
                                    event.changes.forEach { it.consume() }
                                }
                                // ---- Gesture release: classify ----
                                val elapsed = System.currentTimeMillis() - gestureStart
                                if (maxFingers == 2 && travel < 24f && elapsed < 300) {
                                    // Two-finger tap → right-click.
                                    haptic(); viewModel.onTwoFingerTap()
                                } else if (maxFingers >= 3) {
                                    val ax = kotlin.math.abs(threeX)
                                    val ay = kotlin.math.abs(threeY)
                                    if (ax > 60f || ay > 60f) {
                                        haptic()
                                        viewModel.onThreeFingerSwipe(
                                            horizontal = ax >= ay,
                                            positive = threeY > 0f
                                        )
                                    }
                                }
                            }
                        }
                ) {
                    // Specular streak overlay running diagonally
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.0f),
                                        Color.White.copy(alpha = 0.08f),
                                        Color.White.copy(alpha = 0.0f)
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(300f, 850f)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // STITCH REDESIGN — ghosted watermark caption.
                        Text(
                            text = "TRACKPAD AREA",
                            style = MaterialTheme.typography.titleMedium.copy(
                                letterSpacing = androidx.compose.ui.unit.TextUnit(
                                    3f, androidx.compose.ui.unit.TextUnitType.Sp)
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // ---------- Scroll strip ----------
                GlassCard(
                    modifier = Modifier
                        .width(56.dp)
                        .fillMaxHeight()
                        .pointerInput(viewModel) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                viewModel.onScrollDelta(dragAmount.y)
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "S\nC\nR\nO\nL\nL",
                            style = if (spec.monoFont) MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                            else MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ---------- Mouse buttons (LAYOUT v3: taller, cleaner) ----------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KeyCard("Left", modifier = Modifier.weight(2f), height = 64.dp, emphasized = true) {
                    haptic(); viewModel.clickButton(MouseButton.LEFT)
                }
                KeyCard("•", modifier = Modifier.weight(0.7f), height = 64.dp) {
                    haptic(); viewModel.clickButton(MouseButton.MIDDLE)
                }
                KeyCard("Right", modifier = Modifier.weight(2f), height = 64.dp) {
                    haptic(); viewModel.clickButton(MouseButton.RIGHT)
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

/** SECTION 5 - 3D pad lift: frosted surface floats above background. */
private fun Modifier.shadow3DPad(): Modifier =
    this.graphicsLayer { shadowElevation = 8f * density }
