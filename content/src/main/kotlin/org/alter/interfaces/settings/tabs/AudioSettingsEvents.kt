package org.alter.interfaces.settings.tabs

import org.alter.api.ext.boolVarBit
import org.alter.api.ext.intVarBit
import org.alter.api.ext.intVarp
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onButton
import org.alter.interfaces.settings.configs.setting_components
import kotlin.math.min

class AudioSettingsEvents : PluginEvent() {

    private var Player.optionMaster by intVarp("varp.option_master_volume")
    private var Player.optionMasterSaved by intVarBit("varbits.option_master_volume_saved")

    private var Player.optionMusic by intVarp("varp.option_music")
    private var Player.optionMusicSaved by intVarBit("varbits.option_music_saved")

    private var Player.optionSounds by intVarp("varp.option_sounds")
    private var Player.optionSoundsSaved by intVarBit("varbits.option_sounds_saved")

    private var Player.optionAreaSounds by intVarp("varp.option_areasounds")
    private var Player.optionAreaSoundsSaved by intVarBit("varbits.option_areasounds_saved")

    private var Player.unlockMessage by boolVarBit("varbits.music_unlock_text_toggle")

    override fun init() {
        onButton(setting_components.master_icon) { toggleMaster(player) }
        onButton(setting_components.master_bobble_container) { selectMasterSlider(player, slot) }

        onButton(setting_components.music_icon) { toggleMusic(player) }
        onButton(setting_components.music_bobble_container) { selectMusicSlider(player, slot) }

        onButton(setting_components.sound_icon) { toggleSounds(player) }
        onButton(setting_components.sound_bobble_container) { selectSoundSlider(player, slot) }

        onButton(setting_components.areasound_icon) { toggleAreaSounds(player) }
        onButton(setting_components.areasounds_bobble_container) { selectAreaSoundSlider(player, slot) }

        onButton(setting_components.music_toggle) { toggleUnlockMessage(player) }
    }

    private fun toggleMaster(player: Player) {
        val volume = when {
            player.optionMaster > 0 -> 0
            player.optionMasterSaved == 0 -> 100
            else -> player.optionMasterSaved
        }
        setMasterVolume(player, volume)
    }

    private fun selectMasterSlider(player: Player, comsub: Int) {
        val volume = min(100, comsub * 5)
        setMasterVolume(player, volume)
    }

    private fun setMasterVolume(player: Player, volume: Int) {
        if (volume == 0) player.optionMasterSaved = player.optionMaster
        val playMusic = player.optionMaster == 0 && volume > 0
        player.optionMaster = volume
        if (playMusic) enableMusic(player)
    }

    private fun toggleMusic(player: Player) {
        val volume = when {
            player.optionMusic > 0 -> 0
            player.optionMusicSaved == 0 -> 100
            else -> player.optionMusicSaved
        }
        setMusicVolume(player, volume)
    }

    private fun selectMusicSlider(player: Player, comsub: Int) {
        val volume = min(100, comsub * 5)
        setMusicVolume(player, volume)
    }

    private fun setMusicVolume(player: Player, volume: Int) {
        if (volume == 0) player.optionMusicSaved = player.optionMusic
        val playMusic = player.optionMusic == 0 && volume > 0
        player.optionMusic = volume
        if (playMusic) enableMusic(player)
    }

    private fun toggleSounds(player: Player) {
        val volume = when {
            player.optionSounds > 0 -> 0
            player.optionSoundsSaved == 0 -> 100
            else -> player.optionSoundsSaved
        }
        setSoundsVolume(player, volume)
    }

    private fun selectSoundSlider(player: Player, comsub: Int) {
        val volume = min(100, comsub * 5)
        setSoundsVolume(player, volume)
    }

    private fun setSoundsVolume(player: Player, volume: Int) {
        if (volume == 0) player.optionSoundsSaved = player.optionSounds
        player.optionSounds = volume
    }

    private fun toggleAreaSounds(player: Player) {
        val volume = when {
            player.optionAreaSounds > 0 -> 0
            player.optionAreaSoundsSaved == 0 -> 100
            else -> player.optionAreaSoundsSaved
        }
        setAreaSoundsVolume(player, volume)
    }

    private fun selectAreaSoundSlider(player: Player, comsub: Int) {
        val volume = min(100, comsub * 5)
        setAreaSoundsVolume(player, volume)
    }

    private fun setAreaSoundsVolume(player: Player, volume: Int) {
        if (volume == 0) player.optionAreaSoundsSaved = player.optionAreaSounds
        player.optionAreaSounds = volume
    }

    private fun toggleUnlockMessage(player: Player) {
        player.unlockMessage = !player.unlockMessage
    }

    private fun enableMusic(player: Player) {
        //TODO MUSIC PLAYER
        //player.musicPlayer.resume(player)
    }
}