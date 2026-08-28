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
 * Admin item-search-and-spawn tool (`::spawnold`), prompt-based. Kept as a fallback to the v2 grid
 * interface (`content/other/spawn`) - needs no custom cache content, so it always works.
 */
class ItemSpawnMenuScript
@Inject
constructor(private val protectedAccess: ProtectedAccessLauncher) : PluginScript() {
    override fun ScriptContext.startup() {
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
