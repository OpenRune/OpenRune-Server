package org.rsmod.content.raids.toa.party

import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.ifSetText
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.content.raids.toa.raid.ToaRaidManager
import org.rsmod.game.entity.Player

private const val LOBBY_HUD_TARGET = "component.toplevel_osrs_stretch:overlay_hud"

internal fun ProtectedAccess.openLobbyHud() {
    ifOpenOverlay(ToaPartyManager.LOBBY_HUD, LOBBY_HUD_TARGET)
    ToaPartyManager.sendLobbyHud(player)
}

object ToaPartyManager {

    private val CURRENT_PARTY = AttributeKey<ToaLobbyParty>()
    private val VIEWING_PARTY = AttributeKey<ToaLobbyParty>()
    private val APPLIED_PARTY = AttributeKey<ToaLobbyParty>()

    const val PARTY_STATUS_NONE = 0
    const val PARTY_STATUS_IN_PARTY = 1
    const val PARTY_STATUS_STEP_INSIDE = 2

    private var Player.clientPartyStatus by intVarBit("varbit.toa_client_partystatus")

    const val LOBBY_HUD = "interface.toa_lobby"
    private const val LOBBY_HUD_NAMES = "component.toa_lobby:names"
    private val EMPTY_PARTY_NAMES =
        Array(ToaLobbyParty.MAX_PARTY_MEMBERS) { "-" }.joinToString("<br>")

    private var Player.personalInvocationsA by intVarp("varp.toa_personal_invocations_a")
    private var Player.personalInvocationsB by intVarp("varp.toa_personal_invocations_b")
    private var Player.personalInvocationsC by intVarp("varp.toa_personal_invocations_c")
    private var Player.personalKcRequirement by intVarp("varp.toa_personal_kc_requirement")

    const val VIEW_NON_MEMBER = 0
    const val VIEW_MEMBER = 1
    const val VIEW_LEADER = 2
    const val VIEW_APPLICANT = 3
    const val VIEW_KICKED = 4

    private val lobbyParties: MutableList<ToaLobbyParty> = mutableListOf()

    fun allParties(): List<ToaLobbyParty> = lobbyParties

    fun partyCount(): Int = lobbyParties.size

    fun isLobbyFull(): Boolean = lobbyParties.size >= ToaLobbyParty.MAX_LOBBY_PARTIES

    fun getPartyByIndex(index: Int): ToaLobbyParty? = lobbyParties.getOrNull(index)

    fun partyExists(party: ToaLobbyParty): Boolean = party in lobbyParties

    fun addParty(party: ToaLobbyParty) {
        lobbyParties.add(party)
    }

    fun removeParty(party: ToaLobbyParty) {
        lobbyParties.remove(party)
    }

    var Player.currentParty: ToaLobbyParty?
        get() = attr[CURRENT_PARTY]
        set(value) {
            if (value != null) attr[CURRENT_PARTY] = value else attr.remove(CURRENT_PARTY)
        }

    var Player.viewingParty: ToaLobbyParty?
        get() = attr[VIEWING_PARTY]
        set(value) {
            if (value != null) attr[VIEWING_PARTY] = value else attr.remove(VIEWING_PARTY)
        }

    var Player.appliedParty: ToaLobbyParty?
        get() = attr[APPLIED_PARTY]
        set(value) {
            if (value != null) attr[APPLIED_PARTY] = value else attr.remove(APPLIED_PARTY)
        }

    var Player.currentTab by intVarBit("varbit.toa_party_tab")

    fun createParty(player: Player, settings: ToaPartySettings, currentCycle: Int): ToaLobbyParty? {
        if (isLobbyFull()) return null
        if (player.currentParty != null) return null
        withdrawApplication(player)
        val party = ToaLobbyParty(player, currentCycle)
        party.settings = settings
        player.currentParty = party
        player.clientPartyStatus = PARTY_STATUS_IN_PARTY
        addParty(party)
        sendLobbyHud(player)
        return party
    }

    fun acceptApplicant(party: ToaLobbyParty, target: Player): Boolean {
        if (!party.accept(target)) return false
        target.appliedParty = null
        target.currentParty = party
        target.clientPartyStatus = PARTY_STATUS_IN_PARTY
        refreshLobbyHud(party)
        return true
    }

    fun kickMember(target: Player) {
        leaveParty(target)
        refreshDetailsView(target)
    }

    fun withdrawApplication(player: Player) {
        val appliedTo = player.appliedParty ?: return
        appliedTo.withdraw(player)
        player.appliedParty = null
        refreshViewers(appliedTo, exclude = player)
    }

    fun sendLobbyHud(player: Player) {
        if (!player.ui.containsOverlay(LOBBY_HUD)) return
        val text = player.currentParty?.buildPartyString() ?: EMPTY_PARTY_NAMES
        player.ifSetText(LOBBY_HUD_NAMES, text)
    }

