package org.rsmod.content.quest.manager

import org.rsmod.api.config.Constants
import org.rsmod.api.player.dialogue.Dialogue

suspend fun <T> Dialogue.menu(
    vararg options: Pair<String, T>,
    title: String = Constants.cm_options,
): T = menu(options.toList(), title)

suspend fun <T> Dialogue.menu(
    options: List<Pair<String, T>>,
    title: String = Constants.cm_options,
): T {
    val o = options
    return when (o.size) {
        1 -> o[0].second
        2 -> choice2(o[0].first, o[0].second, o[1].first, o[1].second, title)
        3 -> choice3(o[0].first, o[0].second, o[1].first, o[1].second, o[2].first, o[2].second, title)
        4 ->
            choice4(
                o[0].first,
                o[0].second,
                o[1].first,
                o[1].second,
                o[2].first,
                o[2].second,
                o[3].first,
                o[3].second,
                title,
            )
        5 ->
            choice5(
                o[0].first,
                o[0].second,
                o[1].first,
                o[1].second,
                o[2].first,
                o[2].second,
                o[3].first,
                o[3].second,
                o[4].first,
                o[4].second,
                title,
            )
        else -> error("Unsupported menu size: ${o.size}")
    }
}

suspend fun Dialogue.startQuestPrompt(quest: Quest): Boolean =
    choice2("Yes.", true, "No.", false, title = "Start the ${quest.displayName} quest?")
