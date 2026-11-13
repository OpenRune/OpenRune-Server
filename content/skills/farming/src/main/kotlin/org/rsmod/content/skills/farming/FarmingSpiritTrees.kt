package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.enums.FarmingSpiritEnums
import org.rsmod.api.enums.FarmingSpiritGateEnums
import org.rsmod.api.player.dialogue.mesanims
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.farmingLvl
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.content.quest.manager.QuestRequirementResolver
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal data class SpiritTreeDestination(
    val index: Int,
    val name: String,
    val site: String,
    val head: String,
)

internal object FarmingSpiritTransport {
    val DESTINATIONS by lazy {
        FarmingSpiritEnums.farming_spirit_names.keys.sorted().map { index ->
            SpiritTreeDestination(
                index,
                FarmingSpiritEnums.farming_spirit_names.getValue(index),
                FarmingSpiritEnums.farming_spirit_sites[index].orEmpty(),
                FarmingSpiritEnums.farming_spirit_heads.getValue(index).internalName,
            )
        }
    }
}

internal fun spiritTreeOperational(state: PatchState, crop: Crop?): Boolean =
    crop != null && crop.kind == PatchKind.SPIRIT_TREE && state.health == Health.HEALTHY &&
        state.phase == Phase.CHECKED && state.grown(crop)

