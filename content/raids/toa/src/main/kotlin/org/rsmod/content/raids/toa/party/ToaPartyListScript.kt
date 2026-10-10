package org.rsmod.content.raids.toa.party

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
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
import org.rsmod.content.raids.toa.party.ToaPartyManager.VIEW_APPLICANT
import org.rsmod.content.raids.toa.party.ToaPartyManager.VIEW_KICKED
import org.rsmod.content.raids.toa.party.ToaPartyManager.VIEW_LEADER
import org.rsmod.content.raids.toa.party.ToaPartyManager.VIEW_MEMBER
import org.rsmod.content.raids.toa.party.ToaPartyManager.VIEW_NON_MEMBER
import org.rsmod.content.raids.toa.party.ToaPartyManager.appliedParty
import org.rsmod.content.raids.toa.party.ToaPartyManager.currentParty
import org.rsmod.content.raids.toa.party.ToaPartyManager.currentTab
import org.rsmod.content.raids.toa.party.ToaPartyManager.viewingParty
import org.rsmod.content.raids.toa.raid.ToaKillCount
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private const val CS_PARTYLIST_ADDLINE = "clientscript.[clientscript,toa_partylist_addline]"
private const val CS_ADD_MEMBER = "clientscript.[clientscript,toa_partydetails_addmember]"
private const val CS_ADD_APPLICANT = "clientscript.[clientscript,toa_partydetails_addapplicant]"
private const val CS_MASTER_UPDATE = "clientscript.[clientscript,script6729]"
private const val VARP_CURRENT_PARTY = "varp.toa_mycontroller"
private const val VARBIT_FRIENDS_FILTER = "varbit.toa_partylist_filter"
private const val VARBIT_PARTY_STATUS = "varbit.toa_client_partystatus"
private const val VARBIT_PRESET_SELECTED = "varbit.toa_preset_selected"
private const val SYNTH_INVOCATION_ON = "synth.toa_invocation_on"
private const val SYNTH_INVOCATION_OFF = "synth.toa_invocation_off"
private const val SYNTH_PRESET_SAVE_LOAD = "synth.found_gem"
private const val PRESET_SLOTS = 5
private val PRESET_PARTS = listOf("a", "b", "c")
private const val JOIN_PARTY_IN_TOMBS = "You should join your party in the tombs."
private const val NO_LONGER_RECRUITING = "That party is no longer recruiting."
private val SINGLE_SELECT_CATEGORIES = setOf(
    ToaInvocationCategory.ATTEMPTS,
    ToaInvocationCategory.TIME_LIMIT,
    ToaInvocationCategory.HELPFUL_SPIRIT,
    ToaInvocationCategory.PATH_LEVEL,
)

