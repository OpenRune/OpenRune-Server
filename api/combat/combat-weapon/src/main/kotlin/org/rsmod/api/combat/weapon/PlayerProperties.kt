package org.rsmod.api.combat.weapon

import dev.openrune.util.Wearpos
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

internal val Player.righthand: InvObj?
    get() = worn[Wearpos.RightHand.slot]
