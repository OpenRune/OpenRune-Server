package dev.openrune.codegen

/**
 * VarType → KotlinPoet [TypeName], DB column codec class, and scalar reader names for generated
 * row wrappers.
 */
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import dev.openrune.cache.filestore.definition.InterfaceType
import dev.openrune.definition.type.DBRowType
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.definition.util.BaseVarType
import dev.openrune.definition.util.VarType
import dev.openrune.types.ItemServerType
import dev.openrune.types.NpcServerType
import dev.openrune.types.ObjectServerType
import dev.openrune.types.SequenceServerType
import dev.openrune.types.StatType
import dev.openrune.types.aconverted.AreaType
import dev.openrune.types.aconverted.MidiType
import dev.openrune.types.aconverted.SpotanimType

private const val PKG_DB_COL = "dev.openrune.types.dbcol"

/** Nested under [dev.openrune.types.dbcol.DbColumnCodec] (e.g. `IntCodec`). */
internal fun codecNested(simple: String): ClassName =
    ClassName(PKG_DB_COL, "DbColumnCodec").nestedClass(simple)

/** Top-level codec in `types.dbcol` (e.g. `SeqIdCodec`). */
internal fun codecTopLevel(simple: String): ClassName = ClassName(PKG_DB_COL, simple)

private enum class CodecPlacement {
    Nested,
    TopLevel,
}

/**
 * Maps a [VarType] to Kotlin property type, DB column codec(s), and optional scalar reader names on
 * [dev.openrune.types.dbcol.DbColHelper] / `DbHelper` (runtime).
 */
private data class VarTypeCodegen(
    /** `null` → use [VarType.baseType] in [kotlinTypeForVarType]. */
    val kotlin: TypeName? = null,
    val codec: Pair<CodecPlacement, String>? = null,
    val scalarReader: String? = null,
    val scalarOptionalReader: String? = null,
)

private val typeBoolean = Boolean::class.asTypeName()
private val typeCoordGrid = ClassName("org.rsmod.map", "CoordGrid")
private val typeArea = AreaType::class.asTypeName()
private val typeMidi = MidiType::class.asTypeName()
private val typeComponent = ComponentType::class.asTypeName()
private val typeDbRow = DBRowType::class.asTypeName()
private val typeInterface = InterfaceType::class.asTypeName()
private val typeLoc = ObjectServerType::class.asTypeName()
private val typeNpc = NpcServerType::class.asTypeName()
private val typeObj = ItemServerType::class.asTypeName()
private val typeStat = StatType::class.asTypeName()
private val typeSeq = SequenceServerType::class.asTypeName()
private val typeSpot = SpotanimType::class.asTypeName()

