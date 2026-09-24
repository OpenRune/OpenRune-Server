package org.rsmod.content.raids.toa.party

import org.rsmod.game.entity.Player

/**
 * A TOA party in the lobby, before the raid starts.
 *
 * Tracks members, applicants, blocked players, and the party's
 * invocation settings. The first player in [members] is the leader.
 */
class ToaLobbyParty(leader: Player, creationCycle: Int) {

    /** Party members, in join order. Index 0 is always the leader. */
    val members: MutableList<Player> = mutableListOf()

    /** Players who have applied but not yet been accepted/declined. */
    val applicants: MutableList<Player> = mutableListOf()

    /** Players who were declined — cannot re-apply. */
    val blockedPlayers: MutableList<Player> = mutableListOf()

    /** The party's invocation/raid-level settings. Owned by the leader. */
    var settings: ToaPartySettings = ToaPartySettings()

    /** Display name of the leader (kept for the party list UI). */
    var leaderName: String = leader.displayName
        private set

    /**
     * Server tick (map clock) when the party was created. The party list
     * CS2 expects the age in ticks (it converts with scale(60, 100, ...)).
     */
    val creationCycle: Int = creationCycle

    init {
        members.add(leader)
    }

    // ---- Queries ----

    val leader: Player? get() = members.firstOrNull()

    val size: Int get() = members.size

    fun isLeader(player: Player): Boolean = members.firstOrNull() == player

    fun isMember(player: Player): Boolean = player in members

    fun isApplicant(player: Player): Boolean = player in applicants

    fun isBlocked(player: Player): Boolean = player in blockedPlayers

    fun isFull(): Boolean = members.size >= MAX_PARTY_MEMBERS

    // ---- Member operations ----

    /**
     * Adds a player as a member. Returns `true` if successful.
     */
    fun addMember(player: Player): Boolean {
        if (isFull() || isMember(player)) return false
        members.add(player)
        return true
    }

    /**
     * Removes a member. If the leader leaves, leadership passes to
     * the next member. Returns `true` if the player was a member.
     */
    fun removeMember(player: Player): Boolean {
        if (!members.remove(player)) return false
        if (leaderName == player.displayName && members.isNotEmpty()) {
            val newLeader = members.first()
            leaderName = newLeader.displayName
        }
        return true
    }

    // ---- Applicant operations ----

    /**
     * Adds a player as an applicant. Fails if they're blocked,
     * already applied, or the party is full.
     */
    fun apply(player: Player): Boolean {
        if (isBlocked(player) || isApplicant(player) || isFull()) return false
        applicants.add(player)
        return true
    }

    /**
     * Withdraws an application. Returns `true` if they were an applicant.
     */
    fun withdraw(player: Player): Boolean {
        return applicants.remove(player)
    }

    /**
     * Accepts an applicant — removes them from applicants and adds
     * them as a member. Returns `true` if successful.
     */
    fun accept(player: Player): Boolean {
        if (!applicants.remove(player)) return false
        if (isFull()) return false
        members.add(player)
        return true
    }

    /**
     * Declines an applicant and blocks them from re-applying.
     * Returns `true` if they were an applicant.
     */
    fun decline(player: Player): Boolean {
        if (!applicants.remove(player)) return false
        if (!isBlocked(player)) {
            blockedPlayers.add(player)
        }
        return true
    }

    /**
     * Unblocks a previously declined player.
     */
    fun unblock(player: Player): Boolean {
        return blockedPlayers.remove(player)
    }

    // ---- Party string (for the lobby HUD on interface 773) ----

    /**
     * Builds the pipe-delimited member name string for the lobby
     * party overlay (interface 773, component 5).
     * Empty slots show "-".
     */
    fun buildPartyString(): String {
        val names = Array(MAX_PARTY_MEMBERS) { "-" }
        for (i in members.indices) {
            if (i < MAX_PARTY_MEMBERS) {
                names[i] = members[i].displayName
            }
        }
        return names.joinToString("<br>")
    }

    companion object {
        const val MAX_PARTY_MEMBERS = 8
        const val MAX_LOBBY_PARTIES = 45
    }
}
