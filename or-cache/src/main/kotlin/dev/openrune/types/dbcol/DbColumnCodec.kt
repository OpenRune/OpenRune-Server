package dev.openrune.types.dbcol

import dev.openrune.ServerCacheManager
import dev.openrune.cache.filestore.definition.InterfaceType
import dev.openrune.definition.type.DBRowType
import dev.openrune.definition.type.EnumType
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.definition.util.BaseVarType
import dev.openrune.definition.util.CacheVarLiteral
import dev.openrune.definition.util.VarType
import dev.openrune.literal.CacheVarTypeMap.codecOut
import dev.openrune.types.ItemServerType
import dev.openrune.types.NpcServerType
import dev.openrune.types.ObjectServerType
import dev.openrune.types.StatRequirement
import dev.openrune.types.StatType
import dev.openrune.types.aconverted.AreaType
import dev.openrune.types.aconverted.MidiType
import kotlin.reflect.KClass
import org.rsmod.map.CoordGrid

public interface DbColumnCodec<T, R> {
    public val types: List<CacheVarLiteral>

    public fun decode(iterator: Iterator<T, R>): R

    public fun encode(value: R): List<T>

    public class Iterator<T, R>(
        private val codec: DbColumnCodec<T, R>,
        private val values: List<T>,
    ) {
        private var position = 0

        public fun next(): T {
            return values[position++]
        }

        public fun hasNext(): Boolean {
            return position < values.size
        }

        public fun single(): R = codec.decode(this)

        public fun toList(): List<R> {
            return buildList {
                while (hasNext()) {
                    this += codec.decode(this@Iterator)
                }
            }
        }
    }

    public object AreaTypeCodec : BaseIntCodec<AreaType> {
        override val types: List<CacheVarLiteral> = listOf(CacheVarLiteral.AREA)

        override fun decode(iterator: Iterator<Int, AreaType>): AreaType {
            val type = iterator.next()
            return AreaType(type)
        }

        override fun encode(value: AreaType): List<Int> {
            return listOf(value.id)
        }
    }

    public interface BaseIntCodec<R> : DbColumnCodec<Int, R>

    public object BooleanCodec : BaseIntCodec<Boolean> {
        override val types: List<CacheVarLiteral> = listOf(CacheVarLiteral.BOOLEAN)

        override fun decode(iterator: Iterator<Int, Boolean>): Boolean {
            return iterator.next() == 1
        }

        override fun encode(value: Boolean): List<Int> {
            return listOf(if (value) 1 else 0)
        }
    }

    public object ComponentTypeCodec : BaseIntCodec<ComponentType> {
        override val types: List<CacheVarLiteral> = listOf(CacheVarLiteral.COMPONENT)

        override fun decode(iterator: Iterator<Int, ComponentType>): ComponentType {
            val packed = iterator.next()
            if (packed == -1) {
                return ServerCacheManager.fromComponent(0)
            }
            return ServerCacheManager.fromComponent(packed)
        }

        override fun encode(value: ComponentType): List<Int> {
            return listOf(value.packed)
        }
    }

    public object CoordGridCodec : BaseIntCodec<CoordGrid> {
        override val types: List<CacheVarLiteral> = listOf(CacheVarLiteral.COORDGRID)

        override fun decode(iterator: Iterator<Int, CoordGrid>): CoordGrid {
            val packed = iterator.next()
            return CoordGrid(packed)
        }

        override fun encode(value: CoordGrid): List<Int> {
            return listOf(value.packed)
        }
    }

    public object DbRowTypeCodec : BaseIntCodec<DBRowType> {
        override val types: List<CacheVarLiteral> = listOf(CacheVarLiteral.DBROW)

        override fun decode(iterator: Iterator<Int, DBRowType>): DBRowType {
            val type = iterator.next()
            return ServerCacheManager.getDbrow(type) ?: error("Unable to decode db row")
        }

        override fun encode(value: DBRowType): List<Int> {
            return listOf(value.id)
        }
    }

