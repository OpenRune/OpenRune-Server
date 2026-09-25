package org.rsmod.content.raids.toa.party

import dev.openrune.definition.type.widget.IfEvent
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.ui.ifOpenMainModal
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.content.raids.toa.party.ToaPartyManager.appliedParty
import org.rsmod.content.raids.toa.party.ToaPartyManager.currentParty
import org.rsmod.content.raids.toa.party.ToaPartyManager.currentTab
import org.rsmod.content.raids.toa.party.ToaPartyManager.VIEW_APPLICANT
import org.rsmod.content.raids.toa.party.ToaPartyManager.VIEW_KICKED
import org.rsmod.content.raids.toa.party.ToaPartyManager.VIEW_LEADER
import org.rsmod.content.raids.toa.party.ToaPartyManager.VIEW_MEMBER
import org.rsmod.content.raids.toa.party.ToaPartyManager.VIEW_NON_MEMBER
import org.rsmod.content.raids.toa.party.ToaPartyManager.viewingParty
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

// ---- Sounds ----
private const val SYNTH_INVOCATION_ON = 6589
private const val SYNTH_INVOCATION_OFF = 6588
private const val SYNTH_PRESET_SAVE_LOAD = 2655

// ---- Invocation presets ----
// 5 slots x 3 bitmaps, vanilla varps 3680-3694:
// varp.toa_invocations_preset_1a, _1b, _1c, _2a ... _5c
private const val PRESET_SLOTS = 5
private val PRESET_PARTS = listOf("a", "b", "c")

// ---- Invocation categories where only one invocation may be active ----
private val SINGLE_SELECT_CATEGORIES = setOf(
    ToaInvocationCategory.ATTEMPTS,
    ToaInvocationCategory.TIME_LIMIT,
    ToaInvocationCategory.HELPFUL_SPIRIT,
    ToaInvocationCategory.PATH_LEVEL,
)

/**
 * @Singleton because [ToaRaidScript][org.rsmod.content.raids.toa.raid.ToaRaidScript]
 * injects this script to open the board from the raid entrance ("Form or join a
 * party."). Without it Guice would build a second, separate instance.
 */
