package org.rsmod.content.bosses.bryophyta

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BryophytaLogs
@Inject
constructor(private val locRepo: LocRepository, private val objRepo: ObjRepository) :
    PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1(LOG_WITH_AXE) { takeBronzeAxe(it.loc) }
    }

    private suspend fun ProtectedAccess.takeBronzeAxe(logs: BoundLocInfo) {
        if (player.hasAxe()) {
            mesbox("You already have an axe.")
            return
        }
        if (player.inv.isFull()) {
            mesbox("You don't have enough room for the axe.")
            return
        }
        locRepo.change(logs, LOG_WITHOUT_AXE, AXE_RESPAWN_TICKS)
        invAddOrDrop(objRepo, BRONZE_AXE)
        soundSynth("synth.remove_axe")
        objbox(BRONZE_AXE, 400, "You take a bronze axe from the logs.")
    }

    private fun Player.hasAxe(): Boolean {
        val equipped = righthand?.let(::getInvObj)
        if (equipped != null && BryophytaGrowthling.isAxe(equipped)) {
            return true
        }
        return inv.any { obj -> obj != null && BryophytaGrowthling.isAxe(getInvObj(obj)) }
    }

    private companion object {
        private const val LOG_WITH_AXE = "loc.gb_log_withaxe"
        private const val LOG_WITHOUT_AXE = "loc.log_withoutaxe"
        private const val BRONZE_AXE = "obj.bronze_axe"
        private const val AXE_RESPAWN_TICKS = 50
    }
}
