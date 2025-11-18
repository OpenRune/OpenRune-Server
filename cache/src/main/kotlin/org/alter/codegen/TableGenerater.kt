package org.alter.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import dev.openrune.ServerCacheManager
import dev.openrune.cache.gameval.GameValElement
import dev.openrune.cache.gameval.GameValHandler.elementAs
import dev.openrune.cache.gameval.impl.Table
import dev.openrune.definition.type.DBRowType
import dev.openrune.definition.util.BaseVarType
import dev.openrune.definition.util.VarType
import org.alter.ColInfo
import org.alter.game.util.DbException
import org.alter.game.util.DbHelper
import org.alter.game.util.DbHelper.Companion.table
import org.alter.game.util.DbQueryCache
import java.io.File

data class TableColumn(
    val name: String,
    val simpleName: String,
    val varTypes: MutableSet<VarType>?,
    val optional: Boolean = false
)

data class TableDef(
    val tableName: String,
    val className: String,
    val columns: List<TableColumn>
)

fun startGeneration(elements: List<GameValElement>,rows : MutableMap<Int, DBRowType>) {
    elements.forEach { element ->
        val table = element.elementAs<Table>() ?: return@forEach
        val tableName = table.name

        val colInfoMap = table.columns.associate { col ->
            col.name to ColInfo()
        }.toMutableMap()

        table(table.id,rows).forEach { row ->
            table.columns.forEach { col ->
                val info = colInfoMap[col.name]!!
                try {
                    row.getColumn(col.id).types.forEach { type ->
                        info.types.add(type)
                    }
                } catch (e: DbException.MissingColumn) {
                    info.optional = true
                }
            }
        }

        val generatedColumns = table.columns.map { col ->
            val info = colInfoMap[col.name]!!
            TableColumn(
                name = "columns.$tableName:${col.name}",
                simpleName = col.name,
                varTypes = info.types,
                optional = info.optional
            )
        }

        val tableDef = TableDef(
            tableName = tableName,
            className = formatClassName(tableName),
            columns = generatedColumns
        )

        generateTable(tableDef, File("../content/src/main/kotlin/generated"))
    }
}

private fun table(tableId: Int,rows: MutableMap<Int, DBRowType>): List<DbHelper> {

    return DbQueryCache.getTable(tableId.toString()) {
        rows
            .asSequence()
            .filter { it.value.tableId == tableId }
            .map { DbHelper(it.value) }
            .distinctBy { it.id }
            .toList()
    }
}


private fun formatClassName(tableName: String): String {
    return tableName
        .split('_', '-', '.', ':')
        .filter { it.isNotBlank() }
        .joinToString("") { part ->
            part.replaceFirstChar { it.uppercase() }
        } + "Row"
}


/**
 * Legacy function for generating table classes.
 * Note: This is kept for backward compatibility, but table generation is now handled
 * by the KSP processor (TableDataClassGeneratorSymbolProcessor) which runs automatically
 * during build or after cache operations.
 */
