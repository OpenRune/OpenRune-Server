package org.alter.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.alter.game.util.*
import java.io.File
import java.util.Optional

data class TableColumn(
    val name: String,          // "columns.amenity:print_name"
    val simpleName: String,    // "printName"
    val kotlinType: TypeName,  // STRING, INT, etc
    val varType: ClassName,     // StringType, ObjType, etc
    val optional: Boolean = false
)

data class TableDef(
    val tableName: String,         // "amenity"
    val className: String,         // "AmenityRow"
    val columns: List<TableColumn> // all the columns
)

/**
 * Legacy function for generating table classes.
 * Note: This is kept for backward compatibility, but table generation is now handled
 * by the KSP processor (TableDataClassGeneratorSymbolProcessor) which runs automatically
 * during build or after cache operations.
 */
fun generateTable(table: TableDef, outputDir: File) {

    val dbHelper = ClassName("org.alter.game.util", "DbHelper")
    val listType = ClassName("kotlin.collections", "List")
    val rowClassName = ClassName("org.alter.tables", table.className)
    val rscmType = ClassName("org.alter.rscm", "RSCMType")
    val rscm = ClassName("org.alter.rscm", "RSCM")

    // --- Constructor ---
    val ctorParam = ParameterSpec.builder("row", dbHelper).build()

    // --- Class builder ---
    val typeBuilder = TypeSpec.classBuilder(table.className)
        .primaryConstructor(FunSpec.constructorBuilder().addParameter(ctorParam).build())

    // --- Column Fields ---
    table.columns.forEach { col ->
        val kotlinType = if (col.optional) {
            col.kotlinType.copy(nullable = true)
        } else {
            col.kotlinType
        }

        val initializer = if (col.optional) {
            "row.columnOptional(%S, %T)"
        } else {
            "row.column(%S, %T)"
        }

        typeBuilder.addProperty(
            PropertySpec.builder(col.simpleName, kotlinType)
                .initializer(initializer, col.name, col.varType)
                .build()
        )
    }

    // --- Companion Object ---
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

    // --- Write file ---
    val file = FileSpec.builder("org.alter.tables", table.className)
        .addImport("org.alter.game.util", "DbHelper", "column")
        .addImport("org.alter.rscm", "RSCM", "RSCMType")
        .addImport("org.alter.game.util","columnOptional")
        .addImport("org.alter.rscm.RSCM","asRSCM")
        .addType(typeBuilder.build())
        .build()

    println(outputDir.absolutePath)
    file.writeTo(outputDir)
}