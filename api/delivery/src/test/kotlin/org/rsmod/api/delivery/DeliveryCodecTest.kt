package org.rsmod.api.delivery

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.attr.AttributeMap

class DeliveryCodecTest {
    @Test
    fun `frozen physical deliveries retain remaining travel across offline time and persistence`() {
        val state = DeliveryState()
        val frozen = state.line(LINE)
        frozen.pausedAtSave = true
        frozen.pending += PendingDelivery(payload = 453, amount = 3, dueCycle = 1013)
        val active = state.line("active")
        active.pending += PendingDelivery(payload = 444, amount = 1, dueCycle = 1013)
        val document = throughRealPersistencePath(DeliveryCodec.encode(state, 1000, NOW))
        val decoded = DeliveryCodec.decode(document, 42, NOW + 3_600_000L)
        assertEquals(55, decoded.lines.getValue(LINE).pending.single().dueCycle)
        assertEquals(42, decoded.lines.getValue("active").pending.single().dueCycle)
        assertEquals(3, decoded.lines.getValue(LINE).pending.single().amount)

        val resaved = throughRealPersistencePath(DeliveryCodec.encode(decoded, 42, NOW + 3_600_000L))
        val reloaded = DeliveryCodec.decode(resaved, 100, NOW + 7_200_000L)
        assertEquals(113, reloaded.lines.getValue(LINE).pending.single().dueCycle)
    }

    @Test
    fun `round trips parcels and store through the real persistence path`() {
        val state = DeliveryState()
        val line = state.line(LINE)
        line.pending += PendingDelivery(payload = 453, amount = 1, dueCycle = 1013)
        line.pending += PendingDelivery(payload = 444, amount = 2, dueCycle = 1020)
        line.stored[453] = 12
        line.stored[444] = 3
        line.recomputeNextDue()
        state.recomputeNextDue()

        val document = DeliveryCodec.encode(state, cycle = 1000, nowMillis = NOW)
        val restored = throughRealPersistencePath(document)

        val decoded = DeliveryCodec.decode(restored, cycle = 7, nowMillis = NOW + 3_000L)
        val decodedLine = assertNotNull(decoded.lines[LINE])

        assertEquals(2, decodedLine.pending.size)
        assertEquals(453, decodedLine.pending[0].payload)
        assertEquals(1, decodedLine.pending[0].amount)
        assertEquals(7 + 8, decodedLine.pending[0].dueCycle)
        assertEquals(444, decodedLine.pending[1].payload)
        assertEquals(2, decodedLine.pending[1].amount)
        assertEquals(7 + 15, decodedLine.pending[1].dueCycle)
        assertEquals(7 + 8, decodedLine.nextDueCycle)
        assertEquals(7 + 8, decoded.nextDueCycle)

        assertEquals(mapOf(453 to 12, 444 to 3), decodedLine.stored)
        assertEquals(15, decodedLine.storedTotal())
        assertEquals(3, decodedLine.pendingTotal())
    }

    @Test
    fun `parcels that came due while offline are due immediately`() {
        val state = DeliveryState()
        val line = state.line(LINE)
        line.pending += PendingDelivery(payload = 453, amount = 1, dueCycle = 1013)
        line.recomputeNextDue()

        val document = DeliveryCodec.encode(state, cycle = 1000, nowMillis = NOW)
        val restored = throughRealPersistencePath(document)

        val decoded = DeliveryCodec.decode(restored, cycle = 42, nowMillis = NOW + 3_600_000L)
        val decodedLine = assertNotNull(decoded.lines[LINE])

        // `dueCycle == cycle` means the very next `LateCycle` drain delivers it: login runs in the
        // pre-tick phase of the same loop.
        assertEquals(42, decodedLine.pending.single().dueCycle)
        assertEquals(42, decoded.nextDueCycle)
    }

    @Test
    fun `an absurd future instant is clamped rather than dropped or overflowed`() {
        val remaining = DeliveryCodec.remainingTicks(dueMillis = Long.MAX_VALUE, nowMillis = NOW)
        assertEquals(DeliveryCodec.MAX_REMAINING_TICKS, remaining)
        assertTrue(Int.MAX_VALUE - remaining > 0)
    }

    @Test
    fun `malformed rows are skipped without taking the rest of the document with them`() {
        val document =
            mapOf(
                LINE to
                    mapOf(
                        DeliveryCodec.PENDING to
                            listOf(
                                "not a parcel",
                                mapOf(DeliveryCodec.PAYLOAD to 453),
                                mapOf(
                                    DeliveryCodec.PAYLOAD to 444,
                                    DeliveryCodec.AMOUNT to 0,
                                    DeliveryCodec.DUE to NOW,
                                ),
                                mapOf(
                                    DeliveryCodec.PAYLOAD to 440,
                                    DeliveryCodec.AMOUNT to 5,
                                    DeliveryCodec.DUE to NOW + 6_000L,
                                ),
                            ),
                        DeliveryCodec.STORED to mapOf("453" to 4, "bogus" to 9, "444" to "nine"),
                    ),
                "line with a non-map body" to 7,
            )

        val decoded = DeliveryCodec.decode(document, cycle = 0, nowMillis = NOW)

        assertEquals(setOf(LINE), decoded.lines.keys)
        val decodedLine = assertNotNull(decoded.lines[LINE])
        assertEquals(440, decodedLine.pending.single().payload)
        assertEquals(10, decodedLine.pending.single().dueCycle)
        assertEquals(mapOf(453 to 4), decodedLine.stored)
    }

    @Test
    fun `an empty line produces no document row`() {
        val state = DeliveryState()
        state.line(LINE)
        assertTrue(DeliveryCodec.encode(state, cycle = 0, nowMillis = NOW).isEmpty())
        assertTrue(state.isEmpty)
    }

    @Test
    fun `an unregistered line's document survives a decode and re-encode untouched`() {
        val state = DeliveryState()
        val line = state.line("some_uninstalled_module")
        line.pending += PendingDelivery(payload = 453, amount = 1, dueCycle = 13)
        line.stored[444] = 6
        line.recomputeNextDue()

        val first = throughRealPersistencePath(DeliveryCodec.encode(state, 0, NOW))
        val decoded = DeliveryCodec.decode(first, cycle = 0, nowMillis = NOW)
        val second = DeliveryCodec.encode(decoded, cycle = 0, nowMillis = NOW)

        assertEquals(first, second)
    }

    /**
     * Serialises [document] with Jackson, reads it back untyped, and runs it through
     * [AttributeMap]'s persistence normalisation - i.e. everything the real save and load do to it
     * between `player.attr` and the `character_attrs` row.
     */
    private fun throughRealPersistencePath(document: Map<String, Any>): Map<*, *> {
        val mapper = ObjectMapper()
        val json = mapper.writeValueAsString(document)
        val raw = mapper.readValue(json, object : TypeReference<Any>() {})
        val attributes = AttributeMap()
        attributes.putAllFromPersistence(mapOf(KEY to raw))
        val restored = assertNotNull(attributes.toPersistentMap()[KEY])
        return assertNotNull(restored as? Map<*, *>, "Document must normalise back to a map.")
    }

    private companion object {
        private const val LINE = "test_line"
        private const val KEY = DeliveryAttributes.DOCUMENT_PERSISTENCE_KEY
        private const val NOW = 1_700_000_000_000L
    }
}