fun generateTable(table: TableDef, outputDir: File) {
    val dbHelper = ClassName("org.alter.game.util", "DbHelper")
    val listType = ClassName("kotlin.collections", "List")
    
    // Determine package based on table name prefix
    val packagePrefix = findMatchingPrefix(table.tableName)
    val packageName = if (packagePrefix != null) "$BASE_PACKAGE.$packagePrefix" else BASE_PACKAGE
    val rowClassName = ClassName(packageName, table.className)
    
    val rscmType = ClassName("org.alter.rscm", "RSCMType")
    val rscm = ClassName("org.alter.rscm", "RSCM")
    val tileType = ClassName("org.alter.game.model", "Tile")

    val usedColumnFunctions = mutableSetOf<String>()
    var needsTileImport = false

    val typeBuilder = TypeSpec.classBuilder(table.className)
        .primaryConstructor(FunSpec.constructorBuilder().addParameter("row", dbHelper).build())

    // Generate column properties and track which functions are used
    table.columns.forEach { col ->
        val isList = (col.varTypes?.size ?: 0) > 1

        col.varTypes?.firstOrNull()?.let { firstVarType ->
            val initializerType = getVarTypeImplClass(firstVarType)
            val isCoordType = firstVarType == VarType.COORDGRID
            
            val kotlinType = if (isCoordType && !isList) {
                needsTileImport = true
                tileType
            } else {
                getKotlinType(firstVarType.baseType!!, col.optional, isList, firstVarType == VarType.BOOLEAN)
            }

            val (initializerFn, functionName) = when {
                isCoordType && !isList -> {
                    // Special handling for CoordType - wrap with Tile.from30BitHash()
                    "%T.from30BitHash(row.column(%S, %T))" to "column"
                }
                isList && col.optional -> "row.multiColumnOptional(%S, %T)" to "multiColumnOptional"
                isList && !col.optional -> "row.multiColumn(%S, %T)" to "multiColumn"
                !isList && col.optional -> "row.columnOptional(%S, %T)" to "columnOptional"
                else -> "row.column(%S, %T)" to "column"
            }

            usedColumnFunctions.add(functionName)

            val propertyName = toCamelCase(col.simpleName)

            val initializer = if (isCoordType && !isList) {
                CodeBlock.of(initializerFn, tileType, col.name, initializerType)
            } else {
                CodeBlock.of(initializerFn, col.name, initializerType)
            }

            typeBuilder.addProperty(
                PropertySpec.builder(propertyName, kotlinType)
                    .initializer(initializer)
                    .build()
            )
        }
    }

    // Build companion object
    val companion = TypeSpec.companionObjectBuilder()
        .addFunction(
            FunSpec.builder("all")
                .returns(listType.parameterizedBy(rowClassName))
                .addStatement(
                    "return %T.table(%S).map { %T(it) }",
                    dbHelper,
                    "tables.${table.tableName}",
                    rowClassName
                )
                .build()
        )
        .addFunction(
            FunSpec.builder("getRow")
                .addParameter("row", INT)
                .returns(rowClassName)
                .addStatement("return %T(%T.row(row))", rowClassName, dbHelper)
                .build()
        )
        .addFunction(
            FunSpec.builder("getRow")
                .addParameter("column", String::class)
                .returns(rowClassName)
                .addStatement("%T.requireRSCM(%T.COLUMNS, column)", rscm, rscmType)
                .addStatement("return getRow(column.asRSCM() and 0xFFFF)")
                .build()
        )
        .build()

    typeBuilder.addType(companion)

    // Build imports - only include functions that are actually used
    val imports = mutableListOf("DbHelper")
    imports.addAll(usedColumnFunctions.sorted())

    // Create package directory if needed
    val packageDir = File(outputDir, packageName.replace(".", File.separator))
    packageDir.mkdirs()

    val fileSpecBuilder = FileSpec.builder(packageName, table.className)
        .addImport("org.alter.game.util", *imports.toTypedArray())
        .addImport("org.alter.rscm", "RSCM", "RSCMType")
        .addImport("org.alter.rscm.RSCM", "asRSCM")
    
    if (needsTileImport) {
        fileSpecBuilder.addImport("org.alter.game.model", "Tile")
    }
    
    val file = fileSpecBuilder
        .addType(typeBuilder.build())
        .build()

    file.writeTo(outputDir)
}

private const val BASE_PACKAGE = "org.alter.tables"
private val VAR_TYPES_PACKAGE = "org.alter.game.util.vars"

private val PACKAGE_PREFIXES = listOf(
    "fletching", "cluehelper", "fsw", "herblore", "woodcutting", "mining",
    "fishing", "cooking", "smithing", "crafting", "runecrafting",
    "agility", "thieving", "slayer", "construction", "hunter",
    "farming", "prayer", "magic", "ranged", "melee", "combat", "sailing"
)

