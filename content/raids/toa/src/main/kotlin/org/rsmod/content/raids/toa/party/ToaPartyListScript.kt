package org.rsmod.content.raids.toa.party

import dev.openrune.definition.type.widget.IfEvent
import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.ui.ifOpenMainModal
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.ui.ifSetText
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.raids.toa.party.ToaPartyManager.appliedParty
import org.rsmod.content.raids.toa.party.ToaPartyManager.currentParty
import org.rsmod.content.raids.toa.party.ToaPartyManager.currentTab
import org.rsmod.content.raids.toa.party.ToaPartyManager.viewingParty
import org.rsmod.content.raids.toa.party.ToaPartyManager.viewingValue
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// ---- Client scripts ----
private const val CS_PARTYLIST_ADDLINE = 6601
private const val CS_ADD_MEMBER = 6722
private const val CS_ADD_APPLICANT = 6727
private const val CS_MASTER_UPDATE = 6729

// ---- Varps / Varbits ----
private const val VARP_CURRENT_PARTY = "varp.toa_mycontroller"
private const val VARBIT_FRIENDS_FILTER = "varbit.toa_partylist_filter"
private const val VARBIT_PARTY_STATUS = "varbit.toa_client_partystatus"
private const val VARBIT_PRESET_SELECTED = "varbit.toa_preset_selected"

// ---- View value constants (must match vanilla CS2 expectations) ----
private const val VIEW_NON_MEMBER = 0
private const val VIEW_MEMBER = 1
private const val VIEW_LEADER = 2
private const val VIEW_APPLICANT = 3
private const val VIEW_KICKED = 4

