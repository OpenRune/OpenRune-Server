package dev.openrune.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.buildCodeBlock
import dev.openrune.cache.gameval.GameValElement
import dev.openrune.cache.gameval.GameValHandler.elementAs
import dev.openrune.cache.gameval.impl.Table
import dev.openrune.definition.type.DBRowType
import dev.openrune.definition.type.DBTableType
import dev.openrune.definition.type.EnumType
import dev.openrune.definition.util.BaseVarType
import dev.openrune.definition.util.VarType
import java.io.File

private const val PKG_DB_COL = "dev.openrune.types.dbcol"
private const val PKG_ACONVERTED = "dev.openrune.types.aconverted"
private const val PKG_TABLES = "org.rsmod.api.table"
private const val OUT_TABLES_KT = "../api/tables/src/main/kotlin"

private val typeBoolean = Boolean::class.asTypeName()
private val typeCoordGrid = ClassName("org.rsmod.map", "CoordGrid")
private val typeList = ClassName("kotlin.collections", "List")

private val tableSubpackages =
    listOf(
            "fletching",
            "cluehelper",
            "fsw",
            "herblore",
            "woodcutting",
            "mining",
            "fishing",
            "cooking",
            "smithing",
            "crafting",
            "runecrafting",
            "agility",
            "thieving",
            "slayer",
            "construction",
            "hunter",
            "farming",
            "prayer",
            "magic",
            "ranged",
            "melee",
            "combat",
            "sailing",
        )
        .sortedByDescending { it.length }

data class TableColumn(
    val name: String,
    val simpleName: String,
    val varTypes: Map<Int, VarType>? = null,
    val optional: Boolean = false,
    val maxValues: Int = 0,
    val dbRowTargetTable: String? = null,
    val columnId: Int = 0,
)

data class TableDef(
    val tableName: String,
    val className: String,
    val columns: List<TableColumn>,
    val sourceTableId: Int,
)

private data class ColumnMetadata(
    val slotTypes: MutableMap<Int, VarType> = mutableMapOf(),
    var optional: Boolean = false,
)

private fun Any?.asPositiveInt(): Int? =
    when (this) {
        is Int -> takeIf { it > 0 }
        is Number -> toInt().takeIf { it > 0 }
        else -> null
    }

private fun DbHelper.valuesAt(columnId: Int): List<Any>? =
    try {
        getColumn(columnId).column.values?.toList()
    } catch (_: DbException.MissingColumn) {
        null
    }

private fun rowWrapperClassName(tableName: String): String =
    tableName
        .split('_', '-', '.', ':')
        .filter { it.isNotBlank() }
        .joinToString("") { it.replaceFirstChar(Char::uppercase) } + "Row"

private fun tablesPackageFor(tableName: String): String {
    val lower = tableName.lowercase()
    val sub =
        tableSubpackages.firstOrNull { lower.startsWith(it) || "_$it" in lower || it in lower }
    return sub?.let { "$PKG_TABLES.$it" } ?: PKG_TABLES
}

private fun propertyName(columnName: String): String {
    val camel =
        columnName
            .split("_")
            .joinToString("") { w -> w.lowercase().replaceFirstChar(Char::uppercase) }
            .replaceFirstChar(Char::lowercase)
    return if (camel == "object") "objectID" else camel
}

private fun dbtableIdToName(elements: List<GameValElement>): Map<Int, String> =
    elements.mapNotNull { el -> el.elementAs<Table>()?.let { it.id to it.name } }.toMap()

private fun rowClassName(tableName: String): ClassName =
    ClassName(tablesPackageFor(tableName), rowWrapperClassName(tableName))

private fun sampleRowsForTable(tableId: Int, rows: Map<Int, DBRowType>): List<DbHelper> =
    DbQueryCache.getTable(tableId.toString()) {
        rows.values
            .asSequence()
            .filter { it.tableId == tableId }
            .map { DbHelper(it) }
            .distinctBy { it.id }
            .toList()
    }

private fun mergeColumnMetadata(
    table: Table,
    samples: List<DbHelper>,
): Map<String, ColumnMetadata> {
    val byName = table.columns.associate { it.name to ColumnMetadata() }.toMutableMap()
    for (row in samples) {
        for (col in table.columns) {
            val meta = byName.getValue(col.name)
            try {
                row.getColumn(col.id).types.forEachIndexed { i, t -> meta.slotTypes[i] = t }
            } catch (_: DbException.MissingColumn) {
                meta.optional = true
            }
        }
    }
    return byName
}

