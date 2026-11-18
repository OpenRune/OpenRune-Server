package org.rsmod.content.areas.misc.fossilunderwater

import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player

internal var Player.oxygen by intVarBit("varbit.fossil_underwater_oxygen")
internal var Player.bowlOnPile by boolVarBit("varbit.fossil_underwater_bowl")
internal var Player.pufferfishSeen by intVarBit("varbit.fossil_puffer_seefish")
internal var Player.foundOpen by boolVarBit("varbit.fossil_underwater_found")
internal var Player.foundWindow by boolVarBit("varbit.fossil_underwater_found_window")
internal var Player.searchedWindow by intVarBit("varbit.fossil_underwater_window")
internal var Player.drowning by boolVarBit("varbit.fossil_underwater_drowning")
internal var Player.inCurrent by boolVarBit("varbit.fossil_underwater_in_current")
