package org.rsmod.content.bosses.brutus

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bossbar.plugin.BossHpBarScript
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceEnterTransition
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.instances.withInstanceEnterTransition
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onEvent
import org.rsmod.content.quest.area.lumbridge.idesofmilk.IdesOfMilkQuest
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

// The cow pen behind Gillie Groats. Live capture, rev 241, 20261012T180256-df49f48@1156-1169
// (quest) and @1549-1561, @1971-1977 (after the quest).
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
) : InstanceScript(registry) {

    private var Player.dontAskAgain by boolVarBit("varbit.cowboss_regular_dontaskagain")

    override fun settingsRow(): String = "dbrow.instance_cowboss"

    override fun area(): InstanceArea = AREA

    override fun runsPreludeOnFreshRun(): Boolean = true

    // live capture, rev 241, 20261012T180256-df49f48@1236: the quest kill reports no duration.
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
            withInstanceEnterTransition(InstanceEnterTransition(), enter)
            if (fresh) {
                spawnBrutus(player, session)
            }
        }
        onEvent<NpcStateEvents.Respawn> { if (npc.isType(Brutus.BRUTUS)) arrive(npc, fieldPlayer(npc)) }
        onEnterObject { openGate() }
        onExitObject { leaveGate() }
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
        if (leave) {
            defaultLeaveFlow()
        }
    }

    /**
     * Inside the field the release gate does nothing and the pen's west gate leads out. Live
     * capture, rev 241, 20261012T180256-df49f48@1165.
     */
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
        // live capture, rev 241, 20261012T180256-df49f48@1236, @1610: the quest kill isn't counted.
        if (!npc.respawns) {
            npc.vars["varn.skip_killcount"] = 1
        }
        manager.registerSessionNpc(player, npc)
        bossHpBar.onOpen(player, npc)
        arrive(npc, player)
    }

    /**
     * Brutus stands still after (re)spawning, moos, then goes for the player. He can be attacked
     * from the start, so this holds his own attack and movement rather than delaying the npc,
     * which would drop the player's clicks on him. Live capture, rev 241,
     * 20261012T180256-df49f48@1557-1567, @1632-1638 and 20261012T200152-df49f48@723-732.
     */
    private fun arrive(npc: Npc, player: Player?) {
        npc.anim("seq.cow_boss_spawn")
        deps.encounterRegistry.of(npc).nextAttackTick = deps.mapClock.cycle + ARRIVE_ATTACK_DELAY
        npc.movementLocked = true
        deps.worldQueues.add(ARRIVE_MOVE_DELAY) { npc.movementLocked = false }
        if (player == null) {
            return
        }
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

        // live capture, rev 241, 20261012T180256-df49f48@1165: Brutus is added at (3261, 3287).
        private val SPAWN = CoordGrid(3261, 3287)
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
