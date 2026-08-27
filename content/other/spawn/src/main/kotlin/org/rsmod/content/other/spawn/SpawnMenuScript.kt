package org.rsmod.content.other.spawn

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.or2.central.account.Rights
import jakarta.inject.Inject
import java.util.WeakHashMap
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.ifOpenMainModal
import org.rsmod.api.player.ui.ifSetObj
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * `::spawn` - admin item search-and-spawn grid.
 *
 * Set a quantity mode once (1 / 100 / 1000 / custom), search once, then click as many result icons
 * as you like: each click spawns that quantity instantly and the interface stays open. This is the
 * point of the whole thing over the old prompt-loop (`::spawnold`), which asked for a quantity
 * again for every single item.
 *
 * The interface itself is defined in the `spawn-pack` module (`SpawnInterface.kt`); read the notes
 * there before changing either file - the component names used here only resolve because of the
 * `[gamevals.component]` entries that mirror that file's declaration order.
 */
private const val INTERFACE = "interface.spawn_menu"

/** Must stay in sync with `SpawnInterface.kt` (COLS * TOTAL_ROWS = 13 * 15). */
private const val SLOT_COUNT = 195

private const val QTY_BUTTON_COUNT = 4
private const val QTY_CUSTOM_INDEX = 3
private val QTY_PRESETS = intArrayOf(1, 100, 1000)

private class SpawnState {
    var quantity: Int = 1
    val slotItems: Array<ItemServerType?> = arrayOfNulls(SLOT_COUNT)
}

