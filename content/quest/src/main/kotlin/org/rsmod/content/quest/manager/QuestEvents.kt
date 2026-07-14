package org.rsmod.content.quest.manager

import dev.openrune.definition.type.widget.IfEvent
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.table.QuestRow
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class QuestEvents : PluginScript() {

    private var Player.questTotalCount by intVarBit("varbit.quests_total_count")
    private var Player.questPointMax by intVarBit("varbit.qp_max")

    private var questCount: Int = 0
    private var questPointCap: Int = 0

    override fun ScriptContext.startup() {
        val rows = QuestRow.all()
        questCount = rows.size
        questPointCap = rows.sumOf { it.questpoints }

        onPlayerLogin {
            player.questTotalCount = questCount
            player.questPointMax = questPointCap

            player.ifSetEvents(
                "component.questjournal_overview:content_inner",
                0..23,
                IfEvent.Op1,
                IfEvent.Op2,
                IfEvent.Op3,
                IfEvent.Op4,
            )

            player.ifSetEvents(
                "component.questlist:list",
                0..questCount,
                IfEvent.Op1,
                IfEvent.Op2,
                IfEvent.Op3,
                IfEvent.Op4,
            )
        }
    }
}
