package org.rsmod.game.vars

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.VarnBitType
import dev.openrune.types.VarnType
import it.unimi.dsi.fastutil.ints.Int2IntMap
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import org.rsmod.utils.bits.bitMask
import org.rsmod.utils.bits.getBits
import org.rsmod.utils.bits.withBits

@JvmInline
public value class VarNpcIntMap(public val backing: Int2IntMap = Int2IntOpenHashMap()) {
    public fun remove(key: VarnType) {
        backing.remove(key.id)
    }

    public fun remove(key: String) {
        backing.remove(key.asRSCM(RSCMType.VARN))
    }

    public operator fun get(key: String): Int = backing.getOrDefault(key.asRSCM(RSCMType.VARN), 0)

    public operator fun get(key: VarnType): Int = backing.getOrDefault(key.id, 0)

    public operator fun set(key: String, value: Int?) {
        val varnType = ServerCacheManager.getVarn(key.asRSCM(RSCMType.VARN)) ?: error("Unable to find varn: $key")
        set(varnType, value)
    }


    public operator fun set(key: VarnType, value: Int?) {
        if (value == null) {
            backing.remove(key.id)
        } else {
            backing[key.id] = value
        }
    }

    public operator fun get(varn: VarnBitType): Int {
        val mappedValue = this[varn.baseVar]
        val extracted = mappedValue.getBits(varn.bits)
        return extracted
    }

    public operator fun set(varn: VarnBitType, value: Int) {
        assertVarnBitBounds(varn, value)
        val mappedValue = this[varn.baseVar]
        val packedValue = mappedValue.withBits(varn.bits, value)
        set(varn.baseVar, packedValue)
    }

    public operator fun contains(key: VarnType): Boolean = backing.containsKey(key.id)

    override fun toString(): String = backing.toString()

    private companion object {
        private fun assertVarnBitBounds(varn: VarnBitType, value: Int) {
            val maxValue = varn.maxValue()
            require(value in 0..maxValue) {
                "Varnbit overflow on varnbit ${varn.id} " +
                    "Value $value is outside the range 0-$maxValue (type=$varn)"
            }
        }

        private fun VarnBitType.maxValue(): Long = bits.bitMask
    }
}
