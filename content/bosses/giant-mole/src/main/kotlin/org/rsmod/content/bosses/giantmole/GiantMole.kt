package org.rsmod.content.bosses.giantmole

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.HitmarkTypeGroup
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.config.refs.done.hitmark_groups
import org.rsmod.api.npc.clearInteractionRoute
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.player.clearInteractionRoute
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.script.onEvent
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.entity.npc.NpcUid
import org.rsmod.game.hit.Hit
import org.rsmod.game.interact.InteractionNpc
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class GiantMole
@Inject
constructor(deps: BossDeps, private val environment: MoleHoleEnvironment) : BossPluginScript(deps) {
    private val moleNpcId by lazy {
        ServerCacheManager.getNpc("npc.mole_giant".asRSCM(RSCMType.NPC))?.id
            ?: error("Missing npc type: npc.mole_giant")
    }

    override val spec =
        boss("npc.mole_giant") {
            stats(attackRate = 4)

            val melee =
                ability("melee") {
                    anim("seq.mole_attack")
                    sound("synth.mole_attack", radius = 10)
                    hit {
                        damage(0..21).roll()
                        type(Melee)
                    }
                }

            phase("combat") {
                rotationSelector { +then(melee) }
            }
        }

    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps, onHit = { maybeBurrow() })
        onEvent<NpcStateEvents.Spawn>(moleNpcId) {
            npc.spawnCoords = GiantMoleRules.SPAWN
            resetBurrowState(npc)
        }
        onEvent<NpcStateEvents.Respawn> {
            if (npc.id == moleNpcId) {
                resetBurrowState(npc)
            }
        }
    }

    private fun resetBurrowState(npc: Npc) {
        npc.vars[BURROW_LOCK] = 0
        npc.vars[LAST_BURROW_CHECK] = -1
    }

    private fun NpcHitEvents.Impact.maybeBurrow() {
        if (npc.vars[BURROW_LOCK] != 0) {
            return
        }
        if (hit.matchesHitmarkGroup(hitmark_groups.poison_damage)) {
            return
        }
        if (hit.matchesHitmarkGroup(hitmark_groups.venom)) {
            return
        }
        if (GiantMoleRules.isMagicSplash(hit.type, hit.damage)) {
            return
        }
        if (!GiantMoleRules.canBurrow(npc.hitpoints, npc.baseHitpointsLvl)) {
            return
        }

        // A single attack can produce multiple hits (for example a scythe). The OSRS chance is
        // per attack, so only roll once on a given tick.
        val tick = deps.mapClock.cycle
        if (npc.vars[LAST_BURROW_CHECK] == tick) {
            return
        }
        npc.vars[LAST_BURROW_CHECK] = tick
        if (deps.random.of(GiantMoleRules.BURROW_CHANCE) != 0) {
            return
        }
        npc.burrow()
    }

    private fun Npc.burrow() {
        vars[BURROW_LOCK] = 1
        anim("seq.mole_burrow_down")
        deps.worldRepo.soundArea(coords, "synth.mole_burrow_down", radius = 10)
        playBurrowVisuals()
        throwMud()
        extinguishOpenLights()
        stopPlayersChasing()
        clearInteractionRoute()
        resetFaceEntity()
        defaultMode()

        val blocked = deps.playerList.map { it.coords }
        val candidates = GiantMoleRules.burrowCandidates(deps.collision, coords, blocked)
        if (candidates.isEmpty()) {
            vars[BURROW_LOCK] = 0
            return
        }
        val destination = candidates[deps.random.of(candidates.size)]
        deps.worldQueues.add(GiantMoleRules.BURROW_DELAY_TICKS) {
            if (hitpoints <= 0 || !isSlotAssigned) {
                return@add
            }
            stopPlayersChasing()
            telejump(deps.collision, destination)
            spawnCoords = destination
            playBurrowUpVisual(destination)
            anim("seq.mole_burrow_up")
            defaultMode()
            vars[BURROW_LOCK] = 0
        }
    }

    private fun Npc.playBurrowVisuals() {
        val edge = SpotanimType(BURROW_EDGE_SPOTANIM.asRSCM(RSCMType.SPOTANIM))
        val center = SpotanimType(BURROW_CENTER_SPOTANIM.asRSCM(RSCMType.SPOTANIM))
        val centerOffset = size / 2
        for (dx in 0 until size) {
            for (dz in 0 until size) {
                val spotanim = if (dx == centerOffset && dz == centerOffset) center else edge
                deps.worldRepo.spotanimMap(spotanim, coords.translate(dx, dz))
            }
        }
    }

    private fun Npc.playBurrowUpVisual(destination: CoordGrid) {
        val spotanim = SpotanimType(BURROW_UP_SPOTANIM.asRSCM(RSCMType.SPOTANIM))
        val centerOffset = size / 2
        deps.worldRepo.spotanimMap(spotanim, destination.translate(centerOffset, centerOffset))
    }

    private fun Npc.throwMud() {
        val script = MOLE_GO_SPLAT_SCRIPT.asRSCM(RSCMType.CLIENTSCRIPT)
        val model = MOLE_GO_SPLAT_MODEL.asRSCM(RSCMType.MODELS)
        for (player in deps.playerList) {
            if (!GiantMoleRules.withinMeleeRange(coords, player.coords)) {
                continue
            }
            player.runClientScript(
                script,
                charArrayOf('i', 'm'),
                listOf(MOLE_GO_SPLAT_DURATION, model),
            )
            player.soundSynth("synth.mud_pie_impact")
        }
    }

    private fun Npc.stopPlayersChasing() {
        val retaliationQueue = "queue.com_retaliate_npc".asRSCM(RSCMType.QUEUE)
        for (player in deps.playerList) {
            val interaction = player.interaction
            if (interaction is InteractionNpc && interaction.target === this) {
                player.clearInteractionRoute()
                player.resetFaceEntity()
            }
            player.clearRetaliationQueue(retaliationQueue, uid)
        }
    }

    private fun Player.clearRetaliationQueue(queueId: Int, npcUid: NpcUid) {
        val iterator = queueList.iterator() ?: return
        while (iterator.hasNext()) {
            val queue = iterator.next()
            if (queue.id == queueId && queue.args == npcUid) {
                iterator.remove()
            }
        }
    }

    private fun Npc.extinguishOpenLights() {
        for (player in deps.playerList) {
            if (!GiantMoleRules.withinMeleeRange(coords, player.coords)) {
                continue
            }
            val slot =
                player.inv.indices.firstOrNull { index ->
                    val obj = player.inv[index] ?: return@firstOrNull false
                    val type = ServerCacheManager.getItem(obj.id) ?: return@firstOrNull false
                    GiantMoleRules.openLightTarget(type.internalName) != null
                } ?: continue
            val obj = player.inv[slot] ?: continue
            val type = ServerCacheManager.getItem(obj.id) ?: continue
            val unlit = GiantMoleRules.openLightTarget(type.internalName) ?: continue
            player.inv[slot] = InvObj(unlit, obj.count, obj.vars)
            player.mes(
                "Your ${type.name.removePrefix("Lit ").lowercase()} seems to have been extinguished " +
                    "by the mud."
            )
            environment.refresh(player)
        }
    }

    private fun Hit.matchesHitmarkGroup(group: HitmarkTypeGroup): Boolean {
        val lit = group.lit.asRSCM(RSCMType.HITMARK)
        val tint = group.tint?.asRSCM(RSCMType.HITMARK)
        return hitmark.self == lit || (tint != null && hitmark.self == tint)
    }

    private companion object {
        private const val BURROW_LOCK = "varn.giant_mole_burrow_lock"
        private const val LAST_BURROW_CHECK = "varn.giant_mole_last_burrow_check"
        private const val BURROW_EDGE_SPOTANIM = "spotanim.mole_mud_cloud_spotanim"
        private const val BURROW_CENTER_SPOTANIM = "spotanim.mole_mude_hole_spotanim"
        private const val BURROW_UP_SPOTANIM = "spotanim.mole_mud_hole_up_spotanim"
        private const val MOLE_GO_SPLAT_SCRIPT = "clientscript.[clientscript,mole_go_splat]"
        private const val MOLE_GO_SPLAT_MODEL = "models.mole_go_splat_model"
        private const val MOLE_GO_SPLAT_DURATION = 135
    }
}
