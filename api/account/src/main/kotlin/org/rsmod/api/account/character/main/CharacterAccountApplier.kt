package org.rsmod.api.account.character.main

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ModLevelType
import jakarta.inject.Inject
import java.time.LocalDateTime
import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

public class CharacterAccountApplier @Inject constructor() :
    CharacterDataStage.Applier<CharacterAccountData> {
    override fun apply(player: Player, data: CharacterAccountData) {
        player.accountId = data.accountId
        player.characterId = data.characterId

        val accountHash = (data.accountId.toLong() shl 32) or data.realm.toLong()
        val userHash = data.loginName.hashCode().toLong()
        player.userId = data.characterId.toLong()
        player.accountHash = accountHash
        player.userHash = userHash

        val uuid = data.characterId.toLong()
        player.uuid = uuid
        player.observerUUID = uuid

        val device = data.knownDevice
        player.lastKnownDevice = device
        player.members = data.members
        player.username = data.loginName
        player.displayName = data.displayName ?: ""
        player.coords = CoordGrid(data.coordX, data.coordZ, data.coordLevel)
        player.runEnergy = data.runEnergy
        player.xpRate = data.xpRate
        player.lastLogin = LocalDateTime.now()
        player.vars.backing.putAll(data.varps)
        if (data.attrs.isNotEmpty()) {
            player.attr.putAllFromPersistence(data.attrs)
        }
        player.assignModLevel(data)
    }

    private fun Player.assignModLevel(data: CharacterAccountData) {
        val levels = ServerCacheManager.getModelLevels().values
        val defaultLevel = levels.first()
        modLevel =
            resolveModLevelFromRights(data.rights)
                ?: levels.firstOrNull { it.displayName == data.modLevel }
                ?: defaultLevel
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
