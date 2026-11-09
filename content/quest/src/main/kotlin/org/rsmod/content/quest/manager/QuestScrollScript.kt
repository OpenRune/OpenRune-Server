package org.rsmod.content.quest.manager

import jakarta.inject.Inject
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.ifCloseChat
import org.rsmod.api.player.ui.ifCloseSub
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.events.EventBus
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private typealias ScrollContinuation = suspend ProtectedAccess.() -> Unit

private val SCROLL_CONTINUATION = AttributeKey<ScrollContinuation>(temp = true)

private const val SCROLL_CONTINUE_QUEUE = "queue.quest_scroll_continue"

/**
 * Runs [block] once the player closes the open quest scroll with its close button. Closing it any
 * other way (Escape, walking off) drops [block].
 */
fun ProtectedAccess.afterQuestScroll(block: ScrollContinuation) {
    player.attr[SCROLL_CONTINUATION] = block
}

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
        onIfModalButton("component.questscroll:close_button") { closeScroll() }
        onIfClose("interface.questscroll") { player.attr.remove(SCROLL_CONTINUATION) }
        onPlayerQueueWithArgs<ScrollContinuation>(SCROLL_CONTINUE_QUEUE) { it.args(this) }
    }

    private fun ProtectedAccess.closeScroll() {
        val continuation = player.attr[SCROLL_CONTINUATION]
        player.attr.remove(SCROLL_CONTINUATION)
        ifCloseSub("interface.questscroll")
        // The client sends close_modal with this click, and the engine handles it after the
        // button, so the continuation runs from a queue that the modal close leaves alone.
        if (continuation != null) {
            queue(SCROLL_CONTINUE_QUEUE, 1, continuation)
        }
    }
}
