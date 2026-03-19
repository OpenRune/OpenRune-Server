package dev.openrune.types.dbcol

import dev.openrune.ServerCacheManager
import dev.openrune.definition.util.CacheVarLiteral

/**
 * [Int] columns whose cache literal is not [CacheVarLiteral.INT] (used by generated table rows).
 */
public abstract class IntVarIdCodec(literal: CacheVarLiteral) : DbColumnCodec.BaseIntCodec<Int> {
    override val types: List<CacheVarLiteral> = listOf(literal)

    override fun decode(iterator: DbColumnCodec.Iterator<Int, Int>): Int = iterator.next()

    override fun encode(value: Int): List<Int> = listOf(value)
}

public object NpcIdCodec : IntVarIdCodec(CacheVarLiteral.NPC)

public object LocIdCodec : IntVarIdCodec(CacheVarLiteral.LOC)

public object ObjIdCodec : IntVarIdCodec(CacheVarLiteral.OBJ)

public object GraphicIdCodec : IntVarIdCodec(CacheVarLiteral.GRAPHIC)

public object ModelIdCodec : IntVarIdCodec(CacheVarLiteral.MODEL)

public object SeqIdCodec : IntVarIdCodec(CacheVarLiteral.SEQ)

public object StructIdCodec : IntVarIdCodec(CacheVarLiteral.STRUCT)

public object CategoryIdCodec : IntVarIdCodec(CacheVarLiteral.CATEGORY)

public object MapElementIdCodec : IntVarIdCodec(CacheVarLiteral.MAPELEMENT)

public object LocShapeIdCodec : IntVarIdCodec(CacheVarLiteral.LOCSHAPE)

public object InvIdCodec : IntVarIdCodec(CacheVarLiteral.INV)

public object IdkIdCodec : IntVarIdCodec(CacheVarLiteral.IDKIT)

public object VarpIdCodec : IntVarIdCodec(CacheVarLiteral.VARP)

public object VarbitIdCodec : IntVarIdCodec(ServerCacheManager.VARBIT)

public object DbtableIdCodec : IntVarIdCodec(CacheVarLiteral.DBTABLE)

public object NamedObjIdCodec : IntVarIdCodec(CacheVarLiteral.NAMEDOBJ)

public object SynthIdCodec : IntVarIdCodec(CacheVarLiteral.SYNTH)

public object ComponentIdCodec : IntVarIdCodec(CacheVarLiteral.COMPONENT)

/** Plain stat id column (integer slot), not a resolved stat type reference. */
public object StatIdCodec : IntVarIdCodec(CacheVarLiteral.STAT)
