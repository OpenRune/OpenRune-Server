package org.rsmod.content.slayer

import org.rsmod.api.cheat.CheatHandlerBuilder
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.content.slayer.core.SlayerTaskManager
import org.rsmod.content.slayer.rewards.SlayerRewardsPoints
import org.rsmod.game.cheat.Cheat
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SlayerCommands : PluginScript() {
    override fun ScriptContext.startup() {

        onCommand("resetTask", "Reset Slayer Task", ::resetTask) {
            invalidArgs = "Use as ::resetTask"
        }
    }

    private fun resetTask(cheat: Cheat) =
        with(cheat) {
            player.mes("Task reset.")
            SlayerTaskManager.resetTask(player)
        }


    private fun ScriptContext.onCommand(
        command: String,
        desc: String,
        cheat: Cheat.() -> Unit,
        init: CheatHandlerBuilder.() -> Unit = {},
    ) {
        onCommand(command) {
            this.internal = "modlevel.admin"
            this.desc = desc
            this.cheat(cheat)
            init()
        }
    }

}
