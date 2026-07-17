package org.rsmod.content.other.discord

import jakarta.inject.Inject
import org.rsmod.api.net.central.CentralDiscordWorldLink
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onEvent
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DiscordLinkScript
@Inject
constructor(
    private val centralDiscord: CentralDiscordWorldLink,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<SessionStateEvent.Logout> {
            centralDiscord.invalidatePending(player.accountId)
        }

        onCommand("discordlink") {
            desc = "Generate a code to link your Discord account"
            invalidArgs = "Usage: ::discordlink [discord_username]"
            cheat { handleDiscordLink() }
        }
    }

    private fun Cheat.handleDiscordLink() {
        val discordUsername =
            args.getOrNull(0)?.takeIf { it.isNotBlank() }
                ?: player.username.takeIf { it.isNotBlank() }
                ?: player.displayName
        if (discordUsername.isBlank()) {
            player.mes("Could not determine your Discord username. Use: ::discordlink <discord_username>")
            return
        }

        if (player.discordId != null) {
            player.mes("Your account is already linked to Discord.")
            return
        }

        when (
            val result =
                centralDiscord.createGamePending(
                    accountId = player.accountId,
                    discordUsername = discordUsername,
                )
        ) {
            is CentralDiscordWorldLink.GamePendingResult.Ok -> {
                player.mes("Your Discord linking code is: ${result.code}")
                if (result.dmSent) {
                    player.mes("Check your Discord DMs to finish linking.")
                } else {
                    player.mes("Could not send a Discord DM. Check your privacy settings and try again.")
                }
            }
            CentralDiscordWorldLink.GamePendingResult.DiscordNotFound -> {
                player.mes("That Discord user was not found. They must be a member of the Discord server.")
            }
            CentralDiscordWorldLink.GamePendingResult.AlreadyLinked -> {
                player.mes("This game account is already linked to Discord.")
            }
            CentralDiscordWorldLink.GamePendingResult.Unavailable -> {
                player.mes("Discord linking is unavailable right now. Try again later.")
            }
        }
    }
}
