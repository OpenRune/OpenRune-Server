package org.rsmod.api.account.character.main

import java.time.LocalDateTime
import org.rsmod.api.account.character.CharacterDataStage

public data class CharacterAccountData(
    val realm: Int,
    val accountId: Int,
    val characterId: Int,
    val loginName: String,
    val displayName: String?,
    val previousDisplayName: String?,
    val displayNameChangedAt: LocalDateTime?,
    val hashedPassword: String,
    val email: String?,
    val members: Boolean,
    val modLevel: String?,
    val twofaEnabled: Boolean,
    val twofaSecret: String?,
    val twofaLastVerified: LocalDateTime?,
    val knownDevice: Int?,
    val worldId: Int?,
    val coordX: Int,
    val coordZ: Int,
    val coordLevel: Int,
    val varps: Map<Int, Int>,
    val createdAt: LocalDateTime?,
    val lastLogin: LocalDateTime?,
    val lastLogout: LocalDateTime?,
    val mutedUntil: LocalDateTime?,
    val bannedUntil: LocalDateTime?,
    val runEnergy: Int,
    val xpRate: Double,
    val attrs: Map<String, Any>,
) : CharacterDataStage.Segment {
    // Do not include sensitive fields (e.g., password hash, 2fa secret, known device).
    override fun toString(): String =
        "AccountData(" +
            "realm=$realm, " +
            "accountId=$accountId, " +
            "characterId=$characterId, " +
            "loginName=$loginName, " +
            "displayName=$displayName, " +
            "previousDisplayName=$previousDisplayName, " +
            "displayNameChangedAt=$displayNameChangedAt, " +
            "email=$email, " +
            "members=$members, " +
            "modLevel='$modLevel', " +
            "twofaEnabled=$twofaEnabled, " +
            "twofaLastVerified=$twofaLastVerified, " +
            "worldId=$worldId, " +
            "coordX=$coordX, " +
            "coordZ=$coordZ, " +
            "coordLevel=$coordLevel, " +
            "createdAt=$createdAt, " +
            "lastLogin=$lastLogin, " +
            "lastLogout=$lastLogout, " +
            "mutedUntil=$mutedUntil, " +
            "bannedUntil=$bannedUntil, " +
            "xpRate=$xpRate" +
            ")"
}
