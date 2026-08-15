package com.bluepilot.remote.model.custom

import com.bluepilot.remote.model.HidAction
import com.bluepilot.remote.model.HidConsumer
import com.bluepilot.remote.model.HidKeys
import com.bluepilot.remote.model.HidSystem
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

// =====================================================================
// BLEK-PRO PACK v2 — Custom Remote (user-built layouts + macros).
//
// A custom button is a LABEL plus a list of STEPS executed in order.
// Each step is a compact string:
//   "a:<catalogId>"  → one action from the curated catalog below
//   "t:<text>"       → type literal text on the PC
// Multi-step buttons ARE macros: "t:username", "a:tab", "t:password",
// "a:enter" logs into something with one tap.
// =====================================================================

@Serializable
data class CustomButton(
    val id: String,
    val label: String,
    val steps: List<String>
) {
    companion object {
        const val LABEL_MAX = 16
        const val STEPS_MAX = 12
    }

    fun sanitized(): CustomButton = copy(
        label = label.take(LABEL_MAX).ifBlank { "Button" },
        steps = steps.take(STEPS_MAX).filter {
            it.startsWith("t:") || (it.startsWith("a:") && ActionCatalog.byId(it.drop(2)) != null)
        }
    )
}

/** Pure list logic + JSON codec (unit-tested, same pattern as HostProfileCodec). */
object CustomRemoteCodec {

    const val MAX_BUTTONS = 24

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(list: List<CustomButton>): String =
        json.encodeToString(ListSerializer(CustomButton.serializer()), list)

    fun decode(raw: String?): List<CustomButton> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(CustomButton.serializer()), raw)
                .map { it.sanitized() }
                .filter { it.steps.isNotEmpty() }
        }.getOrDefault(emptyList())
    }

    fun add(list: List<CustomButton>, button: CustomButton): List<CustomButton> {
        val clean = button.sanitized()
        if (clean.steps.isEmpty()) return list
        return (list.filterNot { it.id == clean.id } + clean).take(MAX_BUTTONS)
    }

    fun remove(list: List<CustomButton>, id: String): List<CustomButton> =
        list.filterNot { it.id == id }

    // ---- BLEK-PRO v3: share layouts as text codes ----
    // Format: "AEROPAD1:" + Base64(JSON). Versioned prefix so future
    // formats stay importable; Base64 survives every messenger intact
    // (raw JSON gets mangled by smart quotes in chat apps).
    private const val SHARE_PREFIX = "AEROPAD1:"

    fun exportShare(list: List<CustomButton>): String =
        SHARE_PREFIX + java.util.Base64.getEncoder()
            .encodeToString(encode(list).toByteArray(Charsets.UTF_8))

    /** Decodes a share code; returns null (never throws) on any garbage.
     *  Imported buttons get FRESH ids so they merge instead of overwrite. */
    fun importShare(code: String): List<CustomButton>? {
        val body = code.trim().removePrefix(SHARE_PREFIX)
        if (body == code.trim()) return null // prefix missing
        return runCatching {
            val json = String(java.util.Base64.getDecoder().decode(body), Charsets.UTF_8)
            decode(json).map { it.copy(id = java.util.UUID.randomUUID().toString()) }
        }.getOrNull()?.takeIf { it.isNotEmpty() }
    }
}

// =====================================================================
// Curated action catalog — every PC action a custom button can fire.
// Grouped for the picker UI. IDs are stable (persisted in user layouts):
// never rename an id, only add.
// =====================================================================

data class CatalogEntry(val id: String, val label: String, val action: HidAction)
data class CatalogGroup(val name: String, val entries: List<CatalogEntry>)

object ActionCatalog {

    private const val CTRL = 0x01
    private const val SHIFT = 0x02
    private const val ALT = 0x04
    private const val GUI = 0x08

    private fun key(id: String, label: String, key: Byte, mods: Int = 0) =
        CatalogEntry(id, label, HidAction.KeyTap(key, mods.toByte()))

    private fun media(id: String, label: String, usage: Int) =
        CatalogEntry(id, label, HidAction.MediaTap(usage))

