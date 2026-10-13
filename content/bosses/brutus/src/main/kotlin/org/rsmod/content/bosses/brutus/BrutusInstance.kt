package org.rsmod.content.bosses.brutus

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bossbar.plugin.BossHpBarScript
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.instances.hook.InstanceEnterAction
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.music.MusicPlayer
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.table.MusicRow
import org.rsmod.content.quest.area.lumbridge.idesofmilk.IdesOfMilkQuest
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

// The cow pen behind Gillie Groats (live capture, rev 241).
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class BrutusInstance
@Inject
constructor(
    registry: BossInstanceRegistry,
    private val deps: BossDeps,
    private val bossHpBar: BossHpBarScript,
    private val aiPlayerInteractions: AiPlayerInteractions,
    private val idesOfMilk: IdesOfMilkQuest,
    private val locRepo: LocRepository,
    private val musicPlayer: MusicPlayer,
) : InstanceScript(registry) {

    private var Player.dontAskAgain by boolVarBit("varbit.cowboss_regular_dontaskagain")

    override fun settingsRow(): String = "dbrow.instance_cowboss"

    override fun area(): InstanceArea = AREA

    override fun runsPreludeOnFreshRun(): Boolean = true

    // The quest kill reports no duration (live capture, rev 241).
    override fun recordsKillTime(player: Player): Boolean =
        idesOfMilk.stage(player) >= IdesOfMilkQuest.STAGE_COMPLETE

    override fun bestTimeVarp(): String = BrutusScoreboard.BEST_TIME

    override fun ScriptContext.configure() {
        onEnterPrelude { result, enter ->
            val session =
                when (result) {
                    is InstanceManager.Result.Created -> result.session
                    is InstanceManager.Result.Joined -> result.session
                    else -> return@onEnterPrelude
                }
            val fresh = result.isFreshRun()
            if (result is InstanceManager.Result.Created) {
                swapGates(session)
            }
            fieldEntry(enter) { if (fresh) spawnBrutus(player, session) }
        }
        onEvent<NpcStateEvents.Respawn> { if (npc.isType(Brutus.BRUTUS)) arrive(npc, fieldPlayer(npc)) }
        onEnterObject { openGate() }
        onExitObject { leaveGate() }
    }

    // live capture, rev 241
    private suspend fun ProtectedAccess.fieldEntry(enter: InstanceEnterAction, spawn: () -> Unit) {
        fadeOverlay(0, FADE_CLEAR, 0, FADE_BLACK, FADE_CLIENT_CYCLES)
        delay(1)
        minimapHideMap()
        delay(1)
        enter()
        spawn()
        delay(2)
        minimapReset()
        camForceAngle(ENTRY_CAMERA_PITCH, ENTRY_CAMERA_YAW)
        fadeOverlay(0, FADE_BLACK, 0, FADE_CLEAR, FADE_CLIENT_CYCLES)
        closeFadeOverlay(cycles = 2)
    }

    // https://oldschool.runescape.wiki/w/Transcript:The_Ides_of_Milk#Facing_the_bull
    private suspend fun ProtectedAccess.openGate() {
        val stage = idesOfMilk.stage(player)
        when {
            stage < IdesOfMilkQuest.STAGE_BULL_TASK ->
                startDialogue {
                    chatPlayer(neutral, "Releasing the bull into the cow pen is a bad idea.")
                    mesbox(
                        "You need to have completed The Ides of Milk quest to release this bull."
                    )
                }
            stage in IdesOfMilkQuest.STAGE_BRUTUS_KILLED until IdesOfMilkQuest.STAGE_COMPLETE ->
                startDialogue { chatPlayer(neutral, "Brutus has already been dealt with.") }
            else -> releaseBull(questFight = stage < IdesOfMilkQuest.STAGE_COMPLETE)
        }
    }

    private suspend fun ProtectedAccess.releaseBull(questFight: Boolean) {
        val (title, entersText) =
            if (questFight) {
                "Release the bull?" to "The bull enters. The cows scatter."
            } else {
                "Release Brutus?" to "Brutus enters. The cows scatter."
            }
        if (!player.dontAskAgain) {
            var choice = 0
            startDialogue {
                choice = choice3("Yes.", 1, "Yes, and don't ask again.", 2, "No.", 3, title = title)
            }
            when (choice) {
                2 -> player.dontAskAgain = true
                3 -> return
            }
        }
        mesboxNp(entersText)
        defaultInstanceEntry()
        startDialogue { mesbox(entersText) }
    }

    private suspend fun ProtectedAccess.leaveGate() {
        var leave = false
        startDialogue {
            leave = choice2("Yes.", true, "No.", false, title = "Are you sure you want to leave?")
        }
        if (!leave) {
            return
        }
        val session = manager.sessionForPlayer(player) ?: return
        val world = manager.fieldToWorld(session, player.coords)
        val outside = CoordGrid(EXIT_GATE_OUTSIDE_X, world.z.coerceIn(EXIT_GATE_Z))
        manager.leave(player, session, deps.mapClock.cycle)
        telejump(outside, TeleportType.Exempt)
    }

    // Inside the field the release gate does nothing and the pen's west gate leads out (live
    // capture, rev 241).
    private fun swapGates(session: InstanceSession) {
        for ((static, from, into) in GATE_SWAPS) {
            val coords = manager.resolveCoord(session, static) ?: continue
            val fromType = ServerCacheManager.getObject(from.asRSCM(RSCMType.LOC)) ?: continue
            val intoType = ServerCacheManager.getObject(into.asRSCM(RSCMType.LOC)) ?: continue
            val loc = locRepo.findExact(coords, fromType) ?: continue
            locRepo.change(loc, intoType, Int.MAX_VALUE)
        }
    }

    private fun spawnBrutus(player: Player, session: InstanceSession) {
        val coords = manager.resolveCoord(session, SPAWN) ?: return
        val type = ServerCacheManager.getNpc(BRUTUS_ID) ?: return
        val npc = Npc(type, coords)
        deps.npcRepo.add(npc, Int.MAX_VALUE)
        npc.respawns = idesOfMilk.stage(player) >= IdesOfMilkQuest.STAGE_COMPLETE
        // The quest kill isn't counted (live capture, rev 241).
        if (!npc.respawns) {
            npc.vars["varn.skip_killcount"] = 1
        }
        manager.registerSessionNpc(player, npc)
        // Once the screen has cleared (live capture, rev 241).
        deps.worldQueues.add(HP_BAR_DELAY) { if (npc.isSlotAssigned) bossHpBar.onOpen(player, npc) }
        arrive(npc, player)
    }

    // He can be attacked from the start, so this holds his own attack and movement: npc.delay
    // would drop the player's clicks on him (live capture, rev 241).
    private fun arrive(npc: Npc, player: Player?) {
        npc.anim("seq.cow_boss_spawn")
        val encounter = deps.encounterRegistry.of(npc)
        encounter.nextAttackTick = deps.mapClock.cycle + ARRIVE_ATTACK_DELAY
        encounter.lastAbilityTick = deps.mapClock.cycle
        npc.movementLocked = true
        deps.worldQueues.add(ARRIVE_MOVE_DELAY) { npc.movementLocked = false }
        if (player == null) {
            return
        }
        player.soundSynth("synth.brutus_spawn")
        musicPlayer.unlockAndPlay(player, MusicRow.getRow(BOSS_MOOSIC))
        npc.facePlayer(player)
        deps.worldQueues.add(ARRIVE_MOO_DELAY) { if (npc.isSlotAssigned) npc.say("Moo") }
        deps.worldQueues.add(ARRIVE_MOVE_DELAY) {
            if (npc.isSlotAssigned) npc.apPlayer2(player, aiPlayerInteractions)
        }
    }

    private fun fieldPlayer(npc: Npc): Player? {
        val session = manager.instanceForNpc(npc)?.let(manager::sessionForId) ?: return null
        return session.occupants.firstNotNullOfOrNull { uuid -> deps.playerList.firstOrNull { it.uuid == uuid } }
    }

    private companion object {
        private val BRUTUS_ID = Brutus.BRUTUS.asRSCM(RSCMType.NPC)

        // Boss Moosic, started with every spawn and unlocked by the first (live capture, rev 241).
        private const val BOSS_MOOSIC = 8662

        // live capture, rev 241
        private val SPAWN = CoordGrid(3261, 3287)

        // Out through the west gate (live capture, rev 241).
        private const val EXIT_GATE_OUTSIDE_X = 3252
        private val EXIT_GATE_Z = 3266..3267

        private const val FADE_CLEAR = 255
        private const val FADE_BLACK = 0
        private const val FADE_CLIENT_CYCLES = 50
        private const val ENTRY_CAMERA_PITCH = 280
        private const val ENTRY_CAMERA_YAW = 1024
        private const val HP_BAR_DELAY = 4

        private const val ARRIVE_MOO_DELAY = 5
        private const val ARRIVE_MOVE_DELAY = 5
        private const val ARRIVE_ATTACK_DELAY = 6

        // The field sits on the edge of 12851, and the client's scene reaches 52 tiles past the
        // player on every side, so the neighbouring regions are copied too.
        private val AREA = InstanceArea.copyRegions(centerRegionId = 12851, gridSize = 3)

        private val GATE_SWAPS =
            listOf(
                Triple(
                    CoordGrid(3262, 3294),
                    "loc.fencegate_l_cowboss_start",
                    "loc.fencegate_l_cowboss_start_noop",
                ),
                Triple(
                    CoordGrid(3263, 3294),
                    "loc.fencegate_r_cowboss_start",
                    "loc.fencegate_r_cowboss_start_noop",
                ),
                Triple(CoordGrid(3253, 3267), "loc.fencegate_r", "loc.fencegate_r_cowboss_exit"),
                Triple(CoordGrid(3253, 3266), "loc.fencegate_l", "loc.fencegate_l_cowboss_exit"),
            )
    }
}
