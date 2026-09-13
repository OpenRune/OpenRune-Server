package org.rsmod.content.bosses.zulrah

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.content.generic.killcount.BossRecords
import org.rsmod.game.entity.Player

class BossRecordsTest {
    @Test
    fun `personal best compares ticks and survives normal attribute serialization`() {
        val player = Player()
        assertEquals(0, BossRecords.bestTicks(player, 1518))
        assertTrue(BossRecords.recordBest(player, 1518, 100))
        assertFalse(BossRecords.recordBest(player, 1518, 101))
        assertFalse(BossRecords.recordBest(player, 1518, 100))
        assertTrue(BossRecords.recordBest(player, 1518, 99))
        val mapper = jacksonObjectMapper()
        val saved = mapper.writeValueAsString(player.attr.toPersistentMap())
        val restored = Player()
        restored.attr.putAllFromPersistence(mapper.readValue<Map<String, Any>>(saved))
        assertEquals(99, BossRecords.bestTicks(restored, 1518))
        assertEquals(0, BossRecords.bestTicks(restored, 1519))
    }

    @Test
    fun `time formatter matches native tick rounding precision and hour options`() {
        assertEquals("0:07", BossRecords.formatTime(12, false, false))
        assertEquals("0:06", BossRecords.formatTime(10, false, false))
        assertEquals("0:01", BossRecords.formatTime(1, false, false))
        assertEquals("2:55", BossRecords.formatTime(291, false, false))
        assertEquals("0:07.20", BossRecords.formatTime(12, true, false))
        assertEquals("1:00", BossRecords.formatTime(100, false, false))
        assertEquals("1:00:00.60", BossRecords.formatTime(6001, true, false))
        assertEquals("60:00.60", BossRecords.formatTime(6001, true, true))
    }
}
