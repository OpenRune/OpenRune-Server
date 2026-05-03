package org.rsmod.game.vars

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.VarConBitType
import dev.openrune.types.VarConType
import it.unimi.dsi.fastutil.ints.Int2IntMap
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import org.rsmod.utils.bits.bitMask
import org.rsmod.utils.bits.getBits
import org.rsmod.utils.bits.withBits

@JvmInline
public value class VarConIntMap(public val backing: Int2IntMap = Int2IntOpenHashMap()) {
    public fun remove(key: VarConType) {
        backing.remove(key.id)
    }

    public fun remove(key: String) {
        backing.remove(key.asRSCM(RSCMType.VARCON))
    }

    public operator fun get(key: String): Int = backing.getOrDefault(key.asRSCM(RSCMType.VARCON), 0)

    public operator fun get(key: VarConType): Int = backing.getOrDefault(key.id, 0)

    public operator fun set(key: String, value: Int?) {
        val varconType = ServerCacheManager.getVarCon(key.asRSCM(RSCMType.VARCON)) ?: error("Unable to find varcon: $key")
        set(varconType, value)
    }

    public operator fun set(key: VarConType, value: Int?) {
        if (value == null) {
            backing.remove(key.id)
        } else {
            backing[key.id] = value
        }
    }

    public operator fun get(varcon: VarConBitType): Int {
        val mappedValue = this[varcon.baseVar]
        val extracted = mappedValue.getBits(varcon.bits)
        return extracted
    }

    public operator fun set(varcon: VarConBitType, value: Int) {
        assertVarConBitBounds(varcon, value)
        val mappedValue = this[varcon.baseVar]
        val packedValue = mappedValue.withBits(varcon.bits, value)
        set(varcon.baseVar, packedValue)
    }

    public operator fun contains(key: VarConType): Boolean = backing.containsKey(key.id)

    override fun toString(): String = backing.toString()

    private companion object {
        private fun assertVarConBitBounds(varcon: VarConBitType, value: Int) {
            val maxValue = varcon.maxValue()
            require(value in 0..maxValue) {
                "Varconbit overflow on varconbit ${varcon.id} " +
                    "Value $value is outside the range 0-$maxValue (type=$varcon)"
            }
        }

        private fun VarConBitType.maxValue(): Long = bits.bitMask
    }
}
