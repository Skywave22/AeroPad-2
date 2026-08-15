package com.bluepilot.remote.ui.screens.scanner

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bluepilot.remote.model.HidKeys
import com.bluepilot.remote.ui.components.GlassCard
import com.bluepilot.remote.ui.components.NotConnectedBanner
import com.bluepilot.remote.ui.components.rememberHaptic
import com.bluepilot.remote.viewmodel.RemoteControlViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 * BLEK-PRO PACK — Scanner mode: scan QR codes and barcodes with the camera
 * and type the content straight into the connected PC. Great for inventory,
 * WiFi passwords, serial numbers, URLs.
 *
 * The camera capture itself is ZXing's battle-tested CaptureActivity
 * (handles its own CAMERA runtime permission). This screen is the result
 * hub: history of scans this session, send / send+Enter / rescan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onBack: () -> Unit,
    viewModel: RemoteControlViewModel = hiltViewModel()
) {
    val isConnected by viewModel.isConnected.collectAsState()
    val vibration by viewModel.vibrationsEnabled.collectAsState()
    val haptic = rememberHaptic(vibration)

    // Session scan history, newest first.
    val scans = remember { mutableStateListOf<String>() }
    var autoSend by remember { mutableStateOf(false) }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val content = result.contents
        if (!content.isNullOrBlank()) {
            scans.add(0, content)
            if (autoSend) {
                viewModel.typeText(content)
                viewModel.keyTap(HidKeys.ENTER)
            }
        }
    }

    fun launchScan() {
        scanLauncher.launch(
            ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
                setPrompt("Point at a QR code or barcode")
                setBeepEnabled(false)
                setOrientationLocked(true)
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Scanner") },
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
                .verticalScroll(rememberScrollState())
        ) {
            NotConnectedBanner(!isConnected)

            // ---------- Scan button ----------
            Button(
                onClick = { haptic(); launchScan() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Icon(Icons.Rounded.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.padding(horizontal = 6.dp))
                Text("Scan QR / barcode", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(10.dp))

            androidx.compose.material3.FilterChip(
                selected = autoSend,
                onClick = { autoSend = !autoSend },
                label = {
                    Text(if (autoSend) "Auto-send ON (types + Enter after each scan)" else "Auto-send to PC")
                }
            )
            Spacer(Modifier.height(16.dp))

            // ---------- Results ----------
            if (scans.isEmpty()) {
                Text(
                    "Scans appear here. Tap one to type it on the PC — perfect for " +
                        "WiFi passwords, serial numbers, links and inventory work.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "SCANNED THIS SESSION",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                scans.forEach { content ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(4.dp)) {
                            Text(
                                content,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 4
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { haptic(); viewModel.typeText(content) },
                                    enabled = isConnected
                                ) { Text("Type on PC") }
                                OutlinedButton(
                                    onClick = {
                                        haptic()
                                        viewModel.typeText(content)
                                        viewModel.keyTap(HidKeys.ENTER)
                                    },
                                    enabled = isConnected
                                ) { Text("Type + Enter") }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(90.dp))
        }
    }
}
