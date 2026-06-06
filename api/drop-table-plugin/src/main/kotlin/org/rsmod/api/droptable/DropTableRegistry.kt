package org.rsmod.api.droptable

import dtx.rs.RSDropTable
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

@Singleton
public class DropTableRegistry @Inject constructor() {
    private val tablesByNpc: MutableMap<String, MutableList<RSDropTable<Player, DropRollItem>>> = hashMapOf()

    init {
        loadRegisteredTables()
    }

    public fun forNpc(npc: Npc): RSDropTable<Player, DropRollItem>? = forNpc(npc, areaChecker = null)

    public fun forNpc(
        npc: Npc,
        areaChecker: AreaChecker?,
    ): RSDropTable<Player, DropRollItem>? {
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
                            "at ${npc.coords}: ${areaMatched.map { it.tableIdentifier }}",
                    )
            }
        }

        return candidates.firstOrNull { it.areas.isEmpty() } ?: candidates.first()
    }

    private fun loadRegisteredTables() {
        io.github.classgraph.ClassGraph()
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
                register(table)
            } catch (exception: Exception) {
                throw IllegalStateException(
                    "Failed to register drop table from ${clazz.name}.${field.name}",
                    exception,
                )
            }
        }
    }

    private fun register(table: RSDropTable<Player, DropRollItem>) {
        check(table.npcs.isNotEmpty()) {
            "Drop table '${table.tableIdentifier}' must define at least one npc."
        }

        table.npcs.forEach { npc ->
            val existing = tablesByNpc.getOrPut(npc) { mutableListOf() }
            validateRegistration(table, npc, existing)
            existing += table
        }
    }

    private fun validateRegistration(
        table: RSDropTable<Player, DropRollItem>,
        npc: String,
        existing: List<RSDropTable<Player, DropRollItem>>,
    ) {
        if (existing.isEmpty()) {
            return
        }

        val overlapping =
            existing.filter { registered ->
                registered.areas.isEmpty() && table.areas.isEmpty() ||
                    registered.areas.any { it in table.areas }
            }
        check(overlapping.isEmpty()) {
            "Ambiguous drop tables for npc '$npc': " +
                "${(overlapping + table).joinToString { it.tableIdentifier }}"
        }
    }

    private companion object {
        private val SEARCH_PACKAGES = arrayOf("org.rsmod.api", "org.rsmod.content")
    }
}