class SpawnMenuScript @Inject constructor(private val protectedAccess: ProtectedAccessLauncher) :
    PluginScript() {
    private val states = WeakHashMap<Player, SpawnState>()

    private fun state(player: Player): SpawnState = states.getOrPut(player) { SpawnState() }

    override fun ScriptContext.startup() {
        onCommand("spawn") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Search for and spawn items from a visual grid"
            cheat { openMenu() }
        }

        onCommand("spawndebug") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Dump the actual packed child list of interface.spawn_menu"
            cheat { dumpInterface() }
        }

        onIfClose(INTERFACE) { states.remove(player) }

        // "searchbtn"/"qty$i" were removed - the quantity row and search control are now
        // clickable text directly (matching how the real bank's own quantity row is built, see
        // SpawnInterface.kt's comment), not a separate graphic button sitting behind the label.
        onIfModalButton("component.spawn_menu:searchlbl") { runSearch() }

        for (i in 0 until QTY_BUTTON_COUNT) {
            onIfModalButton("component.spawn_menu:qtylbl$i") { selectQuantity(i) }
        }

        for (i in 0 until SLOT_COUNT) {
            onIfModalButton("component.spawn_menu:slot$i") { spawnSlot(i) }
        }
    }

    private fun Cheat.openMenu() {
        protectedAccess.launch(player) { open() }
    }

    /**
     * `::spawndebug` - dumps the real packed component list of `interface.spawn_menu` straight
     * from the loaded cache. Kept intentionally (not just a one-off): this is the fast, ground-truth
     * way to verify `[gamevals.component]` ids for any custom interface in this codebase, rather
     * than inferring the packed-id scheme from documentation/bytecode and guessing wrong (see
     * `docs/dev-notes.md`'s "Custom from-scratch interfaces" note for the story on why this exists).
     */
    private fun Cheat.dumpInterface() {
        // Real ground truth for the mainmodal viewport size, instead of guessing interface
        // dimensions between "clips" and "fits but feels small" data points. mainmodal itself has
        // real width/height baked into the cache - read them directly rather than assume.
        val mainmodalId = "component.toplevel_osrs_stretch:mainmodal".asRSCM(RSCMType.COMPONENT)
        val mainmodal = ServerCacheManager.fromComponent(mainmodalId)
        println(
            "[spawndebug] component.toplevel_osrs_stretch:mainmodal " +
                "width=${mainmodal.width} height=${mainmodal.height} x=${mainmodal.x} y=${mainmodal.y}",
        )
        player.mes("[spawndebug] mainmodal is ${mainmodal.width}x${mainmodal.height}, see console")

        val id = INTERFACE.asRSCM(RSCMType.INTERFACE)
        val iface = ServerCacheManager.getInterface(id)
        if (iface == null) {
            player.mes("[spawndebug] no interface loaded for id $id")
            return
        }
        player.mes("[spawndebug] ${iface.components.size} components, see server console")
        println("[spawndebug] interface.spawn_menu id=$id, ${iface.components.size} components:")
        for ((key, comp) in iface.components.toSortedMap()) {
            println(
                "[spawndebug]   key=$key internalId=${comp.internalId} " +
                    "name=${comp.internalName} pos=(${comp.x},${comp.y})",
            )
        }
    }

    private fun ProtectedAccess.open() {
        val state = state(player)
        ifOpenMainModal(INTERFACE)
        renderQuantityLabels(state)
        renderSlots(state)
        setStatus("Click Search to begin.")
    }

    private suspend fun ProtectedAccess.runSearch() {
        val state = state(player)
        val query = stringDialog("Search for an item:").trim()
        if (query.isEmpty()) {
            return
        }

        val matches =
            ServerCacheManager.getItemTypes()
                .asSequence()
                .filter { !it.isPlaceholder }
                .filter { it.name.isNotBlank() && !it.name.equals("null", ignoreCase = true) }
                .filter { it.name.contains(query, ignoreCase = true) }
                // Shortest name first, so an exact-ish match ("Rune") outranks its many
                // variants ("Rune platebody (t)") instead of being buried past the slot cap.
                .sortedWith(compareBy({ it.name.length }, { it.name }, { it.id }))
                .take(SLOT_COUNT)
                .toList()

        state.slotItems.fill(null)
        for ((index, item) in matches.withIndex()) {
            state.slotItems[index] = item
        }
        renderSlots(state)

        val status =
            when {
                matches.isEmpty() -> "No results for '$query'."
                matches.size >= SLOT_COUNT -> "First $SLOT_COUNT results for '$query'."
                else -> "${matches.size} result(s) for '$query'."
            }
        setStatus(status)
    }

    private suspend fun ProtectedAccess.selectQuantity(index: Int) {
        val state = state(player)
        state.quantity =
            if (index == QTY_CUSTOM_INDEX) {
                countDialog("Enter spawn quantity:").coerceAtLeast(1)
            } else {
                QTY_PRESETS[index]
            }
        renderQuantityLabels(state)
        // Re-push the icons too: the count badge each slot draws is the spawn quantity.
        renderSlots(state)
    }

    private fun ProtectedAccess.spawnSlot(index: Int) {
        val state = state(player)
        val item = state.slotItems[index] ?: return
        val spawned = player.invAdd(player.inv, item.id, state.quantity, strict = false)
        val completed = spawned.completed()
        if (completed <= 0) {
            player.mes("You don't have enough inventory space for ${item.name}.")
            return
        }
        player.mes("Spawned ${item.name} x $completed.")
    }

    private fun ProtectedAccess.renderSlots(state: SpawnState) {
        val tooltip = "clientscript.spawn_menu_set_tooltip".asRSCM(RSCMType.CLIENTSCRIPT)
        for (i in 0 until SLOT_COUNT) {
            val target = "component.spawn_menu:slot$i"
            val item = state.slotItems[i]
            if (item == null) {
                ifSetHide(target, true)
            } else {
                ifSetHide(target, false)
                player.ifSetObj(target, InvObj(item), state.quantity)
                // Hover text ("Spawn <item name>") isn't something the static DSL can set
                // per-slot - see the cs2 script's own doc comment for why (real op-base text is
                // dynamic, client-side state set on an explicit component, not a compile-time
                // string). One call per populated slot, right after that slot's own ifSetObj.
                val comp = target.asRSCM(RSCMType.COMPONENT)
                player.runClientScript(tooltip, comp, item.name)
            }
        }
    }

    private fun ProtectedAccess.renderQuantityLabels(state: SpawnState) {
        for (i in 0 until QTY_BUTTON_COUNT) {
            val custom = i == QTY_CUSTOM_INDEX
            val active =
                if (custom) state.quantity !in QTY_PRESETS else state.quantity == QTY_PRESETS[i]
            val label =
                when {
                    custom && active -> state.quantity.toString()
                    custom -> "X"
                    else -> QTY_PRESETS[i].toString()
                }
            // Active state is now shown by the red "qtyhl$i" box behind the text (matching real
            // bank quantity buttons), not by recolouring the text itself.
            ifSetText("component.spawn_menu:qtylbl$i", label)
            ifSetHide("component.spawn_menu:qtyhl$i", !active)
        }
    }

    private fun ProtectedAccess.setStatus(text: String) {
        ifSetText("component.spawn_menu:status", text)
    }
}
