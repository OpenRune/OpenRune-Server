package org.rsmod.content.minigames.motherlodemine.scripts

import dev.openrune.types.ObjectServerType
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// Generic crate IDs are reused worldwide; restrict supply contents by placement.
class MotherlodeCrateScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(MotherlodeConstants.CRATE_LOC) { search(it.loc, it.type) }
    }

    private suspend fun ProtectedAccess.search(crate: BoundLocInfo, type: ObjectServerType) {
        val contents = MotherlodeConstants.CRATE_CONTENTS[crate.coords]
        if (contents == null) {
            mes(type.paramOrNull(params.game_message) ?: "You find nothing of interest.")
            return
        }
        arriveDelay()
        if (hasSupply(contents)) {
            mes("You already have ${supplyName(contents)}.")
            return
        }
        if (inv.isFull()) {
            mes("You don't have enough inventory space.")
            return
        }
        invAdd(inv, contents, 1)
        mes("You search the crate and find ${supplyName(contents)}.")
    }

    private fun ProtectedAccess.hasSupply(contents: String): Boolean =
        when (contents) {
            MotherlodeConstants.OBJ_HAMMER ->
                MotherlodeConstants.HAMMER_TYPES.any { it in inv || it in player.worn }
            MotherlodeConstants.OBJ_BRONZE_PICKAXE ->
                inv.filterNotNull { getInvObj(it).isContentType(MotherlodeConstants.CONTENT_PICKAXE) }
                    .isNotEmpty() ||
                    player.worn
                        .filterNotNull { getInvObj(it).isContentType(MotherlodeConstants.CONTENT_PICKAXE) }
                        .isNotEmpty()
            else -> false
        }

    private fun supplyName(contents: String): String =
        if (contents == MotherlodeConstants.OBJ_HAMMER) "a hammer" else "a bronze pickaxe"
}
