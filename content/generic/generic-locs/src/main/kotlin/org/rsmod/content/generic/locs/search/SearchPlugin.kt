package org.rsmod.content.generic.locs.search

import dev.openrune.types.ObjectServerType
import jakarta.inject.Inject
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpContentLoc1
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SearchPlugin
@Inject
constructor(private val hooks: Set<@JvmSuppressWildcards CrateSearchHook>) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpContentLoc1("content.empty_crate") { searchCrate(it.loc, it.type) }
        onOpContentLoc1("content.empty_sacks") { player.search(it.type) }
        onOpContentLoc1("content.empty_boxes") { player.search(it.type) }
    }

    private suspend fun ProtectedAccess.searchCrate(crate: BoundLocInfo, type: ObjectServerType) {
        val hook = hooks.firstOrNull { it.claims(player, crate) }
        if (hook == null) {
            player.search(type)
            return
        }
        with(hook) { search(crate, type) }
    }

    private fun Player.search(type: ObjectServerType) {
        val message = type.paramOrNull(params.game_message) ?: SearchConstants.DEFAULT
        mes(message)
    }
}
