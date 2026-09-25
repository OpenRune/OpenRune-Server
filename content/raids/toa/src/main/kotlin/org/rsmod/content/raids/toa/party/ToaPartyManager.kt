package org.rsmod.content.raids.toa.party

import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.ui.ifSetText
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.content.raids.toa.raid.ToaRaidManager
import org.rsmod.game.entity.Player

/**
 * Global registry for TOA lobby parties and per-player party state.
 *
 * Per-player transient state is stored on [Player.attr] using
 * [AttributeKey]s — it's cleared automatically on logout.
 */
object ToaPartyManager {

    // ---- Per-player attribute keys (transient, cleared on logout) ----

    /** The party this player is a member of. */
    private val CURRENT_PARTY = AttributeKey<ToaLobbyParty>()

    /** The party this player is currently viewing in the management UI. */
    private val VIEWING_PARTY = AttributeKey<ToaLobbyParty>()

    /** The party this player has applied to. */
    private val APPLIED_PARTY = AttributeKey<ToaLobbyParty>()

    /** Which tab the player has selected in the management interface. */
    private val CURRENT_TAB = AttributeKey<Int>()

    // ---- Client lobby-HUD status (toa_lobby header: 0 = No Party, 1 = Party, 2 = Step Inside Now!) ----

    const val PARTY_STATUS_NONE = 0
    const val PARTY_STATUS_IN_PARTY = 1
    /** 773 header "Step Inside Now!": your leader has started the raid. */
    const val PARTY_STATUS_STEP_INSIDE = 2

    private var Player.clientPartyStatus by intVarBit("varbit.toa_client_partystatus")

    // ---- Lobby HUD overlay (773) ----
    // The header ("No Party" / "Party") is drawn by the client from the varbit above.
    // The names list below it is plain text the server sends: 8 lines joined by <br>,
    // "-" for an empty slot.

    const val LOBBY_HUD = "interface.toa_lobby"
    private const val LOBBY_HUD_NAMES = "component.toa_lobby:names"
    private val EMPTY_PARTY_NAMES = Array(ToaLobbyParty.MAX_PARTY_MEMBERS) { "-" }.joinToString("<br>")

    // ---- Personal settings (server-only custom varps, see pack/configs/toa_vars.toml) ----
    // The invocations + completion requirement a player's next party starts with.

    private var Player.personalInvocationsA by intVarp("varp.toa_personal_invocations_a")
    private var Player.personalInvocationsB by intVarp("varp.toa_personal_invocations_b")
    private var Player.personalInvocationsC by intVarp("varp.toa_personal_invocations_c")
    private var Player.personalKcRequirement by intVarp("varp.toa_personal_kc_requirement")

    // ---- View value constants (sent to CS2 script 6729) ----
    // Must match vanilla CS2 expectations. Single source of truth.

    const val VIEW_NON_MEMBER = 0
    const val VIEW_MEMBER = 1
    const val VIEW_LEADER = 2
    const val VIEW_APPLICANT = 3
    const val VIEW_KICKED = 4

    // ---- Global party list ----

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

    // ---- Per-player accessors ----

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

    var Player.currentTab: Int
        get() = attr.getOrDefault(CURRENT_TAB, 0)
        set(value) { attr[CURRENT_TAB] = value }

    // ---- High-level operations ----

    /**
     * Creates a new party with the given player as leader, using
     * their current personal invocation settings.
     * Returns the party, or `null` if the lobby is full or the
     * player is already in a party.
     */
    fun createParty(player: Player, settings: ToaPartySettings, currentCycle: Int): ToaLobbyParty? {
        if (isLobbyFull()) return null
        if (player.currentParty != null) return null
        val party = ToaLobbyParty(player, currentCycle)
        party.settings = settings
        player.currentParty = party
        player.clientPartyStatus = PARTY_STATUS_IN_PARTY
        addParty(party)
        sendLobbyHud(player)
        return party
    }

    /**
     * Moves an applicant into the party. Returns `false` if they weren't
     * an applicant (or the party is full).
     */
    fun acceptApplicant(party: ToaLobbyParty, target: Player): Boolean {
        if (!party.accept(target)) return false
        target.appliedParty = null
        target.currentParty = party
        target.clientPartyStatus = PARTY_STATUS_IN_PARTY
        refreshLobbyHud(party) // includes the new member
        return true
    }

