package org.rsmod.content.raids.toa.party

import dev.openrune.definition.type.widget.IfEvent
import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.ifOpenMainModal
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.raids.toa.party.ToaPartyManager.appliedParty
import org.rsmod.content.raids.toa.party.ToaPartyManager.currentParty
import org.rsmod.content.raids.toa.party.ToaPartyManager.currentTab
import org.rsmod.content.raids.toa.party.ToaPartyManager.viewingParty
import org.rsmod.content.raids.toa.party.ToaPartyManager.viewingValue
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Client script 6601: populates one row in the party list. */
private const val CS_PARTYLIST_ADDLINE = 6601

/** Varp 3603: -1 = no party, 0 = has a party. */
private const val VARP_CURRENT_PARTY = "varp.toa_mycontroller"

/** Varbit 14318: 0 = show all, 1 = friends only. */
private const val VARBIT_FRIENDS_FILTER = "varbit.toa_partylist_filter"

class ToaPartyListScript @Inject constructor(
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {

    /**
     * Per-player snapshot of which parties map to which row indices.
     * Set during [populateList], read when a row is clicked.
     */
    private val playerPartyLists = HashMap<Player, List<ToaLobbyParty>>()

    /** Varbit delegate for friends-only filter toggle. */
    private var Player.friendsOnlyFilter by intVarBit(VARBIT_FRIENDS_FILTER)

    /** Varp delegate for current party indicator. */
    private var Player.currentPartyVar by intVarp(VARP_CURRENT_PARTY)

    override fun ScriptContext.startup() {

        // Obelisk click → open the party list
        onOpLoc1("loc.toa_grouping_obelisk") {
            openPartyList()
        }

        // Refresh button
        onIfModalButton("component.toa_partylist:refresh") {
            populateList()
        }

        // Make Party / View My Party button
        onIfModalButton("component.toa_partylist:myparty") {
            handleMakeParty()
        }

        // Friends-only filter toggle
        onIfModalButton("component.toa_partylist:filter") {
            handleFilter()
        }

        // Clicking a party row (components 00..44 on the list layer)
        onIfModalButton("component.toa_partylist:list") {
            handleRowClick(it.comsub)
        }
    }

    // ---- Opening the party list ----

    private fun ProtectedAccess.openPartyList() {
        player.viewingParty = null
        player.currentPartyVar = if (player.currentParty != null) 0 else -1
        populateList()
    }

    // ---- Populating the party list ----

    private fun ProtectedAccess.populateList() {
        ifOpenMainModal("interface.toa_partylist")

        // Enable click events on the row components
        ifSetEvents(
            "component.toa_partylist:list",
            0 until ToaLobbyParty.MAX_LOBBY_PARTIES,
            IfEvent.Op1,
        )

        val allParties = ToaPartyManager.allParties()
        val visibleParties = mutableListOf<ToaLobbyParty>()

        for (index in 0 until ToaLobbyParty.MAX_LOBBY_PARTIES) {
            val party = allParties.getOrNull(index)
            if (party == null) {
                // Empty row — send blank string to clear it
                player.runClientScript(CS_PARTYLIST_ADDLINE, index, "")
            } else {
                val rowString = buildRowString(party)
                player.runClientScript(CS_PARTYLIST_ADDLINE, index, rowString)
                visibleParties.add(party)
            }
        }

        // Store this player's view of the party list for row click lookups
        playerPartyLists[player] = visibleParties
    }

    /**
     * Builds the pipe-delimited string that CS2 script 6601 expects.
     *
     * Format: leader|member2|member3|...|member8|size|kcReq|invocations|raidLevel|mode|age|
     */
    private fun ProtectedAccess.buildRowString(party: ToaLobbyParty): String {
        val settings = party.settings
        val sb = StringBuilder()

        // Leader name — highlight white if we're in this party
        var leaderName = party.leaderName
        if (party.isMember(player)) {
            leaderName = "<col=FFFFFF>$leaderName"
        }
        sb.append(leaderName).append('|')

        // Members 2..8 (index 1..7)
        for (i in 1 until ToaLobbyParty.MAX_PARTY_MEMBERS) {
            if (i < party.members.size) {
                sb.append(party.members[i].displayName)
            }
            sb.append('|')
        }

        // Party size
        sb.append(party.size).append('|')
        // KC requirement
        sb.append(settings.kcRequirement).append('|')
        // Active invocation count
        sb.append(settings.activeInvocations).append('|')
        // Raid level
        sb.append(settings.raidLevel).append('|')
        // Mode string (Entry/Normal/Expert)
        sb.append(settings.mode).append('|')
        // Age in server ticks since creation
        sb.append(System.currentTimeMillis() - party.creationCycle).append('|')

        return sb.toString()
    }

    // ---- Button handlers ----

    private fun ProtectedAccess.handleMakeParty() {
        val existing = player.currentParty
        if (existing != null) {
            // Already in a party — view it
            player.viewingParty = existing
            player.currentTab = 0 // Members tab
            openPartyDetails()
        } else {
            // Create a new party
            if (ToaPartyManager.isLobbyFull()) {
                player.mes(
                    "The list of lobby parties is currently full. " +
                        "Please come back later or apply to an existing party."
                )
                return
            }
            val settings = ToaPartySettings() // TODO: copy from player's personal settings
            val party = ToaPartyManager.createParty(player, settings) ?: return
            player.viewingParty = party
            player.currentTab = 1 // Invocations tab (so leader can configure)
            openPartyDetails()
        }
    }

    private fun ProtectedAccess.handleFilter() {
        player.friendsOnlyFilter = if (player.friendsOnlyFilter == 0) 1 else 0
        populateList()
    }

    private fun ProtectedAccess.handleRowClick(comsub: Int) {
        val parties = playerPartyLists[player] ?: return
        if (comsub < 0 || comsub >= parties.size) return

        // Check if our own party is in a raid
        val ownParty = player.currentParty
        if (ownParty != null) {
            // TODO: check insideRaid when raid system is built
        }

        val selectedParty = parties[comsub]
        if (!ToaPartyManager.partyExists(selectedParty)) {
            player.mes("That party is no longer recruiting.")
            populateList()
            return
        }

        player.viewingParty = selectedParty
        openPartyDetails()
    }

    // ---- Transition to party details (interface 774) ----

    /**
     * Opens the party management/details interface.
     * This will be handled by [ToaPartyDetailsScript].
     */
    private fun ProtectedAccess.openPartyDetails() {
        // Calculate what role this player has relative to the party
        val party = player.viewingParty ?: return
        player.viewingValue = ToaPartyManager.resolveViewingValue(player, party)

        // The details script handles opening interface 774 and populating it.
        // For now, we set the state and open the interface — the details
        // script's onIfOpen or button handlers take over from here.
        ifOpenMainModal("interface.toa_partydetails")
    }
}
