package org.rsmod.content.generic.locs

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfButtonOp
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
import org.rsmod.content.interfaces.bank.openBank
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.content.interfaces.bank.scripts.BankOpenScript
import org.rsmod.game.inv.InvObj

@OptIn(UncheckedType::class)
@Execution(ExecutionMode.SAME_THREAD)
class BankEquipmentTest {
    @Test
    fun GameTestState.`extra op wields op4 knife into offhand with full inventory`() =
        runInjectedGameTest(
            WeaponSpeeds::class, CombatModule(),
            BankInvScript::class, BankOpenScript::class, WeaponAttackStylesScript::class,
        ) {
            player.clearAllInvs()
            fillWithSpades()
            player.inv[2] = InvObj(id(KNIFE), 1)
            player.openBank(eventBus)

            extraOp(2)
            advance()

            assertNull(player.inv[2])
            assertEquals(id(KNIFE), player.worn[offhand]?.id)
            assertEquals(27, player.inv.count(SPADE))
        }

    @Test
    fun GameTestState.`extra op wield displaces offhand shield into clicked slot`() =
        runInjectedGameTest(
            WeaponSpeeds::class, CombatModule(),
            BankInvScript::class, BankOpenScript::class, WeaponAttackStylesScript::class,
        ) {
            player.clearAllInvs()
            fillWithSpades()
            player.inv[7] = InvObj(id(KNIFE), 1)
            player.worn[offhand] = InvObj(id(SHIELD), 1)
            player.openBank(eventBus)

            extraOp(7)
            advance()

            assertEquals(id(SHIELD), player.inv[7]?.id)
            assertEquals(id(KNIFE), player.worn[offhand]?.id)
            assertEquals(27, player.inv.count(SPADE))
        }

    @Test
    fun GameTestState.`extra op wields shield swapping worn knife and keeps spades`() =
        runInjectedGameTest(
            WeaponSpeeds::class, CombatModule(),
            BankInvScript::class, BankOpenScript::class, WeaponAttackStylesScript::class,
        ) {
            player.clearAllInvs()
            fillWithSpades()
            player.inv[4] = InvObj(id(SHIELD), 1)
            player.worn[offhand] = InvObj(id(KNIFE), 1)
            player.openBank(eventBus)

            extraOp(4)
            advance()

            assertEquals(id(KNIFE), player.inv[4]?.id)
            assertEquals(id(SHIELD), player.worn[offhand]?.id)
            assertEquals(27, player.inv.count(SPADE))
        }

    private fun GameTestScope.fillWithSpades() {
        for (slot in 0 until player.inv.size) {
            player.inv[slot] = InvObj(id(SPADE), 1)
        }
    }

    private fun GameTestScope.extraOp(slot: Int) {
        val items = ServerCacheManager.fromComponent("component.bankside:items".asRSCM(RSCMType.COMPONENT))
        player.ifButton(items, comsub = slot, op = IfButtonOp.Op9)
    }

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
