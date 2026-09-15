package org.rsmod.content.skills.thieving

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.thievingLvl
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpLoc5
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Stalls
@Inject
constructor(private val locRepo: LocRepository, private val xpMods: XpModifiers) : PluginScript() {
    override fun ScriptContext.startup() {
        for (stall in ThievingData.stalls) {
            val type = ServerCacheManager.getObject(stall.loc.asRSCM(RSCMType.LOC)) ?: continue
            val slot = (1..5).firstOrNull { type.actions.getOpOrNull(it - 1) == STEAL_OP } ?: continue
            when (slot) {
                1 -> onOpLoc1(stall.loc) { steal(it.loc, stall) }
                2 -> onOpLoc2(stall.loc) { steal(it.loc, stall) }
                3 -> onOpLoc3(stall.loc) { steal(it.loc, stall) }
                4 -> onOpLoc4(stall.loc) { steal(it.loc, stall) }
                else -> onOpLoc5(stall.loc) { steal(it.loc, stall) }
            }
        }
    }

    private suspend fun ProtectedAccess.steal(loc: BoundLocInfo, stall: StallTarget) {
        if (player.thievingLvl < stall.level) {
            mes("You need a Thieving level of ${stall.level} to steal from this stall.")
            return
        }

        if (inv.freeSpace() < 1) {
            mes("You don't have enough inventory space to hold any more items.")
            return
        }

        faceSquare(loc.coords)
        anim(ANIM_STEAL_STALL)

        val (obj, count) = stall.loot.roll(random)
        if (invAdd(inv, obj, count).failure) {
            mes("You don't have enough inventory space to hold any more items.")
            resetAnim()
            return
        }

        statAdvance(STAT_THIEVING, stall.xp * xpMods.get(player, STAT_THIEVING))
        spam("You steal some ${objName(obj)}.")

        if (stall.empty != null) {
            locRepo.change(loc, stall.empty, stall.respawn)
        }

        delay(2)
        resetAnim()
    }

    private fun objName(internal: String): String =
        ServerCacheManager.getItem(internal.asRSCM(RSCMType.OBJ))?.name?.lowercase() ?: internal

    private companion object {
        const val STEAL_OP = "Steal-from"
    }
}
