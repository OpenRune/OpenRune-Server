package org.rsmod.api.account.character.main

import dev.or2.central.account.AccountData
import dev.or2.central.account.Rights
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ModLevelType
import jakarta.inject.Inject
import java.time.LocalDateTime
import org.rsmod.api.account.character.CharacterAccountLoginSegment
import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

public class CharacterAccountApplier @Inject constructor() :
    CharacterDataStage.Applier<CharacterAccountLoginSegment> {
    override fun apply(player: Player, data: CharacterAccountLoginSegment) {
        val d = data.wrapped
        val c = d.characterData
        player.accountId = d.accountId
        player.characterId = c.characterId

        val accountHash = (d.accountId.toLong() shl 32) or c.characterId.toLong()
        val userHash = d.accountName.hashCode().toLong()
        player.userId = c.characterId.toLong()
        player.accountHash = accountHash
        player.userHash = userHash

        val uuid = c.characterId.toLong()
        player.uuid = uuid
        player.observerUUID = uuid

        player.trustedDevices = d.trustedDevices.toMutableList()
        player.twoFactorAuth = d.twoFactorAuth
        player.lastKnownDevice = d.trustedDevices.maxByOrNull { it.verifiedAt }?.deviceId
        player.members = c.members
        player.username = d.accountName
        player.displayName = c.displayName ?: ""
        player.previousDisplayName = c.previousDisplayName ?: ""
        player.displayNameChangedAtMillis = c.displayNameChangedAtMillis
        player.discordId = d.discordId
        player.coords = CoordGrid(c.coordX, c.coordZ, c.coordLevel)
        player.createdAt = c.createdAt
        player.runEnergy = c.runEnergy
        player.xpRate = c.xpRate
        player.lastLogin = LocalDateTime.now()
        player.vars.backing.putAll(c.varps)
        if (c.attrs.isNotEmpty()) {
            player.attr.putAllFromPersistence(c.attrs)
        }
        player.assignModLevel(d)
    }

    private fun Player.assignModLevel(d: AccountData) {
        val levels = ServerCacheManager.getModelLevels().values
        val defaultLevel = levels.first()
        modLevel = resolveModLevelFromRights(d.rights) ?: defaultLevel
    }

    public companion object {
        public fun resolveModLevelFromRights(rights: Rights): ModLevelType? =
            when (rights) {
                Rights.ADMINISTRATOR ->
                    ServerCacheManager.getModLevel("modlevel.admin".asRSCM(RSCMType.MODLEVEL))
                Rights.MOD ->
                    ServerCacheManager.getModLevel("modlevel.moderator".asRSCM(RSCMType.MODLEVEL))
                Rights.NONE -> null
            }
    }
}

