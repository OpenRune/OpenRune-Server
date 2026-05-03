package org.rsmod.content.skills.woodcutting.scripts

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.ObjectServerType
import dev.openrune.types.SequenceServerType
import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.config.locParam
import org.rsmod.api.config.locXpParam
import org.rsmod.api.config.objParam
import org.rsmod.api.config.refs.params
import org.rsmod.api.controller.vars.intVarCon
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.woodcuttingLvl
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.script.onAiConTimer
import org.rsmod.api.script.onOpContentLoc1
import org.rsmod.api.script.onOpContentLoc3
import org.rsmod.api.script.onOpContentU
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.woodcutting.configs.WoodcuttingParams
import org.rsmod.events.UnboundEvent
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Controller
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.getInvObj
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO:
// - bird nests
// - axe effects/charges
class Woodcutting
@Inject
constructor(
    private val locRepo: LocRepository,
    private val conRepo: ControllerRepository,
    private val playerRepo: PlayerRepository,
    private val xpMods: XpModifiers,
    private val invisibleLvls: InvisibleLevels,
    private val mapClock: MapClock,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpContentLoc1("content.tree") { attempt(it.loc, it.type) }
        onOpContentLoc3("content.tree") { cut(it.loc, it.type) }
        onOpContentU("content.tree", "content.woodcutting_axe") { cut(it.loc, it.type) }
        onAiConTimer("controller.woodcutting_tree_duration") { controller.treeDespawnTick() }
    }

    private fun ProtectedAccess.attempt(tree: BoundLocInfo, type: ObjectServerType) {
        if (player.woodcuttingLvl < type.treeLevelReq) {
            mes("You need a Woodcutting level of ${type.treeLevelReq} to chop down this tree.")
            return
        }

        if (inv.isFull()) {
            val product = type.treeLogs
            mes("Your inventory is too full to hold any more ${product.name.lowercase()}.")
            soundSynth("synth.pillory_wrong")
            return
        }

        if (actionDelay < mapClock) {
            actionDelay = mapClock + 3
            skillAnimDelay = mapClock + 3
            opLoc1(tree)
        } else {
            val axe = findAxe(player)
            if (axe == null) {
                mes("You need an axe to chop down this tree.")
                mes("You do not have an axe which you have the woodcutting level to use.")
                return
            }
            anim(RSCM.getReverseMapping(RSCMType.SEQ, getInvObj(axe).axeWoodcuttingAnim.id))
            spam("You swing your axe at the tree.")
            cut(tree, type)
        }
    }

    private fun ProtectedAccess.cut(tree: BoundLocInfo, type: ObjectServerType) {
        val axe = findAxe(player)
        if (axe == null) {
            mes("You need an axe to chop down this tree.")
            mes("You do not have an axe which you have the woodcutting level to use.")
            return
        }

        if (player.woodcuttingLvl < type.treeLevelReq) {
            mes("You need a Woodcutting level of ${type.treeLevelReq} to chop down this tree.")
            return
        }

        if (inv.isFull()) {
            val product = type.treeLogs
            mes("Your inventory is too full to hold any more ${product.name.lowercase()}.")
            soundSynth("synth.pillory_wrong")
            return
        }

        if (skillAnimDelay <= mapClock) {
            skillAnimDelay = mapClock + 4
            anim(RSCM.getReverseMapping(RSCMType.SEQ,getInvObj(axe).axeWoodcuttingAnim.id))
        }

        var cutLogs = false
        val despawn: Boolean

        if (actionDelay < mapClock) {
            actionDelay = mapClock + 3
        } else if (actionDelay == mapClock) {
            val (low, high) = cutSuccessRates(type, axe)
            cutLogs = statRandom("stat.woodcutting", low, high, invisibleLvls)
        }

        if (type.hasDespawnTimer) {
            treeSwingDespawnTick(tree, type)
            despawn = cutLogs && isTreeDespawnRequired(tree)
        } else {
            despawn = cutLogs && random.of(1, 255) > type.treeDepleteChance
        }

        if (cutLogs) {
            val product = type.treeLogs
            val xp = type.treeXp * xpMods.get(player, "stat.woodcutting")
            spam("You get some ${product.name.lowercase()}.")
            statAdvance("stat.woodcutting", xp)
            invAdd(inv, RSCM.getReverseMapping(RSCMType.OBJ, product.id))
            publish(CutLogs(player, tree, product))
        }

        if (despawn) {
            val respawnTime = type.resolveRespawnTime(random)
            locRepo.change(tree, type.treeStump, respawnTime)
            resetAnim()
            soundSynth("synth.tree_fall_sound")
            sendLocalOverlayLoc(tree, type, respawnTime)
        }

        opLoc3(tree)
    }

    private fun Controller.treeDespawnTick() {
        val type = ServerCacheManager.getObject(treeLocId)!!
        val tree = locRepo.findExact(coords, type)
        if (tree == null) {
            // Make sure the controller has lived beyond a single tick. Otherwise, we can make an
            // educated guess that there's an oversight allowing the tree to recreate controllers
            // faster than we'd expect. (1 tick intervals in this case)
            check(mapClock > creationCycle + 1) { "Tree loc deleted faster than expected." }
            conRepo.del(this)
            return
        }

        // If tree is actively being cut down by a player, increment the associated varcon.
        if (treeLastCut == mapClock.cycle - 1) {
            treeActivelyCutTicks++
        } else {
            treeActivelyCutTicks--
        }

        // If the tree has been idle (not cut) for a duration equal to or longer than the time it
        // was actively cut, the controller is no longer needed and can be safely deleted.
        if (treeActivelyCutTicks <= 0) {
            conRepo.del(this)
            return
        }

        // Reset the timer for next tick.
        aiTimer(1)

        // Keep the controller alive.
        resetDuration()
    }

    private fun treeSwingDespawnTick(tree: BoundLocInfo, type: ObjectServerType) {
        val controller = conRepo.findExact(tree.coords, "controller.woodcutting_tree_duration")
        if (controller != null) {
            check(controller.treeLocId == tree.id) {
                "Controller in coords is not associated with tree: " +
                    "controller=$controller, treeLoc=$tree, treeType=$type"
            }
            controller.treeLastCut = mapClock.cycle
            return
        }

        val spawn = Controller("controller.woodcutting_tree_duration", tree.coords)
        conRepo.add(spawn, type.treeDespawnTime)

        spawn.treeLocId = tree.id
        spawn.treeLastCut = mapClock.cycle
        spawn.treeActivelyCutTicks = 0
        spawn.aiTimer(1)
    }

    private fun isTreeDespawnRequired(tree: BoundLocInfo): Boolean {
        val controller = conRepo.findExact(tree.coords, "controller.woodcutting_tree_duration")
        return controller != null && controller.treeActivelyCutTicks >= controller.durationStart
    }

    private fun sendLocalOverlayLoc(tree: BoundLocInfo, type: ObjectServerType, respawnTime: Int) {
        val players = playerRepo.findAll(ZoneKey.from(tree.coords), zoneRadius = 3)
        for (player in players) {
            ClientScripts.addOverlayTimerLoc(
                player = player,
                coords = tree.coords,
                loc = type,
                shape = tree.shape,
                timer = Constants.overlay_timer_woodcutting,
                ticks = respawnTime,
                colour = 16765184,
            )
        }
    }

    data class CutLogs(val player: Player, val tree: BoundLocInfo, val product: ItemServerType) :
        UnboundEvent

    companion object {
        var Controller.treeActivelyCutTicks: Int by intVarCon("varcon.woodcutting_tree_cut_ticks")
        var Controller.treeLastCut: Int by intVarCon("varcon.woodcutting_tree_last_cut")
        var Controller.treeLocId: Int by intVarCon("varcon.woodcutting_tree_loc")

        val ItemServerType.axeWoodcuttingReq: Int by objParam(params.levelrequire)
        val ItemServerType.axeWoodcuttingAnim: SequenceServerType by objParam(params.skill_anim)

        val ObjectServerType.treeLevelReq: Int by locParam(params.levelrequire)
        val ObjectServerType.treeLogs: ItemServerType by locParam(params.skill_productitem)
        val ObjectServerType.treeXp: Double by locXpParam(params.skill_xp)
        val ObjectServerType.treeStump: ObjectServerType by locParam(params.next_loc_stage)
        val ObjectServerType.treeDespawnTime: Int by locParam(params.despawn_time)
        val ObjectServerType.treeDepleteChance: Int by locParam(params.deplete_chance)
        val ObjectServerType.treeRespawnTime: Int by locParam(params.respawn_time)
        val ObjectServerType.treeRespawnTimeLow: Int by locParam(params.respawn_time_low)
        val ObjectServerType.treeRespawnTimeHigh: Int by locParam(params.respawn_time_high)

        private val ObjectServerType.hasDespawnTimer: Boolean
            get() = hasParam(params.despawn_time)

        fun findAxe(player: Player): InvObj? {
            val worn = player.wornAxe()
            val carried = player.carriedAxe()
            if (worn != null && carried != null) {
                if (getInvObj(worn).axeWoodcuttingReq >= getInvObj(carried).axeWoodcuttingReq) {
                    return worn
                }
                return carried
            }
            return worn ?: carried
        }

        private fun Player.wornAxe(): InvObj? {
            val righthand = righthand ?: return null
            return righthand.takeIf { getInvObj(it).isUsableAxe(woodcuttingLvl) }
        }

        private fun Player.carriedAxe(): InvObj? {
            return inv.filterNotNull { getInvObj(it).isUsableAxe(woodcuttingLvl) }
                .maxByOrNull { getInvObj(it).axeWoodcuttingReq }
        }

        private fun ItemServerType.isUsableAxe(woodcuttingLevel: Int): Boolean =
            isContentType("content.woodcutting_axe") && woodcuttingLevel >= axeWoodcuttingReq

        private fun ObjectServerType.resolveRespawnTime(random: GameRandom): Int {
            val fixed = treeRespawnTime
            if (fixed > 0) {
                return fixed
            }
            return random.of(treeRespawnTimeLow, treeRespawnTimeHigh)
        }

        fun cutSuccessRates(treeType: ObjectServerType, axe: InvObj): Pair<Int, Int> {
            val axes = treeType.param(WoodcuttingParams.success_rates)
            val rates = axes.find { it.key.id == axe.id }?.value ?: error("Unable to get axe rates")
            val low = rates shr 16
            val high = rates and 0xFFFF
            return low to high
        }
    }
}