private val TableColumn.slots: List<VarType>
    get() = varTypes?.toSortedMap()?.values?.toList().orEmpty()

private fun dominantDbRowTable(
    sourceTableId: Int,
    columnId: Int,
    slotTypes: List<VarType>,
    rows: Map<Int, DBRowType>,
    tableIdToName: Map<Int, String>,
): String? {
    if (slotTypes.isEmpty() || slotTypes.any { it != VarType.DBROW }) return null
    val stride = slotTypes.size
    val counts = mutableMapOf<Int, Int>()
    for (row in rows.values.filter { it.tableId == sourceTableId }) {
        val vals = DbHelper(row).valuesAt(columnId) ?: continue
        vals
            .chunked(stride)
            .filter { it.size == stride }
            .forEach { chunk ->
                chunk.forEach { raw ->
                    val ref = raw.asPositiveInt() ?: return@forEach
                    val tid = rows[ref]?.tableId ?: return@forEach
                    counts.merge(tid, 1, Int::plus)
                }
            }
    }
    val top = counts.values.maxOrNull() ?: return null
    return counts.filterValues { it == top }.keys.singleOrNull()?.let { tableIdToName[it] }
}

private fun codecNested(simple: String): ClassName =
    ClassName(PKG_DB_COL, "DbColumnCodec").nestedClass(simple)

private fun codecTopLevel(simple: String): ClassName = ClassName(PKG_DB_COL, simple)

private val nestedCodecByVarType: Map<VarType, String> =
    mapOf(
        VarType.BOOLEAN to "BooleanCodec",
        VarType.INT to "IntCodec",
        VarType.LONG to "IntCodec",
        VarType.STRING to "StringCodec",
        VarType.NPC to "NpcTypeCodec",
        VarType.LOC to "LocTypeCodec",
        VarType.OBJ to "ItemServerTypeCodec",
        VarType.COORDGRID to "CoordGridCodec",
        VarType.DBROW to "DbRowTypeCodec",
        VarType.STAT to "StatTypeCodec",
        VarType.COMPONENT to "ComponentTypeCodec",
        VarType.ENUM to "EnumTypeIdCodec",
        VarType.MIDI to "MidiTypeCodec",
        VarType.AREA to "AreaTypeCodec",
        VarType.INTERFACE to "InterfaceTypeCodec",
    )

private val topLevelCodecByVarType: Map<VarType, String> =
    mapOf(
        VarType.MAPELEMENT to "MapElementIdCodec",
        VarType.NAMEDOBJ to "NamedObjIdCodec",
        VarType.GRAPHIC to "GraphicIdCodec",
        VarType.SEQ to "SeqIdCodec",
        VarType.MODEL to "ModelIdCodec",
        VarType.CATEGORY to "CategoryIdCodec",
        VarType.INV to "InvIdCodec",
        VarType.IDKIT to "IdkIdCodec",
        VarType.VARP to "VarpIdCodec",
        VarType.STRUCT to "StructIdCodec",
        VarType.DBTABLE to "DbtableIdCodec",
        VarType.SYNTH to "SynthIdCodec",
        VarType.LOCSHAPE to "LocShapeIdCodec",
    )

private fun codecClass(vt: VarType): ClassName =
    nestedCodecByVarType[vt]?.let(::codecNested)
        ?: topLevelCodecByVarType[vt]?.let(::codecTopLevel)
        ?: codecNested("IntCodec")

private val scalarReaderByVarType: Map<VarType, String> =
    mapOf(
        VarType.BOOLEAN to "boolean",
        VarType.INT to "int",
        VarType.STRING to "string",
        VarType.LONG to "long",
        VarType.COORDGRID to "coord",
        VarType.AREA to "area",
        VarType.COMPONENT to "component",
        VarType.DBROW to "dbRow",
        VarType.INTERFACE to "interf",
        VarType.LOC to "loc",
        VarType.MIDI to "midi",
        VarType.NPC to "npc",
        VarType.OBJ to "obj",
        VarType.STAT to "stat",
        VarType.ENUM to "enumTypeId",
    )

