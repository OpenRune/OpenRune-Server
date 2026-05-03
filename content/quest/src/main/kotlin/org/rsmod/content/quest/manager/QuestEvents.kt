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

    override fun ScriptContext.startup() {
        onPlayerLogin {

            player.questTotalCount = QuestRow.all().size
            player.questPointMax = QuestRow.all().sumOf { it.questpoints }

            player.ifSetEvents(
                "component.questjournal_overview:content_inner",
                0..23,
                IfEvent.Op1,
                IfEvent.Op2,
                IfEvent.Op3,
                IfEvent.Op4
            )


            player.ifSetEvents(
                "component.questlist:list", 0..QuestRow.all().size,
                IfEvent.Op1,
                IfEvent.Op2,
                IfEvent.Op3,
                IfEvent.Op4,
            )
        }
    }

}