    public class EnumTypeCodec<K : Any, V : Any>(
        private val keyType: KClass<K>,
        private val valType: KClass<V>,
    ) : BaseIntCodec<EnumType> {
        override val types: List<CacheVarLiteral> = listOf(CacheVarLiteral.ENUM)

        override fun decode(iterator: Iterator<Int, EnumType>): EnumType {
            val cacheType =
                ServerCacheManager.getEnum(iterator.next())
                    ?: error("Unable to get enum: ${iterator.next()}")
            val typeKey = cacheType.keyType.codecOut
            val typeVal = cacheType.keyType.codecOut
            if (typeKey != keyType || typeVal != valType) {
                val message =
                    "Unexpected enum types: enum='${cacheType.id}', " +
                        "expected=<${typeKey.simpleName}, ${typeVal.simpleName}>, " +
                        "actual=<${keyType.simpleName}, ${valType.simpleName}>"
                throw IllegalArgumentException(message)
            }
            @Suppress("UNCHECKED_CAST")
            return cacheType
        }

        override fun encode(value: EnumType): List<Int> {
            return listOf(value.id)
        }
    }

    public object IntCodec : BaseIntCodec<Int> {
        override val types: List<CacheVarLiteral> = listOf(CacheVarLiteral.INT)

        override fun decode(iterator: Iterator<Int, Int>): Int {
            return iterator.next()
        }

        override fun encode(value: Int): List<Int> {
            return listOf(value)
        }
    }

    public object InterfaceTypeCodec : BaseIntCodec<InterfaceType> {
        override val types: List<CacheVarLiteral> = listOf(CacheVarLiteral.INTERFACE)

        override fun decode(iterator: Iterator<Int, InterfaceType>): InterfaceType {
            val type = iterator.next()
            return ServerCacheManager.getInterface(type)
                ?: error("Unable to decode interface: ${type}")
        }

        override fun encode(value: InterfaceType): List<Int> {
            return listOf(value.id)
        }
    }

    public object LocTypeCodec : BaseIntCodec<ObjectServerType> {
        override val types: List<CacheVarLiteral> = listOf(CacheVarLiteral.LOC)

        override fun decode(iterator: Iterator<Int, ObjectServerType>): ObjectServerType {
            val type = iterator.next()
            return ServerCacheManager.getObject(type) ?: error("Unable to decode loc")
        }

        override fun encode(value: ObjectServerType): List<Int> {
            return listOf(value.id)
        }
    }

    public object MidiTypeCodec : BaseIntCodec<MidiType> {
        override val types: List<CacheVarLiteral> = listOf(CacheVarLiteral.MIDI)

        override fun decode(iterator: Iterator<Int, MidiType>): MidiType {
            val type = iterator.next()
            return MidiType(type)
        }

        override fun encode(value: MidiType): List<Int> {
            return listOf(value.id)
        }
    }

    public object NpcTypeCodec : BaseIntCodec<NpcServerType> {
        override val types: List<CacheVarLiteral> = listOf(CacheVarLiteral.NPC)

        override fun decode(iterator: Iterator<Int, NpcServerType>): NpcServerType {
            val type = iterator.next()
            return ServerCacheManager.getNpc(type) ?: error("Unable to get npc: ${type}")
        }

        override fun encode(value: NpcServerType): List<Int> {
            return listOf(value.id)
        }
    }

    public object ItemServerTypeCodec : BaseIntCodec<ItemServerType> {
        override val types: List<CacheVarLiteral> = listOf(CacheVarLiteral.OBJ)

        override fun decode(iterator: Iterator<Int, ItemServerType>): ItemServerType {
            val type = iterator.next()
            return ServerCacheManager.getItem(type) ?: error("Unable to find item: $type")
        }

        override fun encode(value: ItemServerType): List<Int> {
            return listOf(value.id)
        }
    }

    public object StatReqCodec : DbColumnCodec<Int, StatRequirement> {
        override val types: List<CacheVarLiteral> =
            listOf(CacheVarLiteral.STAT, CacheVarLiteral.INT)

        override fun decode(iterator: Iterator<Int, StatRequirement>): StatRequirement {
            val stat = iterator.next()
            val req = iterator.next()
            val type = ServerCacheManager.getStats(stat) ?: error("Error Getting Stat: $stat")
            return StatRequirement(type, req)
        }

        override fun encode(value: StatRequirement): List<Int> {
            return listOf(value.stat.id, value.level)
        }
    }

    public object StatTypeCodec : BaseIntCodec<StatType> {
        override val types: List<CacheVarLiteral> = listOf(CacheVarLiteral.STAT)

        override fun decode(iterator: Iterator<Int, StatType>): StatType {
            val type = iterator.next()
            return ServerCacheManager.getStats(type) ?: error("Error finding stat: ${type}")
        }

        override fun encode(value: StatType): List<Int> {
            return listOf(value.id)
        }
    }

