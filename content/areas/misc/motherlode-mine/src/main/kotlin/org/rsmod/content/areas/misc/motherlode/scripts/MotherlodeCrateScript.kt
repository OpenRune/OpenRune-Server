package org.rsmod.content.areas.misc.motherlode.scripts

import dev.openrune.types.ObjectServerType
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.getInvObj
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MotherlodeCrateScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(CRATE_LOC) { search(it.loc, it.type) }
    }

    private suspend fun ProtectedAccess.search(crate: BoundLocInfo, type: ObjectServerType) {
        val contents = CRATE_CONTENTS[crate.coords] ?: return
        arriveDelay()
        if (hasSupply(contents)) {
            mes(type.paramOrNull(params.game_message) ?: "You search the crate but find nothing.")
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
            HAMMER -> HAMMER_TYPES.any { it in inv || it in worn }
            BRONZE_PICKAXE ->
                inv.filterNotNull { getInvObj(it).isContentType(PICKAXE_CONTENT) }.isNotEmpty() ||
                    worn.filterNotNull { getInvObj(it).isContentType(PICKAXE_CONTENT) }.isNotEmpty()
            else -> false
        }

    private fun supplyName(contents: String): String =
        if (contents == HAMMER) "a hammer" else "a bronze pickaxe"

    private companion object {
        const val CRATE_LOC = "loc.crate2_old"
        const val HAMMER = "obj.hammer"
        const val BRONZE_PICKAXE = "obj.bronze_pickaxe"
        const val PICKAXE_CONTENT = "content.mining_pickaxe"

        val HAMMER_TYPES =
            listOf("obj.hammer", "obj.imcando_hammer", "obj.imcando_hammer_offhand")

        val CRATE_CONTENTS =
            mapOf(
                CoordGrid(3752, 5674, 0) to HAMMER,
                CoordGrid(3752, 5664, 0) to HAMMER,
                CoordGrid(3753, 5659, 0) to HAMMER,
                CoordGrid(3755, 5665, 0) to BRONZE_PICKAXE,
                CoordGrid(3756, 5668, 0) to BRONZE_PICKAXE,
                CoordGrid(3758, 5671, 0) to BRONZE_PICKAXE,
            )
    }
}
