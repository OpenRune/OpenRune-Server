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
import org.rsmod.api.script.onIfModalButton
import org.rsmod.content.raids.toa.party.ToaPartyManager.appliedParty
import org.rsmod.content.raids.toa.party.ToaPartyManager.currentParty
import org.rsmod.content.raids.toa.party.ToaPartyManager.currentTab
import org.rsmod.content.raids.toa.party.ToaPartyManager.viewingParty
import org.rsmod.content.raids.toa.party.ToaPartyManager.viewingValue
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// ---- Client scripts ----
private const val CS_ADD_MEMBER = 6722
private const val CS_ADD_APPLICANT = 6727
private const val CS_MASTER_UPDATE = 6729

// ---- Varbit for preset selection ----
private const val VARBIT_PRESET_SELECTED = "varbit.toa_preset_selected"

class ToaPartyDetailsScript @Inject constructor(
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {

    private var Player.presetSelected by intVarBit(VARBIT_PRESET_SELECTED)

    override fun ScriptContext.startup() {

        // ---- Navigation ----

        onIfModalButton("component.toa_partydetails:back") {
            openPartyList()
        }

        onIfModalButton("component.toa_partydetails:refresh") {
            refreshDetails()
        }

        // ---- Tabs ----

        onIfModalButton("component.toa_partydetails:members_tab") {
            player.currentTab = 0
            refreshDetails()
        }

        onIfModalButton("component.toa_partydetails:applicants_tab") {
            player.currentTab = 1
            refreshDetails()
        }

        onIfModalButton("component.toa_partydetails:invocations_tab") {
            player.currentTab = 2
            refreshDetails()
        }

        onIfModalButton("component.toa_partydetails:summary_tab") {
            player.currentTab = 3
            refreshDetails()
        }

        // ---- Action button (Apply / Leave / Disband / Withdraw) ----

        onIfModalButton("component.toa_partydetails:action") {
            handleActionButton()
        }

        // ---- Leader-only: Unblock all ----

        onIfModalButton("component.toa_partydetails:unblock") {
            handleUnblock()
        }

        // ---- Leader-only: Set KC requirement ----

        onIfModalButton("component.toa_partydetails:completions") {
            handleSetCompletions()
        }

        // ---- Leader-only: Invocation controls ----

        onIfModalButton("component.toa_partydetails:invocations_clear") {
            handleClearInvocations()
        }

        onIfModalButton("component.toa_partydetails:btn_presets_load") {
            handleLoadPreset()
        }

        onIfModalButton("component.toa_partydetails:btn_presets_save") {
            handleSavePreset()
        }

        // ---- Preset slot selection ----

        onIfModalButton("component.toa_partydetails:presets_button_click") {
            handlePresetSelect(it.comsub, it.op.slot)
        }

        // ---- Member list (click to kick, index 0..7 within the list) ----

        onIfModalButton("component.toa_partydetails:members_list") {
            handleMemberClick(it.comsub)
        }

        // ---- Applicant list (accept/decline buttons) ----

        onIfModalButton("component.toa_partydetails:applicants_list") {
            handleApplicantClick(it.comsub)
        }

        // ---- Invocation toggles (click to toggle, index = invocation ordinal) ----

        onIfModalButton("component.toa_partydetails:invocations_content") {
            handleInvocationToggle(it.comsub)
        }
    }

    // ==================================================================
    // Navigation
    // ==================================================================

    private fun ProtectedAccess.openPartyList() {
        player.viewingParty = null
        ifOpenMainModal("interface.toa_partylist")
        // The party list script will repopulate via its own onIfOpen
    }

    // ==================================================================
    // Refreshing the details interface
    // ==================================================================

    private fun ProtectedAccess.refreshDetails() {
        val party = checkViewingParty() ?: return

        ifOpenMainModal("interface.toa_partydetails")

        // Recalculate the player's role
        player.viewingValue = ToaPartyManager.resolveViewingValue(player, party)

        // Send member rows
        sendMembers(party)

        // Send applicant rows
        sendApplicants(party)

        // Master update — sets tabs, raid level/mode, invocation bitmaps
        sendMasterUpdate(party)

        // Enable button events for the leader
        if (party.isLeader(player)) {
            // Preset slot buttons: op1 = select, op2 = clear
            ifSetEvents(
                "component.toa_partydetails:presets_button_click",
                0..4,
                IfEvent.Op1, IfEvent.Op2,
            )
            // Pausebuttons layer: all sub-buttons
            ifSetEvents(
                "component.toa_partydetails:pausebuttons",
                0..79,
                IfEvent.PauseButton,
            )
        }
    }

    // ==================================================================
    // Sending data to the client
    // ==================================================================

    /**
     * Sends CS2 6722 per member slot. Empty slots get an empty string.
     */
    private fun ProtectedAccess.sendMembers(party: ToaLobbyParty) {
        for (i in 0 until ToaLobbyParty.MAX_PARTY_MEMBERS) {
            if (i >= party.members.size) {
                player.runClientScript(CS_ADD_MEMBER, 2, "")
            } else {
                val member = party.members[i]
                val isSelf = member == player
                player.runClientScript(CS_ADD_MEMBER, 2, buildStatString(member, isSelf))
            }
        }
    }

    /**
     * Sends CS2 6727 per applicant.
     */
    private fun ProtectedAccess.sendApplicants(party: ToaLobbyParty) {
        for (applicant in party.applicants) {
            val isSelf = applicant == player
            player.runClientScript(CS_ADD_APPLICANT, buildStatString(applicant, isSelf))
        }
    }

    /**
     * Sends CS2 6729 — the master update that controls tabs, raid level,
     * mode text, invocation bitmaps, and the bottom bar.
     *
     * Args: viewingValue, kcReq, activeCount, raidLevel, currentTab,
     *       bitmap0, bitmap1, bitmap2
     */
    private fun ProtectedAccess.sendMasterUpdate(party: ToaLobbyParty) {
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
    }

    /**
     * Builds the pipe-delimited stat string that CS2 6722/6727 expect.
     *
     * Format: name|combatLevel|atk|str|rng|mag|def|hp|pray|kc_entry / kc_normal / kc_expert|
     */
    private fun buildStatString(target: Player, highlight: Boolean): String {
        val sb = StringBuilder()
        if (highlight) sb.append("<col=FFFFFF>")
        sb.append(target.displayName).append('|')
        sb.append(target.combatLevel).append('|')
        // TODO: read real stats once the stat API is wired
        // For now, placeholder values
        sb.append("99|99|99|99|99|99|99|")
        // KC: entry / normal / expert
        // TODO: read from player's kill count tracking
        sb.append("0 / 0 / 0|")
        return sb.toString()
    }

    // ==================================================================
    // Action button (context-sensitive based on viewing value)
    // ==================================================================

    private suspend fun ProtectedAccess.handleActionButton() {
        val party = checkViewingParty() ?: return

        when (player.viewingValue) {
            ToaPartyManager.VIEW_NON_MEMBER -> handleApply(party)
            ToaPartyManager.VIEW_MEMBER -> handleLeave(party)
            ToaPartyManager.VIEW_LEADER -> handleDisband(party)
            ToaPartyManager.VIEW_APPLICANT -> handleWithdraw(party)
            ToaPartyManager.VIEW_KICKED -> {
                player.mes("You have been declined by this party.")
                refreshDetails()
            }
        }
    }

    private suspend fun ProtectedAccess.handleApply(party: ToaLobbyParty) {
        // Withdraw from any existing application first
        val previouslyApplied = player.appliedParty
        if (previouslyApplied != null) {
            previouslyApplied.withdraw(player)
            player.appliedParty = null
        }

        // If already in a party, ask to leave first
        val existing = player.currentParty
        if (existing != null) {
            val choice = choice2(
                "Stay in my existing party.", 1,
                "Quit that one and apply to this one.", 2,
                title = "You are already in a party",
            )
            if (choice == 1) {
                refreshDetails()
                return
            }
            ToaPartyManager.leaveParty(player)
        }

        if (party.apply(player)) {
            player.appliedParty = party
            player.mes("You have applied to join the party of ${party.leaderName}.")
            if (player.currentTab != 1) player.currentTab = 1
            refreshDetails()
            // Notify the leader
            val leader = party.leader
            if (leader != null) {
                // TODO: refresh the leader's applicant view if they have 774 open
            }
        } else {
            player.mes("That party is no longer recruiting.")
            openPartyList()
        }
    }

    private fun ProtectedAccess.handleLeave(party: ToaLobbyParty) {
        val leaderName = party.leaderName
        if (ToaPartyManager.leaveParty(player)) {
            player.mes("You have left the party of $leaderName.")
            // Notify remaining leader
            val leader = party.leader
            if (leader != null) {
                // TODO: refresh leader's view
            }
            openPartyList()
        }
    }

    private fun ProtectedAccess.handleDisband(party: ToaLobbyParty) {
        val result = ToaPartyManager.disbandParty(party)
        player.mes("Your party has disbanded.")

        // Notify former members
        for (member in result.formerMembers) {
            if (member != player) {
                member.mes("Your party has disbanded.")
            }
        }
        // Notify former applicants
        for (applicant in result.formerApplicants) {
            applicant.mes("The party to which you were applying has disbanded.")
        }

        openPartyList()
    }

    private fun ProtectedAccess.handleWithdraw(party: ToaLobbyParty) {
        if (party.withdraw(player)) {
            player.appliedParty = null
            player.mes("You have withdrawn your party application.")
            refreshDetails()
            // Notify leader
            val leader = party.leader
            if (leader != null) {
                // TODO: refresh leader's applicant view
            }
        }
    }

    // ==================================================================
    // Leader-only: Member management
    // ==================================================================

    private fun ProtectedAccess.handleMemberClick(comsub: Int) {
        val party = checkViewingParty() ?: return
        if (!party.isLeader(player)) return
        if (comsub < 0 || comsub >= party.members.size) return

        val target = party.members[comsub]
        if (target == player) {
            // Leader clicked themselves → leave/disband
            handleDisband(party)
            return
        }

        // Kick the member
        party.removeMember(target)
        target.currentParty = null
        target.mes("You have been kicked from the party of ${player.displayName}.")
        player.mes("You have kicked ${target.displayName} from your party.")
        refreshDetails()
    }

    // ==================================================================
    // Leader-only: Applicant management
    // ==================================================================

    private fun ProtectedAccess.handleApplicantClick(comsub: Int) {
        val party = checkViewingParty() ?: return
        if (!party.isLeader(player)) return

        // comsub 0..7 = accept buttons, 8..15 = decline buttons
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
        refreshDetails()
    }

    // ==================================================================
    // Leader-only: Invocation toggling
    // ==================================================================

    private fun ProtectedAccess.handleInvocationToggle(comsub: Int) {
        val party = checkViewingParty() ?: return
        if (!party.isLeader(player)) return

        val invocations = ToaInvocation.ALL
        if (comsub < 0 || comsub >= invocations.size) return

        val invocation = invocations[comsub]
        val settings = party.settings

        if (settings.isActive(invocation)) {
            // Turning off — handle dependency chains
            handleDeactivate(settings, invocation)
        } else {
            // Turning on — handle prerequisites and mutual exclusions
            if (!handleActivate(settings, invocation)) return
        }
        refreshDetails()
    }

    /**
     * Deactivates an invocation and any that depend on it.
     * E.g., turning off OVERCLOCKED also turns off OVERCLOCKED_2 and INSANITY.
     */
    private fun handleDeactivate(settings: ToaPartySettings, invocation: ToaInvocation) {
        // Find invocations that depend on this one and deactivate them first
        // This is hardcoded dependency logic matching the vanilla game
        // TODO: wire up the specific dependency chains
        // (OVERCLOCKED → OVERCLOCKED_2 → INSANITY)
        // (NOT_JUST_A_HEAD → ARTERIAL_SPRAY, BLOOD_THINNERS)
        settings.unflag(invocation)
    }

    /**
     * Attempts to activate an invocation. Returns `false` if
     * prerequisites are not met.
     */
    private fun handleActivate(settings: ToaPartySettings, invocation: ToaInvocation): Boolean {
        // Mutual exclusion: categories where only one can be active
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
        return true
    }

    // ==================================================================
    // Leader-only: Invocation clear / presets
    // ==================================================================

    private suspend fun ProtectedAccess.handleClearInvocations() {
        val party = checkViewingParty() ?: return
        if (!party.isLeader(player)) return

        val choice = choice2(
            "Stay in my existing party.", 1,
            "Quit that one and apply to this one.", 2,
            title = "You are already in a party",
        )
        if (choice == 1) {
            party.settings.clear()
            refreshDetails()
        }
    }

    private fun ProtectedAccess.handlePresetSelect(comsub: Int, op: Int) {
        val party = checkViewingParty() ?: return
        if (!party.isLeader(player)) return
        if (comsub < 0 || comsub > 4) return

        if (op == 0) {
            // Op1: Toggle selection
            val current = player.presetSelected
            player.presetSelected = if (current == comsub + 1) 0 else comsub + 1
            refreshDetails()
        } else {
            // Op2: Clear this preset
            // TODO: clear preset from player's persistent storage
            player.mes("Your preset has been cleared.")
            refreshDetails()
        }
    }

    private fun ProtectedAccess.handleLoadPreset() {
        val party = checkViewingParty() ?: return
        if (!party.isLeader(player)) return

        val slot = player.presetSelected - 1
        player.presetSelected = 0
        if (slot < 0) {
            player.mes("You do not have a valid preset selected to load from.")
            refreshDetails()
            return
        }
        // TODO: load preset bitmaps from player's persistent varps
        // party.settings.loadPreset(presetBitmaps)
        player.mes("Your preset has been loaded.")
        refreshDetails()
    }

    private suspend fun ProtectedAccess.handleSavePreset() {
        val party = checkViewingParty() ?: return
        if (!party.isLeader(player)) return

        val slot = player.presetSelected - 1
        player.presetSelected = 0
        if (slot < 0) {
            player.mes("You do not have a valid preset selected to save to.")
            refreshDetails()
            return
        }
        // TODO: check if preset slot is non-empty and confirm overwrite
        // TODO: save current bitmaps to player's persistent varps
        player.mes("Your preset has been saved.")
        refreshDetails()
    }

    // ==================================================================
    // Leader-only: Other
    // ==================================================================

    private fun ProtectedAccess.handleUnblock() {
        val party = checkViewingParty() ?: return
        if (!party.isLeader(player)) return
        party.blockedPlayers.clear()
        player.mes("All players rejected from this party have been unblocked and may apply again.")
        refreshDetails()
    }

    private suspend fun ProtectedAccess.handleSetCompletions() {
        val party = checkViewingParty() ?: return
        if (!party.isLeader(player)) return

        val value = countDialog("Set a preferred number of completions up to 100 (or 0 to clear it):")
        party.settings.kcRequirement = value.coerceIn(0, 100)
        refreshDetails()
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    /**
     * Validates that the player's viewing party still exists.
     * If not, sends them back to the party list.
     */
    private fun ProtectedAccess.checkViewingParty(): ToaLobbyParty? {
        val party = player.viewingParty
        if (party == null || party.leader == null) {
            openPartyList()
            return null
        }
        return party
    }
}
