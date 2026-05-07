package org.rsmod.api.obj.charges

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.VarBitType
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.VarObjBitType
import dev.openrune.types.varp.bits
import dev.openrune.util.Wearpos
import kotlin.contracts.contract
import kotlin.math.min
import org.rsmod.api.config.refs.params
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.getInvObj
import org.rsmod.utils.bits.bitMask
import org.rsmod.utils.bits.getBits
import org.rsmod.utils.bits.withBits

public class ObjChargeManager {

    private sealed interface ChargeBits {
        val bits: IntRange
    }

    private data class VarObjBits(val type: VarObjBitType) : ChargeBits {
        override val bits: IntRange get() = type.bits
    }

    private data class VarBitBits(val type: VarBitType) : ChargeBits {
        override val bits: IntRange get() = type.bits
    }

    private fun resolveBits(internal: String): ChargeBits {
        return when {
            internal.startsWith("varobj.") -> {
                val type = ServerCacheManager.getVarObj(
                    internal.asRSCM(RSCMType.VAROBJ)
                ) ?: error("Unable to find varobj: $internal")

                VarObjBits(type)
            }

            internal.startsWith("varbit.") -> {
                val type = ServerCacheManager.getVarbit(
                    internal.asRSCM(RSCMType.VARBIT)
                ) ?: error("Unable to find varbit: $internal")

                VarBitBits(type)
            }

            else -> error("Invalid internal key (must start with varobj. or varbit.): $internal")
        }
    }

    public fun getCharges(obj: InvObj?, internal: String): Int {
        val bits = resolveBits(internal).bits
        return obj?.vars?.getBits(bits) ?: 0
    }

    public fun addChargesSameItem(
        inventory: Inventory,
        slot: Int,
        add: Int,
        internal: String,
        max: Int,
    ): Charge {
        val bits = resolveBits(internal).bits
        val chargeRange = 0..bits.bitMask

        require(max in chargeRange) {
            "`max` charges ($max) must be within range [0..${bits.bitMask}]"
        }

        val obj = inventory[slot] ?: return Charge.Failure.ObjNotFound
        val curr = obj.vars.getBits(bits)
        val total = min(max, curr + add)

        if (curr == total) {
            return Charge.Failure.AlreadyFullCharges
        }

        val updatedVar = obj.vars.withBits(bits, total)
        inventory[slot] = obj.copy(vars = updatedVar)

        return Charge.Success.AddSameObj(
            added = total - curr,
            total = total
        )
    }

    public fun addCharges(
        inventory: Inventory,
        slot: Int,
        add: Int,
        internal: String,
        max: Int,
    ): Charge {
        val bits = resolveBits(internal).bits
        val chargeRange = 0..bits.bitMask

        require(max in chargeRange) {
            "`max` charges ($max) must be within range [0..${bits.bitMask}]"
        }

        val obj = inventory[slot] ?: return Charge.Failure.ObjNotFound
        val curr = obj.vars.getBits(bits)
        val total = min(max, curr + add)

        if (curr == total) {
            return Charge.Failure.AlreadyFullCharges
        }

        val updatedVar = obj.vars.withBits(bits, total)
        val added = total - curr

        if (curr == 0) {
            val charged = getInvObj(obj).paramOrNull(params.charged_variant)
                ?: error("Obj missing charged_variant param: $obj")

            inventory[slot] = InvObj(charged, vars = updatedVar)

            return Charge.Success.AddChangeObj(
                added = added,
                total = total,
                charged = charged
            )
        }

        inventory[slot] = obj.copy(vars = updatedVar)

        return Charge.Success.AddSameObj(
            added = added,
            total = total
        )
    }

    public fun reduceChargesSameItem(
        inventory: Inventory,
        slot: Int,
        remove: Int,
        internal: String,
    ): Charge {
        val bits = resolveBits(internal).bits

        val obj = inventory[slot] ?: return Charge.Failure.ObjNotFound
        val curr = obj.vars.getBits(bits)

        if (curr == 0) {
            return Charge.Failure.AlreadyFullCharges
        }

        val total = (curr - remove).coerceAtLeast(0)

        if (curr == total) {
            return Charge.Failure.AlreadyFullCharges
        }

        inventory[slot] = obj.copy(vars = obj.vars.withBits(bits, total))

        return Charge.Success.AddSameObj(
            added = total - curr,
            total = total
        )
    }

    public fun reduceWornCharges(
        player: Player,
        wearpos: Wearpos,
        internal: String,
        decrement: Int,
    ): Uncharge {
        val obj = player.worn[wearpos.slot] ?: return Uncharge.Failure.ObjNotFound
        val type = getInvObj(obj)

        val uncharged = type.paramOrNull(params.uncharged_variant)
            ?: error("Obj missing uncharged_variant param: $obj")

        val bits = resolveBits(internal).bits

        val current = obj.vars.getBits(bits)

        if (current < decrement) {
            if (current == 0) {
                player.worn[wearpos.slot] = InvObj(uncharged, vars = obj.vars)
            }
            return Uncharge.Failure.NotEnoughCharges
        }

        val newVal = current - decrement
        val updated = obj.vars.withBits(bits, newVal)

        player.worn[wearpos.slot] =
            if (newVal == 0) InvObj(uncharged, vars = updated)
            else obj.copy(vars = updated)

        return Uncharge.Success(newVal)
    }

    public fun removeAllCharges(
        inventory: Inventory,
        slot: Int,
        internal: String,
    ): Int {
        val obj = inventory.getValue(slot)
        val type = getInvObj(obj)

        val uncharged = type.paramOrNull(params.uncharged_variant)
            ?: error("Obj missing uncharged_variant param: $obj")

        val bits = resolveBits(internal).bits

        val previous = obj.vars.getBits(bits)

        inventory[slot] = InvObj(
            uncharged,
            vars = obj.vars.withBits(bits, 0)
        )

        return previous
    }

    public sealed class Charge {
        public sealed class Success : Charge() {
            public abstract val added: Int
            public abstract val total: Int

            public data class AddSameObj(
                override val added: Int,
                override val total: Int
            ) : Success()

            public data class AddChangeObj(
                override val added: Int,
                override val total: Int,
                val charged: ItemServerType,
            ) : Success()
        }

        public sealed class Failure : Charge() {
            public data object ObjNotFound : Failure()
            public data object AlreadyFullCharges : Failure()
        }
    }

    public sealed class Uncharge {
        public data class Success(val chargesLeft: Int) : Uncharge() {
            val fullyUncharged: Boolean get() = chargesLeft == 0
        }

        public sealed class Failure : Uncharge() {
            public data object ObjNotFound : Failure()
            public data object NotEnoughCharges : Failure()
        }
    }

    public companion object {

        public fun Charge.isFailure(): Boolean {
            contract {
                returns(true) implies (this@isFailure is Charge.Failure)
                returns(false) implies (this@isFailure is Charge.Success)
            }
            return this is Charge.Failure
        }

        public fun Uncharge.isFailure(): Boolean {
            contract {
                returns(true) implies (this@isFailure is Uncharge.Failure)
                returns(false) implies (this@isFailure is Uncharge.Success)
            }
            return this is Uncharge.Failure
        }
    }
}
