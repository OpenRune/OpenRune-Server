package org.rsmod.content.other.commands.spawn

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.content.other.commands.onCommand
import org.rsmod.game.cheat.Cheat
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Admin item-search-and-spawn tool (`::spawn`). Uses the real native item-search dialog
 * (`objDialog`, same client-side live-search overlay used for things like the real GE search box
 * - the client does its own filtering off its cache, no custom interface/grid needed on our end)
 * looped so it never "closes": search, pick a result, enter a quantity, spawn, and it
 * immediately re-opens the search so you can keep going without retyping the command.
 *
 * Deliberately does NOT reuse `shopmain`'s item grid (tried that first) - its `items` component
 * is already permanently claimed by the real shop system (`api/shops/ShopScript.kt`), and the
 * event bus only allows one handler per component, so sharing it isn't possible.
 */
class ItemSpawnMenuScript
@Inject
constructor(private val protectedAccess: ProtectedAccessLauncher) : PluginScript() {
    override fun ScriptContext.startup() {
        // Renamed from `spawn`: the v2 grid interface (content/other/spawn) owns that name now.
        // Kept as a fallback - it needs no custom cache content, so it still works if the packed
        // interface is ever out of date.
        onCommand("spawnold", "Search for and spawn an item (prompt-based)", ::spawn)
    }

    private fun spawn(cheat: Cheat) =
        with(cheat) { protectedAccess.launch(player) { spawnLoop() } }

    private suspend fun ProtectedAccess.spawnLoop() {
        while (true) {
            val item =
                objDialog(
                    title = "Search for an item to spawn:",
                    stockMarketRestriction = false,
                    showLastSearched = true,
                )
            val amount = countDialog("Enter spawn quantity:")
            give(item, amount)
        }
    }

    private fun ProtectedAccess.give(item: ItemServerType, count: Int) {
        val spawned = player.invAdd(player.inv, item.id, count, strict = false)
        val completed = spawned.completed()
        if (completed <= 0) {
            player.mes("You don't have enough inventory space.")
            return
        }
        player.mes("Spawned '${item.name}' x $completed.")
    }
}
