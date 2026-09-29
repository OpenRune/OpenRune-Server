package org.rsmod.content.quest.manager

import jakarta.inject.Inject
import org.rsmod.api.player.ui.ifCloseChat
import org.rsmod.api.player.ui.ifCloseSub
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.events.EventBus
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class QuestScrollScript @Inject constructor(private val eventBus: EventBus) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerSoftTimer(QUEST_DIALOGUE_CLOSE_TIMER) {
            player.clearSoftTimer(QUEST_DIALOGUE_CLOSE_TIMER)
            player.ifCloseChat(eventBus)
        }
        onPlayerSoftTimer(QUEST_SCROLL_CLOSE_TIMER) {
            player.clearSoftTimer(QUEST_SCROLL_CLOSE_TIMER)
            player.ifCloseSub("interface.questscroll", eventBus)
        }
    }
}
