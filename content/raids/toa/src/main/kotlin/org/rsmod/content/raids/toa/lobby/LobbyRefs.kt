package org.rsmod.content.raids.toa.lobby

internal object LobbyLocs {
    const val SACK = "loc.toa_sack_full"
    const val SHROUD_CHEST = "loc.toa_lobby_cape_chest"
}

internal object LobbyNpcs {
    const val BANK_CAMEL = "npc.toa_bank_camel"
}

internal object LobbyObjs {
    const val CAMULET = "obj.camulet"
    const val NEEDLE = "obj.needle"
    const val GRAIN = "obj.toa_grain"
}

internal object LobbySack {
    const val STAT = "stat.thieving"
    const val LOW = 1
    const val HIGH = 10
    const val CAMEL_TITLE = "Bank Camel"
}

internal class ShroudReward(val obj: String, val completions: Int, val option: String)

internal object LobbyShrouds {
    val REWARDS =
        listOf(
            ShroudReward("obj.icthlarins_shroud_1", 100, "Take Icthlarin's shroud (tier 1)."),
            ShroudReward("obj.icthlarins_shroud_2", 500, "Take Icthlarin's shroud (tier 2)."),
            ShroudReward("obj.icthlarins_shroud_3", 1000, "Take Icthlarin's shroud (tier 3)."),
            ShroudReward("obj.icthlarins_shroud_4", 1500, "Take Icthlarin's shroud (tier 4)."),
            ShroudReward("obj.icthlarins_shroud_5", 2000, "Take Icthlarin's shroud (tier 5)."),
            ShroudReward("obj.icthlarins_hood", 2000, "Take Icthlarin's hood (tier 5)."),
        )
    const val MAX_CHOICES = 5
    const val MORE = "More..."
    const val TAKEN = "You take a mysterious shroud from the chest."
    const val NONE_UNLOCKED = "There doesn't seem to be anything inside."
    const val NO_SPACE = "You don't have enough inventory space."
}
