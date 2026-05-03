package org.rsmod.content.interfaces.settings.scripts

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.player.music.MusicPlayer
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class AudioSettingsScript @Inject constructor(private val musicPlayer: MusicPlayer) :
    PluginScript() {
    private var Player.optionMaster by intVarp("varp.option_master_volume")
    private var Player.optionMasterSaved by intVarBit("varbit.option_master_volume_saved")

    private var Player.optionMusic by intVarp("varp.option_music")
    private var Player.optionMusicSaved by intVarBit("varbit.option_music_saved")

    private var Player.optionSounds by intVarp("varp.option_sounds")
    private var Player.optionSoundsSaved by intVarBit("varbit.option_sounds_saved")

    private var Player.optionAreaSounds by intVarp("varp.option_areasounds")
    private var Player.optionAreaSoundsSaved by intVarBit("varbit.option_areasounds_saved")

    private var Player.unlockMessage by boolVarBit("varbit.music_unlock_text_toggle")

    override fun ScriptContext.startup() {
        onIfOverlayButton("component.settings_side:master_icon") { player.toggleMaster() }
        onIfOverlayButton("component.settings_side:master_bobble_container") {
            player.selectMasterSlider(it.comsub)
        }

        onIfOverlayButton("component.settings_side:music_icon") { player.toggleMusic() }
        onIfOverlayButton("component.settings_side:music_bobble_container") {
            player.selectMusicSlider(it.comsub)
        }

        onIfOverlayButton("component.settings_side:sound_icon") { player.toggleSounds() }
        onIfOverlayButton("component.settings_side:sound_bobble_container") {
            player.selectSoundSlider(it.comsub)
        }

        onIfOverlayButton("component.settings_side:areasound_icon") { player.toggleAreaSounds() }
        onIfOverlayButton("component.settings_side:areasounds_bobble_container") {
            player.selectAreaSoundSlider(it.comsub)
        }

        onIfOverlayButton("component.settings_side:music_toggle") { player.toggleUnlockMessage() }
    }

    private fun Player.toggleMaster() {
        val volume =
            when {
                optionMaster > 0 -> 0
                optionMasterSaved == 0 -> 100
                else -> optionMasterSaved
            }
        setMasterVolume(volume)
    }

    private fun Player.selectMasterSlider(comsub: Int) {
        val volume = min(100, comsub * 5)
        setMasterVolume(volume)
    }

    private fun Player.setMasterVolume(volume: Int) {
        if (volume == 0) {
            optionMasterSaved = optionMaster
        }
        val playMusic = optionMaster == 0 && volume > 0
        optionMaster = volume
        if (playMusic) {
            enableMusic()
        }
    }

    private fun Player.toggleMusic() {
        val volume =
            when {
                optionMusic > 0 -> 0
                optionMusicSaved == 0 -> 100
                else -> optionMusicSaved
            }
        setMusicVolume(volume)
    }

    private fun Player.selectMusicSlider(comsub: Int) {
        val volume = min(100, comsub * 5)
        setMusicVolume(volume)
    }

    private fun Player.setMusicVolume(volume: Int) {
        if (volume == 0) {
            optionMusicSaved = optionMusic
        }
        val playMusic = optionMusic == 0 && volume > 0
        optionMusic = volume
        if (playMusic) {
            enableMusic()
        }
    }

    private fun Player.toggleSounds() {
        val volume =
            when {
                optionSounds > 0 -> 0
                optionSoundsSaved == 0 -> 100
                else -> optionSoundsSaved
            }
        setSoundsVolume(volume)
    }

    private fun Player.selectSoundSlider(comsub: Int) {
        val volume = min(100, comsub * 5)
        setSoundsVolume(volume)
    }

    private fun Player.setSoundsVolume(volume: Int) {
        if (volume == 0) {
            optionSoundsSaved = optionSounds
        }
        optionSounds = volume
    }

    private fun Player.toggleAreaSounds() {
        val volume =
            when {
                optionAreaSounds > 0 -> 0
                optionAreaSoundsSaved == 0 -> 100
                else -> optionAreaSoundsSaved
            }
        setAreaSoundsVolume(volume)
    }

    private fun Player.selectAreaSoundSlider(comsub: Int) {
        val volume = min(100, comsub * 5)
        setAreaSoundsVolume(volume)
    }

    private fun Player.setAreaSoundsVolume(volume: Int) {
        if (volume == 0) {
            optionAreaSoundsSaved = optionAreaSounds
        }
        optionAreaSounds = volume
    }

    private fun Player.toggleUnlockMessage() {
        unlockMessage = !unlockMessage
    }

    private fun Player.enableMusic() {
        musicPlayer.resume(this)
    }
}
