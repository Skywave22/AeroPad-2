package com.bluepilot.remote.features

import com.bluepilot.remote.model.custom.ActionCatalog
import com.bluepilot.remote.model.custom.CustomButton
import com.bluepilot.remote.model.custom.CustomRemoteCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** BLEK-PRO PACK v2 — codec + catalog invariants. */
class CustomRemoteCodecTest {

    private fun btn(id: String, label: String = "B", steps: List<String> = listOf("a:copy")) =
        CustomButton(id = id, label = label, steps = steps)

    @Test
    fun `roundtrip preserves buttons`() {
        val list = listOf(
            btn("1", "Login", listOf("t:user", "a:tab", "t:pass", "a:enter")),
            btn("2", "Zoom", listOf("a:fullscreen"))
        )
        assertEquals(list, CustomRemoteCodec.decode(CustomRemoteCodec.encode(list)))
    }

    @Test
    fun `decode of garbage or blank is empty`() {
        assertEquals(emptyList<CustomButton>(), CustomRemoteCodec.decode(null))
        assertEquals(emptyList<CustomButton>(), CustomRemoteCodec.decode(""))
        assertEquals(emptyList<CustomButton>(), CustomRemoteCodec.decode("{not json"))
    }

    @Test
    fun `unknown action steps are dropped and empty buttons removed`() {
        val raw = CustomRemoteCodec.encode(
            listOf(CustomButton("x", "Bad", listOf("a:does_not_exist")))
        )
        assertEquals(emptyList<CustomButton>(), CustomRemoteCodec.decode(raw))
    }

    @Test
    fun `add caps at max and replaces same id`() {
        var list = emptyList<CustomButton>()
        repeat(CustomRemoteCodec.MAX_BUTTONS + 5) { i ->
            list = CustomRemoteCodec.add(list, btn("id$i"))
        }
        assertEquals(CustomRemoteCodec.MAX_BUTTONS, list.size)

        val replaced = CustomRemoteCodec.add(list, btn(list.first().id, "New label"))
        assertEquals(list.size, replaced.size)
        assertEquals("New label", replaced.last().label)
    }

    @Test
    fun `remove drops by id`() {
        val list = listOf(btn("a"), btn("b"))
        assertEquals(listOf(btn("a")), CustomRemoteCodec.remove(list, "b"))
    }

    @Test
    fun `sanitize truncates label and step count`() {
        val b = CustomButton(
            id = "s",
            label = "X".repeat(99),
            steps = List(99) { "a:copy" }
        ).sanitized()
        assertEquals(CustomButton.LABEL_MAX, b.label.length)
        assertEquals(CustomButton.STEPS_MAX, b.steps.size)
    }

    @Test
    fun `catalog ids are unique and resolvable`() {
        val ids = ActionCatalog.groups.flatMap { g -> g.entries.map { it.id } }
        assertEquals(ids.size, ids.toSet().size)
        ids.forEach { assertNotNull(ActionCatalog.byId(it)) }
    }

    @Test
    fun `step labels are human readable`() {
        assertTrue(ActionCatalog.stepLabel("a:copy").contains("Copy"))
        assertTrue(ActionCatalog.stepLabel("t:hello").contains("hello"))
        assertEquals("?", ActionCatalog.stepLabel("junk"))
    }
}
