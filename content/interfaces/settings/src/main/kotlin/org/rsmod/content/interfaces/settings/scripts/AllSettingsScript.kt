package org.rsmod.content.interfaces.settings.scripts

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onDialogInput
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.table.SettingsConfigsRow
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext


val SettingsConfigsRow.varValue: String? get() = varbit?.let {
    RSCM.getReverseMapping(RSCMType.VARBIT, it)
} ?: varp?.let {
    RSCM.getReverseMapping(RSCMType.VARP, it)
}

class AllSettingsScript @Inject constructor(private val protectedAccess: ProtectedAccessLauncher) :
    PluginScript() {

    data class SettingsClick(
        val value: Int,
        val setting: Setting,
    )

    internal data class ConfirmationSetting(
        val setting: Setting,
        val title: String,
        val action: ConfirmationAction,
    )

    internal enum class ConfirmationAction {
        NoOp,
        ResetOpaqueChatColours,
        ResetSplitChatColours,
        ResetQuestListColours,
        ResetVolumeSliders,
        ResetKeybinds,
    }

    private var Player.settingsCategory by intVarBit("varbit.settings_category")
    private var Player.selectedSetting by intVarBit("varbit.settings_selected_setting")
    private var Player.isSearching by intVarBit("varbit.floater_is_searching")
    private var Player.searchListenForKeyboard by intVarBit("varbit.floater_search_listen_for_keyboard")
    private var Player.chatboxOpened by intVarBit("varbit.floater_chatbox_opened")
    private var Player.settingsColourModalOpened by intVarBit("varbit.settings_colour_modal_opened")
    private val Player.newAccount by boolVarBit("varbit.new_player_account")

    private var Player.selectedCategory: Int?
        get() = attr[SELECTED_CATEGORY]
        set(value) {
            if (value == null) {
                attr.remove(SELECTED_CATEGORY)
            } else {
                attr.put(SELECTED_CATEGORY, value)
            }
        }

    override fun ScriptContext.startup() {

        onPlayerLogin {
            player.setDefaultOptions()
        }

        onDialogInput {
            if (player.settingsColourModalOpened == 1) {
                val setting = Settings.getSetting(player.selectedSetting)
                val colour = count
                if (colour == 2147483647) return@onDialogInput
                SettingUtils.setColour(player, colour, setting)
            }
        }

        onIfOpen("interface.settings") {
            player.updateIfEvents()
            player.settingsColourModalOpened = 0
        }

        onIfOpen("interface.colour_pallet") {
            player.updateColourPickerEvents()
        }

        onIfOverlayButton("component.settings:close") {
            player.ifCloseOverlay("interface.settings", eventBus)
            runClientScript(2158)
            player.settingsColourModalOpened = 0
        }

        onIfOverlayButton("component.settings:searchbar_image") {
            player.selectedCategory = -1
            player.beginSearch()
        }

        onIfOverlayButton("component.settings:categories_clickzone") {
            player.selectCategory(it.comsub)
            player.settingsCategory = it.comsub
            player.endSearch()
            runClientScript(2158)
        }

        onIfOverlayButton("component.settings:tooltip_inside_if_clickzone") {
            VarPlayerIntMapSetter.toggle(player, "varbit.settings_disable_tooltip_in_interface")
        }

        onIfOverlayButton("component.settings:declutter_button_clickzone") {
            VarPlayerIntMapSetter.toggle(player, "varbit.option_settings_declutter")
        }

        onIfOverlayButton("component.settings_side:settings_open") {
            ifOpenOverlay("interface.settings")
            player.selectCategory(0)
            player.settingsCategory = 0
            player.endSearch()
        }

        onIfOverlayButton("component.settings:dropdown_buttons") {
            val setting = Settings.getSetting(player.selectedSetting)
            val dropdownIndex = it.comsub / 3
            SettingUtils.setDropdown(player, dropdownIndex, setting)
        }

        onIfOverlayButton("component.settings:settings_clickzone") {
            val settings = player.selectedCategory?.let { index ->
                Settings.getCategory(index)?.settings
            } ?: Settings.allSettings

            val setting = settings[it.comsub]
            selectSetting(SettingsClick(it.comsub, setting))
        }
    }

    private fun Player.beginSearch() {
        isSearching = 1
        searchListenForKeyboard = 1
    }

    private fun Player.endSearch() {
        isSearching = 0
        searchListenForKeyboard = 0
    }

    private fun Player.updateIfEvents() {
        ifSetEvents("component.settings:categories_clickzone", 0..9, IfEvent.Op1)
        ifSetEvents(
            "component.settings:dropdown_buttons",
            0..512,
            IfEvent.Op1,
        )
        ifSetEvents(
            "component.settings:settings_clickzone",
            0..512,
            IfEvent.Op1,
        )
    }

    private fun Player.updateColourPickerEvents() {
        ifSetEvents("component.colour_pallet:pallet_colours", 0..152, IfEvent.Op1)
        ifSetEvents("component.colour_pallet:pallet_custom_clickzone", 0..4, IfEvent.Op1)
    }

    private fun Player.selectCategory(category: Int) {
        selectedCategory = category
    }

    private suspend fun ProtectedAccess.selectSetting(settingsClick: SettingsClick) {
        val settingId = settingsClick.setting.id
        val setting = settingsClick.setting
        val type = setting.type

        player.selectedSetting = settingId

        when (type) {
            SettingType.CHECKBOX -> {
                setting.row?.varValue?.let {
                    VarPlayerIntMapSetter.toggle(player, it)
                }
            }

            SettingType.SLIDER -> {}

            SettingType.INPUT -> setNumberSetting(setting)
            SettingType.COLOUR_PICKER -> setColourSetting(setting)
            SettingType.BUTTON -> confirmSetting(setting)

            // Handled elsewhere.
            SettingType.DROPDOWN, SettingType.KEYBIND -> {}

            else -> println("$type not implemented for setting id=$settingId")
        }
    }

    private suspend fun ProtectedAccess.setNumberSetting(setting: Setting) {
        player.chatboxOpened = 1
        player.isSearching = 0

        val value = countDialog(setting.row?.prompt ?: "Enter amount:")
        SettingUtils.setNumber(player, value, setting)
    }

    private fun ProtectedAccess.setColourSetting(setting: Setting) {
        player.settingsColourModalOpened = 1

        val varValue = setting.row?.varValue ?: return

        ifOpenOverlay("interface.colour_pallet", "component.settings:popup")

        val defaultColour = if (vars[varValue] == 0) {
            setting.defaultColour
        } else {
            vars[varValue]
        }

        runClientScript(
            COLOUR_PALLET_OPEN_CLIENTSCRIPT,
            "component.settings:popup".asRSCM(),
            defaultColour,
        )

        runClientScript(4020)
    }

    private suspend fun ProtectedAccess.confirmSetting(info: Setting) {
        val setting = confirmationSettings.find { it.setting == info } ?: return
        val confirmed = choice2("Yes.", true, "No.", false, title = setting.title)

        if (!confirmed) {
            return
        }

        when (setting.action) {
            ConfirmationAction.NoOp -> Unit
            ConfirmationAction.ResetOpaqueChatColours -> {
                resetColours(player, opaqueChatColourSettingIds)
                mes("Default opaque colours restored.")
            }
            ConfirmationAction.ResetSplitChatColours -> {
                resetColours(player, splitChatColourSettingIds)
                mes("Default split chat colours restored.")
            }
            ConfirmationAction.ResetQuestListColours -> {
                resetColours(player, questListColourSettingIds)
                mes("Default quest list text colours restored.")
            }
            ConfirmationAction.ResetVolumeSliders -> {
                player.resetVolumeSliders()
                mes("Your volume sliders have been reset to their default values.")
            }
            ConfirmationAction.ResetKeybinds -> player.resetDefaultKeybinds()
        }
    }

    private fun Player.resetVolumeSliders() {
        setVolume(
            "varp.option_master_volume",
            "varbit.option_master_volume_desktop",
            MASTER_VOLUME_DEFAULT,
        )
        setVolume(
            "varp.option_music",
            "varbit.option_music_desktop",
            MUSIC_VOLUME_DEFAULT,
        )
        setVolume(
            "varp.option_sounds",
            "varbit.option_sounds_desktop",
            SOUND_VOLUME_DEFAULT,
        )
        setVolume(
            "varp.option_areasounds",
            "varbit.option_areasounds_desktop",
            AREA_SOUND_VOLUME_DEFAULT,
        )

        setSavedVolume(
            "varbit.option_master_volume_saved",
            "varbit.option_master_volume_saved_desktop",
        )
        setSavedVolume(
            "varbit.option_music_saved",
            "varbit.option_music_saved_desktop",
        )
        setSavedVolume(
            "varbit.option_sounds_saved",
            "varbit.option_sounds_saved_desktop",
        )
        setSavedVolume(
            "varbit.option_areasounds_saved",
            "varbit.option_areasounds_saved_desktop",
        )
    }


    private fun Player.setDefaultOptions() {
        if (!newAccount) return

        resetDefaultKeybinds()
        resetVolumeSliders()

        VarPlayerIntMapSetter.set(this, "varbit.keybinding_esc_to_close", 1)
        VarPlayerIntMapSetter.set(this, "varbit.option_collection_new_item", 1)
        VarPlayerIntMapSetter.set(this, "varp.option_attackpriority", 2)
        VarPlayerIntMapSetter.set(this, "varp.option_attackpriority_npc", 2)
        VarPlayerIntMapSetter.set(this, "varbit.bank_hidedepositinv", 1)

        VarPlayerIntMapSetter.set(this, "varbit.bounty_teleport_warning", 1)
        VarPlayerIntMapSetter.set(this, "varbit.dareeyak_teleport_warning", 1)
        VarPlayerIntMapSetter.set(this, "varbit.carrallagar_teleport_warning", 1)
        VarPlayerIntMapSetter.set(this, "varbit.annakarl_teleport_warning", 1)
        VarPlayerIntMapSetter.set(this, "varbit.ghorrock_teleport_warning", 1)
    }

    private fun Player.setVolume(legacy: String, desktop: String, value: Int) {
        VarPlayerIntMapSetter.set(this, legacy, value)
        VarPlayerIntMapSetter.set(this, desktop, value)
    }

    private fun Player.setSavedVolume(legacy: String, desktop: String) {
        VarPlayerIntMapSetter.set(this, legacy, UNMUTE_VOLUME)
        VarPlayerIntMapSetter.set(this, desktop, UNMUTE_VOLUME)
    }

    internal val confirmationSettings by lazy {
        listOf(
            ConfirmationSetting(
                setting = Settings.getSetting(107),
                title = "Are you sure you want to reset your opaque chatbox colours?",
                action = ConfirmationAction.ResetOpaqueChatColours,
            ),
            ConfirmationSetting(
                setting = Settings.getSetting(109),
                title = "Are you sure you want to reset your split chat colours?",
                action = ConfirmationAction.ResetSplitChatColours,
            ),
            ConfirmationSetting(
                setting = Settings.getSetting(228),
                title = "Are you sure you want to reset your quest list text colours?",
                action = ConfirmationAction.ResetQuestListColours,
            ),
            ConfirmationSetting(
                setting = Settings.getSetting(468),
                title = "Are you sure you want to reset your volume sliders?",
                action = ConfirmationAction.ResetVolumeSliders,
            ),
            ConfirmationSetting(
                setting = Settings.getSetting(490),
                title = "Are you sure you want to wipe playlist 1?",
                action = ConfirmationAction.NoOp,
            ),
            ConfirmationSetting(
                setting = Settings.getSetting(491),
                title = "Are you sure you want to wipe playlist 2?",
                action = ConfirmationAction.NoOp,
            ),
            ConfirmationSetting(
                setting = Settings.getSetting(492),
                title = "Are you sure you want to wipe playlist 3?",
                action = ConfirmationAction.NoOp,
            ),
            ConfirmationSetting(
                setting = Settings.getSetting(58),
                title = "Are you sure you want to reset your keybinds?",
                action = ConfirmationAction.ResetKeybinds,
            ),
        )
    }

    fun Player.resetDefaultKeybinds() {
        for (entry in Settings.getDefaultKeybinds()) {
            val setting: Setting = entry.key
            val value: Int = entry.value
            VarPlayerIntMapSetter.set(this, setting.row?.varValue ?: continue, value)
        }
    }

    fun resetColours(player: Player, settingIds: Iterable<Int>) {
        settingIds.forEach { id ->
            val settingInfo = Settings.getSetting(id)
            SettingUtils.setColour(
                player,
                settingInfo.defaultColour,
                settingInfo,
            )
        }
    }

    private companion object {
        private val SELECTED_CATEGORY = AttributeKey<Int>()
        private const val COLOUR_PALLET_OPEN_CLIENTSCRIPT = 4185
        val opaqueChatColourSettingIds = listOf(87, 89, 92, 94, 97, 99, 101, 103, 105, 196, 434)
        val splitChatColourSettingIds = listOf(96)
        val questListColourSettingIds = listOf(224, 225, 226, 227)

        private const val MASTER_VOLUME_DEFAULT = 100
        private const val MUSIC_VOLUME_DEFAULT = 20
        private const val SOUND_VOLUME_DEFAULT = 45
        private const val AREA_SOUND_VOLUME_DEFAULT = 30
        private const val UNMUTE_VOLUME = 5
    }
}
