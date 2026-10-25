package org.rsmod.content.skills.farming

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.instances.InstanceAccess
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceId
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSpec
import org.rsmod.api.instances.RegionLocal
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.farmingLvl
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.generic.locs.water.WateringCans
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareGrid

@Singleton
class FarmingTithe @Inject constructor(
    private val instances: InstanceManager,
    private val locs: LocRepository,
    private val clock: MapClock,
    private val xpMods: XpModifiers,
    private val outfit: FarmerWornSetXpModifiers,
    private val equipment: FarmingEquipment,
    private val events: EventBus,
) {
    private data class Plot(val loc: BoundLocInfo, var plant: TithePlant)
    private class Field(
        val entry: CoordGrid,
        val sacks: List<LocInfo>,
        val plots: MutableMap<CoordGrid, Plot> = mutableMapOf(),
    )
    private val fields = mutableMapOf<InstanceId, Field>()

    fun enter(player: Player, resume: Boolean = false): Boolean {
        if (!resume) {
            val level = FarmingTitheData.crops.first().level
            if (player.farmingLvl < level) {
                player.mes("You need a Farming level of at least $level to enter.")
                return false
            }
            val crop = FarmingTitheData.crops.firstOrNull { player.inv.physicalCount(it.seed.internalName) > 0 }
            if (crop == null) {
                player.mes("Take some seeds from the table, otherwise you can't do any farming in there!")
                return false
            }
            set(player, TYPE, crop.tier)
        }
        val area = InstanceArea.copyRegions(
            regionIds = listOf((ENTRY.mx shl 8) or ENTRY.mz),
            enterCoord = RegionLocal(ENTRY.level, ENTRY.mx, ENTRY.mz, ENTRY.lx, ENTRY.lz),
            exitCoord = EXIT,
        )
        val spec = InstanceSpec(
            fee = 0, maxPlayers = 1, reclaimTicks = 0, graceTicks = 0,
            destroyWhenEmpty = true, area = area,
            settingsRowId = "dbrow.farming_tithe".asRSCM(RSCMType.DBROW),
            bossName = "Tithe Farm",
        )
        val result = instances.create(player, KEY, spec, InstanceAccess.Private, clock.cycle)
        val created = result as? InstanceManager.Result.Created
        if (created == null) {
            if (result is InstanceManager.Result.Failed) player.mes(result.reason)
            return false
        }
        player.coords = created.enter
        instances.finalizeEntry(player, created.session, clock.cycle)
        val emptySack = "loc.tithe_sack_of_fruit_empty".asRSCM(RSCMType.LOC)
        val sacks = FarmingTitheData.row.sacks.map { normal ->
            val coord = checkNotNull(instances.resolveCoord(created.session, normal))
            checkNotNull(locs.findAll(coord).firstOrNull { it.id == emptySack })
        }
        val field = Field(created.enter, sacks)
        fields[created.session.id] = field
        if (!resume) {
            set(player, SESSION, 1)
            set(player, SCORE, 0)
            if (!player.inv.contains(FERTILISER)) player.invAdd(player.inv, FERTILISER)
        }
        showSacks(player, field)
        set(player, "varbit.zeah_tithe_minigame_unlocked", 1)
        player.ifOpenOverlay(HUD, "component.toplevel_osrs_stretch:overlay_hud", events)
        player.softTimer(TIMER, 1)
        return true
    }

    fun login(player: Player) {
        if (player.vars[SESSION] > 0) enter(player, resume = true)
    }

    fun logout(player: Player) {
        instances.sessionForPlayer(player)?.takeIf { it.key == KEY }?.let { fields.remove(it.id) }
    }

    fun leave(player: Player, throughDoor: Boolean = true) {
        val session = instances.sessionForPlayer(player)?.takeIf { it.key == KEY }
        if (session != null) {
            fields.remove(session.id)
            val exit = instances.leave(player, session, clock.cycle)
            if (throughDoor) player.coords = exit
        }
        if (player.vars[SESSION] > 0) reset(player)
    }

    fun departed(player: Player, instance: InstanceId) {
        fields.remove(instance)
        if (!player.loggingOut) reset(player)
    }

    private fun reset(player: Player) {
        val result = player.invTransaction(player.inv) {
            val inv = select(player.inv)
            for (crop in FarmingTitheData.crops) {
                for (type in listOf(crop.seed, crop.fruit)) {
                    val count = player.inv.physicalCount(type.internalName)
                    if (count > 0) delete(inv, type.id, count)
                }
            }
            val fertiliser = player.inv.physicalCount(FERTILISER)
            if (fertiliser > 0) delete(inv, FERTILISER.asRSCM(), fertiliser)
        }
        check(result.success)
        set(player, SESSION, 0)
        set(player, SCORE, 0)
        set(player, TYPE, 0)
        player.ifCloseOverlay(HUD, events)
    }

    fun tick(player: Player) {
        if (player.vars[SESSION] <= 0) return
        val session = instances.sessionForPlayer(player)?.takeIf { it.key == KEY }
        val field = session?.let { fields[it.id] }
        val baseX = field?.entry?.x?.minus(ENTRY.lx) ?: 0
        val baseZ = field?.entry?.z?.minus(ENTRY.lz) ?: 0
        if (field == null || player.coords.level != field.entry.level ||
            player.coords.x !in baseX until baseX + MapSquareGrid.LENGTH ||
            player.coords.z !in baseZ until baseZ + MapSquareGrid.LENGTH
        ) {
            leave(player, throughDoor = false)
            return
        }
        for (plot in field.plots.values) advance(plot)
    }

    private fun advance(plot: Plot) {
        val next = plot.plant.advance(clock.cycle, FarmingTitheData.row.stageTicks)
        if (next != plot.plant && show(plot.loc, FarmingTitheData.loc(next))) plot.plant = next
    }

    internal fun plant(player: Player, loc: BoundLocInfo, crop: TitheCrop, slot: Int): Boolean {
        val field = field(player) ?: return false
        if (loc.coords in field || loc.id != EMPTY.asRSCM(RSCMType.LOC)) return false
        if (player.farmingLvl < crop.level || player.vars[TYPE] != crop.tier) return false
        if (!player.inv.contains(SPADE) ||
            !player.inv.contains(DIBBER) && !equipment.barehandedPlanting(player)
        ) return false
        val payment = player.invTransaction(player.inv, autoCommit = false) {
            delete(select(player.inv), crop.seed.id, 1, slot)
        }
        if (!payment.success) return false
        val plant = TithePlant(crop.tier, deadline = clock.cycle + FarmingTitheData.row.stageTicks)
        if (!show(loc, FarmingTitheData.loc(plant))) return false
        payment.commitAll()
        field[loc.coords] = Plot(loc, plant)
        return true
    }

    fun water(player: Player, loc: BoundLocInfo, can: String? = null): Boolean {
        val plot = field(player)?.get(loc.coords) ?: return false
        advance(plot)
        val plant = plot.plant
        if (plant.dead || plant.stage == 4 || plant.watered) return false
        val selected = can ?: WateringCans.CANS.firstOrNull {
            player.inv.physicalCount(it) > 0 && when {
                it == "obj.zeah_wateringcan" -> player.vars["varbit.zeah_wateringcan_charges"] > 0
                else -> (WateringCans.doses(it) ?: 0) > 0
            }
        } ?: return false
        val next = plant.copy(watered = true)
        if (!show(loc, FarmingTitheData.loc(next))) return false
        if (!WateringCans.consume(player, selected)) {
            check(show(loc, FarmingTitheData.loc(plant)))
            return false
        }
        plot.plant = next
        return true
    }

    fun harvest(player: Player, loc: BoundLocInfo): Boolean {
        val field = field(player) ?: return false
        val plot = field[loc.coords] ?: return false
        advance(plot)
        if (plot.plant.dead || plot.plant.stage != 4 || !player.inv.contains(SPADE)) return false
        val crop = checkNotNull(FarmingTitheData.crop(plot.plant.tier))
        val award = player.invTransaction(player.inv, autoCommit = false) {
            add(select(player.inv), crop.fruit.id, 1)
        }
        if (!award.success || !show(loc, EMPTY)) return false
        award.commitAll()
        field.remove(loc.coords)
        player.statAdvance(STAT_FARMING, crop.harvestXp * xpMods.get(player, STAT_FARMING))
        return true
    }

    fun clear(player: Player, loc: BoundLocInfo): Boolean {
        val field = field(player) ?: return false
        val plot = field[loc.coords] ?: return false
        advance(plot)
        if (!plot.plant.dead || !player.inv.contains(SPADE) || !show(loc, EMPTY)) return false
        field.remove(loc.coords)
        return true
    }

    fun deposit(player: Player): Boolean {
        if (field(player) == null) return false
        val crop = FarmingTitheData.crop(player.vars[TYPE]) ?: return false
        val modifier = xpMods.get(player, STAT_FARMING)
        if (!depositTitheFruit(player, crop, modifier, modifier - with(outfit) { player.modifier() })) return false
        val session = checkNotNull(instances.sessionForPlayer(player))
        showSacks(player, checkNotNull(fields[session.id]))
        return true
    }

    private fun showSacks(player: Player, field: Field) {
        val symbol = if (player.vars[SCORE] == 0) "loc.tithe_sack_of_fruit_empty" else "loc.tithe_sack_of_fruit"
        val id = symbol.asRSCM(RSCMType.LOC)
        for (sack in field.sacks) {
            check(locs.add(LocInfo(sack.layer, sack.coords, LocEntity(id, sack.shapeId, sack.angleId)), Int.MAX_VALUE))
        }
    }

    private fun field(player: Player): MutableMap<CoordGrid, Plot>? {
        val session = instances.sessionForPlayer(player)?.takeIf { it.key == KEY } ?: return null
        return fields[session.id]?.plots
    }

    private fun show(loc: BoundLocInfo, symbol: String): Boolean = locs.add(
        LocInfo(loc.layer, loc.coords, LocEntity(symbol.asRSCM(RSCMType.LOC), loc.shapeId, loc.angleId)),
        Int.MAX_VALUE,
    )

    private fun set(player: Player, variable: String, value: Int) = VarPlayerIntMapSetter.set(player, variable, value)

    companion object {
        const val KEY = "tithe_farm"
        const val TIMER = "timer.farming_tithe"
        const val SESSION = "varp.farming_tithe_session"
        const val SCORE = "varbit.hosidius_tithe_score"
        const val TYPE = "varbit.hosidius_tithe_type"
        const val POINTS = "varbit.hosidius_tithe_rewardpoints"
        const val EMPTY = "loc.hosidius_tithe_empty"
        const val FERTILISER = "obj.hosidius_tithe_fertiliser"
        const val HUD = "interface.hosidius_strip_farming"
        private val ENTRY = CoordGrid(1808, 3501)
        private val EXIT = CoordGrid(1804, 3501)
    }
}

