package org.rsmod.content.interfaces.display.name

import dev.openrune.definition.type.widget.IfEvent
import jakarta.inject.Inject
import org.rsmod.api.db.gateway.GameDbManager
import org.rsmod.api.db.gateway.model.fold
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.ui.ifSetText
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.api.player.output.runClientScript

class DisplayNameScript
@Inject
constructor(
    private val eventBus: EventBus,
    private val protectedAccess: ProtectedAccessLauncher,
    private val playerList: PlayerList,
    private val db: GameDbManager,
    private val names: DisplayNameRepository,
) : PluginScript() {
    private val lookedUpNames: MutableMap<Int, DisplayNameLookupResult> = mutableMapOf()

    private companion object {
        private const val DISPLAY_NAME_READY = 1
        private const val DISPLAY_NAME_UNAVAILABLE = 4
        private const val DISPLAY_NAME_AVAILABLE = 4
    }

    override fun ScriptContext.startup() {
        onIfOverlayButton(AccountComponents.name_button) {
            player.openDisplayNameInterface()
        }

        onIfOpen(DisplayNameInterfaces.displayname) {
            player.onDisplayNameOpen()
        }

        onIfOverlayButton(DisplayNameComponents.close) {
            player.closeDisplayNameInterface()
        }

        onIfOverlayButton(DisplayNameComponents.check) {
            protectedAccess.launch(player) { lookupDisplayName() }
        }

        onIfOverlayButton(DisplayNameComponents.change) {
            protectedAccess.launch(player) { confirmDisplayNameChange() }
        }
    //TODO: BOND SUPPORT
        onIfOverlayButton(DisplayNameComponents.bonds) {
            player.mes("Bond-based instant display-name changes are not wired yet.")
        }
    }

    private fun Player.openDisplayNameInterface() {
        ifOpenOverlay(DisplayNameInterfaces.displayname, GameframeComponents.side8, eventBus)
    }

    private fun Player.closeDisplayNameInterface() {
        lookedUpNames.remove(accountId)
        ifOpenOverlay(DisplayNameInterfaces.account, GameframeComponents.side8, eventBus)
    }

    private fun Player.onDisplayNameOpen() {
        displayNamePermitChange = true
        displayNameChangedThisSession = false
        displayNameStatus = DISPLAY_NAME_READY

        ifSetEvents(DisplayNameComponents.close, -1..-1, IfEvent.Op1)
        ifSetEvents(DisplayNameComponents.check, -1..-1, IfEvent.Op1)
        ifSetEvents(DisplayNameComponents.change, -1..-1, IfEvent.Op1)
        ifSetEvents(DisplayNameComponents.bonds, -1..-1, IfEvent.Op1)

        runClientScript(200)   // displayname_init
        runClientScript(204)   // displayname_draw
        runClientScript(1672)  // displayname_button_init
        runClientScript(1675)  // displayname_button_draw

        refreshDisplayNameText()
    }

    private fun Player.refreshDisplayNameText() {
        val current = displayName.ifBlank { username }
        val lookup = lookedUpNames[accountId]

        val statusText =
            when {
                lookup?.available == true ->
                    "<col=00ff00>${lookup.requestedName}: Available</col>"

                lookup != null ->
                    "<col=ff0000>${lookup.requestedName}: Not available</col>"

                else ->
                    "Current name: <col=ffffff>$current</col>"
            }

        ifSetText(DisplayNameComponents.status, statusText)

        ifSetText(DisplayNameComponents.cooldowntype, "Next free change:")
        ifSetText(DisplayNameComponents.cooldowntime, nextDisplayNameChangeText())
        ifSetText(DisplayNameComponents.extrachanges, "Extra changes: <col=ffffff>0</col>")
        // TODO: Replace with real extra-name-change count when bond support is wired.

        ifSetText(
            DisplayNameComponents.warning_reservations,
            "This form cannot revert you to a previous name that's reserved for you. For that, please use the website.",
        )

        val reloadText =
            if (lookup == null) {
                "Log out to reload new purchases."
            } else {
                ""
            }

        ifSetText(DisplayNameComponents.warning_reload, reloadText)
    }

    private fun Player.nextDisplayNameChangeText(): String {
        val changedAt = displayNameChangedAt ?: return "<col=00ff00>Now!</col>"

        val nextFreeChange = changedAt.plusDays(28)
        val now = java.time.LocalDateTime.now()

        if (!now.isBefore(nextFreeChange)) {
            return "<col=00ff00>Now!</col>"
        }

        val days =
            java.time.Duration
                .between(now, nextFreeChange)
                .toDays()
                .coerceAtLeast(1)

        return "<col=ffff00>$days day(s)</col>"
    }

    /*
 * TODO: Native OSRS-style textbox input likely uses display-name varclients
 *  displayname_lookup=229,
 *  displayname_input_listening=435
 *  displayname_input=436
 *  displayname_input_last=437
 *
 * Current rsprot does not expose outgoing varclient packets, so this first
 * implement uses stringDialog for input and renders the lookup result back onto
 * the display-name interface.
 */

    private suspend fun ProtectedAccess.lookupDisplayName() {
        val requested = stringDialog("Enter a display name to check").trim()
        if (requested.isBlank()) {
            player.mes("You did not enter a display name.")
            return
        }

        val uid = player.uid
        val accountId = player.accountId

        db.request(
            request = { connection ->
                names.checkName(connection, accountId, requested)
            },
            response = { result ->
                val current = uid.resolve(playerList) ?: return@request
                result.fold(
                    onOk = { lookup ->
                        lookedUpNames[current.accountId] = lookup
                        current.displayNameStatus =
                            if (lookup.available) {
                                DISPLAY_NAME_AVAILABLE
                            } else {
                                DISPLAY_NAME_UNAVAILABLE
                            }

                        current.runClientScript(204)
                        current.runClientScript(1672)
                        current.runClientScript(1675)
                        current.refreshDisplayNameText()
                    },
                    onErr = {
                        current.displayNameStatus = DISPLAY_NAME_UNAVAILABLE
                        current.mes("Unable to check that display name right now.")
                        current.runClientScript(204)
                        current.runClientScript(1672)
                        current.runClientScript(1675)
                        current.refreshDisplayNameText()
                    },
                )
            },
        )
    }

    private fun Player.canUseFreeDisplayNameChange(): Boolean {
        val changedAt = displayNameChangedAt ?: return true
        return !java.time.LocalDateTime.now().isBefore(changedAt.plusDays(28))
    }

    private fun ProtectedAccess.confirmDisplayNameChange() {
        val lookup = lookedUpNames[player.accountId]

        if (lookup?.available != true) {
            player.mes("That display name is not available.")
            return
        }

        val requested = lookup.requestedName
        val uid = player.uid
        val accountId = player.accountId
        val oldName = player.displayName.ifBlank { player.username }

        if (!player.canUseFreeDisplayNameChange()) {
            player.mes("You cannot change your display name again yet.")
            return
        }

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
                            current.displayNameChangedThisSession = true
                            current.displayNameStatus = DISPLAY_NAME_READY
                            current.rebuildAppearance()
                            lookedUpNames.remove(current.accountId)

                            current.runClientScript(200)   // displayname_init
                            current.runClientScript(204)   // displayname_draw
                            current.runClientScript(1672)  // displayname_button_init
                            current.runClientScript(1675)  // displayname_button_draw
                        } else {
                            current.displayNameStatus = DISPLAY_NAME_UNAVAILABLE

                            current.runClientScript(204)
                            current.runClientScript(1672)
                            current.runClientScript(1675)
                        }

                        current.refreshDisplayNameText()
                    },
                    onErr = {
                        current.displayNameStatus = DISPLAY_NAME_UNAVAILABLE
                        current.mes("Unable to change your display name right now.")
                        current.runClientScript(204)
                        current.runClientScript(1672)
                        current.runClientScript(1675)
                        current.refreshDisplayNameText()
                    },
                )
            },
        )
    }
}
