package org.rsmod.content.areas.misc.deathsoffice

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.player.ui.ifOpenMainSidePair
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.other.gravestones.deathCoffer
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

/* Obj transaction system is not thread-safe. */
@Execution(ExecutionMode.SAME_THREAD)
class DeathsCofferTest {
    @Test
    fun GameTestState.`sacrificing credits 105 percent and removes the objs`() =
        runCofferTest {
            player.inv[0] = InvObj(valuable, 1)
            player.inv[1] = InvObj(valuable, 1)
            player.inv[2] = InvObj(valuable, 1)
            openCoffer()

            player.ifButton(component(SIDE_ITEMS), comsub = 1)
            player.ifButton(component(QUANTITY_5))
            advance()
            player.ifButton(component(CONFIRM))
            advance()

            assertEquals(0, player.inv.count(valuable.internalName))
            assertEquals(valuable.cost.toLong() * 105 / 100 * 3, player.deathCoffer.toLong())
            assertEquals(-1, player.vars["varp.if2"])
        }

    @Test
    fun GameTestState.`an obj under 10,000 is refused and kept`() =
        runCofferTest {
            player.inv[0] = InvObj(cheap, 1)
            openCoffer()

            player.ifButton(component(SIDE_ITEMS), comsub = 0)
            advance()
            player.ifButton(component(CONFIRM))
            advance()

            assertEquals(1, player.inv.count(cheap.internalName))
            assertEquals(0, player.deathCoffer)
        }

    @Test
    fun GameTestState.`an Old school bond is refused and kept`() =
        runCofferTest {
            player.inv[0] = InvObj(untradeable, 1)
            openCoffer()

            player.ifButton(component(SIDE_ITEMS), comsub = 0)
            advance()
            player.ifButton(component(CONFIRM))
            advance()

            assertEquals(1, player.inv.count(untradeable.internalName))
            assertEquals(0, player.deathCoffer)
        }

    @Test
    fun GameTestState.`an untradeable obj is refused and kept`() =
        runCofferTest {
            val staff = checkNotNull(ServerCacheManager.getItem("obj.ibanstaff".asRSCM(RSCMType.OBJ)))
            player.inv[0] = InvObj(staff, 1)
            openCoffer()

            player.ifButton(component(SIDE_ITEMS), comsub = 0)
            advance()
            player.ifButton(component(CONFIRM))
            advance()

            assertEquals(1, player.inv.count(staff.internalName))
            assertEquals(0, player.deathCoffer)
        }

    private fun GameTestState.runCofferTest(body: GameTestScope.() -> Unit) =
        runGameTest(DeathsCofferScript::class) {
            player.placeAt(CoordGrid(3179, 5728))
            player.clearAllInvs()
            player.deathCoffer = 0
            body()
        }

    private fun GameTestScope.openCoffer() {
        player.ifOpenMainSidePair("interface.death_coffer", "interface.death_coffer_side", -1, -1, eventBus)
        player.ifSetEvents(SIDE_ITEMS, 0 until 28, IfEvent.Op1, IfEvent.Op10)
        VarPlayerIntMapSetter.set(player, "varp.if2", -1)
    }

    private val GameTestScope.valuable: ItemServerType
        get() = firstObjType { !it.isStackable && it.tradeable && it.cost in 20_000 until 50_000 }

    private val GameTestScope.cheap: ItemServerType
        get() = firstObjType { !it.isStackable && it.tradeable && it.cost in 100 until 5_000 }

    private val untradeable: ItemServerType
        get() = checkNotNull(ServerCacheManager.getItem("obj.osrs_bond".asRSCM(RSCMType.OBJ)))

    private fun component(name: String) =
        ServerCacheManager.fromComponent(name.asRSCM(RSCMType.COMPONENT))

    private companion object {
        const val SIDE_ITEMS = "component.death_coffer_side:items"
        const val QUANTITY_5 = "component.death_coffer:5"
        const val CONFIRM = "component.death_coffer:confirm"
    }
}
