package org.alter.plugins.content.commands.commands.admin

import org.alter.api.Skills
import org.alter.api.ext.getCommandArgs
import org.alter.api.ext.message
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.priv.Privilege
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

class SkillPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {
        onCommand("skill", Privilege.ADMIN_POWER, description = "Set skill level: ::skill <skill_name> <level>") {
            val args = player.getCommandArgs()

            if (args.size < 2) {
                player.message("Usage: ::skill <skill_name> <level>")
                player.message("Example: ::skill herblore 99")
                return@onCommand
            }

            var skillName = args[0].lowercase()

            val skillId = Skills.getSkillForName(player.getSkills().maxSkills, skillName)

            if (skillId == -1) {
                player.message("Could not find skill: ${args[0]}")
                return@onCommand
            }

            val level = try {
                args[1].toInt().coerceIn(1, 99)
            } catch (e: NumberFormatException) {
                player.message("Invalid level. Must be a number between 1 and 99.")
                return@onCommand
            }

            player.getSkills().setBaseLevel(skillId, level)
            val skillDisplayName = Skills.getSkillName(skillId)
            player.message("Set $skillDisplayName level to $level.")
        }
    }
}

