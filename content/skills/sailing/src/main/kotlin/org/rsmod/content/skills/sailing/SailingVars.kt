package org.rsmod.content.skills.sailing

import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player

internal var Player.aboardPlayerBoat by intVarBit("varbit.sailing_player_is_on_player_boat")

internal var Player.helmLockedIn by intVarBit("varbit.sailing_boat_facility_lockedin")
