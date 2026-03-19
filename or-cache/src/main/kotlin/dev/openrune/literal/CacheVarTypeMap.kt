package dev.openrune.literal

import dev.openrune.ServerCacheManager
import dev.openrune.cache.filestore.definition.InterfaceType
import dev.openrune.definition.type.DBRowType
import dev.openrune.definition.type.DBTableType
import dev.openrune.definition.type.EnumType
import dev.openrune.definition.type.HitSplatType
import dev.openrune.definition.type.SpotAnimType
import dev.openrune.definition.type.VarBitType
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.definition.util.CacheVarLiteral
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

public object CacheVarTypeMap {

    public val classedLiterals: Map<KClass<*>, CacheVarLiteral> =
        hashMapOf(
            AreaType::class to CacheVarLiteral.AREA,
            Boolean::class to CacheVarLiteral.BOOLEAN,
            ComponentType::class to CacheVarLiteral.COMPONENT,
            Int::class to CacheVarLiteral.INT,
            CategoryType::class to CacheVarLiteral.CATEGORY,
            CoordGrid::class to CacheVarLiteral.COORDGRID,
            DBRowType::class to CacheVarLiteral.DBROW,
            DBTableType::class to CacheVarLiteral.DBTABLE,
            EnumType::class to CacheVarLiteral.ENUM,
            HealthBarServerType::class to CacheVarLiteral.HEADBAR,
            HitSplatType::class to CacheVarLiteral.HITMARK,
            InterfaceType::class to CacheVarLiteral.INTERFACE,
            ObjectServerType::class to CacheVarLiteral.LOC,
            NpcServerType::class to CacheVarLiteral.NPC,
            ItemServerType::class to CacheVarLiteral.OBJ,
            ProjAnimType::class to ServerCacheManager.PROJANIM,
            SequenceServerType::class to CacheVarLiteral.SEQ,
            SpotanimType::class to CacheVarLiteral.SPOTANIM,
            SpotAnimType::class to CacheVarLiteral.SPOTANIM,
            ItemServerType::class to CacheVarLiteral.NAMEDOBJ,
            String::class to CacheVarLiteral.STRING,
            StatType::class to CacheVarLiteral.STAT,
            SynthType::class to CacheVarLiteral.SYNTH,
            VarBitType::class to ServerCacheManager.VARBIT,
            VarpServerType::class to CacheVarLiteral.VARP,
        )

    public val codecMap: Map<KClass<*>, CacheVarCodec<*, *>> =
        hashMapOf(
            AreaType::class to CacheVarAreaCodec,
            Boolean::class to CacheVarBoolCodec,
            CategoryType::class to CacheVarCategoryCodec,
            ComponentType::class to CacheVarComponentCodec,
            DBRowType::class to CacheVarDbRowCodec,
            DBTableType::class to CacheVarDbTableCodec,
            Int::class to CacheVarIntCodec,
            CoordGrid::class to CacheVarCoordGridCodec,
            EnumType::class to CacheVarEnumCodec,
            EnumTypeMap::class to CacheVarEnumCodec,
            HealthBarServerType::class to CacheVarHeadbarCodec,
            HitSplatType::class to CacheVarHitmarkCodec,
            InterfaceType::class to CacheVarInterfaceCodec,
            ObjectServerType::class to CacheVarLocCodec,
            NpcServerType::class to CacheVarNpcCodec,
            ItemServerType::class to CacheVarObjCodec,
            ProjAnimType::class to CacheVarProjAnimCodec,
            SequenceServerType::class to CacheVarSeqCodec,
            SpotanimType::class to CacheVarSpotanimCodec,
            ItemServerType::class to CacheVarNamedObjCodec,
            String::class to CacheVarStringCodec,
            StatType::class to CacheVarStatCodec,
            SynthType::class to CacheVarSynthCodec,
            VarBitType::class to CacheVarVarBitCodec,
            VarpServerType::class to CacheVarVarpCodec,
        )

    public val CacheVarLiteral.codecOut: KClass<*>
        get() =
            when (this) {
                CacheVarLiteral.BOOLEAN -> Boolean::class
                CacheVarLiteral.ENTITYOVERLAY -> Int::class
                CacheVarLiteral.SEQ -> SequenceServerType::class
                CacheVarLiteral.COLOUR -> Int::class
                CacheVarLiteral.TOPLEVELINTERFACE -> Int::class
                CacheVarLiteral.LOCSHAPE -> Int::class
                CacheVarLiteral.COMPONENT -> ComponentType::class
                CacheVarLiteral.STRUCT -> Int::class
                CacheVarLiteral.IDKIT -> Int::class
                CacheVarLiteral.OVERLAYINTERFACE -> Int::class
                CacheVarLiteral.MIDI -> Int::class
                CacheVarLiteral.NPC_MODE -> Int::class
                CacheVarLiteral.NAMEDOBJ -> ItemServerType::class
                CacheVarLiteral.SYNTH -> SynthType::class
                CacheVarLiteral.AREA -> AreaType::class
                CacheVarLiteral.STAT -> StatType::class
                CacheVarLiteral.NPC_STAT -> Int::class
                CacheVarLiteral.MAPAREA -> Int::class
                CacheVarLiteral.INTERFACE -> InterfaceType::class
                CacheVarLiteral.COORDGRID -> CoordGrid::class
                CacheVarLiteral.GRAPHIC -> Int::class
                CacheVarLiteral.FONTMETRICS -> Int::class
                CacheVarLiteral.ENUM -> EnumType::class
                CacheVarLiteral.JINGLE -> Int::class
                CacheVarLiteral.INT -> Int::class
                CacheVarLiteral.LOC -> ObjectServerType::class
                CacheVarLiteral.MODEL -> Int::class
                CacheVarLiteral.NPC -> NpcServerType::class
                CacheVarLiteral.OBJ -> ItemServerType::class
                CacheVarLiteral.PLAYER_UID -> Int::class
                CacheVarLiteral.STRING -> String::class
                CacheVarLiteral.SPOTANIM -> SpotanimType::class
                CacheVarLiteral.NPC_UID -> Int::class
                CacheVarLiteral.INV -> Int::class
                CacheVarLiteral.TEXTURE -> Int::class
                CacheVarLiteral.CATEGORY -> CategoryType::class
                CacheVarLiteral.CHAR -> Int::class
                CacheVarLiteral.MAPELEMENT -> Int::class
                CacheVarLiteral.HITMARK -> HitSplatType::class
                CacheVarLiteral.HEADBAR -> HealthBarServerType::class
                CacheVarLiteral.STRINGVECTOR -> Int::class
                CacheVarLiteral.DBTABLE -> DBTableType::class
                CacheVarLiteral.DBROW -> DBRowType::class
                CacheVarLiteral.MOVESPEED -> Int::class
                ServerCacheManager.VARBIT -> VarBitType::class
                CacheVarLiteral.VARP -> VarpServerType::class
                ServerCacheManager.PROJANIM -> ProjAnimType::class
                else -> error("Error finding out codec")
            }

    public fun <K, V : Any> findCodec(literal: CacheVarLiteral): CacheVarCodec<K, V> =
        findCodec(literal.codecOut)

    @Suppress("UNCHECKED_CAST")
    public fun <K, V : Any> findCodec(type: KClass<*>): CacheVarCodec<K, V> =
        codecMap[type] as? CacheVarCodec<K, V>
            ?: error("CacheVarCodec for type is not implemented in `codecMap`: $type")
}
