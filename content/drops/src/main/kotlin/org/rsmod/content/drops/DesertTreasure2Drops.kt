package org.rsmod.content.drops

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.varp.VarpServerType
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.singleRollable
import org.rsmod.api.config.refs.BaseParams
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

private const val VESTIGE_ROLLS_REQUIRED = 3

public fun vestigeProgressRoll(varp: String, vestigeObj: String): Rollable<Player, DropRollItem> =
    singleRollable {
        selectResult { player, _ ->
            val progress = player.vars[varp] + 1
            if (progress >= VESTIGE_ROLLS_REQUIRED) {
                VarPlayerIntMapSetter.set(player, varp, 0)
                RollResult.Single(DropRollItem(vestigeObj, 1))
            } else {
                VarPlayerIntMapSetter.set(player, varp, progress)
                RollResult.Single(DropRollItem("obj.gold_ring", progress))
            }
        }
    }

private val DT2_AWAKENED_KILLCOUNT_VARPS: List<VarpServerType> by lazy {
    listOf(
        "varp.total_duke_sucellus_awakened_kills",
        "varp.total_leviathan_awakened_kills",
        "varp.total_vardorvis_awakened_kills",
        "varp.total_whisperer_awakened_kills",
    ).map { ServerCacheManager.getVarp(it.asRSCM(RSCMType.VARP))!! }
}

public fun Npc.isDt2AwakenedEncounter(): Boolean = vars["varn.awakened_state"] == 1

public fun Player.shouldDropSanguineTorvaKit(npc: Npc, kitObj: String): Boolean {
    if (!npc.isDt2AwakenedEncounter()) return false
    val defeatedVarp = npc.paramOrNull(BaseParams.killcount_varp_awakened) ?: return false
    if (hasObjInInventoryOrBank(kitObj)) return false
    return DT2_AWAKENED_KILLCOUNT_VARPS.filter { it != defeatedVarp }.all { vars[it] > 0 }
}
