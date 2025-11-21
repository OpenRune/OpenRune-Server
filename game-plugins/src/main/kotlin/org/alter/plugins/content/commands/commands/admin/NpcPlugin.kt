package org.alter.plugins.content.commands.commands.admin

import org.alter.api.ext.getCommandArgs
import org.alter.api.ext.message
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.Npc
import org.alter.game.model.priv.Privilege
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.rscm.RSCM
import org.alter.rscm.RSCMType

class NpcPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {
        onCommand("npc", Privilege.ADMIN_POWER, description = "Spawn Npc") {
            val values = player.getCommandArgs()
            if (values.isEmpty()) {
                player.message("Usage: ::npc <id or rscm_name>")
                return@onCommand
            }

            val input = values[0]
            val npcId: Int

            // Try to parse as integer first
            val numericId = input.toIntOrNull()
            if (numericId != null) {
                npcId = numericId
            } else {
                // Try exact RSCM match first
                val exactMatch = try {
                    RSCM.getRSCM(input)
                } catch (e: Exception) {
                    -1
                }

                if (exactMatch != -1) {
                    npcId = exactMatch
                } else {
                    // Try partial match
                    val partialMatch = findPartialNpcMatch(input)
                    if (partialMatch != null) {
                        npcId = partialMatch
                    } else {
                        player.message("No NPC found matching: $input")
                        return@onCommand
                    }
                }
            }

            val npc = Npc(npcId, player.tile, world)
            val npcName = RSCM.getReverseMapping(RSCMType.NPCTYPES, npcId) ?: "Unknown"
            player.message("NPC: $npcId ($npcName) spawned on x:${player.tile.x} z:${player.tile.z}")
            world.spawn(npc)
        }
    }

    private fun findPartialNpcMatch(query: String): Int? {
        val lowerQuery = query.lowercase()
        val mappings = RSCM.getReverseCache(RSCMType.NPCTYPES) ?: return null

        // First try exact case-insensitive match (without prefix)
        for ((id, name) in mappings) {
            val nameWithoutPrefix = name.removePrefix("$").substringAfter(".")
            if (nameWithoutPrefix.lowercase() == lowerQuery) {
                return id
            }
        }

        // Then try partial match (without prefix)
        for ((id, name) in mappings) {
            val nameWithoutPrefix = name.removePrefix("$").substringAfter(".")
            val lowerName = nameWithoutPrefix.lowercase()
            if (lowerName.contains(lowerQuery) || lowerQuery.contains(lowerName)) {
                return id
            }
        }

        // Finally try with full name including prefix
        for ((id, name) in mappings) {
            val lowerName = name.lowercase()
            if (lowerName.contains(lowerQuery) || lowerQuery.contains(lowerName)) {
                return id
            }
        }

        return null
    }
}
