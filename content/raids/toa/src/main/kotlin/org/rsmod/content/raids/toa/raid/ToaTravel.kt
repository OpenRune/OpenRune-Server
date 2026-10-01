package org.rsmod.content.raids.toa.raid

import org.rsmod.annotations.InternalApi
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.raids.toa.party.ToaPartyManager
import org.rsmod.content.raids.toa.raid.encounter.ToaEncounter
import org.rsmod.game.entity.Player
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid

internal val TOA_OUTSIDE = CoordGrid(3358, 9113, 0)

internal const val TOA_UNAVAILABLE =
    "The Tombs of Amascut are unavailable at the moment. Please try again shortly."

private const val TOA_HUD = "interface.toa_hud"

private const val FADE_CYCLES = 50

internal suspend fun ProtectedAccess.resolveTarget(
    raid: ToaRaid,
    room: ToaRoom,
    checkLeader: Boolean,
): ToaEncounter? {
    raid.current?.takeIf { it.room == room && !it.destroyed }?.let { return it }
    if (checkLeader && !raid.isLeader(player)) {
        mesbox("Your leader, ${raid.leaderName}, must enter first.")
        return null
    }
    val built = raid.advanceTo(room)
    if (built == null) mes(TOA_UNAVAILABLE)
    return built
}

internal suspend fun ProtectedAccess.proceed(raid: ToaRaid, room: ToaRoom, announcement: String) {
    val existing = raid.current?.takeIf { it.room == room && !it.destroyed }
    val target = existing ?: raid.advanceTo(room)
    if (target == null) {
        mes(TOA_UNAVAILABLE)
        return
    }
    if (existing == null) {
        raid.announce(player, "${player.displayName} $announcement. Join ${player.objectPronoun()}...")
    }
    travel(target, fromLobby = false)
}

internal suspend fun ProtectedAccess.travel(target: ToaEncounter, fromLobby: Boolean) {
    val raid = target.raid
    fadeOut()
    delay(1)
    minimapHideMap()
    delay(2)

    if (target.destroyed) {
        minimapReset()
        closeFadeOverlayNow()
        if (raid.isInside(player)) reopenHud(raid)
        return
    }

    val arrival = target.arrival()
    ToaRaidManager.moveTo(player, target)
    telejump(arrival.coords, TeleportType.Exempt)
    arrival.facing?.let { faceDirection(it) }

    if (fromLobby) {
        delay(1)
        minimapReset()
        fadeIn()
        val limit = raid.timeLimitMinutes
        if (limit != null && !raid.timerStarted) {
            mes(
                "Overall time to beat: <col=ef1020>$limit:00</col>. " +
                    "The timer starts upon choosing your first path."
            )
        }
        delay(2)
    } else {
        delay(3)
        minimapReset()
        fadeIn()
        delay(2)
    }

    closeFadeOverlayNow()
    if (raid.isInside(player)) reopenHud(raid)
}

internal fun ProtectedAccess.reopenHud(raid: ToaRaid) {
    ifOpenFullOverlay(TOA_HUD)
    ToaRaidManager.sendHud(player, raid)
}

internal suspend fun ProtectedAccess.exitRaid() {
    fadeOut()
    delay(1)
    minimapHideMap()
    delay(2)

    ToaRaidManager.leave(player, logout = false)
    telejump(TOA_OUTSIDE, TeleportType.Exempt)
    faceDirection(Direction.North)

    delay(1)
    minimapReset()
    fadeIn()
    delay(2)
    closeFadeOverlayNow()
}

internal suspend fun ProtectedAccess.confirmYesNo(title: String): Boolean =
    choice2("Yes.", true, "No.", false, title = title)

internal suspend fun ProtectedAccess.confirmAbandonStragglers(raid: ToaRaid, action: String): Boolean {
    mesbox("Some of your party don't seem to have arrived yet.<br>If you proceed, they will be abandoned.")
    val abandon =
        choice2(
            "No, wait for any stragglers.",
            false,
            "Yes, abandon any stragglers.",
            true,
            title = "$action?",
        )
    if (!abandon) return false
    abandonStragglers(raid)
    return true
}

@OptIn(InternalApi::class)
private fun abandonStragglers(raid: ToaRaid) {
    for (straggler in raid.stragglers()) {
        straggler.mes("Your party moved on without you.")
        if (raid.isInside(straggler)) {
            raid.deps.launcher.launchLenient(straggler) { exitRaid() }
        } else {
            val party = raid.lobbyParty
            if (ToaPartyManager.leaveParty(straggler)) {
                ToaPartyManager.refreshViewers(party, exclude = straggler)
            }
        }
    }
}

internal suspend fun ProtectedAccess.confirmBeginChallenge(raid: ToaRaid, quick: Boolean): Boolean =
    when {
        raid.stragglers().isNotEmpty() -> confirmAbandonStragglers(raid, "Begin the challenge")
        quick -> true
        else -> confirmYesNo("Begin the challenge?")
    }

internal fun ToaRaid.announce(from: Player, text: String) {
    for (member in players) {
        if (member !== from) member.mes(text)
    }
}

internal fun Player.objectPronoun(): String =
    when (appearance.subjectPronoun()) {
        "He" -> "him"
        "She" -> "her"
        else -> "them"
    }

internal fun ProtectedAccess.fadeOut() =
    fadeOverlay(
        startColour = 0,
        startTransparency = 255,
        endColour = 0,
        endTransparency = 0,
        clientDuration = FADE_CYCLES,
    )

internal fun ProtectedAccess.fadeIn() =
    fadeOverlay(
        startColour = 0,
        startTransparency = 0,
        endColour = 0,
        endTransparency = 255,
        clientDuration = FADE_CYCLES,
    )
