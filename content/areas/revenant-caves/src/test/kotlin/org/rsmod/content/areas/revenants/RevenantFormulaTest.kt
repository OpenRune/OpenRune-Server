package org.rsmod.content.areas.revenants

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.util.Wearpos
import java.util.EnumSet
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.combat.formulas.accuracy.magic.MagicAccuracyOperations
import org.rsmod.api.combat.formulas.attributes.*
import org.rsmod.api.combat.formulas.attributes.collector.*
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.random.DefaultGameRandom
import org.rsmod.game.entity.*
import org.rsmod.game.inv.*
import org.rsmod.map.CoordGrid

@ResourceLock("ServerCacheManager")
internal class RevenantFormulaTest {
    @Test fun `activation deposit alone never grants the 50 percent Wilderness weapon boost`() {
        for (kind in RevenantCharges.weapons)for (count in listOf(0, 1000, 1001)) {
            val p = player(); p.worn[Wearpos.RightHand.slot] = RevenantCharges.withCount(InvObj(kind.empty), count)
            val enabled = when {
                kind.empty.contains("bow_") || kind.empty.contains("webweaver_") -> CombatRangedAttributes.RevenantWeapon in CombatRangedAttributeCollector().collect(p, null, null)
                kind.empty.contains("chainmace_") || kind.empty.contains("ursine_") -> CombatMeleeAttributes.RevenantWeapon in CombatMeleeAttributeCollector().collect(p, null)
                else -> CombatStaffAttributes.RevenantWeapon in CombatMagicAttributeCollector().staffCollect(p, DefaultGameRandom(1))
            }
            assertEquals(count > 1000, enabled, kind.empty)
        }
    }

    @Test fun `sceptre accuracy boost applies to any Wilderness NPC and never outside or in PvP`() {
        val target = Npc(ServerCacheManager.getNpc("npc.man".asRSCM())!!, CoordGrid(3197, 10056))
        val collector = CombatNpcAttributeCollector(); val staff = EnumSet.of(CombatStaffAttributes.RevenantWeapon)
        val cave = collector.collect(target.type, target, target.hitpoints, target.hitpoints, false)
        assertFalse(CombatNpcAttributes.Revenant in cave); assertEquals(150, MagicAccuracyOperations.modifyStaffAttackRoll(100, staff, cave))
        target.coords = CoordGrid(3200, 3200); val outside = collector.collect(target.type, target, target.hitpoints, target.hitpoints, false)
        assertEquals(100, MagicAccuracyOperations.modifyStaffAttackRoll(100, staff, outside)); assertEquals(100, MagicAccuracyOperations.modifyStaffAttackRoll(100))
    }

    @Test fun `ether bows ignore ordinary equipped ammunition bonuses`() {
        for (name in listOf("obj.wild_cave_bow_uncharged", "obj.wild_cave_webweaver_uncharged")) {
            val p = player(); p.worn[Wearpos.RightHand.slot] = RevenantCharges.withCount(InvObj(name), 1001)
            val baseline = WornBonuses().rangedStrengthBonus(p)
            p.worn[Wearpos.Quiver.slot] = InvObj("obj.dragon_arrow", 100)
            assertEquals(baseline, WornBonuses().rangedStrengthBonus(p))
        }
    }
    private fun player() = Player().apply { worn = Inventory(ServerCacheManager.getInventory("inv.worn".asRSCM())!!, arrayOfNulls(14)) }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
