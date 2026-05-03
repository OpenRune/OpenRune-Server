package org.rsmod.content.interfaces.gameframe

import dev.openrune.definition.type.widget.ComponentType

internal object StandardOverlays {
    val open: List<GameframeOverlay> =
        listOf(
            GameframeOverlay("interface.chatbox", "component.toplevel_osrs_stretch:chat_container"),
            GameframeOverlay("interface.buff_bar", "component.toplevel_osrs_stretch:buff_bar"),
            GameframeOverlay(
                "interface.stat_boosts_hud",
                "component.toplevel_osrs_stretch:stat_boosts_hud",
            ),
            GameframeOverlay("interface.pm_chat", "component.toplevel_osrs_stretch:pm_container"),
            GameframeOverlay("interface.hpbar_hud", "component.toplevel_osrs_stretch:hpbar_hud"),
            GameframeOverlay("interface.pvp_icons", "component.toplevel_osrs_stretch:pvp_icons"),
            GameframeOverlay("interface.orbs", "component.toplevel_osrs_stretch:orbs"),
            GameframeOverlay("interface.xp_drops", "component.toplevel_osrs_stretch:xp_drops"),
            GameframeOverlay("interface.popout", "component.toplevel_osrs_stretch:popout"),
            GameframeOverlay("interface.ehc_worldhop",
                "component.toplevel_osrs_stretch:ehc_listener"
            ),
            GameframeOverlay("interface.stats", "component.toplevel_osrs_stretch:side1"),
            GameframeOverlay("interface.side_journal", "component.toplevel_osrs_stretch:side2"),
            GameframeOverlay("interface.inventory", "component.toplevel_osrs_stretch:side3"),
            GameframeOverlay("interface.wornitems", "component.toplevel_osrs_stretch:side4"),
            GameframeOverlay("interface.prayerbook", "component.toplevel_osrs_stretch:side5"),
            GameframeOverlay("interface.magic_spellbook", "component.toplevel_osrs_stretch:side6"),
            GameframeOverlay("interface.friends", "component.toplevel_osrs_stretch:side9"),
            GameframeOverlay("interface.account", "component.toplevel_osrs_stretch:side8"),
            GameframeOverlay("interface.logout", "component.toplevel_osrs_stretch:side10"),
            GameframeOverlay("interface.settings_side", "component.toplevel_osrs_stretch:side11"),
            GameframeOverlay("interface.emote", "component.toplevel_osrs_stretch:side12"),
            GameframeOverlay("interface.music", "component.toplevel_osrs_stretch:side13"),
            GameframeOverlay("interface.side_channels", "component.toplevel_osrs_stretch:side7"),
            GameframeOverlay("interface.combat_interface", "component.toplevel_osrs_stretch:side0"),
        )

    val move: List<String> =
        listOf(
            "component.toplevel_osrs_stretch:chat_container",
            "component.toplevel_osrs_stretch:mainmodal",
            "component.toplevel_osrs_stretch:maincrm",
            "component.toplevel_osrs_stretch:overlay_atmosphere",
            "component.toplevel_osrs_stretch:overlay_hud",
            "component.toplevel_osrs_stretch:sidemodal",
            "component.toplevel_osrs_stretch:side0",
            "component.toplevel_osrs_stretch:side1",
            "component.toplevel_osrs_stretch:side2",
            "component.toplevel_osrs_stretch:side3",
            "component.toplevel_osrs_stretch:side4",
            "component.toplevel_osrs_stretch:side5",
            "component.toplevel_osrs_stretch:side6",
            "component.toplevel_osrs_stretch:side7",
            "component.toplevel_osrs_stretch:side8",
            "component.toplevel_osrs_stretch:side9",
            "component.toplevel_osrs_stretch:side10",
            "component.toplevel_osrs_stretch:side11",
            "component.toplevel_osrs_stretch:side12",
            "component.toplevel_osrs_stretch:side13",
            "component.toplevel_osrs_stretch:sidecrm",
            "component.toplevel_osrs_stretch:pvp_icons",
            "component.toplevel_osrs_stretch:pm_container",
            "component.toplevel_osrs_stretch:orbs",
            "component.toplevel_osrs_stretch:xp_drops",
            "component.toplevel_osrs_stretch:zeah",
            "component.toplevel_osrs_stretch:floater",
            "component.toplevel_osrs_stretch:buff_bar",
            "component.toplevel_osrs_stretch:stat_boosts_hud",
            "component.toplevel_osrs_stretch:helper_content",
            "component.toplevel_osrs_stretch:hpbar_hud",
            "component.toplevel_osrs_stretch:popout",
            "component.toplevel_osrs_stretch:ehc_listener",
        )
}
