package com.bluepilot.remote.ui.screens.airmouse

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bluepilot.remote.model.MouseButton
import com.bluepilot.remote.ui.components.GlassCard
import com.bluepilot.remote.ui.components.KeyCard
import com.bluepilot.remote.ui.components.NotConnectedBanner
import com.bluepilot.remote.ui.components.rememberHaptic
import com.bluepilot.remote.viewmodel.RemoteControlViewModel

/**
 * BLEK-PRO PACK — Air Mouse: wave the phone to move the PC pointer.
 *
 * Gyroscope-based (rotation rate, rad/s), the same approach as TV "magic
 * remotes": yaw (rotation around the phone's Y axis when held upright)
 * moves X, pitch (X axis) moves Y. Movement only happens WHILE the big
 * "HOLD TO MOVE" pad is pressed — the standard clutch mechanic so you can
 * reposition your arm without the cursor flying away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirMouseScreen(
    onBack: () -> Unit,
    viewModel: RemoteControlViewModel = hiltViewModel()
) {
    val isConnected by viewModel.isConnected.collectAsState()
    val vibration by viewModel.vibrationsEnabled.collectAsState()
    val haptic = rememberHaptic(vibration)
    val context = LocalContext.current

    var engaged by remember { mutableStateOf(false) }
    var sensitivity by remember { mutableFloatStateOf(18f) } // px per rad/s
    var hasGyro by remember { mutableStateOf(true) }

    // Sensor lifecycle: listener registered for the whole screen visit
    // (cheap — SENSOR_DELAY_GAME), but deltas are only forwarded while
    // the clutch is held AND we're connected.
    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gyro = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        if (gyro == null) {
            hasGyro = false
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (!engaged) return
                    // event.values: [0]=pitch rate (X axis), [1]=yaw rate (Y axis)
                    // Yaw right = pointer right; pitch down = pointer down.
                    val dx = (-event.values[1] * sensitivity).toInt()
                    val dy = (-event.values[0] * sensitivity).toInt()
                    if (dx != 0 || dy != 0) viewModel.onAirMouseDelta(dx, dy)
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sm.registerListener(listener, gyro, SensorManager.SENSOR_DELAY_GAME)
            onDispose { sm.unregisterListener(listener) }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Air Mouse") },
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

            if (!hasGyro) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "This phone has no gyroscope — Air Mouse needs one. " +
                            "The regular trackpad still works great!",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            // ---------- Clutch pad ----------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        if (engaged) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        RoundedCornerShape(24.dp)
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                engaged = true
                                haptic()
                                tryAwaitRelease()
                                engaged = false
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (engaged) "✈ MOVING…" else "HOLD TO MOVE",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (engaged) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Hold this pad and wave the phone to steer the pointer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // ---------- Sensitivity ----------
            Text(
                "Sensitivity",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = sensitivity,
                onValueChange = { sensitivity = it },
                valueRange = 6f..40f
            )
            Spacer(Modifier.height(4.dp))

            // ---------- Click buttons ----------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KeyCard("Left", modifier = Modifier.weight(2f), height = 64.dp, emphasized = true) {
                    haptic(); viewModel.clickButton(MouseButton.LEFT)
                }
                KeyCard("Right", modifier = Modifier.weight(2f), height = 64.dp) {
                    haptic(); viewModel.clickButton(MouseButton.RIGHT)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
