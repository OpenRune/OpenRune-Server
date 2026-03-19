package dev.openrune.types.dbcol

import dev.openrune.cache.filestore.definition.InterfaceType
import dev.openrune.definition.type.DBRowType
import dev.openrune.definition.type.EnumType
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.types.ItemServerType
import dev.openrune.types.NpcServerType
import dev.openrune.types.ObjectServerType
import dev.openrune.types.StatRequirement
import dev.openrune.types.StatType
import dev.openrune.types.aconverted.AreaType
import dev.openrune.types.aconverted.EnumHelper
import dev.openrune.types.aconverted.EnumPair
import dev.openrune.types.aconverted.MidiType
import dev.openrune.types.dbcol.DbColumnCodec.AreaTypeCodec
import dev.openrune.types.dbcol.DbColumnCodec.BooleanCodec
import dev.openrune.types.dbcol.DbColumnCodec.ComponentTypeCodec
import dev.openrune.types.dbcol.DbColumnCodec.CoordGridCodec
import dev.openrune.types.dbcol.DbColumnCodec.DbRowIdCodec
import dev.openrune.types.dbcol.DbColumnCodec.DbRowTypeCodec
import dev.openrune.types.dbcol.DbColumnCodec.EnumTypeCodec
import dev.openrune.types.dbcol.DbColumnCodec.EnumTypeIdCodec
import dev.openrune.types.dbcol.DbColumnCodec.IntCodec
import dev.openrune.types.dbcol.DbColumnCodec.InterfaceTypeCodec
import dev.openrune.types.dbcol.DbColumnCodec.ItemServerTypeCodec
import dev.openrune.types.dbcol.DbColumnCodec.LocTypeCodec
import dev.openrune.types.dbcol.DbColumnCodec.MidiIdCodec
import dev.openrune.types.dbcol.DbColumnCodec.MidiTypeCodec
import dev.openrune.types.dbcol.DbColumnCodec.NpcTypeCodec
import dev.openrune.types.dbcol.DbColumnCodec.StatReqCodec
import dev.openrune.types.dbcol.DbColumnCodec.StatTypeCodec
import dev.openrune.types.dbcol.DbColumnCodec.StringCodec
import kotlin.reflect.KClass
import org.rsmod.map.CoordGrid

/**
 * Typed column accessors for [DbHelper], mirroring [DbColumnReferences] (`boolean`, `int`, `list`,
 * …).
 */
public fun DbHelper.area(internal: String): AreaType = column(internal, AreaTypeCodec)

public fun DbHelper.boolean(internal: String): Boolean = column(internal, BooleanCodec)

public fun DbHelper.component(internal: String): ComponentType =
    column(internal, ComponentTypeCodec)

public fun DbHelper.coord(internal: String): CoordGrid = column(internal, CoordGridCodec)

public fun DbHelper.dbRow(internal: String): DBRowType = column(internal, DbRowTypeCodec)

public fun <K : Any, V : Any> DbHelper.dbEnum(
    internal: String,
    key: KClass<K>,
    value: KClass<V>,
): EnumType = column(internal, EnumTypeCodec(key, value))

public fun <K : Any, V : Any> DbHelper.dbEnumOptional(
    internal: String,
    key: KClass<K>,
    value: KClass<V>,
): EnumType? = columnOptional(internal, EnumTypeCodec(key, value))

public fun DbHelper.int(internal: String): Int = column(internal, IntCodec)

/** INTEGER column interpreted as [Long] (narrowing is not checked). */
public fun DbHelper.long(internal: String): Long = int(internal).toLong()

public fun DbHelper.longOptional(internal: String): Long? = intOptional(internal)?.toLong()

public fun DbHelper.interf(internal: String): InterfaceType = column(internal, InterfaceTypeCodec)

public fun DbHelper.loc(internal: String): ObjectServerType = column(internal, LocTypeCodec)

public fun DbHelper.midi(internal: String): MidiType = column(internal, MidiTypeCodec)

public fun DbHelper.npc(internal: String): NpcServerType = column(internal, NpcTypeCodec)

public fun DbHelper.obj(internal: String): ItemServerType = column(internal, ItemServerTypeCodec)

