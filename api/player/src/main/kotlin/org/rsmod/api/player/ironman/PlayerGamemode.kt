package org.rsmod.api.player.ironman

import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player

public object PlayerGamemode {
    public const val NORMAL: Int = 0
    public const val IRONMAN: Int = 1
    public const val ULTIMATE_IRONMAN: Int = 2
    public const val HARDCORE_IRONMAN: Int = 3
    public const val GROUP_IRONMAN: Int = 4
    public const val HARDCORE_GROUP_IRONMAN: Int = 5
    public const val UNRANKED_GROUP_IRONMAN: Int = 6
}

private var Player.ironmanVarbit by intVarBit("varbit.ironman")

public val Player.isSoloIronman: Boolean
    get() =
        when (gamemode) {
            PlayerGamemode.IRONMAN,
            PlayerGamemode.HARDCORE_IRONMAN,
            PlayerGamemode.ULTIMATE_IRONMAN -> true
            else -> false
        }

public val Player.isHardcoreIronman: Boolean
    get() = gamemode == PlayerGamemode.HARDCORE_IRONMAN

public val Player.isUltimateIronman: Boolean
    get() = gamemode == PlayerGamemode.ULTIMATE_IRONMAN

public val Player.isAnyIronman: Boolean
    get() = gamemode != PlayerGamemode.NORMAL

public fun Player.setGamemode(mode: Int) {
    gamemode = mode
    syncIronmanVarbit()
}

public fun Player.syncIronmanVarbit() {
    ironmanVarbit = gamemode.coerceIn(PlayerGamemode.NORMAL, PlayerGamemode.UNRANKED_GROUP_IRONMAN)
}

public fun Player.syncGamemodeFromVarbit() {
    gamemode = ironmanVarbit.coerceIn(PlayerGamemode.NORMAL, PlayerGamemode.UNRANKED_GROUP_IRONMAN)
}
