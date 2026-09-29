package org.rsmod.content.other.pets.cats

import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player

internal var Player.catGrowthEvents: Int by intVarBit("varbit.cat_growth_events")
internal var Player.catGrowthTicks: Int by intVarBit("varbit.cat_growth_ticks")
var Player.catRatsCaught: Int by intVarBit("varbit.cat_rats_caught")
internal var Player.catOriginalColour: Int by intVarBit("varbit.cat_original_colour")
internal var Player.catStrokes: Int by intVarBit("varbit.cat_strokes")
internal var Player.catHunger: Int by intVarBit("varbit.cat_hunger")
internal var Player.catAttention: Int by intVarBit("varbit.cat_attention")
var Player.catMedalGiven: Boolean by boolVarBit("varbit.cat_medal_given")
