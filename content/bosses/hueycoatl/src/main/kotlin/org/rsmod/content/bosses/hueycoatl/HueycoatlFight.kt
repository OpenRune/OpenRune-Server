package org.rsmod.content.bosses.hueycoatl

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.MoveRestrict
import dev.openrune.types.NpcMode
import dev.openrune.types.aconverted.SpotanimType
import kotlin.math.abs
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bossbar.plugin.BossHpBarScript
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.bossProjectile
import org.rsmod.api.bosses.runtime.repeatTick
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.instances.InstanceId
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.instances.currentInstanceId
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.util.EntityFaceAngle
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.headbar.Headbar
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.game.movement.MoveSpeed
import org.rsmod.map.CoordGrid
import org.rsmod.map.util.Bounds

internal enum class HueyPhase {
    Idle,
    Body,
    Summit,
    Shielded,
    Final,
    Defeated,
}

internal sealed interface HueyFightKey {
    data object Public : HueyFightKey

    data class Instance(val id: InstanceId) : HueyFightKey
}

internal class HueyParticipant(val playerId: Long) {
    var bodyDamage: Int = 0
    var headDamage: Int = 0
    var tailDamage: Int = 0
    var neutralDamage: Int = 0
    var directDamage: Int = 0
    var enteredTick: Int = 0
    var perfect: Boolean = true
    var tookAvoidableDamage: Boolean = false

    val totalDamage: Int
        get() = bodyDamage + headDamage + tailDamage
}

