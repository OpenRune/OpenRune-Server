package org.rsmod.content.events.leaguetasks.triggers

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.events.prayer.PrayerSkillAction
import org.rsmod.api.player.events.skilling.SkillingActionCompleteEvent
import org.rsmod.api.player.events.skilling.SkillingActionContext
import org.rsmod.api.player.events.skilling.SkillingProductSource
import org.rsmod.api.script.onEvent
import org.rsmod.content.events.leaguetasks.LeagueTaskIndex
import org.rsmod.content.events.leaguetasks.LeagueTaskProgress
import org.rsmod.content.events.leaguetasks.LeagueTaskRequirement
import org.rsmod.content.events.leaguetasks.LeagueTaskTrigger
import org.rsmod.game.entity.Player
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SkillingTriggers @Inject constructor(private val progress: LeagueTaskProgress) :
    PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    private val skilling = progress.perLeague(::SkillingRows)

    /** A pickpocket or stall theft counts once, whatever and however much it gave. */
    private fun SkillingRows.thieving(player: Player, source: SkillingProductSource, item: Int) {
        when (source) {
            is SkillingProductSource.Pickpocketing -> {
                val npc = source.npc
                val rows =
                    listOf(npc.type.id, npc.visType.id).flatMap { pickpocketByNpc[it].orEmpty() }
                record(player, rows.distinct())
            }
            is SkillingProductSource.StallThieving -> {
                val rows = stealByStall[source.stall.id].orEmpty()
                record(player, rows.filter { it.objs.isEmpty() || item in it.objs })
            }
            else -> Unit
        }
    }

    override fun ScriptContext.startup() {
        onEvent<SkillingActionCompleteEvent> {
            val rows = skilling[player] ?: return@onEvent
            when (val context = context) {
                is SkillingActionContext.Product -> {
                    val item = context.item.asRSCM(RSCMType.OBJ)
                    val tool = context.source.tool?.id
                    val made =
                        rows.product[item].filter { row ->
                            (row.stats.isEmpty() || context.skill in row.stats) &&
                                (row.tools.isEmpty() || tool in row.tools)
                        }
                    record(player, made, context.count)
                    rows.thieving(player, context.source, item)
                }
                is SkillingActionContext.SpellCast -> record(player, rows.spell[context.spell.id])
                is SkillingActionContext.Alchemy -> {
                    val met = rows.alchemy[context.item.id].filter { context.coins >= it.value }
                    record(player, met)
                }
                is SkillingActionContext.Firemaking ->
                    record(player, rows.firemaking[context.log.asRSCM(RSCMType.OBJ)])
                is SkillingActionContext.Consumed -> record(player, rows.consume[context.item.id])
                is SkillingActionContext.Prayer -> rows.prayer(player, context.action)
            }
        }
    }

    private fun SkillingRows.prayer(player: Player, action: PrayerSkillAction) {
        when (action) {
            is PrayerSkillAction.BuryComplete ->
                if (!action.ashes) {
                    record(player, bury[action.itemInternal.asRSCM(RSCMType.OBJ)])
                }
            is PrayerSkillAction.Offered ->
                record(player, offer[action.itemInternal.asRSCM(RSCMType.OBJ)], action.count)
            is PrayerSkillAction.BonecrusherCrushComplete -> Unit
        }
    }

    private fun record(player: Player, rows: List<LeagueTaskRequirement>, amount: Int = 1) =
        progress.record(player, rows, amount)

    private class SkillingRows(index: LeagueTaskIndex) {
        val product = ObjRows(index, LeagueTaskTrigger.Product)
        val spell = ObjRows(index, LeagueTaskTrigger.Spell)
        val alchemy = ObjRows(index, LeagueTaskTrigger.Alchemy)
        val firemaking = ObjRows(index, LeagueTaskTrigger.Firemaking)
        val bury = ObjRows(index, LeagueTaskTrigger.Bury)
        val offer = ObjRows(index, LeagueTaskTrigger.Offer)
        val consume = ObjRows(index, LeagueTaskTrigger.Consume)
        val pickpocketByNpc = index.rowsBy(LeagueTaskTrigger.Pickpocket) { it.npcs }
        val stealByStall = index.rowsBy(LeagueTaskTrigger.Steal) { it.locs }
    }

    private class ObjRows(index: LeagueTaskIndex, trigger: LeagueTaskTrigger) {
        private val byObj = index.rowsByObj(trigger)
        private val anyObj = index.rowsForAnyObj(trigger)

        operator fun get(obj: Int): List<LeagueTaskRequirement> = byObj[obj].orEmpty() + anyObj
    }
}