private val scalarOptionalReaderByVarType: Map<VarType, String> =
    mapOf(
        VarType.BOOLEAN to "booleanOptional",
        VarType.INT to "intOptional",
        VarType.STRING to "stringOptional",
        VarType.LONG to "longOptional",
        VarType.ENUM to "enumTypeIdOptional",
        VarType.NPC to "npcOptional",
        VarType.OBJ to "objOptional",
    )

private fun kotlinType(vt: VarType, nullable: Boolean): TypeName {
    val base =
        when (vt) {
            VarType.BOOLEAN -> typeBoolean
            VarType.INT,
            VarType.ENUM -> INT
            VarType.STRING -> STRING
            VarType.LONG -> LONG
            VarType.COORDGRID -> typeCoordGrid
            VarType.AREA -> ClassName("dev.openrune.types.aconverted", "AreaType")
            VarType.COMPONENT -> ClassName("dev.openrune.definition.type.widget", "ComponentType")
            VarType.DBROW -> ClassName("dev.openrune.definition.type", "DBRowType")
            VarType.INTERFACE -> ClassName("dev.openrune.types.interf", "InterfaceType")
            VarType.LOC -> ClassName("dev.openrune.types", "ObjectServerType")
            VarType.MIDI -> ClassName("dev.openrune.types.aconverted", "MidiType")
            VarType.NPC -> ClassName("dev.openrune.types", "NpcServerType")
            VarType.OBJ -> ClassName("dev.openrune.types", "ItemServerType")
            VarType.STAT -> ClassName("dev.openrune.types", "StatType")
            else ->
                when (vt.baseType!!) {
                    BaseVarType.INTEGER -> INT
                    BaseVarType.STRING -> STRING
                    BaseVarType.LONG -> LONG
                    else -> INT
                }
        }
    return if (nullable) base.copy(nullable = true) else base
}

/**
 * `row.$fn(%S, %T, …)` through closing `)`; [afterClose] appends e.g. `.map { … }` (use extra `%T`
 * for row type).
 */
private fun rowWithCodecsFormat(fn: String, codecs: List<ClassName>, afterClose: String = ")") =
    "row.$fn(%S" + codecs.joinToString("") { ", %T" } + afterClose

private fun rowWithCodecsArgs(col: String, codecs: List<ClassName>) =
    listOf<Any>(col).plus(codecs).toTypedArray()

private fun distinctEnumIdsInColumn(
    tableId: Int,
    columnId: Int,
    rows: Map<Int, DBRowType>,
): List<Int> =
    rows.values
        .asSequence()
        .filter { it.tableId == tableId }
        .mapNotNull { DbHelper(it).valuesAt(columnId) }
        .flatMap { it.asSequence() }
        .mapNotNull { it.asPositiveInt() }
        .distinct()
        .sorted()
        .toList()

private fun templateEnumId(table: DBTableType?, columnId: Int): Int? =
    table?.columns?.get(columnId)?.values?.firstOrNull()?.asPositiveInt()

private fun unifiedEnumKeyValueTypes(
    enumIds: List<Int>,
    enums: Map<Int, EnumType>,
    templateId: Int?,
): Pair<VarType, VarType>? {
    val fromRows =
        enumIds
            .mapNotNull { id -> enums[id]?.let { it.keyType to it.valueType } }
            .distinct()
            .singleOrNull()
    return fromRows ?: templateId?.let { id -> enums[id]?.let { it.keyType to it.valueType } }
}

fun startGeneration(
    elements: List<GameValElement>,
    rows: MutableMap<Int, DBRowType>,
    enums: MutableMap<Int, EnumType>,
    dbtables: Map<Int, DBTableType>,
) {
    val outDir = File(OUT_TABLES_KT)
    val tableIdToName = dbtableIdToName(elements)
    for (el in elements) {
        val table = el.elementAs<Table>() ?: continue
        val samples = sampleRowsForTable(table.id, rows)
        val meta = mergeColumnMetadata(table, samples)
        val maxLen =
            table.columns.associate { c ->
                c.name to (samples.maxOfOrNull { it.valuesAt(c.id)?.size ?: 0 } ?: 0)
            }
        val columns =
            table.columns.map { c ->
                val m = meta.getValue(c.name)
                val slotList = m.slotTypes.toSortedMap().values.toList()
                TableColumn(
                    name = "dbcol.${table.name}:${c.name}",
                    simpleName = c.name,
                    varTypes = m.slotTypes,
                    optional = m.optional,
                    maxValues = maxLen.getValue(c.name),
                    dbRowTargetTable =
                        dominantDbRowTable(table.id, c.id, slotList, rows, tableIdToName),
                    columnId = c.id,
                )
            }
        generateTable(
            TableDef(table.name, rowWrapperClassName(table.name), columns, table.id),
            outDir,
            rows,
            enums,
            dbtables,
        )
    }
}

