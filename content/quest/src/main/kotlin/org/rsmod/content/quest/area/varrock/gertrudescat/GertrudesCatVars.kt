package org.rsmod.content.quest.area.varrock.gertrudescat

import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player

var Player.fluffsKittenCrate: Int by intVarBit("varbit.gertrudes_cat_kitten_crate")
var Player.metFluffs: Boolean by boolVarBit("varbit.gertrudes_cat_met_fluffs")
var Player.toldAboutSardines: Boolean by boolVarBit("varbit.gertrudes_cat_told_sardines")
