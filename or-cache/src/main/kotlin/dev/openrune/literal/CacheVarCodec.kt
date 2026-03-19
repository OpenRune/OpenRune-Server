package dev.openrune.literal

import dev.openrune.ServerCacheManager
import dev.openrune.cache.filestore.definition.InterfaceType
import dev.openrune.definition.type.DBRowType
import dev.openrune.definition.type.DBTableType
import dev.openrune.definition.type.HitSplatType
import dev.openrune.definition.type.VarBitType
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.types.HealthBarServerType
import dev.openrune.types.ItemServerType
import dev.openrune.types.NpcServerType
import dev.openrune.types.ObjectServerType
import dev.openrune.types.ProjAnimType
import dev.openrune.types.SequenceServerType
import dev.openrune.types.StatType
import dev.openrune.types.aconverted.AreaType
import dev.openrune.types.aconverted.CategoryType
import dev.openrune.types.aconverted.SpotanimType
import dev.openrune.types.aconverted.SynthType
import dev.openrune.types.enums.EnumTypeMap
import dev.openrune.types.varp.VarpServerType
import kotlin.reflect.KClass
import org.rsmod.map.CoordGrid

public sealed class CacheVarCodec<K, V : Any>(public val out: KClass<V>) {
    public abstract fun decode(value: K): V?

    public abstract fun encode(value: V): K
}

public abstract class BaseIntVarCodec<V : Any>(out: KClass<V>) : CacheVarCodec<Int, V>(out)

public abstract class BaseStringVarCodec<V : Any>(out: KClass<V>) : CacheVarCodec<String, V>(out)

public object CacheVarIntCodec : BaseIntVarCodec<Int>(Int::class) {
    override fun decode(value: Int): Int = value

    override fun encode(value: Int): Int = value
}

public object CacheVarStringCodec : BaseStringVarCodec<String>(String::class) {
    override fun decode(value: String): String = value

    override fun encode(value: String): String = value
}

public object CacheVarBoolCodec : BaseIntVarCodec<Boolean>(Boolean::class) {
    override fun decode(value: Int): Boolean = value == 1

    override fun encode(value: Boolean): Int = if (value) 1 else 0
}

public object CacheVarAreaCodec : BaseIntVarCodec<AreaType>(AreaType::class) {
    override fun decode(value: Int): AreaType? = AreaType(value)

    override fun encode(value: AreaType): Int = value.id
}

public object CacheVarCategoryCodec : BaseIntVarCodec<CategoryType>(CategoryType::class) {
    // Note: Not every referenced category requires a symbol name. If a category is not
    // found in the predefined list, we create a new `CategoryType`.
    // This behavior may change in the future if we decide to enforce that every referenced
    // category (e.g., in enum or param types) must have a defined symbol.
    override fun decode(value: Int): CategoryType = CategoryType(value)

    override fun encode(value: CategoryType): Int = value.id
}

public object CacheVarCoordGridCodec : BaseIntVarCodec<CoordGrid>(CoordGrid::class) {
    override fun decode(value: Int): CoordGrid = CoordGrid(value)

    override fun encode(value: CoordGrid): Int = value.packed
}

public object CacheVarComponentCodec : BaseIntVarCodec<ComponentType>(ComponentType::class) {
    override fun decode(value: Int): ComponentType? {
        if (value == -1) {
            return null
        }

        val interfaceId = value ushr 16
        val childId = value and 0xFFFF

        return ServerCacheManager.getInterface(interfaceId)?.components[childId]
    }

    override fun encode(value: ComponentType): Int = value.packed
}

public object CacheVarDbRowCodec : BaseIntVarCodec<DBRowType>(DBRowType::class) {
    override fun decode(value: Int): DBRowType? = ServerCacheManager.getDbrow(value)

    override fun encode(value: DBRowType): Int = value.id
}

public object CacheVarDbTableCodec : BaseIntVarCodec<DBTableType>(DBTableType::class) {
    override fun decode(value: Int): DBTableType? = ServerCacheManager.getDbtable(value)

    override fun encode(value: DBTableType): Int = value.id
}