@Singleton
class ToaPartyListScript @Inject constructor(
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {

    private var Player.friendsOnlyFilter by intVarBit(VARBIT_FRIENDS_FILTER)
    private var Player.clientPartyStatusVar by intVarBit(VARBIT_PARTY_STATUS)
    private var Player.currentPartyVar by intVarp(VARP_CURRENT_PARTY)
    private var Player.presetSelected by intVarBit(VARBIT_PRESET_SELECTED)

    override fun ScriptContext.startup() {
        onOpLoc1("loc.toa_grouping_board") {
            partyListLoop()
        }
        onPlayerLogout {
            ToaPartyManager.onLogout(player)
        }
        onPlayerLogin {
            player.currentPartyVar = -1
            player.clientPartyStatusVar = 0
        }
    }

    internal suspend fun ProtectedAccess.openPartyList() {
        partyListLoop()
    }

    private suspend fun ProtectedAccess.partyListLoop() {
        while (true) {
            player.viewingParty = null
            player.currentPartyVar = if (player.currentParty != null) 0 else -1
            val listed = populateList()

            val input = pauseButton()
            ifClose()

            when (input.component) {
                "component.toa_partylist:contents" -> {
                    when (input.subcomponent) {
                        0 -> continue
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
                    val party = handleRowClick(listed, input.subcomponent)
                    if (party != null) {
                        partyDetailsLoop()
                    }
                    continue
                }
                else -> continue
            }
        }
    }

    private fun ProtectedAccess.populateList(): List<ToaLobbyParty> {
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
                player.runClientScript(CS_PARTYLIST_ADDLINE.asRSCM(RSCMType.CLIENTSCRIPT), index, "")
            } else {
                player.runClientScript(
                    CS_PARTYLIST_ADDLINE.asRSCM(RSCMType.CLIENTSCRIPT),
                    index,
                    buildRowString(party),
                )
                visibleParties.add(party)
            }
        }

        return visibleParties
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
        sb.append(mapClock - party.creationCycle)

        return sb.toString()
    }

    private suspend fun ProtectedAccess.handleMakeParty(): ToaLobbyParty? {
        val existing = player.currentParty
        if (existing != null) {
            if (existing.insideRaid) {
                mesbox(JOIN_PARTY_IN_TOMBS)
                return null
            }
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
        val party = ToaPartyManager.createParty(player, settings, mapClock) ?: return null
        player.viewingParty = party
        player.currentTab = 1
        return party
    }

    private suspend fun ProtectedAccess.handleRowClick(
        listed: List<ToaLobbyParty>,
        comsub: Int,
    ): ToaLobbyParty? {
        val selectedParty = listed.getOrNull(comsub) ?: return null
        if (player.currentParty?.insideRaid == true) {
            mesbox(JOIN_PARTY_IN_TOMBS)
            return null
        }
        if (!ToaPartyManager.partyExists(selectedParty)) {
            mesbox(NO_LONGER_RECRUITING)
            return null
        }

        player.viewingParty = selectedParty
        return selectedParty
    }

    private suspend fun ProtectedAccess.partyDetailsLoop() {
        while (true) {
            val party = player.viewingParty
            if (party == null || party.leader == null) return

            openAndPopulateDetails(party)

            val input = pauseButton()
            ifClose()

            if (input.component == "component.toa_partydetails:pausebuttons") {
                when (input.subcomponent) {
                    0 -> return
                    1 -> continue
                    2 -> handleUnblock(party)
                    3 -> handleSetCompletions(party)
                    4 -> {
                        if (!handleActionButton(party)) return
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

            if (party.isLeader(player)) {
                ToaPartyManager.savePersonalSettings(player, party.settings)
            }

            val isPersonalOnly = input.component != "component.toa_partydetails:pausebuttons" ||
                input.subcomponent in 8..11
            if (!isPersonalOnly) {
                ToaPartyManager.refreshViewers(party, exclude = player)
            }
        }
    }

    private fun ProtectedAccess.openAndPopulateDetails(party: ToaLobbyParty) {

        ifOpenMainModal("interface.toa_partydetails", transparency = -2)

        val viewValue = ToaPartyManager.resolveViewingValue(player, party)
        for (i in 0 until ToaLobbyParty.MAX_PARTY_MEMBERS) {
            if (i >= party.members.size) {
                player.runClientScript(CS_ADD_MEMBER.asRSCM(RSCMType.CLIENTSCRIPT), viewValue, "")
            } else {
                player.runClientScript(
                    CS_ADD_MEMBER.asRSCM(RSCMType.CLIENTSCRIPT),
                    viewValue,
                    buildStatString(party.members[i]),
                )
            }
        }

        for (applicant in party.applicants) {
            player.runClientScript(
                CS_ADD_APPLICANT.asRSCM(RSCMType.CLIENTSCRIPT),
                buildStatString(applicant),
            )
        }

        val settings = party.settings
        val bitmaps = settings.invocationBitmaps
        player.runClientScript(
            CS_MASTER_UPDATE.asRSCM(RSCMType.CLIENTSCRIPT),
            ToaPartyManager.resolveViewingValue(player, party),
            settings.kcRequirement,
            settings.activeInvocations,
            settings.raidLevel,
            player.currentTab,
            bitmaps[0],
            bitmaps[1],
            bitmaps[2],
        )

        ifSetEvents(
            "component.toa_partydetails:pausebuttons",
            0..97,
            IfEvent.PauseButton,
        )
        ifSetEvents(
            "component.toa_partydetails:presets_button_click",
            0..5,
            IfEvent.PauseButton,
        )
    }

    private suspend fun ProtectedAccess.handleActionButton(party: ToaLobbyParty): Boolean {
        return when (ToaPartyManager.resolveViewingValue(player, party)) {
            VIEW_NON_MEMBER -> handleApply(party)
            VIEW_MEMBER -> handleLeave(party)
            VIEW_LEADER -> { handleDisband(party); false }
            VIEW_APPLICANT -> { handleWithdraw(party); true }
            VIEW_KICKED -> { player.mes("You have been declined by this party."); true }
            else -> true
        }
    }

    private suspend fun ProtectedAccess.handleApply(party: ToaLobbyParty): Boolean {
        val existing = player.currentParty
        if (existing != null) {
            if (!existing.insideRaid) {
                val choice = choice2(
                    "Stay in my existing party.", 1,
                    "Quit that one and apply to this one.", 2,
                    title = "You are already in a party",
                )
                if (choice == 1) return true
            }
            if (existing.insideRaid) {
                mesbox(JOIN_PARTY_IN_TOMBS)
                return true
            }
        }

        if (!ToaPartyManager.partyExists(party)) {
            mesbox(NO_LONGER_RECRUITING)
            return false
        }

        ToaPartyManager.withdrawApplication(player)
        if (existing != null && ToaPartyManager.leaveParty(player)) {
            ToaPartyManager.refreshViewers(existing, exclude = player)
        }

        if (party.apply(player)) {
            player.appliedParty = party
            player.mes("You have applied to join the party of ${party.leaderName}.")
            if (player.currentTab != 1) player.currentTab = 1
            return true
        } else {
            mesbox(NO_LONGER_RECRUITING)
            return false
        }
    }

    private suspend fun ProtectedAccess.handleLeave(party: ToaLobbyParty): Boolean {
        if (party.insideRaid) {
            mesbox(JOIN_PARTY_IN_TOMBS)
            return true
        }
        val leaderName = party.leaderName
        if (ToaPartyManager.leaveParty(player)) {
            player.mes("You have left the party of $leaderName.")
            ToaPartyManager.refreshViewers(party, exclude = player)
        }
        return false
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

    private fun ProtectedAccess.handleMemberClick(party: ToaLobbyParty, comsub: Int): Boolean {
        if (!party.isLeader(player)) return true
        if (comsub < 0 || comsub >= party.members.size) return true

        val target = party.members[comsub]
        if (target == player) {
            handleDisband(party)
            return false
        }

        ToaPartyManager.kickMember(target)
        target.mes("You have been kicked from the party of ${player.displayName}.")
        player.mes("You have kicked ${target.displayName} from your party.")
        return true
    }

    private fun ProtectedAccess.handleApplicantClick(party: ToaLobbyParty, comsub: Int) {
        if (!party.isLeader(player)) return

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
                target.mes(
                    "Your application to the party of ${party.leaderName} has been accepted."
                )
            }
        } else {
            if (party.decline(target)) {
                target.appliedParty = null
                player.mes("You have declined the party application from ${target.displayName}.")
                target.mes(
                    "Your application to the party of ${party.leaderName} has been declined."
                )
                ToaPartyManager.refreshDetailsView(target)
            }
        }
    }

    private fun ProtectedAccess.handleInvocationToggle(party: ToaLobbyParty, comsub: Int) {
        if (!party.isLeader(player)) return

        val invocations = ToaInvocation.ALL
        if (comsub < 0 || comsub >= invocations.size) return

        val invocation = invocations[comsub]
        if (invocation.eventOnly) return
        val settings = party.settings

        if (settings.isActive(invocation)) {
            deactivateWithDependents(settings, invocation)
            soundSynth(SYNTH_INVOCATION_OFF)
        } else if (tryActivate(settings, invocation)) {
            soundSynth(SYNTH_INVOCATION_ON)
        }
    }

    private fun deactivateWithDependents(settings: ToaPartySettings, invocation: ToaInvocation) {
        for (dependent in invocation.dependents) {
            if (settings.isActive(dependent)) {
                deactivateWithDependents(settings, dependent)
            }
        }
        settings.unflag(invocation)
    }

    private fun ProtectedAccess.tryActivate(
        settings: ToaPartySettings,
        invocation: ToaInvocation,
    ): Boolean {
        val prerequisite = invocation.prerequisite
        if (prerequisite != null && !settings.isActive(prerequisite)) {
            player.mes(
                "You cannot activate this invocation without first enabling " +
                    "<col=ff0000>${prerequisite.name}</col>."
            )
            return false
        }

        val category = invocation.category
        if (category in SINGLE_SELECT_CATEGORIES) {
            settings.unflagCategory(category)
        }

        settings.flag(invocation)
        return true
    }

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
    private fun presetVarp(slot: Int, part: Int): String =
        "varp.toa_invocations_preset_${slot + 1}${PRESET_PARTS[part]}"

    private fun ProtectedAccess.readPreset(slot: Int): IntArray {
        require(slot in 0 until PRESET_SLOTS) { "Invalid preset slot $slot" }
        return IntArray(PRESET_PARTS.size) { part -> vars[presetVarp(slot, part)] }
    }

    private fun ProtectedAccess.writePreset(slot: Int, bitmaps: IntArray) {
        require(slot in 0 until PRESET_SLOTS) { "Invalid preset slot $slot" }
        for (part in PRESET_PARTS.indices) {
            vars[presetVarp(slot, part)] = bitmaps[part]
        }
    }

    private fun ProtectedAccess.handleUnblock(party: ToaLobbyParty) {
        if (!party.isLeader(player)) return
        party.blockedPlayers.clear()
        player.mes("All players rejected from this party have been unblocked and may apply again.")
    }

    private suspend fun ProtectedAccess.handleSetCompletions(party: ToaLobbyParty) {
        if (!party.isLeader(player)) return

        val value =
            countDialog("Set a preferred number of completions up to 100 (or 0 to clear it):")
        party.settings.kcRequirement = value.coerceIn(0, 100)
    }

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
        sb.append(ToaKillCount.summary(target))
        return sb.toString()
    }
}
