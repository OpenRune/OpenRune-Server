package dev.openrune.types.util

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.VarnBitType
import dev.openrune.types.VarnType
import java.util.BitSet

public object VarnpcCollisions {
    public fun detect(varns: Iterable<VarnType>, varnbits: Iterable<VarnBitType>): List<Error> {
        val results = mutableListOf<Error>()

        val varnsBitSets = varns.associate { it.id to BitSet() }
        for (varnbit in varnbits) {
            val varnBitSet = varnsBitSets[varnbit.baseVar.id]
            if (varnBitSet == null) {
                results += Error.InvalidBaseVar(varnbit)
                continue
            }

            val varnbitRange = varnbit.asBitSet()
            if (varnBitSet.intersects(varnbitRange)) {
                results += Error.VarnBitCollision(varnbit)
                continue
            }

            varnBitSet.or(varnbitRange)
        }

        return results
    }

    private fun VarnBitType.asBitSet(): BitSet = BitSet().apply { set(lsb, msb) }

    public sealed class Error(public val varnbit: VarnBitType) {
        public class InvalidBaseVar(varnbit: VarnBitType) : Error(varnbit) {
            override fun toString(): String =
                "InvalidVarn(varn=${varnbit.baseVar.id}, varnbit=${varnbit.id})"
        }

        public class VarnBitCollision(varnbit: VarnBitType) : Error(varnbit) {
            override fun toString(): String =
                "Collision(" +
                    "varn=${varnbit.baseVar.id}:${RSCM.getReverseMapping(RSCMType.VARN,varnbit.baseVar.id)}, " +
                    "varnbit=${varnbit.id}, " +
                    "bits=${varnbit.lsb}..${varnbit.msb}" +
                    ")"
        }
    }
}
