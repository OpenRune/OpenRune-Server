package org.rsmod.content.areas.city.falador.npcs

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.util.Wearpos
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onOpNpc5
import org.rsmod.api.table.FacialHairStylesRow
import org.rsmod.api.table.HairStylesRow
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.Appearance
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private enum class MakeoverType(
    val interfaceType: Int,
    val stylesEnum: String,
    val kitSlot: Int,
    val request: String,
    val brochure: String,
) {
    Hair(
        interfaceType = 0,
        stylesEnum = "enum.makeover_hair_styles",
        kitSlot = 0,
        request = "I'd like a haircut please.",
        brochure = "Please select the hairstyle you would like from this brochure. " +
            "I'll even throw in a free recolour.",
    ),
    FacialHair(
        interfaceType = 1,
        stylesEnum = "enum.makeover_facial_hair_styles",
        kitSlot = 1,
        request = "I'd like a shave please.",
        brochure = "Please select the facial hair you would like from this brochure. " +
            "I'll even throw in a free recolour.",
    ),
}

class HairdresserScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(HAIRDRESSER) { startDialogue(it.npc) { hairdresser() } }
        onOpNpc4(HAIRDRESSER) { openMakeover(MakeoverType.Hair) }
        onOpNpc5(HAIRDRESSER) { openMakeover(MakeoverType.FacialHair) }

        onIfModalButton("component.makeover:item_area") { previewStyle(it) }
        onIfModalButton("component.makeover:colours") { previewColour(it) }
        onIfModalButton("component.makeover:select") { applyStyle(close = false) }
        onIfModalButton("component.makeover:select_and_close") { applyStyle(close = true) }
        onIfClose("interface.makeover") { player.revertUnappliedStyle() }
    }

    private suspend fun Dialogue.hairdresser() {
        chatNpc(
            happy,
            "Good afternoon. In need of a haircut are we? Currently we're offering this service " +
                "for free!",
        )
        val type =
            choice3(
                MakeoverType.Hair.request,
                MakeoverType.Hair,
                MakeoverType.FacialHair.request,
                MakeoverType.FacialHair,
                "No thank you.",
                null,
                title = "Select an option",
            )
        if (type == null) {
            chatPlayer(neutral, "No thank you.")
            chatNpc(neutral, "Very well. Come back if you change your mind.")
            return
        }
        chatPlayer(happy, type.request)
        chatNpc(happy, type.brochure)
        if (hasHatOn()) {
            return
        }
        access.showMakeover(type)
    }

    private suspend fun ProtectedAccess.openMakeover(type: MakeoverType) {
        startDialogue {
            if (!hasHatOn()) {
                access.showMakeover(type)
            }
        }
    }

    private suspend fun Dialogue.hasHatOn(): Boolean {
        val hat = player.worn[Wearpos.Hat.slot] ?: return false
        objbox(hat, "You should take off your hat if you want to get a haircut!")
        return true
    }

    private fun ProtectedAccess.showMakeover(type: MakeoverType) {
        val styles = type.styles()
        val kit = player.appearance.identKitSnapshot()[type.kitSlot].toInt()
        val colour = player.appearance.coloursSnapshot()[HAIR_COLOUR_SLOT].toInt()
        val current = styles.indexOfFirst { it.kitFor(player) == kit }.coerceAtLeast(0)

        vars[VAR_ACTIVE] = 1
        vars[VAR_ORIGINAL_KIT] = kit + 1
        vars[VAR_ORIGINAL_COLOUR] = colour
        vars["varbit.makeover_interface_type"] = type.interfaceType
        vars["varp.makeover_hair"] = current * SLOTS_PER_STYLE
        vars["varp.makeover_colour"] = colour

        ifOpenMainModal("interface.makeover")
        ifSetEvents("component.makeover:item_area", 0..styles.size * SLOTS_PER_STYLE, IfEvent.Op1)
        ifSetEvents("component.makeover:colours", 0..COLOUR_SLOTS, IfEvent.Op1)
        ifSetEvents("component.makeover:select", 0..10, IfEvent.Op1)
        ifSetEvents("component.makeover:select_and_close", 0..10, IfEvent.Op1)
    }

    private fun ProtectedAccess.previewStyle(button: IfModalButton) {
        val type = player.activeMakeover() ?: return
        val style = type.styles().getOrNull(button.comsub / SLOTS_PER_STYLE) ?: return
        vars["varp.makeover_hair"] = button.comsub - button.comsub % SLOTS_PER_STYLE
        player.appearance.setIdentKit(type.kitSlot, style.kitFor(player))
    }

    private fun ProtectedAccess.previewColour(button: IfModalButton) {
        player.activeMakeover() ?: return
        val colour = button.comsub / SLOTS_PER_COLOUR
        vars["varp.makeover_colour"] = colour
        player.appearance.setColour(HAIR_COLOUR_SLOT, colour)
    }

    private fun ProtectedAccess.applyStyle(close: Boolean) {
        val type = player.activeMakeover() ?: return
        val appearance = player.appearance
        vars[VAR_ORIGINAL_KIT] = appearance.identKitSnapshot()[type.kitSlot] + 1
        vars[VAR_ORIGINAL_COLOUR] = appearance.coloursSnapshot()[HAIR_COLOUR_SLOT].toInt()
        if (close) {
            ifClose()
        }
    }

    private fun Player.revertUnappliedStyle() {
        val type = activeMakeover() ?: return
        val kit = vars[VAR_ORIGINAL_KIT] - 1
        if (kit >= 0) {
            appearance.setIdentKit(type.kitSlot, kit)
        }
        appearance.setColour(HAIR_COLOUR_SLOT, vars[VAR_ORIGINAL_COLOUR])
        VarPlayerIntMapSetter.set(this, VAR_ACTIVE, 0)
    }

    private fun Player.activeMakeover(): MakeoverType? {
        if (vars[VAR_ACTIVE] == 0) {
            return null
        }
        val interfaceType = vars["varbit.makeover_interface_type"]
        return MakeoverType.entries.firstOrNull { it.interfaceType == interfaceType }
    }

    private fun MakeoverType.styles(): List<MakeoverStyle> {
        val enum =
            ServerCacheManager.getEnum(stylesEnum.asRSCM(RSCMType.ENUM)) ?: return emptyList()
        return enum.values.entries
            .sortedBy { it.key }
            .map { (_, row) ->
                val rowId = (row as Number).toInt() and 0xFFFF
                when (this) {
                    MakeoverType.Hair ->
                        HairStylesRow.getRow(rowId).let {
                            MakeoverStyle(it.playerKitIdTypeA, it.playerKitIdTypeB)
                        }
                    MakeoverType.FacialHair ->
                        FacialHairStylesRow.getRow(rowId).let {
                            MakeoverStyle(it.playerKitIdTypeA, it.playerKitIdTypeB)
                        }
                }
            }
    }

    private class MakeoverStyle(val kitTypeA: Int, val kitTypeB: Int) {
        fun kitFor(player: Player): Int =
            if (player.appearance.bodyType == Appearance.BODY_TYPE_A) kitTypeA else kitTypeB
    }

    private companion object {
        const val HAIRDRESSER = "npc.hairdresser"
        const val HAIR_COLOUR_SLOT = 0
        const val SLOTS_PER_STYLE = 5
        const val SLOTS_PER_COLOUR = 2
        const val COLOUR_SLOTS = 60

        const val VAR_ACTIVE = "varbit.falador_makeover_active"
        const val VAR_ORIGINAL_KIT = "varbit.falador_makeover_original_kit"
        const val VAR_ORIGINAL_COLOUR = "varbit.falador_makeover_original_colour"
    }
}
