package org.rsmod.content.bosses.araxxor

import org.rsmod.api.player.stat.stat
import org.rsmod.api.table.slayer.SlayerTaskRow
import org.rsmod.game.entity.Player

internal object AraxxorAccess {
    private val tasks by lazy {
        SlayerTaskRow.all().filter { it.nameLowercase in setOf("araxytes", "spiders") }.map { it.id }.toSet()
    }
    fun task(player: Player): Int? = player.vars["varp.slayer_target"].takeIf {
        it in tasks && player.vars["varp.slayer_count"] > 0
    }
    fun allowed(player: Player): Boolean = player.stat("stat.slayer") >= 92 && task(player) != null
}
