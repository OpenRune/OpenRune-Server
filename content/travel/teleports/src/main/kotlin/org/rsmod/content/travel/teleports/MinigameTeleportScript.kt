package org.rsmod.content.travel.teleports

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfSubType
import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.clearMapFlag
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.table.MinigameTeleportRow
import org.rsmod.api.utils.time.epochMinute
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The channels side tab and the Grouping minigame teleport.
 *
 * Each channels tab opens its panel into `side_channels:contents`. In Grouping, picking a minigame
 * is client-side (`grouping_viewchannel` stores it in a varc), and the client then rebuilds the
 * Teleport button as `grouping:teleport` with the minigame's id as its subcomponent, so the click
 * carries the id of the `minigame_teleport` row. Landing tiles are the `minigame` rows of
 * `teleport_destination`, matched by name.
 */
class MinigameTeleportScript
@Inject
constructor(
    private val teleportValidator: PlayerTeleportValidator,
    private val areaChecker: AreaChecker,
) : PluginScript() {
    private val landings: Map<Int, TeleportDestination> by lazy {
        val byName = TeleportNetworks.of(NETWORK).associateBy { it.name }
        MinigameTeleportRow.all()
            .mapNotNull { row -> byName[row.name]?.let { row.id to it } }
            .toMap()
    }

    override fun ScriptContext.startup() {
        onPlayerLogin {
            for (tab in TAB_PANELS.indices) {
                player.ifSetEvents("component.side_channels:tab_$tab", -1..-1, IfEvent.Op1)
            }
        }
        for ((tab, panel) in TAB_PANELS.withIndex()) {
            onIfOverlayButton("component.side_channels:tab_$tab") { openTab(tab, panel) }
        }
        onIfOverlayButton(TELEPORT_BUTTON) { teleport(it.comsub) }
        for (spell in SPELLS) {
            onIfOverlayButton(spell) { openList() }
        }
        for (tile in 1..LIST_TILES) {
            onIfModalButton("component.minigames:minigame_$tile") { teleport(listGameId(tile)) }
        }
        onPlayerQueueWithArgs<PendingTeleport>(QUEUE) { processTeleport(it.args) }
    }

    /** The Minigame Teleport spell opens the scrolling list of every minigame. */
    private fun ProtectedAccess.openList() {
        ifOpenMainModal(LIST)
        for (tile in 1..LIST_TILES) {
            ifSetEvents("component.minigames:minigame_$tile", -1..-1, IfEvent.Op1)
        }
    }

    /**
     * `minigames_init` lays out tile `minigame_N` for entry N - 1 of the list enum, whose value is
     * that minigame's `minigame_teleport` row.
     */
    private fun listGameId(tile: Int): Int {
        val rows = ServerCacheManager.getEnum(LIST_ROWS.asRSCM(RSCMType.ENUM))?.values ?: return -1
        val row = (rows[tile - 1] as? Number)?.toInt() ?: return -1
        return MinigameTeleportRow.getRow(row).id
    }

    private fun ProtectedAccess.openTab(tab: Int, panel: String) {
        vars[SELECTED_TAB] = tab
        ifOpenSub(panel, CONTENTS, IfSubType.Overlay)
        if (panel == GROUPING) {
            ifSetEvents(TELEPORT_BUTTON, 0..MAX_GAME_ID, IfEvent.Op1)
            ifSetEvents("component.grouping:dropdown_contents", 0..MAX_GAME_ID, IfEvent.Op1)
        }
    }

    /**
     * Same staged cast as Home Teleport: a weak queue steps through the chalk circle, book and
     * portal, so walking away cancels it. The cooldown starts only once the player lands.
     */
    private fun ProtectedAccess.teleport(gameId: Int) {
        val landing = landings[gameId]
        if (landing == null) {
            mes("Select a minigame from the list first.")
            return
        }
        if (actionDelay > mapClock) {
            return
        }
        // UNVERIFIED: refusal wording while moving or in combat copies Home Teleport's
        if (player.routeDestination.isNotEmpty() || isInCombat()) {
            mes("You can't use that teleport at the moment.")
            return
        }
        val minutesLeft = COOLDOWN_MINUTES - (epochMinute() - vars[TIMER])
        if (minutesLeft > 0) {
            // UNVERIFIED: cooldown message wording (guess)
            val unit = if (minutesLeft == 1) "minute" else "minutes"
            mes("You need to wait another $minutesLeft $unit to use a minigame teleport.")
            return
        }
        if (!canTeleport()) {
            return
        }
        ifClose()
        player.clearMapFlag()
        resetAnim()
        resetSpotanim()
        walkTrigger(CANCEL_ANIM_WALK_TRIGGER)
        clearWeakQueue(QUEUE)
        val task = PendingTeleport(landing.coords.packed, gameId, stage = 0)
        weakQueue(QUEUE, STAGES.first().tick + SAME_TICK_QUEUE, task)
    }

    private suspend fun ProtectedAccess.processTeleport(task: PendingTeleport) {
        val stage = STAGES[task.stage]
        if (stage.spotanim != null) {
            spotanim(stage.spotanim, height = stage.spotanimHeight)
        }
        anim(stage.anim)
        val next = STAGES.getOrNull(task.stage + 1)
        if (next != null) {
            val cycles = next.tick - stage.tick + SAME_TICK_QUEUE
            weakQueue(QUEUE, cycles, task.copy(stage = task.stage + 1))
            return
        }

        vars[BUSY] = 1
        delay(LANDING_TICK - stage.tick)
        player.clearWalkTrigger()
        val allowed = canTeleport()
        if (allowed) {
            vars[TIMER] = epochMinute()
        }
        vars[BUSY] = 0
        if (allowed) {
            telejump(CoordGrid(task.destination), TeleportType.Exempt)
            landings[task.gameId]?.let { mes("You teleport to ${it.name}.") }
        }
        resetAnim()
        resetSpotanim()
    }

    private fun ProtectedAccess.canTeleport(): Boolean {
        val denial = teleportValidator.validate(player, TeleportType.Minigame, areaChecker)
        if (denial == null) {
            return true
        }
        player.clearMapFlag()
        mes(denial, ChatType.Engine)
        return false
    }

    private data class PendingTeleport(val destination: Int, val gameId: Int, val stage: Int)

    private data class Stage(
        val anim: String,
        val spotanim: String?,
        val tick: Int,
        val spotanimHeight: Int = 0,
    )

    private companion object {
        private const val NETWORK = "minigame"
        private const val SELECTED_TAB = "varbit.side_channels_tab_selected"
        private const val CONTENTS = "component.side_channels:contents"
        private const val GROUPING = "interface.grouping"
        private const val TELEPORT_BUTTON = "component.grouping:teleport"
        private const val MAX_GAME_ID = 40
        private const val LIST = "interface.minigames"
        private const val LIST_ROWS = "enum.minigames_list_rows"
        private const val LIST_TILES = 21

        private val SPELLS =
            listOf("standard", "ancient", "lunar", "arceuus").map {
                "component.magic_spellbook:teleport_minigame_$it"
            }

        /** Chat-channel, Your Clan, View another clan, Grouping: `enum_3839` order. */
        private val TAB_PANELS =
            listOf(
                "interface.chatchannel_current",
                "interface.clans_sidepanel",
                "interface.clans_guest_sidepanel",
                GROUPING,
            )

        private const val QUEUE = "queue.minigame_teleport"
        private const val TIMER = "varp.slug2_regionuid"
        private const val COOLDOWN_MINUTES = 20
        private const val BUSY = "varbit.busy"
        private const val CANCEL_ANIM_WALK_TRIGGER = "walktrigger.emote_cancelanim"
        private const val LANDING_TICK = 24

        // A weak queue added before or during queue processing is decremented on that same tick.
        private const val SAME_TICK_QUEUE = 1

        private val STAGES =
            listOf(
                Stage("seq.aide_drawing_chalk_circle", "spotanim.aide_chalk_circle", tick = 1),
                Stage("seq.aide_sitting_down_crosslegged", spotanim = null, tick = 7),
                Stage("seq.aide_player_getting_book", "spotanim.aide_player_book_get", tick = 13),
                Stage(
                    "seq.aide_reciting_incantation",
                    "spotanim.aide_player_book_portal",
                    tick = 17,
                    spotanimHeight = 65526,
                ),
                Stage("seq.aide_player_teleporting", "spotanim.aide_player_teleport", tick = 21),
            )
    }
}
