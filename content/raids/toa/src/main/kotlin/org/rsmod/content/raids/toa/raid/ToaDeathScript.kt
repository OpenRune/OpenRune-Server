package org.rsmod.content.raids.toa.raid

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import net.rsprot.protocol.game.outgoing.sound.MidiJingle
import org.rsmod.api.mechanics.toxins.Toxin.cureAllToxins
import org.rsmod.api.player.death.DEATH_CAUSE_ATTR
import org.rsmod.api.player.deathResetTimers
import org.rsmod.api.player.disablePrayers
import org.rsmod.api.player.hasProtectItemPrayer
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.midiSong
import org.rsmod.api.player.musicClocks
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
 * Not ported yet: Retribution, Ba-Ba's pit fall.
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
        // Prayers go off before the raid can fail; the retrieval chest needs this one.
        player.attr[ToaRetrieval.PROTECT_ITEM_AT_DEATH] = player.hasProtectItemPrayer()
        player.queue(TOA_DEATH_QUEUE, 1)
    }

    private companion object {
        const val STANDARD_DEATH_QUEUE = "queue.death"
        const val TOA_DEATH_QUEUE = "queue.toa_death"
    }
}

/**
 * The death, in ticks after the killing hit (K). The queue starts at K+1. Capture (Crondis
 * puzzle, solo):
 *
 * | Tick | |
 * |---|---|
 * | K+1 | `tracking_deaths` + 1 |
 * | K+3 | death animation, no sound |
 * | K+7 | minimap reset |
 * | K+8 | messages, restore, teleport, camera reset, ghost (tabs closed, HUD 30) |
 * | K+10 | wipe check ([wipeAftermath]) |
 *
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
    player.trackingDeaths++
    delay(2)
    anim("seq.human_death")
    delay(4)
    minimapReset()
    delay(1)
    combatClearQueue()
    resetAnim()

    // During the animation the party may have wiped, moved on, or ended the raid.
    val room = raid.encounterOf(player)
    if (player.currentRaid !== raid || room == null) {
        player.toaRestore()
        return
    }

    raid.totalDeaths++
    raid.points.onDeath(player)
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
    // The supplies' timed effects end with the death (the consumables module's own guidance).
    raid.deps.supplyEffects.clearSessionEffects(player)
    camReset()

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
 * outside the lobby. Ticks after the wipe (W), from the capture (Crondis puzzle, retry):
 *
 * | Tick | |
 * |---|---|
 * | W | message, fade out, music silenced, jingle 90 |
 * | W+1 | minimap hidden |
 * | W+2 | the raid's music again |
 * | W+3 | revive (tabs back), honey locusts |
 * | W+5 | fade in, minimap back, HUD |
 * | W+7 | fade closed, `toa_hud` back |
 *
 * The failed raid isn't captured; it follows the same ticks, with the move outside at W+3 and no
 * raid music.
 *
 * A failed raid is a normal death: what isn't kept goes to the lobby's retrieval chest
 * ([ToaRetrieval]). It can only fail with a death invocation on, which is when that applies.
 */
internal suspend fun ProtectedAccess.wipeAftermath(room: ToaEncounter, retry: Boolean) {
    val raid = room.raid
    if (retry) {
        val limit = raid.permittedTeamDeaths
        val attempts =
            if (limit == null) {
                "You may try again..."
            } else {
                "You have <col=ef1020>${limit - raid.teamDeaths}</col> attempts remaining..."
            }
        mes("Your party failed to complete the challenge. $attempts")
    } else {
        mes("You failed to survive the Tombs of Amascut.")
    }
    fadeOut()
    player.midiSong(STOP_MUSIC)
    player.jingle(WIPE_JINGLE, WIPE_JINGLE_MILLIS)
    delay(1)
    minimapHideMap()
    delay(1)
    if (retry) player.midiSong(RAID_MIDI)
    delay(1)

    raid.revive(player)
    player.toaRestore()
    if (retry) {
        // Capture: at the revive. None with On a Diet, or when the raid fails.
        if (!raid.isActive(ON_A_DIET)) invAdd(inv, HONEY_LOCUST, room.honeyLocusts())
    } else {
        // leave() takes the raid's own items first (Offline_Scape triggerTOAFailure's order).
        ToaRaidManager.leave(player, logout = false)
        val protectItem = player.attr[ToaRetrieval.PROTECT_ITEM_AT_DEATH] == true
        val deps = raid.deps
        if (ToaRetrieval.store(player, deps.deathDrops, deps.marketPrices, protectItem)) {
            mes(
                "A magical chest has retrieved some of your items. You can collect them from it " +
                    "in the Tombs of Amascut lobby."
            )
        }
        telejump(TOA_OUTSIDE, TeleportType.Exempt)
    }
    delay(2)

    minimapReset()
    fadeIn()
    if (retry) ToaRaidManager.refreshHudStates(raid)
    delay(2)
    closeFadeOverlayNow()
    if (raid.isInside(player)) reopenHud(raid)
}

/**
 * Plays jingle [id]. There's no gameval name for these jingles to pass to `midiJingle`, so this
 * sends the packet itself and resets the music clock as `midiJingle` does. The capture sends the
 * jingle's length too.
 */
private fun Player.jingle(id: Int, lengthMillis: Int) {
    musicClocks = 0
    client.write(MidiJingle(id, lengthMillis))
}

/**
 * Full restore after a death, a wipe or a beaten boss (Offline_Scape `player.reset()`). The
 * public pieces of the standard death's private `resetPlayerState`. [camReset] and
 * [ProtectedAccess.minimapReset] need access, so callers with access do those themselves.
 *
 * Capture (Zebak): a beaten boss restores stats but leaves prayers on; the player turned them
 * off. Turning them off here let projectiles already in flight hit unprotected.
 */
internal fun Player.toaRestore(prayersOff: Boolean = true) {
    if (prayersOff) disablePrayers()
    cureAllToxins()
    deathResetTimers()
    statRestoreAll(ALL_STATS)
    specialAttackType = 0
    skullIcon = null
    rebuildAppearance()
}

private var Player.specialAttackType by intVarp("varp.sa_attack")

/** The account's total deaths. Capture: + 1 on the tick after the killing hit. */
private var Player.trackingDeaths by intVarp("varp.tracking_deaths")

private val ALL_STATS: List<String> by lazy {
    ServerCacheManager.getStats().values.map { RSCM.getReverseMapping(RSCMType.STAT, it.id) }
}

private const val ON_A_DIET = "On a Diet"
private const val HONEY_LOCUST = "obj.toa_honey_locust"

/** Midi 147, "silence" (osrs-dumps midi.sym). */
private const val STOP_MUSIC = "midi.stop_music"

/** Capture: the track before a challenge starts, in the nexus and the Crondis room. */
private const val RAID_MIDI = 730

private const val WIPE_JINGLE = 90
private const val WIPE_JINGLE_MILLIS = 4_718
