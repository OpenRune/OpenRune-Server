package dev.openrune.types.util

import dev.openrune.definition.type.VarBitType
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.varp.VarpServerType
import dev.openrune.types.varp.baseVar
import dev.openrune.types.varp.lsb
import dev.openrune.types.varp.msb
import java.util.BitSet

public object VarplayerCollisions {
    public fun detect(varps: Iterable<VarpServerType>, varbits: Iterable<VarBitType>): List<Error> {
        val results = mutableListOf<Error>()

        val varpsBitSets = varps.associate { it.id to BitSet() }
        for (varbit in varbits) {
            val varpBitSet = varpsBitSets[varbit.baseVar.id]
            if (varpBitSet == null) {
                results += Error.InvalidBaseVar(varbit)
                continue
            }

            val varbitRange = varbit.asBitSet()
            if (varpBitSet.intersects(varbitRange)) {
                results += Error.VarpBitCollision(varbit)
                continue
            }

            varpBitSet.or(varbitRange)
        }

        return results
    }

    private fun VarBitType.asBitSet(): BitSet = BitSet().apply { set(lsb, msb) }

    public sealed class Error(public val varbit: VarBitType) {
        public class InvalidBaseVar(varbit: VarBitType) : Error(varbit) {
            override fun toString(): String =
                "InvalidVarp(varp=${varbit.baseVar?.id}, varbit=${varbit.id})"
        }

        public class VarpBitCollision(varbit: VarBitType) : Error(varbit) {
            override fun toString(): String =
                "Collision(" +
                    "varp=${varbit.baseVar.id}:${RSCM.getReverseMapping(RSCMType.VARP,varbit.baseVar.id)}, " +
                    "varbit=${varbit.id}:${RSCM.getReverseMapping(RSCMType.VARBIT,varbit.id)}, " +
                    "bits=${varbit.lsb}..${varbit.msb}" +
                    ")"
        }
    }
}
