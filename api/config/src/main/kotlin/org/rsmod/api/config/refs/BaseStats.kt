@file:Suppress("SpellCheckingInspection", "unused")

package org.rsmod.api.config.refs

import dev.openrune.stat

typealias stats = BaseStats

object BaseStats {
    val attack = stat("attack")
    val defence = stat("defence")
    val strength = stat("strength")
    val hitpoints = stat("hitpoints")
    val ranged = stat("ranged")
    val prayer = stat("prayer")
    val magic = stat("magic")
    val cooking = stat("cooking")
    val woodcutting = stat("woodcutting")
    val fletching = stat("fletching")
    val fishing = stat("fishing")
    val firemaking = stat("firemaking")
    val crafting = stat("crafting")
    val smithing = stat("smithing")
    val mining = stat("mining")
    val herblore = stat("herblore")
    val agility = stat("agility")
    val thieving = stat("thieving")
    val slayer = stat("slayer")
    val farming = stat("farming")
    val runecrafting = stat("runecrafting")
    val hunter = stat("hunter")
    val construction = stat("construction")
}
