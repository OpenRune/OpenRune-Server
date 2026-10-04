package org.rsmod.content.bosses.araxxor

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.commons.types.RangedAttackType
import org.rsmod.api.combat.manager.NpcMaxHitRegistry
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.table.slayer.SlayerTaskRow
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.map.CoordGrid

@ResourceLock("ServerCacheManager")
class AraxyteRulesTest {
    @Test fun `crush and heavy ranged require the highest equipped accuracy bonus`() {
        val player = Player().apply {
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
        }
        val bonuses = mock(WornBonuses::class.java)
        val rules = AraxyteMaxHits(bonuses)
        `when`(bonuses.offensiveCrushBonus(player)).thenReturn(90)
        `when`(bonuses.offensiveSlashBonus(player)).thenReturn(100)
        assertFalse(rules.melee(player, MeleeAttackType.Crush))
        `when`(bonuses.offensiveCrushBonus(player)).thenReturn(100)
        assertTrue(rules.melee(player, MeleeAttackType.Crush))
        assertFalse(rules.melee(player, MeleeAttackType.Slash))
        `when`(bonuses.offensiveRangedBonus(player)).thenReturn(110)
        assertFalse(rules.melee(player, MeleeAttackType.Crush))
        assertTrue(rules.ranged(player, RangedAttackType.Heavy))
        assertFalse(rules.ranged(player, RangedAttackType.Light))
        player.worn[dev.openrune.util.Wearpos.RightHand.slot] = org.rsmod.game.inv.InvObj("obj.noxious_halberd")
        assertTrue(rules.melee(player, MeleeAttackType.Slash))
    }

    @Test fun `max hit extension cannot affect unregistered bosses or players`() {
        val registry = NpcMaxHitRegistry()
        val player = Player()
        val spider = Npc(checkNotNull(ServerCacheManager.getNpc(AraxyteKind.RUPTURA.spider.asRSCM())))
        val boss = Npc(checkNotNull(ServerCacheManager.getNpc(AraxxorAssets.BOSS.asRSCM())))
        registry.register(spider.id, { _, _ -> true }, { _, _ -> true })
        assertTrue(registry.melee(player, spider, MeleeAttackType.Crush))
        assertTrue(registry.ranged(player, spider, RangedAttackType.Heavy))
        assertFalse(registry.melee(player, boss, MeleeAttackType.Crush))
        assertFalse(registry.ranged(player, Player(), RangedAttackType.Heavy))
        assertThrows(IllegalStateException::class.java) {
            registry.register(spider.id, { _, _ -> false }, { _, _ -> false })
        }
    }

    @Test fun `ruptura egg damage leaves one hp and distances respect both footprints`() {
        val source = CoordGrid(100, 100)
        assertEquals(0, AraxxorAttackRules.distance(source, 2, CoordGrid(99, 99), 7))
        for (tile in listOf(CoordGrid(99, 100), CoordGrid(102, 100), CoordGrid(100, 99), CoordGrid(100, 102))) {
            assertEquals(1, AraxxorAttackRules.distance(source, 2, tile, 1))
        }
        assertEquals(1, 65 - AraxxorAttackRules.rupturaNpcMax(0, egg = true))
        assertEquals(80, AraxxorAttackRules.rupturaNpcMax(0, egg = false))
        assertEquals(64, AraxxorAttackRules.rupturaNpcMax(1, egg = false))
        assertEquals(0, AraxxorAttackRules.rupturaNpcMax(4, egg = false))
    }

    @Test fun `native spider and araxyte assignments permit boosted level and reject exhausted tasks`() {
        val tasks = SlayerTaskRow.all().filter { it.nameLowercase in setOf("araxytes", "spiders") }
        assertEquals(2, tasks.size)
        val player = Player()
        player.statMap.setCurrentLevel("stat.slayer", 92)
        for (task in tasks) {
            player.vars.backing["varp.slayer_target".asRSCM()] = task.id
            player.vars.backing["varp.slayer_count".asRSCM()] = 1
            assertTrue(AraxxorAccess.allowed(player))
            player.vars.backing["varp.slayer_count".asRSCM()] = 0
            assertFalse(AraxxorAccess.allowed(player))
        }
        player.vars.backing["varp.slayer_count".asRSCM()] = 1
        player.statMap.setCurrentLevel("stat.slayer", 91)
        assertFalse(AraxxorAccess.allowed(player))
    }

    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
