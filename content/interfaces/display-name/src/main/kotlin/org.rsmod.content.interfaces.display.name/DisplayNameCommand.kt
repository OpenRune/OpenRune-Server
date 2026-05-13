package org.rsmod.content.interfaces.display.name

import jakarta.inject.Inject
import org.rsmod.api.db.gateway.GameDbManager
import org.rsmod.api.db.gateway.model.fold
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DisplayNameDevCommands
@Inject
constructor(
    private val db: GameDbManager,
    private val names: DisplayNameRepository,
    private val playerList: PlayerList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("dnchange") {
            internal = "modlevel.admin"
            desc = "Change your display name for testing if needed."
            invalidArgs = "Usage: ::dnchange new name"
            cheat(::changeDisplayName)
        }

        onCommand("dninfo") {
            internal = "modlevel.admin"
            desc = "Show current and previous display-name state."
            cheat(::displayNameInfo)
        }

        onCommand("dnreset") {
            internal = "modlevel.admin"
            desc = "Reset your display-name cooldown."
            cheat(::resetDisplayNameCooldown)
        }
        onCommand("dnchangeother") {
            internal = "modlevel.admin"
            desc = "Change another online player's display name."
            invalidArgs = "Usage: ::dnchangeother target | new name"
            cheat(::changeOtherDisplayName)
        }
    }

    private fun changeDisplayName(cheat: Cheat) {
        val player = cheat.player
        val requested = cheat.args.joinToString(" ").trim()

        if (requested.isBlank()) {
            player.mes("Usage: ::dnchange new name")
            return
        }

        val uid = player.uid
        val accountId = player.accountId
        val oldName = player.displayName.ifBlank { player.username }

        db.request(
            request = { connection ->
                names.changeName(connection, accountId, oldName, requested)
            },
            response = { result ->
                val current = uid.resolve(playerList) ?: return@request

                result.fold(
                    onOk = { change ->
                        current.mes(change.message)

                        if (change.success) {
                            val now = java.time.LocalDateTime.now()

                            current.previousDisplayName = oldName
                            current.displayName = change.requestedName
                            current.displayNameChangedAt = now
                            current.rebuildAppearance()

                            current.mes("Current name: ${current.displayName}")
                            current.mes("Previous name: ${current.previousDisplayName}")
                        }
                    },
                    onErr = {
                        current.mes("Unable to change your display name right now.")
                    },
                )
            },
        )
    }

    private fun displayNameInfo(cheat: Cheat) {
        val player = cheat.player
        player.mes("Login username: ${player.username}")
        player.mes("Display name: ${player.displayName}")
        player.mes("Previous name: ${player.previousDisplayName.ifBlank { "<none>" }}")
    }

        /*
        *Note:
        *Reset lasts until player logs out, the server restarts
        *or the account is reloaded from DB
        *it does not clear accounts.display_name_changed_at
        *in the database

         */
    private fun resetDisplayNameCooldown(cheat: Cheat) {
        val player = cheat.player

        player.displayNameChangedAt = null
        player.displayNameChangedThisSession = false
        player.displayNamePermitChange = true
        player.displayNameStatus = 1

        player.mes("Display-name cooldown reset for this session.")
    }

    private fun changeOtherDisplayName(cheat: Cheat) {
        val admin = cheat.player
        val raw = cheat.args.joinToString(" ").trim()

        val parts = raw.split("|", limit = 2)
        if (parts.size != 2) {
            admin.mes("Usage: ::dnchangeother target | new name")
            return
        }

        val targetName = parts[0].trim()
        val requested = parts[1].trim()

        if (targetName.isBlank() || requested.isBlank()) {
            admin.mes("Usage: ::dnchangeother target | new name")
            return
        }

        val target =
            playerList.firstOrNull {
                it.username.equals(targetName, ignoreCase = true) ||
                    it.displayName.equals(targetName, ignoreCase = true) ||
                    it.previousDisplayName.equals(targetName, ignoreCase = true)
            }

        if (target == null) {
            admin.mes("Player '$targetName' is not online.")
            return
        }

        val targetUid = target.uid
        val adminUid = admin.uid
        val accountId = target.accountId
        val oldName = target.displayName.ifBlank { target.username }

        db.request(
            request = { connection ->
                names.changeName(connection, accountId, oldName, requested)
            },
            response = { result ->
                val currentTarget = targetUid.resolve(playerList) ?: return@request
                val currentAdmin = adminUid.resolve(playerList)

                result.fold(
                    onOk = { change ->
                        if (change.success) {
                            val now = java.time.LocalDateTime.now()

                            currentTarget.previousDisplayName = oldName
                            currentTarget.displayName = change.requestedName
                            currentTarget.displayNameChangedAt = now
                            currentTarget.rebuildAppearance()

                            currentTarget.mes("Your display name has been changed to ${currentTarget.displayName}.")
                            currentAdmin?.mes(
                                "Changed ${oldName}'s display name to ${currentTarget.displayName}."
                            )
                        } else {
                            currentAdmin?.mes(change.message)
                        }
                    },
                    onErr = {
                        currentAdmin?.mes("Unable to change that display name right now.")
                    },
                )
            },
        )
    }
}
