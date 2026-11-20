package org.rsmod.content.events.demonicpacts

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.hook.ItemTeleportCaster
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpWorn2
import org.rsmod.game.entity.Player
import org.rsmod.game.world.WorldType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

@JsonIgnoreProperties(ignoreUnknown = true)
data class RelicTeleport(
    @param:JsonProperty("item") val item: String,
    @param:JsonProperty("group") val group: String,
    @param:JsonProperty("name") val name: String,
    @param:JsonProperty("coord") val coord: List<Int>,
    @param:JsonProperty("quest") val quest: String? = null,
) {
    val coords: CoordGrid
        get() = CoordGrid(coord[0], coord[1], coord[2])
}

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class RelicTeleportFile(
    @param:JsonProperty("teleport") val teleports: List<RelicTeleport> = emptyList()
)

/**
 * The teleport menus of the relic items: Bank Heist's briefcase, Evil Eye's necklace, Map of
 * Alacrity's map and Nature's Accord's fairy mushroom. Each lists the destinations in league
 * areas the player has unlocked, grouped by area.
 */
class RelicTeleportScript @Inject constructor(private val teleports: ItemTeleportCaster) :
    PluginScript() {
    private val logger = InlineLogger()

    override val worldTypes = listOf(WorldType.DEMONIC_PACTS_LEAGUE)

    private val destinations: Map<String, List<RelicTeleport>> by lazy {
        val stream =
            RelicTeleportScript::class.java.classLoader.getResourceAsStream(TELEPORT_FILE)
                ?: error("Missing Demonic Pacts resource: $TELEPORT_FILE")
        stream.use {
            ObjectMapper(TomlFactory())
                .registerKotlinModule()
                .readValue<RelicTeleportFile>(it)
                .teleports
                .filter { tele -> tele.coord.size == 3 }
                .groupBy { tele -> tele.item }
        }
    }

    override fun ScriptContext.startup() {
        for ((item, relic) in TELEPORT_ITEMS) {
            onRelicItem(item) { teleportMenu(item, relic) }
        }
    }

    private suspend fun ProtectedAccess.teleportMenu(item: String, relic: String) {
        if (!player.hasRelic(relic)) {
            mes("You need the $relic relic to use this.")
            return
        }
        val open = destinations[item].orEmpty().filter { isOpen(player, it) }
        if (open.isEmpty()) {
            mes("None of this item's destinations are in areas you have unlocked.")
            return
        }
        val groups = open.groupBy { it.group }.toSortedMap()
        val group =
            if (groups.size == 1) {
                groups.keys.first()
            } else {
                val names = groups.keys.toList()
                names.getOrNull(menu(relic, hotkeys = true, choices = names)) ?: return
            }
        val choices = groups.getValue(group).sortedBy { it.name }.take(MAX_CHOICES)
        val dest = choices.getOrNull(menu("$relic: $group", hotkeys = false, choices = choices.map { it.name }))
        dest ?: return
        with(teleports) { cast(TeleportType.Exempt, dest.coords) }
    }

    /** A destination is open when its tile, or failing that its menu group, is unlocked. */
    private fun isOpen(player: Player, teleport: RelicTeleport): Boolean {
        val region = LeagueRegions.regionAt(teleport.coords)
        if (region != null) {
            return player.hasRegion(region)
        }
        val byGroup =
            DemonicPactsData.regions.firstOrNull { it.name.startsWith(teleport.group, ignoreCase = true) }
        return byGroup == null || player.hasRegion(byGroup)
    }

    /** Registers [action] on whichever op of [item] has a teleport-like name, held or worn. */
    private fun ScriptContext.onRelicItem(item: String, action: suspend ProtectedAccess.() -> Unit) {
        val id = runCatching { item.asRSCM(RSCMType.OBJ) }.getOrNull()
        val type = id?.let { ServerCacheManager.getItem(it) }
        if (type == null) {
            logger.warn { "Demonic Pacts League: relic item $item is not in the cache." }
            return
        }
        val held = type.interfaceOptions.indexOfFirst { it.isTeleportOp() }
        when (held) {
            0 -> onOpHeld1(item) { action() }
            1 -> onOpHeld2(item) { action() }
            2 -> onOpHeld3(item) { action() }
            3 -> onOpHeld4(item) { action() }
            else -> logger.warn { "Demonic Pacts League: no teleport op on $item (${type.interfaceOptions})." }
        }
        if (item in WEARABLE_ITEMS) {
            onOpWorn2(item) { action() }
        }
    }

    private fun String?.isTeleportOp(): Boolean =
        this != null && TELEPORT_OPS.any { it.equals(this, ignoreCase = true) }

    private companion object {
        const val TELEPORT_FILE = "demonic_pacts_relic_teleports.toml"
        const val MAX_CHOICES = 127

        val TELEPORT_ITEMS =
            mapOf(
                "obj.league_bank_heist_teleport" to RelicNames.BANK_HEIST,
                "obj.league_evil_eye_necklace" to RelicNames.EVIL_EYE,
                "obj.league_agility_map" to RelicNames.MAP_OF_ALACRITY,
                "obj.league_trailblazer_fairys_flight_teleport" to RelicNames.NATURES_ACCORD,
            )

        /** Worn op 1 is Remove; the necklace's teleport is the next one. */
        val WEARABLE_ITEMS = setOf("obj.league_evil_eye_necklace")

        val TELEPORT_OPS =
            setOf("Teleport", "Rub", "Open", "Use", "Activate", "Heist", "Read", "Commune", "Check", "Fly", "Travel")
    }
}
