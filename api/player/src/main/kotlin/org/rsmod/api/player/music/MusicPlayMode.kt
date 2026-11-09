package org.rsmod.api.player.music

import org.rsmod.api.utils.vars.VarEnumDelegate

public enum class MusicPlayMode(override val varValue: Int) : VarEnumDelegate {
    Area(0),
    Random(1),
    Manual(2),
}
