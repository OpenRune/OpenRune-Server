package org.rsmod.content.other.bots

import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocInfo
import org.rsmod.map.CoordGrid

enum class BotActivity(val skill: String? = null, val workXp: Int = 0) {
    WOODCUTTING("stat.woodcutting", 25),
    MINING("stat.mining", 35),
    FISHING("stat.fishing", 30),
    QUESTING,
    WANDERING,
    IDLING,
}

enum class BotPhase {
    RESTING,
    WALKING,
    WORKING,
}

class Bot(
    val player: Player,
    val activity: BotActivity,
    val home: CoordGrid,
    val radius: Int,
    val spot: CoordGrid,
    val bank: CoordGrid?,
    val runs: Boolean,
    val hotspot: Int,
    val style: String,
) {
    var phase: BotPhase = BotPhase.RESTING
    var phaseTicks: Int = 0
    var nextAnimCycle: Int = 0
    var workAnim: String? = null
    var targetLoc: LocInfo? = null
    var targetNpc: Npc? = null
    var workTarget: CoordGrid = CoordGrid.NULL
    var claim: CoordGrid? = null
    var tripsSinceBank: Int = 0
    var onErrand: Boolean = false
    internal var levelUpReply: BotLevelUpReply? = null
}

internal data class BotLevelUpReply(val source: Bot, val cycle: Int)
