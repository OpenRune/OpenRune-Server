package org.rsmod.content.bosses.dukesucellus

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import java.util.Collections
import java.util.IdentityHashMap
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.api.combat.commons.player.finishNpcHit
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceEnterTransition
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceNpc
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.instances.withInstanceEnterTransition
import org.rsmod.api.instances.withInstanceLeaveTransition
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc5
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType as EngineHitType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

private data class VatState(var arderPowder: Int = 0, var muscaPowder: Int = 0, var salaxSalt: Int = 0)

class DukeSucellusInstance
@Inject
constructor(
    registry: BossInstanceRegistry,
    private val deps: BossDeps,
    private val aiPlayerInteractions: AiPlayerInteractions,
) : InstanceScript(registry) {

    private val pendingAwakened = HashSet<Player>()
    private val feedProgress: MutableMap<Npc, Int> = IdentityHashMap()
    private val hazardsActive: MutableSet<Npc> = Collections.newSetFromMap(IdentityHashMap())
    private val vats: MutableMap<Player, VatState> = IdentityHashMap()

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

        onOpLoc1(ARDER_MUSHROOM_LOC) { pickMushroom(ARDER_MUSHROOM_ITEM) }
        onOpLoc1(MUSCA_MUSHROOM_LOC) { pickMushroom(MUSCA_MUSHROOM_ITEM) }
        onOpLoc1(SALT_LOC) { mineSalt() }
        onOpLoc1(PESTLE_MORTAR_LOC) { takeTool(PESTLE_MORTAR_ITEM, TAKE_PESTLE_MESSAGE) }
        onOpLoc1(PICKAXE_LOC) { takeTool(WALL_PICKAXE_ITEM, TAKE_PICKAXE_MESSAGE) }

        onOpHeldU(ARDER_MUSHROOM_ITEM, PESTLE_MORTAR_ITEM) { grind(ARDER_MUSHROOM_ITEM, ARDER_POWDER_ITEM, ARDER_GROUND_MESSAGE) }
        onOpHeldU(MUSCA_MUSHROOM_ITEM, PESTLE_MORTAR_ITEM) { grind(MUSCA_MUSHROOM_ITEM, MUSCA_POWDER_ITEM, MUSCA_GROUND_MESSAGE) }

        onOpLocU(VAT_FERMENT_LOC, ARDER_POWDER_ITEM) { addToVat(ARDER_POWDER_ITEM) { it.arderPowder++ } }
        onOpLocU(VAT_FERMENT_LOC, MUSCA_POWDER_ITEM) { addToVat(MUSCA_POWDER_ITEM) { it.muscaPowder++ } }
        onOpLocU(VAT_FERMENT_LOC, SALT_ITEM) { addToVat(SALT_ITEM) { it.salaxSalt++ } }
        onOpLoc1(VAT_CHECK_LOC) { checkVat() }

        onInstancePlayerJoin {
            val npc = manager.npcsForInstance(instanceId).firstOrNull() ?: return@onInstancePlayerJoin
            val session = manager.sessionForId(instanceId) ?: return@onInstancePlayerJoin
            startHazards(npc, session)
        }
        onInstancePlayerLeave {
            manager.npcsForInstance(instanceId).forEach(hazardsActive::remove)
            vats.remove(player)
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
        deps.suppressAttacks(npc, WAKE_ATTACK_DELAY)
        if (target.isValidTarget()) npc.apPlayer2(target, aiPlayerInteractions)
    }

    private fun ProtectedAccess.pickMushroom(item: String) {
        if (!inv.isFull()) invAdd(inv, item)
        mes(PICK_MUSHROOM_MESSAGE)
    }

    private fun ProtectedAccess.mineSalt() {
        if (PICKAXE_ITEMS.none { it in inv }) {
            mes(NEED_PICKAXE_MESSAGE)
            return
        }
        if (!inv.isFull()) invAdd(inv, SALT_ITEM, SALT_YIELD)
        mes(SALT_MINED_MESSAGE)
    }

    private fun ProtectedAccess.takeTool(item: String, message: String) {
        if (item !in inv && !inv.isFull()) {
            invAdd(inv, item)
        }
        mes(message)
    }

    private fun ProtectedAccess.grind(mushroom: String, powder: String, message: String) {
        val herblore = statBase("stat.herblore")
        val amount =
            when {
                herblore >= HIGH_HERBLORE_LEVEL -> HIGH_POWDER_YIELD
                herblore >= MID_HERBLORE_LEVEL -> MID_POWDER_YIELD
                else -> LOW_POWDER_YIELD
            }
        invDel(inv, mushroom, 1)
        if (!inv.isFull()) invAdd(inv, powder, amount)
        mes(message)
    }

    private fun ProtectedAccess.addToVat(item: String, increment: (VatState) -> Unit) {
        invDel(inv, item, 1)
        val state = vats.getOrPut(player) { VatState() }
        increment(state)
        mes(VAT_ADD_MESSAGE)
        if (state.arderPowder >= INGREDIENTS_PER_BATCH &&
            state.muscaPowder >= INGREDIENTS_PER_BATCH &&
            state.salaxSalt >= INGREDIENTS_PER_BATCH
        ) {
            state.arderPowder -= INGREDIENTS_PER_BATCH
            state.muscaPowder -= INGREDIENTS_PER_BATCH
            state.salaxSalt -= INGREDIENTS_PER_BATCH
            mes(VAT_FERMENTING_MESSAGE)
            val target = player
            deps.worldQueues.add(FERMENT_TICKS) { finishFerment(target) }
        }
    }

    private fun finishFerment(target: Player) {
        if (!target.isValidTarget()) return
        if (!target.inv.isFull()) target.invAdd(target.inv, POISON_ITEM, POISONS_PER_BATCH)
        target.mes(VAT_READY_MESSAGE)
    }

    private fun ProtectedAccess.checkVat() {
        val state = vats[player] ?: VatState()
        mes(
            "Vat: ${state.arderPowder}/$INGREDIENTS_PER_BATCH arder powder, " +
                "${state.muscaPowder}/$INGREDIENTS_PER_BATCH musca powder, " +
                "${state.salaxSalt}/$INGREDIENTS_PER_BATCH salax salt.",
        )
    }

    private fun startHazards(npc: Npc, session: InstanceSession) {
        if (!hazardsActive.add(npc)) return
        scheduleGasVents(npc, session, 0)
        scheduleFallingIce(npc, session)
        scheduleExtremityGaze(npc, session)
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

    private fun scheduleGasVents(npc: Npc, session: InstanceSession, groupIndex: Int) {
        deps.worldQueues.add(GAS_VENT_INTERVAL) {
            if (npc !in hazardsActive) return@add
            val group = VENT_GROUPS[groupIndex % VENT_GROUPS.size].map { VENT_TILES[it] }
            group.forEach { rampVent(npc, session, resolve(session, it)) }
            scheduleGasVents(npc, session, groupIndex + 1)
        }
    }

    private fun rampVent(npc: Npc, session: InstanceSession, tile: CoordGrid) {
        GAS_RAMP_SPOTANIMS.forEachIndexed { index, spot ->
            deps.worldQueues.add(index + 1) { mapSpot(spot, tile) }
        }
        deps.worldQueues.add(GAS_RAMP_SPOTANIMS.size) {
            val player = instancePlayerAt(session, tile) ?: return@add
            player.statSub("stat.prayer", GAS_VENT_PRAYER_DRAIN, 0)
            val damage = GAS_VENT_DAMAGE.first + deps.random.of(GAS_VENT_DAMAGE.last - GAS_VENT_DAMAGE.first + 1)
            player.finishNpcHit(npc, GAS_VENT_HIT_DELAY, EngineHitType.Typeless, damage, deps.playerHitModifier)
        }
    }

    private fun scheduleFallingIce(npc: Npc, session: InstanceSession) {
        deps.worldQueues.add(ICE_INTERVAL) {
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
            player.frozen = true
            player.routeDestination.clear()
            player.timer("timer.combat_freeze", ICE_FREEZE_TICKS)
            val damage = ICE_DAMAGE.first + deps.random.of(ICE_DAMAGE.last - ICE_DAMAGE.first + 1)
            player.finishNpcHit(npc, ICE_HIT_DELAY, EngineHitType.Typeless, damage, deps.playerHitModifier)
        }
    }

    private fun scheduleExtremityGaze(npc: Npc, session: InstanceSession) {
        deps.worldQueues.add(EXTREMITY_INTERVAL) {
            if (npc !in hazardsActive) return@add
            fireExtremityCone(npc, session, resolve(session, EXTREMITY_TILES.random()))
            scheduleExtremityGaze(npc, session)
        }
    }

    private fun fireExtremityCone(npc: Npc, session: InstanceSession, tile: CoordGrid) {
        mapSpot(EXTREMITY_CONE_SPOTANIM, tile)
        deps.worldQueues.add(EXTREMITY_RESOLVE_DELAY) {
            val player = instancePlayerAt(session, tile) ?: return@add
            player.mes(EXTREMITY_HIT_MESSAGE)
            player.frozen = true
            player.routeDestination.clear()
            player.timer("timer.combat_freeze", EXTREMITY_FREEZE_TICKS)
            player.statSub("stat.prayer", EXTREMITY_PRAYER_DRAIN, 0)
            val damage = EXTREMITY_DAMAGE.first + deps.random.of(EXTREMITY_DAMAGE.last - EXTREMITY_DAMAGE.first + 1)
            player.finishNpcHit(npc, EXTREMITY_HIT_DELAY, EngineHitType.Typeless, damage, deps.playerHitModifier)
        }
    }

    private companion object {
        private const val SLEEP_NPC = "npc.duke_sucellus_asleep"
        private const val BOSS_NPC = "npc.duke_sucellus_awake"
        private const val ESCAPE_LOC = "loc.duke_sucellus_escape"
        private const val AWAKENERS_ORB = "obj.dt2_awakeners_orb"
        private const val ENTER_MESSAGE = "You enter the Duke's chamber."
        private const val ARENA_REGION = 12132

        private const val NORMAL_FEED_COUNT = 2
        private const val AWAKENED_FEED_COUNT = 3
        private const val FEED_PROGRESS_MESSAGE = "You feed the poison to Duke Sucellus."
        private const val WAKE_MESSAGE = "<col=ff289d>Duke Sucellus awakens...</col>"
        private const val WAKE_SEQ = "seq.npc_duke_sucellus01_wake_up_01"
        private const val WAKE_TRANSFORM_DELAY = 3
        private const val WAKE_ATTACK_DELAY = 4

        const val NORMAL_HITPOINTS = 485
        const val AWAKENED_HITPOINTS = 1697

        // Ardermusca poison brewing — real items confirmed via a live rsprox packet capture
        // (rsprox-1751-rev227): mushrooms_1/mushroom_1(_ground) = musca, mushrooms_3/mushroom_3
        // (_ground) = arder; mushrooms_2/4 (holos/resper) are the decoy patches and are ignored.
        // potion_1_3 is the real "Arder-musca poison" (obj 28351 in the capture); the other five
        // 2-of-4 combinations are non-functional flavour items.
        private const val ARDER_MUSHROOM_LOC = "loc.duke_sucellus_mushrooms_3"
        private const val MUSCA_MUSHROOM_LOC = "loc.duke_sucellus_mushrooms_1"
        private const val SALT_LOC = "loc.duke_sucellus_salt"
        private const val PESTLE_MORTAR_LOC = "loc.duke_sucellus_pestle_mortar"
        private const val PICKAXE_LOC = "loc.duke_sucellus_pickaxe"
        private const val VAT_FERMENT_LOC = "loc.duke_sucellus_vat_ferment"
        private const val VAT_CHECK_LOC = "loc.duke_sucellus_vat_check"

        private const val ARDER_MUSHROOM_ITEM = "obj.duke_sucellus_mushroom_3"
        private const val MUSCA_MUSHROOM_ITEM = "obj.duke_sucellus_mushroom_1"
        private const val ARDER_POWDER_ITEM = "obj.duke_sucellus_mushroom_3_ground"
        private const val MUSCA_POWDER_ITEM = "obj.duke_sucellus_mushroom_1_ground"
        private const val SALT_ITEM = "obj.duke_sucellus_salt"
        private const val POISON_ITEM = "obj.duke_sucellus_potion_1_3"
        private const val PESTLE_MORTAR_ITEM = "obj.pestle_and_mortar"
        private const val WALL_PICKAXE_ITEM = "obj.iron_pickaxe"
        private val PICKAXE_ITEMS =
            listOf(
                "obj.bronze_pickaxe",
                "obj.iron_pickaxe",
                "obj.steel_pickaxe",
                "obj.mithril_pickaxe",
                "obj.adamant_pickaxe",
                "obj.rune_pickaxe",
                "obj.dragon_pickaxe",
            )

        private const val HIGH_HERBLORE_LEVEL = 80
        private const val MID_HERBLORE_LEVEL = 70
        private const val HIGH_POWDER_YIELD = 8
        private const val MID_POWDER_YIELD = 6
        private const val LOW_POWDER_YIELD = 3
        private const val SALT_YIELD = 2
        private const val INGREDIENTS_PER_BATCH = 6
        private const val POISONS_PER_BATCH = 2
        private const val FERMENT_TICKS = 10

        private const val NEED_PICKAXE_MESSAGE = "You need a pickaxe to mine the salt."
        private const val TAKE_PESTLE_MESSAGE = "You take the pestle and mortar."
        private const val TAKE_PICKAXE_MESSAGE = "You take the pickaxe."
        private const val PICK_MUSHROOM_MESSAGE = "You pick a mushroom."
        private const val ARDER_GROUND_MESSAGE = "You grind the mushroom into dust."
        private const val MUSCA_GROUND_MESSAGE = "You grind the mushroom into dust."
        private const val SALT_MINED_MESSAGE = "You manage to mine some salt."
        private const val VAT_ADD_MESSAGE = "You add the ingredient to the vat."
        private const val VAT_FERMENTING_MESSAGE = "You let the vat ferment."
        private const val VAT_READY_MESSAGE = "<col=229628>A fermentation vat is ready to be emptied.</col>"
        private const val NO_POISON_MESSAGE = "You don't have any Arder-musca poison to feed to Duke Sucellus."

        private const val GAS_VENT_INTERVAL = 9
        private val GAS_RAMP_SPOTANIMS =
            listOf(
                "spotanim.spotanim_duke_vent_01_spawn_01",
                "spotanim.spotanim_duke_vent_01_idle_01",
                "spotanim.spotanim_duke_vent_01_idle_02",
                "spotanim.spotanim_duke_vent_01_idle_03",
            )
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
        private const val ICE_TELEGRAPH_SPOTANIM = "spotanim.icicle_small_up"
        private const val ICE_LAND_SPOTANIM = "spotanim.icicle_small_floor"
        private const val ICE_HIT_MESSAGE = "<col=00ffff>You've been frozen in place!</col>"
        private const val ICE_FREEZE_TICKS = 2
        private val ICE_DAMAGE = 3..18
        private const val ICE_HIT_DELAY = 1
        private const val LEFT_CORRIDOR_X = 3029
        private const val RIGHT_CORRIDOR_X = 3047
        private val ICE_TILES: List<CoordGrid> =
            (6438..6458 step 2).flatMap { y ->
                listOf(CoordGrid(LEFT_CORRIDOR_X, y, 0), CoordGrid(RIGHT_CORRIDOR_X, y, 0))
            }

        // Extremity gaze: a fixed cone position along each corridor flares up periodically.
        private const val EXTREMITY_INTERVAL = 20
        private const val EXTREMITY_RESOLVE_DELAY = 4
        private const val EXTREMITY_CONE_SPOTANIM = "spotanim.duke_extremity_cone_1"
        private const val EXTREMITY_HIT_MESSAGE = "<col=ff00ff>You are caught in the extremity's gaze!</col>"
        private const val EXTREMITY_FREEZE_TICKS = 5
        private const val EXTREMITY_PRAYER_DRAIN = 8
        private val EXTREMITY_DAMAGE = 20..70
        private const val EXTREMITY_HIT_DELAY = 1
        private val EXTREMITY_TILES =
            listOf(6440, 6448, 6456).flatMap { y ->
                listOf(CoordGrid(LEFT_CORRIDOR_X, y, 0), CoordGrid(RIGHT_CORRIDOR_X, y, 0))
            }

        private val INSTANCE =
            InstanceArea.copyRegions(
                regionIds = listOf(ARENA_REGION),
                npcSpawns = listOf(InstanceNpc(SLEEP_NPC, CoordGrid(3036, 6452, 0))),
            )
    }
}