    /** Removes a member from the party at the leader's request. */
    fun kickMember(party: ToaLobbyParty, target: Player) {
        party.removeMember(target)
        target.currentParty = null
        target.clientPartyStatus = PARTY_STATUS_NONE
        sendLobbyHud(target) // back to "-" lines
        refreshLobbyHud(party)
        refreshDetailsView(target)
    }

    // ---- Lobby HUD ----

    /**
     * Sends [player]'s names list to their lobby HUD: their party's members,
     * or eight "-" lines if they aren't in one.
     *
     * Does nothing if the HUD isn't open (the player is outside the lobby).
     * The lobby script calls this again when they walk in, so they always get
     * the current list then.
     */
    fun sendLobbyHud(player: Player) {
        if (!player.ui.containsOverlay(LOBBY_HUD)) return
        val text = player.currentParty?.buildPartyString() ?: EMPTY_PARTY_NAMES
        player.ifSetText(LOBBY_HUD_NAMES, text)
    }

    /** Sends the names list to every member of [party]. */
    fun refreshLobbyHud(party: ToaLobbyParty) {
        for (member in party.members) {
            sendLobbyHud(member)
        }
    }

    // ---- Cross-player refresh ----

    /**
     * Refreshes the party-details screen (774) of every member and applicant
     * of [party], except [exclude] (normally the player who made the change,
     * whose own loop reopens 774 anyway).
     */
    fun refreshViewers(party: ToaLobbyParty, exclude: Player? = null) {
        // Copy first: a refreshed player's loop may change the party lists.
        val viewers = party.members.toList() + party.applicants.toList()
        for (viewer in viewers) {
            if (viewer != exclude) refreshDetailsView(viewer)
        }
    }

    /**
     * Refreshes one player's 774 screen, if they have it open.
     *
     * Their loop is suspended in `pauseButton()`, so we resume it with the
     * same input a real click on the Refresh button (774:1, sub 1) produces.
     * Their loop then closes and reopens 774 with fresh data. If they are in
     * a dialog instead (choice, count input), 774 is closed or the coroutine
     * isn't waiting on a pause button, so nothing happens.
     */
    fun refreshDetailsView(viewer: Player) {
        if (!viewer.ui.containsModal("interface.toa_partydetails")) return
        val coroutine = viewer.activeCoroutine ?: return
        if (!coroutine.isAwaiting(ResumePauseButtonInput::class)) return
        viewer.resumeActiveCoroutine(ResumePauseButtonInput(REFRESH_COMPONENT, REFRESH_SUBCOMPONENT))
    }

    private const val REFRESH_COMPONENT = "component.toa_partydetails:pausebuttons"
    private const val REFRESH_SUBCOMPONENT = 1

    /** Builds a new party's settings from the player's saved personal settings. */
    fun loadPersonalSettings(player: Player): ToaPartySettings {
        val settings = ToaPartySettings()
        settings.loadPreset(
            intArrayOf(player.personalInvocationsA, player.personalInvocationsB, player.personalInvocationsC)
        )
        settings.kcRequirement = player.personalKcRequirement
        return settings
    }

    /** Stores the given party settings as the player's personal settings. */
    fun savePersonalSettings(player: Player, settings: ToaPartySettings) {
        val bitmaps = settings.invocationBitmaps
        player.personalInvocationsA = bitmaps[0]
        player.personalInvocationsB = bitmaps[1]
        player.personalInvocationsC = bitmaps[2]
        player.personalKcRequirement = settings.kcRequirement
    }

    /**
     * Calculates the viewing value for a player relative to a party.
     * This controls what the client shows (leader buttons, apply
     * button, leave button, etc).
     */
    fun resolveViewingValue(player: Player, party: ToaLobbyParty): Int {
        return when {
            party.isLeader(player) -> VIEW_LEADER
            party.isMember(player) -> VIEW_MEMBER
            party.isApplicant(player) -> VIEW_APPLICANT
            party.isBlocked(player) -> VIEW_KICKED
            else -> VIEW_NON_MEMBER
        }
    }

