package org.alter.plugins.content.magic

import org.alter.game.model.Graphic

/**
 * @author Tom <rspsmods@gmail.com>
 */
enum class TeleportType(
    val teleportDelay: Int,
    val animation: String,
    val endAnimation: String? = null,
    val graphic: Graphic? = null,
    val endGraphic: Graphic? = null,
    val wildLvlRestriction: Int = 20,
) {
    MODERN(teleportDelay = 4, animation = "sequences.human_castteleport", graphic = Graphic("spotanims.teleport_casting", 92)),
    GLORY(teleportDelay = 4, animation = "sequences.human_castteleport", graphic = Graphic("spotanims.teleport_casting", 92), wildLvlRestriction = 30),
    ANCIENT(teleportDelay = 5, animation = "sequences.zaros_vertical_casting", graphic = Graphic("spotanims.zaros_teleport", 0)),
    LUNAR(teleportDelay = 4, animation = "sequences.human_teleport_other_impact", graphic = Graphic("spotanims.lunar_teleport_spotanim", 120)),
    ARCEUUS(teleportDelay = 4, animation = "sequences.human_teleport_other_impact", graphic = Graphic("spotanims.lunar_teleport_spotanim", 120)),
}
