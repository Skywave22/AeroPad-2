package com.bluepilot.remote.ui.screens.custom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.hilt.navigation.compose.hiltViewModel
import com.bluepilot.remote.model.custom.ActionCatalog
import com.bluepilot.remote.model.custom.CustomButton
import com.bluepilot.remote.ui.components.KeyCard
import com.bluepilot.remote.ui.components.NotConnectedBanner
import com.bluepilot.remote.ui.components.rememberHaptic
import com.bluepilot.remote.viewmodel.CustomRemoteViewModel
import com.bluepilot.remote.viewmodel.RemoteControlViewModel

/**
 * BLEK-PRO PACK v2 — Custom Remote: build your own remote control.
 *
 * Users create buttons; each button is a sequence of steps (actions from
 * the catalog and/or literal text). One step = a plain custom key.
 * Several steps = a macro. The grid IS the remote — big 2-column KeyCards.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomRemoteScreen(
    onBack: () -> Unit,
    viewModel: CustomRemoteViewModel = hiltViewModel(),
    remote: RemoteControlViewModel = hiltViewModel()
) {
    val isConnected by viewModel.isConnected.collectAsState()
    val vibration by remote.vibrationsEnabled.collectAsState()
    val haptic = rememberHaptic(vibration)
    val buttons by viewModel.buttons.collectAsState()

    var showBuilder by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Custom Remote") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showBuilder = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add button")
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

            if (buttons.isEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    "Your remote, your rules",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tap + to create buttons for the things YOU do: a one-tap " +
                        "login macro (text → Tab → text → Enter), a Netflix " +
                        "fullscreen key, your favorite app shortcuts…\n\n" +
                        "One step = a key. Many steps = a macro.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { showBuilder = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("Create first button")
                }
            } else {
                Spacer(Modifier.height(6.dp))
                var editMode by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${buttons.size} button" + (if (buttons.size == 1) "" else "s"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.material3.FilterChip(
                        selected = editMode,
                        onClick = { editMode = !editMode },
                        label = { Text(if (editMode) "Done" else "Edit") }
                    )
                }
                Spacer(Modifier.height(10.dp))
                buttons.chunked(2).forEach { rowButtons ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowButtons.forEach { btn ->
                            if (editMode) {
                                KeyCard(
                                    "✕ " + btn.label,
                                    modifier = Modifier.weight(1f),
                                    height = 64.dp
                                ) { haptic(); viewModel.remove(btn.id) }
                            } else {
                                KeyCard(
                                    btn.label,
                                    modifier = Modifier.weight(1f),
                                    height = 64.dp,
                                    emphasized = btn.steps.size > 1,
                                    enabled = isConnected
                                ) { haptic(); viewModel.run(btn) }
                            }
                        }
                        if (rowButtons.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                }
                if (!editMode) {
                    Text(
                        "Highlighted buttons are macros (multiple steps).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(100.dp))
        }
    }

    if (showBuilder) {
        ButtonBuilderSheet(
            onDismiss = { showBuilder = false },
            onSave = { label, steps ->
                viewModel.add(label, steps)
                showBuilder = false
            }
        )
    }
}

/**
 * Bottom-sheet builder: name the button, stack steps from the action
 * catalog and/or text entries, save.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ButtonBuilderSheet(
    onDismiss: () -> Unit,
    onSave: (String, List<String>) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var textStep by remember { mutableStateOf("") }
    val steps = remember { mutableStateListOf<String>() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Text("New button", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = label,
                onValueChange = { label = it.take(CustomButton.LABEL_MAX) },
                label = { Text("Button name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            // ---------- Current steps ----------
            if (steps.isNotEmpty()) {
                Text(
                    "STEPS (run in order — tap to remove)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    steps.forEachIndexed { i, step ->
                        androidx.compose.material3.InputChip(
                            selected = false,
                            onClick = { steps.removeAt(i) },
                            label = { Text("${i + 1}. " + ActionCatalog.stepLabel(step)) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ---------- Add text step ----------
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = textStep,
                    onValueChange = { textStep = it },
                    label = { Text("Add text step (types this)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.padding(horizontal = 4.dp))
                OutlinedButton(
                    onClick = {
                        if (textStep.isNotBlank() && steps.size < CustomButton.STEPS_MAX) {
                            steps.add("t:" + textStep)
                            textStep = ""
                        }
                    },
                    enabled = textStep.isNotBlank() && steps.size < CustomButton.STEPS_MAX
                ) { Text("Add") }
            }
            Spacer(Modifier.height(12.dp))

            // ---------- Action catalog ----------
            Text(
                "ADD AN ACTION STEP",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                ActionCatalog.groups.forEach { group ->
                    item {
                        Text(
                            group.name,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            group.entries.forEach { entry ->
                                androidx.compose.material3.AssistChip(
                                    onClick = {
                                        if (steps.size < CustomButton.STEPS_MAX) {
                                            steps.add("a:" + entry.id)
                                        }
                                    },
                                    label = { Text(entry.label) }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // ---------- Save ----------
            Button(
                onClick = { onSave(label.ifBlank { "Button" }, steps.toList()) },
                enabled = steps.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text("Save button") }
            Spacer(Modifier.height(28.dp))
        }
    }
}
