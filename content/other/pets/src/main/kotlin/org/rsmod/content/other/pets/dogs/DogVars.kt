package org.rsmod.content.other.pets.dogs

import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player

internal var Player.dogGrowthEvents: Int by intVarBit("varbit.dog_growth_events")
internal var Player.dogGrowthTicks: Int by intVarBit("varbit.dog_growth_ticks")
internal var Player.dogFedTicks: Int by intVarBit("varbit.dog_fed_ticks")
