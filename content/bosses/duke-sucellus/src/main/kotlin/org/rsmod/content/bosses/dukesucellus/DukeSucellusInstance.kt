package org.rsmod.content.bosses.dukesucellus

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.math.abs
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.api.combat.commons.player.finishNpcHit
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceEnterTransition
import org.rsmod.api.instances.InstanceId
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceNpc
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.instances.withInstanceEnterTransition
import org.rsmod.api.instances.withInstanceLeaveTransition
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.events.skilling.SkillingActionCompleteEvent
import org.rsmod.api.player.events.skilling.SkillingActionContext
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.miningLvl
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc5
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.headbar.Headbar
import org.rsmod.game.hit.HitType as EngineHitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

private enum class VatStage { FILLING, FERMENTING, READY }

private data class VatKey(val instanceId: InstanceId, val coords: CoordGrid)

private data class VatState(
    val loc: BoundLocInfo,
    var arderPowder: Int = 0,
    var muscaPowder: Int = 0,
    var salaxSalt: Int = 0,
    var stage: VatStage = VatStage.FILLING,
)

class DukeSucellusInstance
@Inject
constructor(
    registry: BossInstanceRegistry,
    private val deps: BossDeps,
    private val locRepo: LocRepository,
    private val aiPlayerInteractions: AiPlayerInteractions,
    private val npcDeath: NpcDeath,
) : InstanceScript(registry) {

    private val pendingAwakened = HashSet<Player>()
    private val feedProgress: MutableMap<Npc, Int> = IdentityHashMap()
    private val hazardsActive: MutableSet<Npc> = Collections.newSetFromMap(IdentityHashMap())
    private val vats: MutableMap<VatKey, VatState> = HashMap()

    override fun settingsRow(): String = "dbrow.instance_duke_sucellus"

    override fun area(): InstanceArea = INSTANCE

    override fun ScriptContext.configure() {
        onEnterPrelude { result, enter ->
            withInstanceEnterTransition(InstanceEnterTransition(message = ENTER_MESSAGE), enter)
            if (result is InstanceManager.Result.Created && pendingAwakened.remove(player)) {
                manager.npcsForInstance(result.session.id).firstOrNull()?.let(::markAwakened)
            }
        }

        onEnterObject { enterInstance() }
        onExitObject { defaultLeaveFlow() }
        onOpLoc5(ESCAPE_LOC) { quickEscape() }
        onOpNpc1(SLEEP_NPC) { feed(it.npc) }
        val awakeType = ServerCacheManager.getNpc(BOSS_NPC.asRSCM(RSCMType.NPC))!!
        onNpcQueue(awakeType, "queue.death") { dukeDeath() }

        onOpLoc1(ARDER_MUSHROOM_LOC) { pickMushroom(ARDER_MUSHROOM_ITEM) }
        onOpLoc1(MUSCA_MUSHROOM_LOC) { pickMushroom(MUSCA_MUSHROOM_ITEM) }
        onOpLoc1(PESTLE_MORTAR_LOC) { takeTool(PESTLE_MORTAR_ITEM, TAKE_PESTLE_MESSAGE) }
        onOpLoc1(PICKAXE_LOC) { takeTool(WALL_PICKAXE_ITEM, TAKE_PICKAXE_MESSAGE) }

        onOpHeldU(ARDER_MUSHROOM_ITEM, PESTLE_MORTAR_ITEM) { grind(ARDER_MUSHROOM_ITEM, ARDER_POWDER_ITEM, ARDER_GROUND_MESSAGE) }
        onOpHeldU(MUSCA_MUSHROOM_ITEM, PESTLE_MORTAR_ITEM) { grind(MUSCA_MUSHROOM_ITEM, MUSCA_POWDER_ITEM, MUSCA_GROUND_MESSAGE) }

        onOpLoc1(VAT_CHECK_LOC) { fillVat(it.loc) }
        onOpLoc3(VAT_CHECK_LOC) { checkVat(it.loc) }
        onOpLoc1(VAT_COLLECT_LOC) { emptyVat(it.loc) }
        onOpLocU(VAT_CHECK_LOC, ARDER_POWDER_ITEM) { fillVat(it.loc) }
        onOpLocU(VAT_CHECK_LOC, MUSCA_POWDER_ITEM) { fillVat(it.loc) }
        onOpLocU(VAT_CHECK_LOC, SALT_ITEM) { fillVat(it.loc) }

        onEvent<SkillingActionCompleteEvent> {
            val product = context as? SkillingActionContext.Product ?: return@onEvent
            if (product.isBonus || product.skill != MINING_SKILL || product.item != SALT_ITEM) {
                return@onEvent
            }
            if (manager.sessionForPlayer(player) == null) return@onEvent
            val extra = saltYield(player.miningLvl) - product.count
            if (extra > 0) player.invAdd(player.inv, SALT_ITEM, extra)
        }

        onInstanceEnded { vats.keys.removeIf { it.instanceId == instanceId } }

        onInstanceStarted {
            val session = manager.sessionForId(instanceId) ?: return@onInstanceStarted
            activateLocs(session)
        }

        onInstancePlayerJoin {
            val npc = manager.npcsForInstance(instanceId).firstOrNull() ?: return@onInstancePlayerJoin
            val session = manager.sessionForId(instanceId) ?: return@onInstancePlayerJoin
            startHazards(npc, session)
        }
        onInstancePlayerLeave {
            manager.npcsForInstance(instanceId).forEach(hazardsActive::remove)
        }
    }

    private suspend fun ProtectedAccess.quickEscape() {
        withInstanceLeaveTransition { defaultLeaveFlow() }
    }

    private suspend fun ProtectedAccess.enterInstance() {
        val owned = player.uuid?.let { manager.sessionOwnedBy(key, it) }
        if (owned == null && tryConsumeAwakenersOrb()) {
            pendingAwakened += player
        }
        defaultInstanceEntry()
    }

    private suspend fun ProtectedAccess.tryConsumeAwakenersOrb(): Boolean {
        if (AWAKENERS_ORB !in inv) {
            return false
        }
        val useOrb =
            choice2(
                "Yes - consume an Awakener's orb.",
                true,
                "No - fight the normal encounter.",
                false,
                title = "Use an Awakener's orb to fight an Awakened Duke Sucellus?",
            )
        if (!useOrb) {
            return false
        }
        invDel(inv, AWAKENERS_ORB, 1)
        return true
    }

    private fun markAwakened(npc: Npc) {
        npc.vars["varn.awakened_state"] = 1
        npc.vars["varn.skip_killcount"] = 1
    }

    private fun awakened(npc: Npc): Boolean = npc.vars["varn.awakened_state"] == 1

    private suspend fun ProtectedAccess.feed(npc: Npc) {
        if (POISON_ITEM !in inv) {
            mes(NO_POISON_MESSAGE)
            return
        }
        invDel(inv, POISON_ITEM, 1)

        val required = if (awakened(npc)) AWAKENED_FEED_COUNT else NORMAL_FEED_COUNT
        val progress = (feedProgress[npc] ?: 0) + 1
        if (progress < required) {
            feedProgress[npc] = progress
            mes(FEED_PROGRESS_MESSAGE)
            return
        }
        feedProgress.remove(npc)
        hazardsActive.remove(npc)
        mes(WAKE_MESSAGE)
        npc.anim(WAKE_SEQ)
        val target = player
        deps.worldQueues.add(WAKE_TRANSFORM_DELAY) { wake(npc, target) }
    }

    @OptIn(InternalApi::class)
    private fun wake(npc: Npc, target: Player) {
        if (!npc.isSlotAssigned) return
        val awake = ServerCacheManager.getNpc(BOSS_NPC.asRSCM(RSCMType.NPC)) ?: return
        val wasAwakened = awakened(npc)
        npc.resetAnim()
        npc.transmog(awake, Int.MAX_VALUE)
        npc.copyStats(awake)
        npc.assignUid()
        if (wasAwakened) {
            npc.vars["varn.awakened_state"] = 1
            npc.vars["varn.skip_killcount"] = 1
            npc.baseHitpointsLvl = AWAKENED_HITPOINTS
        } else {
            npc.baseHitpointsLvl = NORMAL_HITPOINTS
        }
        npc.hitpoints = npc.baseHitpointsLvl
        npc.apRequiresLineOfSight = false
        npc.apRangeOverride = ARENA_AP_RANGE
        deps.suppressAttacks(npc, WAKE_ATTACK_DELAY)
        if (target.isValidTarget()) npc.apPlayer2(target, aiPlayerInteractions)
        val session = sessionOf(npc) ?: return
        animateAllEyes(session, EYE_WAKE_SEQ)
        loopAwakeEyes(npc, session)
    }

    private fun loopAwakeEyes(npc: Npc, session: InstanceSession) {
        deps.worldQueues.add(1) {
            if (!npc.isSlotAssigned || !npc.isVisType(BOSS_NPC)) return@add
            animateAllEyes(session, EYE_AWAKE_SEQ)
            loopAwakeEyes(npc, session)
        }
    }

    private fun sessionOf(npc: Npc): InstanceSession? =
        manager.instanceForNpc(npc)?.let(manager::sessionForId)

    private fun animateAllEyes(session: InstanceSession, seq: String) {
        for (x in listOf(EYE_LEFT_X, EYE_RIGHT_X)) {
            for (step in EXTREMITY_SWEEP) {
                val coords = resolve(session, CoordGrid(x, step.y + 1))
                val eye = locRepo.findExact(coords, LocShape.CentrepieceStraight) ?: continue
                deps.worldRepo.locAnim(eye, seq)
            }
        }
    }

    private suspend fun StandardNpcAccess.dukeDeath() {
        val dead = ServerCacheManager.getNpc(DEAD_NPC.asRSCM(RSCMType.NPC)) ?: return
        val asleep = ServerCacheManager.getNpc(SLEEP_NPC.asRSCM(RSCMType.NPC)) ?: return
        noneMode()
        changeType(dead, Int.MAX_VALUE)
        hideAllOps()
        anim(DEATH_SEQ)
        sessionOf(npc)?.let { animateAllEyes(it, EYE_DEATH_SEQ) }
        delay(DEATH_DROP_DELAY)

        npcDeath.spawnDrops(this, coords.translate(DEATH_DROP_OFFSET_X, DEATH_DROP_OFFSET_Z))
        npc.vars["varn.awakened_state"] = 0
        npc.vars["varn.skip_killcount"] = 0
        delay(DEATH_RESET_DELAY)

        npc.resetTransmog()
        npc.copyStats(asleep)
        npc.hitpoints = npc.baseHitpointsLvl
        npc.apRequiresLineOfSight = true
        npc.apRangeOverride = null
        deps.encounterRegistry.remove(npc)
        resetMode()
        showAllOps()
        anim(DEATH_RESET_SEQ)
        val session = sessionOf(npc) ?: return
        startHazards(
            npc,
            session,
            ventDelay = RESTART_VENT_DELAY,
            iceDelay = RESTART_ICE_DELAY,
            sweepDelay = RESTART_SWEEP_DELAY,
        )
    }

    private fun ProtectedAccess.pickMushroom(item: String) {
        if (!inv.isFull()) invAdd(inv, item)
        mes(PICK_MUSHROOM_MESSAGE)
    }

    private fun ProtectedAccess.takeTool(item: String, message: String) {
        if (item !in inv && !inv.isFull()) {
            invAdd(inv, item)
        }
        mes(message)
    }

    private fun ProtectedAccess.grind(mushroom: String, powder: String, message: String) {
        val herblore = statBase("stat.herblore")
        val amount = ((herblore - POWDER_LEVEL_OFFSET) / POWDER_LEVEL_STEP).coerceIn(1, MAX_POWDER_YIELD)
        invDel(inv, mushroom, 1)
        if (!inv.isFull()) invAdd(inv, powder, amount)
        mes(message)
    }

    private fun saltYield(miningLevel: Int): Int =
        when {
            miningLevel >= SALT_YIELD_SIX_LEVEL -> 6
            miningLevel >= SALT_YIELD_FIVE_LEVEL -> 5
            miningLevel >= SALT_YIELD_FOUR_LEVEL -> 4
            else -> 3
        }

    private fun vatState(session: InstanceSession, loc: BoundLocInfo): VatState =
        vats.getOrPut(VatKey(session.id, loc.coords)) { VatState(loc) }

    private fun ProtectedAccess.fillVat(loc: BoundLocInfo) {
        val session = manager.sessionForPlayer(player) ?: return
        val state = vatState(session, loc)
        when (state.stage) {
            VatStage.FERMENTING -> mes(VAT_FERMENTING_CHECK_MESSAGE)
            VatStage.READY -> mes(VAT_READY_CHECK_MESSAGE)
            VatStage.FILLING -> {
                val arder = deposit(ARDER_POWDER_ITEM, VAT_ADD_ARDER_MESSAGE, state.arderPowder)
                val musca = deposit(MUSCA_POWDER_ITEM, VAT_ADD_MUSCA_MESSAGE, state.muscaPowder)
                val salt = deposit(SALT_ITEM, VAT_ADD_SALT_MESSAGE, state.salaxSalt)
                state.arderPowder += arder
                state.muscaPowder += musca
                state.salaxSalt += salt
                if (state.complete()) {
                    startFerment(session, state)
                } else if (arder + musca + salt == 0) {
                    mes(VAT_NOTHING_TO_ADD_MESSAGE)
                }
            }
        }
    }

    private fun VatState.complete(): Boolean =
        arderPowder >= INGREDIENTS_PER_BATCH &&
            muscaPowder >= INGREDIENTS_PER_BATCH &&
            salaxSalt >= INGREDIENTS_PER_BATCH

    private fun ProtectedAccess.deposit(item: String, message: String, current: Int): Int {
        val amount = minOf(INGREDIENTS_PER_BATCH - current, invTotal(inv, item))
        if (amount <= 0) return 0
        invDel(inv, item, amount)
        spam(message)
        return amount
    }

    private fun ProtectedAccess.startFerment(session: InstanceSession, state: VatState) {
        state.stage = VatStage.FERMENTING
        mes(VAT_FERMENTING_MESSAGE)
        anim(VAT_FILL_SEQ)
        ServerCacheManager.getNpc(VAT_NPC.asRSCM(RSCMType.NPC))?.let { type ->
            val timer = Npc(type, state.loc.coords)
            timer.mode = NpcMode.None
            deps.npcRepo.add(timer, FERMENT_TICKS)
            timer.showHeadbar(
                Headbar.fromNoSource(
                    self = VAT_HEADBAR,
                    public = VAT_HEADBAR,
                    startFill = 0,
                    endFill = VAT_HEADBAR_FILL,
                    startTime = 0,
                    endTime = FERMENT_TICKS * CYCLES_PER_TICK,
                )
            )
        }
        val target = player
        deps.worldQueues.add(FERMENT_TICKS + 1) { readyFerment(session, state, target) }
    }

    private fun readyFerment(session: InstanceSession, state: VatState, target: Player) {
        if (state.stage != VatStage.FERMENTING) return
        state.stage = VatStage.READY
        locRepo.change(state.loc, VAT_COLLECT_LOC, Int.MAX_VALUE)
        if (target.isValidTarget() && manager.sessionForPlayer(target)?.id == session.id) {
            target.mes(VAT_READY_MESSAGE)
        }
    }

    private suspend fun ProtectedAccess.checkVat(loc: BoundLocInfo) {
        val session = manager.sessionForPlayer(player) ?: return
        val state = vatState(session, loc)
        when (state.stage) {
            VatStage.FILLING ->
                mesbox(
                    "Mushroom one:  ${state.muscaPowder} / $INGREDIENTS_PER_BATCH " +
                        "Mushroom two:  ${state.arderPowder} / $INGREDIENTS_PER_BATCH " +
                        "Salax salt:  ${state.salaxSalt} / $INGREDIENTS_PER_BATCH",
                )
            VatStage.FERMENTING -> mesbox(VAT_FERMENTING_CHECK_MESSAGE)
            VatStage.READY -> mesbox(VAT_READY_CHECK_MESSAGE)
        }
    }

    private fun ProtectedAccess.emptyVat(loc: BoundLocInfo) {
        val session = manager.sessionForPlayer(player) ?: return
        val state = vatState(session, loc)
        if (state.stage != VatStage.READY) return
        if (inv.isFull()) {
            mes(INVENTORY_FULL_MESSAGE)
            return
        }
        state.stage = VatStage.FILLING
        state.arderPowder = 0
        state.muscaPowder = 0
        state.salaxSalt = 0
        anim(VAT_FILL_SEQ)
        mes(VAT_COLLECT_MESSAGE)
        invAdd(inv, POISON_ITEM, POISONS_PER_BATCH)
        statAdvance("stat.herblore", POISON_HERBLORE_XP)
        locRepo.change(state.loc, VAT_CHECK_LOC, Int.MAX_VALUE)
    }

    private fun activateLocs(session: InstanceSession) {
        for ((loc, x, z, rotation, shape) in ACTIVE_LOCS) {
            val coords = resolve(session, CoordGrid(x, z))
            locRepo.add(coords, loc, Int.MAX_VALUE, LocAngle[rotation], shape)
        }
    }

    private fun startHazards(
        npc: Npc,
        session: InstanceSession,
        ventDelay: Int = GAS_VENT_INTERVAL,
        iceDelay: Int = ICE_INTERVAL,
        sweepDelay: Int = EXTREMITY_INTERVAL,
    ) {
        if (!hazardsActive.add(npc)) return
        scheduleGasVents(npc, session, 0, ventDelay)
        scheduleFallingIce(npc, session, iceDelay)
        scheduleExtremityGaze(npc, session, sweepDelay)
    }

    private fun resolve(session: InstanceSession, coords: CoordGrid): CoordGrid =
        manager.resolveCoord(session, coords) ?: coords

    private fun instancePlayerAt(session: InstanceSession, tile: CoordGrid): Player? =
        deps.playerList.firstOrNull {
            it.coords == tile && manager.sessionForPlayer(it)?.id == session.id
        }

    private fun mapSpot(spot: String, coords: CoordGrid) {
        deps.worldRepo.spotanimMap(SpotanimType(spot.asRSCM(RSCMType.SPOTANIM)), coords)
    }

    private fun scheduleGasVents(
        npc: Npc,
        session: InstanceSession,
        groupIndex: Int,
        delay: Int = GAS_VENT_INTERVAL,
    ) {
        deps.worldQueues.add(delay) {
            if (npc !in hazardsActive) return@add
            val group = VENT_GROUPS[groupIndex % VENT_GROUPS.size].map { VENT_TILES[it] }
            group.forEach { rampVent(npc, session, resolve(session, it)) }
            scheduleGasVents(npc, session, groupIndex + 1)
        }
    }

    private fun rampVent(npc: Npc, session: InstanceSession, tile: CoordGrid) {
        val ventType = ServerCacheManager.getNpc(GAS_VENT_NPC.asRSCM(RSCMType.NPC)) ?: return
        val vent = Npc(ventType, tile.translate(GAS_VENT_NPC_OFFSET, GAS_VENT_NPC_OFFSET))
        vent.mode = NpcMode.None
        deps.npcRepo.add(vent, GAS_VENT_LIFESPAN)
        vent.spotanim(GAS_VENT_SPAWN_SPOTANIM, GAS_VENT_SPAWN_DELAY)

        for (tick in GAS_VENT_IDLE_DELAY until GAS_VENT_DESPAWN_DELAY step GAS_VENT_IDLE_INTERVAL) {
            deps.worldQueues.add(tick) {
                if (vent.isSlotAssigned) vent.spotanim(GAS_VENT_IDLE_SPOTANIM)
            }
        }
        deps.worldQueues.add(GAS_VENT_DESPAWN_DELAY) {
            if (vent.isSlotAssigned) vent.spotanim(GAS_VENT_DESPAWN_SPOTANIM)
        }

        for (tick in GAS_VENT_IDLE_DELAY..GAS_VENT_DESPAWN_DELAY) {
            deps.worldQueues.add(tick) {
                if (npc !in hazardsActive) return@add
                for (player in instancePlayersNear(session, tile, GAS_VENT_RADIUS)) {
                    player.statSub("stat.prayer", GAS_VENT_PRAYER_DRAIN, 0)
                    val damage = GAS_VENT_DAMAGE.first + deps.random.of(GAS_VENT_DAMAGE.last - GAS_VENT_DAMAGE.first + 1)
                    player.finishNpcHit(npc, GAS_VENT_HIT_DELAY, EngineHitType.Typeless, damage, deps.playerHitModifier)
                }
            }
        }
    }

    private fun instancePlayersNear(session: InstanceSession, tile: CoordGrid, radius: Int): List<Player> =
        deps.playerList.filter {
            it.coords.level == tile.level &&
                tile.chebyshevDistance(it.coords) <= radius &&
                manager.sessionForPlayer(it)?.id == session.id
        }

    private fun scheduleFallingIce(npc: Npc, session: InstanceSession, delay: Int = ICE_INTERVAL) {
        deps.worldQueues.add(delay) {
            if (npc !in hazardsActive) return@add
            telegraphIce(npc, session, resolve(session, ICE_TILES.random()))
            scheduleFallingIce(npc, session)
        }
    }

    private fun telegraphIce(npc: Npc, session: InstanceSession, tile: CoordGrid) {
        mapSpot(ICE_TELEGRAPH_SPOTANIM, tile)
        deps.worldQueues.add(ICE_WINDUP) {
            mapSpot(ICE_LAND_SPOTANIM, tile)
            val player = instancePlayerAt(session, tile) ?: return@add
            player.mes(ICE_HIT_MESSAGE)
            player.spotanim(STUN_SPOTANIM, height = STUN_SPOTANIM_HEIGHT, slot = STUN_SPOTANIM_SLOT)
            player.soundSynth(ICE_HIT_SYNTH)
            player.frozen = true
            player.routeDestination.clear()
            player.timer("timer.combat_freeze", ICE_FREEZE_TICKS)
            val damage = ICE_DAMAGE.first + deps.random.of(ICE_DAMAGE.last - ICE_DAMAGE.first + 1)
            player.finishNpcHit(npc, ICE_HIT_DELAY, EngineHitType.Typeless, damage, deps.playerHitModifier)
        }
    }

    private fun scheduleExtremityGaze(npc: Npc, session: InstanceSession, delay: Int = EXTREMITY_INTERVAL) {
        deps.worldQueues.add(delay) {
            if (npc !in hazardsActive) return@add
            EXTREMITY_SWEEP.forEach { step ->
                deps.worldQueues.add(step.delay.coerceAtLeast(1)) {
                    if (npc in hazardsActive) fireExtremityPair(npc, session, step)
                }
            }
            scheduleExtremityGaze(npc, session)
        }
    }

    private fun fireExtremityPair(npc: Npc, session: InstanceSession, step: ExtremityStep) {
        fireExtremityCone(npc, session, CoordGrid(EXTREMITY_LEFT_X, step.y, 0), step.leftCone, 1)
        fireExtremityCone(npc, session, CoordGrid(EXTREMITY_RIGHT_X, step.y, 0), step.rightCone, -1)
    }

    private fun animateEye(session: InstanceSession, base: CoordGrid, coneIndex: Int, direction: Int) {
        val eyeX = if (direction > 0) EYE_LEFT_X else EYE_RIGHT_X
        val eye = locRepo.findExact(resolve(session, CoordGrid(eyeX, base.z + 1)), LocShape.CentrepieceStraight)
            ?: return
        deps.worldRepo.locAnim(eye, "$EYE_ROUSE_SEQ$coneIndex")
        deps.worldQueues.add(EXTREMITY_CONE_LIFESPAN + 1) { deps.worldRepo.locAnim(eye, EYE_IDLE_SEQ) }
    }

    private fun fireExtremityCone(
        npc: Npc,
        session: InstanceSession,
        base: CoordGrid,
        coneIndex: Int,
        direction: Int,
    ) {
        val tile = resolve(session, base)
        animateEye(session, base, coneIndex, direction)
        val coneType =
            ServerCacheManager.getNpc("$EXTREMITY_CONE_NPC$coneIndex".asRSCM(RSCMType.NPC)) ?: return
        val cone = Npc(coneType, tile)
        cone.mode = NpcMode.None
        cone.respawnDir = if (direction > 0) Direction.East else Direction.West
        // Cones spawned from a world queue are deleted one tick sooner than their duration.
        deps.npcRepo.add(cone, EXTREMITY_CONE_LIFESPAN + 1)
        val faceX = if (direction > 0) EXTREMITY_LEFT_FACE_DX else EXTREMITY_RIGHT_FACE_DX
        cone.faceSquare(tile.translate(faceX, EXTREMITY_FACE_DZ))
        deps.worldQueues.add(EXTREMITY_RESOLVE_DELAY) {
            val player =
                deps.playerList.firstOrNull {
                    manager.sessionForPlayer(it)?.id == session.id &&
                        !it.frozen &&
                        abs(it.coords.x - (tile.x + EXTREMITY_CONE_CENTER)) <= 1 &&
                        abs(it.coords.z - (tile.z + EXTREMITY_CONE_CENTER)) <= 1
                } ?: return@add
            player.mes(EXTREMITY_HIT_MESSAGE)
            player.spotanim(STUN_SPOTANIM, height = STUN_SPOTANIM_HEIGHT, slot = STUN_SPOTANIM_SLOT)
            player.spotanim(EXTREMITY_FREEZE_SPOTANIM, slot = EXTREMITY_FREEZE_SPOTANIM_SLOT)
            player.soundSynth(EXTREMITY_FREEZE_SYNTH)
            player.frozen = true
            player.routeDestination.clear()
            player.timer("timer.combat_freeze", EXTREMITY_FREEZE_TICKS)
            val damage = EXTREMITY_DAMAGE.first + deps.random.of(EXTREMITY_DAMAGE.last - EXTREMITY_DAMAGE.first + 1)
            player.finishNpcHit(npc, EXTREMITY_HIT_DELAY, EngineHitType.Typeless, damage, deps.playerHitModifier)
        }
    }

    private data class ActiveLoc(
        val loc: String,
        val x: Int,
        val z: Int,
        val rotation: Int,
        val shape: LocShape = LocShape.CentrepieceStraight,
    )

    private companion object {
        private val ACTIVE_LOCS =
            listOf(
                ActiveLoc("loc.duke_sucellus_mushrooms_3", 3030, 6453, 0),
                ActiveLoc("loc.duke_sucellus_mushrooms_1", 3048, 6453, 0),
                ActiveLoc("loc.duke_sucellus_pestle_mortar", 3034, 6434, 1),
                ActiveLoc("loc.duke_sucellus_pickaxe", 3044, 6434, 1),
                ActiveLoc("loc.duke_sucellus_vat_check", 3034, 6438, 3),
                ActiveLoc("loc.duke_sucellus_vat_check", 3043, 6438, 1),
                ActiveLoc("loc.duke_sucellus_salt", 3033, 6448, 2),
                ActiveLoc("loc.duke_sucellus_salt", 3043, 6448, 0),
                ActiveLoc("loc.duke_sucellus_escape", 3039, 6433, 3, LocShape.WallStraight),
                ActiveLoc("loc.dt2_duke_prisondoor_eye01", 3028, 6445, 3),
                ActiveLoc("loc.dt2_duke_prisondoor_eye02", 3028, 6441, 3),
                ActiveLoc("loc.dt2_duke_prisondoor_eye03", 3028, 6449, 3),
                ActiveLoc("loc.dt2_duke_prisondoor_eye03", 3049, 6445, 1),
                ActiveLoc("loc.dt2_duke_prisondoor_eye01", 3049, 6441, 1),
                ActiveLoc("loc.dt2_duke_prisondoor_eye02", 3049, 6449, 1),
            )

        private const val SLEEP_NPC = "npc.duke_sucellus_asleep"
        private const val BOSS_NPC = "npc.duke_sucellus_awake"
        private const val DEAD_NPC = "npc.duke_sucellus_dead"
        private const val DEATH_SEQ = "seq.npc_duke_sucellus01_death_01"
        private const val DEATH_RESET_SEQ = "seq.npc_duke_sucellus01_death_reset_01"
        private const val DEATH_DROP_DELAY = 4
        private const val DEATH_RESET_DELAY = 19
        private const val DEATH_DROP_OFFSET_X = 3
        private const val DEATH_DROP_OFFSET_Z = -1
        private const val ESCAPE_LOC = "loc.duke_sucellus_escape"
        private const val AWAKENERS_ORB = "obj.dt2_awakeners_orb"
        private const val ENTER_MESSAGE = "You enter the Duke's chamber."
        private const val ARENA_REGION = 12132

        private const val NORMAL_FEED_COUNT = 2
        private const val AWAKENED_FEED_COUNT = 3
        private const val FEED_PROGRESS_MESSAGE = "You feed the poison to Duke Sucellus."
        private const val WAKE_MESSAGE = "<col=ff289d>Duke Sucellus awakens..."
        private const val WAKE_SEQ = "seq.npc_duke_sucellus01_wake_up_01"
        private const val WAKE_TRANSFORM_DELAY = 3
        private const val WAKE_ATTACK_DELAY = 4
        private const val ARENA_AP_RANGE = 20

        const val NORMAL_HITPOINTS = 485
        const val AWAKENED_HITPOINTS = 1697

        private const val ARDER_MUSHROOM_LOC = "loc.duke_sucellus_mushrooms_3"
        private const val MUSCA_MUSHROOM_LOC = "loc.duke_sucellus_mushrooms_1"
        private const val PESTLE_MORTAR_LOC = "loc.duke_sucellus_pestle_mortar"
        private const val PICKAXE_LOC = "loc.duke_sucellus_pickaxe"
        private const val VAT_CHECK_LOC = "loc.duke_sucellus_vat_check"
        private const val VAT_COLLECT_LOC = "loc.duke_sucellus_vat_collect_1_3"
        private const val VAT_NPC = "npc.duke_sucellus_vat_ferment"
        private const val VAT_FILL_SEQ = "seq.qip_cook_hopper_grain"
        private const val VAT_HEADBAR = 33
        private const val VAT_HEADBAR_FILL = 50
        private const val CYCLES_PER_TICK = 30

        private const val ARDER_MUSHROOM_ITEM = "obj.duke_sucellus_mushroom_3"
        private const val MUSCA_MUSHROOM_ITEM = "obj.duke_sucellus_mushroom_1"
        private const val ARDER_POWDER_ITEM = "obj.duke_sucellus_mushroom_3_ground"
        private const val MUSCA_POWDER_ITEM = "obj.duke_sucellus_mushroom_1_ground"
        private const val MINING_SKILL = "stat.mining"
        private const val SALT_YIELD_FOUR_LEVEL = 70
        private const val SALT_YIELD_FIVE_LEVEL = 75
        private const val SALT_YIELD_SIX_LEVEL = 80
        private const val SALT_ITEM = "obj.duke_sucellus_salt"
        private const val POISON_ITEM = "obj.duke_sucellus_potion_1_3"
        private const val PESTLE_MORTAR_ITEM = "obj.pestle_and_mortar"
        private const val WALL_PICKAXE_ITEM = "obj.iron_pickaxe"

        private const val POWDER_LEVEL_OFFSET = 20
        private const val POWDER_LEVEL_STEP = 10
        private const val MAX_POWDER_YIELD = 6
        private const val INGREDIENTS_PER_BATCH = 6
        private const val POISONS_PER_BATCH = 1
        private const val POISON_HERBLORE_XP = 10.0
        private const val FERMENT_TICKS = 6

        private const val TAKE_PESTLE_MESSAGE = "You take the pestle and mortar."
        private const val TAKE_PICKAXE_MESSAGE = "You take the pickaxe."
        private const val PICK_MUSHROOM_MESSAGE = "You pick a mushroom."
        private const val ARDER_GROUND_MESSAGE = "You grind the mushroom into dust."
        private const val MUSCA_GROUND_MESSAGE = "You grind the mushroom into dust."
        private const val INVENTORY_FULL_MESSAGE = "Your inventory is too full to hold any more."
        private const val VAT_NOTHING_TO_ADD_MESSAGE = "You have nothing to add to the vat."
        private const val VAT_ADD_ARDER_MESSAGE = "You add some arder powder to the vat."
        private const val VAT_ADD_MUSCA_MESSAGE = "You add some musca powder to the vat."
        private const val VAT_ADD_SALT_MESSAGE = "You add some salax salt to the vat."
        private const val VAT_FERMENTING_MESSAGE = "The mixture in the vat begins to ferment."
        private const val VAT_FERMENTING_CHECK_MESSAGE = "The vat is currently fermenting."
        private const val VAT_READY_MESSAGE = "<col=229628>A fermentation vat is ready to be emptied.</col>"
        private const val VAT_READY_CHECK_MESSAGE = "The vat is ready to be emptied."
        private const val VAT_COLLECT_MESSAGE = "You collect some poison from the vat."
        private const val NO_POISON_MESSAGE = "You don't have any Arder-musca poison to feed to Duke Sucellus."

        private const val GAS_VENT_INTERVAL = 25
        private const val GAS_VENT_NPC = "npc.duke_vent_spotanim_npc"
        private const val GAS_VENT_SPAWN_SPOTANIM = "spotanim.spotanim_duke_vent_01_spawn_01"
        private const val GAS_VENT_IDLE_SPOTANIM = "spotanim.spotanim_duke_vent_01_idle_01"
        private const val GAS_VENT_DESPAWN_SPOTANIM = "spotanim.spotanim_duke_vent_01_despawn_01"
        private const val GAS_VENT_SPAWN_DELAY = 120
        private const val GAS_VENT_NPC_OFFSET = -1
        private const val GAS_VENT_IDLE_DELAY = 6
        private const val GAS_VENT_IDLE_INTERVAL = 2
        private const val GAS_VENT_DESPAWN_DELAY = 11
        private const val GAS_VENT_LIFESPAN = 15
        private const val GAS_VENT_RADIUS = 1
        private const val GAS_VENT_PRAYER_DRAIN = 4
        private val GAS_VENT_DAMAGE = 5..11
        private const val GAS_VENT_HIT_DELAY = 1
        private val VENT_TILES: List<CoordGrid> =
            buildList {
                for (col in 0..2) {
                    for (row in 0..2) {
                        add(CoordGrid(3036 + col * 3, 6442 + row * 4, 0))
                    }
                }
            }
        private val VENT_GROUPS = listOf(listOf(2, 4, 6), listOf(0, 5, 7), listOf(1, 3, 8))

        private const val ICE_INTERVAL = 4
        private const val ICE_WINDUP = 4
        private const val ICE_TELEGRAPH_SPOTANIM = "spotanim.gargboss_debris_shadow_120"
        private const val ICE_LAND_SPOTANIM = "spotanim.enakh_spell_blood_barrage_impact"
        private const val ICE_HIT_MESSAGE = "<col=ff3045>You've been frozen in place!"
        private const val ICE_FREEZE_TICKS = 2
        private val ICE_DAMAGE = 3..18
        private const val ICE_HIT_DELAY = 1
        private const val LEFT_CORRIDOR_X = 3029
        private const val RIGHT_CORRIDOR_X = 3047
        private val ICE_TILES: List<CoordGrid> =
            (6438..6458 step 2).flatMap { y ->
                listOf(CoordGrid(LEFT_CORRIDOR_X, y, 0), CoordGrid(RIGHT_CORRIDOR_X, y, 0))
            }

        // Recorded sweep (rsprox 1751): a cone pair spawns on each wall every 2 ticks, 4 tick lifespan.
        private const val EXTREMITY_INTERVAL = 15
        private const val EXTREMITY_CONE_LIFESPAN = 4
        private const val EXTREMITY_RESOLVE_DELAY = 2
        private const val EXTREMITY_CONE_CENTER = 2
        private const val EXTREMITY_LEFT_FACE_DX = 12
        private const val EXTREMITY_RIGHT_FACE_DX = -8
        private const val EXTREMITY_FACE_DZ = 2
        private const val EYE_LEFT_X = 3028
        private const val EYE_RIGHT_X = 3049
        private const val EYE_ROUSE_SEQ = "seq.duke_door_eye_01_rouse_0"
        private const val EYE_IDLE_SEQ = "seq.duke_door_eye_01_asleep_idle_01"
        private const val EYE_WAKE_SEQ = "seq.duke_door_eye_01_wake_up_01"
        private const val EYE_AWAKE_SEQ = "seq.duke_door_eye_01_awake_idle_01"
        private const val EYE_DEATH_SEQ = "seq.duke_door_eye_01_death_01"
        private const val RESTART_SWEEP_DELAY = 1
        private const val RESTART_ICE_DELAY = 4
        private const val RESTART_VENT_DELAY = 15
        private const val EXTREMITY_LEFT_X = 3028
        private const val EXTREMITY_RIGHT_X = 3046
        private const val EXTREMITY_CONE_NPC = "npc.duke_extremity_cone_"
        private const val EXTREMITY_HIT_MESSAGE = "<col=ff3045>You've been frozen in place!"
        private const val STUN_SPOTANIM = "spotanim.tob_bloat_stunned"
        private const val STUN_SPOTANIM_HEIGHT = 124
        private const val STUN_SPOTANIM_SLOT = 4
        private const val EXTREMITY_FREEZE_SPOTANIM = "spotanim.ice_barrage_impact"
        private const val EXTREMITY_FREEZE_SPOTANIM_SLOT = 2
        private const val EXTREMITY_FREEZE_SYNTH = "synth.ice_cast"
        private const val ICE_HIT_SYNTH = "synth.thieving_stunned"
        private const val EXTREMITY_FREEZE_TICKS = 9
        private val EXTREMITY_DAMAGE = 30..70
        private const val EXTREMITY_HIT_DELAY = 1
        private val EXTREMITY_SWEEP =
            listOf(
                ExtremityStep(delay = 0, y = 6444, leftCone = 1, rightCone = 3),
                ExtremityStep(delay = 2, y = 6440, leftCone = 2, rightCone = 1),
                ExtremityStep(delay = 4, y = 6448, leftCone = 3, rightCone = 2),
            )

        private val INSTANCE =
            InstanceArea.copyRegions(
                regionIds = listOf(ARENA_REGION),
                npcSpawns = listOf(InstanceNpc(SLEEP_NPC, CoordGrid(3036, 6452, 0))),
            )
    }
}

private data class ExtremityStep(
    val delay: Int,
    val y: Int,
    val leftCone: Int,
    val rightCone: Int,
)
