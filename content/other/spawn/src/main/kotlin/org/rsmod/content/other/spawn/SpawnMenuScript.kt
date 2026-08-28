package org.rsmod.content.other.spawn

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.or2.central.account.Rights
import jakarta.inject.Inject
import java.awt.Color
import java.util.WeakHashMap
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.ifOpenMainModal
import org.rsmod.api.player.ui.ifSetObj
import org.rsmod.api.player.ui.setColour
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * `::spawn` - admin item search-and-spawn grid. Set a quantity mode, search, then click as many
 * result cards as you like.
 *
 * The interface is defined in the `spawn-pack` module (`SpawnInterface.kt`) - component names
 * here only resolve because of the `[gamevals.component]` entries that mirror its declaration
 * order.
 */
private const val INTERFACE = "interface.spawn_menu"

/** Must stay in sync with `SpawnInterface.kt` (CARD_COLS * TOTAL_ROWS). */
private const val SLOT_COUNT = 100

/** Must stay in sync with `SpawnInterface.kt`'s CARD_COLS/CARD_PITCH_Y. */
private const val CARD_COLS = 2
private const val CARD_PITCH_Y = 48

private const val QTY_BUTTON_COUNT = 4
private const val QTY_CUSTOM_INDEX = 3
private val QTY_PRESETS = intArrayOf(1, 100, 1000)

/** Must stay in sync with `SpawnInterface.kt`'s COLOUR_CARD_BORDER/COLOUR_CARD_FILL border. */
private val COLOUR_BUTTON_NORMAL = Color(0x5a4a2f)
private val COLOUR_BUTTON_ACTIVE = Color(0xff981f)

private class SpawnState {
    var quantity: Int = 1
    var note: Boolean = false
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

        onIfModalButton("component.spawn_menu:searchlbl") { runSearch() }

        for (i in 0 until QTY_BUTTON_COUNT) {
            onIfModalButton("component.spawn_menu:qtylbl$i") { selectQuantity(i) }
        }

        onIfModalButton("component.spawn_menu:notelbl") { toggleNote() }

        for (i in 0 until SLOT_COUNT) {
            onIfModalButton("component.spawn_menu:slot$i") { spawnSlot(i) }
            onIfModalButton("component.spawn_menu:slotname$i") { spawnSlot(i) }
        }
    }

    private fun Cheat.openMenu() {
        protectedAccess.launch(player) { open() }
    }

    /** Dumps the packed component list of `interface.spawn_menu` and `mainmodal`'s real
     * width/height, for verifying `[gamevals.component]` ids and frame sizing. */
    private fun Cheat.dumpInterface() {
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
        renderNoteToggle(state)
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
                .toList()
                // Several ids can share an exact display name (e.g. old "Air rune" duplicates) -
                // collapse to one representative, preferring the GE-tradeable version.
                .groupBy { it.name.lowercase() }
                .values
                .map { dupes ->
                    dupes.firstOrNull { it.stockmarket }
                        ?: dupes.firstOrNull { it.tradeable }
                        ?: dupes.minBy { it.id }
                }
                // Shortest name first, so an exact match outranks its variants.
                .sortedWith(compareBy({ it.name.length }, { it.name }, { it.id }))
                .take(SLOT_COUNT)

        state.slotItems.fill(null)
        for ((index, item) in matches.withIndex()) {
            state.slotItems[index] = item
        }
        renderSlots(state)
        renderScrollSize(matches.size)

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
        renderSlots(state)
    }

    private fun ProtectedAccess.toggleNote() {
        val state = state(player)
        state.note = !state.note
        renderNoteToggle(state)
    }

    private fun ProtectedAccess.spawnSlot(index: Int) {
        val state = state(player)
        val item = state.slotItems[index] ?: return

        // Note mode spawns the noted counterpart instead - not every item has one, and if it
        // doesn't, say so rather than silently falling back to the unnoted item.
        val toSpawn =
            if (state.note) {
                if (!item.canCert) {
                    player.mes("${item.name} cannot be noted.")
                    return
                }
                ServerCacheManager.getItem(item.certlink)
                    ?: run {
                        player.mes("${item.name} cannot be noted.")
                        return
                    }
            } else {
                item
            }

        val spawned = player.invAdd(player.inv, toSpawn.id, state.quantity, strict = false)
        val completed = spawned.completed()
        if (completed <= 0) {
            player.mes("You don't have enough inventory space for ${toSpawn.name}.")
            return
        }
        player.mes("Spawned ${toSpawn.name} x $completed.")
    }

    private fun ProtectedAccess.renderSlots(state: SpawnState) {
        for (i in 0 until SLOT_COUNT) {
            val iconTarget = "component.spawn_menu:slot$i"
            val nameTarget = "component.spawn_menu:slotname$i"
            val borderTarget = "component.spawn_menu:cardborder$i"
            val bgTarget = "component.spawn_menu:cardbg$i"
            val item = state.slotItems[i]
            if (item == null) {
                ifSetHide(borderTarget, true)
                ifSetHide(bgTarget, true)
                ifSetHide(iconTarget, true)
                ifSetHide(nameTarget, true)
            } else {
                ifSetHide(borderTarget, false)
                ifSetHide(bgTarget, false)
                ifSetHide(iconTarget, false)
                ifSetHide(nameTarget, false)
                player.ifSetObj(iconTarget, InvObj(item), state.quantity)
                ifSetText(nameTarget, item.name)
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
            ifSetText("component.spawn_menu:qtylbl$i", label)
            val colour = if (active) COLOUR_BUTTON_ACTIVE else COLOUR_BUTTON_NORMAL
            player.setColour("component.spawn_menu:qtyborder$i", colour)
        }
    }

    private fun ProtectedAccess.renderScrollSize(resultCount: Int) {
        val rows = (resultCount + CARD_COLS - 1) / CARD_COLS // ceiling division
        val height = rows * CARD_PITCH_Y
        val setScrollSize = "clientscript.spawn_menu_set_scrollsize".asRSCM(RSCMType.CLIENTSCRIPT)
        val grid = "component.spawn_menu:grid".asRSCM(RSCMType.COMPONENT)
        val scrollbar = "component.spawn_menu:scrollbar".asRSCM(RSCMType.COMPONENT)
        player.runClientScript(setScrollSize, grid, scrollbar, height)
    }

    private fun ProtectedAccess.renderNoteToggle(state: SpawnState) {
        val colour = if (state.note) COLOUR_BUTTON_ACTIVE else COLOUR_BUTTON_NORMAL
        player.setColour("component.spawn_menu:noteborder", colour)
    }

    private fun ProtectedAccess.setStatus(text: String) {
        ifSetText("component.spawn_menu:status", text)
    }
}