internal class HueycoatlFight(
    val key: HueyFightKey,
    val session: InstanceSession?,
    private val deps: BossDeps,
    private val instances: InstanceManager,
    private val loot: HueycoatlLoot,
    private val hpBar: BossHpBarScript,
    private val wornBonuses: WornBonuses,
    private val worldStats: HueycoatlWorldStats,
) {
    private val players: PlayerList get() = deps.playerList

    private val origin: CoordGrid
    private var initialized = false
    private var disposed = false
    private var headRef: Npc? = null
    private val bodyRefs = mutableListOf<Npc>()
    private val deadBodies = mutableSetOf<Npc>()
    private var tailRef: Npc? = null
    private var seerRef: Npc? = null
    private val bodyStatic = mutableMapOf<Npc, CoordGrid>()

    val participants = linkedMapOf<Long, HueyParticipant>()

    var phase: HueyPhase = HueyPhase.Idle
        private set
    var braziersLit: Boolean = false
        private set
    var bodyPartsDead: Int = 0
        private set
    var fightStartTick: Int = 0
        private set
    var respawnAtTick: Int = 0
        private set

    private var attackNextTick = 0
    private var lightningNextTick = 0
    private var tailNextTick = 0
    private var seerBarNextTick = 0
    private var seerReminderNextTick = 0
    private var seerBodyNextTick = 0
    private var seerApproachingBody = false
    private var seerAtBody = false
    private var summitReady = false
    private val cosmeticNpcs = mutableListOf<Npc>()
    private var pillarStageNextTick = 0
    private var tailSide = 1
    private var stages = IntArray(3)
    private var lootRolled = false

    init {
        origin =
            session?.let { instances.resolveCoord(it, HueycoatlCoords.ARENA_ORIGIN) }
                ?: HueycoatlCoords.ARENA_ORIGIN
    }

    val head: Npc? get() = headRef
    val bodyParts: List<Npc> get() = bodyRefs
    val tail: Npc? get() = tailRef
    val seer: Npc? get() = seerRef

    fun initialize() {
        if (initialized || session != null && instances.sessionForId(session.id) == null) return
        initialized = true
        disposed = false
        spawnInitialNpcs()
        resetLocs()
        deps.repeatTick(
            Int.MAX_VALUE,
            onTick = {
                if (disposed) {
                    false
                } else {
                    tick()
                    true
                }
            },
        )
    }

    fun dispose() {
        disposed = true
        headRef?.let { head ->
            participantsInParticipation().forEach { (player, _) -> hpBar.onClose(player, head) }
        }
        deleteNpc(headRef)
        bodyRefs.forEach(::deleteNpc)
        deleteNpc(tailRef)
        deleteNpc(seerRef)
        cosmeticNpcs.forEach(::deleteNpc)
        cosmeticNpcs.clear()
        players.filter { participants.containsKey(it.uuid) }.forEach { it.npcViewDistance = null }
        headRef = null
        bodyRefs.clear()
        deadBodies.clear()
        tailRef = null
        seerRef = null
    }

    fun addParticipant(player: Player) {
        val id = player.uuid ?: return
        player.npcViewDistance = NPC_VIEW_DISTANCE
        val participant = participants.getOrPut(id) { HueyParticipant(id) }
        participant.enteredTick = now()
        if (phase == HueyPhase.Idle) startBody()
        if (phase != HueyPhase.Body && phase != HueyPhase.Defeated) {
            val barNpc =
                if (phase == HueyPhase.Shielded) tailRef ?: headRef ?: return else headRef ?: return
            hpBar.onOpen(player, barNpc)
        }
    }

    fun removeParticipant(player: Player) {
        val id = player.uuid ?: return
        player.npcViewDistance = null
        participants.remove(id)
        hpBar.onClose(player, headRef ?: return)
    }

    fun startIfIdle() {
        if (phase == HueyPhase.Idle) startBody()
    }

    fun resolveStatic(coord: CoordGrid): CoordGrid =
        session?.let { instances.resolveCoord(it, coord) } ?: coord

    fun playersInParticipation(): List<Player> {
        val sessionId = session?.id
        return players.filter { player ->
            (sessionId == null || player.currentInstanceId() == sessionId) &&
                HueycoatlArena.inParticipation(origin, player.coords)
        }
    }

    fun playersOnHazard(): List<Player> {
        val sessionId = session?.id
        return players.filter { player ->
            (sessionId == null || player.currentInstanceId() == sessionId) &&
                HueycoatlArena.inHazard(origin, player.coords)
        }
    }

    fun participantsInParticipation(): List<Pair<Player, HueyParticipant>> =
        playersInParticipation().mapNotNull { player ->
            val id = player.uuid ?: return@mapNotNull null
            val participant = participants[id] ?: return@mapNotNull null
            player to participant
        }

    private fun participantsOnHazard(): List<Player> =
        playersOnHazard().filter { player -> player.uuid?.let(participants::containsKey) == true }

    fun lootCoord(player: Player): CoordGrid {
        val head = headRef ?: return player.coords
        return resolveStatic(HueycoatlCoords.HEAD).translate(-1, -3)
    }

    fun recordHit(event: org.rsmod.api.npc.events.NpcHitEvents.Impact) {
        val npc = event.npc
        val damage = event.hit.damage
        if (damage <= 0 || !event.hit.isFromPlayer) return
        val player = event.hit.resolvePlayerSource(players) ?: return
        val id = player.uuid ?: return
        if (!playersInParticipation().any { it.uuid == id }) return
        val participant = participants[id] ?: return
        when {
            npc === headRef -> participant.headDamage += damage
            bodyRefs.any { it === npc } -> participant.bodyDamage += damage
            npc === tailRef -> participant.tailDamage += damage
            else -> return
        }
        if (event.hit.type == HitType.Typeless) {
            participant.neutralDamage += damage
        } else {
            participant.directDamage += damage
        }
        if (npc === headRef) {
            val head = headRef ?: return
            if (phase != HueyPhase.Shielded) {
                participantsInParticipation().forEach { (viewer, _) ->
                    hpBar.onUpdate(viewer, head)
                }
            }
            if (phase == HueyPhase.Summit && head.hitpoints <= head.baseHitpointsLvl / 2) {
                startShield()
            }
        } else if (npc === tailRef) {
            val tail = tailRef ?: return
            participantsInParticipation().forEach { (viewer, _) -> hpBar.onUpdate(viewer, tail) }
        }
    }

    fun modifyHit(event: org.rsmod.api.npc.events.NpcHitEvents.Modify) {
        val npc = event.npc
        val player =
            event.hit.sourceUid?.let {
                org.rsmod.game.entity.player.PlayerUid(it).resolve(players)
            } ?: return
        if (!event.hit.isFromPlayer) return
        val attackerId = player.uuid ?: return
        if (attackerId !in participants || phase == HueyPhase.Idle) {
            event.hit.damage = 0
            return
        }
        if (npc === headRef) {
            if (phase == HueyPhase.Body || phase == HueyPhase.Shielded) {
                event.hit.damage = 0
                return
            }
            if (braziersLit) event.hit.damage = (event.hit.damage * 1.3).toInt()
            return
        }
        if (bodyRefs.any { it === npc }) {
            if (braziersLit) event.hit.damage = (event.hit.damage * 1.3).toInt()
            return
        }
        if (npc === tailRef) {
            if (!HueycoatlArena.inHazard(origin, player.coords)) {
                event.hit.damage = 0
                return
            }
            var damage = event.hit.damage
            if (braziersLit) damage = (damage * 1.3).toInt()
            event.hit.damage = maxOf(1, minOf(damage, tailCap(player)))
        }
    }

    fun onNpcDeath(npc: Npc) {
        when {
            npc === headRef -> onHeadDeath(npc)
            bodyRefs.any { it === npc } -> onBodyDeath(npc)
            npc === tailRef -> onTailDeath(npc)
        }
    }

    private fun startBody() {
        if (phase != HueyPhase.Idle) return
        phase = HueyPhase.Body
        fightStartTick = now()
        attackNextTick = fightStartTick + 20
        lightningNextTick = fightStartTick + 36
        seerBodyNextTick = fightStartTick + 17
        if (!seerApproachingBody && !seerAtBody) {
            seerRef?.let { seer ->
                seerApproachingBody = true
                seer.movementLocked = false
                seer.defaultMoveSpeed = MoveSpeed.Run
                seer.walk(HueycoatlCoords.SEER_START_PATH.map(::resolveStatic)) {
                    if (seer === seerRef) {
                        seerApproachingBody = false
                        seerAtBody = true
                        seer.movementLocked = true
                        seer.defaultMoveSpeed = MoveSpeed.Walk
                    }
                }
                seer.say("Let's make this beastie move!")
            }
        }
    }

    private fun startSummit() {
        if (phase != HueyPhase.Body) return
        phase = HueyPhase.Summit
        summitReady = false
        animateBarrier(HueycoatlSeqs.BARRIER_TRANSITION, retract = true)
        deps.worldRepo.soundArea(resolveStatic(HueycoatlCoords.BARRIER), HueycoatlSounds.BARRIER_TRANSITION, radius = 10)
        attackNextTick = Int.MAX_VALUE
        lightningNextTick = Int.MAX_VALUE
        seerBarNextTick = Int.MAX_VALUE
        seerReminderNextTick = Int.MAX_VALUE
        pillarStageNextTick = Int.MAX_VALUE
        val seer = seerRef
        seerAtBody = false
        seerApproachingBody = false
        deps.worldQueues.add(1) {
            if (disposed || phase != HueyPhase.Summit) return@add
            message("<col=a53fff>The Hueycoatl shifts, opening a path up the mountain.</col>")
            if (seer != null && seer === seerRef && seer.isSlotAssigned) {
                seer.say("Nicely done!")
                seer.movementLocked = false
                seer.defaultMoveSpeed = MoveSpeed.Run
                seer.walk(HueycoatlCoords.SEER_BODY_PATH.map(::resolveStatic))
            }
        }
        deps.worldQueues.add(6) {
            if (!disposed && seer != null && seer === seerRef && seer.isSlotAssigned) {
                seer.say("Steady now!")
            }
        }
        deps.worldQueues.add(10) {
            if (disposed || phase != HueyPhase.Summit || summitReady) return@add
            if (seer != null && seer === seerRef && seer.isSlotAssigned) {
                seer.say("Let's go!")
                seer.walk(HueycoatlCoords.SEER_SUMMIT_PATH.map(::resolveStatic)) {
                    if (!disposed && seer === seerRef) beginSummit(seer)
                }
            } else {
                beginSummit(null)
            }
        }
        deps.worldQueues.add(30) {
            if (!disposed && !summitReady) {
                beginSummit(seer)
            }
        }
        participantsInParticipation().forEach { (player, _) -> headRef?.let { hpBar.onOpen(player, it) } }
    }

    private fun beginSummit(seer: Npc?) {
        if (summitReady || phase != HueyPhase.Summit) return
        summitReady = true
        if (seer != null && seer === seerRef && seer.isSlotAssigned) {
            seer.movementLocked = true
            seer.defaultMoveSpeed = MoveSpeed.Walk
            seer.say("Keep praying to bolster your strength!")
        }
        seerRef?.showHeadbar(Headbar.fromNoSource(34, 34, 0, 60, 0, 900))
        val tick = now()
        attackNextTick = tick + 2
        lightningNextTick = tick + 1
        seerBarNextTick = tick + 30
        seerReminderNextTick = tick + 30
        pillarStageNextTick = tick + 2
        stages = IntArray(3)
        updatePillarLocs()
    }

    private fun startShield() {
        if (phase != HueyPhase.Summit) return
        val head = headRef ?: return
        phase = HueyPhase.Shielded
        message("<col=a53fff>The Hueycoatl shields itself!</col>")
        head.anim(HueycoatlSeqs.SHIELD_ACTIVATION)
        transmog(head, HueycoatlNpcs.HEAD_INVULNERABLE)
        deps.worldRepo.soundArea(head.coords, HueycoatlSounds.SHIELD_ACTIVATION, radius = 15)
        spawnTail()
        seerRef?.say("It's shielded itself! You need to break its concentration!")
        attackNextTick = now() + 8
        lightningNextTick = now() + 18
        tailRef?.let { tail ->
            participantsInParticipation().forEach { (player, _) -> hpBar.onOpen(player, tail) }
        }
    }

    private fun startFinal() {
        if (phase != HueyPhase.Shielded) return
        val head = headRef ?: return
        phase = HueyPhase.Final
        message("<col=a53fff>The Hueycoatl's shield falls!</col>")
        head.anim(HueycoatlSeqs.SCREECH)
        deps.worldQueues.add(2) {
            if (head === headRef && head.isSlotAssigned && phase == HueyPhase.Final) {
                transmog(head, HueycoatlNpcs.HEAD)
                head.anim(HueycoatlSeqs.SCREECH_RESET)
                participantsInParticipation().forEach { (player, _) ->
                    hpBar.onOpen(player, head)
                }
            }
        }
        attackNextTick = now() + 4
        participantsInParticipation().forEach { (player, _) -> hpBar.onOpen(player, head) }
    }

    private fun onBodyDeath(npc: Npc) {
        if (!deadBodies.add(npc)) return
        val static = bodyStatic[npc] ?: npc.spawnCoords
        val cave = HueycoatlCoords.CAVE_BY_BODY[static]
        if (cave != null) {
            val (coord, rotation) = cave
            val resolved = resolveStatic(coord)
            val diagonal = static in HueycoatlCoords.CAVE_DIAGONAL
            val openSymbol = if (diagonal) HueycoatlLocs.CAVE_DIAG else HueycoatlLocs.CAVE
            val currentSymbol =
                if (diagonal) HueycoatlLocs.CAVE_DIAG_HUEY else HueycoatlLocs.CAVE_HUEY
            val currentId = currentSymbol.asRSCM(RSCMType.LOC)
            val seq = if (diagonal) HueycoatlSeqs.BODY_DEATH_DIAG else HueycoatlSeqs.BODY_DEATH
            val existing = deps.locRepo.findAll(resolved).firstOrNull { it.id == currentId }
            if (existing != null) deps.worldRepo.locAnim(existing, seq)
            deps.worldQueues.add(3) {
                if (disposed) return@add
                val loc =
                    deps.locRepo.add(
                        resolved,
                        openSymbol,
                        Int.MAX_VALUE,
                        LocAngle[rotation],
                        LocShape[10],
                    )
                if (existing == null) deps.worldRepo.locAnim(loc, seq)
            }
        }
        npc.anim(HueycoatlSeqs.BODY_DEATH)
        npc.hideAllOps()
        npc.ignoreCombatInteractions = true
        npc.mode = NpcMode.None
        npc.movementLocked = true
        npc.hitpoints = 0
        transmog(npc, HueycoatlNpcs.BODY_BROKEN)
        deps.worldRepo.soundArea(npc.coords, HueycoatlSounds.BODY_DEATH, radius = 8)
        bodyPartsDead++
        if (bodyPartsDead >= 5) {
            startSummit()
        } else {
            animateBarrier(HueycoatlSeqs.BARRIER_DAMAGE)
        }
    }

    private fun onTailDeath(npc: Npc) {
        if (phase != HueyPhase.Shielded) return
        npc.anim(HueycoatlSeqs.TAIL_BREAK)
        npc.hideAllOps()
        npc.ignoreCombatInteractions = true
        npc.mode = NpcMode.None
        npc.movementLocked = true
        npc.hitpoints = 0
        transmog(npc, HueycoatlNpcs.TAIL_BROKEN)
        deps.worldRepo.soundArea(npc.coords, HueycoatlSounds.TAIL_BREAK, radius = 15)
        seerRef?.say("Well done! Now, let's weaken it a little more!")
        startFinal()
    }

    private fun onHeadDeath(npc: Npc) {
        if (phase == HueyPhase.Defeated) return
        phase = HueyPhase.Defeated
        npc.anim(HueycoatlSeqs.KNOCKOUT)
        npc.hideAllOps()
        npc.ignoreCombatInteractions = true
        npc.mode = NpcMode.None
        npc.movementLocked = true
        npc.hitpoints = 0
        transmog(npc, HueycoatlNpcs.HEAD_DEFEATED)
        val deathTick = now()
        respawnAtTick = deathTick + 50
        deps.worldQueues.add(39) {
            if (!disposed && phase == HueyPhase.Defeated) {
                message("<col=a53fff>The Hueycoatl begins to awaken...</col>")
                if (npc === headRef && npc.isSlotAssigned) {
                    npc.anim(HueycoatlSeqs.AWAKEN)
                }
            }
        }
        seerRef?.say("We did it! Well done!")
        seerRef?.removeHeadbar(34)
        tailRef?.let { tail ->
            if (tail.isSlotAssigned) {
                tail.anim(HueycoatlSeqs.TAIL_DISAPPEAR)
                deps.worldQueues.add(2) {
                    if (!disposed && tail === tailRef && tail.isSlotAssigned) {
                        deleteNpc(tail)
                        tailRef = null
                    }
                }
            }
        }
        spawnVictoryWorkers()
        deps.worldQueues.add(38) {
            if (!disposed && phase == HueyPhase.Defeated) spawnWarningWorkers()
        }
        if (!lootRolled) {
            lootRolled = true
            awardKillStats()
            loot.award(this)
        }
        participantsInParticipation().forEach { (player, _) -> hpBar.onClose(player, npc) }
    }

    private fun awardKillStats() {
        val duration = now() - fightStartTick
        worldStats.addKillAndTime(duration)
        for ((player, participant) in participantsInParticipation()) {
            val count = player.vars["varp.total_huey_kills"] + 1
            VarPlayerIntMapSetter.set(player, "varp.total_huey_kills", count)
            player.mes("Your Hueycoatl kill count is: <col=ff0000>$count</col>.")
            val streak =
                if (participant.perfect) {
                    player.vars["varp.huey_perfect_streak"] + 1
                } else {
                    0
                }
            VarPlayerIntMapSetter.set(player, "varp.huey_perfect_streak", streak)
            val best = player.vars["varp.huey_best_time"]
            val formatted = HueycoatlArena.formatDuration(duration)
            val personalBest =
                if (best <= 0 || duration < best) {
                    VarPlayerIntMapSetter.set(player, "varp.huey_best_time", duration)
                    " (new personal best)"
                } else {
                    ". Personal best: ${HueycoatlArena.formatDuration(best)}"
                }
            player.mes("Fight duration: <col=ff0000>$formatted</col>$personalBest")
        }
    }

    private fun tick() {
        val now = now()
        when (phase) {
            HueyPhase.Idle -> {
                if (participantsOnHazard().isNotEmpty()) startBody()
                return
            }
            HueyPhase.Defeated -> {
                if (now >= respawnAtTick) reset()
                return
            }
            else -> Unit
        }
        val head = headRef ?: return
        if (!head.isSlotAssigned || head.hitpoints <= 0) return
        if (participantsInParticipation().isEmpty()) return
        if (phase == HueyPhase.Body && now >= seerBodyNextTick) {
            seerRef?.say("Block its attacks with your prayers!")
            seerBodyNextTick = now + 15
        }
        if (now >= attackNextTick) {
            attack()
            attackNextTick =
                now +
                    when (phase) {
                        HueyPhase.Body, HueyPhase.Summit -> 20
                        HueyPhase.Shielded -> 15
                        HueyPhase.Final -> 11
                        else -> 20
                    }
        }
        if (now >= lightningNextTick) {
            lightning()
            lightningNextTick = now + if (phase == HueyPhase.Final) 24 else 36
        }
        if ((phase == HueyPhase.Shielded || phase == HueyPhase.Final) && now >= tailNextTick) {
            tailCycle()
        }
        if (phase == HueyPhase.Summit || phase == HueyPhase.Shielded || phase == HueyPhase.Final) {
            if (now >= pillarStageNextTick) {
                pillarStageTick()
                pillarStageNextTick = now + 2
            }
            if (now >= seerBarNextTick) {
                seerBarCheck()
                seerBarNextTick = now + 30
            }
            if (now >= seerReminderNextTick) {
                val seer = seerRef
                seer?.say("We need to charge all of the pillars using our prayers!")
                deps.worldQueues.add(4) {
                    if (!disposed && seer != null && seer === seerRef && seer.isSlotAssigned) {
                        if (phase == HueyPhase.Summit ||
                            phase == HueyPhase.Shielded ||
                            phase == HueyPhase.Final
                        ) {
                            seer.say("Keep praying to charge the pillars!")
                        }
                    }
                }
                seerReminderNextTick = now + 30
            }
        }
    }

    private fun attack() {
        val targets = playersInParticipation()
        if (targets.isEmpty()) return
        val head = headRef ?: return
        val style = deps.random.of(3)
        val shielded = phase == HueyPhase.Shielded
        val (seq, launch, projectile, impact) =
            when (style) {
                0 ->
                    Quad(
                        if (shielded) HueycoatlSeqs.SHIELD_MELEE else HueycoatlSeqs.ATTACK_MELEE,
                        HueycoatlGfx.MELEE_LAUNCH,
                        HueycoatlGfx.MELEE_PROJECTILE,
                        HueycoatlGfx.MELEE_IMPACT,
                    )
                1 ->
                    Quad(
                        if (shielded) HueycoatlSeqs.SHIELD_RANGED else HueycoatlSeqs.ATTACK_RANGED,
                        HueycoatlGfx.RANGED_LAUNCH,
                        HueycoatlGfx.RANGED_PROJECTILE,
                        HueycoatlGfx.RANGED_IMPACT,
                    )
                else ->
                    Quad(
                        if (shielded) HueycoatlSeqs.SHIELD_MAGIC else HueycoatlSeqs.ATTACK_MAGIC,
                        HueycoatlGfx.MAGIC_LAUNCH,
                        HueycoatlGfx.MAGIC_PROJECTILE,
                        HueycoatlGfx.MAGIC_IMPACT,
                    )
            }
        head.anim(seq)
        head.spotanim(launch, slot = 0)
        val travel = listOf(150, 180, 210)[deps.random.of(3)]
        val impactTicks = travel / 30
        deps.worldQueues.add(4) {
            if (head !== headRef || !head.isSlotAssigned || head.hitpoints <= 0) return@add
            val active = targets.filter { it.isSlotAssigned && it.hitpoints > 0 }
            if (active.isEmpty()) return@add
            deps.worldRepo.soundArea(head.coords, HueycoatlSounds.HEAD_PROJECTILE, radius = 10)
            val spot = projectile.asRSCM(RSCMType.SPOTANIM)
            for (target in active) {
                val towardHead = Direction.between(Bounds(target.coords), head.bounds())
                val src = target.coords.translate(towardHead.xOff, towardHead.zOff)
                deps.bossProjectile(
                    spotanim = spot,
                    src = src,
                    target = target.coords,
                    startHeight = 900,
                    endHeight = 200,
                    delay = 0,
                    travel = travel,
                    curve = 80,
                    progress = 0,
                    homing = target,
                    viewer = target,
                )
            }
            deps.worldQueues.add(impactTicks) {
                val participating = playersInParticipation().mapNotNull { it.uuid }.toSet()
                for (target in active) {
                    if (!target.isSlotAssigned || target.hitpoints <= 0) continue
                    val id = target.uuid ?: continue
                    if (id !in participating) continue
                    target.spotanim(impact, slot = 2)
                    damageHeadProjectile(target, style)
                }
            }
        }
    }

    private fun damageHeadProjectile(player: Player, style: Int) {
        val correct =
            when (style) {
                0 -> player.vars["varbit.prayer_protectfrommelee"] > 0
                1 -> player.vars["varbit.prayer_protectfrommissiles"] > 0
                else -> player.vars["varbit.prayer_protectfrommagic"] > 0
            }
        val head = headRef ?: return
        if (correct) {
            val damage = deps.random.of(0, 2)
            player.queueHit(head, 1, HitType.Typeless, damage, NoopPlayerHitModifier)
            return
        }
        val hit =
            when (style) {
                0 -> deps.accuracy.rollMeleeAccuracy(head, player, MeleeAttackType.Crush, deps.random)
                1 -> deps.accuracy.rollRangedAccuracy(head, player, deps.random)
                else -> deps.accuracy.rollMagicAccuracy(head, player, deps.random)
            }
        val damage = if (hit) deps.random.of(0, 14) else 0
        val type = when (style) {
            0 -> HitType.Melee
            1 -> HitType.Ranged
            else -> HitType.Magic
        }
        player.queueHit(head, 1, type, damage, NoopPlayerHitModifier)
        if (damage > 0) markAvoidable(player)
    }

    private fun lightning() {
        val head = headRef ?: return
        val anchors: List<CoordGrid>
        val extraPool: List<CoordGrid>
        val extraCount: Int
        if (phase == HueyPhase.Body) {
            anchors = HueycoatlGlyphs.BODY_ANCHORS
            extraPool = HueycoatlGlyphs.BODY_EXTRAS
            extraCount = deps.random.of(8, 9)
        } else {
            anchors = HueycoatlGlyphs.SUMMIT_ANCHORS
            extraPool = HueycoatlGlyphs.SUMMIT_EXTRAS
            extraCount = deps.random.of(6, 13)
        }
        val playerTiles = playersOnHazard().map { it.coords }.distinct()
        val anchorTiles = anchors.map(::resolveStatic)
        val extras = extraPool.map(::resolveStatic).toMutableList()
        val centers = anchorTiles.toMutableList()
        for (tile in playerTiles) {
            if (tile in anchorTiles || tile in centers) continue
            extras.remove(tile)
            centers += tile
        }
        val targetExtras = maxOf(extraCount, playerTiles.size)
        repeat((anchors.size + targetExtras - centers.size).coerceAtLeast(0)) {
            if (extras.isEmpty()) return@repeat
            centers += extras.removeAt(deps.random.of(extras.size))
        }
        val prayer = SpotanimType(HueycoatlGfx.PRAYER.asRSCM(RSCMType.SPOTANIM))
        for (center in centers) {
            deps.locRepo.add(center, HueycoatlLocs.ATTACK_TILE, 5, LocAngle[deps.random.of(4)], LocShape[10])
            deps.worldRepo.spotanimMap(prayer, center, delay = 120)
        }
        val soundTile =
            resolveStatic(if (phase == HueyPhase.Body) CoordGrid(1524, 3274) else CoordGrid(1512, 3283))
        deps.worldQueues.add(4) {
            if (head !== headRef) return@add
            deps.worldRepo.soundArea(soundTile, HueycoatlSounds.LIGHTNING_IMPACT, radius = 31, size = 31)
            for (player in playersOnHazard()) {
                if (player.coords in centers) {
                    val damage = deps.random.of(0, 16)
                    player.queueHit(head, 1, HitType.Typeless, damage, NoopPlayerHitModifier)
                    if (damage > 0) markAvoidable(player)
                }
            }
        }
    }

    private fun tailCycle() {
        val tail = tailRef ?: return
        if (phase == HueyPhase.Shielded || phase == HueyPhase.Final) {
            tail.anim(HueycoatlSeqs.TAIL_DISAPPEAR)
            deps.worldQueues.add(2) { tailShift(tail) }
        } else {
            tailShift(tail)
        }
        if (phase == HueyPhase.Shielded) seerRef?.say("Go for the tail! It's a weakspot!")
        tailNextTick = now() + 36
    }

    private fun tailShift(tail: Npc) {
        if (tail !== tailRef || !tail.isSlotAssigned) return
        tailSide = -tailSide
        val dest =
            resolveStatic(if (tailSide > 0) HueycoatlCoords.TAIL_WEST else HueycoatlCoords.TAIL_EAST)
        tail.telejump(deps.collision, dest)
        faceCardinal(tail, if (tailSide > 0) Direction.East else Direction.West)
        tail.anim(HueycoatlSeqs.TAIL_APPEAR)
        deps.worldRepo.soundArea(tail.coords, HueycoatlSounds.TAIL_APPEAR, radius = 31, size = 31)
        deps.worldQueues.add(2) {
            if (tail === tailRef && tail.isSlotAssigned) tailSlam(tail)
        }
    }

    private fun spawnTail() {
        deleteNpc(tailRef)
        tailSide = 1
        val tail = spawnNpc(HueycoatlNpcs.TAIL, HueycoatlCoords.TAIL_EAST, 300)
        tail.respawnDir = Direction.West
        faceCardinal(tail, Direction.West)
        tailRef = tail
        deps.worldQueues.add(1) {
            if (tail === tailRef && tail.isSlotAssigned) {
                tail.telejump(deps.collision, resolveStatic(HueycoatlCoords.TAIL_WEST))
                faceCardinal(tail, Direction.East)
                tail.anim(HueycoatlSeqs.TAIL_APPEAR)
                deps.worldRepo.soundArea(tail.coords, HueycoatlSounds.TAIL_APPEAR, radius = 31, size = 31)
                deps.worldQueues.add(2) {
                    if (tail === tailRef && tail.isSlotAssigned) tailSlam(tail)
                }
            }
        }
        tailNextTick = now() + 36
    }

    private fun faceCardinal(npc: Npc, direction: Direction) {
        npc.infoProtocol.setFaceAngle(direction.angle, instant = true)
    }

    private fun tailSlam(tail: Npc) {
        deps.worldRepo.soundArea(tail.coords, HueycoatlSounds.TAIL_SLAM, radius = 31, size = 31)
        val waveTiles = spawnTailWave(tail)
        if (phase == HueyPhase.Shielded) seerRef?.say("Watch out for its slam!")
        val tilesByTick = waveTiles.groupBy { it.delayTicks }
        for ((delayTicks, tiles) in tilesByTick) {
            val coords = tiles.mapTo(HashSet()) { it.coords }
            deps.worldQueues.add(delayTicks) {
                if (tail !== tailRef || !tail.isSlotAssigned) return@add
                for (player in playersInParticipation()) {
                    if (player.coords !in coords) continue
                    if (!player.isSlotAssigned || player.hitpoints <= 0) continue
                    val damage = deps.random.of(20, 25)
                    player.queueHit(tail, 1, HitType.Typeless, damage, NoopPlayerHitModifier)
                    markAvoidable(player)
                }
            }
        }
    }

    private fun spawnTailWave(tail: Npc): List<TailWaveTile> {
        val waveOrigin =
            if (tailSide > 0) tail.coords.translate(3, 1) else tail.coords.translate(1, 3)
        val maxDistance = 16
        val waveTiles = mutableListOf<TailWaveTile>()
        for (dx in -maxDistance..maxDistance) {
            for (dz in -maxDistance..maxDistance) {
                val distance = abs(dx) + abs(dz)
                if (distance > maxDistance) continue
                val tile = waveOrigin.translate(dx, dz)
                if (!inTailWaveArea(tile)) continue
                if (tile != waveOrigin && deps.collision.isWalkBlocked(tile)) continue
                val wave =
                    when {
                        dx > 0 && dz > 0 -> HueycoatlGfx.TAIL_WAVE_NORTHEAST
                        dx < 0 && dz > 0 -> HueycoatlGfx.TAIL_WAVE_NORTHWEST
                        dx > 0 && dz < 0 -> HueycoatlGfx.TAIL_WAVE_SOUTHEAST
                        dx < 0 && dz < 0 -> HueycoatlGfx.TAIL_WAVE_SOUTHWEST
                        dx > 0 -> HueycoatlGfx.TAIL_WAVE_EAST
                        dx < 0 -> HueycoatlGfx.TAIL_WAVE_WEST
                        dz > 0 -> HueycoatlGfx.TAIL_WAVE_NORTH
                        dz < 0 -> HueycoatlGfx.TAIL_WAVE_SOUTH
                        else -> if (tailSide > 0) HueycoatlGfx.TAIL_WAVE_EAST else HueycoatlGfx.TAIL_WAVE_WEST
                    }
                val delay = distance * 15 + 30
                deps.worldRepo.spotanimMap(
                    SpotanimType(wave.asRSCM(RSCMType.SPOTANIM)),
                    tile,
                    delay = delay,
                )
                waveTiles += TailWaveTile(tile, (delay + 29) / 30)
            }
        }
        return waveTiles
    }

    private fun inTailWaveArea(tile: CoordGrid): Boolean {
        val dx = tile.x - origin.x
        val dz = tile.z - origin.z
        val body = dx in 12..27 && dz in 0..12
        val summit = dx in 0..15 && dz in 6..21
        return body || summit
    }

    private fun pillarStageTick() {
        val teamSize = playersInParticipation().size
        val required = HueycoatlArena.requiredPrayers(teamSize)
        val magic = playersInParticipation().count { it.vars["varbit.prayer_protectfrommagic"] > 0 }
        val ranged = playersInParticipation().count { it.vars["varbit.prayer_protectfrommissiles"] > 0 }
        val melee = playersInParticipation().count { it.vars["varbit.prayer_protectfrommelee"] > 0 }
        stages[0] = if (magic >= required) minOf(5, stages[0] + 1) else maxOf(0, stages[0] - 1)
        stages[1] = if (ranged >= required) minOf(5, stages[1] + 1) else maxOf(0, stages[1] - 1)
        stages[2] = if (melee >= required) minOf(5, stages[2] + 1) else maxOf(0, stages[2] - 1)
        updatePillarLocs()
    }

    private fun seerBarCheck() {
        val head = headRef ?: return
        braziersLit = stages.all { it >= 5 }
        updatePillarLocs()
        seerRef?.let { seer ->
            seer.showHeadbar(Headbar.fromNoSource(34, 34, 0, 60, 0, 900))
            deps.worldRepo.soundArea(seer.coords, HueycoatlSounds.SEER_CHARGE_A, delay = 21, radius = 10)
            deps.worldRepo.soundArea(seer.coords, HueycoatlSounds.SEER_CHARGE_B, delay = 81, radius = 10)
            deps.worldRepo.soundArea(seer.coords, HueycoatlSounds.SEER_CHARGE_C, delay = 108, radius = 10)
            deps.worldRepo.soundArea(seer.coords, HueycoatlSounds.SEER_CHARGE_D, delay = 122, radius = 10)
        }
        val barNpc = if (phase == HueyPhase.Shielded) tailRef ?: head else head
        participantsInParticipation().forEach { (player, _) -> hpBar.onUpdate(player, barNpc) }
    }

    private fun updatePillarLocs() {
        updatePillar(HueycoatlCoords.MAGIC_PILLAR, "magic", stages[0])
        updatePillar(HueycoatlCoords.RANGED_PILLAR, "ranged", stages[1])
        updatePillar(HueycoatlCoords.MELEE_PILLAR, "melee", stages[2])
    }

    private fun updatePillar(static: CoordGrid, type: String, stage: Int) {
        val tile = resolveStatic(static)
        val brazier =
            "loc.huey_prayer_pillar_${type}_brazier_${if (braziersLit) "on" else "off"}"
        deps.locRepo.add(tile, brazier, Int.MAX_VALUE, LocAngle[3], LocShape[11])
        val stageLoc =
            if (stage <= 0) "loc.huey_prayer_pillar_${type}_empty"
            else "loc.huey_prayer_pillar_${type}_s$stage"
        deps.locRepo.add(tile, stageLoc, Int.MAX_VALUE, LocAngle[0], LocShape[6])
    }

    private fun reset() {
        cosmeticNpcs.forEach(::deleteNpc)
        cosmeticNpcs.clear()
        deleteNpc(headRef)
        bodyRefs.forEach(::deleteNpc)
        deleteNpc(tailRef)
        headRef = null
        bodyRefs.clear()
        deadBodies.clear()
        tailRef = null
        bodyStatic.clear()
        spawnInitialNpcs(spawnSeer = false)
        headRef?.anim(HueycoatlSeqs.AWAKEN_RESET)
        resetLocs(afterKill = true)
        participants.values.forEach {
            it.bodyDamage = 0
            it.headDamage = 0
            it.tailDamage = 0
            it.neutralDamage = 0
            it.directDamage = 0
            it.perfect = true
            it.tookAvoidableDamage = false
            it.enteredTick = now()
        }
        bodyPartsDead = 0
        stages = IntArray(3)
        braziersLit = false
        tailSide = 1
        lootRolled = false
        phase = if (participantsOnHazard().isNotEmpty()) HueyPhase.Body else HueyPhase.Idle
        if (phase == HueyPhase.Body) {
            fightStartTick = now()
            attackNextTick = fightStartTick + 20
            lightningNextTick = fightStartTick + 36
            seerBodyNextTick = fightStartTick + 24
        } else {
            seerBodyNextTick = 0
        }
        seerBarNextTick = 0
        pillarStageNextTick = 0
        respawnAtTick = 0
        summitReady = false
        seerApproachingBody = true
        seerAtBody = false
        for (player in playersOnHazard()) {
            player.clearInteraction()
            player.abortRoute()
            PathingEntityCommon.telejump(player, deps.collision, resolveStatic(HueycoatlCoords.RESET))
            headRef?.let { head -> player.faceSquare(head.coords) }
            player.anim(HueycoatlSeqs.HUMAN_KNOCKDOWN)
            deps.worldQueues.add(1) {
                if (player.isSlotAssigned) player.anim(HueycoatlSeqs.HUMAN_GETUP)
            }
        }
        seerRef?.let { seer ->
            seer.removeHeadbar(34)
            seer.clearInteraction()
            seer.hideAllOps()
            seer.ignoreCombatInteractions = true
            seer.movementLocked = true
            seer.moveRestrict = MoveRestrict.Normal
            seer.telejump(deps.collision, resolveStatic(HueycoatlCoords.SEER_BASE))
            headRef?.let { head -> seer.faceSquare(head.coords) }
            seer.anim(HueycoatlSeqs.HUMAN_KNOCKDOWN)
            seer.say("Oof!")
            deps.worldQueues.add(1) {
                if (!disposed && seer === seerRef && seer.isSlotAssigned) {
                    seer.anim(HueycoatlSeqs.HUMAN_GETUP)
                }
            }
            deps.worldQueues.add(2) {
                if (!disposed && seer === seerRef && seer.isSlotAssigned) {
                    seer.movementLocked = false
                    seer.defaultMoveSpeed = MoveSpeed.Run
                    seer.walk(HueycoatlCoords.SEER_START_PATH.map(::resolveStatic)) {
                        if (seer === seerRef) {
                            seerApproachingBody = false
                            seerAtBody = true
                            seer.movementLocked = true
                            seer.defaultMoveSpeed = MoveSpeed.Walk
                        }
                    }
                }
            }
            deps.worldQueues.add(4) {
                if (!disposed && seer === seerRef && seer.isSlotAssigned) {
                    seer.say("Let's convince that beastie to move!")
                }
            }
        }
    }

    private fun spawnInitialNpcs(spawnSeer: Boolean = true) {
        headRef = spawnNpc(HueycoatlNpcs.HEAD, HueycoatlCoords.HEAD, 2500)
        bodyRefs.clear()
        bodyStatic.clear()
        for (coord in HueycoatlCoords.BODY_PARTS) {
            val npc = spawnNpc(HueycoatlNpcs.BODY, coord, 250)
            bodyRefs += npc
            bodyStatic[npc] = coord
        }
        if (spawnSeer) {
            seerRef = spawnNpc(HueycoatlNpcs.SEER, HueycoatlCoords.SEER_BASE, 1)
            seerRef?.hideAllOps()
            seerRef?.ignoreCombatInteractions = true
            seerRef?.movementLocked = true
            seerRef?.moveRestrict = MoveRestrict.Normal
        }
    }

    private fun spawnVictoryWorkers() {
        spawnVictoryWorker(
            HueycoatlNpcs.VICTORY_WORKER_1,
            CoordGrid(1510, 3275),
            Direction.North,
            VICTORY_WORKER_ONE_LINES[deps.random.of(VICTORY_WORKER_ONE_LINES.size)],
            listOf(
                CoordGrid(1508, 3277),
                CoordGrid(1506, 3279),
                CoordGrid(1505, 3281),
                CoordGrid(1505, 3283),
            ),
            7,
            deferWalk = true,
        )
        spawnVictoryWorker(
            HueycoatlNpcs.VICTORY_WORKER_2,
            CoordGrid(1511, 3275),
            Direction.North,
            VICTORY_WORKER_TWO_LINES[deps.random.of(VICTORY_WORKER_TWO_LINES.size)],
            listOf(
                CoordGrid(1513, 3277),
                CoordGrid(1515, 3279),
                CoordGrid(1517, 3281),
                CoordGrid(1519, 3283),
            ),
            7,
            deferWalk = true,
        )
    }

    private fun spawnWarningWorkers() {
        spawnVictoryWorker(
            HueycoatlNpcs.VICTORY_WORKER_2,
            CoordGrid(1519, 3283),
            Direction.West,
            WARNING_WORKER_TWO_LINES[deps.random.of(WARNING_WORKER_TWO_LINES.size)],
            listOf(
                CoordGrid(1519, 3285),
                CoordGrid(1519, 3287),
                CoordGrid(1519, 3289),
            ),
            6,
            faceOnSpawn = true,
        )
        spawnVictoryWorker(
            HueycoatlNpcs.VICTORY_WORKER_1,
            CoordGrid(1505, 3283),
            Direction.East,
            WARNING_WORKER_ONE_LINES[deps.random.of(WARNING_WORKER_ONE_LINES.size)],
            listOf(
                CoordGrid(1507, 3285),
                CoordGrid(1509, 3287),
                CoordGrid(1511, 3289),
                CoordGrid(1513, 3289),
                CoordGrid(1515, 3289),
                CoordGrid(1517, 3289),
                CoordGrid(1519, 3289),
            ),
            10,
            faceOnSpawn = true,
        )
    }

    private fun spawnVictoryWorker(
        symbol: String,
        static: CoordGrid,
        facing: Direction,
        line: String,
        path: List<CoordGrid>,
        despawnDelay: Int,
        faceOnSpawn: Boolean = false,
        deferWalk: Boolean = false,
    ) {
        val type =
            ServerCacheManager.getNpc(symbol.asRSCM(RSCMType.NPC)) ?: error("Missing npc: $symbol")
        val npc = Npc(type, resolveStatic(static))
        npc.hitpoints = 1
        npc.mode = NpcMode.None
        npc.moveRestrict = MoveRestrict.Normal
        npc.movementLocked = false
        npc.ignoreCombatInteractions = true
        npc.setHunt(0)
        npc.respawns = false
        npc.respawnDir = facing
        deps.npcRepo.add(npc, Int.MAX_VALUE)
        npc.respawns = false
        npc.mode = NpcMode.None
        npc.ignoreCombatInteractions = true
        npc.hideAllOps()
        session?.let { instances.attachNpc(it.id, npc) }
        if (faceOnSpawn) npc.pendingFaceAngle = EntityFaceAngle(facing.angle)
        npc.say(line)
        npc.defaultMoveSpeed = MoveSpeed.Run
        if (deferWalk) {
            deps.worldQueues.add(1) {
                if (disposed || !npc.isSlotAssigned) return@add
                npc.walk(path.map(::resolveStatic))
                npc.forcedRoute = true
            }
        } else {
            npc.walk(path.map(::resolveStatic))
            npc.forcedRoute = true
        }
        cosmeticNpcs += npc
        deps.worldQueues.add(despawnDelay) {
            cosmeticNpcs.remove(npc)
            deleteNpc(npc)
        }
    }

    private fun spawnNpc(symbol: String, static: CoordGrid, hitpoints: Int): Npc {
        val type = ServerCacheManager.getNpc(symbol.asRSCM(RSCMType.NPC)) ?: error("Missing npc: $symbol")
        val npc = Npc(type, resolveStatic(static))
        npc.hitpoints = hitpoints
        npc.baseHitpointsLvl = hitpoints
        npc.mode = NpcMode.None
        npc.movementLocked = true
        npc.moveRestrict = MoveRestrict.NoMove
        npc.ignoreCombatInteractions = true
        npc.setHunt(0)
        npc.respawns = false
        deps.npcRepo.add(npc, Int.MAX_VALUE)
        npc.respawns = false
        npc.mode = NpcMode.None
        npc.movementLocked = true
        npc.ignoreCombatInteractions = true
        session?.let { instances.attachNpc(it.id, npc) }
        return npc
    }

    private fun deleteNpc(npc: Npc?) {
        if (npc == null) return
        val instance = session
        if (instance != null) {
            instances.removeNpc(instance.id, npc)
        } else if (npc.isSlotAssigned) {
            deps.npcRepo.del(npc, Int.MAX_VALUE)
        }
    }

    @OptIn(InternalApi::class)
    private fun transmog(npc: Npc, symbol: String) {
        val type = ServerCacheManager.getNpc(symbol.asRSCM(RSCMType.NPC)) ?: return
        npc.transmog(type, Int.MAX_VALUE)
        npc.assignUid()
    }

    private fun setBarrier(symbol: String, clear: Boolean = true) {
        if (clear) clearBarrierTile()
        deps.locRepo.add(
            resolveStatic(HueycoatlCoords.BARRIER),
            symbol,
            Int.MAX_VALUE,
            LocAngle[0],
            LocShape[10],
        )
    }

    private fun animateBarrier(seq: String, retract: Boolean = false) {
        val tile = resolveStatic(HueycoatlCoords.BARRIER)
        val barrierId = HueycoatlLocs.BARRIER.asRSCM(RSCMType.LOC)
        val barrier =
            deps.locRepo.findAll(tile).firstOrNull { it.id == barrierId }
                ?: deps.locRepo.add(
                    tile,
                    HueycoatlLocs.BARRIER,
                    Int.MAX_VALUE,
                    LocAngle[0],
                    LocShape[10],
                )
        deps.worldRepo.locAnim(barrier, seq)
        if (retract) {
            deps.worldQueues.add(9) {
                if (!disposed && phase != HueyPhase.Body) {
                    setBarrier(HueycoatlLocs.BARRIER_RETRACTED)
                }
            }
        }
    }

    private fun resetLocs(afterKill: Boolean = false) {
        setBarrier(
            if (afterKill) HueycoatlLocs.BARRIER_RESPAWN else HueycoatlLocs.BARRIER,
            clear = !afterKill,
        )
        stages = IntArray(3)
        braziersLit = false
        updatePillarLocs()
        for ((body, cave) in HueycoatlCoords.CAVE_BY_BODY) {
            val (coord, rotation) = cave
            val diagonal = body in HueycoatlCoords.CAVE_DIAGONAL
            val symbol =
                if (afterKill) {
                    if (diagonal) HueycoatlLocs.CAVE_DIAG_HUEY_RESPAWN
                    else HueycoatlLocs.CAVE_HUEY_RESPAWN
                } else {
                    if (diagonal) HueycoatlLocs.CAVE_DIAG_HUEY else HueycoatlLocs.CAVE_HUEY
                }
            deps.locRepo.add(resolveStatic(coord), symbol, Int.MAX_VALUE, LocAngle[rotation], LocShape[10])
        }
        if (afterKill) {
            deps.worldQueues.add(2) {
                if (disposed) return@add
                for ((body, cave) in HueycoatlCoords.CAVE_BY_BODY) {
                    val (coord, rotation) = cave
                    val symbol =
                        if (body in HueycoatlCoords.CAVE_DIAGONAL) HueycoatlLocs.CAVE_DIAG_HUEY
                        else HueycoatlLocs.CAVE_HUEY
                    deps.locRepo.add(
                        resolveStatic(coord),
                        symbol,
                        Int.MAX_VALUE,
                        LocAngle[rotation],
                        LocShape[10],
                    )
                }
            }
        }
    }

    private fun clearBarrierTile() {
        val tile = resolveStatic(HueycoatlCoords.BARRIER)
        for (loc in deps.locRepo.findAll(tile).toList()) {
            deps.locRepo.del(loc, Int.MAX_VALUE)
        }
    }

    private fun tailCap(player: Player): Int {
        val crush = wornBonuses.offensiveCrushBonus(player)
        val highest =
            crush > 0 &&
                crush >= wornBonuses.offensiveStabBonus(player) &&
                crush >= wornBonuses.offensiveSlashBonus(player) &&
                crush >= wornBonuses.offensiveMagicBonus(player) &&
                crush >= wornBonuses.offensiveRangedBonus(player)
        return HueycoatlArena.tailDamageCap(highest)
    }

    private fun markAvoidable(player: Player) {
        val id = player.uuid ?: return
        val participant = participants[id] ?: return
        participant.tookAvoidableDamage = true
        participant.perfect = false
    }

    fun qualifies(player: Player, participant: HueyParticipant): Boolean =
        HueycoatlArena.qualifies(
            teamSize = playersInParticipation().size,
            inDropRange = HueycoatlArena.inDropRange(origin, player.coords),
            bodyDamage = participant.bodyDamage,
            headDamage = participant.headDamage,
            tailDamage = participant.tailDamage,
            neutralDamage = participant.neutralDamage,
            directDamage = participant.directDamage,
        )

    fun mvp(): Player? =
        participantsInParticipation().maxByOrNull { it.second.totalDamage }?.first

    fun message(text: String) {
        for (player in playersInParticipation()) player.mes(text)
    }

    fun now(): Int = deps.mapClock.cycle

    private data class TailWaveTile(val coords: CoordGrid, val delayTicks: Int)

    private data class Quad(val a: String, val b: String, val c: String, val d: String)
}

private const val NPC_VIEW_DISTANCE = 32

private val VICTORY_WORKER_ONE_LINES =
    listOf("What's it left this time?", "Quickly, while it's recovering!", "Let's go!")

private val VICTORY_WORKER_TWO_LINES =
    listOf("For Karlo!", "Into the tunnels, quick!")

private val WARNING_WORKER_ONE_LINES =
    listOf("Prepare yourselves!", "It's coming back!", "Run!")

private val WARNING_WORKER_TWO_LINES =
    listOf("Prepare yourselves!", "It's coming back!", "Retreat!")
