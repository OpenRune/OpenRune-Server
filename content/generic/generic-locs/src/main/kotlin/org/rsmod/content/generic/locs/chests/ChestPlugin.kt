package org.rsmod.content.generic.locs.chests

import dev.openrune.types.ObjectServerType
import jakarta.inject.Inject
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpContentLoc1
import org.rsmod.api.script.onOpContentLoc2
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ChestPlugin @Inject constructor(private val locRepo: LocRepository): PluginScript() {
    override fun ScriptContext.startup() {
        onOpContentLoc1("content.empty_chest") { player.search(it.type) }
        onOpContentLoc2("content.empty_chest") { toggleChest(it.loc, it.type) }
        onOpContentLoc1("content.chest") { toggleChest(it.loc, it.type) }
    }

    private fun ProtectedAccess.toggleChest(closed: BoundLocInfo, type: ObjectServerType) {
        val sound = type.param(params.opensound)
        val openedLoc = type.param(params.next_loc_stage)
        soundSynth(sound)


        locRepo.del(closed, ChestConstants.DURATION)
        locRepo.add(
            closed.coords,
            openedLoc.internalName,
            ChestConstants.DURATION,
            closed.angle,
            closed.shape,
            onDespawn = {}
        )
    }

    private fun Player.search(type: ObjectServerType) {
        val message = type.paramOrNull(params.game_message) ?: ChestConstants.DEFAULT
        mes(message)
    }
}