    /**
     * Handles a player leaving their current party cleanly.
     * Removes them from the party, clears their attributes,
     * and removes the party from the lobby if it's now empty.
     * Returns `true` if they were in a party.
     */
    fun leaveParty(player: Player): Boolean {
        val party = player.currentParty ?: return false
        val wasLeader = party.isLeader(player)
        party.removeMember(player)
        player.currentParty = null
        player.clientPartyStatus = PARTY_STATUS_NONE
        sendLobbyHud(player)
        // If the party has a raid running, a member who never entered loses
        // their raid slot too.
        ToaRaidManager.onLeftParty(player, party)
        if (party.members.isEmpty()) {
            removeParty(party)
        } else {
            refreshLobbyHud(party)
            if (wasLeader) {
                // Leadership passed on: the party's settings become the new leader's own.
                party.leader?.let { savePersonalSettings(it, party.settings) }
            }
        }
        return true
    }

    /**
     * Fully disbands a party — removes all members, notifies
     * applicants, and removes from the lobby list.
     * Returns the lists of affected players for the caller
     * to send UI updates to.
     */
    fun disbandParty(party: ToaLobbyParty): DisbandResult {
        val members = party.members.toList()
        val applicants = party.applicants.toList()
        val blocked = party.blockedPlayers.toList()

        for (member in members) {
            member.currentParty = null
            member.clientPartyStatus = PARTY_STATUS_NONE
            sendLobbyHud(member)
        }
        for (applicant in applicants) {
            applicant.appliedParty = null
        }
        for (blockedPlayer in blocked) {
            blockedPlayer.appliedParty = null
        }

        party.members.clear()
        party.applicants.clear()
        party.blockedPlayers.clear()
        removeParty(party)

        return DisbandResult(members, applicants, blocked)
    }

    /**
     * Result of disbanding a party. The caller uses these lists
     * to send appropriate messages and UI updates.
     */
    data class DisbandResult(
        val formerMembers: List<Player>,
        val formerApplicants: List<Player>,
        val formerBlocked: List<Player>,
    )

    /**
     * Called when a player leaves the lobby area (walking out, teleporting,
     * or logging out). Mirrors Offline_Scape's TOALobbyArea.leave(): withdraws
     * any application and removes them from their party.
     *
     * Returns `true` if they were removed from a party, so the caller can tell
     * them why.
     *
     * Blocked lists are left alone: a declined player stays declined if they
     * walk out and back in. Logout clears those separately (see [onLogout]).
     */
    fun onLeaveLobby(player: Player): Boolean {
        val appliedTo = player.appliedParty
        if (appliedTo != null) {
            appliedTo.withdraw(player)
            player.appliedParty = null
            refreshViewers(appliedTo, exclude = player)
        }

        val party = player.currentParty
        val left = leaveParty(player)
        if (party != null && left) {
            refreshViewers(party, exclude = player)
        }

        player.viewingParty = null
        return left
    }

    // ---- Raid start ----

    /** Sets the 773 header state (see the PARTY_STATUS_* constants). */
    fun setPartyStatus(player: Player, status: Int) {
        player.clientPartyStatus = status
    }

    /**
     * Called when the leader starts the raid. Mirrors Offline_Scape's
     * `currentLobbyParty.removeFromList()`:
     * - the party leaves the 772 list, so nobody new can find or join it;
     * - pending applicants are dropped (their 774 view refreshes);
     * - the other members' 773 headers switch to "Step Inside Now!".
     *
     * Members stay in the party: vanilla keeps toa_client_partystatus at 1
     * throughout the raid, and only leaving the raid removes you.
     */
    fun onRaidStarted(party: ToaLobbyParty) {
        removeParty(party)
        val applicants = party.applicants.toList()
        for (applicant in applicants) {
            party.withdraw(applicant)
            if (applicant.appliedParty == party) applicant.appliedParty = null
            refreshDetailsView(applicant)
        }
        for (member in party.members) {
            if (!party.isLeader(member)) {
                member.clientPartyStatus = PARTY_STATUS_STEP_INSIDE
            }
        }
    }

    /**
     * Clears all party state for a player. Called on logout.
     */
    fun onLogout(player: Player) {
        // Drop every reference other parties hold to this player, so the
        // global registry never keeps a logged-out Player alive.
        for (party in lobbyParties) {
            party.withdraw(player)
            party.unblock(player)
        }
        val appliedTo = player.appliedParty
        player.appliedParty = null
        appliedTo?.let { refreshViewers(it, exclude = player) }
        // Leave any party (passes leadership on, or removes the party if now empty)
        val party = player.currentParty
        leaveParty(player)
        party?.let { refreshViewers(it, exclude = player) }
        // Clear viewing state
        player.viewingParty = null
        player.currentTab = 0
    }
}
