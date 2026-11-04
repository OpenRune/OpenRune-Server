package org.rsmod.content.skills.farming

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.table.QuestRow
import org.rsmod.content.quest.manager.QuestRequirementResolver
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

@Singleton
internal class FarmingWhiteTree
@Inject
constructor(
    private val requirements: QuestRequirementResolver,
    private val xpMods: XpModifiers,
    private val eventBus: EventBus,
) {
    private val questEnd by lazy { QuestRow.getRow("dbrow.$QUEST").endstate }

    internal fun available(player: Player): Boolean =
        requirements.hasCompleted(player, QUEST) || player.vars[QUEST_PROGRESS] >= questEnd

    internal fun refresh(player: Player, millis: Long = System.currentTimeMillis()) {
        if (!available(player)) return
        val art = player.vars[TREE]
        val last = player.vars[CLOCK]
        val now = FarmingClock.cycle(player, REGROW_MINUTES, millis).toInt()
        if (art == 0 && last == 0) {
            VarPlayerIntMapSetter.set(player, TREE, FULL)
            VarPlayerIntMapSetter.set(player, CLOCK, now)
        } else if (art in EMPTY..FULL) {
            if (last > 0 && now > last) {
                val grown = minOf(FULL.toLong(), art.toLong() + now.toLong() - last).toInt()
                if (grown != art) VarPlayerIntMapSetter.set(player, TREE, grown)
            }
            if (now > last) VarPlayerIntMapSetter.set(player, CLOCK, now)
        }
        if (player.vars[TREE] in EMPTY until FULL) {
            player.softTimer(TIMER, FarmingClock.ticksUntilNext(player, REGROW_MINUTES, millis))
        }
    }

    internal fun pick(player: Player, millis: Long = System.currentTimeMillis()): Boolean {
        refresh(player, millis)
        if (!available(player)) return false
        val art = player.vars[TREE]
        if (art !in EMPTY + 1..FULL) return false
        val award = player.invTransaction(player.inv) {
            add(select(player.inv), FRUIT.asRSCM(RSCMType.OBJ), 1)
        }
        if (!award.success) return false
        VarPlayerIntMapSetter.set(player, TREE, art - 1)
        player.statAdvance(STAT_FARMING, HARVEST_XP * xpMods.get(player, STAT_FARMING))
        player.softTimer(TIMER, FarmingClock.ticksUntilNext(player, REGROW_MINUTES, millis))
        eventBus.publish(FarmingWhiteTreeHarvestEvent(player))
        return true
    }

    internal companion object {
        const val TREE = "varbit.garden_white_tree_varbit"
        const val CLOCK = "varp.farming_white_tree_clock"
        const val TIMER = "timer.farming_white_tree"
        const val QUEST = "quest_gardenoftranquillity"
        const val QUEST_PROGRESS = "varbit.garden_quest"
        const val FRUIT = "obj.garden_white_tree_fruit"
        private const val EMPTY = 8
        private const val FULL = 12
        private const val REGROW_MINUTES = 5
        private const val HARVEST_XP = 12.0
    }
}

internal class FarmingWhiteTreeScript @Inject constructor(private val tree: FarmingWhiteTree) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerLogin { tree.refresh(player) }
        onPlayerSoftTimer(FarmingWhiteTree.TIMER) { tree.refresh(player) }
        for (fruit in 1..4) {
            onOpLoc1("loc.garden_white_tree_fruit_$fruit") {
                faceLoc(it.loc)
                if (!tree.pick(player) && tree.available(player) &&
                    player.vars[FarmingWhiteTree.TREE] in 9..12
                ) {
                    mes("You don't have enough inventory space to hold any more fruit.")
                }
            }
        }
    }
}