@Singleton
class FarmingSpiritTrees @Inject constructor(
    private val farming: FarmingService,
    private val quests: QuestRequirementResolver,
) {
    suspend fun interact(access: ProtectedAccess, patch: PatchDef, action: String) {
        if (patch.kind != PatchKind.SPIRIT_TREE || !operational(access.player, patch)) return
        val source = FarmingSpiritTransport.DESTINATIONS.firstOrNull { it.site == patch.site } ?: return
        access.interact(source, action) { operational(player, patch) }
    }

    internal suspend fun interactNatural(
        access: ProtectedAccess,
        sourceIndex: Int,
        loc: CoordGrid,
        action: String,
    ) {
        val source = FarmingSpiritTransport.DESTINATIONS.asSequence()
            .filter { if (sourceIndex == 0) it.site.isEmpty() && it.index != HOUSE_DESTINATION else it.index == sourceIndex }
            .minByOrNull { FarmingSpiritEnums.farming_spirit_anchors.getValue(it.index).chebyshevDistance(loc) }
            ?: return
        access.interact(source, action) { true }
    }

    private suspend fun ProtectedAccess.interact(
        source: SpiritTreeDestination,
        action: String,
        sourceOperational: ProtectedAccess.() -> Boolean,
    ) {
        val origin = coords
        if (!canDepart(source, sourceOperational)) return
        if (action == "talk-to") {
            if (source.head == "npc.treevillage_spirittree") {
                treeChat(source, "You friend of gnome people, you friend of mine. Would you like me to take you somewhere?")
                val go = choice2("No thanks, old tree.", false, "Where can I go?", true)
                if (!go) {
                    chatPlayer(mesanims.neutral, "No thanks, old tree.")
                    return
                }
                chatPlayer(mesanims.neutral, "Where can I go?")
                treeChat(source, "You can travel to the trees which are related to me.")
            } else {
                treeChat(source, "Hello gnome friend. Where would you like to go?")
            }
        }
        var selected = if (action == "last-destination") vars[PREVIOUS_DESTINATION] else 0
        if (action == "last-destination" && selected == 0) {
            treeChat(source, "This once, you will have to tell me where you wish to go, for I do not yet know where to take you back.")
        }
        while (coords == origin && canDepart(source, sourceOperational)) {
            if (selected == 0) {
                val choices = FarmingSpiritTransport.DESTINATIONS.map { destination ->
                    if (available(player, destination)) destination.name
                    else "<col=5f5f5f>${destination.name}</col>"
                } + "Cancel"
                val input = menu("Spirit Tree Locations", hotkeys = true, choices = choices)
                ifClose()
                selected = FarmingSpiritTransport.DESTINATIONS.getOrNull(input)?.index ?: return
            }
            val destination = FarmingSpiritTransport.DESTINATIONS.firstOrNull { it.index == selected }
                ?: return
            if (coords != origin || !canDepart(source, sourceOperational)) return
            when {
                destination.index == source.index -> treeChat(source, "You're already here.")
                !available(player, destination) -> {
                    if (destination.site == "farming_guild_spirit" && operationalAt(player, destination)) {
                        treeChat(source, "Travel to that area is restricted.")
                        mesbox("You need a Farming level of at least 85 to teleport there.")
                    } else if (destination.index == HOUSE_DESTINATION) {
                        treeChat(source, "None of my brethren are in your house.")
                    } else if (destination.name == "Laguna Aurorae") {
                        treeChat(source, "You cannot travel to that land at this time.")
                        mesbox("You must have visited Laguna Aurorae before you can travel there.")
                    } else {
                        treeChat(source, "None of my brethren are at that location.")
                    }
                }
                else -> {
                    travel(source, destination, origin, sourceOperational)
                    return
                }
            }
            selected = 0
        }
    }

    private fun ProtectedAccess.canDepart(
        source: SpiritTreeDestination,
        sourceOperational: ProtectedAccess.() -> Boolean,
    ): Boolean {
        if (!sourceOperational()) return false
        if (!quests.hasCompleted(player, TREE_GNOME_VILLAGE)) {
            mes(if (source.site.isEmpty()) "The tree doesn't feel like talking."
                else "The tree won't transport you until you've completed the Tree Gnome Village quest.")
            return false
        }
        val quest = FarmingSpiritEnums.farming_spirit_origin_quests[source.index]
        if (!quest.isNullOrEmpty() && !quests.hasCompleted(player, quest)) {
            mes("The tree doesn't feel like talking.")
            return false
        }
        return passesGate(player, source)
    }

    internal fun available(player: Player, destination: SpiritTreeDestination): Boolean {
        if (destination.index == HOUSE_DESTINATION) return false
        if (destination.site.isNotEmpty() && !operationalAt(player, destination)) return false
        val quest = FarmingSpiritEnums.farming_spirit_destination_quests[destination.index]
        if (!quest.isNullOrEmpty() && !quests.hasCompleted(player, quest)) return false
        if (!passesGate(player, destination)) return false
        val level = FarmingSpiritEnums.farming_spirit_farming_levels[destination.index] ?: 0
        return player.farmingLvl >= level
    }

    private fun passesGate(player: Player, destination: SpiritTreeDestination): Boolean {
        val variable = FarmingSpiritGateEnums.farming_spirit_gate_vars[destination.index] ?: return true
        if (variable == PATH_OF_GLOUPHRIE_PROGRESS && quests.hasCompleted(player, "quest_pathofglouphrie")) return true
        val varbit = checkNotNull(ServerCacheManager.getVarbit(variable))
        return player.vars[varbit] >= FarmingSpiritGateEnums.farming_spirit_gate_values.getValue(destination.index)
    }

    private fun operationalAt(player: Player, destination: SpiritTreeDestination): Boolean =
        FarmingPatches.bySite(destination.site).any { operational(player, it) }

    private fun operational(player: Player, patch: PatchDef): Boolean {
        val state = farming.state(player, patch)
        return spiritTreeOperational(state, state.crop)
    }

    private suspend fun ProtectedAccess.treeChat(source: SpiritTreeDestination, text: String) {
        chatNpcSpecific("Spirit Tree", source.head, mesanims.mesanim("mesanim.farming_spirit_tree"), text)
    }

    private suspend fun ProtectedAccess.travel(
        source: SpiritTreeDestination,
        destination: SpiritTreeDestination,
        origin: CoordGrid,
        sourceOperational: ProtectedAccess.() -> Boolean,
    ) {
        // shortcut: four exact arrival tiles are unobserved; replace adjacent landings when recorded.
        val arrival = FarmingSpiritEnums.farming_spirit_arrivals[destination.index]
            ?: mapFindSquareNone(
                FarmingSpiritEnums.farming_spirit_anchors.getValue(destination.index),
                LANDING_RADIUS,
                LANDING_RADIUS,
            )
        if (arrival == null) {
            mes("You can't travel there at the moment.")
            return
        }
        anim("seq.human_reachforladder", delay = 15)
        objboxNp("obj.spirit_tree_dummy", TRANSIT_TEXT)
        delay(2)
        if (coords != origin || !canDepart(source, sourceOperational) || !available(player, destination)) {
            resetAnim()
            ifClose()
            return
        }
        telejump(arrival)
        resetAnim()
        if (coords != arrival) {
            ifClose()
            return
        }
        VarPlayerIntMapSetter.set(player, PREVIOUS_DESTINATION, destination.index)
        delay(1)
        objbox("obj.spirit_tree_dummy", TRANSIT_TEXT)
    }

    companion object {
        fun canPlant(player: Player): Boolean {
            var limit = 0
            for ((level, count) in FarmingSpiritEnums.farming_spirit_planting_limits) {
                if (level <= player.farmingLvl) limit = maxOf(limit, checkNotNull(count))
            }
            var planted = 0
            for (patch in FarmingPatches.all) {
                if (patch.kind == PatchKind.SPIRIT_TREE &&
                    PatchState.unpack(player.vars[patch.varp]).cropIndex != 0) planted++
            }
            return planted < limit
        }

        private const val TREE_GNOME_VILLAGE = "quest_treegnomevillage"
        private const val PREVIOUS_DESTINATION = "varbit.spirit_tree_previous"
        private const val HOUSE_DESTINATION = 12
        private const val TRANSIT_TEXT = "You place your hands on the dry tough bark of the spirit tree, and feel a surge of energy run through your veins."
        private val PATH_OF_GLOUPHRIE_PROGRESS by lazy { "varbit.pog".asRSCM(RSCMType.VARBIT) }
        private val LANDING_RADIUS by lazy {
            val tree = checkNotNull(ServerCacheManager.getObject("loc.spirit_tree_fullygrown".asRSCM(RSCMType.LOC)))
            maxOf(tree.width, tree.length) / 2 + 1
        }
    }
}

class FarmingSpiritTreeScript @Inject constructor(private val trees: FarmingSpiritTrees) : PluginScript() {
    override fun ScriptContext.startup() {
        for ((loc, index) in FarmingSpiritEnums.farming_spirit_source_locs) {
            val sourceIndex = checkNotNull(index)
            onOpLoc1(loc) { trees.interactNatural(this, sourceIndex, it.loc.coords, "travel") }
            onOpLoc2(loc) { trees.interactNatural(this, sourceIndex, it.loc.coords, "talk-to") }
            onOpLoc3(loc) { trees.interactNatural(this, sourceIndex, it.loc.coords, "last-destination") }
        }
    }
}
