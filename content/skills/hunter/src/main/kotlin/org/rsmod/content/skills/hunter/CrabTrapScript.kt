package org.rsmod.content.skills.hunter

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.enums.enum
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.route.RouteFactory
import org.rsmod.api.route.walkTo
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.table.hunter.HunterCrabTrapsRow
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.skills.hunter.HunterTrapScript.Companion.distance
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

@Singleton
class CrabTrapScript
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val worldRepo: WorldRepository,
    private val routes: RouteFactory,
    private val random: GameRandom,
    private val clock: MapClock,
) : PluginScript() {
    private val sites: List<Site> by lazy { HunterCrabTrapsRow.all().map(::Site) }
    private val pending = HashMap<Player, MutableList<Pending>>()

    override fun ScriptContext.startup() {
        for (site in sites) {
            for ((index, loc) in site.locs.withIndex()) {
                onOpLoc1(loc) { op(site, index, it.loc.coords, LocInfo(it.loc.layer, it.loc.coords, it.loc.entity)) }
            }
        }
        onPlayerSoftTimer(TIMER) { player.tick() }
        onPlayerLogout { pending.remove(player) }
    }

    private suspend fun ProtectedAccess.op(site: Site, index: Int, coords: CoordGrid, loc: LocInfo) {
        val varbit = site.varbits[index]
        when (vars[varbit]) {
            UNBUILT -> build(varbit)
            BUILT -> bait(site, index, coords, loc)
            else -> empty(site, index, coords, loc)
        }
    }

    private suspend fun ProtectedAccess.build(varbit: String) {
        if (stat(CONSTRUCTION) < BUILD_LEVEL) {
            mes("You need a Construction level of $BUILD_LEVEL to build a crab trap.")
            return
        }
        val nails = NAILS.firstOrNull { inv.count(it) >= NAILS_NEEDED }
        if (HAMMER !in inv || SAWS.none { it in inv } || BUCKET !in inv || PLANK !in inv || nails == null) {
            mes("You need a hammer, a saw, a bucket, a plank and two nails to build a crab trap.")
            return
        }
        anim(BUILD_SEQ)
        delay(BUILD_TICKS)
        if (vars[varbit] != UNBUILT) return
        val used =
            player.invTransaction(inv) {
                val target = select(inv)
                delete(target, BUCKET.asRSCM(RSCMType.OBJ), 1)
                delete(target, PLANK.asRSCM(RSCMType.OBJ), 1)
                delete(target, nails.asRSCM(RSCMType.OBJ), NAILS_NEEDED)
            }
        if (used.failure) return
        statAdvance(CONSTRUCTION, BUILD_XP)
        VarPlayerIntMapSetter.set(player, varbit, BUILT)
    }

    private fun ProtectedAccess.bait(site: Site, index: Int, coords: CoordGrid, loc: LocInfo) {
        if (statBase(HUNTER) < site.level) {
            mes("You need a Hunter level of ${site.level} to catch these crabs.")
            return
        }
        if (statBase(SAILING) < site.sailingLevel) {
            mes("You need a Sailing level of ${site.sailingLevel} to catch these crabs.")
            return
        }
        if (site.quest.isNotEmpty() && !QuestRequirements.hasCompleted(player, site.quest) &&
            !QuestRequirements.isOnQuest(player, site.quest)
        ) {
            return
        }
        val limit = maxTraps(statBase(HUNTER))
        if (activeTraps(player) >= limit) {
            val amount = if (limit == 1) "one trap" else "$limit traps"
            mes("You may set up only $amount at a time at your Hunter level.")
            return
        }
        if (invDel(inv, site.bait).failure) {
            mes("You need some bait to put in the trap.")
            return
        }
        anim(BAIT_SEQ)
        VarPlayerIntMapSetter.set(player, site.varbits[index], ACTIVE)
        pending.getOrPut(player) { mutableListOf() } += Pending(site, index, coords, loc, clock.cycle + site.delay)
        player.softTimer(TIMER, 1)
    }

    private suspend fun ProtectedAccess.empty(site: Site, index: Int, coords: CoordGrid, loc: LocInfo) {
        val varbit = site.varbits[index]
        val state = vars[varbit]
        anim(EMPTY_SEQ)
        if (state == ACTIVE) {
            pending[player]?.removeAll { it.site === site && it.index == index }
            VarPlayerIntMapSetter.set(player, varbit, BUILT)
            return
        }
        if (state < FULL) return
        val catch = site.catches.getOrNull(state - FULL) ?: site.catches.first()
        if (invAdd(inv, catch).failure) {
            mes("You don't have enough inventory space.")
            return
        }
        statAdvance(HUNTER, site.xp.toDouble())
        VarPlayerIntMapSetter.set(player, varbit, BUILT)
        delay(REBAIT_TICKS)
        if (vars[varbit] == BUILT && site.bait in inv) bait(site, index, coords, loc)
    }

    private fun activeTraps(player: Player): Int =
        sites.sumOf { site -> site.varbits.count { player.vars[it] >= ACTIVE } }

    private fun Player.tick() {
        val list = pending[this]
        if (list.isNullOrEmpty()) {
            pending.remove(this)
            clearSoftTimer(TIMER)
            return
        }
        val now = clock.cycle
        for (entry in list.toList()) {
            if (distance(coords, entry.coords) > LEAVE_RANGE) {
                list.remove(entry)
                VarPlayerIntMapSetter.set(this, entry.site.varbits[entry.index], BUILT)
                continue
            }
            if (entry.crab == null && now >= entry.arrive - CRAB_WALK_TICKS) {
                val start = entry.coords.translate(random.of(-CRAB_START, CRAB_START), random.of(-CRAB_START, CRAB_START))
                val crab = Npc(entry.site.crab, start)
                npcRepo.add(crab, CRAB_WALK_TICKS + 1)
                crab.walkTo(routes, entry.coords)
                entry.crab = crab
            }
            if (now < entry.arrive) continue
            list.remove(entry)
            entry.crab?.let { if (it.isSlotAssigned) npcRepo.del(it, Int.MAX_VALUE) }
            if (vars[entry.site.varbits[entry.index]] != ACTIVE) continue
            worldRepo.locAnim(entry.loc, TRIGGER_SEQ)
            val full = FULL + random.of(entry.site.catches.size)
            VarPlayerIntMapSetter.set(this, entry.site.varbits[entry.index], full)
        }
    }

    class Pending(val site: Site, val index: Int, val coords: CoordGrid, val loc: LocInfo, val arrive: Int) {
        var crab: Npc? = null
    }

    class Site(row: HunterCrabTrapsRow) {
        val key: String = row.site
        val locs: List<String> = row.locs.map { RSCM.getReverseMapping(RSCMType.LOC, it.id) }
        val varbits: List<String> = row.varbits
        val crab: String = RSCM.getReverseMapping(RSCMType.NPC, row.crab.id)
        val catches: List<String> = row.catches.map { it.internalName }
        val level: Int = row.level
        val xp: Int = row.xp
        val sailingLevel: Int = row.sailingLevel
        val bait: String = row.bait.internalName
        val delay: Int = row.delayTicks
        val quest: String = row.quest
    }

    companion object {
        const val HUNTER = "stat.hunter"
        const val SAILING = "stat.sailing"
        const val CONSTRUCTION = "stat.construction"
        const val TIMER = "timer.hunter_crab_traps"
        const val HAMMER = "obj.hammer"
        val SAWS = listOf("obj.poh_saw", "obj.eyeglo_crystal_saw")
        const val BUCKET = "obj.bucket_empty"
        const val PLANK = "obj.woodplank"
        val NAILS =
            listOf(
                "obj.nails_bronze", "obj.nails_iron", "obj.nails", "obj.nails_black", "obj.nails_mithril",
                "obj.nails_adamant", "obj.nails_rune",
            )
        const val BUILD_SEQ = "seq.human_poh_build"
        const val BAIT_SEQ = "seq.human_pickupfloor"
        const val EMPTY_SEQ = "seq.human_pickupfloor"
        const val TRIGGER_SEQ = "seq.hunting_crab01_trap01_triggered"
        const val UNBUILT = 0
        const val BUILT = 1
        const val ACTIVE = 2
        const val FULL = 3
        const val BUILD_LEVEL = 10
        const val BUILD_XP = 30.0
        const val BUILD_TICKS = 4
        const val NAILS_NEEDED = 2
        const val REBAIT_TICKS = 3
        const val LEAVE_RANGE = 30
        const val CRAB_WALK_TICKS = 4
        const val CRAB_START = 3

        private val TRAP_LIMITS by lazy { enum<Int, Int>("hunter_crab_trap_limits") }

        fun maxTraps(level: Int): Int = HunterTrapScript.byLevel(TRAP_LIMITS, level)
    }
}
