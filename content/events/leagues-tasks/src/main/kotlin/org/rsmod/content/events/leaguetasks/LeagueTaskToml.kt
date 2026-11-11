package org.rsmod.content.events.leaguetasks

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

/** One `[[action_row]]` block of a league task file, its symbols not yet resolved. */
internal data class RequirementToml(
    val trigger: String,
    val stat: List<String> = emptyList(),
    val obj: List<String> = emptyList(),
    val npc: List<String> = emptyList(),
    val loc: List<String> = emptyList(),
    val category: List<String> = emptyList(),
    val area: List<String> = emptyList(),
    val tool: List<String> = emptyList(),
    val collection: List<String> = emptyList(),
    val value: Int = 0,
    val times: Int = 1,
    val counter: String? = null,
    val varp: String? = null,
    val varbit: String? = null,
    @JsonProperty("interface") val interf: String? = null,
    val option: Int = 0,
    val emote: String? = null,
    val slayerTask: Int? = null,
)

internal class LeagueTaskSource(val path: String, val text: String)

internal class TaskDefinition(
    val task: String,
    val file: String,
    val requirements: List<RequirementToml>,
) {
    fun where(): String = "$file [$task]"
}

internal class LeagueTaskFolder(
    val key: String,
    val type: Int,
    val tasks: Map<String, TaskDefinition>,
)

/**
 * Every task file under `league_tasks/`: `generic/` holds tasks any league may list, each other
 * folder is one league named by its world type key, with its type in `league.toml`. A task lives
 * in exactly one place, so for a league its definitions are `generic/` plus its own folder.
 */
internal class LeagueTaskFiles(
    val generic: Map<String, TaskDefinition>,
    val leagues: Map<String, LeagueTaskFolder>,
) {
    fun definitions(key: String): Map<String, TaskDefinition> {
        val folder = leagues[key] ?: return emptyMap()
        return generic + folder.tasks
    }

    internal companion object {
        const val ROOT = "league_tasks"
        const val GENERIC = "generic"
        const val LEAGUE_FILE = "league.toml"

        private val MAPPER =
            ObjectMapper(TomlFactory())
                .registerKotlinModule()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)

        private class LeagueToml(val type: Int)

        fun parse(sources: Collection<LeagueTaskSource>): LeagueTaskFiles {
            val tasksByFolder = linkedMapOf<String, MutableMap<String, TaskDefinition>>()
            val types = mutableMapOf<String, Int>()
            for (source in sources.distinctBy { it.path }.sortedBy { it.path }) {
                val relative = source.path.substringAfter("$ROOT/")
                val folder = relative.substringBefore('/')
                val file = relative.substringAfter('/')
                check(folder != relative && folder.isNotEmpty()) {
                    "League task file ${source.path} must sit in a league folder or $GENERIC/."
                }
                if (file == LEAGUE_FILE) {
                    check(folder != GENERIC) { "$GENERIC/ belongs to no league: remove $relative." }
                    types[folder] = read<LeagueToml>(source).type
                    continue
                }
                val tasks = tasksByFolder.getOrPut(folder) { linkedMapOf() }
                for ((task, requirements) in read<Map<String, List<RequirementToml>>>(source)) {
                    val existing = tasks[task]
                    check(existing == null) {
                        "League task [$task] is defined in both ${existing?.file} and $relative."
                    }
                    check(requirements.isNotEmpty()) { "$relative [$task] has no requirements." }
                    tasks[task] = TaskDefinition(task, relative, requirements)
                }
            }
            val generic = tasksByFolder.remove(GENERIC).orEmpty()
            for (folder in tasksByFolder.keys) {
                check(folder in types) { "League folder $folder/ has no $LEAGUE_FILE." }
            }
            val leagues =
                types.mapValues { (key, type) ->
                    val tasks = tasksByFolder[key].orEmpty()
                    for (task in tasks.values) {
                        val shared = generic[task.task] ?: continue
                        error(
                            "League task [${task.task}] is defined in both ${shared.file} and " +
                                "${task.file}; a task is either generic or belongs to its leagues."
                        )
                    }
                    LeagueTaskFolder(key, type, tasks)
                }
            val duplicateType = leagues.values.groupBy { it.type }.values.firstOrNull { it.size > 1 }
            check(duplicateType == null) {
                "League folders ${duplicateType?.map { it.key }} share league type " +
                    "${duplicateType?.first()?.type}."
            }
            return LeagueTaskFiles(generic, leagues)
        }

        private inline fun <reified T> read(source: LeagueTaskSource): T =
            try {
                MAPPER.readValue<T>(source.text)
            } catch (e: Exception) {
                throw IllegalStateException("Could not read ${source.path}: ${e.message}", e)
            }
    }
}