    fun refreshLobbyHud(party: ToaLobbyParty) {
        for (member in party.members) {
            sendLobbyHud(member)
        }
    }

    fun refreshViewers(party: ToaLobbyParty, exclude: Player? = null) {
        val viewers = party.members.toList() + party.applicants.toList()
        for (viewer in viewers) {
            if (viewer != exclude) refreshDetailsView(viewer)
        }
    }

    fun refreshDetailsView(viewer: Player) {
        if (!viewer.ui.containsModal("interface.toa_partydetails")) return
        val coroutine = viewer.activeCoroutine ?: return
        if (!coroutine.isAwaiting(ResumePauseButtonInput::class)) return
        val input = ResumePauseButtonInput(REFRESH_COMPONENT, REFRESH_SUBCOMPONENT)
        viewer.resumeActiveCoroutine(input)
    }

    private const val REFRESH_COMPONENT = "component.toa_partydetails:pausebuttons"
    private const val REFRESH_SUBCOMPONENT = 1

    fun loadPersonalSettings(player: Player): ToaPartySettings {
        val settings = ToaPartySettings()
        settings.loadPreset(
            intArrayOf(
                player.personalInvocationsA,
                player.personalInvocationsB,
                player.personalInvocationsC,
            )
        )
        settings.kcRequirement = player.personalKcRequirement
        return settings
    }

    fun savePersonalSettings(player: Player, settings: ToaPartySettings) {
        val bitmaps = settings.invocationBitmaps
        player.personalInvocationsA = bitmaps[0]
        player.personalInvocationsB = bitmaps[1]
        player.personalInvocationsC = bitmaps[2]
        player.personalKcRequirement = settings.kcRequirement
    }

    fun resolveViewingValue(player: Player, party: ToaLobbyParty): Int {
        return when {
            party.isLeader(player) -> VIEW_LEADER
            party.isMember(player) -> VIEW_MEMBER
            party.isApplicant(player) -> VIEW_APPLICANT
            party.isBlocked(player) -> VIEW_KICKED
            else -> VIEW_NON_MEMBER
        }
    }

    fun leaveParty(player: Player): Boolean {
        val party = player.currentParty ?: return false
        val wasLeader = party.isLeader(player)
        party.removeMember(player)
        player.currentParty = null
        player.clientPartyStatus = PARTY_STATUS_NONE
        sendLobbyHud(player)
        ToaRaidManager.onLeftParty(player, party)
        if (party.members.isEmpty()) {
            removeParty(party)
            for (applicant in releaseApplicants(party)) {
                applicant.mes("The party to which you were applying has disbanded.")
            }
        } else {
            refreshLobbyHud(party)
            if (wasLeader && !party.insideRaid) {
                party.leader?.let { savePersonalSettings(it, party.settings) }
            }
        }
        return true
    }

    fun disbandParty(party: ToaLobbyParty): DisbandResult {
        val members = party.members.toList()
        val applicants = party.applicants.toList()
        val blocked = party.blockedPlayers.toList()

        for (member in members) {
            member.currentParty = null
            member.clientPartyStatus = PARTY_STATUS_NONE
            sendLobbyHud(member)
            ToaRaidManager.onLeftParty(member, party)
        }
        for (applicant in applicants) {
            if (applicant.appliedParty == party) applicant.appliedParty = null
        }

        party.members.clear()
        party.applicants.clear()
        party.blockedPlayers.clear()
        removeParty(party)

        return DisbandResult(members, applicants, blocked)
    }

    data class DisbandResult(
        val formerMembers: List<Player>,
        val formerApplicants: List<Player>,
        val formerBlocked: List<Player>,
    )

    fun onLeaveLobby(player: Player): Boolean {
        withdrawApplication(player)

        val party = player.currentParty
        val left = leaveParty(player)
        if (party != null && left) {
            refreshViewers(party, exclude = player)
        }

        player.viewingParty = null
        return left
    }

    fun setPartyStatus(player: Player, status: Int) {
        player.clientPartyStatus = status
    }

    fun onRaidStarted(party: ToaLobbyParty) {
        party.lockForRaid()
        removeParty(party)
        releaseApplicants(party)
        for (member in party.members) {
            if (!party.isLeader(member)) {
                member.clientPartyStatus = PARTY_STATUS_STEP_INSIDE
            }
        }
    }

    private fun releaseApplicants(party: ToaLobbyParty): List<Player> {
        val applicants = party.applicants.toList()
        party.applicants.clear()
        for (applicant in applicants) {
            if (applicant.appliedParty == party) applicant.appliedParty = null
            refreshDetailsView(applicant)
        }
        return applicants
    }

    fun onLogout(player: Player) {
        for (party in lobbyParties) {
            party.withdraw(player)
            party.unblock(player)
        }
        withdrawApplication(player)
        val party = player.currentParty
        leaveParty(player)
        party?.let { refreshViewers(it, exclude = player) }
        player.viewingParty = null
        player.currentTab = 0
    }
}
