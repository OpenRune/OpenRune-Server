package org.rsmod.content.generic.npcs.ducks

import dev.openrune.hunt
import dev.openrune.npc

typealias duck_npcs = DuckNpcs

typealias duck_hunt = DuckHunt

object DuckNpcs {
    val duck = npc("duck")
    val duck_female = npc("duck_female")
    val duckling = npc("duck_update_ducklings")
}

object DuckHunt {
    val duckling = hunt("duck_hunt")
}
