package org.rsmod.content.bosses.doom

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeldU
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class AvernicTreadsUpgrade @Inject constructor(private val objRepo: ObjRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (boots in Boots.entries) {
            for (state in STATES) {
                if (boots in state.boots) {
                    continue
                }
                onOpHeldU(state.obj, boots.obj) { upgrade(it.firstSlot, state, boots) }
            }
        }
        for (state in STATES) {
            if (state.boots.isNotEmpty()) {
                onOpHeld3(state.obj) { dismantle(it.inventory, it.slot, state) }
            }
        }
    }

    private fun ProtectedAccess.upgrade(treadsSlot: Int, state: State, boots: Boots) {
        if (stat("stat.magic") < MAGIC_LEVEL || stat("stat.runecrafting") < RC_LEVEL) {
            mes("You need level $MAGIC_LEVEL Magic and $RC_LEVEL Runecrafting to do this.")
            return
        }
        if (inv[treadsSlot]?.isType(state.obj) != true) {
            return
        }
        if (invTotal(inv, TEAR) < TEARS_PER_PAIR) {
            mes("You need $TEARS_PER_PAIR demon tears to add the ${boots.display} boots.")
            return
        }
        val upgraded = STATES.first { it.boots == state.boots + boots }
        if (invDel(inv, boots.obj, 1).failure) {
            return
        }
        if (invDel(inv, TEAR, TEARS_PER_PAIR).failure) {
            invAddOrDrop(objRepo, boots.obj, 1)
            return
        }
        if (invDel(inv, state.obj, 1, slot = treadsSlot).failure) {
            invAddOrDrop(objRepo, boots.obj, 1)
            invAddOrDrop(objRepo, TEAR, TEARS_PER_PAIR)
            return
        }
        invAddOrDrop(objRepo, upgraded.obj, 1)
        statAdvance("stat.magic", XP)
        statAdvance("stat.runecrafting", XP)
        mes("You add the ${boots.display} boots to the Avernic treads.")
    }

    private suspend fun ProtectedAccess.dismantle(
        inventory: Inventory,
        invSlot: Int,
        state: State,
    ) {
        if (inventory[invSlot]?.isType(state.obj) != true) {
            return
        }
        val confirmed =
            choice2(
                "Yes.",
                true,
                "No.",
                false,
                title = "Dismantle the treads? The demon tears will be lost.",
            )
        if (!confirmed || inventory[invSlot]?.isType(state.obj) != true) {
            return
        }
        if (inv.freeSpace() < state.boots.size) {
            mes("You need ${state.boots.size} free inventory slots to do this.")
            return
        }
        if (invDel(inventory, state.obj, 1, slot = invSlot).failure) {
            return
        }
        invAddOrDrop(objRepo, BASE, 1)
        for (boots in state.boots) {
            invAddOrDrop(objRepo, boots.obj, 1)
        }
        mes("You dismantle the Avernic treads.")
    }

    private enum class Boots(val obj: String, val display: String) {
        PRIMORDIAL("obj.primordial_boots", "primordial"),
        PEGASIAN("obj.pegasian_boots", "pegasian"),
        ETERNAL("obj.eternal_boots", "eternal"),
    }

    private class State(val obj: String, val boots: Set<Boots>)

    private companion object {
        const val BASE = "obj.avernic_treads"
        const val TEAR = "obj.demon_tear"
        const val TEARS_PER_PAIR = 4_000
        const val MAGIC_LEVEL = 80
        const val RC_LEVEL = 60
        const val XP = 200.0

        val STATES =
            listOf(
                State(BASE, emptySet()),
                State("obj.avernic_treads_melee", setOf(Boots.PRIMORDIAL)),
                State("obj.avernic_treads_ranged", setOf(Boots.PEGASIAN)),
                State("obj.avernic_treads_magic", setOf(Boots.ETERNAL)),
                State("obj.avernic_treads_melee_ranged", setOf(Boots.PRIMORDIAL, Boots.PEGASIAN)),
                State("obj.avernic_treads_melee_magic", setOf(Boots.PRIMORDIAL, Boots.ETERNAL)),
                State("obj.avernic_treads_ranged_magic", setOf(Boots.PEGASIAN, Boots.ETERNAL)),
                State(
                    "obj.avernic_treads_max",
                    setOf(Boots.PRIMORDIAL, Boots.PEGASIAN, Boots.ETERNAL),
                ),
            )
    }
}
