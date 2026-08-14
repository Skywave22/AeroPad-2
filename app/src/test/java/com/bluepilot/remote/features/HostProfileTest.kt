package com.bluepilot.remote.features

import com.bluepilot.remote.data.hosts.HostProfile
import com.bluepilot.remote.data.hosts.HostProfileCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Host profile codec: upsert, cap, encode/decode round trip. */
class HostProfileTest {

    private fun p(addr: String) =
        HostProfile(
            id = addr, label = "L-$addr", address = addr,
            lastUsedAt = 1L
        )

    @Test
    fun `upsert prepends new and dedupes by address`() {
        var list = emptyList<HostProfile>()
        list = HostProfileCodec.upsert(list, p("AA"))
        list = HostProfileCodec.upsert(list, p("BB"))
        assertEquals(listOf("BB", "AA"), list.map { it.address })
        // same address again → moves to front, no duplicate
        list = HostProfileCodec.upsert(list, p("AA"))
        assertEquals(listOf("AA", "BB"), list.map { it.address })
        assertEquals(2, list.size)
    }

    @Test
    fun `list capped at max profiles`() {
        var list = emptyList<HostProfile>()
        repeat(HostProfileCodec.MAX_PROFILES + 5) { i ->
            list = HostProfileCodec.upsert(list, p("ADDR-$i"))
        }
        assertEquals(HostProfileCodec.MAX_PROFILES, list.size)
    }

    @Test
    fun `encode decode round trips`() {
        val list = listOf(p("AA"), p("BB"))
        val decoded = HostProfileCodec.decode(HostProfileCodec.encode(list))
        assertEquals(list.map { it.address }, decoded.map { it.address })
    }

    @Test
    fun `decode of garbage is empty not crash`() {
        assertTrue(HostProfileCodec.decode("{not json]").isEmpty())
        assertTrue(HostProfileCodec.decode(null).isEmpty())
        assertTrue(HostProfileCodec.decode("").isEmpty())
    }

    @Test
    fun `remove drops by id`() {
        val list = listOf(p("AA"), p("BB"))
        val out = HostProfileCodec.remove(list, "AA")
        assertEquals(listOf("BB"), out.map { it.address })
    }

    @Test
    fun `sanitized blank label falls back to address`() {
        val prof = HostProfile(id = "x", label = "  ", address = "AA:BB").sanitized()
        assertEquals("AA:BB", prof.label)
    }
}
