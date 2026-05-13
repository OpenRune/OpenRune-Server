package org.rsmod.content.interfaces.display.name

import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player

internal var Player.displayNameStatus: Int by intVarBit("varbit.displayname_status")
internal var Player.displayNamePermitChange: Boolean by boolVarBit("varbit.displayname_permitchange")
internal var Player.displayNameChangedThisSession: Boolean by boolVarBit("varbit.displayname_changedthissession")
