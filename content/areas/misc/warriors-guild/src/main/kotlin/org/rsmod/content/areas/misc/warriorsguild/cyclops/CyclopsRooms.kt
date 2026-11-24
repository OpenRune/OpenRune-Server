package org.rsmod.content.areas.misc.warriorsguild.cyclops

import dev.openrune.ServerCacheManager
import org.rsmod.api.player.back
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

internal object CyclopsRooms {
    const val NONE = 0
    const val TOP = 1
    const val BASEMENT = 2

    const val ENTRY_TOKENS = 100
    const val TOKENS_PER_MINUTE = 10
    const val MINUTE_TICKS = 100
    const val TIMER = "timer.warguild_cyclops"

    const val TOP_AREA = "area.warguild_cyclops_top"
    const val BASEMENT_AREA = "area.warguild_cyclops_basement"

    const val KAMFREENA = "npc.warguild_kamfreena"
    const val LORELAI = "npc.warguild_lorelai"

    val TOP_EXIT: CoordGrid = CoordGrid(2846, 3540, 2)
    val BASEMENT_EXIT: CoordGrid = CoordGrid(2911, 9968, 0)

    private val ATTACK_CAPES = setOf("obj.skillcape_attack", "obj.skillcape_attack_trimmed")

    fun Player.wearsAttackCape(): Boolean {
        if (ATTACK_CAPES.any { it in worn }) return true
        val cape = back ?: return false
        return ServerCacheManager.getItem(cape.id)?.isContentType("content.max_cape") == true
    }

    fun area(room: Int): String = if (room == TOP) TOP_AREA else BASEMENT_AREA

    fun exit(room: Int): CoordGrid = if (room == TOP) TOP_EXIT else BASEMENT_EXIT

    fun keeper(room: Int): String = if (room == TOP) KAMFREENA else LORELAI

    fun keeperName(room: Int): String = if (room == TOP) "Kamfreena" else "Lorelai"
}
