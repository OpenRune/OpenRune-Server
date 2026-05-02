package org.rsmod.content.interfaces.gameframe

import dev.openrune.component
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.inter
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.enum
import dev.openrune.types.dbcol.DbColumnCodec.ComponentTypeCodec
import org.rsmod.api.table.GameframeRow
import org.rsmod.game.ui.Component

internal class GameframeLoader {
    fun loadGameframes(): Map<Int, Gameframe> {
        return loadGameframeRows()
    }

    private fun loadGameframeRows(): Map<Int, Gameframe> {

        val mapped = mutableMapOf<Int, Gameframe>()

        GameframeRow.all().forEach { entry ->
            val gameframe = loadGameframe(entry)
            val previous = mapped[gameframe.topLevel.id]
            if (previous != null) {
                val message =
                    "Gameframe for toplevel already exists: '${previous.topLevel.internalName}' " +
                        "(previous=$previous, curr=$gameframe)"
                throw IllegalStateException(message)
            }
            mapped[gameframe.topLevel.id] = gameframe
        }

        return mapped
    }

    private fun loadGameframe(row: GameframeRow): Gameframe {
        val topLevel = inter(RSCM.getReverseMapping(RSCMType.INTERFACE, row.toplevel))
        val clientMode = row.clientMode
        val resizable = row.resizable
        val isDefault = row.default
        val stoneArrangement = row.stoneArrangement

        val mappings =
            row.mappings.filterValuesNotNull().associate { Component(it.key.packed) to Component(it.value.packed) }

        return Gameframe(
            topLevel = topLevel,
            overlays = overlays,
            mappings = mappings,
            clientMode = clientMode,
            resizable = resizable,
            isDefault = isDefault,
            stoneArrangement = stoneArrangement,
        )
    }

    fun loadMoveEvents(): Map<ComponentType, ComponentType> =
        enum("enum.toplevel_move_events", ComponentTypeCodec, ComponentTypeCodec).associate {
            val from =
                RSCM.getReverseMapping(RSCMType.COMPONENT, it.key.packed).replace("component.", "")
            val to =
                RSCM.getReverseMapping(RSCMType.COMPONENT, it.value.packed)
                    .replace("component.", "")
            component(from) to component(to)
        }

    val overlays: List<GameframeOverlay> =
        listOf(
            GameframeOverlay(inter("chatbox"), component("toplevel_osrs_stretch:chat_container")),
            GameframeOverlay(inter("buff_bar"), component("toplevel_osrs_stretch:buff_bar")),
            GameframeOverlay(
                inter("stat_boosts_hud"),
                component("toplevel_osrs_stretch:stat_boosts_hud"),
            ),
            GameframeOverlay(inter("pm_chat"), component("toplevel_osrs_stretch:pm_container")),
            GameframeOverlay(inter("hpbar_hud"), component("toplevel_osrs_stretch:hpbar_hud")),
            GameframeOverlay(inter("pvp_icons"), component("toplevel_osrs_stretch:pvp_icons")),
            GameframeOverlay(inter("orbs"), component("toplevel_osrs_stretch:orbs")),
            GameframeOverlay(inter("xp_drops"), component("toplevel_osrs_stretch:xp_drops")),
            GameframeOverlay(inter("popout"), component("toplevel_osrs_stretch:popout")),
            GameframeOverlay(
                inter("ehc_worldhop"),
                component("toplevel_osrs_stretch:ehc_listener"),
            ),
            GameframeOverlay(inter("stats"), component("toplevel_osrs_stretch:side1")),
            GameframeOverlay(inter("side_journal"), component("toplevel_osrs_stretch:side2")),
            GameframeOverlay(inter("inventory"), component("toplevel_osrs_stretch:side3")),
            GameframeOverlay(inter("wornitems"), component("toplevel_osrs_stretch:side4")),
            GameframeOverlay(inter("prayerbook"), component("toplevel_osrs_stretch:side5")),
            GameframeOverlay(inter("magic_spellbook"), component("toplevel_osrs_stretch:side6")),
            GameframeOverlay(inter("friends"), component("toplevel_osrs_stretch:side9")),
            GameframeOverlay(inter("account"), component("toplevel_osrs_stretch:side8")),
            GameframeOverlay(inter("logout"), component("toplevel_osrs_stretch:side10")),
            GameframeOverlay(inter("settings_side"), component("toplevel_osrs_stretch:side11")),
            GameframeOverlay(inter("emote"), component("toplevel_osrs_stretch:side12")),
            GameframeOverlay(inter("music"), component("toplevel_osrs_stretch:side13")),
            GameframeOverlay(inter("side_channels"), component("toplevel_osrs_stretch:side7")),
            GameframeOverlay(inter("combat_interface"), component("toplevel_osrs_stretch:side0")),
        )
}