private fun findMatchingPrefix(tableName: String): String? {
    val lowerTableName = tableName.lowercase()
    return PACKAGE_PREFIXES
        .sortedByDescending { it.length } // Check longer prefixes first
        .firstOrNull { prefix ->
            lowerTableName.startsWith(prefix) || lowerTableName.contains(prefix)
        }
}

private fun toCamelCase(name: String): String {
    val result = name.split("_")
        .joinToString("") { part ->
            part.lowercase().replaceFirstChar { it.uppercase() }
        }.replaceFirstChar { it.lowercase() }
    
    // Handle reserved keyword "object"
    return if (result == "object") "objectID" else result
}

private fun getKotlinType(varType: BaseVarType, optional: Boolean, isList: Boolean, isBooleanType: Boolean): TypeName {
    val baseType = when {
        isBooleanType -> BOOLEAN
        else -> when (varType) {
            BaseVarType.INTEGER -> INT
            BaseVarType.STRING -> STRING
            BaseVarType.LONG -> LONG
        }
    }

    return if (isList) {
        val elementType = if (optional) baseType.copy(nullable = true) else baseType
        val listType = LIST.parameterizedBy(elementType)
        if (optional) listType.copy(nullable = true) else listType
    } else {
        if (optional) baseType.copy(nullable = true) else baseType
    }
}

private fun getVarTypeImplClass(varType: VarType): ClassName {
    return when (varType) {
        VarType.BOOLEAN -> ClassName(VAR_TYPES_PACKAGE, "BooleanType")
        VarType.INT -> ClassName(VAR_TYPES_PACKAGE, "IntType")
        VarType.STRING -> ClassName(VAR_TYPES_PACKAGE, "StringType")
        VarType.LONG -> ClassName(VAR_TYPES_PACKAGE, "LongType")
        VarType.NPC -> ClassName(VAR_TYPES_PACKAGE, "NpcType")
        VarType.LOC -> ClassName(VAR_TYPES_PACKAGE, "LocType")
        VarType.OBJ -> ClassName(VAR_TYPES_PACKAGE, "ObjType")
        VarType.COORDGRID -> ClassName(VAR_TYPES_PACKAGE, "CoordType")
        VarType.MAPELEMENT -> ClassName(VAR_TYPES_PACKAGE, "MapElementType")
        VarType.DBROW -> ClassName(VAR_TYPES_PACKAGE, "RowType")
        VarType.NAMEDOBJ -> ClassName(VAR_TYPES_PACKAGE, "NamedObjType")
        VarType.GRAPHIC -> ClassName(VAR_TYPES_PACKAGE, "GraphicType")
        VarType.SEQ -> ClassName(VAR_TYPES_PACKAGE, "SeqType")
        VarType.MODEL -> ClassName(VAR_TYPES_PACKAGE, "ModelType")
        VarType.STAT -> ClassName(VAR_TYPES_PACKAGE, "StatType")
        VarType.CATEGORY -> ClassName(VAR_TYPES_PACKAGE, "CategoryType")
        VarType.COMPONENT -> ClassName(VAR_TYPES_PACKAGE, "ComponentType")
        VarType.INV -> ClassName(VAR_TYPES_PACKAGE, "InvType")
        VarType.IDKIT -> ClassName(VAR_TYPES_PACKAGE, "IdkType")
        VarType.ENUM -> ClassName(VAR_TYPES_PACKAGE, "EnumType")
        VarType.MIDI -> ClassName(VAR_TYPES_PACKAGE, "MidiType")
        VarType.VARP -> ClassName(VAR_TYPES_PACKAGE, "VarpType")
        VarType.STRUCT -> ClassName(VAR_TYPES_PACKAGE, "StructType")
        VarType.DBTABLE -> ClassName(VAR_TYPES_PACKAGE, "TableType")

        else -> error("Unmapped Type: $varType")
    }
}