private val tupleAritiesGlobal = mutableSetOf<Int>()

fun generateTable(
    def: TableDef,
    outputDir: File,
    rows: Map<Int, DBRowType>,
    enums: Map<Int, EnumType>,
    dbtables: Map<Int, DBTableType>,
) {
    val dbHelper = ClassName(PKG_DB_COL, "DbHelper")
    val rscm = ClassName("dev.openrune.rscm", "RSCM")
    val rscmType = ClassName("dev.openrune.rscm", "RSCMType")
    val pkg = tablesPackageFor(def.tableName)
    val rowType = ClassName(pkg, def.className)

    val extImports = linkedSetOf<String>()
    val tupleUsed = mutableSetOf<Int>()
    val tupleAsLists = mutableSetOf<Int>()
    val crossRowTypes = mutableSetOf<ClassName>()
    val aconvertedImports = mutableSetOf<String>()

    val rowSpec =
        TypeSpec.classBuilder(def.className)
            .primaryConstructor(FunSpec.constructorBuilder().addParameter("row", dbHelper).build())
            .apply {
                RowPropertyCodegen(
                        def,
                        this,
                        typeList,
                        extImports,
                        tupleUsed,
                        tupleAsLists,
                        crossRowTypes,
                        aconvertedImports,
                    )
                    .emitAll(rows, enums, dbtables)
                addProperty(
                    PropertySpec.builder("rowId", INT)
                        .addModifiers(KModifier.PUBLIC)
                        .initializer("row.id")
                        .build()
                )
                addProperty(
                    PropertySpec.builder("tableId", INT)
                        .addModifiers(KModifier.PUBLIC)
                        .initializer("row.tableId")
                        .build()
                )
                addType(rowCompanion(rowType, dbHelper, def.tableName, typeList, rscm, rscmType))
            }
            .build()

    FileSpec.builder(pkg, def.className)
        .addFileComment("AUTO-GENERATED for dbtable.${def.tableName} — do not edit.")
        .addImport(PKG_DB_COL, "DbHelper")
        .addImport("dev.openrune.rscm", "RSCM", "RSCMType")
        .addImport("dev.openrune.rscm.RSCM", "asRSCM")
        .apply {
            if (extImports.isNotEmpty()) addImport(PKG_DB_COL, *extImports.sorted().toTypedArray())
            if (aconvertedImports.isNotEmpty())
                addImport(PKG_ACONVERTED, *aconvertedImports.sorted().toTypedArray())
            crossRowTypes.forEach { addImport(it.packageName, it.simpleName) }
            tupleUsed.forEach { addImport(PKG_TABLES, "Tuple$it", "toTuple$it") }
            tupleAsLists.forEach { addImport(PKG_TABLES, "toListOfTuple$it") }
        }
        .addType(rowSpec)
        .build()
        .writeTo(outputDir)

    generateTupleHelpers(outputDir, tupleAritiesGlobal)
}

private fun rowCompanion(
    rowType: ClassName,
    dbHelper: ClassName,
    tableName: String,
    listType: ClassName,
    rscm: ClassName,
    rscmType: ClassName,
): TypeSpec =
    TypeSpec.companionObjectBuilder()
        .addFunction(
            FunSpec.builder("all")
                .returns(listType.parameterizedBy(rowType))
                .addStatement(
                    "return %T.table(%S).map { %T(it) }",
                    dbHelper,
                    "dbtable.$tableName",
                    rowType,
                )
                .build()
        )
        .addFunction(
            FunSpec.builder("getRow")
                .addParameter("row", INT)
                .returns(rowType)
                .addStatement("return %T(%T.row(row))", rowType, dbHelper)
                .build()
        )
        .addFunction(
            FunSpec.builder("getRow")
                .addParameter("column", String::class)
                .returns(rowType)
                .addStatement("%T.requireRSCM(%T.DBROW, column)", rscm, rscmType)
                .addStatement("return getRow(column.asRSCM() and 0xFFFF)")
                .build()
        )
        .build()

