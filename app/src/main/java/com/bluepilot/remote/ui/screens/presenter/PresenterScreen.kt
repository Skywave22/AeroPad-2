package com.bluepilot.remote.ui.screens.presenter

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bluepilot.remote.model.HidKeys
import com.bluepilot.remote.model.HidModifiers
import com.bluepilot.remote.ui.components.KeyCard
import com.bluepilot.remote.ui.components.NotConnectedBanner
import com.bluepilot.remote.ui.components.rememberHaptic
import com.bluepilot.remote.viewmodel.RemoteControlViewModel

/**
 * Presenter: big next/previous slide keys plus start/end/black-screen
 * controls (PowerPoint, Google Slides, Keynote-on-Windows conventions).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresenterScreen(
    onBack: () -> Unit,
    viewModel: RemoteControlViewModel = hiltViewModel()
) {
    val isConnected by viewModel.isConnected.collectAsState()
    val vibration by viewModel.vibrationsEnabled.collectAsState()
    val haptic = rememberHaptic(vibration)

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Presenter") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // FEATURE: presentation timer — tap to start/pause,
                    // long-press to reset. Keeps you on schedule on stage.
                    var running by androidx.compose.runtime.remember {
                        androidx.compose.runtime.mutableStateOf(false)
                    }
                    var seconds by androidx.compose.runtime.remember {
                        androidx.compose.runtime.mutableIntStateOf(0)
                    }
                    androidx.compose.runtime.LaunchedEffect(running) {
                        while (running) {
                            kotlinx.coroutines.delay(1000)
                            seconds++
                        }
                    }
                    val mm = seconds / 60
                    val ss = seconds % 60
                    androidx.compose.material3.AssistChip(
                        onClick = { running = !running },
                        label = {
                            Text(
                                String.format("%s %02d:%02d", if (running) "⏸" else "▶", mm, ss),
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    if (seconds > 0 && !running) {
                        IconButton(onClick = { seconds = 0 }) {
                            Text("↺", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            )
        }
    ) { padding ->
        // LANDSCAPE FIX — fixed 56dp rows under weighted prev/next squeezed
        // the main buttons to slivers in landscape. Landscape: prev/next
        // dominate (weight 1f) and the secondary keys become ONE compact
        // horizontally-scrollable strip. Portrait unchanged.
        val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE
        if (isLandscape) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NotConnectedBanner(!isConnected)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KeyCard("◀ Previous", Modifier.weight(1f).fillMaxHeight()) {
                        haptic(); viewModel.keyTap(HidKeys.PAGE_UP)
                    }
                    KeyCard("Next ▶", Modifier.weight(1f).fillMaxHeight(), emphasized = true) {
                        haptic(); viewModel.keyTap(HidKeys.PAGE_DOWN)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KeyCard("Start (F5)", Modifier.width(120.dp), 44.dp) {
                        haptic(); viewModel.keyTap(HidKeys.F5)
                    }
                    KeyCard("From here", Modifier.width(120.dp), 44.dp) {
                        haptic(); viewModel.keyTap(HidKeys.F5, HidModifiers.LEFT_SHIFT)
                    }
                    KeyCard("Black (B)", Modifier.width(110.dp), 44.dp) {
                        haptic(); viewModel.keyTap(HidKeys.B)
                    }
                    KeyCard("White (W)", Modifier.width(110.dp), 44.dp) {
                        haptic(); viewModel.keyTap(HidKeys.W)
                    }
                    KeyCard("End (Esc)", Modifier.width(110.dp), 44.dp) {
                        haptic(); viewModel.keyTap(HidKeys.ESCAPE)
                    }
                }
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NotConnectedBanner(!isConnected)

            // Big prev/next — the main controls, huge touch targets.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KeyCard("◀ Previous", Modifier.weight(1f), 160.dp) {
                    haptic(); viewModel.keyTap(HidKeys.PAGE_UP)
                }
                KeyCard("Next ▶", Modifier.weight(1f), 160.dp, emphasized = true) {
                    haptic(); viewModel.keyTap(HidKeys.PAGE_DOWN)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KeyCard("Start (F5)", Modifier.weight(1f), 56.dp) {
                    haptic(); viewModel.keyTap(HidKeys.F5)
                }
                KeyCard("From here (Shift+F5)", Modifier.weight(1.3f), 56.dp) {
                    haptic(); viewModel.keyTap(HidKeys.F5, HidModifiers.LEFT_SHIFT)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KeyCard("Black screen (B)", Modifier.weight(1f), 56.dp) {
                    haptic(); viewModel.keyTap(HidKeys.B)
                }
                KeyCard("White screen (W)", Modifier.weight(1f), 56.dp) {
                    haptic(); viewModel.keyTap(HidKeys.W)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KeyCard("End (Esc)", Modifier.weight(1f), 56.dp) {
                    haptic(); viewModel.keyTap(HidKeys.ESCAPE)
                }
            }
            Text(
                text = "Works with PowerPoint, Google Slides and most presentation apps.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}
