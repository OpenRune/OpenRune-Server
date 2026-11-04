package org.rsmod.content.minigames.motherlodemine.configs

import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player

var Player.motherlodeUpperFloorUnlocked by boolVarBit("varbit.motherlode_upper_floor_unlocked")
var Player.motherlodeSuperHopperUnlocked by boolVarBit("varbit.motherlode_super_hopper_unlocked")

internal var Player.motherlodeLastSackNotify by intVarp("varp.motherlode_last_sack_notify")

private val LegacyUpperFloor = AttributeKey<Boolean>(persistenceKey = "motherlode_upper_floor_unlocked")
private val LegacySuperHopper = AttributeKey<Boolean>(persistenceKey = "motherlode_super_hopper_unlocked")

internal fun Player.migrateMotherlodeUnlocks() {
    if (attr[LegacyUpperFloor] == true) motherlodeUpperFloorUnlocked = true
    if (attr[LegacySuperHopper] == true) motherlodeSuperHopperUnlocked = true
    if (attr.has(LegacyUpperFloor)) attr.remove(LegacyUpperFloor)
    if (attr.has(LegacySuperHopper)) attr.remove(LegacySuperHopper)
}
