package org.rsmod.content.areas.misc.warriorsguild

import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player

internal var Player.tokensOwed by intVarp("varp.warguild_tokens_owed")
internal var Player.kegTokensOwed by intVarp("varp.warguild_keg_tokens_owed")

internal var Player.defenderEarned by intVarBit("varbit.warguild_defender_earned")

internal var Player.lostArmour by intVarBit("varbit.warguild_lost_armour")

internal var Player.cyclopsRoom by intVarBit("varbit.warguild_cyclops_room")
internal var Player.cyclopsTier by intVarBit("varbit.warguild_cyclops_tier")
internal var Player.cyclopsGrace by boolVarBit("varbit.warguild_cyclops_grace")
internal var Player.cyclopsFree by boolVarBit("varbit.warguild_cyclops_free")
internal var Player.basementUnlocked by boolVarBit("varbit.warguild_basement_unlocked")

internal var Player.shotDusted by boolVarBit("varbit.warguild_shot_dusted")
internal var Player.kegTicks by intVarBit("varbit.warguild_keg_ticks")
internal var Player.catapultStance by intVarBit("varbit.warguild_defence_type")
internal var Player.dummyRound by intVarp("varp.warguild_dummy_round")
