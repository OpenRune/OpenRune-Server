package org.rsmod.content.other.special.weapons.scripts.charge

import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isType

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class TridentOfTheSeasChargingTest {
    class Deps @Inject constructor(val charges: ObjChargeManager, val objRepo: ObjRepository)

    private fun GameTestScope.stock(death: Int, chaos: Int, fire: Int, coins: Int) {
        player.clearInv()
        player.inv[1] = InvObj("obj.deathrune", death)
        player.inv[2] = InvObj("obj.chaosrune", chaos)
        player.inv[3] = InvObj("obj.firerune", fire)
        player.inv[4] = InvObj("obj.coins", coins)
    }

    private fun GameTestScope.total(obj: String) = player.inv.count(obj)

    private fun GameTestScope.charge(deps: Deps, requested: Int, player: Player = this.player) {
        val script = TridentOfTheSeasCharging(deps.charges, deps.objRepo)
        player.withProtectedAccess {
            with(script) { applyCharge(inv, TRIDENT_SLOT, requested) }
        }
    }

    private fun GameTestScope.uncharge(deps: Deps) {
        val script = TridentOfTheSeasCharging(deps.charges, deps.objRepo)
        player.withProtectedAccess { with(script) { applyUncharge(inv, TRIDENT_SLOT) } }
    }

    @Test
    fun GameTestState.`charging takes exactly the materials for the charges added`() =
        runInjectedGameTest(Deps::class) { deps ->
            stock(death = 20, chaos = 20, fire = 100, coins = 200)
            player.inv[TRIDENT_SLOT] = InvObj("obj.tots_uncharged")

            charge(deps, requested = 5)

            assertEquals(15, total("obj.deathrune"))
            assertEquals(15, total("obj.chaosrune"))
            assertEquals(75, total("obj.firerune"))
            assertEquals(150, total("obj.coins"))
            assertEquals(5, deps.charges.getCharges(player.inv[TRIDENT_SLOT]!!, CHARGE_VAROBJ))
        }

    @Test
    fun GameTestState.`charging more than the materials allow takes nothing`() =
        runInjectedGameTest(Deps::class) { deps ->
            stock(death = 3, chaos = 3, fire = 100, coins = 200)
            player.inv[TRIDENT_SLOT] = InvObj("obj.tots_uncharged")

            charge(deps, requested = 4)

            assertEquals(3, total("obj.deathrune"))
            assertEquals(3, total("obj.chaosrune"))
            assertEquals(100, total("obj.firerune"))
            assertEquals(200, total("obj.coins"))
            assertTrue(player.inv[TRIDENT_SLOT].isType("obj.tots_uncharged"))
        }

    @Test
    fun GameTestState.`charging past the maximum takes nothing`() =
        runInjectedGameTest(Deps::class) { deps ->
            stock(death = 10, chaos = 10, fire = 50, coins = 100)
            player.inv[TRIDENT_SLOT] = InvObj("obj.tots")

            charge(deps, requested = 1)

            assertEquals(10, total("obj.deathrune"))
            assertEquals(10, total("obj.chaosrune"))
            assertEquals(50, total("obj.firerune"))
            assertEquals(100, total("obj.coins"))
        }

    @Test
    fun GameTestState.`charging after the trident left the slot takes nothing`() =
        runInjectedGameTest(Deps::class) { deps ->
            stock(death = 10, chaos = 10, fire = 50, coins = 100)

            charge(deps, requested = 5)

            assertEquals(10, total("obj.deathrune"))
            assertEquals(10, total("obj.chaosrune"))
            assertEquals(50, total("obj.firerune"))
            assertEquals(100, total("obj.coins"))
        }

    @Test
    fun GameTestState.`charging with the materials gone since the prompt takes nothing`() =
        runInjectedGameTest(Deps::class) { deps ->
            stock(death = 10, chaos = 10, fire = 50, coins = 100)
            player.inv[TRIDENT_SLOT] = InvObj("obj.tots_uncharged")
            player.inv[4] = null

            charge(deps, requested = 5)

            assertEquals(10, total("obj.deathrune"))
            assertEquals(10, total("obj.chaosrune"))
            assertEquals(50, total("obj.firerune"))
            assertTrue(player.inv[TRIDENT_SLOT].isType("obj.tots_uncharged"))
        }

    @Test
    fun GameTestState.`uncharging a charged trident returns every rune`() =
        runInjectedGameTest(Deps::class) { deps ->
            player.clearInv()
            player.inv[TRIDENT_SLOT] = InvObj("obj.tots_uncharged")
            deps.charges.addCharges(player.inv, TRIDENT_SLOT, 10, CHARGE_VAROBJ, MAX_CHARGES)

            uncharge(deps)

            assertEquals(10, total("obj.deathrune"))
            assertEquals(10, total("obj.chaosrune"))
            assertEquals(50, total("obj.firerune"))
            assertTrue(player.inv[TRIDENT_SLOT].isType("obj.tots_uncharged"))
        }

    @Test
    fun GameTestState.`uncharging a full trident returns the maximum charges worth of runes`() =
        runInjectedGameTest(Deps::class) { deps ->
            player.clearInv()
            player.inv[TRIDENT_SLOT] = InvObj("obj.tots")

            uncharge(deps)

            assertEquals(2_500, total("obj.deathrune"))
            assertEquals(2_500, total("obj.chaosrune"))
            assertEquals(12_500, total("obj.firerune"))
            assertTrue(player.inv[TRIDENT_SLOT].isType("obj.tots_uncharged"))
        }

    @Test
    fun GameTestState.`uncharging after the trident left the slot returns nothing`() =
        runInjectedGameTest(Deps::class) { deps ->
            player.clearInv()

            uncharge(deps)

            assertEquals(0, total("obj.deathrune"))
            assertEquals(0, total("obj.chaosrune"))
            assertEquals(0, total("obj.firerune"))
        }

    private companion object {
        const val TRIDENT_SLOT = 0
        const val CHARGE_VAROBJ = "varobj.charges_16383"
        const val MAX_CHARGES = 2_500
    }
}
