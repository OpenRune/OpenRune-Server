package org.rsmod.content.other.bots

import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PublicMessage

object BotChat {
    val congratulations = listOf("gz", "grtz", "grats", "congrats!", "congratulations!", "nice lvl", "nice!")

    /**
     * Queues public chat through the bot's avatar so nearby clients can see it.
     *
     * As with other avatar updates, call this before the bot's client cycle updates.
     */
    fun say(player: Player, text: String) {
        player.publicMessage = PublicMessage(text, 0, 0, null, 0, false, null)
    }

    private val GENERAL =
        listOf(
            "anyone know a good money maker?",
            "nice weather today",
            "lol",
            "brb",
            "gl all",
            "hi",
            "how long have you been playing?",
            "this takes forever",
        )

    private val SKILLING =
        GENERAL +
            listOf(
                "so close to the next level",
                "finally got the level",
                "these xp rates though",
                "one more inventory then i'm done",
                "zzz",
            )

    private val IDLING =
        GENERAL +
            listOf(
                "selling stuff, pm me",
                "anyone want to duo?",
                "waiting for a friend",
                "where is everyone training?",
                "afk",
            )

    private val QUESTING =
        GENERAL +
            listOf(
                "where do i go next for this quest?",
                "anyone done this quest before?",
                "need to find some npc around here",
                "almost got the quest cape",
                "this quest is so long",
            )

    fun linesFor(activity: BotActivity): List<String> =
        when (activity) {
            BotActivity.WOODCUTTING,
            BotActivity.MINING,
            BotActivity.FISHING -> SKILLING
            BotActivity.QUESTING -> QUESTING
            BotActivity.IDLING -> IDLING
            BotActivity.WANDERING -> GENERAL
        }
}
