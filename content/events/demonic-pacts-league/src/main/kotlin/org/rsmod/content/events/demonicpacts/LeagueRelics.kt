package org.rsmod.content.events.demonicpacts

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.openrune.ServerCacheManager
import dev.openrune.types.enums.enum
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

data class RelicTier(val index: Int, val points: Int, val passive: String, val relics: List<Relic>)

/**
 * [key] is the relic's key in its tier's relic enum, which is what `league_relic_selection_<tier>`
 * stores. [clickzone] is its running index across all tiers, which is the comsub the client sends.
 */
data class Relic(
    val tier: Int,
    val key: Int,
    val clickzone: Int,
    val struct: Int,
    val name: String,
    val description: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class RelicTextFile(
    @param:JsonProperty("tier") val tiers: List<RelicTierText> = emptyList(),
    @param:JsonProperty("relic") val relics: List<RelicText> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class RelicTierText(
    @param:JsonProperty("tier") val tier: Int,
    @param:JsonProperty("passive") val passive: String = "",
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class RelicText(
    @param:JsonProperty("name") val name: String,
    @param:JsonProperty("description") val description: String = "",
)

/** The league's relic tiers, read from the same cache enums and structs the client draws. */
object LeagueRelics {
    private val selectionVarbits =
        listOf(
            "varbit.league_relic_selection_0",
            "varbit.league_relic_selection_1",
            "varbit.league_relic_selection_2",
            "varbit.league_relic_selection_3",
            "varbit.league_relic_selection_4",
            "varbit.league_relic_selection_5",
            "varbit.league_relic_selection_6",
            "varbit.league_relic_selection_7",
        )

    private val texts: RelicTextFile by lazy {
        val stream =
            LeagueRelics::class.java.classLoader.getResourceAsStream("demonic_pacts_relics.toml")
                ?: error("Missing Demonic Pacts resource: demonic_pacts_relics.toml")
        stream.use { ObjectMapper(TomlFactory()).registerKotlinModule().readValue<RelicTextFile>(it) }
    }

    val tiers: List<RelicTier> by lazy { loadTiers() }

    val byClickzone: Map<Int, Relic> by lazy {
        tiers.flatMap { it.relics }.associateBy { it.clickzone }
    }

    val clickzoneCount: Int
        get() = byClickzone.size

    private fun loadTiers(): List<RelicTier> {
        val league = enum<Int, Int>(LEAGUE_STRUCT_ENUM)[LEAGUE_TYPE_DEMONIC_PACTS]
            ?: error("No league struct for league type $LEAGUE_TYPE_DEMONIC_PACTS")
        val tierEnum = structParam(league, TIER_ENUM_PARAM) as Int
        val passives = texts.tiers.associate { it.tier to it.passive }
        val descriptions = texts.relics.associate { it.name.lowercase() to it.description }
        var clickzone = 0
        val result = mutableListOf<RelicTier>()
        val tierStructs = enum<Int, Int>(tierEnum).backing.toSortedMap()
        for ((index, tierStruct) in tierStructs) {
            tierStruct ?: continue
            val points = structParam(tierStruct, TIER_POINTS_PARAM) as? Int ?: 0
            val relicEnum = structParam(tierStruct, TIER_RELICS_PARAM) as? Int ?: continue
            val relics = mutableListOf<Relic>()
            for ((key, relicStruct) in enum<Int, Int>(relicEnum).backing.toSortedMap()) {
                relicStruct ?: continue
                val name = structParam(relicStruct, RELIC_NAME_PARAM) as? String ?: "Relic"
                val description = descriptions[name.lowercase()] ?: ""
                relics += Relic(index, key, clickzone++, relicStruct, name, description)
            }
            result += RelicTier(index, points, passives[index + 1] ?: "", relics)
        }
        return result
    }

    private fun structParam(struct: Int, param: Int): Any? =
        ServerCacheManager.getStruct(struct)?.params?.get(param)

    fun selection(player: Player, tier: Int): Int = player.vars[selectionVarbits[tier]]

    fun isUnlocked(player: Player, tier: RelicTier): Boolean = player.leaguePoints >= tier.points

    /** Tiers are chosen in order, like the client draws them: the first open tier only. */
    fun canSelect(player: Player, relic: Relic): Boolean {
        val tier = tiers.getOrNull(relic.tier) ?: return false
        if (!isUnlocked(player, tier) || selection(player, relic.tier) != 0) {
            return false
        }
        return tiers.take(relic.tier).all { selection(player, it.index) != 0 }
    }

    fun select(player: Player, relic: Relic) {
        VarPlayerIntMapSetter.set(player, selectionVarbits[relic.tier], relic.key)
    }

    fun selected(player: Player): List<Relic> =
        tiers.mapNotNull { tier ->
            val key = selection(player, tier.index)
            tier.relics.firstOrNull { it.key == key }
        }

    private const val LEAGUE_STRUCT_ENUM = 2670
    private const val LEAGUE_TYPE_DEMONIC_PACTS = 6
    private const val TIER_ENUM_PARAM = 870
    private const val TIER_POINTS_PARAM = 877
    private const val TIER_RELICS_PARAM = 878
    private const val RELIC_NAME_PARAM = 879
}
