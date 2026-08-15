package com.bluepilot.remote.ui.screens.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Mouse
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bluepilot.remote.model.HidConnectionState
import com.bluepilot.remote.ui.components.GlassCard
import com.bluepilot.remote.ui.navigation.Routes
import com.bluepilot.remote.ui.theme.LocalAppTheme
import com.bluepilot.remote.viewmodel.ConnectionViewModel

/**
 * HOME v3 — clean hero-card interface.
 *
 * Layout, top to bottom:
 *  1. Header       — app name + live status pill.
 *  2. Hero card    — full-width connection card: status, device, and a
 *                    context action (Connect / quick actions).
 *  3. Controls     — 2-column grid of the five control surfaces, each a
 *                    color-tinted tile with a large icon.
 *  4. Footer row   — Settings · Help side by side.
 *
 * Everything scrolls in one column; the floating dock overlays the
 * bottom. No orbs, no tiers-of-tiers — one glance, one tap.
 */

private data class Tile(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String,
    val tint: Long
)

private val controlTiles = listOf(
    Tile("Mouse", "Trackpad & clicks", Icons.Rounded.Mouse, Routes.MOUSE, 0xFF3D8BFF),
    Tile("Keyboard", "Type & shortcuts", Icons.Rounded.Keyboard, Routes.KEYBOARD, 0xFF9B59F6),
    Tile("Gamepad", "Play games", Icons.Rounded.Gamepad, Routes.GAMEPAD, 0xFFFF5C8A),
    Tile("Media", "Volume & playback", Icons.Rounded.MusicNote, Routes.MULTIMEDIA, 0xFF17C3CE),
    Tile("Presenter", "Slides & timer", Icons.Rounded.Slideshow, Routes.PRESENTER, 0xFFF5C542),
    // BLEK-PRO PACK
    Tile("Air Mouse", "Wave to point", Icons.Rounded.Air, Routes.AIR_MOUSE, 0xFF35D07F),
    Tile("Scanner", "QR & barcodes to PC", Icons.Rounded.QrCodeScanner, Routes.SCANNER, 0xFFFF8A3D)
)

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    viewModel: ConnectionViewModel = hiltViewModel()
) {
    val state by viewModel.connectionState.collectAsState()
    val permissionsGranted by viewModel.permissionsGranted.collectAsState()
    val spec = LocalAppTheme.current

    LaunchedEffect(Unit) { viewModel.initialize() }

    fun openConnect() {
        if (!permissionsGranted) onNavigate(Routes.PERMISSIONS)
        else onNavigate(Routes.CONNECTION)
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(14.dp))

            // ---------- 1. Header ----------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AeroPad",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                StatusPill(state)
            }
            Spacer(Modifier.height(16.dp))

            // ---------- 2. Hero connection card ----------
            HeroConnectionCard(
                state = state,
                onConnect = { openConnect() },
                onDisconnect = { viewModel.disconnect() }
            )
            Spacer(Modifier.height(12.dp))

            // Context strip: quick actions when connected,
            // quick-connect chips when not.
            if (state.isConnected) {
                QuickActionsRow()
                Spacer(Modifier.height(18.dp))
            } else {
                val saved by viewModel.savedHosts.collectAsState()
                if (saved.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        saved.take(6).forEach { host ->
                            androidx.compose.material3.AssistChip(
                                onClick = { viewModel.connect(host.address) },
                                label = { Text("⚡ " + host.label.take(18)) }
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                } else {
                    Spacer(Modifier.height(6.dp))
                }
            }

            // ---------- 3. Controls grid ----------
            Text(
                text = "CONTROLS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            controlTiles.chunked(2).forEach { rowTiles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowTiles.forEach { tile ->
                        ControlTile(
                            tile = tile,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(tile.route) }
                        )
                    }
                    if (rowTiles.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(8.dp))

            // ---------- 4. Footer: Settings · Help ----------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FooterButton(
                    label = "Settings",
                    icon = Icons.Rounded.Settings,
                    modifier = Modifier.weight(1f)
                ) { onNavigate(Routes.SETTINGS) }
                FooterButton(
                    label = "Help",
                    icon = Icons.AutoMirrored.Rounded.HelpOutline,
                    modifier = Modifier.weight(1f)
                ) { onNavigate(Routes.HELP) }
            }

            // Space to scroll past the floating dock + gesture bar.
            Spacer(Modifier.height(110.dp))
        }
    }
}

