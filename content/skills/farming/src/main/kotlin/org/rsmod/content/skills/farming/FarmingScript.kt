package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.invtx.*
import org.rsmod.api.player.events.skilling.FarmingSeedPlantedEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.farmingLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FarmingScript
@Inject
constructor(private val xpMods: XpModifiers, private val random: GameRandom, private val clock: FarmingClock, private val seedContainers: Set<FarmingSeedContainer> = emptySet()) : PluginScript() {
    private val transmitVarbits = HashMap<String, String>()

    override fun ScriptContext.startup() {
        for (patch in FarmingPatches.all) {
            resolveTransmitVarbit(patch)
            onOpLoc1(patch.loc) { if (atPatch(patch, it.loc.coords)) interact(patch) }
            onOpLoc2(patch.loc) { if (atPatch(patch, it.loc.coords)) inspect(patch) }
            onOpLocU(patch.loc) {
                if (atPatch(patch, it.loc.coords) && inv[it.invSlot]?.id == it.objType.id) {
                    useItem(patch, it.objType.id, it.loc.coords, it.invSlot)
                }
            }
        }

        onPlayerLogin {
            player.growPatches()
            player.transmitNearest()
            player.softTimer(TIMER_TRANSMIT, TRANSMIT_INTERVAL)
        }

        onPlayerSoftTimer(TIMER_TRANSMIT) {
            player.growPatches()
            player.transmitNearest()
        }
    }

    private fun resolveTransmitVarbit(patch: PatchDef) {
        val type = ServerCacheManager.getObject(patch.loc.asRSCM(RSCMType.LOC)) ?: return
        if (type.multiVarBit <= 0) {
            return
        }
        val name = RSCM.getReverseMapping(RSCMType.VARBIT, type.multiVarBit)
        if (name.isNotBlank()) {
            transmitVarbits[patch.loc] = name
        }
    }

    private fun Player.state(patch: PatchDef): PatchState = PatchState.unpack(vars[patch.varp])

    private fun Player.store(patch: PatchDef, state: PatchState) {
        VarPlayerIntMapSetter.set(this, patch.varp, state.pack())
        val varbit = transmitVarbits[patch.loc] ?: return
        if (FarmingPatches.nearest(coords) === FarmingPatches.areaOf(patch)) {
            VarPlayerIntMapSetter.set(this, varbit, state.transmit(state.crop))
        }
    }

    private fun Player.transmitNearest() {
        val area = FarmingPatches.nearest(coords) ?: return
        for (patch in area.patches) {
            val varbit = transmitVarbits[patch.loc] ?: continue
            val state = state(patch)
            VarPlayerIntMapSetter.set(this, varbit, state.transmit(state.crop))
        }
    }

    private fun Player.growPatches() {
        val now = clock.minute()
        val last = vars[VARP_CLOCK]
        if (last > now) return
        VarPlayerIntMapSetter.set(this, VARP_CLOCK, now)
        val elapsed = now - last
        if (last == 0 || elapsed <= 0) {
            return
        }
        for (patch in FarmingPatches.all) {
            val current = state(patch)
            val next = current.advance(current.crop, elapsed) { chance -> random.randomDouble() < chance }
            if (next != current) {
                VarPlayerIntMapSetter.set(this, patch.varp, next.pack())
            }
        }
    }

    private fun ProtectedAccess.atPatch(patch: PatchDef, coords: CoordGrid): Boolean {
        val area = FarmingPatches.areaOf(patch)
        return FarmingPatches.nearest(coords) === area && FarmingPatches.nearest(player.coords) === area
    }

    private suspend fun ProtectedAccess.waitUnchanged(patch: PatchDef, state: PatchState, ticks: Int): Boolean {
        val start = player.coords
        delay(ticks)
        player.growPatches()
        if (player.hitpoints <= 0 || player.pendingLogout || player.loggingOut || player.coords != start || !atPatch(patch, start) || player.state(patch) != state) {
            resetAnim()
            return false
        }
        return true
    }

    private suspend fun ProtectedAccess.interact(patch: PatchDef) {
        player.growPatches()
        val state = player.state(patch)
        val crop = state.crop

        when {
            !state.cleared -> rake(patch, state)
            crop == null -> mes("The patch is empty. Plant a seed in it to grow a crop.")
            state.health == Health.DEAD -> clear(patch)
            state.health == Health.DISEASED -> cure(patch, state, crop)
            !state.grown(crop) -> mes("The ${crop.name} is still growing.")
            else -> harvest(patch, state, crop)
        }
    }

    private fun ProtectedAccess.inspect(patch: PatchDef) {
        player.growPatches()
        val state = player.state(patch)
        val crop = state.crop
        mes(when {
            !state.cleared -> "The patch needs raking."
            crop == null -> "The patch is empty."
            state.health == Health.DEAD -> "The ${crop.name} has died."
            state.health == Health.DISEASED -> "The ${crop.name} is diseased and needs plant cure."
            state.grown(crop) -> "The ${crop.name} is ready to harvest."
            else -> "The ${crop.name} is growing."
        })
    }

    private suspend fun ProtectedAccess.rake(patch: PatchDef, state: PatchState) {
        if (!inv.contains(RAKE)) {
            mes("You need a rake to clear this patch.")
            return
        }

        var current = state
        while (!current.cleared) {
            anim(ANIM_RAKE)
            if (!waitUnchanged(patch, current, RAKE_TICKS) || !inv.contains(RAKE)) { resetAnim(); return }
            if (invAdd(inv, WEEDS, 1).failure) {
                mes("You don't have enough inventory space to hold any more weeds.")
                break
            }
            current = current.copy(weeds = current.weeds + 1)
            player.store(patch, current)
            statAdvance(STAT_FARMING, RAKE_XP * xpMods.get(player, STAT_FARMING))
        }
        resetAnim()
    }

    private suspend fun ProtectedAccess.cure(patch: PatchDef, state: PatchState, crop: Crop) {
        if (!inv.contains(PLANT_CURE)) {
            mes("You need plant cure to treat this ${crop.name}.")
            return
        }
        anim(ANIM_CURE)
        if (!waitUnchanged(patch, state, 2)) return
        if (invDel(inv, PLANT_CURE, 1).failure) {
            resetAnim()
            return
        }
        player.store(patch, state.copy(health = Health.HEALTHY))
        resetAnim()
        mes("The plant cure completely cures the disease.")
    }

    private suspend fun ProtectedAccess.harvest(patch: PatchDef, state: PatchState, crop: Crop) {
        if (!inv.contains(SPADE) && crop.kind == PatchKind.ALLOTMENT) {
            mes("You need a spade to harvest this patch.")
            return
        }

        var current = state
        while (current.produce > 0) {
            if (inv.freeSpace() < 1) {
                mes("You don't have enough inventory space to hold any more produce.")
                break
            }
            anim(ANIM_HARVEST)
            if (!waitUnchanged(patch, current, HARVEST_TICKS) || (crop.kind == PatchKind.ALLOTMENT && !inv.contains(SPADE))) { resetAnim(); return }
            if (invAdd(inv, crop.produce, 1).failure) {
                break
            }
            statAdvance(STAT_FARMING, crop.harvestXp * xpMods.get(player, STAT_FARMING))
            current = current.copy(produce = current.produce - 1)
            player.store(patch, current)
        }

        resetAnim()
        if (current.produce <= 0) {
            player.store(patch, PatchState())
            spam("The patch is now empty and full of weeds again.")
        }
    }

    private suspend fun ProtectedAccess.clear(patch: PatchDef) {
        val state = player.state(patch)
        if (state.crop == null) {
            mes("The patch has no crop to clear.")
            return
        }
        if (!inv.contains(SPADE)) {
            mes("You need a spade to clear this patch.")
            return
        }
        anim(ANIM_HARVEST)
        if (!waitUnchanged(patch, state, 2)) return
        if (!inv.contains(SPADE)) { resetAnim(); return }
        player.store(patch, PatchState())
        resetAnim()
        mes("You clear the patch.")
    }

    private suspend fun ProtectedAccess.useItem(patch: PatchDef, obj: Int, coords: CoordGrid, slot: Int) {
        if (player.hitpoints <= 0 || player.pendingLogout || player.loggingOut) return
        player.growPatches()
        val state = player.state(patch)
        val container = seedContainers.firstOrNull { it.recognises(obj) }
        when {
            container != null -> {
                val crop = container.seeds(player).asSequence().mapNotNull { FarmingCrops.bySeed(it) }.firstOrNull { it.kind == patch.kind }
                if (crop == null) mes("The seed box has no suitable seeds for this patch.")
                else plant(patch, state, crop, coords, SeedBoxInput(container, slot, inv[slot]!!))
            }
            obj == SPADE.asRSCM() -> clear(patch)
            obj == PLANT_CURE.asRSCM() -> {
                val crop = state.crop
                if (crop != null && state.health == Health.DISEASED) {
                    cure(patch, state, crop)
                } else {
                    mes("There is nothing on this patch that needs curing.")
                }
            }
            obj in WATERING_CANS.map { it.asRSCM() } -> water(patch, state, WATERING_CANS.first { it.asRSCM() == obj })
            Compost.entries.any { it != Compost.NONE && it.obj.asRSCM() == obj } -> applyCompost(patch, state, Compost.entries.first { it != Compost.NONE && it.obj.asRSCM() == obj }.obj)
            FarmingCrops.bySeed(obj) != null -> plant(patch, state, FarmingCrops.bySeed(obj)!!, coords)
            else -> mes("Nothing interesting happens.")
        }
    }

    private suspend fun ProtectedAccess.plant(patch: PatchDef, state: PatchState, crop: Crop, coords: CoordGrid, box: SeedBoxInput? = null) {
        if (crop.kind != patch.kind) {
            mes("You can't plant ${crop.name} seeds in this patch.")
            return
        }
        if (!state.cleared) {
            mes("The patch needs to be raked before you can plant anything in it.")
            return
        }
        if (state.crop != null) {
            mes("There is already something growing in this patch.")
            return
        }
        if (player.farmingLvl < crop.level) {
            mes("You need a Farming level of ${crop.level} to plant ${crop.name}.")
            return
        }
        if (!inv.contains(DIBBER)) {
            mes("You need a seed dibber to plant seeds.")
            return
        }
        if ((box?.container?.count(player, crop.seed.asRSCM()) ?: inv.physicalCount(crop.seed)) < crop.seedsPerPlant) {
            mes("You need ${crop.seedsPerPlant} ${crop.name} seeds to plant this patch.")
            return
        }

        anim(ANIM_PLANT)
        if (!waitUnchanged(patch, state, PLANT_TICKS)) return
        if (!inv.contains(DIBBER) || player.farmingLvl < crop.level || box != null && inv[box.slot] !== box.reference) { resetAnim(); return }
        val consumed = if (box != null) box.container.consume(player, crop.seed.asRSCM(), crop.seedsPerPlant) else player.invTransaction(inv) {
            delete(select(inv), crop.seed.asRSCM(), crop.seedsPerPlant)
        }.success
        if (!consumed) {
            resetAnim()
            return
        }

        val planted =
            state.copy(
                cropIndex = FarmingCrops.index(crop),
                stage = 0,
                watered = false,
                health = Health.HEALTHY,
                produce = 0,
                minutes = crop.stageMinutes,
            )
        player.store(patch, planted)
        statAdvance(STAT_FARMING, crop.plantXp * xpMods.get(player, STAT_FARMING))
        publish(FarmingSeedPlantedEvent(player, crop.seed.asRSCM(), patch.loc.asRSCM(), coords))
        resetAnim()
        spam("You plant the ${crop.name} seeds in the patch.")
    }

    private suspend fun ProtectedAccess.water(patch: PatchDef, state: PatchState, can: String) {
        val charges = WATERING_CANS.indexOf(can)
        val crop = state.crop
        when {
            charges <= 0 -> mes("Your watering can is empty.")
            crop == null -> mes("There is nothing to water on this patch.")
            crop.kind == PatchKind.HERB -> mes("Watering herbs does nothing.")
            state.health != Health.HEALTHY -> mes("Water won't cure your crops.")
            state.grown(crop) -> mes("Your crops are already fully grown.")
            state.watered -> mes("This patch has already been watered.")
            else -> {
                anim(ANIM_WATER)
                if (!waitUnchanged(patch, state, 2)) return
                if (player.invTransaction(inv) {
                        val inventory = select(inv)
                        delete(inventory, can.asRSCM(), 1)
                        add(inventory, WATERING_CANS[charges - 1].asRSCM(), 1)
                    }.failure) { resetAnim(); return }
                player.store(patch, state.copy(watered = true))
                statAdvance(STAT_FARMING, WATER_XP * xpMods.get(player, STAT_FARMING))
                resetAnim()
                spam("You water the ${crop.name}.")
            }
        }
    }

    private suspend fun ProtectedAccess.applyCompost(
        patch: PatchDef,
        state: PatchState,
        obj: String,
    ) {
        val compost = Compost.entries.first { it.obj == obj }
        when {
            !state.cleared -> mes("The patch needs to be raked first.")
            state.crop != null -> mes("You can only treat an empty patch with compost.")
            state.compost != Compost.NONE -> mes("This patch has already been treated.")
            else -> {
                anim(ANIM_COMPOST)
                if (!waitUnchanged(patch, state, 2)) return
                if (player.invTransaction(inv) {
                        val inventory = select(inv)
                        delete(inventory, obj.asRSCM(), 1)
                        add(inventory, "obj.bucket_empty".asRSCM(), 1)
                    }.failure) {
                    resetAnim()
                    return
                }
                player.store(patch, state.copy(compost = compost))
                statAdvance(STAT_FARMING, COMPOST_XP * xpMods.get(player, STAT_FARMING))
                resetAnim()
                spam("You treat the patch with compost.")
            }
        }
    }

    private data class SeedBoxInput(val container: FarmingSeedContainer, val slot: Int, val reference: InvObj)

    private companion object {
        const val VARP_CLOCK = "varp.farming_clock"
        const val TIMER_TRANSMIT = "timer.farming_transmit"
        const val TRANSMIT_INTERVAL = 10
        const val MILLIS_PER_MINUTE = 60_000L

        const val RAKE_TICKS = 3
        const val PLANT_TICKS = 3
        const val HARVEST_TICKS = 3

        const val RAKE_XP = 4.0
        const val WATER_XP = 1.0
        const val COMPOST_XP = 18.0
    }
}
