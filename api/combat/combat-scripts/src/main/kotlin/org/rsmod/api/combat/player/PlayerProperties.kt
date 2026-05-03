package org.rsmod.api.combat.player

import org.rsmod.api.combat.commons.CombatStance
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.enumVarBit
import org.rsmod.api.player.vars.enumVarp
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.player.vars.typeNpcUidVarp
import org.rsmod.api.player.vars.typePlayerUidVarp
import org.rsmod.api.specials.SpecialAttackType
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcUid
import org.rsmod.game.entity.player.PlayerUid

internal val ProtectedAccess.spellbook by enumVarBit<Spellbook>("varbit.spellbook")
internal var ProtectedAccess.autocastEnabled by boolVarBit("varbit.autocast_set")
internal val ProtectedAccess.autocastSpell by intVarBit("varbit.autocast_spell")
internal val ProtectedAccess.defensiveCasting by boolVarBit("varbit.autocast_defmode")
internal var ProtectedAccess.combatStance by enumVarp<CombatStance>("varp.com_mode")

internal var ProtectedAccess.aggressiveNpc: NpcUid? by typeNpcUidVarp("varp.aggressive_npc")
internal var ProtectedAccess.pkPrey1: PlayerUid? by typePlayerUidVarp("varp.pk_prey1")
internal var ProtectedAccess.pkPrey2: PlayerUid? by typePlayerUidVarp("varp.pk_prey2")
internal var ProtectedAccess.pkPredator1: PlayerUid? by typePlayerUidVarp("varp.pk_predator1")

internal var ProtectedAccess.specialAttackType by enumVarp<SpecialAttackType>("varp.sa_attack")

internal var Player.lastCombat: Int by intVarp("varp.lastcombat")
internal var Player.lastCombatPvp: Int by intVarp("varp.lastcombat_pvp")
internal var Player.aggressiveNpc: NpcUid? by typeNpcUidVarp("varp.aggressive_npc")
internal var Player.pkPredator1: PlayerUid? by typePlayerUidVarp("varp.pk_predator1")
internal var Player.pkPredator2: PlayerUid? by typePlayerUidVarp("varp.pk_predator2")
internal var Player.pkPredator3: PlayerUid? by typePlayerUidVarp("varp.pk_predator3")