public object CacheVarHeadbarCodec :
    BaseIntVarCodec<HealthBarServerType>(HealthBarServerType::class) {
    override fun decode(value: Int): HealthBarServerType? = ServerCacheManager.getHealthBar(value)

    override fun encode(value: HealthBarServerType): Int = value.id
}

public object CacheVarHitmarkCodec : BaseIntVarCodec<HitSplatType>(HitSplatType::class) {
    override fun decode(value: Int): HitSplatType? = ServerCacheManager.getHitSplats(value)

    override fun encode(value: HitSplatType): Int = value.id
}

public object CacheVarInterfaceCodec : BaseIntVarCodec<InterfaceType>(InterfaceType::class) {
    override fun decode(value: Int): InterfaceType? = ServerCacheManager.getInterface(value)

    override fun encode(value: InterfaceType): Int = value.id
}

public object CacheVarNamedObjCodec : BaseIntVarCodec<ItemServerType>(ItemServerType::class) {
    override fun decode(value: Int): ItemServerType? = ServerCacheManager.getItem(value)

    override fun encode(value: ItemServerType): Int = value.id
}

public object CacheVarObjCodec : BaseIntVarCodec<ItemServerType>(ItemServerType::class) {
    override fun decode(value: Int): ItemServerType? = ServerCacheManager.getItem(value)

    override fun encode(value: ItemServerType): Int = value.id
}

public object CacheVarSeqCodec : BaseIntVarCodec<SequenceServerType>(SequenceServerType::class) {
    override fun decode(value: Int): SequenceServerType? = ServerCacheManager.getAnim(value)

    override fun encode(value: SequenceServerType): Int = value.id
}

public object CacheVarSpotanimCodec : BaseIntVarCodec<SpotanimType>(SpotanimType::class) {
    override fun decode(value: Int): SpotanimType = SpotanimType(value)

    override fun encode(value: SpotanimType): Int = value.id
}

public object CacheVarLocCodec : BaseIntVarCodec<ObjectServerType>(ObjectServerType::class) {
    override fun decode(value: Int): ObjectServerType? = ServerCacheManager.getObject(value)

    override fun encode(value: ObjectServerType): Int = value.id
}

public object CacheVarNpcCodec : BaseIntVarCodec<NpcServerType>(NpcServerType::class) {
    override fun decode(value: Int): NpcServerType? = ServerCacheManager.getNpc(value)

    override fun encode(value: NpcServerType): Int = value.id
}

public object CacheVarEnumCodec : BaseIntVarCodec<EnumTypeMap<*, *>>(EnumTypeMap::class) {
    override fun decode(value: Int): EnumTypeMap<Any, Any>? {
        val type = ServerCacheManager.getEnum(value) ?: return null

        return EnumTypeMap(type)
    }

    override fun encode(value: EnumTypeMap<*, *>): Int = value.id
}

public object CacheVarProjAnimCodec : BaseIntVarCodec<ProjAnimType>(ProjAnimType::class) {
    override fun decode(value: Int): ProjAnimType? = ServerCacheManager.getProjectile(value)

    override fun encode(value: ProjAnimType): Int = value.id
}

public object CacheVarStatCodec : BaseIntVarCodec<StatType>(StatType::class) {
    override fun decode(value: Int): StatType? = ServerCacheManager.getStats(value)

    override fun encode(value: StatType): Int = value.id
}

public object CacheVarSynthCodec : BaseIntVarCodec<SynthType>(SynthType::class) {
    // Note: Not every referenced synth requires a symbol name. If a synth is not found in
    // the predefined list, we create a new `SynthType`.
    // This behavior may change in the future if we decide to enforce that every referenced
    // synth (e.g., in enum or param types) must have a defined symbol.
    override fun decode(value: Int): SynthType = SynthType(value)

    override fun encode(value: SynthType): Int = value.id
}

public object CacheVarVarBitCodec : BaseIntVarCodec<VarBitType>(VarBitType::class) {
    override fun decode(value: Int): VarBitType? = ServerCacheManager.getVarbit(value)

    override fun encode(value: VarBitType): Int = value.id
}

public object CacheVarVarpCodec : BaseIntVarCodec<VarpServerType>(VarpServerType::class) {
    override fun decode(value: Int): VarpServerType? = ServerCacheManager.getVarp(value)

    override fun encode(value: VarpServerType): Int = value.id
}
