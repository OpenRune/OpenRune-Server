package org.rsmod.content.other.spawn

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM
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
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.player.ui.IfScriptArgs
import org.rsmod.api.player.ui.ifOpenMainModal
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onGameStartup
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfScriptTrigger
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private const val INTERFACE = "interface.spawn_menu"
private const val RESULT_INV = "inv.spawn_results"

private const val MAX_RESULTS = 60
private const val COMPONENTS_PER_CARD = 5

private const val LABEL_WIDTH = 32

private const val QTY_BUTTON_COUNT = 4
private const val QTY_CUSTOM_INDEX = 3
private val QTY_PRESETS = intArrayOf(1, 100, 1000)

private class SpawnState {
    var quantity: Int = 1
    var note: Boolean = false
    var bank: Boolean = false

    var query: String = ""
}

private var Player.spawnIncludeNulls by boolVarBit("varbit.spawn_include_nulls")

private object SpawnSearchIndex {
    private class Entry(val name: String, val gameval: String, val item: ItemServerType)

    @Volatile private var entries: List<Entry> = emptyList()

    @Volatile private var stackVariants: Set<Int> = emptySet()

    fun build() {
        val items = ServerCacheManager.getItems().values
        val stackVariants = stackVariantIds(items)
        this.stackVariants = stackVariants
        entries =
            items
                .mapNotNull { item ->
                    if (item.id in stackVariants) {
                        return@mapNotNull null
                    }
                    val gameval = gamevalOf(item)
                    if (!searchable(item, gameval)) {
                        return@mapNotNull null
                    }
                    Entry(displayNameOf(item).lowercase(), gameval.lowercase(), item)
                }
                .sortedWith(compareBy({ labelOf(it).length }, { labelOf(it) }, { it.item.id }))
    }

    private fun stackVariantIds(items: Collection<ItemServerType>): Set<Int> {
        val variants = HashSet<Int>()
        for (item in items) {
            val countObj = item.countObj ?: continue
            for (id in countObj) {
                if (id > 0 && id != item.id) {
                    variants.add(id)
                }
            }
        }
        return variants
    }

    fun search(query: String, limit: Int, includeNulls: Boolean): List<ItemServerType> {
        val exact = ArrayList<ItemServerType>()
        val prefix = ArrayList<ItemServerType>()
        val partial = ArrayList<ItemServerType>()
        for (entry in entries) {
            if (!includeNulls && entry.name.isEmpty()) {
                continue
            }
            when {
                entry.name == query || entry.gameval == query -> exact
                entry.name.startsWith(query) || entry.gameval.startsWith(query) -> prefix
                entry.name.contains(query) || entry.gameval.contains(query) -> partial
                else -> null
            }?.add(entry.item)
        }
        return (exact + prefix + partial).take(limit)
    }

    fun size(): Int = entries.size

    fun gamevalOf(item: ItemServerType): String {
        val mapped = RSCM.getReverseMapping(RSCMType.OBJ, item.id).removePrefix("obj.")
        val unmapped = mapped.isBlank() || mapped == "-1" || mapped.toIntOrNull() != null
        return if (unmapped) "" else mapped
    }

    fun displayNameOf(item: ItemServerType): String =
        if (item.name.isBlank() || item.name.equals("null", ignoreCase = true)) {
            ""
        } else {
            item.name
        }

    private fun labelOf(entry: Entry): String = entry.name.ifEmpty { entry.gameval }

    private fun searchable(item: ItemServerType, gameval: String): Boolean =
        (displayNameOf(item).isNotEmpty() || gameval.isNotEmpty()) &&
            !item.isPlaceholder &&
            !item.isCert &&
            !item.isDummyItem
}

class SpawnMenuScript @Inject constructor(private val protectedAccess: ProtectedAccessLauncher) :
    PluginScript() {
    private val states = WeakHashMap<Player, SpawnState>()

    private fun state(player: Player): SpawnState = states.getOrPut(player) { SpawnState() }

    private val Player.searchResults: Inventory
        get() = invMap.getOrPut(RESULT_INV)

    internal data class TriggerArgs(val comsub: Int, val op: Int, val text: String) : IfScriptArgs

    override fun ScriptContext.startup() {
        onGameStartup { SpawnSearchIndex.build() }

        onCommand("spawn") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Search for and spawn items from a visual grid"
            cheat { openMenu() }
        }

        onIfClose(INTERFACE) {
            states.remove(player)
            player.searchResults.fillNulls()
            player.stopInvTransmit(player.searchResults)
            ClientScripts.chatDefaultRestoreInput(player)
        }

        onIfScriptTrigger<TriggerArgs>("component.spawn_menu:searchtext") {
            if (isAdmin()) search(it.text)
        }

        for (i in 0 until QTY_BUTTON_COUNT) {
            onIfModalButton("component.spawn_menu:qtybtn$i") {
                if (isAdmin()) selectQuantity(i)
            }
        }

        onIfModalButton("component.spawn_menu:notebtn") { if (isAdmin()) toggleNote() }

        onIfModalButton("component.spawn_menu:bankbtn") { if (isAdmin()) toggleBank() }

        onIfModalButton("component.spawn_menu:nullbtn") { if (isAdmin()) toggleNulls() }

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

    private fun ProtectedAccess.open() {
        val results = player.searchResults
        results.fillNulls()
        ifOpenMainModal(INTERFACE)
        ifSetEvents(
            "component.spawn_menu:grid",
            0 until MAX_RESULTS * COMPONENTS_PER_CARD,
            IfEvent.Op1,
        )
        ifSetEvents(
            "component.spawn_menu:searchtext",
            -1..-1,
            IfEvent.DeprecatedOp1,
            IfEvent.ScriptTrigger,
        )
        invTransmit(results)
        renderButtons(state(player))
    }

    private fun ProtectedAccess.search(query: String) {
        val results = player.searchResults
        results.fillNulls()
        state(player).query = query

        val trimmed = query.trim().lowercase()
        val matches =
            if (trimmed.isEmpty()) {
                emptyList()
            } else {
                SpawnSearchIndex.search(trimmed, MAX_RESULTS, player.spawnIncludeNulls)
            }

        sendLabels(matches)
        for (slot in matches.indices) {
            results[slot] = InvObj(matches[slot], 1)
        }
    }

    private fun ProtectedAccess.sendLabels(matches: List<ItemServerType>) {
        val blob = buildString {
            for (item in matches) {
                val gameval = SpawnSearchIndex.gamevalOf(item).ifEmpty { "obj_${item.id}" }
                append(gameval.take(LABEL_WIDTH).padEnd(LABEL_WIDTH))
            }
        }
        player.runClientScript(
            "clientscript.spawn_menu_labels".asRSCM(RSCMType.CLIENTSCRIPT),
            "component.spawn_menu:grid".asRSCM(RSCMType.COMPONENT),
            "component.spawn_menu:scrollbar".asRSCM(RSCMType.COMPONENT),
            "component.spawn_menu:status".asRSCM(RSCMType.COMPONENT),
            blob,
        )
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

    private fun ProtectedAccess.toggleNulls() {
        val state = state(player)
        player.spawnIncludeNulls = !player.spawnIncludeNulls
        renderButtons(state)
        search(state.query)
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
        player.runClientScript(
            "clientscript.spawn_menu_buttons".asRSCM(RSCMType.CLIENTSCRIPT),
            selected,
            customLabel,
            if (state.note) 1 else 0,
            if (state.bank) 1 else 0,
            if (player.spawnIncludeNulls) 1 else 0,
        )
    }
}
