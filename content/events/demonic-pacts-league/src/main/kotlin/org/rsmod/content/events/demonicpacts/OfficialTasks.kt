package org.rsmod.content.events.demonicpacts

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager

/**
 * One task from the cache's Leagues VI task list. [index] is the task's bit in the client's
 * `league_task_completed_*` varps. [track] says how the server notices it; see
 * `demonic_pacts_league_tasks.toml`.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class OfficialTask(
    @param:JsonProperty("index") val index: Int,
    @param:JsonProperty("name") val name: String,
    @param:JsonProperty("desc") val desc: String,
    @param:JsonProperty("tier") val tier: Int,
    @param:JsonProperty("area") val area: Int,
    @param:JsonProperty("track") val track: String = MANUAL,
    @param:JsonProperty("stat") val stat: String? = null,
    @param:JsonProperty("value") val value: Long = 0,
    @param:JsonProperty("items") val items: List<String> = emptyList(),
    @param:JsonProperty("npcs") val npcs: List<String> = emptyList(),
    @param:JsonProperty("count") val count: Int = 1,
    @param:JsonProperty("action") val action: String? = null,
    @param:JsonProperty("quest") val quest: String? = null,
    @param:JsonProperty("event") val event: String? = null,
    @param:JsonProperty("match") val match: String? = null,
    @param:JsonProperty("counter") val counter: String? = null,
) {
    companion object {
        const val MANUAL = "manual"
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class OfficialTaskFile(
    @param:JsonProperty("task") val tasks: List<OfficialTask> = emptyList()
)

object OfficialTasks {
    private val logger = InlineLogger()

    /** Area filter ids the client gives tasks; Kourend's tasks use 10 for the 20 shield. */
    private val AREA_REGIONS =
        mapOf(
            1 to "misthalin",
            2 to "karamja",
            3 to "asgarnia",
            4 to "kandarin",
            5 to "morytania",
            6 to "desert",
            7 to "tirannwn",
            8 to "fremennik",
            9 to "kourend",
            10 to "kourend",
            11 to "wilderness",
            20 to "kourend",
            21 to "varlamore",
        )

    val all: List<OfficialTask> by lazy {
        val stream =
            OfficialTasks::class.java.classLoader.getResourceAsStream(TASK_FILE)
                ?: error("Missing Demonic Pacts resource: $TASK_FILE")
        val tasks =
            stream.use {
                ObjectMapper(TomlFactory()).registerKotlinModule().readValue<OfficialTaskFile>(it).tasks
            }
        require(tasks.map { it.index }.toSet().size == tasks.size) { "Duplicate task index." }
        require(tasks.all { it.index in 0 until MAX_TASKS }) { "Task index out of range." }
        tasks
    }

    val byIndex: Map<Int, OfficialTask> by lazy { all.associateBy { it.index } }

    fun regionFor(task: OfficialTask): LeagueRegion? =
        AREA_REGIONS[task.area]?.let { DemonicPactsData.regionsByKey[it] }

    fun regionForArea(area: Int): LeagueRegion? =
        AREA_REGIONS[area]?.let { DemonicPactsData.regionsByKey[it] }

    fun pointsFor(task: OfficialTask): Int {
        val settings = DemonicPactsData.settings
        return when (task.tier) {
            1 -> settings.easyPoints
            2 -> settings.mediumPoints
            3 -> settings.hardPoints
            4 -> settings.elitePoints
            5 -> settings.masterPoints
            else -> 0
        }
    }

    /** Item ids by lower-case name, leaving out noted objs and bank placeholders. */
    private val itemIdsByName: Map<String, Set<Int>> by lazy {
        val map = HashMap<String, MutableSet<Int>>()
        for ((id, type) in ServerCacheManager.getItems()) {
            if (type.isCert || type.isPlaceholder || type.name.isBlank() || type.name == "null") {
                continue
            }
            map.getOrPut(type.name.lowercase()) { mutableSetOf() } += id
        }
        map
    }

    fun itemName(id: Int): String = ServerCacheManager.getItem(id)?.name?.lowercase() ?: ""

    /**
     * The item ids each `items` group of an equip or obtain task accepts, or null when some group
     * names nothing in the cache, in which case the task can only be completed with `::taskdone`.
     */
    private val resolvedGroups: Map<Int, List<Set<Int>>?> by lazy {
        val result = HashMap<Int, List<Set<Int>>?>()
        var unresolved = 0
        for (task in all) {
            if (task.track != "equip" && task.track != "obtain") {
                continue
            }
            val groups =
                task.items.map { group ->
                    group.split(" | ").flatMap { resolveItem(it) }.toSet()
                }
            if (groups.isEmpty() || groups.any { it.isEmpty() }) {
                result[task.index] = null
                unresolved++
            } else {
                result[task.index] = groups
            }
        }
        logger.info { "Demonic Pacts League: $unresolved equip/obtain tasks name items this cache lacks." }
        result
    }

    fun itemGroups(task: OfficialTask): List<Set<Int>>? = resolvedGroups[task.index]

    private fun resolveItem(name: String): Set<Int> {
        val key = name.trim().lowercase()
        itemIdsByName[key]?.let { return it }
        for (variant in listOf(singular(key), "$key (u)", key.removeSuffix("s"))) {
            itemIdsByName[variant]?.let { return it }
        }
        return emptySet()
    }

    /** Whether the task can be noticed by the server at all. */
    fun isTracked(task: OfficialTask): Boolean =
        when (task.track) {
            OfficialTask.MANUAL -> false
            "equip", "obtain" -> itemGroups(task) != null
            else -> true
        }

    /**
     * Loose name matching between a task's wording and a cache name: equal, or the cache name is
     * the task's with extra whole words in front or behind ("raw monkfish" for "monkfish"). With
     * [allowShorter] the task's wording may also be the longer one ("kraken boss" for "kraken").
     */
    fun namesMatch(actual: String, wanted: String, allowShorter: Boolean = false): Boolean {
        val a = actual.lowercase().trim()
        val w = wanted.lowercase().trim()
        if (a.isEmpty() || w.isEmpty()) {
            return false
        }
        if (w.startsWith("*")) {
            val suffix = w.removePrefix("*")
            return suffix.isEmpty() || a.endsWith(suffix) || a.endsWith(singular(suffix))
        }
        val forms = setOf(a, singular(a))
        return forms.any { f ->
            f == w ||
                f.endsWith(" $w") ||
                f.startsWith("$w ") ||
                (allowShorter && (w.endsWith(" $f") || w.startsWith("$f ")))
        }
    }

    private fun singular(name: String): String =
        when {
            name.endsWith("men") -> name.dropLast(3) + "man"
            name.endsWith("ies") -> name.dropLast(3) + "y"
            name.endsWith("ses") || name.endsWith("xes") -> name.dropLast(2)
            name.endsWith("s") && !name.endsWith("ss") -> name.dropLast(1)
            else -> name
        }

    const val MAX_TASKS: Int = 62 * 32
    private const val TASK_FILE = "demonic_pacts_league_tasks.toml"
}
