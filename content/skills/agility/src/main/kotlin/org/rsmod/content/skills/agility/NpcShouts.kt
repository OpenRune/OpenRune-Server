package org.rsmod.content.skills.agility

import dev.openrune.types.enums.enum
import dev.openrune.types.hunt.HuntVis
import jakarta.inject.Inject
import org.rsmod.api.hunt.NpcSearch
import org.rsmod.api.random.GameRandom
import org.rsmod.map.CoordGrid

class NpcShouts @Inject constructor(private val search: NpcSearch, private val random: GameRandom) {
    internal fun say(near: CoordGrid, npc: String, lines: Int, within: Int) {
        val speaker = search.find(near, npc, within, HuntVis.Off) ?: return
        speaker.say(random.pick(lines(lines)))
    }

    internal fun lines(enum: Int): List<String> =
        enum<Int, String>(enum).backing.values.filterNotNull()
}