private class RowPropertyCodegen(
    private val def: TableDef,
    private val row: TypeSpec.Builder,
    private val listType: ClassName,
    private val extImports: MutableSet<String>,
    private val tupleUsed: MutableSet<Int>,
    private val tupleAsLists: MutableSet<Int>,
    private val crossRowTypes: MutableSet<ClassName>,
    private val aconvertedImports: MutableSet<String>,
) {
    private fun typeFor(col: TableColumn, vt: VarType, nullable: Boolean): TypeName =
        if (vt == VarType.DBROW && col.dbRowTargetTable != null) {
            rowClassName(col.dbRowTargetTable).let {
                if (nullable) it.copy(nullable = true) else it
            }
        } else {
            kotlinType(vt, nullable)
        }

    private fun selfReferentialDbRow(col: TableColumn): Boolean =
        col.dbRowTargetTable != null && col.dbRowTargetTable == def.tableName

    private fun addProp(name: String, type: TypeName, init: CodeBlock, lazyInit: Boolean = false) {
        val spec = PropertySpec.builder(name, type).addModifiers(KModifier.PUBLIC)
        if (lazyInit) {
            spec.delegate(
                buildCodeBlock {
                    add("lazy { ")
                    add(init)
                    add(" }")
                }
            )
        } else {
            spec.initializer(init)
        }
        row.addProperty(spec.build())
    }

    private fun emitEnumColumn(
        col: TableColumn,
        prop: String,
        rows: Map<Int, DBRowType>,
        enums: Map<Int, EnumType>,
        dbtables: Map<Int, DBTableType>,
    ) {
        val templateId = templateEnumId(dbtables[def.sourceTableId], col.columnId)
        val ids =
            (distinctEnumIdsInColumn(def.sourceTableId, col.columnId, rows).toSet() +
                    listOfNotNull(templateId))
                .sorted()
        val kv = unifiedEnumKeyValueTypes(ids, enums, templateId)
        if (kv != null) {
            val (k, v) = kv
            val pair =
                ClassName(PKG_ACONVERTED, "EnumPair")
                    .parameterizedBy(kotlinType(k, false), kotlinType(v, false))
            val listOfPairs = listType.parameterizedBy(pair)
            val fn = if (col.optional) "enumOptional" else "enum"
            extImports += fn
            aconvertedImports += "EnumPair"
            addProp(
                prop,
                if (col.optional) listOfPairs.copy(nullable = true) else listOfPairs,
                CodeBlock.of("row.$fn(%S, %T, %T)", col.name, codecClass(k), codecClass(v)),
            )
            return
        }
        extImports += if (col.optional) "enumTypeIdOptional" else "enumTypeId"
        addProp(
            prop,
            if (col.optional) INT.copy(nullable = true) else INT,
            CodeBlock.of(
                if (col.optional) "row.enumTypeIdOptional(%S)" else "row.enumTypeId(%S)",
                col.name,
            ),
        )
    }

    fun emitAll(
        rows: Map<Int, DBRowType>,
        enums: Map<Int, EnumType>,
        dbtables: Map<Int, DBTableType>,
    ) {
        for (col in def.columns) {
            val slots = col.slots
            if (slots.isEmpty()) continue
            val prop = propertyName(col.simpleName)
            val multi = col.maxValues > 1
            when {
                slots.singleOrNull() == VarType.ENUM ->
                    emitEnumColumn(col, prop, rows, enums, dbtables)
                slots.distinct().size > 1 -> emitMixedSlots(col, slots, multi, prop)
                else -> emitUniformSlots(col, slots, slots.first(), multi, prop)
            }
        }
    }

    private fun emitMixedSlots(
        col: TableColumn,
        slots: List<VarType>,
        multi: Boolean,
        prop: String,
    ) {
        val n = slots.size
        tupleAritiesGlobal.add(n)
        tupleUsed += n
        val tupleClass = ClassName(PKG_TABLES, "Tuple$n")
        val fn = if (col.optional) "multiColumnMixedOptional" else "multiColumnMixed"
        extImports += fn
        val codecs = slots.map(::codecClass)
        val head = rowWithCodecsFormat(fn, codecs)
        val args = rowWithCodecsArgs(col.name, codecs)
        if (multi) {
            tupleAsLists += n
            val tupleT =
                tupleClass.parameterizedBy(*slots.map { kotlinType(it, false) }.toTypedArray())
            addProp(
                prop,
                listType.parameterizedBy(tupleT),
                CodeBlock.of("$head.toListOfTuple$n()", *args),
            )
        } else {
            val tupleT =
                tupleClass.parameterizedBy(
                    *slots.map { kotlinType(it, col.optional) }.toTypedArray()
                )
            val init =
                if (col.optional) {
                    CodeBlock.of("$head.toTuple$n()", *args)
                } else {
                    CodeBlock.of(
                        "$head.toTuple$n() ?: error(\"Column \${%S} returned empty list but is not optional\")",
                        *args,
                        col.name,
                    )
                }
            addProp(prop, if (col.optional) tupleT.copy(nullable = true) else tupleT, init)
        }
    }

    private fun emitUniformSlots(
        col: TableColumn,
        slots: List<VarType>,
        first: VarType,
        multi: Boolean,
        prop: String,
    ) {
        val coordScalar = first == VarType.COORDGRID && !multi
        val ty =
            when {
                coordScalar ->
                    if (col.optional) typeCoordGrid.copy(nullable = true) else typeCoordGrid
                multi -> LIST.parameterizedBy(typeFor(col, first, false))
                else -> typeFor(col, first, col.optional)
            }
        val init =
            when {
                coordScalar -> coordInit(col)
                multi && col.optional -> slotsOptionalInit(col, slots)
                multi -> listOrMultiInit(col, slots, first)
                else -> scalarInit(col, first)
            }
        val lazyInit =
            when {
                coordScalar -> false
                multi && col.optional ->
                    selfReferentialDbRow(col) && slots.all { it == VarType.DBROW }
                multi ->
                    selfReferentialDbRow(col) &&
                        first == VarType.DBROW &&
                        slots.all { it == VarType.DBROW }
                else -> first == VarType.DBROW && selfReferentialDbRow(col)
            }
        addProp(prop, ty, init, lazyInit)
    }

    private fun coordInit(col: TableColumn): CodeBlock =
        if (col.optional) {
            extImports += "columnOptional"
            CodeBlock.of("row.columnOptional(%S, %T)", col.name, codecNested("CoordGridCodec"))
        } else {
            extImports += "coord"
            CodeBlock.of("row.coord(%S)", col.name)
        }

    private fun slotsOptionalInit(col: TableColumn, slots: List<VarType>): CodeBlock {
        extImports += "slotsOptional"
        val ic = codecNested("IntCodec")
        return when {
            slots.all { it == VarType.INT } && slots.size <= 1 ->
                CodeBlock.of("row.slotsOptional(%S, %T)", col.name, ic)
            slots.all { it == VarType.INT } ->
                List(slots.size) { ic }
                    .let { codecs ->
                        CodeBlock.of(
                            rowWithCodecsFormat("slotsOptional", codecs),
                            *rowWithCodecsArgs(col.name, codecs),
                        )
                    }
            slots.all { it == VarType.DBROW } && col.dbRowTargetTable != null -> {
                val target = rowClassName(col.dbRowTargetTable)
                crossRowTypes += target
                CodeBlock.of(
                    "row.slotsOptional(%S, %T).map { %T.getRow(it.id) }",
                    col.name,
                    codecNested("DbRowTypeCodec"),
                    target,
                )
            }
            slots.all { it == VarType.DBROW } ->
                CodeBlock.of("row.slotsOptional(%S, %T)", col.name, codecNested("DbRowTypeCodec"))
            else ->
                CodeBlock.of(
                    rowWithCodecsFormat("slotsOptional", slots.map(::codecClass)),
                    *rowWithCodecsArgs(col.name, slots.map(::codecClass)),
                )
        }
    }

    private fun listOrMultiInit(col: TableColumn, slots: List<VarType>, first: VarType): CodeBlock {
        val dbRowOnly = first == VarType.DBROW && slots.all { it == VarType.DBROW }
        val targetName = col.dbRowTargetTable
        if (dbRowOnly && targetName != null) {
            val target = rowClassName(targetName)
            crossRowTypes += target
            val rowCodec = codecNested("DbRowTypeCodec")
            return if (slots.size == 1) {
                extImports += "list"
                CodeBlock.of(
                    "row.list(%S, %T).map { %T.getRow(it.id) }",
                    col.name,
                    rowCodec,
                    target,
                )
            } else {
                extImports += "multiColumn"
                val codecs = slots.map(::codecClass)
                CodeBlock.of(
                    rowWithCodecsFormat("multiColumn", codecs, ").map { %T.getRow(it.id) }"),
                    *rowWithCodecsArgs(col.name, codecs),
                    target,
                )
            }
        }
        return if (slots.size == 1) {
            extImports += "list"
            val c =
                when (first) {
                    VarType.INT -> codecNested("IntCodec")
                    VarType.DBROW -> codecNested("DbRowTypeCodec")
                    else -> codecClass(first)
                }
            CodeBlock.of("row.list(%S, %T)", col.name, c)
        } else {
            extImports += "multiColumn"
            val codecs = slots.map(::codecClass)
            CodeBlock.of(
                rowWithCodecsFormat("multiColumn", codecs),
                *rowWithCodecsArgs(col.name, codecs),
            )
        }
    }

    private fun scalarInit(col: TableColumn, vt: VarType): CodeBlock {
        if (vt == VarType.DBROW && col.dbRowTargetTable != null) {
            val target = rowClassName(col.dbRowTargetTable)
            crossRowTypes += target
            return if (!col.optional) {
                extImports += "dbRow"
                CodeBlock.of("%T.getRow(row.dbRow(%S).id)", target, col.name)
            } else {
                extImports += "columnOptional"
                CodeBlock.of(
                    "row.columnOptional(%S, %T)?.let { %T.getRow(it.id) }",
                    col.name,
                    codecNested("DbRowTypeCodec"),
                    target,
                )
            }
        }
        val req = scalarReaderByVarType[vt]
        val opt = scalarOptionalReaderByVarType[vt]
        val codec = codecClass(vt)
        return when {
            req != null && !col.optional -> {
                extImports += req
                CodeBlock.of("row.$req(%S)", col.name)
            }
            opt != null && col.optional -> {
                extImports += opt
                CodeBlock.of("row.$opt(%S)", col.name)
            }
            col.optional -> {
                extImports += "columnOptional"
                CodeBlock.of("row.columnOptional(%S, %T)", col.name, codec)
            }
            else -> {
                extImports += "column"
                CodeBlock.of("row.column(%S, %T)", col.name, codec)
            }
        }
    }
}

