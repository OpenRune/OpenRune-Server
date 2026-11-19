package org.rsmod.content.events.demonicpacts

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

@JsonIgnoreProperties(ignoreUnknown = true)
data class LeagueSettings(
    @param:JsonProperty("start_x") val startX: Int = 1681,
    @param:JsonProperty("start_z") val startZ: Int = 3134,
    @param:JsonProperty("start_level") val startLevel: Int = 0,
    @param:JsonProperty("tier_thresholds")
    val tierThresholds: List<Int> = listOf(0, 600, 1200, 2600, 5200, 8500, 16500, 28000),
    @param:JsonProperty("easy_points") val easyPoints: Int = 10,
    @param:JsonProperty("medium_points") val mediumPoints: Int = 30,
    @param:JsonProperty("hard_points") val hardPoints: Int = 80,
    @param:JsonProperty("elite_points") val elitePoints: Int = 200,
    @param:JsonProperty("master_points") val masterPoints: Int = 400,
    @param:JsonProperty("starting_pact_points") val startingPactPoints: Int = 2,
    @param:JsonProperty("max_pact_points_earned") val maxPactPointsEarned: Int = 44,
    @param:JsonProperty("max_pacts_spent") val maxPactsSpent: Int = 40,
    @param:JsonProperty("max_pact_resets") val maxPactResets: Int = 6,
    @param:JsonProperty("bonus_pact_point_every_tasks") val bonusPactPointEveryTasks: Int = 10,
    @param:JsonProperty("karamja_tasks") val karamjaTasks: Int = 80,
    @param:JsonProperty("region_task_thresholds")
    val regionTaskThresholds: List<Int> = listOf(200, 300, 450),
    @param:JsonProperty("region_lock") val regionLock: Boolean = true,
) {
    fun pointsFor(tier: String): Int =
        when (tier.lowercase()) {
            "easy" -> easyPoints
            "medium" -> mediumPoints
            "hard" -> hardPoints
            "elite" -> elitePoints
            "master" -> masterPoints
            else -> 0
        }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class PactNode(
    @param:JsonProperty("id") val id: String,
    @param:JsonProperty("bit") val bit: Int,
    @param:JsonProperty("size") val size: String = "sm",
    @param:JsonProperty("out") val out: List<String> = emptyList(),
    @param:JsonProperty("effect") val effect: String = "none",
    @param:JsonProperty("value") val value: Int = 0,
    @param:JsonProperty("style") val style: String = "all",
    @param:JsonProperty("text") val text: String = "",
) {
    val isImplemented: Boolean
        get() = effect != "none"
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class LeagueRegion(
    @param:JsonProperty("key") val key: String,
    @param:JsonProperty("name") val name: String,
    @param:JsonProperty("bit") val bit: Int,
    @param:JsonProperty("unlockable") val unlockable: Boolean = false,
    @param:JsonProperty("boxes") val boxes: List<List<Int>> = emptyList(),
    @param:JsonProperty("raw_boxes") val rawBoxes: List<List<Int>> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LeagueTask(
    @param:JsonProperty("bit") val bit: Int,
    @param:JsonProperty("id") val id: String,
    @param:JsonProperty("name") val name: String,
    @param:JsonProperty("tier") val tier: String,
    @param:JsonProperty("region") val region: String = "general",
    @param:JsonProperty("type") val type: String,
    @param:JsonProperty("npc") val npc: String? = null,
    @param:JsonProperty("stat") val stat: String? = null,
    @param:JsonProperty("value") val value: Int = 0,
    @param:JsonProperty("pact") val pact: Boolean = false,
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class PactTreeFile(@param:JsonProperty("node") val nodes: List<PactNode> = emptyList())

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class RegionFile(
    @param:JsonProperty("region") val regions: List<LeagueRegion> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class TaskFile(@param:JsonProperty("task") val tasks: List<LeagueTask> = emptyList())

object DemonicPactsData {
    const val MAX_PACT_BITS: Int = 160
    const val MAX_TASK_BITS: Int = 256

    private val mapper: ObjectMapper = ObjectMapper(TomlFactory()).registerKotlinModule()

    val settings: LeagueSettings by lazy { read<LeagueSettings>("demonic_pacts.toml") }

    val nodes: List<PactNode> by lazy {
        read<PactTreeFile>("demonic_pacts_tree.toml").nodes.also { validateNodes(it) }
    }

    val nodesById: Map<String, PactNode> by lazy { nodes.associateBy { it.id.uppercase() } }

    /** Undirected: a node can be bought next to any owned node, in either direction. */
    val neighbours: Map<String, Set<String>> by lazy {
        val map = HashMap<String, MutableSet<String>>()
        for (node in nodes) {
            for (target in node.out) {
                map.getOrPut(node.id) { mutableSetOf() } += target
                map.getOrPut(target) { mutableSetOf() } += node.id
            }
        }
        map
    }

    val root: PactNode by lazy { nodes.first() }

    val regions: List<LeagueRegion> by lazy {
        read<RegionFile>("demonic_pacts_regions.toml").regions.also { validateRegions(it) }
    }

    val regionsByKey: Map<String, LeagueRegion> by lazy { regions.associateBy { it.key } }

    val tasks: List<LeagueTask> by lazy {
        read<TaskFile>("demonic_pacts_tasks.toml").tasks.also { validateTasks(it) }
    }

    private fun validateNodes(nodes: List<PactNode>) {
        require(nodes.isNotEmpty()) { "Demonic pact tree is empty." }
        val ids = nodes.map { it.id }
        require(ids.size == ids.toSet().size) { "Duplicate pact node ids." }
        val bits = nodes.map { it.bit }
        require(bits.size == bits.toSet().size) { "Duplicate pact node bits." }
        require(bits.all { it in 0 until MAX_PACT_BITS }) { "Pact bits must be 0-159." }
        val known = ids.toSet()
        for (node in nodes) {
            for (target in node.out) {
                require(target in known) { "Pact node ${node.id} links to unknown node $target." }
            }
        }
    }

    private fun validateRegions(regions: List<LeagueRegion>) {
        val bits = regions.map { it.bit }.filter { it >= 0 }
        require(bits.size == bits.toSet().size) { "Duplicate region bits." }
        require(bits.all { it in 0..30 }) { "Region bits must be 0-30." }
        for (region in regions) {
            for (box in region.boxes + region.rawBoxes) {
                require(box.size == 4) { "Region ${region.key} has a box without 4 numbers." }
            }
        }
    }

    private fun validateTasks(tasks: List<LeagueTask>) {
        val bits = tasks.map { it.bit }
        require(bits.size == bits.toSet().size) { "Duplicate task bits." }
        require(bits.all { it in 0 until MAX_TASK_BITS }) { "Task bits must be 0-255." }
        val ids = tasks.map { it.id }
        require(ids.size == ids.toSet().size) { "Duplicate task ids." }
    }

    private inline fun <reified T> read(resource: String): T {
        val stream =
            DemonicPactsData::class.java.classLoader.getResourceAsStream(resource)
                ?: error("Missing Demonic Pacts resource: $resource")
        return stream.use { mapper.readValue<T>(it) }
    }
}
