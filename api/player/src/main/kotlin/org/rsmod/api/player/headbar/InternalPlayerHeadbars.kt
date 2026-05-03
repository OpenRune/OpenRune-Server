package org.rsmod.api.player.headbar

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.HealthBarServerType
import org.rsmod.game.headbar.Headbar
import org.rsmod.game.hit.Hitmark

internal object InternalPlayerHeadbars {
    fun createFromHitmark(
        hitmark: Hitmark,
        currHp: Int,
        maxHp: Int,
        headbar: String,
    ): Headbar {
        return when {
            hitmark.isNpcSource ->
                createNpcSource(
                    sourceSlot = hitmark.npcSlot,
                    currHp = currHp,
                    maxHp = maxHp,
                    internal = headbar,
                    clientDelay = hitmark.delay,
                )

            hitmark.isPlayerSource ->
                createPlayerSource(
                    sourceSlot = hitmark.playerSlot,
                    currHp = currHp,
                    maxHp = maxHp,
                    internal = headbar,
                    clientDelay = hitmark.delay,
                    specific = hitmark.isPrivate,
                )

            else ->
                createNoSource(
                    currHp = currHp,
                    maxHp = maxHp,
                    internal = headbar,
                    clientDelay = hitmark.delay,
                )
        }
    }

    private fun createNpcSource(
        sourceSlot: Int,
        currHp: Int,
        maxHp: Int,
        internal: String,
        clientDelay: Int,
    ): Headbar {

        val headbar = ServerCacheManager.getHealthBar(internal.asRSCM(RSCMType.HEADBAR))
            ?: error("No headbar found for $internal")

        val fill = calculateFill(headbar.segments, currHp, maxHp)
        return Headbar.fromNpcSource(
            self = headbar.id,
            public = headbar.id,
            startFill = fill,
            endFill = fill,
            startTime = clientDelay,
            endTime = clientDelay,
            slotId = sourceSlot,
        )
    }

    private fun createPlayerSource(
        sourceSlot: Int,
        currHp: Int,
        maxHp: Int,
        internal: String,
        clientDelay: Int,
        specific: Boolean,
    ): Headbar {
        val headbar = ServerCacheManager.getHealthBar(internal.asRSCM(RSCMType.HEADBAR))
            ?: error("No headbar found for $internal")

        val fill = calculateFill(headbar.segments, currHp, maxHp)
        return Headbar.fromPlayerSource(
            self = headbar.id,
            public = if (specific) null else headbar.id,
            startFill = fill,
            endFill = fill,
            startTime = clientDelay,
            endTime = clientDelay,
            slotId = sourceSlot,
        )
    }

    private fun createNoSource(
        currHp: Int,
        maxHp: Int,
        internal: String,
        clientDelay: Int,
    ): Headbar {

        val headbar = ServerCacheManager.getHealthBar(internal.asRSCM(RSCMType.HEADBAR))
            ?: error("No headbar found for $internal")

        val fill = calculateFill(headbar.segments, currHp, maxHp)
        return Headbar.fromNoSource(
            self = headbar.id,
            public = headbar.id,
            startFill = fill,
            endFill = fill,
            startTime = clientDelay,
            endTime = clientDelay,
        )
    }

    private fun calculateFill(segments: Int, currHp: Int, maxHp: Int): Int {
        return (currHp * segments) / maxHp
    }
}
