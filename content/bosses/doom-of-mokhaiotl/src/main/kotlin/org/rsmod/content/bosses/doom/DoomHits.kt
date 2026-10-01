package org.rsmod.content.bosses.doom

import org.rsmod.api.combat.commons.DemonbaneChecks
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.game.hit.HitBuilder

internal fun HitBuilder.playerAttacker(players: PlayerList): Player? =
    if (isFromPlayer) sourceUid?.let { PlayerUid(it).resolve(players) } else null

internal fun HitBuilder.isDemonbaneOrEyeOfAyak(): Boolean =
    DemonbaneChecks.isDemonbane(type, righthandType(), secondaryType()) ||
        righthandType()?.isType(EYE_OF_AYAK) == true

private const val EYE_OF_AYAK = "obj.eye_of_ayak"

internal fun Player.clearAttackDelay() {
    actionDelay = currentMapClock
}
