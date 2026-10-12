package org.rsmod.content.areas.city.rellekka

import org.rsmod.api.player.dialogue.Dialogue

internal suspend fun <T> Dialogue.choose(options: List<Pair<String, T>>): T =
    when (options.size) {
        1 -> options[0].second
        2 -> choice2(options[0].first, options[0].second, options[1].first, options[1].second)
        3 ->
            choice3(
                options[0].first,
                options[0].second,
                options[1].first,
                options[1].second,
                options[2].first,
                options[2].second,
            )
        4 ->
            choice4(
                options[0].first,
                options[0].second,
                options[1].first,
                options[1].second,
                options[2].first,
                options[2].second,
                options[3].first,
                options[3].second,
            )
        5 ->
            choice5(
                options[0].first,
                options[0].second,
                options[1].first,
                options[1].second,
                options[2].first,
                options[2].second,
                options[3].first,
                options[3].second,
                options[4].first,
                options[4].second,
            )
        else -> error("Expected 1-5 options, got ${options.size}")
    }

internal fun Dialogue.brotherOrSister(): String = if (access.isBodyTypeA()) "brother" else "sister"
