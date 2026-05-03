package org.rsmod.api.player.cinematic

import dev.openrune.types.aconverted.interf.IfSubType
import net.rsprot.protocol.game.outgoing.misc.client.HideLocOps
import net.rsprot.protocol.game.outgoing.misc.client.HideNpcOps
import net.rsprot.protocol.game.outgoing.misc.client.HideObjOps
import net.rsprot.protocol.game.outgoing.misc.client.MinimapToggle
import org.rsmod.api.player.output.ClientScripts.ccDeleteAll
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenFullOverlay
import org.rsmod.api.player.ui.ifOpenSub
import org.rsmod.api.player.ui.ifSetHide
import org.rsmod.api.player.ui.ifSetText
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.enumVarBit
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player

public object Cinematic {
    private var Player.camMode by enumVarBit<CameraMode>("varbit.fov_clamp")
    private var Player.minimap by enumVarBit<MinimapState>("varbit.minimap_state")
    private var Player.hideTop by boolVarBit("varbit.cutscene_status")
    private var Player.hideHud by boolVarBit("varbit.gravestone_tli_hide")
    private var Player.acceptAid by boolVarBit("varbit.option_acceptaid")
    private var Player.acceptAidRestore by boolVarBit("varbit.accept_aid_restore")

    public fun setCameraMode(player: Player, mode: CameraMode) {
        player.camMode = mode
    }

    public fun setHideToplevel(player: Player, hide: Boolean) {
        player.hideTop = hide
    }

    public fun clearHealthHud(player: Player) {
        player.ifSetHide("component.hpbar_hud:hp", hide = true)
        ccDeleteAll(player, "component.hpbar_hud:container")
    }

    public fun setHideHealthHud(player: Player, hide: Boolean) {
        player.hideHud = hide
    }

    public fun disableAcceptAid(player: Player) {
        player.acceptAidRestore = player.acceptAid
        player.acceptAid = false
    }

    public fun restoreAcceptAid(player: Player) {
        player.acceptAid = player.acceptAidRestore
        player.acceptAidRestore = false
    }

    public fun setMinimapState(player: Player, state: MinimapState) {
        player.minimap = state
        player.client.write(MinimapToggle(state.varValue))
    }

    public fun syncMinimapState(player: Player) {
        val state = player.minimap
        player.client.write(MinimapToggle(state.varValue))
    }

    public fun setHideEntityOps(player: Player, hide: Boolean) {
        player.client.write(HideNpcOps(hide))
        player.client.write(HideLocOps(hide))
        player.client.write(HideObjOps(hide))
    }

    public fun fadeOverlay(
        player: Player,
        startColour: Int,
        startTransparency: Int,
        endColour: Int,
        endTransparency: Int,
        clientDuration: Int,
        eventBus: EventBus,
    ) {
        player.ifSetText("component.fade_overlay:message", "")
        player.ifOpenFullOverlay("interface.fade_overlay", eventBus)
        player.runClientScript(
            948,
            startColour,
            startTransparency,
            endColour,
            endTransparency,
            clientDuration,
        )
    }

    public fun closeFadeOverlay(player: Player, eventBus: EventBus) {
        player.ifCloseOverlay("interface.fade_overlay", eventBus)
    }

    // TODO: Add and publish events for these toplevel tab functions instead to allow for any
    //  "gameframe" plugin script control over what is closed and re-opened.

    public fun closeToplevelTabs(player: Player, eventBus: EventBus) {
        player.ifCloseOverlay("interface.orbs", eventBus)
        player.ifCloseOverlay("interface.xp_drops", eventBus)
        player.ifCloseOverlay("interface.combat_interface", eventBus)
        player.ifCloseOverlay("interface.stats", eventBus)
        player.ifCloseOverlay("interface.side_journal", eventBus)
        player.ifCloseOverlay("interface.inventory", eventBus)
        player.ifCloseOverlay("interface.wornitems", eventBus)
        player.ifCloseOverlay("interface.prayerbook", eventBus)
        player.ifCloseOverlay("interface.magic_spellbook", eventBus)
        player.ifCloseOverlay("interface.friends", eventBus)
        player.ifCloseOverlay("interface.account", eventBus)
        player.ifCloseOverlay("interface.settings_side", eventBus)
        player.ifCloseOverlay("interface.emote", eventBus)
        player.ifCloseOverlay("interface.music", eventBus)
    }

