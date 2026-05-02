package dev.openrune.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock
import dev.openrune.cache.gameval.GameValElement
import dev.openrune.cache.gameval.GameValHandler.elementAs
import dev.openrune.cache.gameval.impl.Table
import dev.openrune.definition.type.DBRowType
import dev.openrune.definition.type.DBTableType
import dev.openrune.definition.type.EnumType
import dev.openrune.definition.util.VarType
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.dbcol.DbHelper as DbColHelper
import java.io.File

private const val PKG_DB_COL = "dev.openrune.types.dbcol"
private const val PKG_ACONVERTED = "dev.openrune.types.aconverted"
private const val PKG_TABLES = "org.rsmod.api.table"
private const val PKG_OPENRUNE_ENUMS = "dev.openrune.types.enums"
private const val OUT_TABLES_KT = "../api/generated/src/main/kotlin"

private val cnameEnumTypeMap = ClassName("dev.openrune.types.enums", "EnumTypeMap")

// region Models & naming (gameval / row wrapper)

private val typeList = ClassName("kotlin.collections", "List")

private val classNameDbColHelper = DbColHelper::class.asClassName()

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

// endregion
// region Column sampling (infer optional columns, slot types, DBROW targets)

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

// endregion
// region Cleanup, enum metadata, `row.*(%S, %T)` format helpers

