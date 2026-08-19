package org.rsmod.api.droptable

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dtx.rs.RSDropTable
import io.github.classgraph.ClassGraph
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.droptable.toml.DropTableTomlParser
import org.rsmod.api.droptable.toml.DropTableTomlResolver
import org.rsmod.api.droptable.toml.DropTableTomlTextFixer
import org.rsmod.api.droptable.toml.TomlDropTableDef
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

@Singleton
public class DropTableRegistry @Inject constructor(tomlResolver: DropTableTomlResolver) {
    private val tablesByNpc: MutableMap<String, MutableList<RSDropTable<Player, DropRollItem>>> =
        hashMapOf()
    private val tablesByLoc: MutableMap<String, RSDropTable<Player, DropRollItem>> = hashMapOf()
    private val tomlTablesByNpc: MutableMap<String, MutableSet<String>> = hashMapOf()

    init {
        loadTomlTables(tomlResolver)
        loadAnnotatedTables()
    }

    public fun forNpc(npc: Npc): RSDropTable<Player, DropRollItem>? =
        forNpc(npc, areaChecker = null)

    public fun forNpc(npc: Npc, areaChecker: AreaChecker?): RSDropTable<Player, DropRollItem>? {
        val candidates = tablesByNpc[npc.type.internalName] ?: return null
        if (candidates.size == 1) {
            return candidates.first()
        }

        if (areaChecker != null) {
            val areaMatched =
                candidates.filter { table ->
                    table.areas.isNotEmpty() &&
                        table.areas.any { areaChecker.inArea(it, npc.coords) }
                }
            when (areaMatched.size) {
                1 -> return areaMatched.first()
                0 -> return candidates.firstOrNull { it.areas.isEmpty() } ?: candidates.first()
                else ->
                    error(
                        "Multiple drop tables match npc '${npc.type.internalName}' " +
                            "at ${npc.coords}: ${areaMatched.map { it.tableIdentifier }}"
                    )
            }
        }

        return candidates.firstOrNull { it.areas.isEmpty() } ?: candidates.first()
    }

    public fun forLoc(loc: String): RSDropTable<Player, DropRollItem>? = tablesByLoc[loc]

    private fun loadTomlTables(resolver: DropTableTomlResolver) {
        val mapper =
            ObjectMapper(TomlFactory())
                .registerKotlinModule()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        ClassGraph().ignoreClassVisibility().acceptPaths(TOML_RESOURCE_ROOT).scan().use { scan ->
            scan.allResources
                .filter { resource -> resource.path.endsWith(".toml") }
                .forEach { resource ->
                    val raw =
                        DropTableTomlTextFixer.hoistTableLevelKeys(resource.getContentAsString())
                    val def = mapper.readValue<TomlDropTableDef>(raw)
                    val table = DropTableTomlParser.parse(def, resolver, sourcePath = resource.path)
                    register(table, DropTableSource.Toml)
                }
        }
    }

    private fun loadAnnotatedTables() {
        io.github.classgraph
            .ClassGraph()
            .ignoreClassVisibility()
            .enableClassInfo()
            .enableFieldInfo()
            .enableAnnotationInfo()
            .acceptPackages(*SEARCH_PACKAGES)
            .scan()
            .use { scan ->
                scan.allClasses.forEach { classInfo ->
                    registerAnnotatedFields(classInfo.loadClass())
                }
            }
    }

    private fun registerAnnotatedFields(clazz: Class<*>) {
        for (field in clazz.declaredFields) {
            if (!java.lang.reflect.Modifier.isStatic(field.modifiers)) {
                continue
            }
            if (field.getAnnotation(RegisterDropTable::class.java) == null) {
                continue
            }
            if (!RSDropTable::class.java.isAssignableFrom(field.type)) {
                continue
            }

            try {
                field.isAccessible = true
                @Suppress("UNCHECKED_CAST")
                val table = field.get(null) as RSDropTable<Player, DropRollItem>
                register(table, DropTableSource.Annotation)
            } catch (exception: Exception) {
                throw IllegalStateException(
                    "Failed to register drop table from ${clazz.name}.${field.name}",
                    exception,
                )
            }
        }
    }

    private fun register(table: RSDropTable<Player, DropRollItem>, source: DropTableSource) {
        check(table.npcs.isNotEmpty() || table.locs.isNotEmpty()) {
            "Drop table '${table.tableIdentifier}' must define at least one npc or loc."
        }

        table.npcs.forEach { npc ->
            val existing = tablesByNpc.getOrPut(npc) { mutableListOf() }
            validateRegistration(table, npc, existing, source)
            existing += table
            if (source == DropTableSource.Toml) {
                tomlTablesByNpc.getOrPut(npc) { mutableSetOf() } += table.tableIdentifier
            }
        }

        table.locs.forEach { loc ->
            check(!tablesByLoc.containsKey(loc)) {
                "Duplicate drop table for loc '$loc': '${tablesByLoc[loc]?.tableIdentifier}' and '${table.tableIdentifier}'."
            }
            tablesByLoc[loc] = table
        }
    }

    private fun validateRegistration(
        table: RSDropTable<Player, DropRollItem>,
        npc: String,
        existing: List<RSDropTable<Player, DropRollItem>>,
        source: DropTableSource,
    ) {
        if (existing.isEmpty()) {
            return
        }

        val overlapping =
            existing.filter { registered ->
                registered.areas.isEmpty() && table.areas.isEmpty() ||
                    registered.areas.any { it in table.areas }
            }
        check(overlapping.isEmpty()) { buildConflictMessage(npc, overlapping, table, source) }
    }

    private fun buildConflictMessage(
        npc: String,
        overlapping: List<RSDropTable<Player, DropRollItem>>,
        table: RSDropTable<Player, DropRollItem>,
        source: DropTableSource,
    ): String {
        val conflicting = overlapping + table
        val tableNames = conflicting.joinToString { "'${it.tableIdentifier}'" }
        val tomlLoaded = tomlTablesByNpc[npc]?.isNotEmpty() == true
        val suffix =
            when {
                source == DropTableSource.Annotation && tomlLoaded ->
                    " Remove the @RegisterDropTable Kotlin definition or the TOML file under drops/tables/."
                source == DropTableSource.Toml ->
                    " Duplicate TOML drop tables cannot target the same npc."
                else -> " Remove duplicate @RegisterDropTable definitions."
            }
        return "Ambiguous drop tables for npc '$npc': $tableNames.$suffix"
    }

    private enum class DropTableSource {
        Toml,
        Annotation,
    }

    private companion object {
        private const val TOML_RESOURCE_ROOT = "drops/tables"
        private val SEARCH_PACKAGES = arrayOf("org.rsmod.api", "org.rsmod.content")
    }
}
