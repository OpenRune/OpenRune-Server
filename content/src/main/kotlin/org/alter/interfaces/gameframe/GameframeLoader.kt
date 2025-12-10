package org.alter.interfaces.gameframe

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.Component
import org.alter.game.ui.Gameframe
import org.alter.game.ui.GameframeOverlay
import org.alter.game.util.enum
import org.alter.game.util.vars.ComponentVarType
import org.alter.game.util.vars.IntType
import org.alter.game.util.vars.RowType
import org.alter.rscm.RSCM
import org.alter.rscm.RSCMType
import org.generated.tables.GameframeRow

object GameframeLoader {

    fun loadGameframe(): Map<String, Gameframe> {
        val mapped = mutableMapOf<String, Gameframe>()
        val rows = enum("enums.gameframe_dbrows", IntType, RowType)

        rows.forEach {
            val gameframe = loadGameframe(GameframeRow.getRow(it.value))

            mapped[gameframe.topLevel]?.let { previous ->
                error(
                    "Gameframe for toplevel already exists: '${previous.topLevel}' " +
                            "(previous=$previous, curr=$gameframe)"
                )
            }

            mapped[gameframe.topLevel] = gameframe
        }

        return mapped
    }

    private fun loadGameframe(row: GameframeRow): Gameframe {
        val mappings = enum(row.mappings, ComponentVarType, ComponentVarType).associate {
            Component(it.key) to Component(it.value)
        }

        return Gameframe(
            topLevel = RSCM.getReverseMapping(RSCMType.INTERFACES, row.toplevel),
            overlays = open,
            mappings = mappings,
            clientMode = row.clientMode,
            resizable = row.resizable,
            isDefault = row.default,
            stoneArrangement = row.stoneArrangement,
        )
    }

    fun loadMoveEvents(): Map<String, String> =
        enum("enums.toplevel_move_events", ComponentVarType, ComponentVarType).associate {
            val from = RSCM.getReverseMapping(RSCMType.COMPONENTS, it.key)
            val to = RSCM.getReverseMapping(RSCMType.COMPONENTS, it.value)
            from to to
        }

    val open: List<GameframeOverlay> = listOf(
        GameframeOverlay("interfaces.chatbox", "components.toplevel_osrs_stretch:chat_container"),
        GameframeOverlay("interfaces.buff_bar", "components.toplevel_osrs_stretch:buff_bar"),
        GameframeOverlay("interfaces.stat_boosts_hud", "components.toplevel_osrs_stretch:stat_boosts_hud"),
        GameframeOverlay("interfaces.pm_chat", "components.toplevel_osrs_stretch:pm_container"),
        GameframeOverlay("interfaces.hpbar_hud", "components.toplevel_osrs_stretch:hpbar_hud"),
        GameframeOverlay("interfaces.pvp_icons", "components.toplevel_osrs_stretch:pvp_icons"),
        GameframeOverlay("interfaces.orbs", "components.toplevel_osrs_stretch:orbs"),
        GameframeOverlay("interfaces.xp_drops", "components.toplevel_osrs_stretch:xp_drops"),
        GameframeOverlay("interfaces.popout", "components.toplevel_osrs_stretch:popout"),
        GameframeOverlay("interfaces.ehc_worldhop", "components.toplevel_osrs_stretch:ehc_listener"),
        GameframeOverlay("interfaces.stats", "components.toplevel_osrs_stretch:side1"),
        GameframeOverlay("interfaces.side_journal", "components.toplevel_osrs_stretch:side2"),
        GameframeOverlay("interfaces.inventory", "components.toplevel_osrs_stretch:side3"),
        GameframeOverlay("interfaces.wornitems", "components.toplevel_osrs_stretch:side4"),
        GameframeOverlay("interfaces.prayerbook", "components.toplevel_osrs_stretch:side5"),
        GameframeOverlay("interfaces.magic_spellbook", "components.toplevel_osrs_stretch:side6"),
        GameframeOverlay("interfaces.friends", "components.toplevel_osrs_stretch:side9"),
        GameframeOverlay("interfaces.account", "components.toplevel_osrs_stretch:side8"),
        GameframeOverlay("interfaces.logout", "components.toplevel_osrs_stretch:side10"),
        GameframeOverlay("interfaces.settings_side", "components.toplevel_osrs_stretch:side11"),
        GameframeOverlay("interfaces.emote", "components.toplevel_osrs_stretch:side12"),
        GameframeOverlay("interfaces.music", "components.toplevel_osrs_stretch:side13"),
        GameframeOverlay("interfaces.side_channels", "components.toplevel_osrs_stretch:side7"),
        GameframeOverlay("interfaces.combat_interface", "components.toplevel_osrs_stretch:side0"),
    )

    val move: List<dev.openrune.definition.type.widget.ComponentType> = listOf(
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:chat_container"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:mainmodal"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:maincrm"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:overlay_atmosphere"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:overlay_hud"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:sidemodal"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:side0"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:side1"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:side2"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:side3"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:side4"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:side5"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:side6"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:side7"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:side8"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:side9"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:side10"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:side11"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:side12"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:side13"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:sidecrm"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:pvp_icons"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:pm_container"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:orbs"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:xp_drops"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:zeah"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:floater"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:buff_bar"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:stat_boosts_hud"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:helper_content"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:hpbar_hud"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:popout"),
        ServerCacheManager.fromComponent("components.toplevel_osrs_stretch:ehc_listener"),
    )

}