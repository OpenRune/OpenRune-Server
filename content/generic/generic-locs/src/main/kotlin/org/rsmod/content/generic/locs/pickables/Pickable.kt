package org.rsmod.content.generic.locs.pickables

import dev.openrune.types.ItemServerType
import dev.openrune.types.ObjectServerType
import jakarta.inject.Inject
import org.rsmod.api.config.locParam
import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.synths
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpLoc2
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Pickable
@Inject
constructor(private val objRepo: ObjRepository, private val locRepo: LocRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc2(content.pickable_crop) { pickCrop(it.loc, it.type) }
        onOpLoc2(pickable_locs.cabbage) { pickCabbage(it.loc, it.type) }
    }

    private suspend fun ProtectedAccess.pickCrop(
        loc: BoundLocInfo,
        type: ObjectServerType,
        takeObj: ItemServerType = type.takeItemServerType,
        takeMessage: String = type.takeMessage,
    ) {
        arriveDelay()

        if (inv.isFull()) {
            playerWalk(loc.coords)
            mes(type.invFullMessage)
            return
        }

        anim(seqs.human_pickupfloor)
        playerWalkWithMinDelay(loc.coords)

        locRepo.del(loc, type.respawnTime)

        mes(takeMessage)
        soundSynth(synths.pick)
        invAddOrDrop(objRepo, takeObj)
    }

    private suspend fun ProtectedAccess.pickCabbage(loc: BoundLocInfo, type: ObjectServerType) {
        // The rate for cabbage seed is currently unknown.
        if (random.randomBoolean(25)) {
            pickCrop(loc, type, objs.cabbage_seed, "You pick a cabbage seed.")
        } else {
            pickCrop(loc, type)
        }
    }
}

private val ObjectServerType.takeMessage by locParam(params.game_message)
private val ObjectServerType.invFullMessage by locParam(params.game_message2)
private val ObjectServerType.takeItemServerType by locParam(params.rewarditem)
private val ObjectServerType.respawnTime by locParam(params.respawn_time)
