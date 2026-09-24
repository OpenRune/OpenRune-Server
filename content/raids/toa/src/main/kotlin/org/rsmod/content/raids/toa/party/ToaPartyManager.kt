package org.rsmod.content.raids.toa.party

import org.rsmod.api.attr.AttributeKey
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
        addParty(party)
        return party
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
        party.removeMember(player)
        player.currentParty = null
        if (party.members.isEmpty()) {
            removeParty(party)
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
     * Clears all party state for a player. Called on logout.
     */
    fun onLogout(player: Player) {
        // Drop every reference other parties hold to this player, so the
        // global registry never keeps a logged-out Player alive.
        for (party in lobbyParties) {
            party.withdraw(player)
            party.unblock(player)
        }
        player.appliedParty = null
        // Leave any party (passes leadership on, or removes the party if now empty)
        leaveParty(player)
        // Clear viewing state
        player.viewingParty = null
        player.currentTab = 0
    }
}