    public fun closeToplevelTabsLenient(player: Player, eventBus: EventBus) {
        player.ifOpenSub(
            "interface.orbs",
            "component.toplevel_osrs_stretch:orbs",
            IfSubType.Overlay,
            eventBus,
        )
        player.ifCloseOverlay("interface.xp_drops", eventBus)
        player.ifCloseOverlay("interface.combat_interface", eventBus)
        player.ifCloseOverlay("interface.stats", eventBus)
        player.ifCloseOverlay("interface.side_journal", eventBus)
        player.ifCloseOverlay("interface.inventory", eventBus)
        player.ifCloseOverlay("interface.wornitems", eventBus)
        player.ifCloseOverlay("interface.prayerbook", eventBus)
        player.ifCloseOverlay("interface.magic_spellbook", eventBus)
        player.ifOpenSub(
            "interface.friends",
            "component.toplevel_osrs_stretch:side9",
            IfSubType.Overlay,
            eventBus,
        )
        player.ifOpenSub(
            "interface.account",
            "component.toplevel_osrs_stretch:side8",
            IfSubType.Overlay,
            eventBus,
        )
        player.ifCloseOverlay("interface.settings_side", eventBus)
        player.ifCloseOverlay("interface.emote", eventBus)
        player.ifOpenSub(
            "interface.music",
            "component.toplevel_osrs_stretch:side13",
            IfSubType.Overlay,
            eventBus,
        )
    }

    public fun openTopLevelTabs(player: Player, eventBus: EventBus) {
        player.ifOpenSub(
            "interface.xp_drops",
            "component.toplevel_osrs_stretch:xp_drops",
            IfSubType.Overlay,
            eventBus,
        )

        player.ifOpenSub(
            "interface.combat_interface",
            "component.toplevel_osrs_stretch:side0",
            IfSubType.Overlay,
            eventBus,
        )

        player.ifOpenSub(
            "interface.stats",
            "component.toplevel_osrs_stretch:side1",
            IfSubType.Overlay,
            eventBus,
        )

        player.ifOpenSub(
            "interface.side_journal",
            "component.toplevel_osrs_stretch:side2",
            IfSubType.Overlay,
            eventBus,
        )

        player.ifOpenSub(
            "interface.inventory",
            "component.toplevel_osrs_stretch:side3",
            IfSubType.Overlay,
            eventBus,
        )

        player.ifOpenSub(
            "interface.wornitems",
            "component.toplevel_osrs_stretch:side4",
            IfSubType.Overlay,
            eventBus,
        )

        player.ifOpenSub(
            "interface.prayerbook",
            "component.toplevel_osrs_stretch:side5",
            IfSubType.Overlay,
            eventBus,
        )

        player.ifOpenSub(
            "interface.magic_spellbook",
            "component.toplevel_osrs_stretch:side6",
            IfSubType.Overlay,
            eventBus,
        )

        player.ifOpenSub(
            "interface.friends",
            "component.toplevel_osrs_stretch:side9",
            IfSubType.Overlay,
            eventBus,
        )

        player.ifOpenSub(
            "interface.account",
            "component.toplevel_osrs_stretch:side8",
            IfSubType.Overlay,
            eventBus,
        )

        player.ifOpenSub(
            "interface.settings_side",
            "component.toplevel_osrs_stretch:side11",
            IfSubType.Overlay,
            eventBus,
        )

        player.ifOpenSub(
            "interface.emote",
            "component.toplevel_osrs_stretch:side12",
            IfSubType.Overlay,
            eventBus,
        )

        player.ifOpenSub(
            "interface.music",
            "component.toplevel_osrs_stretch:side13",
            IfSubType.Overlay,
            eventBus,
        )
    }
}
