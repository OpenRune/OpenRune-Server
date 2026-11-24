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
import org.rsmod.content.other.special.weapons.magic.RevenantSceptre
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isType

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class SpecialStaffChargingTest {
    class Deps @Inject constructor(val charges: ObjChargeManager, val objRepo: ObjRepository)

    private fun GameTestScope.total(obj: String) = player.inv.count(obj)

    private fun GameTestScope.ayakCharge(deps: Deps, requested: Int, runes: Boolean) {
        val script = EyeOfAyakCharging(deps.charges, deps.objRepo)
        player.withProtectedAccess {
            with(script) { applyCharge(inv, SLOT, requested, runes) }
        }
    }

    private fun GameTestScope.ayakUncharge(deps: Deps) {
        val script = EyeOfAyakCharging(deps.charges, deps.objRepo)
        player.withProtectedAccess { with(script) { applyUncharge(inv, SLOT) } }
    }

    private fun GameTestScope.sceptreCharge(deps: Deps, sceptre: RevenantSceptre, requested: Int) {
        val script = RevenantSceptreCharging(deps.charges, deps.objRepo)
        player.withProtectedAccess { with(script) { applyCharge(sceptre, inv, SLOT, requested) } }
    }

    private fun GameTestScope.sceptreUncharge(deps: Deps, sceptre: RevenantSceptre) {
        val script = RevenantSceptreCharging(deps.charges, deps.objRepo)
        player.withProtectedAccess { with(script) { applyUncharge(sceptre, inv, SLOT) } }
    }

    private fun GameTestScope.boneCharge(deps: Deps, requested: Int) {
        val script = BoneStaffCharging(deps.charges, deps.objRepo)
        player.withProtectedAccess { with(script) { applyCharge(inv, SLOT, requested) } }
    }

    private fun GameTestScope.boneUncharge(deps: Deps) {
        val script = BoneStaffCharging(deps.charges, deps.objRepo)
        player.withProtectedAccess { with(script) { applyUncharge(inv, SLOT) } }
    }

    @Test
    fun GameTestState.`eye of ayak charged with demon tears takes one tear a charge`() =
        runInjectedGameTest(Deps::class) { deps ->
            player.clearInv()
            player.inv[1] = InvObj("obj.demon_tear", 100)
            player.inv[SLOT] = InvObj("obj.eye_of_ayak_uncharged")

            ayakCharge(deps, requested = 40, runes = false)

            assertEquals(60, total("obj.demon_tear"))
            assertTrue(player.inv[SLOT].isType("obj.eye_of_ayak"))
            assertEquals(40, deps.charges.getCharges(player.inv[SLOT]!!, AYAK_CHARGES))
            assertEquals(0, deps.charges.getCharges(player.inv[SLOT]!!, AYAK_RUNE_MODE))
        }

    @Test
    fun GameTestState.`eye of ayak charged with runes takes two death and a chaos a charge`() =
        runInjectedGameTest(Deps::class) { deps ->
            player.clearInv()
            player.inv[1] = InvObj("obj.deathrune", 100)
            player.inv[2] = InvObj("obj.chaosrune", 50)
            player.inv[SLOT] = InvObj("obj.eye_of_ayak_uncharged")

            ayakCharge(deps, requested = 30, runes = true)

            assertEquals(40, total("obj.deathrune"))
            assertEquals(20, total("obj.chaosrune"))
            assertEquals(1, deps.charges.getCharges(player.inv[SLOT]!!, AYAK_RUNE_MODE))
        }

    @Test
    fun GameTestState.`eye of ayak charged with runes refuses demon tears`() =
        runInjectedGameTest(Deps::class) { deps ->
            player.clearInv()
            player.inv[1] = InvObj("obj.demon_tear", 100)
            player.inv[SLOT] = InvObj("obj.eye_of_ayak_uncharged")
            deps.charges.addCharges(player.inv, SLOT, 10, AYAK_CHARGES, 50_000)
            deps.charges.setValue(player.inv, SLOT, AYAK_RUNE_MODE, 1)

            ayakCharge(deps, requested = 10, runes = false)

            assertEquals(100, total("obj.demon_tear"))
            assertEquals(10, deps.charges.getCharges(player.inv[SLOT]!!, AYAK_CHARGES))
        }

    @Test
    fun GameTestState.`eye of ayak charging past the maximum takes nothing`() =
        runInjectedGameTest(Deps::class) { deps ->
            player.clearInv()
            player.inv[1] = InvObj("obj.demon_tear", 100)
            player.inv[SLOT] = InvObj("obj.eye_of_ayak_uncharged")
            deps.charges.addCharges(player.inv, SLOT, 49_990, AYAK_CHARGES, 50_000)

            ayakCharge(deps, requested = 11, runes = false)

            assertEquals(100, total("obj.demon_tear"))
            assertEquals(49_990, deps.charges.getCharges(player.inv[SLOT]!!, AYAK_CHARGES))
        }

    @Test
    fun GameTestState.`eye of ayak uncharging returns the runes it was charged with`() =
        runInjectedGameTest(Deps::class) { deps ->
            player.clearInv()
            player.inv[SLOT] = InvObj("obj.eye_of_ayak_uncharged")
            deps.charges.addCharges(player.inv, SLOT, 25, AYAK_CHARGES, 50_000)
            deps.charges.setValue(player.inv, SLOT, AYAK_RUNE_MODE, 1)

            ayakUncharge(deps)

            assertEquals(50, total("obj.deathrune"))
            assertEquals(25, total("obj.chaosrune"))
            assertEquals(0, total("obj.demon_tear"))
            assertTrue(player.inv[SLOT].isType("obj.eye_of_ayak_uncharged"))
            assertEquals(0, deps.charges.getCharges(player.inv[SLOT]!!, AYAK_RUNE_MODE))
        }

    @Test
    fun GameTestState.`sceptre needs the activation deposit before it charges`() =
        runInjectedGameTest(Deps::class) { deps ->
            player.clearInv()
            player.inv[1] = InvObj("obj.wild_cave_shard", 999)
            player.inv[SLOT] = InvObj("obj.wild_cave_sceptre_uncharged")

            sceptreCharge(deps, RevenantSceptre.THAMMARON, requested = 999)

            assertEquals(999, total("obj.wild_cave_shard"))
            assertTrue(player.inv[SLOT].isType("obj.wild_cave_sceptre_uncharged"))
        }

    @Test
    fun GameTestState.`sceptre activates with its deposit and keeps it as ether`() =
        runInjectedGameTest(Deps::class) { deps ->
            player.clearInv()
            player.inv[1] = InvObj("obj.wild_cave_shard", 1_500)
            player.inv[SLOT] = InvObj("obj.wild_cave_accursed_uncharged")

            sceptreCharge(deps, RevenantSceptre.ACCURSED, requested = 1_500)

            assertEquals(0, total("obj.wild_cave_shard"))
            assertTrue(player.inv[SLOT].isType("obj.wild_cave_accursed_charged"))
            assertEquals(1_500, deps.charges.getCharges(player.inv[SLOT]!!, ETHER_VAROBJ))
        }

    @Test
    fun GameTestState.`sceptre uncharging returns every ether including the deposit`() =
        runInjectedGameTest(Deps::class) { deps ->
            player.clearInv()
            player.inv[SLOT] = InvObj("obj.wild_cave_sceptre_charged")
            deps.charges.setValue(player.inv, SLOT, ETHER_VAROBJ, 5_000)

            sceptreUncharge(deps, RevenantSceptre.THAMMARON)

            assertEquals(5_000, total("obj.wild_cave_shard"))
            assertTrue(player.inv[SLOT].isType("obj.wild_cave_sceptre_uncharged"))
            assertEquals(0, deps.charges.getCharges(player.inv[SLOT]!!, ETHER_VAROBJ))
        }

    @Test
    fun GameTestState.`bone staff charging takes chaos runes and uncharging returns them`() =
        runInjectedGameTest(Deps::class) { deps ->
            player.clearInv()
            player.inv[1] = InvObj("obj.chaosrune", 300)
            player.inv[SLOT] = InvObj("obj.rat_bone_staff")

            boneCharge(deps, requested = 200)

            assertEquals(100, total("obj.chaosrune"))
            assertEquals(200, deps.charges.getCharges(player.inv[SLOT]!!, ETHER_VAROBJ))
        }

    @Test
    fun GameTestState.`bone staff uncharging returns every rune`() =
        runInjectedGameTest(Deps::class) { deps ->
            player.clearInv()
            player.inv[SLOT] = InvObj("obj.rat_bone_staff")
            deps.charges.setValue(player.inv, SLOT, ETHER_VAROBJ, 750)

            boneUncharge(deps)

            assertEquals(750, total("obj.chaosrune"))
            assertTrue(player.inv[SLOT].isType("obj.rat_bone_staff"))
            assertEquals(0, deps.charges.getCharges(player.inv[SLOT]!!, ETHER_VAROBJ))
        }

    private companion object {
        const val SLOT = 0
        const val AYAK_CHARGES = "varobj.charges_65535"
        const val AYAK_RUNE_MODE = "varobj.ayak_rune_charges"
        const val ETHER_VAROBJ = "varobj.tumeken_charges"
    }
}
