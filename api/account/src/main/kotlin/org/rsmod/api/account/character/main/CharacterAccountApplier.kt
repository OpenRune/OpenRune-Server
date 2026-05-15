package org.rsmod.api.account.character.main

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

        val device = d.knownDevice
        player.lastKnownDevice = device
        player.members = c.members
        player.username = d.accountName
        player.displayName = c.displayName ?: ""
        player.previousDisplayName = c.previousDisplayName ?: ""
        player.displayNameChangedAtMillis = c.displayNameChangedAtMillis
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

    private fun Player.assignModLevel(d: dev.or2.central.account.AccountData) {
        val levels = ServerCacheManager.getModelLevels().values
        val defaultLevel = levels.first()
        modLevel = resolveModLevelFromRights(d.rights) ?: defaultLevel
    }

    public companion object {
        private val RIGHTS_MOD_LEVEL_PRIORITY =
            arrayOf("modlevel.owner", "modlevel.admin", "modlevel.moderator", "modlevel.player")

        public fun resolveModLevelFromRights(rights: String): ModLevelType? {
            if (rights.isBlank()) {
                return null
            }
            val tokens =
                rights.split(',').map { it.trim() }.filter { it.isNotEmpty() }.map { it.lowercase() }
            for (internal in RIGHTS_MOD_LEVEL_PRIORITY) {
                if (tokens.contains(internal.lowercase())) {
                    return ServerCacheManager.getModLevel(internal.asRSCM(RSCMType.MODLEVEL))
                }
            }
            return null
        }
    }
}