    val groups: List<CatalogGroup> = listOf(
        CatalogGroup(
            "Shortcuts", listOf(
                key("copy", "Copy (Ctrl+C)", HidKeys.C, CTRL),
                key("paste", "Paste (Ctrl+V)", HidKeys.V, CTRL),
                key("cut", "Cut (Ctrl+X)", HidKeys.X, CTRL),
                key("undo", "Undo (Ctrl+Z)", HidKeys.Z, CTRL),
                key("redo", "Redo (Ctrl+Y)", HidKeys.Y, CTRL),
                key("select_all", "Select all (Ctrl+A)", HidKeys.A, CTRL),
                key("save", "Save (Ctrl+S)", HidKeys.S, CTRL),
                key("find", "Find (Ctrl+F)", HidKeys.F, CTRL),
                key("alt_tab", "Switch app (Alt+Tab)", HidKeys.TAB, ALT),
                key("win_d", "Show desktop (Win+D)", HidKeys.D, GUI),
                key("win_l", "Lock PC (Win+L)", HidKeys.L, GUI),
                key("win_e", "File Explorer (Win+E)", HidKeys.E, GUI),
                key("win_r", "Run (Win+R)", HidKeys.R, GUI),
                key("screenshot", "Screenshot (Win+Shift+S)", HidKeys.S, GUI or SHIFT),
                key("task_mgr", "Task Manager (Ctrl+Shift+Esc)", HidKeys.ESCAPE, CTRL or SHIFT),
                key("close_win", "Close window (Alt+F4)", HidKeys.F4, ALT),
                key("new_tab", "New tab (Ctrl+T)", HidKeys.T, CTRL),
                key("close_tab", "Close tab (Ctrl+W)", HidKeys.W, CTRL),
                key("reopen_tab", "Reopen tab (Ctrl+Shift+T)", HidKeys.T, CTRL or SHIFT),
                key("next_tab", "Next tab (Ctrl+Tab)", HidKeys.TAB, CTRL),
                key("refresh", "Refresh (F5)", HidKeys.F5),
                key("fullscreen", "Fullscreen (F11)", HidKeys.F11)
            )
        ),
        CatalogGroup(
            "Keys", listOf(
                key("enter", "Enter", HidKeys.ENTER),
                key("tab", "Tab", HidKeys.TAB),
                key("esc", "Escape", HidKeys.ESCAPE),
                key("space", "Space", HidKeys.SPACE),
                key("backspace", "Backspace", HidKeys.BACKSPACE),
                key("delete", "Delete", HidKeys.DELETE),
                key("up", "Arrow ↑", HidKeys.ARROW_UP),
                key("down", "Arrow ↓", HidKeys.ARROW_DOWN),
                key("left", "Arrow ←", HidKeys.ARROW_LEFT),
                key("right", "Arrow →", HidKeys.ARROW_RIGHT),
                key("home", "Home", HidKeys.HOME),
                key("end", "End", HidKeys.END),
                key("page_up", "Page Up", HidKeys.PAGE_UP),
                key("page_down", "Page Down", HidKeys.PAGE_DOWN),
                key("print_screen", "Print Screen", HidKeys.PRINT_SCREEN)
            )
        ),
        CatalogGroup(
            "Media & system", listOf(
                media("play_pause", "Play / Pause", HidConsumer.PLAY_PAUSE),
                media("next_track", "Next track", HidConsumer.NEXT_TRACK),
                media("prev_track", "Previous track", HidConsumer.PREV_TRACK),
                media("vol_up", "Volume up", HidConsumer.VOLUME_UP),
                media("vol_down", "Volume down", HidConsumer.VOLUME_DOWN),
                media("mute", "Mute", HidConsumer.MUTE),
                CatalogEntry("pc_sleep", "PC sleep", HidAction.SystemTap(HidSystem.SLEEP))
            )
        )
    )

    private val index: Map<String, CatalogEntry> =
        groups.flatMap { it.entries }.associateBy { it.id }

    fun byId(id: String): CatalogEntry? = index[id]

    /** Human label for a step string (for chips in the editor). */
    fun stepLabel(step: String): String = when {
        step.startsWith("t:") -> "\u201C" + step.drop(2).take(14) + if (step.length > 16) "…\u201D" else "\u201D"
        step.startsWith("a:") -> byId(step.drop(2))?.label ?: "?"
        else -> "?"
    }
}
