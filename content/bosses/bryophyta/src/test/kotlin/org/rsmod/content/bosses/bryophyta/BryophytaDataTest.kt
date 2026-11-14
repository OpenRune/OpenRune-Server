package org.rsmod.content.bosses.bryophyta

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.config.refs.params
import org.rsmod.api.table.prayer.SkillPrayerRow

@ResourceLock("ServerCacheManager")
class BryophytaDataTest {

    @Test
    fun `giant bones grant 150 prayer experience`() {
        withCache {
            val row = SkillPrayerRow.all().firstOrNull { it.item.internalName == GIANT_BONES }
            assertNotNull(row)
            assertEquals(150, row?.exp)
        }
    }

    @Test
    fun `boss poisons for 8 and drops no default remains`() {
        withCache {
            val boss = ServerCacheManager.getNpc(BOSS.asRSCM(RSCMType.NPC))
            assertNotNull(boss)
            assertEquals(40, boss?.paramOrNull(params.npc_poison_severity))
            assertEquals(true, boss?.hasParam(params.dropped_remains.raw))
            assertNull(boss?.paramOrNull(params.dropped_remains))
        }
    }

    @Test
    fun `growthlings drop no default remains`() {
        withCache {
            val growthling = ServerCacheManager.getNpc(GROWTHLING.asRSCM(RSCMType.NPC))
            assertNotNull(growthling)
            assertEquals(true, growthling?.hasParam(params.dropped_remains.raw))
            assertNull(growthling?.paramOrNull(params.dropped_remains))
        }
    }

    @Test
    fun `axes and magic secateurs prune in one click`() {
        withCache {
            val bronzeAxe = ServerCacheManager.getItem("obj.bronze_axe".asRSCM(RSCMType.OBJ))
            val magicSecateurs =
                ServerCacheManager.getItem("obj.fairy_enchanted_secateurs".asRSCM(RSCMType.OBJ))
            assertNotNull(bronzeAxe)
            assertNotNull(magicSecateurs)
            assertTrue(BryophytaGrowthling.isOneClick(bronzeAxe!!))
            assertTrue(BryophytaGrowthling.isOneClick(magicSecateurs!!))
        }
    }

    @Test
    fun `regular secateurs only prune as a use item`() {
        withCache {
            val secateurs = ServerCacheManager.getItem("obj.secateurs".asRSCM(RSCMType.OBJ))
            assertNotNull(secateurs)
            assertFalse(BryophytaGrowthling.isOneClick(secateurs!!))
            assertTrue(BryophytaGrowthling.isAxeOrSecateurs(secateurs))
        }
    }

    @Test
    fun `prune animations resolve in the cache`() {
        withCache {
            val weapons =
                listOf(
                    "obj.bronze_axe",
                    "obj.secateurs",
                    "obj.fairy_enchanted_secateurs",
                )
            for (weapon in weapons) {
                val type = ServerCacheManager.getItem(weapon.asRSCM(RSCMType.OBJ))
                assertNotNull(type)
                val seq = BryophytaGrowthling.pruneAnim(type!!)
                assertDoesNotThrow { ServerCacheManager.getAnim(seq.asRSCM(RSCMType.SEQ)) }
            }
        }
    }

    private fun withCache(block: () -> Unit) {
        val cache = ServerCacheManager.init(240)
        try {
            block()
        } finally {
            cache.close()
        }
    }

    private companion object {
        private const val BOSS = "npc.gb_mossgiant"
        private const val GROWTHLING = "npc.gb_growthling"
        private const val GIANT_BONES = "obj.giant_bones"
    }
}