class ToaPartyListScript @Inject constructor(
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {

    private val playerPartyLists = HashMap<Player, List<ToaLobbyParty>>()

    private var Player.friendsOnlyFilter by intVarBit(VARBIT_FRIENDS_FILTER)
    private var Player.clientPartyStatusVar by intVarBit(VARBIT_PARTY_STATUS)
    private var Player.currentPartyVar by intVarp(VARP_CURRENT_PARTY)
    private var Player.presetSelected by intVarBit(VARBIT_PRESET_SELECTED)

    override fun ScriptContext.startup() {
        onOpLoc1("loc.toa_grouping_board") {
            partyListLoop()
        }
    }

    // ==================================================================
    // Interface 772 — Party list loop
    // ==================================================================

    private suspend fun ProtectedAccess.partyListLoop() {
        while (true) {
            player.viewingParty = null
            player.currentPartyVar = if (player.currentParty != null) 0 else -1
            populateList()

            val input = pauseButton()
            ifClose()

            when (input.component) {
                "component.toa_partylist:contents" -> {
                    when (input.subcomponent) {
                        0 -> continue // Refresh
                        1 -> {
                            val party = handleMakeParty() ?: continue
                            partyDetailsLoop()
                        }
                        2 -> {
                            player.friendsOnlyFilter =
                                if (player.friendsOnlyFilter == 0) 1 else 0
                            continue
                        }
                    }
                }
                "component.toa_partylist:list" -> {
                    val party = handleRowClick(input.subcomponent)
                    if (party != null) {
                        partyDetailsLoop()
                    }
                    continue
                }
                else -> continue
            }
        }
    }

    private fun ProtectedAccess.populateList() {
        ifOpenMainModal("interface.toa_partylist")

        ifSetEvents(
            "component.toa_partylist:contents",
            0..2,
            IfEvent.PauseButton,
        )
        ifSetEvents(
            "component.toa_partylist:list",
            0 until ToaLobbyParty.MAX_LOBBY_PARTIES,
            IfEvent.PauseButton,
        )

        val allParties = ToaPartyManager.allParties()
        val visibleParties = mutableListOf<ToaLobbyParty>()

        for (index in 0 until ToaLobbyParty.MAX_LOBBY_PARTIES) {
            val party = allParties.getOrNull(index)
            if (party == null) {
                player.runClientScript(CS_PARTYLIST_ADDLINE, index, "")
            } else {
                player.runClientScript(CS_PARTYLIST_ADDLINE, index, buildRowString(party))
                visibleParties.add(party)
            }
        }

        playerPartyLists[player] = visibleParties
    }

    private fun ProtectedAccess.buildRowString(party: ToaLobbyParty): String {
        val settings = party.settings
        val sb = StringBuilder()

        var leaderName = party.leaderName
        if (party.isMember(player)) {
            leaderName = "<col=FFFFFF>$leaderName"
        }
        sb.append(leaderName).append('|')

        for (i in 1 until ToaLobbyParty.MAX_PARTY_MEMBERS) {
            if (i < party.members.size) {
                sb.append(party.members[i].displayName)
            }
            sb.append('|')
        }

        sb.append(party.size).append('|')
        sb.append(settings.kcRequirement).append('|')
        sb.append(settings.activeInvocations).append('|')
        sb.append(settings.raidLevel).append('|')
        sb.append(settings.mode).append('|')
        sb.append(System.currentTimeMillis() - party.creationCycle).append('|')

        return sb.toString()
    }

    private fun ProtectedAccess.handleMakeParty(): ToaLobbyParty? {
        val existing = player.currentParty
        if (existing != null) {
            player.viewingParty = existing
            player.currentTab = 0
            return existing
        }

        if (ToaPartyManager.isLobbyFull()) {
            player.mes(
                "The list of lobby parties is currently full. " +
                    "Please come back later or apply to an existing party."
            )
            return null
        }

        val settings = ToaPartySettings() // TODO: copy from player's personal settings
        val party = ToaPartyManager.createParty(player, settings) ?: return null
        player.clientPartyStatusVar = 1
        updateLobbyHud(party)
        player.viewingParty = party
        player.currentTab = 1
        return party
    }

    private fun ProtectedAccess.handleRowClick(comsub: Int): ToaLobbyParty? {
        val parties = playerPartyLists[player] ?: return null
        if (comsub < 0 || comsub >= parties.size) return null

        val selectedParty = parties[comsub]
        if (!ToaPartyManager.partyExists(selectedParty)) {
            player.mes("That party is no longer recruiting.")
            return null
        }

        player.viewingParty = selectedParty
        return selectedParty
    }

    // ==================================================================
    // Interface 774 — Party details loop
    // ==================================================================

    /**
     * Main loop for the party details interface (774).
     * Returns when Back is clicked or the party becomes invalid,
     * sending the player back to the list loop.
     *
     * Sub-index mapping (774:1 pausebuttons):
     *   0=Back, 1=Refresh, 2=Unblock, 3=Set completions,
     *   4=Action, 5=Clear invocations, 6=Load preset, 7=Save preset,
     *   8-11=Tabs, 12-19=Member list, 36-51=Applicant list,
     *   52-97=Invocation toggles
     */
    private suspend fun ProtectedAccess.partyDetailsLoop() {
        while (true) {
            val party = player.viewingParty
            if (party == null || party.leader == null) return

            openAndPopulateDetails(party)

            val input = pauseButton()
            ifClose()

            if (input.component == "component.toa_partydetails:pausebuttons") {
                when (input.subcomponent) {
                    0 -> return // Back
                    1 -> continue // Refresh
                    2 -> handleUnblock(party)
                    3 -> handleSetCompletions(party)
                    4 -> {
                        if (!handleActionButton(party)) return // leave/disband → back to list
                    }
                    5 -> handleClearInvocations(party)
                    6 -> handleLoadPreset(party)
                    7 -> handleSavePreset(party)
                    in 8..11 -> {
                        player.currentTab = input.subcomponent - 8
                    }
                    in 12..19 -> {
                        if (!handleMemberClick(party, input.subcomponent - 12)) return
                    }
                    in 36..51 -> handleApplicantClick(party, input.subcomponent - 36)
                    in 52..97 -> handleInvocationToggle(party, input.subcomponent - 52)
                }
            } else if (input.component == "component.toa_partydetails:presets_button_click") {
                handlePresetSelect(party, input.subcomponent)
            }
            // Loop back → reopens and repopulates 774
        }
    }

    private fun ProtectedAccess.openAndPopulateDetails(party: ToaLobbyParty) {
        player.viewingValue = resolveViewingValue(player, party)

        ifOpenMainModal("interface.toa_partydetails")

        for (i in 0 until ToaLobbyParty.MAX_PARTY_MEMBERS) {
            if (i >= party.members.size) {
                player.runClientScript(CS_ADD_MEMBER, 2, "")
            } else {
                val member = party.members[i]
                player.runClientScript(CS_ADD_MEMBER, 2, buildStatString(member, member == player))
            }
        }

        for (applicant in party.applicants) {
            player.runClientScript(CS_ADD_APPLICANT, buildStatString(applicant, applicant == player))
        }

        val settings = party.settings
        val bitmaps = settings.invocationBitmaps
        player.runClientScript(
            CS_MASTER_UPDATE,
            player.viewingValue,
            settings.kcRequirement,
            settings.activeInvocations,
            settings.raidLevel,
            player.currentTab,
            bitmaps[0],
            bitmaps[1],
            bitmaps[2],
        )

        if (party.isLeader(player)) {
            ifSetEvents(
                "component.toa_partydetails:pausebuttons",
                0..97,
                IfEvent.PauseButton,
            )
            ifSetEvents(
                "component.toa_partydetails:presets_button_click",
                0..5,
                IfEvent.PauseButton, // TODO: vanilla uses Op1+Op2 for select/clear
            )
        }
    }

    // ==================================================================
    // Action button (context-sensitive)
    // ==================================================================

    /**
     * Returns `true` to stay in the details loop, `false` to exit to list.
     */
    private suspend fun ProtectedAccess.handleActionButton(party: ToaLobbyParty): Boolean {
        return when (player.viewingValue) {
            VIEW_NON_MEMBER -> handleApply(party)
            VIEW_MEMBER -> { handleLeave(party); false }
            VIEW_LEADER -> { handleDisband(party); false }
            VIEW_APPLICANT -> { handleWithdraw(party); true }
            VIEW_KICKED -> { player.mes("You have been declined by this party."); true }
            else -> true
        }
    }

    /**
     * Returns `true` to stay in details, `false` to exit to list.
     */
    private suspend fun ProtectedAccess.handleApply(party: ToaLobbyParty): Boolean {
        val previouslyApplied = player.appliedParty
        if (previouslyApplied != null) {
            previouslyApplied.withdraw(player)
            player.appliedParty = null
        }

        val existing = player.currentParty
        if (existing != null) {
            val choice = choice2(
                "Stay in my existing party.", 1,
                "Quit that one and apply to this one.", 2,
                title = "You are already in a party",
            )
            if (choice == 1) return true
            ToaPartyManager.leaveParty(player)
        }

        if (party.apply(player)) {
            player.appliedParty = party
            player.mes("You have applied to join the party of ${party.leaderName}.")
            if (player.currentTab != 1) player.currentTab = 1
            // TODO: refresh the leader's applicant view
            return true
        } else {
            player.mes("That party is no longer recruiting.")
            return false
        }
    }

    private fun ProtectedAccess.handleLeave(party: ToaLobbyParty) {
        val leaderName = party.leaderName
        if (ToaPartyManager.leaveParty(player)) {
            player.mes("You have left the party of $leaderName.")
            // TODO: refresh leader's view
        }
    }

    private fun ProtectedAccess.handleDisband(party: ToaLobbyParty) {
        val result = ToaPartyManager.disbandParty(party)
        player.mes("Your party has disbanded.")

        for (member in result.formerMembers) {
            if (member != player) {
                member.mes("Your party has disbanded.")
            }
        }
        for (applicant in result.formerApplicants) {
            applicant.mes("The party to which you were applying has disbanded.")
        }
    }

    private fun ProtectedAccess.handleWithdraw(party: ToaLobbyParty) {
        if (party.withdraw(player)) {
            player.appliedParty = null
            player.mes("You have withdrawn your party application.")
            // TODO: refresh leader's applicant view
        }
    }

    // ==================================================================
    // Leader-only: Member management
    // ==================================================================

    /**
     * Returns `true` to stay in details, `false` to exit to list.
     */
    private fun ProtectedAccess.handleMemberClick(party: ToaLobbyParty, comsub: Int): Boolean {
        if (!party.isLeader(player)) return true
        if (comsub < 0 || comsub >= party.members.size) return true

        val target = party.members[comsub]
        if (target == player) {
            handleDisband(party)
            return false
        }

        party.removeMember(target)
        target.currentParty = null
        target.mes("You have been kicked from the party of ${player.displayName}.")
        player.mes("You have kicked ${target.displayName} from your party.")
        return true
    }

    // ==================================================================
    // Leader-only: Applicant management
    // ==================================================================

    private fun ProtectedAccess.handleApplicantClick(party: ToaLobbyParty, comsub: Int) {
        if (!party.isLeader(player)) return

        // comsub 0..7 = accept, 8..15 = decline
        val isAccept = comsub < 8
        val applicantIndex = if (isAccept) comsub else comsub - 8

        if (applicantIndex < 0 || applicantIndex >= party.applicants.size) return

        val target = party.applicants[applicantIndex]

        if (isAccept) {
            if (party.isFull()) {
                player.mes("Your party is full.")
                return
            }
            if (party.accept(target)) {
                target.appliedParty = null
                target.currentParty = party
                player.mes("You have accepted ${target.displayName} into your party.")
                target.mes("Your application to the party of ${party.leaderName} has been accepted.")
            }
        } else {
            if (party.decline(target)) {
                target.appliedParty = null
                player.mes("You have declined the party application from ${target.displayName}.")
                target.mes("Your application to the party of ${party.leaderName} has been declined.")
            }
        }
    }

    // ==================================================================
    // Leader-only: Invocation toggling
    // ==================================================================

    private fun ProtectedAccess.handleInvocationToggle(party: ToaLobbyParty, comsub: Int) {
        if (!party.isLeader(player)) return

        val invocations = ToaInvocation.ALL
        if (comsub < 0 || comsub >= invocations.size) return

        val invocation = invocations[comsub]
        val settings = party.settings

        if (settings.isActive(invocation)) {
            handleDeactivate(settings, invocation)
        } else {
            handleActivate(settings, invocation)
        }
    }

    private fun handleDeactivate(settings: ToaPartySettings, invocation: ToaInvocation) {
        // TODO: dependency chains (OVERCLOCKED → OVERCLOCKED_2 → INSANITY)
        // TODO: (NOT_JUST_A_HEAD → ARTERIAL_SPRAY, BLOOD_THINNERS)
        settings.unflag(invocation)
    }

    private fun handleActivate(settings: ToaPartySettings, invocation: ToaInvocation) {
        val category = invocation.category
        if (category == ToaInvocationCategory.ATTEMPTS ||
            category == ToaInvocationCategory.TIME_LIMIT ||
            category == ToaInvocationCategory.HELPFUL_SPIRIT ||
            category == ToaInvocationCategory.PATH_LEVEL
        ) {
            settings.unflagCategory(category)
        }

        // TODO: prerequisite checks
        // INSANITY requires OVERCLOCKED_2
        // OVERCLOCKED_2 requires OVERCLOCKED
        // ARTERIAL_SPRAY requires NOT_JUST_A_HEAD
        // BLOOD_THINNERS requires NOT_JUST_A_HEAD

        settings.flag(invocation)
    }

    // ==================================================================
    // Leader-only: Invocation clear / presets
    // ==================================================================

    private suspend fun ProtectedAccess.handleClearInvocations(party: ToaLobbyParty) {
        if (!party.isLeader(player)) return

        val choice = choice2(
            "Yes.", 1,
            "No.", 2,
            title = "Are you sure you want to clear all active Invocations?",
        )
        if (choice == 1) {
            party.settings.clear()
        }
    }

    private fun ProtectedAccess.handlePresetSelect(party: ToaLobbyParty, comsub: Int) {
        if (!party.isLeader(player)) return
        if (comsub < 0 || comsub > 4) return

        val current = player.presetSelected
        player.presetSelected = if (current == comsub + 1) 0 else comsub + 1
    }

    private fun ProtectedAccess.handleLoadPreset(party: ToaLobbyParty) {
        if (!party.isLeader(player)) return

        val slot = player.presetSelected - 1
        player.presetSelected = 0
        if (slot < 0) {
            player.mes("You do not have a valid preset selected to load from.")
            return
        }
        // TODO: load preset bitmaps from player's persistent varps
        // party.settings.loadPreset(presetBitmaps)
        player.mes("Your preset has been loaded.")
    }

    private suspend fun ProtectedAccess.handleSavePreset(party: ToaLobbyParty) {
        if (!party.isLeader(player)) return

        val slot = player.presetSelected - 1
        player.presetSelected = 0
        if (slot < 0) {
            player.mes("You do not have a valid preset selected to save to.")
            return
        }
        // TODO: check if preset slot is non-empty and confirm overwrite
        // TODO: save current bitmaps to player's persistent varps
        player.mes("Your preset has been saved.")
    }

    // ==================================================================
    // Leader-only: Other
    // ==================================================================

    private fun ProtectedAccess.handleUnblock(party: ToaLobbyParty) {
        if (!party.isLeader(player)) return
        party.blockedPlayers.clear()
        player.mes("All players rejected from this party have been unblocked and may apply again.")
    }

    private suspend fun ProtectedAccess.handleSetCompletions(party: ToaLobbyParty) {
        if (!party.isLeader(player)) return

        val value = countDialog("Set a preferred number of completions up to 100 (or 0 to clear it):")
        party.settings.kcRequirement = value.coerceIn(0, 100)
    }

    // ==================================================================
    // Shared helpers
    // ==================================================================

    private fun resolveViewingValue(player: Player, party: ToaLobbyParty): Int {
        return when {
            party.isLeader(player) -> VIEW_LEADER
            party.isMember(player) -> VIEW_MEMBER
            party.isApplicant(player) -> VIEW_APPLICANT
            party.isBlocked(player) -> VIEW_KICKED
            else -> VIEW_NON_MEMBER
        }
    }

    private fun buildStatString(target: Player, highlight: Boolean): String {
        val sb = StringBuilder()
        if (highlight) sb.append("<col=FFFFFF>")
        sb.append(target.displayName).append('|')
        sb.append(target.combatLevel).append('|')
        sb.append(target.statBase("stat.attack")).append('|')
        sb.append(target.statBase("stat.strength")).append('|')
        sb.append(target.statBase("stat.ranged")).append('|')
        sb.append(target.statBase("stat.magic")).append('|')
        sb.append(target.statBase("stat.defence")).append('|')
        sb.append(target.statBase("stat.hitpoints")).append('|')
        sb.append(target.statBase("stat.prayer")).append('|')
        // TODO: read from TOA kill count varps once the raid system is built
        sb.append("0 / 0 / 0|")
        return sb.toString()
    }

    private fun updateLobbyHud(party: ToaLobbyParty) {
        val text = party.buildPartyString()
        for (member in party.members) {
            member.ifSetText("component.toa_lobby:names", text)
        }
    }
}
