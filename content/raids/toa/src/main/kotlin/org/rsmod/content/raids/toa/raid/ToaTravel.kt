package org.rsmod.content.raids.toa.raid

import org.rsmod.annotations.InternalApi
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.raids.toa.party.ToaPartyManager
import org.rsmod.content.raids.toa.raid.encounter.ToaEncounter
import org.rsmod.game.entity.Player
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid

/*
 * Moving players between the lobby and the raid's rooms, and the dialogs shared by several
 * locs. These are top-level ProtectedAccess extensions rather than private functions of
 * ToaRaidScript so that rooms (WardensFirstEncounter) and the debug cheats can use them too.
 */

/** Offline_Scape TOAManager.OUTSIDE_LOCATION; the capture's abandon landed here too. */
internal val TOA_OUTSIDE = CoordGrid(3358, 9113, 0)

internal const val TOA_UNAVAILABLE =
    "The Tombs of Amascut are unavailable at the moment. Please try again shortly."

private const val TOA_HUD = "interface.toa_hud"

/** Capture: fade_overlay script 948 with duration 50 both ways. */
private const val FADE_CYCLES = 50

// ---------------------------------------------------------------------------------------------
// Travelling
// ---------------------------------------------------------------------------------------------

/**
 * The room this player should travel into to reach [room]: the current room if it is [room],
 * otherwise a newly built one. Offline_Scape TOAManager.enter: with [checkLeader], only the
 * leader may build a new room; anyone else can only follow.
 */
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

/**
 * Moves the party on to [room] without a leader check (puzzle exit, Osmumten, the Wardens
 * crystal). The first player through builds the room and tells the others to follow
 * (Offline_Scape TOAManager.advanceRaid, OsmumtenAction, RewardCrystalAction).
 *
 * @param announcement e.g. "has proceeded to the next challenge".
 */
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

/**
 * The fade-teleport-fade into [target]. Ticks relative to the call:
 *
 * | Tick | From the lobby (capture: raid entry) | Between rooms (capture: Scabaras path) |
 * |---|---|---|
 * | +0 | fade out | fade out |
 * | +1 | hide minimap | hide minimap |
 * | +3 | vars, teleport | vars, teleport |
 * | +4 | fade in, minimap back (+ time-limit message) | |
 * | +6 | close fade, HUD | fade in, minimap back |
 * | +8 | | close fade, HUD |
 *
 * Not reproduced: the capture's Scabaras entry teleported twice, to (3552, 5280) at +3 and then
 * to (3523, 5280) at +5. Revisit with the Scabaras puzzle (Phase B).
 */
internal suspend fun ProtectedAccess.travel(target: ToaEncounter, fromLobby: Boolean) {
    val raid = target.raid
    fadeOut() // replaces toa_hud: they share overlay_atmosphere
    delay(1)
    minimapHideMap()
    delay(2)

    if (target.destroyed) {
        // The raid ended during the fade (everyone else left). Undo the fade and stay put.
        minimapReset()
        closeFadeOverlayNow()
        if (raid.isInside(player)) reopenHud(raid)
        return
    }

    val arrival = target.arrival()
    // Before the teleport: leaving the lobby area checks isInRaid() to keep the party.
    ToaRaidManager.moveTo(player, target)
    telejump(arrival.coords)
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

private fun ProtectedAccess.reopenHud(raid: ToaRaid) {
    ifOpenFullOverlay(TOA_HUD) // same slot as the fade, hence after closing it
    ToaRaidManager.sendHud(player, raid)
}

/**
 * Leaves the raid through a fade and lands outside the lobby (capture: abandon). The dialogs are
 * the caller's. Walking into the lobby area afterwards reopens 773.
 */
internal suspend fun ProtectedAccess.exitRaid() {
    fadeOut()
    delay(1)
    minimapHideMap()
    delay(2)

    ToaRaidManager.leave(player, logout = false)
    telejump(TOA_OUTSIDE)
    faceDirection(Direction.North)

    delay(1)
    minimapReset()
    fadeIn()
    delay(2)
    closeFadeOverlayNow()
}

// ---------------------------------------------------------------------------------------------
// Dialogs
// ---------------------------------------------------------------------------------------------

/** A "Yes." / "No." chatmenu (capture: the path door's "Do you wish to walk the Path of X?"). */
internal suspend fun ProtectedAccess.confirmYesNo(title: String): Boolean =
    choice2("Yes.", true, "No.", false, title = title)

/**
 * Offline_Scape TOAManager.startAbandonDialogue. Asks the leader whether to leave behind the
 * players who haven't reached the current room, and abandons them on yes.
 *
 * @param action the question without its "?", e.g. "Begin the challenge".
 * @return `true` to go ahead.
 */
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

/**
 * Removes everyone who isn't in the current room. Players inside the raid are faded out to the
 * lobby; players still in the lobby just lose their party.
 */
@OptIn(InternalApi::class)
private fun abandonStragglers(raid: ToaRaid) {
    for (straggler in raid.stragglers()) {
        straggler.mes("Your party moved on without you.")
        if (raid.isInside(straggler)) {
            // Forced, like a kick: must happen even if they have a dialog open.
            raid.deps.launcher.launchLenient(straggler) { exitRaid() }
        } else {
            val party = raid.lobbyParty
            if (ToaPartyManager.leaveParty(straggler)) {
                ToaPartyManager.refreshViewers(party, exclude = straggler)
            }
        }
    }
}

/**
 * "Begin the challenge?" before a barrier or teleport crystal starts a room (Offline_Scape
 * TOARaidArea.handleBarrier / handleTeleportCrystal): the stragglers question if anyone is
 * behind, else a Yes/No unless it was a Quick- op.
 */
internal suspend fun ProtectedAccess.confirmBeginChallenge(raid: ToaRaid, quick: Boolean): Boolean =
    when {
        raid.stragglers().isNotEmpty() -> confirmAbandonStragglers(raid, "Begin the challenge")
        quick -> true
        else -> confirmYesNo("Begin the challenge?")
    }

// ---------------------------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------------------------

/** Sends [text] to every raid member except [from]. */
internal fun ToaRaid.announce(from: Player, text: String) {
    for (member in players) {
        if (member !== from) member.mes(text)
    }
}

/** "him" / "her" / "them", from the player's pronoun setting. */
internal fun Player.objectPronoun(): String =
    when (appearance.subjectPronoun()) {
        "He" -> "him"
        "She" -> "her"
        else -> "them"
    }

private fun ProtectedAccess.fadeOut() =
    fadeOverlay(
        startColour = 0,
        startTransparency = 255,
        endColour = 0,
        endTransparency = 0,
        clientDuration = FADE_CYCLES,
    )

private fun ProtectedAccess.fadeIn() =
    fadeOverlay(
        startColour = 0,
        startTransparency = 0,
        endColour = 0,
        endTransparency = 255,
        clientDuration = FADE_CYCLES,
    )
