package org.rsmod.content.raids.toa.raid

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.mechanics.toxins.Toxin.cureAllToxins
import org.rsmod.api.player.death.DEATH_CAUSE_ATTR
import org.rsmod.api.player.deathResetTimers
import org.rsmod.api.player.disablePrayers
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statRestoreAll
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onPlayerHit
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.content.raids.toa.raid.ToaRaidManager.currentRaid
import org.rsmod.content.raids.toa.raid.encounter.ToaEncounter
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Deaths inside the raid: you respawn in the same room with nothing lost and, if the room's
 * challenge is running, as a ghost until it ends. Port of Offline_Scape TOARaidArea.sendDeath.
 *
 * **How the standard (Lumbridge) death is avoided without touching the engine.** Both player hit
 * processors do, inside one protected access: (1) queue `queue.death` when hitpoints reach 0,
 * (2) show the hitmark, (3) publish [PlayerHitEvents.Impact][org.rsmod.api.player.events.PlayerHitEvents].
 * `Impact` allows many listeners, so [onPlayerHit] hears step 3. At that moment `queue.death`
 * is queued but can't have started: `PlayerQueueProcessor` won't launch a normal queue while
 * access is protected, and the hit is still being processed. So we swap it for our own
 * `queue.toa_death` there. Our own queue id means no clash with PlayerDeathScript's handler.
 *
 * Covered: NPC and player hits, poison and venom (they damage through `takeInstantHit`), and
 * any scripted damage through `queueHit`/`takeInstantHit`. Not covered: disease's direct
 * `statSub` (doesn't happen in TOA) and admin `::die` (calls `queueDeath()` directly, so it
 * still sends you to Lumbridge).
 *
 * Not ported yet: points lost on death (no points system), Retribution, Ba-Ba's pit fall.
 */
class ToaDeathScript : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerHit { interceptDeath(player) }
        onPlayerQueue(TOA_DEATH_QUEUE) { raidDeath() }
    }

    private fun interceptDeath(player: Player) {
        val raid = player.currentRaid ?: return
        if (STANDARD_DEATH_QUEUE !in player.queueList) return
        player.clearQueue(STANDARD_DEATH_QUEUE)
        // Hits that land on a 0-hp player during the death animation queue another death; swallow
        // them. Offline_Scape did this with lock() and blockIncomingHits().
        if (!raid.startDying(player)) return
        player.queue(TOA_DEATH_QUEUE, 1)
    }

    private companion object {
        const val STANDARD_DEATH_QUEUE = "queue.death"
        const val TOA_DEATH_QUEUE = "queue.toa_death"
    }
}

/**
 * Offline_Scape timeline: +1 death animation, +5 messages and respawn, +7 room reset check.
 * Everything the standard death would have done for us (clearing the death cause, restoring the
 * player) happens here too.
 */
private suspend fun ProtectedAccess.raidDeath() {
    // A killing hit records who/what killed you before we intercept. Clear it, or a later real
    // death outside the raid could be credited to it.
    player.attr.remove(DEATH_CAUSE_ATTR)

    val raid = player.currentRaid
    if (raid == null || !raid.isDying(player)) {
        // The raid ended (or you left it) between the hit and now. Don't leave you at 0 hp.
        player.toaRestore()
        return
    }

    try {
        dieInRaid(raid)
    } finally {
        // Also runs if the coroutine ends early (logout), so the flag never sticks.
        raid.stopDying(player)
    }
}

