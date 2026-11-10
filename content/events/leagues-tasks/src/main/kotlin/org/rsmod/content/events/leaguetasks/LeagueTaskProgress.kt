package org.rsmod.content.events.leaguetasks

import dev.openrune.ServerCacheManager
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.interfaces.collectionlog.CollectionLog
import org.rsmod.game.entity.Player

/** Decides when all of a task's requirements are met, and completes it through [LeagueTasks]. */
@Singleton
public class LeagueTaskProgress
@Inject
constructor(
    private val tasks: LeagueTasks,
    private val definitions: LeagueTaskDefinitions,
    private val areas: AreaChecker,
) {
    private val trainableStats: List<String> =
        ServerCacheManager.getStats().values.filterNot { it.unreleased }.map { it.internalName }

    private var indexes: Map<Int, LeagueTaskIndex> = buildIndexes()

    private val views = mutableListOf<PerLeague<*>>()

    public val activeIndexes: Collection<LeagueTaskIndex>
        get() = indexes.values

    private fun buildIndexes(): Map<Int, LeagueTaskIndex> =
        definitions.active.associate { it.league.type to it.index }

    /** A value built from each active league's index, rebuilt when the task files are reloaded. */
    public fun <T> perLeague(build: (LeagueTaskIndex) -> T): PerLeague<T> =
        PerLeague(build).also { views += it }

    public fun indexOf(player: Player): LeagueTaskIndex? =
        tasks.leagueOf(player)?.let { indexes[it.type] }

    internal fun reload() {
        definitions.reload()
        indexes = buildIndexes()
        views.forEach { it.rebuild() }
    }

    public fun check(player: Player, task: Int) {
        val index = indexOf(player) ?: return
        check(player, index, task)
    }

    private fun check(player: Player, index: LeagueTaskIndex, task: Int) {
        val bit = index.league.indexOf(task) ?: return
        if (tasks.isComplete(player, bit)) {
            return
        }
        val rows = index.rowsByTask[task] ?: return
        if (rows.all { isMet(player, it) }) {
            tasks.complete(player, task)
        }
    }

    /**
     * Records [amount] occurrences of one event that met [rows]. Each row whose task is still open
     * and whose areas hold the player completes a one-off task straight away, or advances its
     * counter; a counter shared by several rows advances once, capped at the largest of their
     * counts. Every advanced task is then checked.
     */
    public fun record(player: Player, rows: Collection<LeagueTaskRequirement>, amount: Int = 1) {
        if (amount <= 0 || rows.isEmpty()) {
            return
        }
        val index = indexOf(player) ?: return
        val advanced = HashSet<Int>()
        for (row in rows) {
            val bit = index.league.indexOf(row.task) ?: continue
            if (tasks.isComplete(player, bit)) {
                continue
            }
            if (row.areas.isNotEmpty() && row.areas.none { areas.inArea(it, player.coords) }) {
                continue
            }
            val counter = row.counter
            if (counter == null) {
                tasks.complete(player, row.task)
                continue
            }
            if (advanced.add(counter.id)) {
                val cap = index.counterCaps.getValue(counter.id)
                val progress = player.vars[counter]
                if (progress < cap) {
                    val capped = (progress.toLong() + amount).coerceAtMost(cap.toLong()).toInt()
                    VarPlayerIntMapSetter.set(player, counter, capped)
                }
            }
            check(player, index, row.task)
        }
    }

    public fun checkAll(player: Player, trigger: LeagueTaskTrigger) {
        val index = indexOf(player) ?: return
        for (row in index.rows(trigger)) {
            check(player, index, row.task)
        }
    }

    public fun checkState(player: Player) {
        for (trigger in LeagueTaskTrigger.entries) {
            if (trigger.isState) {
                checkAll(player, trigger)
            }
        }
    }

    private fun isMet(player: Player, row: LeagueTaskRequirement): Boolean {
        val stats = row.stats.ifEmpty { trainableStats }
        return when (row.trigger) {
            LeagueTaskTrigger.Level ->
                stats.count { player.statBase(it) >= row.value } >= row.count
            LeagueTaskTrigger.BaseLevel -> trainableStats.all { player.statBase(it) >= row.value }
            LeagueTaskTrigger.TotalLevel -> trainableStats.sumOf(player::statBase) >= row.value
            LeagueTaskTrigger.CombatLevel -> player.combatLevel >= row.value
            LeagueTaskTrigger.Xp ->
                stats.count { player.statMap.getXP(it) >= row.value } >= row.count
            LeagueTaskTrigger.Equip -> {
                val worn = player.worn.mapNotNullTo(HashSet()) { it?.id }
                row.objs.count { it in worn } >= row.count
            }
            LeagueTaskTrigger.Var -> {
                val varbit = row.varbit
                val varp = row.varp
                val current =
                    when {
                        varbit != null -> player.vars[varbit]
                        varp != null -> player.vars[varp]
                        else -> 0
                    }
                current >= row.value
            }
            LeagueTaskTrigger.CollectionSlots ->
                CollectionLog.filledSlots(player, row.collections) >= row.value
            LeagueTaskTrigger.Kill,
            LeagueTaskTrigger.Product,
            LeagueTaskTrigger.Spell,
            LeagueTaskTrigger.Alchemy,
            LeagueTaskTrigger.Firemaking,
            LeagueTaskTrigger.Bury,
            LeagueTaskTrigger.Offer,
            LeagueTaskTrigger.Consume,
            LeagueTaskTrigger.Interface,
            LeagueTaskTrigger.Enter,
            LeagueTaskTrigger.Obtain,
            LeagueTaskTrigger.ShopBuy,
            LeagueTaskTrigger.Interact,
            LeagueTaskTrigger.Pickpocket,
            LeagueTaskTrigger.Steal,
            LeagueTaskTrigger.SpecialAttack,
            LeagueTaskTrigger.Ammo,
            LeagueTaskTrigger.Emote,
            LeagueTaskTrigger.UseOn,
            LeagueTaskTrigger.Pick,
            LeagueTaskTrigger.SlayerTaskComplete,
            LeagueTaskTrigger.SlayerKill,
            LeagueTaskTrigger.SuperiorKill ->
                row.counter?.let { player.vars[it] >= row.count } ?: false
        }
    }

    public inner class PerLeague<T> internal constructor(private val build: (LeagueTaskIndex) -> T) {
        private var values: Map<Int, T> = buildAll()

        private fun buildAll(): Map<Int, T> =
            indexes.mapValues { (_, index) -> build(index) }

        internal fun rebuild() {
            values = buildAll()
        }

        public operator fun get(player: Player): T? =
            tasks.leagueOf(player)?.let { values[it.type] }
    }
}
