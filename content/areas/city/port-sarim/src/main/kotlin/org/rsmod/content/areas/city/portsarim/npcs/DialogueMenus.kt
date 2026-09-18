package org.rsmod.content.areas.city.portsarim.npcs

import org.rsmod.api.player.dialogue.Dialogue

internal suspend fun <T> Dialogue.menu(options: List<Pair<String, T>>): T {
    val o = options
    return when (o.size) {
        2 -> choice2(o[0].first, o[0].second, o[1].first, o[1].second)
        3 -> choice3(o[0].first, o[0].second, o[1].first, o[1].second, o[2].first, o[2].second)
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
            )
        else -> error("Unsupported menu size: ${o.size}")
    }
}