    public object StringCodec : DbColumnCodec<String, String> {
        override val types: List<CacheVarLiteral> = listOf(CacheVarLiteral.STRING)

        override fun decode(iterator: Iterator<String, String>): String {
            return iterator.next()
        }

        override fun encode(value: String): List<String> {
            return listOf(value)
        }
    }

    /**
     * Interface column storing a raw interface id (e.g. packed id), not a resolved [InterfaceType].
     */
    public object InterfaceIdCodec : BaseIntCodec<Int> {
        override val types: List<CacheVarLiteral> = listOf(CacheVarLiteral.INTERFACE)

        override fun decode(iterator: Iterator<Int, Int>): Int = iterator.next()

        override fun encode(value: Int): List<Int> = listOf(value)
    }

    /** Enum column storing the enum definition id as a plain integer. */
    public object EnumTypeIdCodec : BaseIntCodec<Int> {
        override val types: List<CacheVarLiteral> = listOf(CacheVarLiteral.ENUM)

        override fun decode(iterator: Iterator<Int, Int>): Int = iterator.next()

        override fun encode(value: Int): List<Int> = listOf(value)
    }

    /** DB row column storing a row id as a plain integer. */
    public object DbRowIdCodec : BaseIntCodec<Int> {
        override val types: List<CacheVarLiteral> = listOf(CacheVarLiteral.DBROW)

        override fun decode(iterator: Iterator<Int, Int>): Int = iterator.next()

        override fun encode(value: Int): List<Int> = listOf(value)
    }

    /** MIDI column storing a midi id as a plain integer. */
    public object MidiIdCodec : BaseIntCodec<Int> {
        override val types: List<CacheVarLiteral> = listOf(CacheVarLiteral.MIDI)

        override fun decode(iterator: Iterator<Int, Int>): Int = iterator.next()

        override fun encode(value: Int): List<Int> = listOf(value)
    }
}

/**
 * Decode one enum key/value slot using the same [Iterator]/[decode] path as db columns. [raw]
 * matches stringified entries in [EnumType.values].
 */
public fun <R> DbColumnCodec<Int, R>.decodeEnumSlot(raw: String): R {
    val v = raw.toIntOrNull() ?: error("Expected int-compatible enum slot, got '$raw'")
    return DbColumnCodec.Iterator(this, listOf(v)).single()
}

public fun DbColumnCodec<String, String>.decodeEnumSlot(raw: String): String =
    DbColumnCodec.Iterator(this, listOf(raw)).single()

/** [VarType] for this codec's single slot, for matching [EnumType] key/value types. */
public fun DbColumnCodec<*, *>.openRuneEnumSlotVarType(): VarType {
    require(types.size == 1) {
        "Enum decode requires a single-slot codec, got ${types.size} slots (${types.joinToString()})"
    }
    val lit = types[0]
    return VarType.byID(lit.id)
}

@Suppress("UNCHECKED_CAST")
public fun <V> decodeEnumSlotForEnum(codec: DbColumnCodec<*, V>, raw: String): V {
    require(codec.types.size == 1) { "Enum decode requires a single-slot codec" }
    return when (codec.types[0].baseType) {
        BaseVarType.STRING -> {
            (codec as DbColumnCodec<String, String>).decodeEnumSlot(raw) as V
        }
        else -> {
            (codec as DbColumnCodec<Int, V>).decodeEnumSlot(raw)
        }
    }
}

/** Maps a column [VarType] to the cache literal used for validation. */
public fun cacheLiteralForOpenRuneVarType(varType: VarType): CacheVarLiteral =
    CacheVarLiteral[varType.id]
        ?: error("No CacheVarLiteral for OpenRune VarType id=${varType.id} ('${varType.ch}')")

@Suppress("UNCHECKED_CAST")
internal fun <T> List<Any?>.asCodecRawValues(codec: DbColumnCodec<T, *>): List<T> {
    require(size == codec.types.size) {
        "Value count $size does not match codec slot count ${codec.types.size}"
    }
    return mapIndexed { i, raw ->
        when (codec.types[i].baseType) {
            BaseVarType.STRING -> raw as T
            else -> raw.toOpenRuneInt() as T
        }
    }
}

private fun Any?.toOpenRuneInt(): Int =
    when (this) {
        null -> error("Unexpected null in numeric db column slot")
        is Int -> this
        is Number -> this.toInt()
        is String -> this.toIntOrNull() ?: error("Expected int-compatible string, got '$this'")
        else -> error("Cannot coerce ${this::class} to Int for db column slot")
    }
