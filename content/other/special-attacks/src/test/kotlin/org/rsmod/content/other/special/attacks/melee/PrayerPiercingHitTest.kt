package org.rsmod.content.other.special.attacks.melee

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.*
import org.mockito.Mockito.*
import org.rsmod.annotations.InternalApi
import org.rsmod.api.combat.manager.PlayerAttackManager
import org.rsmod.api.player.cheat.adminGodMode
import org.rsmod.api.player.hit.modifier.PlayerHitModifier
import org.rsmod.api.player.hit.modifier.StandardPlayerHitModifier
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory

@ResourceLock("ServerCacheManager")
@Execution(ExecutionMode.SAME_THREAD)
@OptIn(InternalApi::class)
class PrayerPiercingHitTest {
    private var Player.protectMelee by intVarBit("varbit.prayer_protectfrommelee")

    @Test fun `real combat queue bypasses prayer without bypassing normal hit modifier`() {
        val modifier = StandardPlayerHitModifier(mock(EventBus::class.java))
        val constructor = PlayerAttackManager::class.java.constructors.single()
        val manager = constructor.newInstance(*constructor.parameterTypes.map {
            when (it) {
                PlayerHitModifier::class.java -> modifier
                Set::class.java -> emptySet<Any>()
                else -> mock(it)
            }
        }.toTypedArray()) as PlayerAttackManager
        val source = player(1)
        val target = player(2).apply { protectMelee = 1 }
        assertEquals(12, manager.queueMeleeHit(source, target, 20, 1).damage)
        assertEquals(20, manager.queueMeleeHitIgnoringPrayer(source, target, 20, 1).damage)
        target.adminGodMode = true
        assertEquals(0, manager.queueMeleeHitIgnoringPrayer(source, target, 20, 1).damage)
    }

    private fun player(slot: Int) = Player().apply {
        slotId = slot; uuid = slot.toLong(); assignUid()
        worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
    }

    companion object { @JvmStatic @BeforeAll fun setup() { ServerCacheManager.init(240).close() } }
}
