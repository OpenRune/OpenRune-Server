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
    fun `prune animation resolves in the cache`() {
        withCache {
            assertDoesNotThrow {
                ServerCacheManager.getAnim(BryophytaGrowthling.PRUNE_ANIM.asRSCM(RSCMType.SEQ))
            }
        }
    }

    @Test
    fun `growthling uses tanglefoot animations and keeps chase range`() {
        withCache {
            val growthling = ServerCacheManager.getNpc(GROWTHLING.asRSCM(RSCMType.NPC))
            assertNotNull(growthling)
            assertEquals(10, growthling?.hitpoints)
            assertEquals(40, growthling?.attack)
            assertEquals(38, growthling?.strength)
            assertEquals(40, growthling?.defence)
            assertEquals(1, growthling?.magic)
            assertEquals(1, growthling?.ranged)
            assertEquals(37, growthling?.combatLevel)
            assertEquals(4, growthling?.param(params.attackrate))
            assertEquals(1, growthling?.paramOrNull(params.poison_immunity))
            assertEquals(2, growthling?.paramOrNull(params.venom_immunity))
            assertTrue(
                growthling?.paramOrNull(params.npc_attack_type)
                    ?.isType("category.attacktype_slash") == true
            )
            assertEquals(
                "seq.tanglefoot_attack".asRSCM(RSCMType.SEQ),
                growthling?.paramOrNull(params.attack_anim)?.id,
            )
            assertEquals(
                "seq.tanglefoot_block".asRSCM(RSCMType.SEQ),
                growthling?.paramOrNull(params.defend_anim)?.id,
            )
            assertEquals(
                "seq.tanglefoot_death".asRSCM(RSCMType.SEQ),
                growthling?.paramOrNull(params.death_anim)?.id,
            )
            assertEquals(30, growthling?.maxRange)
            assertEquals(0, growthling?.wanderRange)
        }
    }

    @Test
    fun `boss uses crush accuracy and osrs experience bonus`() {
        withCache {
            val boss = ServerCacheManager.getNpc(BOSS.asRSCM(RSCMType.NPC))
            assertNotNull(boss)
            assertEquals(33, boss?.paramOrNull(params.attack_melee))
            assertEquals(1025, boss?.paramOrNull(params.npc_com_xp_multiplier))
            assertTrue(
                boss?.paramOrNull(params.npc_attack_type)
                    ?.isType("category.attacktype_crush") == true
            )
        }
    }

    @Test
    fun `boss uses moss giant animations and keeps attack range`() {
        withCache {
            val boss = ServerCacheManager.getNpc(BOSS.asRSCM(RSCMType.NPC))
            assertNotNull(boss)
            assertEquals(115, boss?.hitpoints)
            assertEquals(130, boss?.attack)
            assertEquals(100, boss?.strength)
            assertEquals(100, boss?.defence)
            assertEquals(90, boss?.magic)
            assertEquals(1, boss?.ranged)
            assertEquals(128, boss?.combatLevel)
            assertEquals(6, boss?.param(params.attackrate))
            assertEquals("seq.giant_update_moss_attack".asRSCM(RSCMType.SEQ), boss?.paramOrNull(params.attack_anim)?.id)
            assertEquals("seq.giant_update_moss_defend".asRSCM(RSCMType.SEQ), boss?.paramOrNull(params.defend_anim)?.id)
            assertEquals("seq.giant_update_moss_death".asRSCM(RSCMType.SEQ), boss?.paramOrNull(params.death_anim)?.id)
            assertEquals(30, boss?.maxRange)
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
