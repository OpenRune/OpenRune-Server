package org.rsmod.content.events.leaguetasks.triggers

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.events.prayer.PrayerSkillAction
import org.rsmod.api.player.events.skilling.SkillingActionCompleteEvent
import org.rsmod.api.player.events.skilling.SkillingActionContext
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

    override fun ScriptContext.startup() {
        onEvent<SkillingActionCompleteEvent> {
            when (val context = context) {
                is SkillingActionContext.Product -> {
                    val rows = product[context.item.asRSCM(RSCMType.OBJ)]
                    val made = rows.filter { it.stats.isEmpty() || context.skill in it.stats }
                    record(player, made, context.count)
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

    private fun record(player: Player, rows: List<LeagueTaskRequirement>, amount: Int = 1) {
        for (row in rows) {
            progress.record(player, row, amount)
        }
    }

    /** One kind's rows by the obj they name, plus the rows that take any obj. */
    private inner class ObjRows(trigger: LeagueTaskTrigger) {
        private val byObj = progress.rowsByObj(trigger)
        private val anyObj = progress.rowsForAnyObj(trigger)

        operator fun get(obj: Int): List<LeagueTaskRequirement> = byObj[obj].orEmpty() + anyObj
    }
}
