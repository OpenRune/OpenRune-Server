package org.rsmod.content.bosses.araxxor

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.varp.baseVar
import dev.openrune.types.varp.bits
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.annotations.InternalApi
import org.rsmod.api.combat.formulas.accuracy.magic.NvPMagicAccuracy
import org.rsmod.api.combat.formulas.accuracy.melee.NvPMeleeAccuracy
import org.rsmod.api.combat.formulas.accuracy.ranged.NvPRangedAccuracy
import org.rsmod.api.player.cheat.adminGodMode
import org.rsmod.api.player.hit.modifier.StandardPlayerHitModifier
import org.rsmod.api.player.hit.processor.InstantPlayerHitProcessor
import org.rsmod.api.random.GameRandom
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.Inventory
import org.rsmod.map.CoordGrid

@ResourceLock("ServerCacheManager")
@OptIn(InternalApi::class)
class AraxxorCombatTest {
    @Test fun `prayer is snapshotted at launch and normal player mitigation still applies`() {
        val melee = mock(NvPMeleeAccuracy::class.java) { Int.MAX_VALUE }
        val ranged = mock(NvPRangedAccuracy::class.java) { Int.MAX_VALUE }
        val magic = mock(NvPMagicAccuracy::class.java) { Int.MAX_VALUE }
        val random = mock(GameRandom::class.java) { call ->
            (call.arguments.firstOrNull() as? IntRange)?.last ?: 0
        }
        val impacts = mutableListOf<Hit>()
        val processor = InstantPlayerHitProcessor { impacts += it }
        val combat = AraxxorCombat(melee, ranged, magic, random,
            StandardPlayerHitModifier(mock(EventBus::class.java)), processor)
        val npc = Npc(checkNotNull(ServerCacheManager.getNpc(AraxxorAssets.BOSS.asRSCM()))).apply {
            slotId = 1; assignUid()
        }
        val player = Player().apply {
            slotId = 1; uuid = 1; assignUid()
            statMap.setCurrentLevel("stat.hitpoints", 99)
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
        }
        for ((style, prayer, maximum) in listOf(
            Triple(HitType.Melee, "varbit.prayer_protectfrommelee", 5),
            Triple(HitType.Ranged, "varbit.prayer_protectfrommissiles", 18),
            Triple(HitType.Magic, "varbit.prayer_protectfrommagic", 0),
        )) {
            val bit = checkNotNull(ServerCacheManager.getVarbit(prayer.asRSCM()))
            player.vars.backing[bit.baseVar.id] = 1 shl bit.bits.first
            val hit = combat.roll(npc, player, style)
            assertEquals(maximum, hit.damage)
            player.vars.backing[bit.baseVar.id] = 0
            combat.impact(npc, player, hit)
            assertEquals(maximum, impacts.last().damage)
        }
        player.adminGodMode = true
        assertEquals(0, combat.roll(npc, player, HitType.Melee).damage)
    }

    @Test fun `cleave captures three tiles perpendicular to attack bearing`() {
        val boss = CoordGrid(100, 100)
        val target = CoordGrid(107, 103)
        val cleave = AraxxorAttackRules.cleave(boss, 7, target)
        assertEquals(listOf(CoordGrid(107, 102), target, CoordGrid(107, 104)), cleave)
        assertFalse(target.translate(0, 2) in cleave)
        assertFalse(target.translate(1, 0) in cleave)
        assertEquals(listOf(CoordGrid(102, 107), CoordGrid(103, 107), CoordGrid(104, 107)),
            AraxxorAttackRules.cleave(boss, 7, CoordGrid(103, 107)))
    }

    @Test fun `acid ray retains shallow bearing rather than snapping to a diagonal`() {
        val from = CoordGrid(100, 100)
        val through = CoordGrid(106, 102)
        val ray = AraxxorAttackRules.ray(from, through, 12)
        assertEquals(through, ray[5])
        assertEquals(CoordGrid(112, 104), ray.last())
        assertEquals(12, ray.distinct().size)
        assertTrue(AraxxorAttackRules.ray(from, from, 12).isEmpty())
    }

    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
