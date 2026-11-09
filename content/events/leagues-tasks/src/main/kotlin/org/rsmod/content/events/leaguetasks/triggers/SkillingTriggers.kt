package org.rsmod.content.events.leaguetasks.triggers

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.events.prayer.PrayerSkillAction
import org.rsmod.api.player.events.skilling.SkillingActionCompleteEvent
import org.rsmod.api.player.events.skilling.SkillingActionContext
import org.rsmod.api.player.events.skilling.SkillingProductSource
import org.rsmod.api.script.onEvent
import org.rsmod.content.events.leaguetasks.LeagueTaskProgress
import org.rsmod.content.events.leaguetasks.LeagueTaskRequirement
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTrigger
import org.rsmod.game.entity.Player
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SkillingTriggers @Inject constructor(private val progress: LeagueTaskProgress) :
    PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    private val product = ObjRows(LeagueTaskTrigger.Product)
    private val spell = ObjRows(LeagueTaskTrigger.Spell)
    private val alchemy = ObjRows(LeagueTaskTrigger.Alchemy)
    private val firemaking = ObjRows(LeagueTaskTrigger.Firemaking)
    private val bury = ObjRows(LeagueTaskTrigger.Bury)
    private val offer = ObjRows(LeagueTaskTrigger.Offer)
    private val consume = ObjRows(LeagueTaskTrigger.Consume)

    private val pickpocketByNpc: Map<Int, List<LeagueTaskRequirement>> =
        progress.rows(LeagueTaskTrigger.Pickpocket).flatMap { row -> row.npcs.map { it to row } }
            .groupBy({ it.first }, { it.second })

    private val stealByStall: Map<Int, List<LeagueTaskRequirement>> =
        progress.rows(LeagueTaskTrigger.Steal).flatMap { row -> row.locs.map { it to row } }
            .groupBy({ it.first }, { it.second })

    /** A pickpocket or stall theft counts once, whatever and however much it gave. */
    private fun thieving(player: Player, source: SkillingProductSource, item: Int) {
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
            when (val context = context) {
                is SkillingActionContext.Product -> {
                    val item = context.item.asRSCM(RSCMType.OBJ)
                    val tool = context.source.tool?.id
                    val made =
                        product[item].filter { row ->
                            (row.stats.isEmpty() || context.skill in row.stats) &&
                                (row.tools.isEmpty() || tool in row.tools)
                        }
                    record(player, made, context.count)
                    thieving(player, context.source, item)
                }
                is SkillingActionContext.SpellCast -> record(player, spell[context.spell.id])
                is SkillingActionContext.Alchemy -> {
                    val rows = alchemy[context.item.id].filter { context.coins >= it.value }
                    record(player, rows)
                }
                is SkillingActionContext.Firemaking ->
                    record(player, firemaking[context.log.asRSCM(RSCMType.OBJ)])
                is SkillingActionContext.Consumed -> record(player, consume[context.item.id])
                is SkillingActionContext.Prayer -> prayer(player, context.action)
            }
        }
    }

    private fun prayer(player: Player, action: PrayerSkillAction) {
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

    private inner class ObjRows(trigger: LeagueTaskTrigger) {
        private val byObj = progress.rowsByObj(trigger)
        private val anyObj = progress.rowsForAnyObj(trigger)

        operator fun get(obj: Int): List<LeagueTaskRequirement> = byObj[obj].orEmpty() + anyObj
    }
}
