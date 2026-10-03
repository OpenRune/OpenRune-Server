package org.rsmod.content.other.special.attacks.melee

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.util.Wearpos
import java.util.EnumSet
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.*
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.accuracy.melee.MeleeAccuracyOperations
import org.rsmod.api.combat.formulas.attributes.CombatNpcAttributes
import org.rsmod.api.combat.formulas.attributes.collector.CombatMeleeAttributeCollector
import org.rsmod.api.combat.formulas.maxhit.melee.MeleeMaxHitOperations
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory

@ResourceLock("ServerCacheManager")
@Execution(ExecutionMode.SAME_THREAD)
class DemonbaneFormulaTest {
    @Test fun `equipped swords apply the right damage and accuracy bonuses with Duke resistance`() {
        val player = Player().apply {
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
        }
        for ((weapon, normal, resistant) in listOf(
            Triple("obj.silverlight", 160, 142), Triple("obj.darklight", 160, 142),
            Triple("obj.agrith_silverlight_dyed", 160, 142),
            Triple("obj.arclight", 170, 149), Triple("obj.emberlight", 170, 149),
            Triple("obj.dragon_shortsword", 100, 100))) {
            player.worn[Wearpos.RightHand.slot] = InvObj(weapon)
            val attributes = CombatMeleeAttributeCollector().collect(player, MeleeAttackType.Stab)
            val demon = EnumSet.of(CombatNpcAttributes.Demon)
            val duke = EnumSet.of(CombatNpcAttributes.Demon, CombatNpcAttributes.DemonbaneResistance)
            val ordinary = EnumSet.noneOf(CombatNpcAttributes::class.java)
            assertEquals(normal, MeleeMaxHitOperations.modifyBaseDamage(100, attributes, demon), weapon)
            assertEquals(resistant, MeleeMaxHitOperations.modifyBaseDamage(100, attributes, duke), weapon)
            assertEquals(100, MeleeMaxHitOperations.modifyBaseDamage(100, attributes, ordinary), weapon)
            val dyed = weapon == "obj.agrith_silverlight_dyed"
            assertEquals(if (dyed) 100 else normal, MeleeAccuracyOperations.modifyAttackRoll(100, attributes, demon), weapon)
            assertEquals(if (dyed) 100 else resistant, MeleeAccuracyOperations.modifyAttackRoll(100, attributes, duke), weapon)
            assertEquals(100, MeleeAccuracyOperations.modifyAttackRoll(100, attributes, ordinary), weapon)
        }
    }

    companion object { @JvmStatic @BeforeAll fun setup() { ServerCacheManager.init(240).close() } }
}
