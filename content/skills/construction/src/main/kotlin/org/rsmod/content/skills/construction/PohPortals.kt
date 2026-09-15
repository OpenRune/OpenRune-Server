package org.rsmod.content.skills.construction

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import org.rsmod.api.config.refs.params
import org.rsmod.map.CoordGrid

/**
 * A portal room portal is one furniture row per material and a separate loc per destination, named
 * `poh_portal_<material>_<destination>`. The row builds `..._empty`, so the destination is chosen
 * when it is built and remembered as the layout's variant loc.
 *
 * Destinations reuse the teleport the matching spell already performs rather than repeating its
 * coords here: the spell obj carries `param.spell_telecoord`, which is where the spellbook reads it
 * from too. A destination whose spell this cache does not carry simply is not offered.
 */
object PohPortals {
    private const val LOC_PREFIX = "loc.poh_portal_"
    private const val EMPTY_SUFFIX = "_empty"

    val MATERIALS: List<String> = listOf("teak", "mahogany", "marble")

    /** Portal destination -> the teleport spell whose destination it shares. */
    private val DESTINATIONS: Map<String, String> =
        linkedMapOf(
            "varrock" to "obj.25_varrock_teleport",
            "lumbridge" to "obj.31_lumbridge_teleport",
            "falador" to "obj.37_falador_teleport",
            "camelot" to "obj.45_camelot_teleport",
            "ardougne" to "obj.51_ardougne_teleport",
            "watchtower" to "obj.58_watchtower_teleport",
            "trollheim" to "obj.61_trollheim_teleport",
            "ape_atoll" to "obj.64_ape_atoll_teleport",
        )

    /** Titles for the build-time menu, in the order [DESTINATIONS] declares them. */
    fun labels(): List<String> = DESTINATIONS.keys.map(::title)

    fun destinationAt(index: Int): String? = DESTINATIONS.keys.toList().getOrNull(index)

    fun title(destination: String): String =
        destination.split('_').joinToString(" ") { part ->
            part.replaceFirstChar(Char::uppercaseChar)
        }

    fun isPortalFrame(locId: Int): Boolean = locId in frameLocs

    /** The loc a portal of [material] leading to [destination] shows as. */
    fun portalLoc(material: String, destination: String): Int? =
        locId("$LOC_PREFIX${material}_$destination")

    /** Where [destination] leads, for callers that hold the name rather than a loc. */
    fun destinationCoord(destination: String): CoordGrid? =
        DESTINATIONS[destination]?.let(::spellDestination)

    /** Every portal loc that leads somewhere, paired with where it leads. */
    val teleports: Map<Int, CoordGrid> by lazy {
        val out = HashMap<Int, CoordGrid>()
        for ((destination, spellObj) in DESTINATIONS) {
            val coord = spellDestination(spellObj) ?: continue
            for (material in MATERIALS) {
                val loc = portalLoc(material, destination) ?: continue
                out[loc] = coord
            }
        }
        out
    }

    private val frameLocs: Set<Int> by lazy {
        MATERIALS.mapNotNullTo(HashSet()) { locId("$LOC_PREFIX$it$EMPTY_SUFFIX") }
    }

    private fun locId(name: String): Int? {
        val id = runCatching { RSCM.getRSCM(name) }.getOrDefault(-1)
        return id.takeIf { it > 0 && ServerCacheManager.getObject(it) != null }
    }

    private fun spellDestination(spellObj: String): CoordGrid? {
        val objId = runCatching { RSCM.getRSCM(spellObj) }.getOrDefault(-1)
        if (objId <= 0) {
            return null
        }
        val type = ServerCacheManager.getItem(objId) ?: return null
        return type.paramOrNull(params.spell_telecoord)
    }
}
