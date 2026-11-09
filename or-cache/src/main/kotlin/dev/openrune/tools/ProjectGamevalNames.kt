package dev.openrune.tools

import dev.openrune.cache.tools.iftype.dsl.InterfaceInherit
import dev.openrune.cache.tools.iftype.toml.loadInterfaceToml
import dev.openrune.definition.dbtables.DBTable
import dev.openrune.gamevals.GameValProvider
import dev.openrune.gamevals.GamevalDumper.childNames
import dev.openrune.gamevals.GamevalDumper.standardizeName
import dev.openrune.pack.PluginPack
import dev.openrune.toml.rsconfig.rsconfig
import dev.openrune.toml.tomlMapper

/** Resolves project-generated names for validation without writing or packing cache data. */
internal object ProjectGamevalNames {
    fun collect(
        provider: GameValProvider,
        packs: List<PluginPack>,
        baseTables: List<DBTable>,
    ): Set<String> {
        val namespaces = mutableMapOf<String, List<String>>()
        val mapper = tomlMapper { rsconfig { enableConstantProvider() } }
        val interfaces = packs.flatMap { it.interfaces() } +
            packs.mapNotNull { it.interfaceDirectory() }.flatMap { directory ->
                directory.walkTopDown()
                    .filter { it.isFile && it.extension == "if3" }
                    .map { loadInterfaceToml(it.toPath(), mapper) }
                    .toList()
            }
        for (definition in interfaces) {
            val inherited = InterfaceInherit.take(definition.id)
            val parent = inherited?.removePrefix("interface.") ?: definition.internalName
            val namespace = "component.${standardizeName(parent)}"
            val children =
                definition.components.values.mapNotNull { component ->
                    val name = component.internalName ?: "com_${component.component}"
                    if (inherited != null && name == "universe") null else name to component.packed
                }
            val names = childNames(parent, children).map { "component.$it" }
            namespaces[namespace] =
                if (inherited == null) {
                    removeBaseChildren(provider, "component", parent)
                    names
                } else {
                    // Inheritance retains base names even when the client component IDs shift.
                    namespaces[namespace].orEmpty() + names
                }
        }

        for (table in baseTables + packs.flatMap { it.dbTables() }) {
            val parent = table.rscmName ?: "table_${table.tableId}"
            val children = table.columns.map { (id, column) ->
                (column.rscmName ?: "col_$id") to ((table.tableId shl 16) or id)
            }
            removeBaseChildren(provider, "dbcol", parent)
            namespaces["dbcol.${standardizeName(parent)}"] =
                childNames(parent, children).map { "dbcol.$it" }
        }
        return namespaces.values.flatten().toSet()
    }

    private fun removeBaseChildren(provider: GameValProvider, table: String, parent: String) {
        val prefix = "$table.${standardizeName(parent)}:"
        provider.mappings[table]?.keys?.removeIf { key ->
            key.startsWith(prefix) && provider.sourceOf(table, key.removePrefix("$table.")) == null
        }
    }
}