@Singleton
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
        onPlayerLogout {
            ToaPartyManager.onLogout(player) // also refreshes the other party members' screens
        }
        onPlayerLogin {
            // OpenRune saves every varp by default, so without this a player who
            // logged out while in a party would log back in still flagged as in one.
            player.currentPartyVar = -1
            player.clientPartyStatusVar = 0
        }
    }

    // ==================================================================
    // Interface 772 — Party list loop
    // ==================================================================

    /** Opens the party board (772), as if the grouping board was clicked. */
    internal suspend fun ProtectedAccess.openPartyList() {
        partyListLoop()
    }

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
        ifOpenMainModal("interface.toa_partylist", transparency = -2)

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
            leaderName = "<col=ffffff>$leaderName</col>"
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
        sb.append(mapClock - party.creationCycle) // age in ticks, last field: no trailing pipe

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

        val settings = ToaPartyManager.loadPersonalSettings(player)
        // createParty also updates the lobby HUD (773) names list.
        val party = ToaPartyManager.createParty(player, settings, mapClock) ?: return null
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

            // Remember the leader's latest settings for their next party.
            if (party.isLeader(player)) {
                ToaPartyManager.savePersonalSettings(player, party.settings)
            }

            // Show the change to everyone else in the party. Tab switches and
            // preset slot selection only affect this player's own screen.
            val isPersonalOnly = input.component != "component.toa_partydetails:pausebuttons" ||
                input.subcomponent in 8..11
            if (!isPersonalOnly) {
                ToaPartyManager.refreshViewers(party, exclude = player)
            }
            // Loop back → reopens and repopulates 774
        }
    }

    private fun ProtectedAccess.openAndPopulateDetails(party: ToaLobbyParty) {

        ifOpenMainModal("interface.toa_partydetails", transparency = -2)

        // First arg is the viewer's view value: the CS2 only makes member rows
        // hoverable/clickable (kick) when it is VIEW_LEADER.
        val viewValue = ToaPartyManager.resolveViewingValue(player, party)
        for (i in 0 until ToaLobbyParty.MAX_PARTY_MEMBERS) {
            if (i >= party.members.size) {
                player.runClientScript(CS_ADD_MEMBER, viewValue, "")
            } else {
                player.runClientScript(CS_ADD_MEMBER, viewValue, buildStatString(party.members[i]))
            }
        }

        for (applicant in party.applicants) {
            player.runClientScript(CS_ADD_APPLICANT, buildStatString(applicant))
        }

        val settings = party.settings
        val bitmaps = settings.invocationBitmaps
        player.runClientScript(
            CS_MASTER_UPDATE,
            ToaPartyManager.resolveViewingValue(player, party),
            settings.kcRequirement,
            settings.activeInvocations,
            settings.raidLevel,
            player.currentTab,
            bitmaps[0],
            bitmaps[1],
            bitmaps[2],
        )

        // Vanilla enables these for EVERY viewer, not just the leader. Without them the
        // server silently drops the click (Apply, Leave, Back, Refresh, the client's own
        // 10-second auto-refresh...). Leader-only actions are checked in their handlers.
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

    // ==================================================================
    // Action button (context-sensitive)
    // ==================================================================

    /**
     * Returns `true` to stay in the details loop, `false` to exit to list.
     */
    private suspend fun ProtectedAccess.handleActionButton(party: ToaLobbyParty): Boolean {
        return when (ToaPartyManager.resolveViewingValue(player, party)) {
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
            ToaPartyManager.refreshViewers(previouslyApplied, exclude = player)
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
            ToaPartyManager.refreshViewers(existing, exclude = player)
        }

        if (party.apply(player)) {
            player.appliedParty = party
            player.mes("You have applied to join the party of ${party.leaderName}.")
            if (player.currentTab != 1) player.currentTab = 1
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
            ToaPartyManager.refreshViewers(party, exclude = player)
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

        // Their loops see the party has no leader and drop back to the list.
        for (other in result.formerMembers + result.formerApplicants + result.formerBlocked) {
            if (other != player) ToaPartyManager.refreshDetailsView(other)
        }
    }

    private fun ProtectedAccess.handleWithdraw(party: ToaLobbyParty) {
        if (party.withdraw(player)) {
            player.appliedParty = null
            player.mes("You have withdrawn your party application.")
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

        ToaPartyManager.kickMember(party, target)
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
            if (ToaPartyManager.acceptApplicant(party, target)) {
                player.mes("You have accepted ${target.displayName} into your party.")
                target.mes("Your application to the party of ${party.leaderName} has been accepted.")
            }
        } else {
            if (party.decline(target)) {
                target.appliedParty = null
                player.mes("You have declined the party application from ${target.displayName}.")
                target.mes("Your application to the party of ${party.leaderName} has been declined.")
                ToaPartyManager.refreshDetailsView(target)
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
            deactivateWithDependents(settings, invocation)
            soundSynth(SYNTH_INVOCATION_OFF)
        } else if (tryActivate(settings, invocation)) {
            soundSynth(SYNTH_INVOCATION_ON)
        }
    }

    /**
     * Disables an invocation and, recursively, every active invocation
     * that requires it (cache param 1346). Handles chains such as
     * Overclocked → Overclocked 2 → Insanity.
     */
    private fun deactivateWithDependents(settings: ToaPartySettings, invocation: ToaInvocation) {
        for (dependent in invocation.dependents) {
            if (settings.isActive(dependent)) {
                deactivateWithDependents(settings, dependent)
            }
        }
        settings.unflag(invocation)
    }

    /**
     * Enables an invocation. Returns `false` (and tells the player why)
     * if its prerequisite isn't active yet.
     */
    private fun ProtectedAccess.tryActivate(settings: ToaPartySettings, invocation: ToaInvocation): Boolean {
        val prerequisite = invocation.prerequisite
        if (prerequisite != null && !settings.isActive(prerequisite)) {
            player.mes("You cannot activate this invocation without first enabling <col=ff0000>${prerequisite.name}</col>.")
            return false
        }

        val category = invocation.category
        if (category in SINGLE_SELECT_CATEGORIES) {
            settings.unflagCategory(category)
        }

        settings.flag(invocation)
        return true
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
        val preset = readPreset(slot)
        if (preset.all { it == 0 }) {
            player.mes("You do not have any invocations stored in this preset.")
            return
        }
        party.settings.loadPreset(preset)
        player.mes("Your preset has been loaded.")
        soundSynth(SYNTH_PRESET_SAVE_LOAD)
    }

    private suspend fun ProtectedAccess.handleSavePreset(party: ToaLobbyParty) {
        if (!party.isLeader(player)) return

        val slot = player.presetSelected - 1
        player.presetSelected = 0
        if (slot < 0) {
            player.mes("You do not have a valid preset selected to save to.")
            return
        }

        if (readPreset(slot).any { it != 0 }) {
            val choice = choice2(
                "Save and overwrite this preset.", 1,
                "Cancel", 2,
                title = "You already have a preset saved in this slot.",
            )
            if (choice != 1) return
        }

        writePreset(slot, party.settings.invocationBitmaps)
        player.mes("Your preset has been saved.")
        soundSynth(SYNTH_PRESET_SAVE_LOAD)
    }

    /** Varp name for one bitmap of a preset, e.g. slot 0, part 2 → varp.toa_invocations_preset_1c */
    private fun presetVarp(slot: Int, part: Int): String =
        "varp.toa_invocations_preset_${slot + 1}${PRESET_PARTS[part]}"

    /** Reads all three bitmaps of a preset slot (0-based). */
    private fun ProtectedAccess.readPreset(slot: Int): IntArray {
        require(slot in 0 until PRESET_SLOTS) { "Invalid preset slot $slot" }
        return IntArray(PRESET_PARTS.size) { part -> vars[presetVarp(slot, part)] }
    }

    /** Writes all three bitmaps of a preset slot (0-based). */
    private fun ProtectedAccess.writePreset(slot: Int, bitmaps: IntArray) {
        require(slot in 0 until PRESET_SLOTS) { "Invalid preset slot $slot" }
        for (part in PRESET_PARTS.indices) {
            vars[presetVarp(slot, part)] = bitmaps[part]
        }
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

    /**
     * Vanilla format: name|combat|7 stats|entry / normal / expert KC (no trailing pipe).
     * No colour tags: the CS2 colours the viewer's own row white itself by comparing
     * the name to chat_playername, and a tag would break that comparison.
     */
    private fun buildStatString(target: Player): String {
        val sb = StringBuilder()
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
        sb.append("0 / 0 / 0")
        return sb.toString()
    }
}
