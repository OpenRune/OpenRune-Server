package org.rsmod.content.other.spawn

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.or2.central.account.Rights
import jakarta.inject.Inject
import java.util.WeakHashMap
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.ifOpenMainModal
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private const val INTERFACE = "interface.spawn_menu"

private const val MAX_RESULTS = 60
private const val COMPONENTS_PER_CARD = 4

private const val QTY_BUTTON_COUNT = 4
private const val QTY_CUSTOM_INDEX = 3
private val QTY_PRESETS = intArrayOf(1, 100, 1000)

private class SpawnState {
    var quantity: Int = 1
    var note: Boolean = false
    var bank: Boolean = false
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

        onIfClose(INTERFACE) {
            states.remove(player)
            ClientScripts.chatDefaultRestoreInput(player)
        }

        for (i in 0 until QTY_BUTTON_COUNT) {
            onIfModalButton("component.spawn_menu:qtybtn$i") {
                if (isAdmin()) selectQuantity(i)
            }
        }

        onIfModalButton("component.spawn_menu:notebtn") { if (isAdmin()) toggleNote() }

        onIfModalButton("component.spawn_menu:bankbtn") { if (isAdmin()) toggleBank() }

        onIfModalButton("component.spawn_menu:grid") { if (isAdmin()) spawn(it.obj) }
    }

    private fun ProtectedAccess.isAdmin(): Boolean {
        if (player.modLevel.isAtLeast(Rights.ADMINISTRATOR)) {
            return true
        }
        ifClose()
        return false
    }

    private fun Cheat.openMenu() {
        protectedAccess.launch(player) { open() }
    }

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
        if (!player.modLevel.isAtLeast(Rights.ADMINISTRATOR)) {
            return
        }
        val state = state(player)
        ifOpenMainModal(INTERFACE)
        ifSetEvents(
            "component.spawn_menu:grid",
            0 until MAX_RESULTS * COMPONENTS_PER_CARD,
            IfEvent.Op1,
        )
        renderButtons(state)
    }

    private suspend fun ProtectedAccess.selectQuantity(index: Int) {
        val state = state(player)
        state.quantity =
            if (index == QTY_CUSTOM_INDEX) {
                countDialog("Enter spawn quantity:").coerceAtLeast(1)
            } else {
                QTY_PRESETS[index]
            }
        renderButtons(state)
    }

    private fun ProtectedAccess.toggleNote() {
        val state = state(player)
        state.note = !state.note
        renderButtons(state)
    }

    private fun ProtectedAccess.toggleBank() {
        val state = state(player)
        state.bank = !state.bank
        renderButtons(state)
    }

    private fun ProtectedAccess.spawn(clicked: ItemServerType?) {
        val item = clicked ?: return
        val state = state(player)

        var toSpawn =
            if (state.note) {
                if (!item.canCert) {
                    player.mes("${item.name} cannot be noted.")
                    return
                }
                noted(item)
                    ?: run {
                        player.mes("${item.name} cannot be noted.")
                        return
                    }
            } else {
                item
            }

        if (!state.bank && !toSpawn.isStackable && state.quantity > player.inv.freeSpace()) {
            noted(toSpawn)?.let { toSpawn = it }
        }

        val target = if (state.bank) player.invMap.getOrPut("inv.bank") else player.inv
        val destination = if (state.bank) "bank" else "inventory"
        val spawned = player.invAdd(target, toSpawn.id, state.quantity, strict = false)
        val completed = spawned.completed()
        if (completed <= 0) {
            player.mes("You don't have enough $destination space for ${toSpawn.name}.")
            return
        }
        player.mes("Spawned ${toSpawn.name} x $completed to your $destination.")
    }

    private fun noted(item: ItemServerType): ItemServerType? =
        if (item.canCert) ServerCacheManager.getItem(item.certlink) else null

    private fun ProtectedAccess.renderButtons(state: SpawnState) {
        val customActive = state.quantity !in QTY_PRESETS
        val selected =
            if (customActive) QTY_CUSTOM_INDEX else QTY_PRESETS.indexOf(state.quantity)
        val customLabel = if (customActive) state.quantity.toString() else "X"
        val script = "clientscript.spawn_menu_buttons".asRSCM(RSCMType.CLIENTSCRIPT)
        player.runClientScript(
            script,
            selected,
            customLabel,
            if (state.note) 1 else 0,
            if (state.bank) 1 else 0,
        )
    }
}
