package dev.openrune.types.varp

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.VarBitType

val VarBitType.lsb: Int
    get() = this.startBit

val VarBitType.msb: Int
    get() = this.endBit

val VarBitType.bits: IntRange
    get() = lsb..msb

val VarBitType.baseVar: VarpServerType
    get() =
        ServerCacheManager.getVarp(this.varp) ?: error("Error getting varp from varbit: ${this.id}")

fun VarBitType.isType(varbit: VarBitType) = this.id == varbit.id
