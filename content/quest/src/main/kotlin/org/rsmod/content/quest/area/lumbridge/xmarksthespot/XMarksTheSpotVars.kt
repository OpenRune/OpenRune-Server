package org.rsmod.content.quest.area.lumbridge.xmarksthespot

import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player

var Player.veosLumbridgeVis: Int by intVarBit("varbit.veos_lumbridge_vis")
var Player.veosSarimVis: Int by intVarBit("varbit.veos_sarim_vis")
var Player.orbPreviousDistance: Int by intVarBit("varbit.cluequest_prev_distance")
var Player.lampUsed: Boolean by boolVarBit("varbit.cluequest_lamp_reward")
var Player.scrollBoxOwed: Boolean by boolVarBit("varbit.cluequest_clue_reward")
var Player.metVeosInKourend: Boolean by boolVarBit("varbit.cluequest_veos_already_met")

fun Player.owns(obj: String): Boolean =
    inv.count(obj) > 0 || (invMap["inv.bank"]?.count(obj) ?: 0) > 0
