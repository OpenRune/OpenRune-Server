package org.rsmod.api.registry.player

import jakarta.inject.Inject
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.registry.zone.ZonePlayerActivityBitSet
import org.rsmod.events.EventBus
import org.rsmod.game.entity.PathingEntity.Companion.INVALID_SLOT
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.map.zone.ZoneKey
import org.rsmod.routefinder.collision.CollisionFlagMap

public class PlayerRegistry
@Inject
constructor(
    public val playerList: PlayerList,
    private val collision: CollisionFlagMap,
    private val zoneActivity: ZonePlayerActivityBitSet,
    private val eventBus: EventBus,
) {
    public val zones: ZonePlayerMap = ZonePlayerMap()

    public fun count(): Int = zones.playerCount()

    public fun add(player: Player): PlayerRegistryResult.Add {
        val slot = player.slotId
        if (slot == INVALID_SLOT) {
            return PlayerRegistryResult.Add.NoAvailableSlot
        } else if (playerList[slot] != null) {
            return PlayerRegistryResult.Add.ListSlotMismatch(playerList[slot])
        }
        playerList[slot] = player
        player.slotId = slot
        player.assignUid()
        eventBus.publish(SessionStateEvent.Initialize(player))
        return PlayerRegistryResult.Add.Success
    }

    public fun del(player: Player): PlayerRegistryResult.Delete {
        val slot = player.slotId
        if (slot == INVALID_SLOT) {
            return PlayerRegistryResult.Delete.UnexpectedSlot
        } else if (playerList[slot] != player) {
            return PlayerRegistryResult.Delete.ListSlotMismatch(playerList[slot])
        }
        playerList.remove(slot)
        eventBus.publish(SessionStateEvent.Delete(player))
        player.removeBlockWalkCollision(collision, player.coords)
        zoneDel(player, ZoneKey.from(player.coords))
        player.slotId = INVALID_SLOT
        player.clearUid()
        player.destroy()
        return PlayerRegistryResult.Delete.Success
    }

    public fun hide(player: Player) {
        player.removeBlockWalkCollision(collision, player.coords)
        player.hidden = true
    }

    public fun reveal(player: Player) {
        player.addBlockWalkCollision(collision, player.coords)
        player.hidden = false
    }

    public fun change(player: Player, from: ZoneKey, to: ZoneKey) {
        zoneDel(player, from)
        zoneAdd(player, to)
    }

    /**
     * Returns a sequence of all [Player]s in the given [zone].
     *
     * _Note: This function does **not** filter out "hidden" players, or those in the process of
     * dying or logging out. If you want to exclude these, filter the result using
     * `Player.isValidTarget`._
     */
    public fun findAll(zone: ZoneKey): Sequence<Player> {
        val entries = zones[zone] ?: return emptySequence()
        return entries.entries.asSequence()
    }

    private fun zoneDel(player: Player, zone: ZoneKey) {
        if (zone == ZoneKey.NULL) {
            return
        }
        val oldZone = zones[zone] ?: return
        oldZone.remove(player)

        if (oldZone.isEmpty()) {
            zoneActivity.unflag(zone)
        }
    }

    private fun zoneAdd(player: Player, zone: ZoneKey) {
        if (zone == ZoneKey.NULL) {
            return
        }
        val newZone = zones.getOrPut(zone)
        check(player !in newZone) { "Player already registered to zone($zone): $player" }
        newZone.add(player)

        if (newZone.size == 1) {
            zoneActivity.flag(zone)
        }
    }

    public fun nextFreeSlot(): Int? = playerList.nextFreeSlot()

    public fun isOnline(userId: Long): Boolean = playerList.any { it.userId == userId }

    public fun findOnlineByCharacterId(characterId: Int): Player? {
        if (characterId <= 0) {
            return null
        }
        for (player in playerList) {
            if (player.characterId == characterId) {
                return player
            }
        }
        return null
    }

    public fun applyCentralMuteUpdate(
        centralAccountId: Long,
        characterId: Int,
        mutedUntilEpochMillis: Long,
    ) {
        val aid = centralAccountId.toInt()
        val newUntil: LocalDateTime? =
            if (mutedUntilEpochMillis <= 0L) {
                null
            } else {
                LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(mutedUntilEpochMillis),
                    ZoneId.systemDefault(),
                )
            }
        for (player in playerList) {
            if (player.accountId != aid) {
                continue
            }
            if (characterId != 0 && player.characterId != characterId) {
                continue
            }
            //TODO MUTED
        }
    }

    /**
     * Marks matching online players disconnected after Central revokes their session (e.g. ban while online).
     * [centralAccountId] matches [Player.accountId] (shared `accounts.id` with Central).
     * [characterId] `0` = every character on that account; otherwise only that character row id.
     */
    public fun disconnectPlayersForCentralRevoke(
        centralAccountId: Long,
        characterId: Int,
    ) {
        val aid = centralAccountId.toInt()
        for (player in playerList) {
            if (player.accountId != aid) {
                continue
            }
            if (characterId != 0 && player.characterId != characterId) {
                continue
            }
            player.forceDisconnect = true
        }
    }

    /**
     * Disconnects matching online players after Central applies a `kick` punishment row (one-shot
     * notify). Uses a kick-style client close without clearing Central sessions.
     */
    public fun disconnectPlayersForCentralKick(
        centralAccountId: Long,
        characterId: Int,
    ) {
        val aid = centralAccountId.toInt()
        for (player in playerList) {
            if (player.accountId != aid) {
                continue
            }
            if (characterId != 0 && player.characterId != characterId) {
                continue
            }
            player.forceDisconnect = true
        }
    }

    /**
     * Applies a display name written in Central's DB (staff rename, etc.) to the matching online player
     * and rebuilds appearance so other clients see the new name.
     */
    public fun applyCentralDisplayNameSync(
        centralAccountId: Long,
        characterId: Int,
        newDisplayName: String,
        priorDisplayName: String? = null,
    ) {
        if (characterId <= 0 || newDisplayName.isBlank()) {
            return
        }
        val aid = centralAccountId.toInt()
        val player = findOnlineByCharacterId(characterId) ?: return
        if (player.accountId != aid) {
            return
        }
        if (priorDisplayName != null) {
            player.previousDisplayName = priorDisplayName
        }

        player.displayName = newDisplayName
        player.displayNameChangedAtMillis = System.currentTimeMillis()
        player.rebuildAppearance()
    }

    public inline fun forEachOnline(action: (Player) -> Unit) {
        for (player in playerList) {
            action(player)
        }
    }
}