private fun varTypeCodegen(vt: VarType): VarTypeCodegen =
    when (vt) {
        VarType.BOOLEAN ->
            VarTypeCodegen(
                typeBoolean,
                CodecPlacement.Nested to "BooleanCodec",
                "boolean",
                "booleanOptional",
            )
        VarType.INT ->
            VarTypeCodegen(
                kotlin = null,
                codec = CodecPlacement.Nested to "IntCodec",
                scalarReader = "int",
                scalarOptionalReader = "intOptional",
            )
        VarType.LONG ->
            VarTypeCodegen(
                LONG,
                CodecPlacement.Nested to "IntCodec",
                "long",
                "longOptional",
            )
        VarType.STRING ->
            VarTypeCodegen(
                STRING,
                CodecPlacement.Nested to "StringCodec",
                "string",
                "stringOptional",
            )
        VarType.SEQ ->
            VarTypeCodegen(
                typeSeq,
                CodecPlacement.Nested to "SeqCodec",
                scalarReader = "seq",
            )
        VarType.SPOTANIM ->
            VarTypeCodegen(
                typeSpot,
                CodecPlacement.Nested to "SpotCodec",
                scalarReader = "spot",
            )
        VarType.NPC ->
            VarTypeCodegen(
                typeNpc,
                CodecPlacement.Nested to "NpcTypeCodec",
                "npc",
                "npcOptional",
            )
        VarType.LOC ->
            VarTypeCodegen(
                typeLoc,
                CodecPlacement.Nested to "LocTypeCodec",
                scalarReader = "loc",
            )
        VarType.OBJ ->
            VarTypeCodegen(
                typeObj,
                CodecPlacement.Nested to "ItemServerTypeCodec",
                "obj",
                "objOptional",
            )
        VarType.COORDGRID ->
            VarTypeCodegen(
                typeCoordGrid,
                CodecPlacement.Nested to "CoordGridCodec",
                scalarReader = "coord",
            )
        VarType.DBROW ->
            VarTypeCodegen(
                typeDbRow,
                CodecPlacement.Nested to "DbRowTypeCodec",
                scalarReader = "dbRow",
            )
        VarType.STAT ->
            VarTypeCodegen(
                typeStat,
                CodecPlacement.Nested to "StatTypeCodec",
                scalarReader = "stat",
            )
        VarType.COMPONENT ->
            VarTypeCodegen(
                typeComponent,
                CodecPlacement.Nested to "ComponentTypeCodec",
                scalarReader = "component",
            )
        VarType.ENUM ->
            VarTypeCodegen(
                INT,
                CodecPlacement.Nested to "EnumTypeIdCodec",
                "enumTypeId",
                "enumTypeIdOptional",
            )
        VarType.MIDI ->
            VarTypeCodegen(
                typeMidi,
                CodecPlacement.Nested to "MidiTypeCodec",
                scalarReader = "midi",
            )
        VarType.AREA ->
            VarTypeCodegen(
                typeArea,
                CodecPlacement.Nested to "AreaTypeCodec",
                scalarReader = "area",
            )
        VarType.INTERFACE ->
            VarTypeCodegen(
                typeInterface,
                CodecPlacement.Nested to "InterfaceTypeCodec",
                scalarReader = "interf",
            )
        VarType.MAPELEMENT ->
            VarTypeCodegen(codec = CodecPlacement.TopLevel to "MapElementIdCodec")
        VarType.NAMEDOBJ ->
            VarTypeCodegen(codec = CodecPlacement.TopLevel to "NamedObjIdCodec")
        VarType.GRAPHIC ->
            VarTypeCodegen(codec = CodecPlacement.TopLevel to "GraphicIdCodec")
        VarType.SEQ -> VarTypeCodegen(codec = CodecPlacement.TopLevel to "SeqIdCodec")
        VarType.MODEL -> VarTypeCodegen(codec = CodecPlacement.TopLevel to "ModelIdCodec")
        VarType.CATEGORY ->
            VarTypeCodegen(codec = CodecPlacement.TopLevel to "CategoryIdCodec")
        VarType.INV -> VarTypeCodegen(codec = CodecPlacement.TopLevel to "InvIdCodec")
        VarType.IDKIT -> VarTypeCodegen(codec = CodecPlacement.TopLevel to "IdkIdCodec")
        VarType.VARP -> VarTypeCodegen(codec = CodecPlacement.TopLevel to "VarpIdCodec")
        VarType.STRUCT -> VarTypeCodegen(codec = CodecPlacement.TopLevel to "StructIdCodec")
        VarType.DBTABLE ->
            VarTypeCodegen(codec = CodecPlacement.TopLevel to "DbtableIdCodec")
        VarType.SYNTH -> VarTypeCodegen(codec = CodecPlacement.TopLevel to "SynthIdCodec")
        VarType.LOCSHAPE ->
            VarTypeCodegen(codec = CodecPlacement.TopLevel to "LocShapeIdCodec")
        else -> VarTypeCodegen()
    }

internal fun codecClassForVarType(vt: VarType): ClassName {
    val (placement, simple) = varTypeCodegen(vt).codec ?: return codecNested("IntCodec")
    return when (placement) {
        CodecPlacement.Nested -> codecNested(simple)
        CodecPlacement.TopLevel -> codecTopLevel(simple)
    }
}

internal fun kotlinTypeForVarType(vt: VarType, nullable: Boolean): TypeName {
    val gen = varTypeCodegen(vt)
    val base =
        gen.kotlin
            ?: when (vt.baseType!!) {
                BaseVarType.INTEGER -> INT
                BaseVarType.STRING -> STRING
                BaseVarType.LONG -> LONG
                else -> INT
            }
    return if (nullable) base.copy(nullable = true) else base
}

/** Exposed for row wrappers that special-case scalar coord (no codec in init). */
internal val tableCodegenCoordGridType: TypeName = typeCoordGrid

internal fun scalarDbHelperReaders(vt: VarType): Pair<String?, String?> =
    varTypeCodegen(vt).let { it.scalarReader to it.scalarOptionalReader }
