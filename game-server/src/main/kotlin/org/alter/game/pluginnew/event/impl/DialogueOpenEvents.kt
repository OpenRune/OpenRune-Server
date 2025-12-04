package org.alter.game.pluginnew.event.impl

import net.rsprot.protocol.util.CombinedId
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.event.PlayerEvent

class DialogPlayerOpen(
    val message : String,
    val animation: String,
    val title : String,
    player: Player
) : PlayerEvent(player)

class DialogNpcOpen(
    val message : String,
    val npc : Int,
    val animation: String,
    val title : String,
    player: Player
) : PlayerEvent(player)

class DialogMessageOpen(
    val message : String,
    val continues : Boolean,
    player: Player
) : PlayerEvent(player)

class DialogMessageOption(
    val title : String,
    val options : String,
    player: Player
) : PlayerEvent(player)

class DialogItem(
    val message : String,
    val item : Int,
    val zoom : Int,
    player: Player
) : PlayerEvent(player)

class DialogItemDouble(
    val message : String,
    val item1 : Int,
    val item2 : Int,
    val zoom1 : Int,
    val zoom2 : Int,
    player: Player
) : PlayerEvent(player)

class DialogSkillMulti(
    player: Player
) : PlayerEvent(player)