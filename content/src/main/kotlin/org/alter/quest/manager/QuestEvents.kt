package org.alter.quest.manager

import dev.openrune.definition.type.widget.IfEvent
import org.alter.api.ext.getVarp
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ButtonClickEvent
import org.alter.game.pluginnew.event.impl.onLogin
import org.alter.interfaces.ifSetEvents
import org.alter.rscm.RSCM.asRSCM
import org.generated.tables.QuestRow

class QuestEvents : PluginEvent() {
    override fun init() {
        onLogin {

            player.ifSetEvents(
                "components.questjournal_overview:content_inner",
                0..23,
                IfEvent.Op1,
                IfEvent.Op2,
                IfEvent.Op3,
                IfEvent.Op4
            )


            player.ifSetEvents(
                "components.questlist:list",
                0..QuestRow.all().size,
                IfEvent.Op1,
                IfEvent.Op2,
                IfEvent.Op3,
                IfEvent.Op4
            )
        }
    }

}