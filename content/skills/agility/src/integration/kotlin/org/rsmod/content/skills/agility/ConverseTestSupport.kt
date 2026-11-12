package org.rsmod.content.skills.agility

import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.testing.scope.GameTestScope

private const val MAX_STEPS = 120
private const val IDLE_STEPS = 3
private const val CHAT_MENU = "interface.chatmenu"

private val CONTINUES =
    mapOf(
        "interface.chat_left" to "component.chat_left:continue",
        "interface.chat_right" to "component.chat_right:continue",
        "interface.objectbox" to "component.objectbox:universe",
        "interface.messagebox" to "component.messagebox:continue",
    )

/**
 * Plays out the interaction just queued: continues every chat box and mesbox, answers each chat
 * menu with the next of [options], and returns the game messages sent until the player has been
 * idle for a few ticks.
 */
internal fun GameTestScope.converse(vararg options: Int): List<String> {
    val messages = mutableListOf<String>()
    val choices = options.iterator()
    var seen = 0
    fun collect() {
        val sent = gameMessages()
        messages += sent.drop(seen)
        seen = sent.size
    }
    var idle = 0
    repeat(MAX_STEPS) {
        val coroutine = player.activeCoroutine
        if (coroutine == null || !coroutine.isSuspended) {
            if (idle++ == IDLE_STEPS) {
                return messages
            }
            advance()
            seen = 0
            collect()
            return@repeat
        }
        idle = 0
        if (!coroutine.isAwaiting(ResumePauseButtonInput::class)) {
            advance()
            seen = 0
            collect()
            return@repeat
        }
        val input =
            if (player.ui.containsModal(CHAT_MENU)) {
                check(choices.hasNext()) { "Out of options at a menu: $messages" }
                ResumePauseButtonInput("component.chatmenu:options", choices.next())
            } else {
                val modal = CONTINUES.keys.first { player.ui.containsModal(it) }
                ResumePauseButtonInput(CONTINUES.getValue(modal), -1)
            }
        coroutine.resumeWith(input)
        collect()
    }
    error("The interaction did not end: $messages")
}
