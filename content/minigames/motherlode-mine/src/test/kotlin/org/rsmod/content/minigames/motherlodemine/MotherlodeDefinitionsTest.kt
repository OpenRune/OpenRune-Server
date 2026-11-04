package org.rsmod.content.minigames.motherlodemine

import dev.openrune.ServerCacheManager
import dev.openrune.filesystem.Cache
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.varp.VarpLifetime
import dev.openrune.types.varp.VarpTransmitLevel
import java.nio.file.Paths
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants.isMotherlodeUpperFloor
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants.motherlodeInRestrictedArea
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeData
import org.rsmod.content.minigames.motherlodemine.configs.migrateMotherlodeUnlocks
import org.rsmod.content.minigames.motherlodemine.configs.motherlodeLastSackNotify
import org.rsmod.content.minigames.motherlodemine.configs.motherlodeSuperHopperUnlocked
import org.rsmod.content.minigames.motherlodemine.configs.motherlodeUpperFloorUnlocked
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

@ResourceLock("ServerCacheManager")
class MotherlodeDefinitionsTest {
    @Test fun `server only vein and rockfall overrides retain native client definitions`() {
        val symbols = MotherlodeConstants.ORE_VEINS + MotherlodeConstants.ROCKFALLS
        for (cacheName in listOf("LIVE", "SERVER")) {
            val cache = Cache.load(Paths.get(".data/cache", cacheName))
            try {
                for (symbol in symbols) {
                    val id = symbol.asRSCM()
                    assertNotNull(cache.data(2, 6, id), "$cacheName native $symbol")
                    val loc = checkNotNull(ServerCacheManager.getObject(id))
                    assertTrue(loc.name.isNotBlank(), "$symbol lost its inherited name")
                    assertTrue(loc.contentGroup >= 0, "$symbol lost its content binding")
                }
            } finally {
                cache.close()
            }
        }
    }

    @Test fun `legacy unlocks migrate without changing upper floor position or other attrs`() {
        val player = Player()
        player.attr.putAllFromPersistence(mapOf(
            "motherlode_upper_floor_unlocked" to true,
            "motherlode_super_hopper_unlocked" to true,
            "unrelated_content" to 17,
        ))

        player.migrateMotherlodeUnlocks()

        assertTrue(player.motherlodeUpperFloorUnlocked)
        assertTrue(player.motherlodeSuperHopperUnlocked)
        assertFalse(player.motherlodeInRestrictedArea)
        assertEquals(mapOf("unrelated_content" to 17), player.attr.toPersistentMap())
        player.migrateMotherlodeUnlocks()
        assertTrue(player.motherlodeSuperHopperUnlocked)
    }

    @Test fun `migration cannot revoke current unlocks and separate flags do not overlap`() {
        val player = Player()
        player.motherlodeUpperFloorUnlocked = true
        player.attr.putAllFromPersistence(mapOf("motherlode_upper_floor_unlocked" to false))
        player.migrateMotherlodeUnlocks()
        assertTrue(player.motherlodeUpperFloorUnlocked)
        assertFalse(player.motherlodeSuperHopperUnlocked)
        player.motherlodeSuperHopperUnlocked = true
        player.motherlodeUpperFloorUnlocked = false
        assertTrue(player.motherlodeSuperHopperUnlocked)
    }

    @Test fun `unlocks persist while sack notification clock is temporary and server only`() {
        val unlocks = checkNotNull(ServerCacheManager.getVarp("varp.motherlode_unlocks".asRSCM()))
        val notify = checkNotNull(ServerCacheManager.getVarp("varp.motherlode_last_sack_notify".asRSCM()))
        assertEquals(VarpLifetime.Perm, unlocks.scope)
        assertEquals(VarpLifetime.Temp, notify.scope)
        assertEquals(VarpTransmitLevel.Never, unlocks.transmit)
        assertEquals(VarpTransmitLevel.Never, notify.transmit)
        val player = Player()
        assertEquals(0, player.motherlodeLastSackNotify)
        player.motherlodeLastSackNotify = 101
        assertEquals(101, player.motherlodeLastSackNotify)
        assertFalse(player.motherlodeUpperFloorUnlocked)
    }

    @Test fun `cache ore definitions preserve persisted item payloads xp and roll priority`() {
        val expected = linkedMapOf(
            "obj.motherlode_nugget" to 0.0,
            "obj.runite_ore" to 75.0,
            "obj.adamantite_ore" to 45.0,
            "obj.mithril_ore" to 30.0,
            "obj.gold_ore" to 15.0,
            "obj.coal" to 15.0,
        )
        assertEquals(expected.keys.map { it.asRSCM() }, MotherlodeConstants.sackTierOrderIds)
        for ((symbol, xp) in expected) {
            val savedPayload = symbol.asRSCM()
            assertEquals(savedPayload, MotherlodeConstants.payloadIdOf[symbol])
            assertEquals(symbol, MotherlodeConstants.payloadObjOf[savedPayload])
            assertEquals(xp, MotherlodeConstants.arrivalXpFor(savedPayload))
        }
        assertEquals(listOf(85, 70, 55, 40), MotherlodeConstants.ORE_TIERS.map { it.req })
        assertEquals(listOf(-20, -90, -19, -40), MotherlodeConstants.ORE_TIERS.map { it.low })
        assertEquals(listOf(5, 50, 90, 126), MotherlodeConstants.ORE_TIERS.map { it.high })
    }

    @Test fun `upper floor cache respects bridge plane boundary holes and ladder landings`() {
        for (coords in listOf(CoordGrid(3755, 5675), CoordGrid(3755, 5677), CoordGrid(3743, 5687))) {
            assertTrue(coords.isMotherlodeUpperFloor(), coords.toString())
            assertFalse(CoordGrid(coords.x, coords.z, 1).isMotherlodeUpperFloor())
        }
        for (coords in listOf(
            CoordGrid(3755, 5673), CoordGrid(3738, 5686), CoordGrid(3752, 5686),
            CoordGrid(3749, 5685), CoordGrid(3742, 5681), CoordGrid(3748, 5672),
        )) assertFalse(coords.isMotherlodeUpperFloor(), coords.toString())
        assertEquals(4, MotherlodeData.depletedVeins.size)
        for (variant in listOf("single", "left", "middle", "right")) {
            assertEquals("loc.motherlode_depleted_$variant", MotherlodeData.depletedVeins["loc.motherlode_ore_$variant"])
        }
    }

    companion object {
        @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(241).close() }
    }
}