public fun DbHelper.stat(internal: String): StatType = column(internal, StatTypeCodec)

public fun DbHelper.statReq(internal: String): StatRequirement = column(internal, StatReqCodec)

public fun DbHelper.string(internal: String): String = column(internal, StringCodec)

public fun DbHelper.enumTypeId(internal: String): Int = column(internal, EnumTypeIdCodec)

/**
 * Reads an ENUM column as an enum type id, loads that enum from cache, decodes entries with
 * [keyCodec] / [valueCodec], and checks [org.rsmod.api.type.refs.aconverted.EnumHelper.keyType] /
 * [valueType] match the codecs (same as top-level [org.rsmod.api.type.refs.aconverted.enum] /
 * [org.rsmod.api.type.refs.aconverted.EnumHelper.getEnum]).
 */
public fun <V1, V2> DbHelper.enum(
    internal: String,
    keyCodec: DbColumnCodec<*, V1>,
    valueCodec: DbColumnCodec<*, V2>,
): List<EnumPair<V1, V2>> {
    val id = enumTypeId(internal)
    return enumPairsFromId(internal, id, keyCodec, valueCodec)
}

/** Same as [enum] but returns null when the column is absent / has no enum id. */
public fun <V1, V2> DbHelper.enumOptional(
    internal: String,
    keyCodec: DbColumnCodec<*, V1>,
    valueCodec: DbColumnCodec<*, V2>,
): List<EnumPair<V1, V2>>? {
    val id = enumTypeIdOptional(internal) ?: return null
    return enumPairsFromId(internal, id, keyCodec, valueCodec)
}

private fun <V1, V2> enumPairsFromId(
    internal: String,
    id: Int,
    keyCodec: DbColumnCodec<*, V1>,
    valueCodec: DbColumnCodec<*, V2>,
): List<EnumPair<V1, V2>> {
    val helper = EnumHelper.of(id)

    return helper.getEnum(keyCodec, valueCodec)
}

/**
 * Decodes a column into a list: the column values are read in consecutive chunks of
 * [DbColumnCodec.types].size, each chunk decoded with [decoder].
 */
public fun <T, R> DbHelper.list(internal: String, decoder: DbColumnCodec<T, R>): List<R> {
    val col = getColumn(internal)
    val values = col.column.values ?: return emptyList()
    val step = decoder.types.size
    require(step > 0) { "Codec must declare at least one literal type" }
    require(values.size % step == 0) {
        "Column '$internal' has ${values.size} value(s) but codec requires chunks of $step"
    }
    return buildList(values.size / step) {
        var i = 0
        while (i < values.size) {
            add(col.decodeAt(i, decoder))
            i += step
        }
    }
}

public fun DbHelper.booleanOptional(internal: String): Boolean? =
    columnOptional(internal, BooleanCodec)

public fun DbHelper.intOptional(internal: String): Int? = columnOptional(internal, IntCodec)

public fun DbHelper.stringOptional(internal: String): String? =
    columnOptional(internal, StringCodec)

public fun DbHelper.dbRowIdOptional(internal: String): Int? = columnOptional(internal, DbRowIdCodec)

public fun DbHelper.midiIdOptional(internal: String): Int? = columnOptional(internal, MidiIdCodec)

public fun DbHelper.enumTypeIdOptional(internal: String): Int? =
    columnOptional(internal, EnumTypeIdCodec)

public fun DbHelper.npcOptional(internal: String): NpcServerType? =
    columnOptional(internal, NpcTypeCodec)

public fun DbHelper.objOptional(internal: String): ItemServerType? =
    columnOptional(internal, ItemServerTypeCodec)

/**
 * Each value in the column is decoded with [codecs] cycled by index (same rules as
 * [multiColumnOptional]); failed slots are omitted instead of appearing as `null`.
 */
public fun <T, R> DbHelper.slotsOptional(
    internal: String,
    vararg codecs: DbColumnCodec<T, R>,
): List<R> {
    require(codecs.isNotEmpty()) { "At least one codec required" }
    return multiColumnOptional(internal, *codecs).filterNotNull()
}
