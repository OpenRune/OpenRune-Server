package org.rsmod.content.areas.misc.deathsoffice

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.player.ui.ifOpenMainModal
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.other.gravestones.deathOffice
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

/* Obj transaction system is not thread-safe. */
@Execution(ExecutionMode.SAME_THREAD)
class DeathsOfficeTest {
    @Test
    fun GameTestState.`selecting a free obj takes all of it`() =
        runOfficeTest {
            player.deathOffice[0] = InvObj(cheap, 1)
            player.deathOffice[1] = InvObj(stack, 40)
            openOffice()

            player.ifButton(component(ITEMS), comsub = 1)
            advance()

            assertEquals(40, player.inv.count(stack.internalName))
            assertEquals(0, player.deathOffice.count(stack.internalName))
            assertEquals(1, player.deathOffice.count(cheap.internalName))
        }

    @Test
    fun GameTestState.`a paid obj is selected, then taken for 5 percent`() =
        runOfficeTest {
            player.deathOffice[0] = InvObj(pricey, 1)
            val fee = pricey.cost * 5 / 100
            player.inv[27] = InvObj("obj.coins", fee + 7)
            openOffice()

            player.ifButton(component(ITEMS), comsub = 0)
            advance()
            assertEquals(1, player.deathOffice.count(pricey.internalName))
            assertEquals(0, player.vars["varp.if2"])

            player.ifButton(component(TAKE_1))
            advance()

            assertEquals(1, player.inv.count(pricey.internalName))
            assertEquals(0, player.deathOffice.count(pricey.internalName))
            assertEquals(7, player.inv.count("obj.coins"))
        }

    @Test
    fun GameTestState.`a paid obj stays without the coins`() =
        runOfficeTest {
            player.deathOffice[0] = InvObj(pricey, 1)
            openOffice()

            player.ifButton(component(ITEMS), comsub = 0)
            advance()
            player.ifButton(component(TAKE_1))
            advance()

            assertEquals(0, player.inv.count(pricey.internalName))
            assertEquals(1, player.deathOffice.count(pricey.internalName))
        }

    @Test
    fun GameTestState.`take-all with a full inv moves and loses nothing`() =
        runOfficeTest {
            player.deathOffice[0] = InvObj(cheap, 5)
            for (slot in 0 until player.inv.size - 2) {
                player.inv[slot] = InvObj("obj.coins", 1).takeIf { slot == 0 } ?: InvObj(other, 1)
            }
            openOffice()

            player.ifButton(component(TAKE_EVERYTHING))
            advance()

            assertEquals(2, player.inv.count(cheap.internalName))
            assertEquals(3, player.deathOffice.count(cheap.internalName))
        }

    @Test
    fun GameTestState.`x takes the entered count and charges for each`() =
        runOfficeTest {
            player.deathOffice[0] = InvObj(pricey, 3)
            val fee = pricey.cost * 5 / 100
            player.inv[27] = InvObj("obj.coins", fee * 3)
            openOffice()

            player.ifButton(component(ITEMS), comsub = 0)
            advance()
            player.ifButton(component(TAKE_X))
            advance()
            player.resumeCountDialog(1)
            advance()

            assertEquals(1, player.inv.count(pricey.internalName))
            assertEquals(2, player.deathOffice.count(pricey.internalName))
            assertEquals(fee * 2, player.inv.count("obj.coins"))
        }

    @Test
    fun GameTestState.`take-all asks before taking paid objs`() =
        runOfficeTest {
            player.deathOffice[0] = InvObj(cheap, 1)
            player.deathOffice[1] = InvObj(pricey, 1)
            player.inv[27] = InvObj("obj.coins", pricey.cost)
            openOffice()

            player.ifButton(component(TAKE_EVERYTHING))
            advance()

            assertEquals(1, player.deathOffice.count(cheap.internalName))
            assertEquals(1, player.deathOffice.count(pricey.internalName))
            assertEquals(pricey.cost, player.inv.count("obj.coins"))
        }

    private fun GameTestState.runOfficeTest(body: GameTestScope.() -> Unit) =
        runGameTest(DeathsOfficeScript::class) {
            player.placeAt(CoordGrid(3171, 5726))
            player.clearAllInvs()
            player.deathOffice.fillNulls()
            body()
        }

    private fun GameTestScope.openOffice() {
        player.ifOpenMainModal("interface.death_office", eventBus)
        player.ifSetEvents(ITEMS, 0 until 120, IfEvent.Op1, IfEvent.Op10)
    }

    private val GameTestScope.cheap: ItemServerType
        get() = firstObjType { !it.isStackable && it.cost in 1 until 1000 && it.tradeable }

    private val GameTestScope.other: ItemServerType
        get() = firstObjType { !it.isStackable && it.cost in 1000 until 2000 && it.tradeable }

    private val GameTestScope.pricey: ItemServerType
        get() = firstObjType { !it.isStackable && it.cost in 200_000 until 900_000 && it.tradeable }

    private val GameTestScope.stack: ItemServerType
        get() = firstObjType { it.isStackable && it.cost in 1 until 100 && it.tradeable }

    private fun component(name: String) =
        ServerCacheManager.fromComponent(name.asRSCM(RSCMType.COMPONENT))

    private companion object {
        const val ITEMS = "component.death_office:items"
        const val TAKE_1 = "component.death_office:1"
        const val TAKE_X = "component.death_office:x"
        const val TAKE_EVERYTHING = "component.death_office:takeall"
    }
}