internal fun depositTitheFruit(player: Player, crop: TitheCrop, modifier: Double, bonusModifier: Double): Boolean {
    val row = FarmingTitheData.row
    val deposited = player.vars[FarmingTithe.SESSION] - 1
    if (deposited !in 0 until row.seedLimit) return false
    val available = minOf(player.inv.physicalCount(crop.fruit.internalName), row.seedLimit - deposited)
    val reward = titheDeposit(player.vars[FarmingTithe.SCORE], available, crop.harvestXp, row.sackSize, row.bonusThreshold)
    if (reward.count <= 0) return false
    val result = player.invTransaction(player.inv) {
        delete(select(player.inv), crop.fruit.id, reward.count)
    }
    if (!result.success) return false
    VarPlayerIntMapSetter.set(player, FarmingTithe.SESSION, deposited + reward.count + 1)
    VarPlayerIntMapSetter.set(player, FarmingTithe.SCORE, reward.score)
    VarPlayerIntMapSetter.set(player, FarmingTithe.POINTS, minOf(row.pointsCap, player.vars[FarmingTithe.POINTS] + reward.points))
    val singleXp = crop.harvestXp * 10 * modifier
    repeat(reward.count - reward.doubled) { player.statAdvance(STAT_FARMING, singleXp) }
    repeat(reward.doubled) { player.statAdvance(STAT_FARMING, singleXp * 2) }
    if (reward.bonusXp > 0) player.statAdvance(STAT_FARMING, reward.bonusXp * bonusModifier)
    return true
}

internal fun takeTitheSeeds(player: Player, crop: TitheCrop, amount: Int): Boolean {
    if (player.farmingLvl < crop.level || amount <= 0) return false
    if (FarmingTitheData.crops.any { it != crop && player.inv.physicalCount(it.seed.internalName) > 0 }) return false
    val owned = player.inv.physicalCount(crop.seed.internalName)
    val take = minOf(amount, FarmingTitheData.row.seedLimit - owned)
    if (take <= 0 || !player.invAdd(player.inv, crop.seed.internalName, take).success) return false
    VarPlayerIntMapSetter.set(player, FarmingTithe.TYPE, crop.tier)
    return true
}

internal fun refundTitheOutfit(player: Player, obj: Int, slot: Int, points: Int): Boolean {
    if (player.vars[FarmingTithe.POINTS] > FarmingTitheData.row.pointsCap - points) return false
    val result = player.invTransaction(player.inv) { delete(select(player.inv), obj, 1, slot) }
    if (!result.success) return false
    VarPlayerIntMapSetter.set(player, FarmingTithe.POINTS, player.vars[FarmingTithe.POINTS] + points)
    return true
}