private suspend fun ProtectedAccess.dieInRaid(raid: ToaRaid) {
    stopAction()
    combatClearQueue()
    camReset()
    delay(1)
    soundSynth("synth.human_death")
    anim("seq.human_death")
    delay(4)
    combatClearQueue()
    resetAnim()

    // During the animation the party may have wiped, moved on, or ended the raid.
    val room = raid.encounterOf(player)
    if (player.currentRaid !== raid || room == null) {
        player.toaRestore()
        return
    }

    raid.totalDeaths++
    mes("You have died. Total deaths: <col=ff0000>${raid.totalDeaths}</col>.")
    val othersFighting =
        room.stage == ToaStage.STARTED &&
            room.challengePlayers.any { it !== player && !raid.isGhost(it) }
    if (othersFighting) {
        if (raid.canRetryAfter(extraWipes = 1)) {
            mes("You will respawn when your party completes or fails the challenge.")
        } else {
            mes("You will respawn when your party completes the challenge.")
        }
    }
    player.toaRestore()
    minimapReset()

    // Where you come back: the room's spawn tile, or inside the challenge area if the room is
    // already beaten. You're a ghost while the challenge is still running (not in the nexus).
    val isHall = room.room.kind == ToaRoom.Kind.MAIN_HALL
    val challengeSpawn = room.room.challengeSpawn
    val dest =
        if (room.stage == ToaStage.COMPLETED && !isHall && challengeSpawn != null) {
            room.coords(challengeSpawn)
        } else {
            room.coords(room.room.randomSpawn(random))
        }
    telejump(dest, TeleportType.Exempt)
    if (!isHall && room.stage == ToaStage.STARTED) {
        raid.makeGhost(player)
    }
    ToaRaidManager.refreshHudStates(raid)

    // Offline_Scape put the *recipient's* name in this message; it should be the dead player's.
    for (other in room.players) {
        if (other !== player) {
            other.mes(
                "<col=ff0000>${player.displayName}</col> has died. " +
                    "Total deaths: <col=ff0000>${raid.totalDeaths}</col>."
            )
        }
    }

    // Before the last suspension on purpose: the death is fully resolved, only the wipe check is
    // left, and that also runs from ToaRaidManager.leave if the player logs out now.
    raid.stopDying(player)
    delay(2)
    room.checkRoomReset()
}

/**
 * What each player in a wiped room sees (Offline_Scape checkRoomReset's FadeScreen): a fade,
 * then either another attempt, or, with no attempts left, the raid fails and they're put
 * outside the lobby.
 *
 * TODO: jingle 90; the failure's item retrieval chest (Offline_Scape ItemRetrievalService).
 * Until that exists, a failed raid keeps your items.
 */
internal suspend fun ProtectedAccess.wipeAftermath(room: ToaEncounter, retry: Boolean) {
    val raid = room.raid
    fadeOut()
    delay(2)

    raid.revive(player)
    player.toaRestore()
    if (retry) {
        val limit = raid.permittedTeamDeaths
        val attempts =
            if (limit == null) {
                "You may try again..."
            } else {
                "You have <col=ff0000>${limit - raid.teamDeaths}</col> attempts remaining..."
            }
        mes("Your party failed to complete the challenge. $attempts")
        ToaRaidManager.refreshHudStates(raid)
    } else {
        mes("You failed to survive the Tombs of Amascut.")
        minimapHideMap()
        ToaRaidManager.leave(player, logout = false)
        telejump(TOA_OUTSIDE, TeleportType.Exempt)
        minimapReset()
    }

    fadeIn()
    delay(2)
    closeFadeOverlayNow()
    if (raid.isInside(player)) reopenHud(raid)
}

/**
 * Full restore after a death, a wipe or a beaten boss (Offline_Scape `player.reset()`). The
 * public pieces of the standard death's private `resetPlayerState`. [camReset] and
 * [ProtectedAccess.minimapReset] need access, so callers with access do those themselves.
 */
internal fun Player.toaRestore() {
    disablePrayers()
    cureAllToxins()
    deathResetTimers()
    statRestoreAll(ALL_STATS)
    specialAttackType = 0
    skullIcon = null
    rebuildAppearance()
}

private var Player.specialAttackType by intVarp("varp.sa_attack")

private val ALL_STATS: List<String> by lazy {
    ServerCacheManager.getStats().values.map { RSCM.getReverseMapping(RSCMType.STAT, it.id) }
}
