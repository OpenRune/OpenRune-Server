package org.rsmod.content.skills.construction

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.stat.baseMagicLvl
import org.rsmod.api.player.stat.magicLvl
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.api.table.construction.ConstructionPortalDestinationRow
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

class PortalDestination(private val row: ConstructionPortalDestinationRow, val spell: MagicSpell) {
    val name: String
        get() = row.name

    val nexusId: Int
        get() = row.nexusId

    val coord: CoordGrid?
        get() = row.spell.paramOrNull(params.spell_telecoord)

    private val pairs: List<Pair<Int, Int>> =
        row.portals.chunked(2).map { (frame, portal) -> frame.id to portal.id }

    val portalLocs: List<Int>
        get() = pairs.map { it.second }

    fun portalFor(frame: Int): Int? = pairs.firstOrNull { it.first == frame }?.second

    fun isFrame(locId: Int): Boolean = pairs.any { it.first == locId }
}

/**
 * Where a portal chamber portal or the portal nexus can lead, out of
 * `dbtable.construction_portal_destination`.
 *
 * Directing either needs what casting the destination's spell needs - its Magic level and quest -
 * and costs its runes many times over: a hundred casts' worth for a portal, a thousand for the
 * nexus. A portal accepts a boosted Magic level and the nexus does not.
 */
@Singleton
class PohPortals @Inject constructor(private val spells: MagicSpellRegistry) {
    fun destinations(): List<PortalDestination> =
        ConstructionPortalDestinationRow.all().mapNotNull { row ->
            val spell = spells.getObjSpell(row.spell) ?: return@mapNotNull null
            PortalDestination(row, spell).takeIf { it.coord != null }
        }

    fun isPortalFrame(locId: Int): Boolean = destinations().any { it.isFrame(locId) }

    fun byPortalLoc(locId: Int): PortalDestination? =
        destinations().firstOrNull { locId in it.portalLocs }

    fun byNexusId(nexusId: Int): PortalDestination? =
        destinations().firstOrNull { it.nexusId == nexusId }

    fun runeCost(destination: PortalDestination, multiplier: Int): List<Pair<String, Int>> =
        destination.spell.objReqs
            .filter { it.wornSlot == null }
            .map { RSCM.getReverseMapping(RSCMType.OBJ, it.obj.id) to it.count * multiplier }

    /** Why [player] cannot direct a teleport to [destination], or null when they can. */
    fun refusal(player: Player, destination: PortalDestination, boostable: Boolean): String? {
        val level = if (boostable) player.magicLvl else player.baseMagicLvl
        if (level < destination.spell.levelReq) {
            return "You need a Magic level of ${destination.spell.levelReq} to direct this teleport."
        }
        val quest = destination.spell.questReq
        if (quest != null && !QuestRequirements.hasCompleted(player, quest)) {
            return "You need to complete the required quest to direct this teleport."
        }
        return null
    }

    companion object {
        const val PORTAL_RUNE_MULTIPLIER: Int = 100
        const val NEXUS_RUNE_MULTIPLIER: Int = 1000
        const val PORTAL_XP_MULTIPLIER: Double = 5.0
    }
}
