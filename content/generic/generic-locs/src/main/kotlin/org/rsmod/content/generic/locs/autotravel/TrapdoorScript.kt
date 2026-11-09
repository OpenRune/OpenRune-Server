package org.rsmod.content.generic.locs.autotravel

import jakarta.inject.Inject
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpContentLoc1
import org.rsmod.api.script.onOpContentLoc2
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Trapdoors paired with their open model by the travel coverage report: opening one swaps in the
 * open model for a while, and the open model's first op climbs down through it.
 */
class TrapdoorScript @Inject constructor(private val locRepo: LocRepository, private val traveller: AutoTraveller) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpContentLoc1(CLOSED) { swap(it.loc, it.type.param(params.next_loc_stage)) }
        onOpContentLoc1(OPEN) { with(traveller) { travel(it.loc, TravelKind.Down, up = false) } }
        onOpContentLoc2(OPEN) { swap(it.loc, it.type.param(params.next_loc_stage)) }
    }

    private suspend fun ProtectedAccess.swap(loc: BoundLocInfo, next: dev.openrune.types.ObjectServerType) {
        arriveDelay()
        locRepo.del(loc, DURATION)
        locRepo.add(loc.coords, next, DURATION, loc.angle, loc.shape)
    }

    private companion object {
        const val CLOSED = "content.auto_trapdoor_closed"
        const val OPEN = "content.auto_trapdoor_open"
        const val DURATION = 500
    }
}
