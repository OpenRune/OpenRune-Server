package org.rsmod.api.combat.manager

import dev.openrune.types.VarObjBitType
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.player.events.ResourceKind
import org.rsmod.api.player.events.attackRecord
import org.rsmod.api.player.righthand
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getOrNull

public class CombatChargeManager
@Inject
constructor(private val manager: ObjChargeManager, private val random: GameRandom) {
    /**
     * Returns the number of charges on the player's weapon.
     *
     * Internally calls [ObjChargeManager.getCharges] with `obj = player.righthand`.
     */
    public fun getWeaponCharges(player: Player, varobj: String): Int =
        manager.getCharges(player.righthand, varobj)

    /**
     * Attempts to reduce the charges on the player's weapon by [decrement].
     *
     * Internally calls [ObjChargeManager.reduceWornCharges] with `wearpos = Wearpos.RightHand`.
     */
    public fun attemptDetractWeapon(
        player: Player,
        varobj: String,
        decrement: Int = 1,
    ): ObjChargeManager.Uncharge {
        val record = player.attackRecord
        val weapon = getOrNull(player.righthand)
        val charges = getWeaponCharges(player, varobj)
        if (charges >= decrement && record?.rollSave(random) == true) {
            record.addResource(ResourceKind.Charge, weapon, decrement, true)
            return ObjChargeManager.Uncharge.Success(chargesLeft = charges)
        }
        val result = manager.reduceWornCharges(player, Wearpos.RightHand, varobj, decrement)
        if (result is ObjChargeManager.Uncharge.Success) {
            record?.addResource(ResourceKind.Charge, weapon, decrement, false)
        }
        return result
    }
}
