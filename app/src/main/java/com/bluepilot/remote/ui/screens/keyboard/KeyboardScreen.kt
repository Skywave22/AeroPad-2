package com.bluepilot.remote.ui.screens.keyboard

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bluepilot.remote.model.HidKeys
import com.bluepilot.remote.model.HidModifiers
import com.bluepilot.remote.ui.components.KeyCard
import com.bluepilot.remote.ui.components.NotConnectedBanner
import com.bluepilot.remote.ui.components.rememberHaptic
import com.bluepilot.remote.viewmodel.RemoteControlViewModel

/**
 * PC Keyboard screen:
 *  - text input bar (types whole strings on the PC)
 *  - shortcut row (copy/paste/cut/select-all/save/undo/redo)
 *  - F1..F12, navigation cluster, arrows, modifier combos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardScreen(
    onBack: () -> Unit,
    // UI/UX v2.1 — Full Board + PC Combo are now OPTIONS of the Keyboard
    // hub (top-bar chips) instead of separate home tiles.
    onOpenFullBoard: () -> Unit = {},
    viewModel: RemoteControlViewModel = hiltViewModel()
) {
    val isConnected by viewModel.isConnected.collectAsState()
    val keyboardSettings by viewModel.keyboardSettings.collectAsState()
    val vibration by viewModel.vibrationsEnabled.collectAsState()
    val haptic = rememberHaptic(vibration)
    var text by remember { mutableStateOf("") }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Keyboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // UI/UX v2.1 — keyboard modes as options, not home tiles.
                    androidx.compose.material3.AssistChip(
                        onClick = onOpenFullBoard,
                        label = { Text("Full board") }
                    )
                    Spacer(Modifier.width(6.dp))
                }
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

            // ---------- Text input bar ----------
            if (keyboardSettings.showTextInputBar) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type here, send to PC…") },
                        singleLine = true
                    )
                    run {
                        val settingsVm: com.bluepilot.remote.viewmodel.SettingsViewModel =
                            androidx.hilt.navigation.compose.hiltViewModel()
                        Box(
                            modifier = Modifier
                                .pointerInput(text) {
                                    detectTapGestures(
                                        onTap = {
                                            haptic()
                                            viewModel.typeText(text)
                                            text = ""
                                        },
                                        onLongPress = {
                                            // FEATURE: long-press send = save as snippet
                                            if (text.isNotBlank()) {
                                                haptic()
                                                settingsVm.addSnippet(text)
                                            }
                                        }
                                    )
                                }
                                .padding(12.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "Send text (long-press to save snippet)",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                // AEROPAD v1.0 #12 — sent-text history (tap to resend).
                run {
                    val history by viewModel.sentHistory.collectAsState()
                    if (history.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            history.forEach { item ->
                                androidx.compose.material3.AssistChip(
                                    onClick = { haptic(); viewModel.typeText(item) },
                                    label = {
                                        Text(if (item.length > 18) item.take(18) + "…" else item)
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // AEROPAD v1.0 #16 — toggleable numpad overlay.
            run {
                var showNumpad by remember { mutableStateOf(false) }
                androidx.compose.material3.FilterChip(
                    selected = showNumpad,
                    onClick = { showNumpad = !showNumpad },
                    label = { Text(if (showNumpad) "Hide numpad" else "Numpad") }
                )
                if (showNumpad) {
                    Spacer(Modifier.height(6.dp))
                    listOf(
                        listOf("7", "8", "9"),
                        listOf("4", "5", "6"),
                        listOf("1", "2", "3"),
                        listOf("0", ".", "⏎")
                    ).forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowKeys.forEach { k ->
                                KeyCard(
                                    label = k,
                                    modifier = Modifier.weight(1f),
                                    height = 44.dp
                                ) {
                                    haptic()
                                    when (k) {
                                        "⏎" -> viewModel.keyTap(HidKeys.ENTER, 0)
                                        "." -> viewModel.typeText(".")
                                        else -> viewModel.typeText(k)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ---------- FEATURE: Quick snippets ----------
            SectionLabel("Snippets")
            run {
                val settingsVm: com.bluepilot.remote.viewmodel.SettingsViewModel =
                    androidx.hilt.navigation.compose.hiltViewModel()
                val app by settingsVm.app.collectAsState()
                val snippets = remember(app.quickSnippets) {
                    app.quickSnippets.split('\u001F').filter { it.isNotEmpty() }
                }
                if (snippets.isEmpty()) {
                    Text(
                        "Save emails, passwords hints, or any phrase you type often — long-press the send button to save the current text.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        snippets.forEach { snip ->
                            androidx.compose.material3.InputChip(
                                selected = false,
                                onClick = { haptic(); viewModel.typeText(snip) },
                                label = { Text(snip.take(24) + if (snip.length > 24) "…" else "") },
                                trailingIcon = {
                                    Text(
                                        "✕",
                                        modifier = Modifier
                                            .padding(start = 2.dp)
                                            .clickable { settingsVm.removeSnippet(snip) },
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))

            // ---------- FEATURE: Modifier lock (sticky keys) ----------
            SectionLabel("Modifier lock")
            run {
                val locked by viewModel.lockedModifiers.collectAsState()
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "Ctrl" to HidModifiers.LEFT_CTRL,
                        "Shift" to HidModifiers.LEFT_SHIFT,
                        "Alt" to HidModifiers.LEFT_ALT,
                        "Win" to HidModifiers.LEFT_GUI
                    ).forEach { (label, mod) ->
                        androidx.compose.material3.FilterChip(
                            selected = (locked.toInt() and mod.toInt()) != 0,
                            onClick = { haptic(); viewModel.toggleModifier(mod) },
                            label = { Text(label) }
                        )
                    }
                }
                if (locked != 0.toByte()) {
                    Text(
                        "Armed — the next key you tap fires with these modifiers, then they clear.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            // ---------- Shortcuts ----------
            SectionLabel("Shortcuts")
            val ctrl = HidModifiers.LEFT_CTRL
            KeyGrid(
                keys = listOf(
                    "Copy" to { viewModel.keyTap(HidKeys.C, ctrl) },
                    "Paste" to { viewModel.keyTap(HidKeys.V, ctrl) },
                    "Cut" to { viewModel.keyTap(HidKeys.X, ctrl) },
                    "All" to { viewModel.keyTap(HidKeys.A, ctrl) },
                    "Save" to { viewModel.keyTap(HidKeys.S, ctrl) },
                    "Undo" to { viewModel.keyTap(HidKeys.Z, ctrl) },
                    "Redo" to { viewModel.keyTap(HidKeys.Y, ctrl) },
                    "Del" to { viewModel.keyTap(HidKeys.DELETE) }
                ),
                columns = 4,
                haptic = haptic
            )

            // ---------- Main control keys ----------
            SectionLabel("Keys")
            KeyGrid(
                keys = listOf(
                    "Esc" to { viewModel.keyTap(HidKeys.ESCAPE) },
                    "Tab" to { viewModel.keyTap(HidKeys.TAB) },
                    "Space" to { viewModel.keyTap(HidKeys.SPACE) },
                    "Enter" to { viewModel.keyTap(HidKeys.ENTER) },
                    "Backspc" to { viewModel.keyTap(HidKeys.BACKSPACE) },
                    "Win" to { viewModel.keyTap(HidKeys.NONE, HidModifiers.LEFT_GUI) },
                    "Menu" to { viewModel.keyTap(HidKeys.APPLICATION) },
                    "PrtScr" to { viewModel.keyTap(HidKeys.PRINT_SCREEN) }
                ),
                columns = 4,
                haptic = haptic
            )

            // ---------- Arrows + navigation ----------
            SectionLabel("Navigation")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Spacer(Modifier.weight(1f))
                        RepeatKeyCard("▲", Modifier.weight(1f), haptic, viewModel, HidKeys.ARROW_UP)
                        Spacer(Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RepeatKeyCard("◀", Modifier.weight(1f), haptic, viewModel, HidKeys.ARROW_LEFT)
                        RepeatKeyCard("▼", Modifier.weight(1f), haptic, viewModel, HidKeys.ARROW_DOWN)
                        RepeatKeyCard("▶", Modifier.weight(1f), haptic, viewModel, HidKeys.ARROW_RIGHT)
                    }
                }
                Column(modifier = Modifier.weight(0.8f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KeyCard("Home", modifier = Modifier.weight(1f)) { haptic(); viewModel.keyTap(HidKeys.HOME) }
                        KeyCard("End", modifier = Modifier.weight(1f)) { haptic(); viewModel.keyTap(HidKeys.END) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KeyCard("PgUp", modifier = Modifier.weight(1f)) { haptic(); viewModel.keyTap(HidKeys.PAGE_UP) }
                        KeyCard("PgDn", modifier = Modifier.weight(1f)) { haptic(); viewModel.keyTap(HidKeys.PAGE_DOWN) }
                    }
                }
            }

            // ---------- Function keys ----------
            SectionLabel("Function keys")
            val fKeys = listOf(
                HidKeys.F1, HidKeys.F2, HidKeys.F3, HidKeys.F4, HidKeys.F5, HidKeys.F6,
                HidKeys.F7, HidKeys.F8, HidKeys.F9, HidKeys.F10, HidKeys.F11, HidKeys.F12
            )
            KeyGrid(
                keys = fKeys.mapIndexed { i, key -> "F${i + 1}" to { viewModel.keyTap(key) } },
                columns = 6,
                haptic = haptic
            )

            // ---------- Combos ----------
            SectionLabel("Combos")
            KeyGrid(
                keys = listOf(
                    "Alt+Tab" to { viewModel.keyTap(HidKeys.TAB, HidModifiers.LEFT_ALT) },
                    "Alt+F4" to { viewModel.keyTap(HidKeys.F4, HidModifiers.LEFT_ALT) },
                    "Win+D" to { viewModel.keyTap(HidKeys.D, HidModifiers.LEFT_GUI) },
                    "Win+L" to { viewModel.keyTap(HidKeys.L, HidModifiers.LEFT_GUI) },
                    "Ctrl+Esc" to { viewModel.keyTap(HidKeys.ESCAPE, HidModifiers.LEFT_CTRL) },
                    "Ctl+Sh+Esc" to {
                        viewModel.keyTap(
                            HidKeys.ESCAPE,
                            (HidModifiers.LEFT_CTRL.toInt() or HidModifiers.LEFT_SHIFT.toInt()).toByte()
                        )
                    },
                    // FEATURE: extended combo pack
                    "Win+Tab" to { viewModel.keyTap(HidKeys.TAB, HidModifiers.LEFT_GUI) },
                    "Win+E" to { viewModel.keyTap(HidKeys.E, HidModifiers.LEFT_GUI) },
                    "Win+Shot" to {
                        // Win+Shift+S — Windows snip tool
                        viewModel.keyTap(
                            HidKeys.S,
                            (HidModifiers.LEFT_GUI.toInt() or HidModifiers.LEFT_SHIFT.toInt()).toByte()
                        )
                    },
                    "New Tab" to { viewModel.keyTap(HidKeys.T, HidModifiers.LEFT_CTRL) },
                    "Close Tab" to { viewModel.keyTap(HidKeys.W, HidModifiers.LEFT_CTRL) },
                    "Reopen Tab" to {
                        viewModel.keyTap(
                            HidKeys.T,
                            (HidModifiers.LEFT_CTRL.toInt() or HidModifiers.LEFT_SHIFT.toInt()).toByte()
                        )
                    },
                    "Find" to { viewModel.keyTap(HidKeys.F, HidModifiers.LEFT_CTRL) },
                    "Refresh" to { viewModel.keyTap(HidKeys.F5) }
                ),
                columns = 3,
                haptic = haptic
            )

            Spacer(Modifier.height(24.dp))
        }
    }

}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

/** Simple fixed-column grid of KeyCards. */
@Composable
private fun KeyGrid(
    keys: List<Pair<String, () -> Unit>>,
    columns: Int,
    haptic: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.chunked(columns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (label, action) ->
                    KeyCard(label, modifier = Modifier.weight(1f)) { haptic(); action() }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}


/**
 * FEATURE: hold-to-repeat key — behaves like a physical keyboard key:
 * tap = one keystroke, press-and-hold = repeat until release.
 */
@Composable
private fun RepeatKeyCard(
    label: String,
    modifier: Modifier,
    haptic: () -> Unit,
    viewModel: RemoteControlViewModel,
    key: Byte,
    modifiers: Byte = 0
) {
    androidx.compose.material3.Card(
        modifier = modifier
            .heightIn(min = 48.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        haptic()
                        viewModel.keyRepeatStart(key, modifiers)
                        tryAwaitRelease()
                        viewModel.keyRepeatStop()
                    }
                )
            },
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
