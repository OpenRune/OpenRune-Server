package org.rsmod.content.slayer

import org.rsmod.api.cheat.CheatHandlerBuilder
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.content.slayer.core.SlayerTaskManager
import org.rsmod.content.slayer.rewards.SlayerRewardsManager
import org.rsmod.game.cheat.Cheat
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SlayerCommands : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("slayertask", "Assign slayer task (default: Skeletons)", ::assignSlayerTask) {
            invalidArgs =
                "Use as ::slayertask [taskId|name] [amount] (default: Skeletons / 11, 35 kills)"
        }
        onCommand("slayerpoints", "Set slayer reward points", ::setSlayerPoints) {
            invalidArgs = "Use as ::slayerpoints [amount] (default: 100000)"
        }
        onCommand("resetTask", "Reset Slayer Task", ::resetTask) {
            invalidArgs = "Use as ::resetTask"
        }
    }

    private fun resetTask(cheat: Cheat) =
        with(cheat) {
            player.mes("Task reset.")
            SlayerTaskManager.resetTask(player)
        }


    private fun assignSlayerTask(cheat: Cheat) =
        with(cheat) {
            val taskId =
                when {
                    args.isEmpty() -> SlayerTaskManager.SKELETONS_TASK_ID
                    else ->
                        SlayerTaskManager.resolveTaskId(args[0])
                            ?: args[0].toIntOrNull()
                            ?: run {
                                player.mes("Unknown slayer task: '${args[0]}'")
                                return@with
                            }
                }

            val amount =
                when {
                    args.size >= 2 -> args[1].toIntOrNull()
                    else -> null
                }
            if (args.size >= 2 && amount == null) {
                player.mes("Invalid amount: '${args[1]}'")
                return@with
            }

            val resolvedAmount = amount ?: DEFAULT_CHEAT_TASK_AMOUNT
            if (resolvedAmount <= 0) {
                player.mes("Amount must be positive.")
                return@with
            }

            if (!SlayerTaskManager.assignTaskById(player, taskId, resolvedAmount)) {
                player.mes("Failed to assign slayer task id $taskId.")
                return@with
            }

            val task = SlayerTaskManager.slayerTargets.find { it.id == taskId }
            val name = task?.nameUppercase ?: "task $taskId"
            player.mes("Assigned slayer task: kill $resolvedAmount $name (id=$taskId).")
        }

    private fun setSlayerPoints(cheat: Cheat) =
        with(cheat) {
            val amount =
                when {
                    args.isEmpty() -> DEFAULT_CHEAT_SLAYER_POINTS
                    else ->
                        args[0].toIntOrNull() ?: run {
                            player.mes("Invalid amount: '${args[0]}'")
                            return@with
                        }
                }
            if (amount < 0) {
                player.mes("Amount cannot be negative.")
                return@with
            }
            SlayerRewardsManager.setPoints(player, amount)
            SlayerRewardsManager.syncPoints(player)
            player.mes("Slayer points set to $amount.")
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

    private companion object {
        private const val DEFAULT_CHEAT_TASK_AMOUNT = 35
        private const val DEFAULT_CHEAT_SLAYER_POINTS = 100_000
    }
}
