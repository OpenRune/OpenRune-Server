package org.rsmod.content.events.demonicpacts

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

@JsonIgnoreProperties(ignoreUnknown = true)
data class LeagueSettings(
    @param:JsonProperty("start_x") val startX: Int = 1760,
    @param:JsonProperty("start_z") val startZ: Int = 3112,
    @param:JsonProperty("start_level") val startLevel: Int = 0,
    @param:JsonProperty("broken_map_squares") val brokenMapSquares: List<String> = emptyList(),
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
    @param:JsonProperty("starting_pact_resets") val startingPactResets: Int = 6,
    @param:JsonProperty("web_port") val webPort: Int = 8081,
    @param:JsonProperty("bonus_pact_point_every_tasks") val bonusPactPointEveryTasks: Int = 10,
    @param:JsonProperty("karamja_tasks") val karamjaTasks: Int = 80,
    @param:JsonProperty("region_task_thresholds")
    val regionTaskThresholds: List<Int> = listOf(200, 300, 450),
    @param:JsonProperty("region_lock") val regionLock: Boolean = true,
) {
    /** True when [x],[z] is close enough to a broken map square that the client would load it. */
    fun nearBrokenMapSquare(x: Int, z: Int): Boolean =
        brokenMapSquares.any { key ->
            val parts = key.split('_')
            val squareX = parts.getOrNull(0)?.toIntOrNull() ?: return@any false
            val squareZ = parts.getOrNull(1)?.toIntOrNull() ?: return@any false
            val minX = squareX * 64 - VIEW_MARGIN
            val minZ = squareZ * 64 - VIEW_MARGIN
            x in minX until minX + 64 + VIEW_MARGIN * 2 && z in minZ until minZ + 64 + VIEW_MARGIN * 2
        }

}

private const val VIEW_MARGIN = 56

@JsonIgnoreProperties(ignoreUnknown = true)
data class PactNode(
    @param:JsonProperty("id") val id: String,
    @param:JsonProperty("bit") val bit: Int,
    @param:JsonProperty("size") val size: Int = 1,
    @param:JsonProperty("out") val out: List<String> = emptyList(),
    @param:JsonProperty("effect") val effect: Int = 0,
    @param:JsonProperty("value") val value: Int = 0,
    @param:JsonProperty("text") val text: String = "",
) {
    val isImplemented: Boolean
        get() = effect in PactEffectIds.IMPLEMENTED
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class PactEffectVarbit(
    @param:JsonProperty("id") val id: Int,
    @param:JsonProperty("varbit") val varbit: String,
    @param:JsonProperty("max") val max: Int,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LeagueRegion(
    @param:JsonProperty("key") val key: String,
    @param:JsonProperty("name") val name: String,
    @param:JsonProperty("bit") val bit: Int,
    @param:JsonProperty("unlockable") val unlockable: Boolean = false,
    @param:JsonProperty("area") val area: Int = -1,
    @param:JsonProperty("teleport") val teleport: List<Int> = emptyList(),
    @param:JsonProperty("boxes") val boxes: List<List<Int>> = emptyList(),
    @param:JsonProperty("raw_boxes") val rawBoxes: List<List<Int>> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class PactTreeFile(@param:JsonProperty("node") val nodes: List<PactNode> = emptyList())

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class EffectFile(
    @param:JsonProperty("effect") val effects: List<PactEffectVarbit> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class RegionFile(
    @param:JsonProperty("region") val regions: List<LeagueRegion> = emptyList()
)


object DemonicPactsData {
    const val MAX_PACT_BITS: Int = 192

    private val mapper: ObjectMapper = ObjectMapper(TomlFactory()).registerKotlinModule()

    val settings: LeagueSettings by lazy { read<LeagueSettings>("demonic_pacts.toml") }

    val nodes: List<PactNode> by lazy {
        read<PactTreeFile>("demonic_pacts_tree.toml").nodes.also { validateNodes(it) }
    }

    val nodesById: Map<String, PactNode> by lazy { nodes.associateBy { it.id.uppercase() } }

    val nodesByBit: Map<Int, PactNode> by lazy { nodes.associateBy { it.bit } }

    val effectVarbits: List<PactEffectVarbit> by lazy {
        read<EffectFile>("demonic_pacts_effects.toml").effects
    }

    /** A node by its cache name, with or without the prefix: `node12` or `12`. */
    fun findNode(key: String): PactNode? {
        val trimmed = key.trim().uppercase()
        return nodesById[trimmed] ?: nodesById["NODE$trimmed"]
    }

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

    val root: PactNode by lazy { nodes.first { it.bit == 0 } }

    val regions: List<LeagueRegion> by lazy {
        read<RegionFile>("demonic_pacts_regions.toml").regions.also { validateRegions(it) }
    }

    val regionsByKey: Map<String, LeagueRegion> by lazy { regions.associateBy { it.key } }


    private fun validateNodes(nodes: List<PactNode>) {
        require(nodes.isNotEmpty()) { "Demonic pact tree is empty." }
        val ids = nodes.map { it.id }
        require(ids.size == ids.toSet().size) { "Duplicate pact node ids." }
        val bits = nodes.map { it.bit }
        require(bits.size == bits.toSet().size) { "Duplicate pact node bits." }
        require(bits.all { it in 0 until MAX_PACT_BITS }) { "Pact bits must be 0-191." }
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


    private inline fun <reified T> read(resource: String): T {
        val stream =
            DemonicPactsData::class.java.classLoader.getResourceAsStream(resource)
                ?: error("Missing Demonic Pacts resource: $resource")
        return stream.use { mapper.readValue<T>(it) }
    }
}

/** Cache effect ids (talent_effect_value) the server implements. */
object PactEffectIds {
    const val MAGIC_DAMAGE: Int = 7
    const val ALL_STYLE_ACCURACY: Int = 12
    const val DEFENCE_BOOST: Int = 13
    const val MELEE_DAMAGE: Int = 14
    const val RANGED_DAMAGE: Int = 15
    const val THORNS: Int = 19
    const val SHIELD_REFLECT: Int = 23
    const val MELEE_MIN_HIT_DISTANCE: Int = 25
    const val PRAYER_PEN_ALL: Int = 40
    const val PRAYER_RESTORE_NO_OVERHEAD: Int = 61
    const val SPEC_FROM_DISTANCE: Int = 62
    const val SPEC_ON_MELEE_HIT: Int = 66
    const val MELEE_MAX_HIT_DISTANCE: Int = 70
    const val THORNS_DOUBLE_HIT: Int = 84

    val IMPLEMENTED: Set<Int> =
        setOf(
            MAGIC_DAMAGE,
            ALL_STYLE_ACCURACY,
            DEFENCE_BOOST,
            MELEE_DAMAGE,
            RANGED_DAMAGE,
            THORNS,
            SHIELD_REFLECT,
            MELEE_MIN_HIT_DISTANCE,
            PRAYER_PEN_ALL,
            PRAYER_RESTORE_NO_OVERHEAD,
            SPEC_FROM_DISTANCE,
            SPEC_ON_MELEE_HIT,
            MELEE_MAX_HIT_DISTANCE,
            THORNS_DOUBLE_HIT,
        )
}
