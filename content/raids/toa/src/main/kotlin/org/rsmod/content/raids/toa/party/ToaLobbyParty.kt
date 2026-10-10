package org.rsmod.content.raids.toa.party

import org.rsmod.game.entity.Player

class ToaLobbyParty(leader: Player, creationCycle: Int) {

    val members: MutableList<Player> = mutableListOf()

    val applicants: MutableList<Player> = mutableListOf()

    val blockedPlayers: MutableList<Player> = mutableListOf()

    var settings: ToaPartySettings = ToaPartySettings()

    var leaderName: String = leader.displayName
        private set

    val creationCycle: Int = creationCycle

    var insideRaid: Boolean = false
        private set

    init {
        members.add(leader)
    }

    val leader: Player? get() = members.firstOrNull()

    val size: Int get() = members.size

    fun isLeader(player: Player): Boolean = members.firstOrNull() == player

    fun isMember(player: Player): Boolean = player in members

    fun isApplicant(player: Player): Boolean = player in applicants

    fun isBlocked(player: Player): Boolean = player in blockedPlayers

    fun isFull(): Boolean = members.size >= MAX_PARTY_MEMBERS

    fun addMember(player: Player): Boolean {
        if (isFull() || isMember(player)) return false
        members.add(player)
        return true
    }

    fun removeMember(player: Player): Boolean {
        if (!members.remove(player)) return false
        if (leaderName == player.displayName && members.isNotEmpty()) {
            val newLeader = members.first()
            leaderName = newLeader.displayName
        }
        return true
    }

    fun apply(player: Player): Boolean {
        if (insideRaid || isBlocked(player) || isApplicant(player) || isFull()) return false
        applicants.add(player)
        return true
    }

    fun withdraw(player: Player): Boolean {
        return applicants.remove(player)
    }

    fun accept(player: Player): Boolean {
        if (!applicants.remove(player)) return false
        if (isFull()) return false
        members.add(player)
        return true
    }

    fun decline(player: Player): Boolean {
        if (!applicants.remove(player)) return false
        if (!isBlocked(player)) {
            blockedPlayers.add(player)
        }
        return true
    }

    fun unblock(player: Player): Boolean {
        return blockedPlayers.remove(player)
    }

    fun lockForRaid() {
        insideRaid = true
    }

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
