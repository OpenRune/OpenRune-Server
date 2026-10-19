package org.rsmod.content.skills.magic.runepouch

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.util.UncheckedType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj

@OptIn(UncheckedType::class)
@Execution(ExecutionMode.SAME_THREAD)
class RunePouchScriptTest {
    @Test
    fun `open and empty are the pouches' first and fourth inventory ops`() {
        for (pouch in RunePouch.pouches) {
            val type = checkNotNull(ServerCacheManager.getItem(pouch.asRSCM(RSCMType.OBJ)))
            assertEquals(0, type.interfaceOptions.indexOf("Open"), pouch)
            assertEquals(3, type.interfaceOptions.indexOf("Empty"), pouch)
        }
    }

    @Test
    fun `the rune varbits use the client's compact ids`() {
        assertEquals(1, RunePouch.compactId("obj.airrune"))
        assertEquals(4, RunePouch.compactId("obj.firerune"))
    }

    @Test
    fun GameTestState.`store and withdraw move runes without losing any`() =
        runInjectedGameTest(RunePouchScript::class, null, RunePouchScript::class) { script ->
            player.clearInv()
            player.inv[0] = InvObj("obj.bh_rune_pouch".asRSCM(RSCMType.OBJ), 1)
            player.inv[1] = InvObj("obj.airrune".asRSCM(RSCMType.OBJ), 100)
            player.inv[2] = InvObj("obj.firerune".asRSCM(RSCMType.OBJ), 20)
            with(script) {
                player.withProtectedAccess {
                    assertEquals(30, store("obj.airrune", 30))
                    assertEquals(20, store("obj.firerune", Int.MAX_VALUE))
                }
            }
            assertEquals(70, player.inv.count("obj.airrune"))
            assertEquals(0, player.inv.count("obj.firerune"))
            assertEquals("obj.airrune" to 30, RunePouch.slot(player, 0))
            assertEquals("obj.firerune" to 20, RunePouch.slot(player, 1))
            assertEquals(1, player.vars["varbit.rune_pouch_type_1"])
            assertEquals(30, player.vars["varbit.rune_pouch_quantity_1"])

            with(script) { player.withProtectedAccess { assertEquals(5, withdraw(0, 5)) } }
            assertEquals(75, player.inv.count("obj.airrune"))
            assertEquals(25, RunePouch.storedAmount(player, "obj.airrune"))

            with(script) { player.withProtectedAccess { emptyPouch() } }
            assertEquals(100, player.inv.count("obj.airrune"))
            assertEquals(20, player.inv.count("obj.firerune"))
            assertNull(RunePouch.slot(player, 0))
            assertNull(RunePouch.slot(player, 1))
            assertEquals(0, player.vars["varbit.rune_pouch_type_1"])
        }

    @Test
    fun GameTestState.`a regular pouch holds three rune types up to 16000 each`() =
        runInjectedGameTest(RunePouchScript::class, null, RunePouchScript::class) { script ->
            player.clearInv()
            player.inv[0] = InvObj("obj.bh_rune_pouch".asRSCM(RSCMType.OBJ), 1)
            val runes = listOf("obj.airrune", "obj.waterrune", "obj.earthrune", "obj.firerune")
            runes.forEachIndexed { i, rune ->
                player.inv[i + 1] = InvObj(rune.asRSCM(RSCMType.OBJ), 20_000)
            }
            with(script) {
                player.withProtectedAccess {
                    assertEquals(16_000, store("obj.airrune", Int.MAX_VALUE))
                    assertEquals(0, store("obj.airrune", Int.MAX_VALUE))
                    assertEquals(10, store("obj.waterrune", 10))
                    assertEquals(10, store("obj.earthrune", 10))
                    assertEquals(0, store("obj.firerune", 10))
                }
            }
            assertEquals(4_000, player.inv.count("obj.airrune"))
            assertEquals(20_000, player.inv.count("obj.firerune"))
            assertEquals(16_000, RunePouch.storedAmount(player, "obj.airrune"))
        }

    @Test
    fun GameTestState.`withdrawing into a full inventory keeps the runes in the pouch`() =
        runInjectedGameTest(RunePouchScript::class, null, RunePouchScript::class) { script ->
            player.clearInv()
            player.inv[0] = InvObj("obj.divine_rune_pouch".asRSCM(RSCMType.OBJ), 1)
            player.inv[1] = InvObj("obj.lawrune".asRSCM(RSCMType.OBJ), 50)
            with(script) { player.withProtectedAccess { store("obj.lawrune", 50) } }
            for (slot in 1 until player.inv.size) {
                player.inv[slot] = InvObj("obj.bronze_dagger".asRSCM(RSCMType.OBJ), 1)
            }
            with(script) { player.withProtectedAccess { assertEquals(0, withdraw(0, 50)) } }
            assertEquals(50, RunePouch.storedAmount(player, "obj.lawrune"))
            assertEquals(0, player.inv.count("obj.lawrune"))
        }
}
