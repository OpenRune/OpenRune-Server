package org.rsmod.api.combat.npc

import org.rsmod.api.npc.vars.intVarn
import org.rsmod.api.npc.vars.typePlayerUidVarn
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.player.PlayerUid

internal var Npc.lastAttack: Int by intVarn("varn.lastattack")
internal var Npc.attackingPlayer: PlayerUid? by typePlayerUidVarn("varn.attacking_player")

internal var Npc.lastCombat: Int by intVarn("varn.lastcombat")
internal var Npc.aggressivePlayer by typePlayerUidVarn("varn.aggressive_player")
