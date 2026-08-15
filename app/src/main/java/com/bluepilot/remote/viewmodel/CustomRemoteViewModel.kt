package com.bluepilot.remote.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluepilot.remote.data.custom.CustomRemote
import com.bluepilot.remote.domain.usecase.ObserveConnectionUseCase
import com.bluepilot.remote.domain.usecase.SendHidActionUseCase
import com.bluepilot.remote.model.HidAction
import com.bluepilot.remote.model.custom.ActionCatalog
import com.bluepilot.remote.model.custom.CustomButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BLEK-PRO PACK v2 — Custom Remote driver.
 *
 * Executes a button's steps SEQUENTIALLY with a small inter-step delay so
 * multi-step macros (type → tab → type → enter) land reliably: the HID
 * link processes reports in order, but the receiving OS needs a beat
 * between "text finished" and the next keystroke (fast Enter after text
 * can outrun a slow app's input handler).
 */
@HiltViewModel
class CustomRemoteViewModel @Inject constructor(
    observeConnection: ObserveConnectionUseCase,
    private val sendAction: SendHidActionUseCase,
    private val store: CustomRemote
) : ViewModel() {

    val isConnected: StateFlow<Boolean> = observeConnection()
        .map { it.isConnected }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val buttons: StateFlow<List<CustomButton>> = store.buttons

    private var runJob: kotlinx.coroutines.Job? = null

    /** Fire a button: run all its steps in order. Re-tap cancels + restarts. */
    fun run(button: CustomButton) {
        runJob?.cancel()
        runJob = viewModelScope.launch {
            button.steps.forEach { step ->
                when {
                    step.startsWith("t:") -> {
                        val text = step.drop(2)
                        if (text.isNotEmpty()) sendAction(HidAction.TypeText(text))
                        // TypeText is queued per char on the engine side;
                        // give it time to drain before the next step.
                        delay(40L * text.length.coerceAtMost(25) + 60L)
                    }
                    step.startsWith("a:") -> {
                        ActionCatalog.byId(step.drop(2))?.let { sendAction(it.action) }
                        delay(90L)
                    }
                }
            }
        }
    }

    fun add(label: String, steps: List<String>) {
        if (steps.isEmpty()) return
        store.add(
            CustomButton(
                id = java.util.UUID.randomUUID().toString(),
                label = label,
                steps = steps
            )
        )
    }

    fun remove(id: String) = store.remove(id)

    // ---- BLEK-PRO v3: layout sharing ----
    fun exportCode(): String =
        com.bluepilot.remote.model.custom.CustomRemoteCodec.exportShare(buttons.value)

    /** Imports a share code; returns how many buttons were added (0 = bad code). */
    fun importCode(code: String): Int {
        val imported = com.bluepilot.remote.model.custom.CustomRemoteCodec.importShare(code)
            ?: return 0
        imported.forEach { store.add(it) }
        return imported.size
    }
}
