package org.rsmod.content.raids.toa.raid

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import net.rsprot.protocol.game.outgoing.sound.MidiJingle
import org.rsmod.api.config.constants
import org.rsmod.api.mechanics.toxins.Toxin.cureAllToxins
import org.rsmod.api.player.death.DEATH_CAUSE_ATTR
import org.rsmod.api.player.deathResetTimers
import org.rsmod.api.player.disablePrayers
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.midiSong
import org.rsmod.api.player.musicClocks
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.stat.statRestore
import org.rsmod.api.player.stat.statRestoreAll
import org.rsmod.api.player.vars.intVarp
import org.rsmod.content.raids.toa.lobby.ToaStats
import org.rsmod.content.raids.toa.party.ToaInvocationKey
import org.rsmod.content.raids.toa.raid.ToaRaidManager.currentRaid
import org.rsmod.content.raids.toa.raid.ToaRetrieval.protectItemAtDeath
import org.rsmod.content.raids.toa.raid.encounter.ToaEncounter
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.game.entity.Player

internal suspend fun ProtectedAccess.raidDeath() {
    player.attr.remove(DEATH_CAUSE_ATTR)

    val raid = player.currentRaid
    if (raid == null || !raid.isDying(player)) {
        player.toaRestore()
        return
    }

    try {
        dieInRaid(raid)
    } finally {
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

    val room = raid.encounterOf(player)
    if (player.currentRaid !== raid || room == null) {
        player.toaRestore()
        return
    }

    raid.totalDeaths++
    ToaStats.recordDeath(player, raid.settings.mode)
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
    raid.deps.supplyEffects.clearSessionEffects(player)
    camReset()

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

    for (other in room.players) {
        if (other !== player) {
            other.mes(
                "<col=ff0000>${player.displayName}</col> has died. " +
                    "Total deaths: <col=ff0000>${raid.totalDeaths}</col>."
            )
        }
    }

    raid.stopDying(player)
    delay(2)
    room.checkRoomReset()
}

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
    player.jingle(WIPE_JINGLE.asRSCM(RSCMType.JINGLE), WIPE_JINGLE_MILLIS)
    delay(1)
    minimapHideMap()
    delay(1)
    if (retry) player.midiSong(RAID_MIDI)
    delay(1)

    raid.revive(player)
    player.toaRestore()
    if (retry) {
        if (!raid.isActive(ToaInvocationKey.OnADiet)) {
            invAddOrDrop(raid.deps.objRepo, HONEY_LOCUST, room.honeyLocusts())
        }
    } else {
        ToaRaidManager.leave(player, logout = false)
        val protectItem = player.protectItemAtDeath
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

private fun Player.jingle(id: Int, lengthMillis: Int) {
    musicClocks = 0
    client.write(MidiJingle(id, lengthMillis))
}

internal fun Player.toaRestore() {
    disablePrayers()
    cureAllToxins()
    deathResetTimers()
    statRestoreAll(ALL_STATS)
    specialAttackType = 0
    skullIcon = null
    rebuildAppearance()
}

internal fun Player.toaBossRestore() {
    cureAllToxins()
    for (name in ALL_STATS) {
        if (stat(name) < statBase(name)) statRestore(name)
    }
    runEnergy = constants.run_max_energy
    UpdateRun.energy(this, runEnergy)
    specialAttackEnergy = constants.sa_max_energy
    specialAttackType = 0
}

private var Player.specialAttackType by intVarp("varp.sa_attack")

private var Player.specialAttackEnergy by intVarp("varp.sa_energy")

private var Player.trackingDeaths by intVarp("varp.tracking_deaths")

private val ALL_STATS: List<String> by lazy {
    ServerCacheManager.getStats().values.map { RSCM.getReverseMapping(RSCMType.STAT, it.id) }
}

private const val HONEY_LOCUST = "obj.toa_honey_locust"

private const val STOP_MUSIC = "midi.stop_music"

private const val RAID_MIDI = "midi.toa_ambience"

private const val WIPE_JINGLE = "jingle.lord_drakan_reveal_part_1"
private const val WIPE_JINGLE_MILLIS = 4_718
