package org.rsmod.content.bosses.royaltitans

import jakarta.inject.Inject
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceAccess
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceNpc
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.instances.SessionState
import org.rsmod.api.player.interact.LocInteractions
import org.rsmod.api.player.music.MusicPlayer
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarp
import org.rsmod.api.script.onApLoc1
import org.rsmod.api.script.onApLoc2
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.table.MusicRow
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The Royal Titans arena, entered from the tunnel above the Giants' Plateau.
 *
 * Both titans are spawns of the copy itself, so every fresh run starts a dormant pair; they are
 * engaged only once a player is actually inside the arena.
 */
class RoyalTitansInstance
@Inject
constructor(
    registry: BossInstanceRegistry,
    private val encounter: RoyalTitansEncounter,
    private val locInteractions: LocInteractions,
    private val musicPlayer: MusicPlayer,
) : InstanceScript(registry) {

    private var Player.dontAskAgain by boolVarp("varp.royal_titans_dontaskagain")

    override fun settingsRow(): String = SETTINGS_ROW

    override fun area(): InstanceArea = AREA

    override fun runsPreludeOnFreshRun(): Boolean = true

    override fun recordsKillTime(player: Player): Boolean = false

    override fun bestTimeVarp(): String = BEST_TIME_VARP

    override fun ScriptContext.configure() {
        locInteractions.setApRange(ESCAPE_LOC_SYMBOL, ESCAPE_AP_RANGE)
        onEnterPrelude { result, enter ->
            val session =
                when (result) {
                    is InstanceManager.Result.Created -> result.session
                    is InstanceManager.Result.Joined -> result.session
                    else -> return@onEnterPrelude
                }
            val fresh = result.isFreshRun()
            enterPrelude(enter)
            if (fresh) {
                engageTitans(session, player)
            }
        }
        onEnterObject { openEntryMenu() }
        // The entrance exposes Enter (op1), Join (op2) and Quick-start (op3). Op1 opens the menu
        // above; the other two skip straight to the action they name.
        onOpLoc2(ENTRANCE_LOC_SYMBOL) { joinFight() }
        onOpLoc3(ENTRANCE_LOC_SYMBOL) { quickStart() }
        // The arena tunnel exposes Inspect (op1) and Quick-escape (op2). The instance framework
        // routes op1 of an exit object to this override, so keep that option informational and
        // bind the leave flow to the Quick-escape option the client actually offers.
        onExitObject { inspectTunnel() }
        onApLoc1(ESCAPE_LOC_SYMBOL) { inspectTunnel() }
        onOpLoc2(ESCAPE_LOC_SYMBOL) { quickEscape() }
        onApLoc2(ESCAPE_LOC_SYMBOL) { quickEscape() }
    }

    private suspend fun ProtectedAccess.enterPrelude(enter: suspend ProtectedAccess.() -> Unit) {
        anim("seq.human_longcrawl")
        soundSynth(CRAWL_SOUND, loops = 2, delay = 4)
        fadeOverlay(0, FADE_CLEAR, 0, FADE_BLACK, FADE_CLIENT_CYCLES)
        delay(2)
        minimapHideMap()
        enter()
        delay(2)
        minimapReset()
        closeFadeOverlay(cycles = 2)
        musicPlayer.unlockAndPlay(player, MusicRow.getRow(MUSIC))
    }

    private suspend fun ProtectedAccess.openEntryMenu() {
        if (manager.sessionForPlayer(player) != null) {
            mes("You are already inside an instance.")
            return
        }
        val pick =
            choice3(
                "Start a fight (Your friends will be able to join you).",
                1,
                "Join a fight.",
                2,
                "Do nothing.",
                3,
                title = "Select an option",
            )
        if (pick == 3) {
            return
        }
        if (!player.dontAskAgain) {
            startDialogue { mesbox(ENTRY_WARNING) }
            val confirm =
                choice3(
                    "Yes.",
                    1,
                    "Yes, and don't ask again.",
                    2,
                    "No.",
                    3,
                    title = "Are you sure you wish to proceed?",
                )
            when (confirm) {
                2 -> player.dontAskAgain = true
                3 -> return
            }
        }
        if (pick == 1) {
            startFight()
        } else {
            joinFight()
        }
    }

    private suspend fun ProtectedAccess.startFight() {
        val spec = buildSpec()
        val session =
            manager.createServerOwned(key, spec, InstanceAccess.Friends, worldClock.cycle)
                ?: run {
                    mes("No arena is available, try again shortly.")
                    return
                }
        val result = manager.join(player, session, worldClock.cycle, code = null, forceAccess = true)
        completeInstanceEntry(result)
    }

    private suspend fun ProtectedAccess.quickStart() {
        if (manager.sessionForPlayer(player) != null) {
            mes("You are already inside an instance.")
            return
        }
        startFight()
    }

    private suspend fun ProtectedAccess.joinFight() {
        if (manager.sessionForPlayer(player) != null) {
            mes("You are already inside an instance.")
            return
        }
        val name = stringDialog("Enter the player's name:")
        if (name.isBlank()) {
            mes("No player name entered.")
            return
        }
        val session = manager.sessionForKeyAndMemberName(key, name.trim())
        if (session == null) {
            mes("No fight found for '${name.trim()}'.")
            return
        }
        if (session.state is SessionState.Grace) {
            mes("That fight has already ended.")
            return
        }
        if (session.isFull()) {
            mes("That fight is full.")
            return
        }
        val uuid = player.uuid
        if (session.occupants.isNotEmpty() && uuid != null && uuid in session.departed) {
            mes("You cannot re-join a fight you have already left.")
            return
        }
        if (session.occupants.isNotEmpty()) {
            val failure = ongoingFightJoinFailure(session)
            if (failure != null) {
                mes(failure)
                return
            }
        }
        val result = manager.join(player, session, worldClock.cycle, code = null, forceAccess = false)
        completeInstanceEntry(result)
    }

    /** OSRS allows a late join until both titans have lost more than 25% of their health. */
    private fun ongoingFightJoinFailure(session: InstanceSession): String? {
        val npcs = manager.npcsForInstance(session.id)
        if (npcs.isEmpty()) return null
        val titans = npcs.filter { it.isType(FIRE_QUEEN) || it.isType(ICE_KING) }
        if (titans.isEmpty()) return "That fight has already ended."
        if (titans.none { it.hitpoints * 4 >= it.baseHitpointsLvl * 3 }) {
            return "You cannot join this fight; the Titans have already lost too much health."
        }
        return null
    }

    private suspend fun ProtectedAccess.quickEscape() {
        defaultLeaveFlow()
    }

    private fun ProtectedAccess.inspectTunnel() {
        mes(INSPECT_INFO)
    }

    private fun engageTitans(session: InstanceSession, player: Player) {
        val titans =
            manager
                .npcsForInstance(session.id)
                .filter { it.isType(FIRE_QUEEN) || it.isType(ICE_KING) }
        val queen = titans.firstOrNull { it.isType(FIRE_QUEEN) }
        val king = titans.firstOrNull { it.isType(ICE_KING) }
        if (queen == null || king == null) return
        encounter.start(session, queen, king, player)
    }

    private companion object {
        private const val SETTINGS_ROW = "dbrow.instance_royal_titans"
        private const val BEST_TIME_VARP = "varp.royal_titans_best_time"

        // DbRow id of "In the Pits" (dbtable.music). Its cache/midi id is 469, which is a
        // different row (a giant-lore dbrow); passing that id here throws while loading the
        // music row and took the server down on entry.
        private const val MUSIC = 2808
        private const val CRAWL_SOUND = 2454
        private const val FADE_CLEAR = 255
        private const val FADE_BLACK = 0
        private const val FADE_CLIENT_CYCLES = 50
        private const val ESCAPE_AP_RANGE = 40
        private const val ESCAPE_LOC_SYMBOL = "loc.royal_titans_exit"
        private const val ENTRANCE_LOC_SYMBOL = "loc.royal_titans_entrance"
        private const val INSPECT_INFO =
            "This tunnel leads out of the arena. Right-click it and select 'Quick-escape' to " +
                "leave the encounter."
        private const val ENTRY_WARNING =
            "You are about to begin an encounter with the Royal Titans. Dying<br>during this " +
                "encounter will not be considered a safe death. To leave,<br>you'll need to use " +
                "<col=ff0000>'Quick-escape'</col> via the tunnel or teleport out. Are<br>you " +
                "sure you wish to proceed?"

        private val FIRE_QUEEN = "npc.rt_fire_queen"
        private val ICE_KING = "npc.rt_ice_king"

        private val NPC_SPAWNS =
            listOf(
                InstanceNpc(FIRE_QUEEN, CoordGrid(2906, 9566)),
                InstanceNpc(ICE_KING, CoordGrid(2914, 9566)),
            )

        private val AREA =
            InstanceArea.copyRegions(
                centerRegionId = 11669,
                gridSize = 3,
                npcSpawns = NPC_SPAWNS,
            )
    }
}
