package org.rsmod.content.areas.city.falador.npcs

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfButtonOp
import dev.openrune.types.util.UncheckedType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.enumVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.interfaces.bank.BankTab
import org.rsmod.content.interfaces.bank.bankCapacity
import org.rsmod.content.interfaces.bank.openBank
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.placeholder

private var Player.bankTabSize1 by intVarBit("varbit.bank_tab_1")
private var Player.bankTabSizeMain by intVarBit("varbit.bank_tab_main")
private var Player.currentBankTab by enumVarBit<BankTab>("varbit.bank_currenttab")

@OptIn(UncheckedType::class)
@Execution(ExecutionMode.SAME_THREAD)
class WysonNestBoxBankTest {
    @Test
    fun GameTestState.`bank extract deposits every nest and removes the box`() =
        bankTest {
            val bank = bankInventory()
            player.inv[0] = box(5)
            extract(0)
            advance()

            assertEquals(5, bank.count(NEST))
            assertEquals(InvObj(NEST.asRSCM(RSCMType.OBJ), 5), bank[0])
            assertNull(player.inv[0])
            assertEquals(1, player.bankTabSizeMain)
        }

    @Test
    fun GameTestState.`bank extract stores all nests in one bank slot`() =
        bankTest {
            val bank = bankInventory()
            player.bankCapacity = 3
            player.inv[0] = box(5)
            extract(0)
            advance()

            assertEquals(5, bank.count(NEST))
            assertEquals(InvObj(NEST.asRSCM(RSCMType.OBJ), 5), bank[0])
            assertNull(player.inv[0])
            assertEquals(1, player.bankTabSizeMain)
        }

    @Test
    fun GameTestState.`bank extract merges into an existing nest slot`() =
        bankTest {
            val bank = bankInventory()
            player.bankCapacity = 1
            bank[0] = InvObj(NEST.asRSCM(RSCMType.OBJ), 2)
            player.bankTabSizeMain = 1
            player.inv[0] = box(3)
            extract(0)
            advance()

            assertEquals(InvObj(NEST.asRSCM(RSCMType.OBJ), 5), bank[0])
            assertEquals(1, player.bankTabSizeMain)
            assertNull(player.inv[0])
        }

    @Test
    fun GameTestState.`bank extract keeps every nest when the bank is full`() =
        bankTest {
            val bank = bankInventory()
            player.bankCapacity = 1
            bank[0] = InvObj("obj.beer", 1)
            player.bankTabSizeMain = 1
            player.inv[0] = box(5)
            extract(0)
            advance()

            assertEquals(0, bank.count(NEST))
            assertEquals(1, player.inv[0]!!.count)
            assertEquals(5, player.inv[0]!!.wysonNestCount())
        }

    @Test
    fun GameTestState.`multiple nest boxes stay separate in the bank`() =
        bankTest {
            val bank = bankInventory()
            player.inv[0] = box(3)
            player.inv[1] = box(4)
            depositInventory()
            advance()

            assertEquals(InvObj(WysonNest.SEEDS.boxId, 1, 3), bank[0])
            assertEquals(InvObj(WysonNest.SEEDS.boxId, 1, 4), bank[1])
            assertEquals(2, player.bankTabSizeMain)
            assertEquals(0, player.inv.occupiedSpace())
        }

    @Test
    fun GameTestState.`nest boxes can be banked and withdrawn with their contents`() =
        bankTest {
            val bank = bankInventory()
            player.inv[0] = box(4)
            deposit(0)
            advance()

            assertEquals(InvObj(WysonNest.SEEDS.boxId, 1, 4), bank[0])
            assertNull(player.inv[0])
            assertEquals(1, player.bankTabSizeMain)

            withdraw(0)
            advance()

            assertEquals(InvObj(WysonNest.SEEDS.boxId, 1, 4), player.inv[0])
            assertNull(bank[0])
            assertEquals(0, player.bankTabSizeMain)
        }

