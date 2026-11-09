package org.rsmod.content.generic.locs

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.util.UncheckedType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.weapon.WeaponSpeeds
import org.rsmod.api.combat.weapon.scripts.WeaponAttackStylesScript
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.interfaces.equipment.stats.EquipmentStats
import org.rsmod.game.inv.InvObj

@OptIn(UncheckedType::class)
@Execution(ExecutionMode.SAME_THREAD)
class EquipmentStatsEquipTest {
    @Test
    fun GameTestState.`equip button wields op4 knife into offhand swapping shield with full inventory`() =
        statsTest {
            player.inv[2] = InvObj(id(KNIFE), 1)
            player.worn[offhand] = InvObj(id(SHIELD), 1)
            equipButton(2)

            assertEquals(id(SHIELD), player.inv[2]?.id)
            assertEquals(id(KNIFE), player.worn[offhand]?.id)
            assertEquals(27, player.inv.count(SPADE))
        }

    @Test
    fun GameTestState.`equip button wields normal op2 item`() = statsTest {
        player.inv[5] = InvObj(id(SHIELD), 1)
        equipButton(5)

        assertNull(player.inv[5])
        assertEquals(id(SHIELD), player.worn[offhand]?.id)
        assertEquals(27, player.inv.count(SPADE))
    }

    private fun GameTestState.statsTest(body: GameTestScope.() -> Unit) =
        runInjectedGameTest(
            WeaponSpeeds::class,
            CombatModule(),
            EquipmentStats::class,
            WeaponAttackStylesScript::class,
        ) {
            player.clearAllInvs()
            for (slot in 0 until player.inv.size) {
                player.inv[slot] = InvObj(id(SPADE), 1)
            }
            player.ifOpenOverlay("interface.wornitems", "component.toplevel_osrs_stretch:side4")
            body()
        }

    private fun GameTestScope.equipButton(slot: Int) {
        player.ifButton(component("component.wornitems:equipment"))
        advance()
        player.ifButton(component("component.equipment_side:items"), comsub = slot)
        advance()
    }

    private fun component(name: String) =
        ServerCacheManager.fromComponent(name.asRSCM(RSCMType.COMPONENT))

    private val GameTestScope.offhand: Int
        get() = checkNotNull(ServerCacheManager.getItem(id(KNIFE))).wearpos1

    private fun id(item: String): Int = item.asRSCM(RSCMType.OBJ)

    private class CombatModule : AbstractModule() {
        override fun configure() {
            bind(AttackStyles::class.java).`in`(Scopes.SINGLETON)
            bind(WeaponSpeeds::class.java).`in`(Scopes.SINGLETON)
        }
    }

    private companion object {
        const val KNIFE = "obj.fletching_knife"
        const val SHIELD = "obj.iron_kiteshield"
        const val SPADE = "obj.spade"
    }
}