fun generateTupleHelpers(outputDir: File, arities: MutableSet<Int>) {
    FileSpec.builder(PKG_TABLES, "Tuples")
        .addFileComment("AUTO-GENERATED tuple helpers for db table rows — do not edit.")
        .apply { arities.forEach { addTupleArity(this, it) } }
        .build()
        .writeTo(outputDir)
}

private fun addTupleArity(file: FileSpec.Builder, n: Int) {
    val tParams = (0 until n).map { "T$it" }
    val tupleName = ClassName(PKG_TABLES, "Tuple$n")
    val typeVars = tParams.map { TypeVariableName(it) }
    val ctor =
        FunSpec.constructorBuilder()
            .apply { tParams.forEach { tp -> addParameter(tp.lowercase(), TypeVariableName(tp)) } }
            .build()
    file.addType(
        TypeSpec.classBuilder("Tuple$n")
            .addModifiers(KModifier.PUBLIC, KModifier.DATA)
            .primaryConstructor(ctor)
            .apply {
                tParams.forEach { tp ->
                    val tv = TypeVariableName(tp)
                    addTypeVariable(tv)
                    addProperty(
                        PropertySpec.builder(tp.lowercase(), tv).initializer(tp.lowercase()).build()
                    )
                }
            }
            .build()
    )
    val tupleInst = tupleName.parameterizedBy(typeVars)
    val starList = typeList.parameterizedBy(STAR)
    file.addFunction(
        FunSpec.builder("toTuple$n")
            .receiver(starList)
            .addTypeVariables(typeVars)
            .returns(tupleInst.copy(nullable = true))
            .addCode(
                buildCodeBlock {
                    add("if (size < %L) return null\n", n)
                    add("return %T(", tupleName)
                    tParams.forEachIndexed { i, tp ->
                        if (i > 0) add(", ")
                        add("this[%L] as %T", i, TypeVariableName(tp))
                    }
                    add(")\n")
                }
            )
            .build()
    )
    file.addFunction(
        FunSpec.builder("toListOfTuple$n")
            .receiver(starList)
            .addTypeVariables(typeVars)
            .returns(typeList.parameterizedBy(tupleInst))
            .addCode(
                "return chunked(%L).mapNotNull { it.toTuple%L<%L>() }\n",
                n,
                n,
                tParams.joinToString(),
            )
            .build()
    )
}