    @Test
    fun GameTestState.`bank extract keeps the box when the nest stack is full`() =
        bankTest {
            val bank = bankInventory()
            player.bankCapacity = 1
            bank[0] = InvObj(NEST.asRSCM(RSCMType.OBJ), Int.MAX_VALUE)
            player.bankTabSizeMain = 1
            player.inv[0] = box(1)
            extract(0)
            advance()

            assertEquals(InvObj(NEST.asRSCM(RSCMType.OBJ), Int.MAX_VALUE), bank[0])
            assertEquals(1, player.inv[0]!!.count)
            assertEquals(1, player.inv[0]!!.wysonNestCount())
        }

    @Test
    fun GameTestState.`bank extract fills a nest placeholder`() =
        bankTest {
            val bank = bankInventory()
            player.bankCapacity = 1
            val nestType = checkNotNull(ServerCacheManager.getItem(NEST.asRSCM(RSCMType.OBJ)))
            check(nestType.hasPlaceholder)
            bank[0] = InvObj(placeholder(nestType), 1)
            player.bankTabSizeMain = 1
            player.inv[0] = box(3)
            extract(0)
            advance()

            assertEquals(InvObj(NEST.asRSCM(RSCMType.OBJ), 3), bank[0])
            assertEquals(1, player.bankTabSizeMain)
            assertNull(player.inv[0])
        }

    @Test
    fun GameTestState.`bank extract uses the selected tab and shifts later tabs`() =
        bankTest {
            val bank = bankInventory()
            player.bankCapacity = 10
            bank[0] = InvObj("obj.beer", 1)
            bank[1] = InvObj("obj.beer", 1)
            bank[2] = InvObj("obj.shark", 1)
            player.bankTabSize1 = 2
            player.bankTabSizeMain = 1
            player.currentBankTab = BankTab.Tab1
            player.inv[0] = box(3)
            extract(0)
            advance()

            assertEquals(3, bank.count(NEST))
            assertEquals(InvObj("obj.beer", 1), bank[0])
            assertEquals(InvObj("obj.beer", 1), bank[1])
            assertEquals(InvObj(NEST.asRSCM(RSCMType.OBJ), 3), bank[2])
            assertEquals(InvObj("obj.shark", 1), bank[3])
            assertEquals(3, player.bankTabSize1)
            assertEquals(1, player.bankTabSizeMain)
        }

    private fun GameTestState.bankTest(body: GameTestScope.() -> Unit) =
        runGameTest(BankInvScript::class, WysonNestBoxScript::class) {
            player.clearAllInvs()
            body()
        }

    private fun GameTestScope.bankInventory(): Inventory =
        player.invMap.getOrPut("inv.bank")

    private fun GameTestScope.box(count: Int): InvObj = WysonNest.SEEDS.createBox(count)

    private fun GameTestScope.extract(slot: Int) {
        openBankWithEvents()
        val component =
            ServerCacheManager.fromComponent("component.bankside:items".asRSCM(RSCMType.COMPONENT))
        player.ifButton(component, comsub = slot, op = IfButtonOp.Op9)
    }

    private fun GameTestScope.deposit(slot: Int) {
        openBankWithEvents()
        val component =
            ServerCacheManager.fromComponent("component.bankside:items".asRSCM(RSCMType.COMPONENT))
        player.ifButton(component, comsub = slot, op = IfButtonOp.Op8)
    }

    private fun GameTestScope.withdraw(slot: Int) {
        val component =
            ServerCacheManager.fromComponent("component.bankmain:items".asRSCM(RSCMType.COMPONENT))
        player.ifButton(component, comsub = slot, op = IfButtonOp.Op2)
    }

    private fun GameTestScope.depositInventory() {
        openBankWithEvents()
        val component =
            ServerCacheManager.fromComponent("component.bankmain:depositinv".asRSCM(RSCMType.COMPONENT))
        player.ifButton(component)
    }

    private fun GameTestScope.openBankWithEvents() {
        player.openBank(eventBus)
        player.ifSetEvents(
            "component.bankside:items",
            player.inv.indices,
            IfEvent.Op8,
            IfEvent.Op9,
            IfEvent.Depth1,
        )
        player.ifSetEvents(
            "component.bankmain:items",
            player.inv.indices,
            IfEvent.Op2,
            IfEvent.Op10,
            IfEvent.Depth2,
        )
        player.ifSetEvents("component.bankmain:depositinv", 0..0, IfEvent.Op1)
    }

    private companion object {
        const val NEST = "obj.bird_nest_decentseeds_jan2019"
    }
}