/** Clears all `*.kt` under [root], then empty dirs, so each generator run starts clean. */
private fun clearGeneratedTableSources(root: File) {
    if (!root.exists()) {
        root.mkdirs()
        return
    }
    check(root.isDirectory) { "Table output path is not a directory: ${root.absolutePath}" }
    root.walkBottomUp()
        .filter { it.isFile && it.extension.equals("kt", ignoreCase = true) }
        .forEach { it.delete() }
    root.walkBottomUp()
        .filter { it.isDirectory && it != root && it.listFiles().isNullOrEmpty() }
        .forEach { it.delete() }
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

// endregion
// region Orchestration

/** Tuple arities seen across all tables in one [startGeneration] run (written once at the end). */
private val tupleAritiesGlobal = mutableSetOf<Int>()

/**
 * Emits typed row wrappers under [OUT_TABLES_KT]. Wipes prior `*.kt` there first, then writes
 * `Tuples.kt` once with all tuple helpers used by any table.
 */
fun startGeneration(
    elements: List<GameValElement>,
    rows: MutableMap<Int, DBRowType>,
    enums: MutableMap<Int, EnumType>,
    dbtables: Map<Int, DBTableType>,
) {
    val outDir = File(OUT_TABLES_KT).canonicalFile
    clearGeneratedTableSources(outDir)
    tupleAritiesGlobal.clear()
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
    generateTupleHelpers(outDir, tupleAritiesGlobal)
}

/** Writes one `*Row` file + companion for a single dbtable. */
fun generateTable(
    def: TableDef,
    outputDir: File,
    rows: Map<Int, DBRowType>,
    enums: Map<Int, EnumType>,
    dbtables: Map<Int, DBTableType>,
) {
    val dbHelper = classNameDbColHelper
    val rscm = RSCM::class.asClassName()
    val rscmType = RSCMType::class.asClassName()
    val pkg = tablesPackageFor(def.tableName)
    val rowType = ClassName(pkg, def.className)

    val extImports = linkedSetOf<String>()
    val tupleUsed = mutableSetOf<Int>()
    val tupleAsLists = mutableSetOf<Int>()
    val crossRowTypes = mutableSetOf<ClassName>()
    val aconvertedImports = mutableSetOf<String>()
    val extraClassImports = mutableSetOf<ClassName>()
    val extraTopLevelImports = linkedSetOf<Pair<String, String>>()

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
                        extraClassImports,
                        extraTopLevelImports,
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
        .addImportClass(dbHelper)
        .addImportClass(rscm)
        .addImportClass(rscmType)
        .addImport(rscm, "asRSCM")
        .apply {
            if (extImports.isNotEmpty()) addImport(PKG_DB_COL, *extImports.sorted().toTypedArray())
            if (aconvertedImports.isNotEmpty())
                addImport(PKG_ACONVERTED, *aconvertedImports.sorted().toTypedArray())
            crossRowTypes.forEach { addImportClass(it) }
            extraClassImports.forEach { addImportClass(it) }
            extraTopLevelImports
                .sortedBy { (p, n) -> "$p.$n" }
                .forEach { (p, n) -> addImport(p, n) }
            tupleUsed.forEach { addImport(PKG_TABLES, "Tuple$it", "toTuple$it") }
            tupleAsLists.forEach { addImport(PKG_TABLES, "toListOfTuple$it") }
        }
        .addType(rowSpec)
        .build()
        .writeTo(outputDir)
}

// endregion
// region Companion + tuple file

private fun rowCompanion(
    rowType: ClassName,
    dbHelper: TypeName,
    tableName: String,
    listType: ClassName,
    rscm: TypeName,
    rscmType: TypeName,
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

/** Builds `public val` properties on each row wrapper from column metadata. */
private class RowPropertyCodegen(
    private val def: TableDef,
    private val row: TypeSpec.Builder,
    private val listType: ClassName,
    private val extImports: MutableSet<String>,
    private val tupleUsed: MutableSet<Int>,
    private val tupleAsLists: MutableSet<Int>,
    private val crossRowTypes: MutableSet<ClassName>,
    private val aconvertedImports: MutableSet<String>,
    private val extraClassImports: MutableSet<ClassName>,
    private val extraTopLevelImports: MutableSet<Pair<String, String>>,
) {
    private fun typeFor(col: TableColumn, vt: VarType, nullable: Boolean): TypeName =
        if (vt == VarType.DBROW && col.dbRowTargetTable != null) {
            rowClassName(col.dbRowTargetTable).let {
                if (nullable) it.copy(nullable = true) else it
            }
        } else {
            kotlinTypeForVarType(vt, nullable)
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
            extraClassImports += cnameEnumTypeMap
            extraTopLevelImports += PKG_OPENRUNE_ENUMS to "enum"
            val slugId =
                when {
                    templateId != null && enums.containsKey(templateId) -> templateId
                    else -> ids.first { enums.containsKey(it) }
                }
            val outerDef = enums.getValue(slugId)
            val (kType, vType) = kotlinTypePairForEnumDefinition(outerDef, enums, cnameEnumTypeMap)
            referencedOpenRuneClassNames(kType).forEach { extraClassImports += it }
            referencedOpenRuneClassNames(vType).forEach { extraClassImports += it }
            val mapType = cnameEnumTypeMap.parameterizedBy(kType, vType)
            if (col.optional) {
                extImports += "enumTypeIdOptional"
                addProp(
                    prop,
                    mapType.copy(nullable = true),
                    CodeBlock.of("row.enumTypeIdOptional(%S)?.let { enum(it) }", col.name),
                )
            } else {
                extImports += "enumTypeId"
                addProp(prop, mapType, CodeBlock.of("enum(row.enumTypeId(%S))", col.name))
            }
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
        val codecs = slots.map(::codecClassForVarType)
        val head = rowWithCodecsFormat(fn, codecs)
        val args = rowWithCodecsArgs(col.name, codecs)
        if (multi) {
            tupleAsLists += n
            val tupleT =
                tupleClass.parameterizedBy(*slots.map { kotlinTypeForVarType(it, false) }.toTypedArray())
            addProp(
                prop,
                listType.parameterizedBy(tupleT),
                CodeBlock.of("$head.toListOfTuple$n()", *args),
            )
        } else {
            val tupleT =
                tupleClass.parameterizedBy(
                    *slots.map { kotlinTypeForVarType(it, col.optional) }.toTypedArray()
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
                    if (col.optional) {
                        tableCodegenCoordGridType.copy(nullable = true)
                    } else {
                        tableCodegenCoordGridType
                    }
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
                    rowWithCodecsFormat("slotsOptional", slots.map(::codecClassForVarType)),
                    *rowWithCodecsArgs(col.name, slots.map(::codecClassForVarType)),
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
                val codecs = slots.map(::codecClassForVarType)
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
                    else -> codecClassForVarType(first)
                }
            CodeBlock.of("row.list(%S, %T)", col.name, c)
        } else {
            extImports += "multiColumn"
            val codecs = slots.map(::codecClassForVarType)
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
        val (req, opt) = scalarDbHelperReaders(vt)
        val codec = codecClassForVarType(vt)
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