/** Small live status pill in the header. */
@Composable
private fun StatusPill(state: HidConnectionState) {
    val spec = LocalAppTheme.current
    val (label, color) = when (state) {
        is HidConnectionState.Connected -> "● Online" to spec.connected
        is HidConnectionState.Connecting -> "◐ Connecting" to spec.primary
        else -> "○ Offline" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val animated by animateColorAsState(color, tween(300), label = "pillColor")
    Box(
        modifier = Modifier
            .background(animated.copy(alpha = 0.12f), CircleShape)
            .border(1.dp, animated.copy(alpha = 0.4f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = animated)
    }
}

/**
 * Hero card: device icon in a tinted coin + status text + action button.
 * One card, one clear action — replaces the old Command Orb.
 */
@Composable
private fun HeroConnectionCard(
    state: HidConnectionState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val spec = LocalAppTheme.current
    val connected = state is HidConnectionState.Connected
    val accent = if (connected) spec.connected else spec.primary

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(accent.copy(alpha = 0.35f), accent.copy(alpha = 0.08f))
                            ),
                            RoundedCornerShape(16.dp)
                        )
                        .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Computer,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    val (title, sub) = when (state) {
                        is HidConnectionState.Connected ->
                            state.device.name to "Connected — ready to control"
                        is HidConnectionState.Connecting ->
                            state.device.name to "Connecting…"
                        HidConnectionState.BluetoothDisabled ->
                            "Bluetooth is off" to "Turn on Bluetooth to connect"
                        HidConnectionState.PermissionMissing ->
                            "Setup needed" to "Grant Bluetooth permissions"
                        is HidConnectionState.Error ->
                            "Not connected" to state.message
                        else -> "Not connected" to "Pair once, connect in one tap"
                    }
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            if (connected) {
                OutlinedButton(
                    onClick = onDisconnect,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Disconnect") }
            } else {
                Button(
                    onClick = onConnect,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = spec.primary)
                ) {
                    Icon(
                        Icons.Rounded.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("Connect to a PC")
                }
            }
        }
    }
}

/** Instant PC controls while connected. */
@Composable
private fun QuickActionsRow() {
    val remote: com.bluepilot.remote.viewmodel.RemoteControlViewModel = hiltViewModel()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        androidx.compose.material3.AssistChip(
            onClick = {
                remote.keyTap(
                    com.bluepilot.remote.model.HidKeys.L,
                    com.bluepilot.remote.model.HidModifiers.LEFT_GUI
                )
            },
            label = { Text("🔒 Lock PC") }
        )
        androidx.compose.material3.AssistChip(
            onClick = {
                remote.keyTap(
                    com.bluepilot.remote.model.HidKeys.D,
                    com.bluepilot.remote.model.HidModifiers.LEFT_GUI
                )
            },
            label = { Text("🖥 Desktop") }
        )
        androidx.compose.material3.AssistChip(
            onClick = { remote.mediaTap(com.bluepilot.remote.model.HidConsumer.PLAY_PAUSE) },
            label = { Text("⏯ Media") }
        )
        androidx.compose.material3.AssistChip(
            onClick = { remote.mediaTap(com.bluepilot.remote.model.HidConsumer.MUTE) },
            label = { Text("🔇 Mute") }
        )
    }
}

/**
 * Control tile: icon coin on a tinted gradient, title + subtitle.
 * Fixed height keeps the grid rhythm; large touch target.
 */
@Composable
private fun ControlTile(tile: Tile, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val tint = Color(tile.tint)
    val spec = LocalAppTheme.current
    Column(
        modifier = modifier
            .height(112.dp)
            .background(
                Brush.verticalGradient(
                    listOf(tint.copy(alpha = 0.16f), spec.surface.copy(alpha = spec.surfaceAlpha))
                ),
                RoundedCornerShape(18.dp)
            )
            .border(1.dp, tint.copy(alpha = 0.30f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(tint.copy(alpha = 0.22f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(tile.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Column {
            Text(
                tile.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                tile.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/** Compact footer button (Settings / Help). */
@Composable
private fun FooterButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val spec = LocalAppTheme.current
    Row(
        modifier = modifier
            .background(spec.surface.copy(alpha = spec.surfaceAlpha), RoundedCornerShape(14.dp))
            .border(1.dp, spec.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.size(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
