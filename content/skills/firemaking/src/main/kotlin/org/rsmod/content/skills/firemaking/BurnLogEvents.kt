package org.rsmod.content.skills.firemaking

import dev.openrune.internalName
import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.firemakingLvl
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpObj4
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.table.FiremakingLogsRow
import org.rsmod.game.obj.Obj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext


public class BurnLogEvents @Inject constructor(private val objRepo: ObjRegistry) : PluginScript() {

    override fun ScriptContext.startup() {
        FiremakingLogsRow.all().forEach { log ->
            onOpHeldU("obj.tinderbox",log.item) { burnLog(log) }

            onOpObj4(log.item) { burnLog(log, it.obj) }
        }

        onPlayerQueueWithArgs<FiremakingLogsRow>("queue.firemaking_light") { lightTick(it.args) }
    }

    private fun ProtectedAccess.burnLog(log : FiremakingLogsRow,groundItem: Obj? = null) {
        val isGroundBurning = groundItem != null

        if (!canBurn(isGroundBurning,log,groundItem)) {
            resetAnim()
            return
        }

        stopAction()
        player.mes("You attempt to light the logs.")

        weakQueue("queue.firemaking_light", 4, log)

    }

    private fun ProtectedAccess.canBurn(groundBurning: Boolean, log : FiremakingLogsRow,groundItem: Obj?): Boolean {
        if (groundBurning && groundItem != null)  {
            if (!objRepo.isValid(player,groundItem)) {
                return false
            }
        }

        if (!inv.contains("obj.tinderbox")) {
            player.mes("You do not have any fire source to light this.")
            return false
        }

        if (player.firemakingLvl < log.level) {
            player.mes("You need a Firemaking level of ${log.level} to burn ${log.item.name} logs.")
            return false
        }

        val blocked = false

        if (blocked) {
            player.mes("You can't light a fire here.")
            return false
        }

        return true
    }

    private fun ProtectedAccess.lightTick(arg: FiremakingLogsRow) {
        println("light tick")
        weakQueue("queue.firemaking_light", 4, arg)
    }

}
