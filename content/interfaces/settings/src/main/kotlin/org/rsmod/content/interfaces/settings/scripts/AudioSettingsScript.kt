package org.rsmod.content.interfaces.settings.scripts

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.player.music.MusicPlayer
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class AudioSettingsScript @Inject constructor(private val musicPlayer: MusicPlayer) :
    PluginScript() {
    private var Player.optionMaster by intVarBit("varbit.option_master_volume_desktop")
    private var Player.optionMasterLegacy by intVarp("varp.option_master_volume")
    private var Player.optionMasterSaved by intVarBit("varbit.option_master_volume_saved_desktop")
    private var Player.optionMasterSavedLegacy by intVarBit("varbit.option_master_volume_saved")

    private var Player.optionMusic by intVarBit("varbit.option_music_desktop")
    private var Player.optionMusicLegacy by intVarp("varp.option_music")
    private var Player.optionMusicSaved by intVarBit("varbit.option_music_saved_desktop")
    private var Player.optionMusicSavedLegacy by intVarBit("varbit.option_music_saved")

    private var Player.optionSounds by intVarBit("varbit.option_sounds_desktop")
    private var Player.optionSoundsLegacy by intVarp("varp.option_sounds")
    private var Player.optionSoundsSaved by intVarBit("varbit.option_sounds_saved_desktop")
    private var Player.optionSoundsSavedLegacy by intVarBit("varbit.option_sounds_saved")

    private var Player.optionAreaSounds by intVarBit("varbit.option_areasounds_desktop")
    private var Player.optionAreaSoundsLegacy by intVarp("varp.option_areasounds")
    private var Player.optionAreaSoundsSaved by intVarBit("varbit.option_areasounds_saved_desktop")
    private var Player.optionAreaSoundsSavedLegacy by intVarBit("varbit.option_areasounds_saved")

    override fun ScriptContext.startup() {
        onIfOverlayButton("component.settings_side:master_icon") { player.toggleMaster() }
        onIfOverlayButton("component.settings_side:master_bobble_container") {
            player.selectMasterSlider(it.comsub)
        }
        //onIfOverlayScriptTrigger("component.settings_side:master_slider_bobble", MASTER_VOLUME_SCRIPT) {
            //player.setMasterVolume(volumeArg(), forceMusicResume = volumeArg() > 0)
        //}

        onIfOverlayButton("component.settings_side:music_icon") { player.toggleMusic() }
        onIfOverlayButton("component.settings_side:music_bobble_container") {
            player.selectMusicSlider(it.comsub)
        }
        //onIfOverlayScriptTrigger("component.settings_side:music_slider_bobble", AUDIO_VOLUME_SCRIPT) {
            //player.setMusicVolume(volumeArg(), forceMusicResume = volumeArg() > 0)
        //}

        onIfOverlayButton("component.settings_side:sound_icon") { player.toggleSounds() }
        onIfOverlayButton("component.settings_side:sound_bobble_container") {
            player.selectSoundSlider(it.comsub)
        }
        //onIfOverlayScriptTrigger("component.settings_side:sound_slider_bobble", SOUND_VOLUME_SCRIPT) {
            //player.setSoundsVolume(volumeArg())
        //}

        onIfOverlayButton("component.settings_side:areasound_icon") { player.toggleAreaSounds() }
        onIfOverlayButton("component.settings_side:areasounds_bobble_container") {
            player.selectAreaSoundSlider(it.comsub)
        }
        //onIfOverlayScriptTrigger("component.settings_side:areasounds_slider_bobble", AREA_SOUND_VOLUME_SCRIPT) {
            //player.setAreaSoundsVolume(volumeArg())
        //}

        //onIfOverlayScriptTrigger("component.settings:settings_clickzone", SETTINGS_VOLUME_SCRIPT) {
            //player.setSettingsVolume(optionArg(), volumeArg())
        //}
    }

    private fun Player.toggleMaster() {
        val volume =
            when {
                optionMaster > 0 -> 0
                else -> UNMUTE_VOLUME
            }
        setMasterVolume(volume, forceMusicResume = volume > 0)
    }

    private fun Player.selectMasterSlider(comsub: Int) {
        val volume = min(100, comsub * 5)
        setMasterVolume(volume, forceMusicResume = volume > 0)
    }

    private fun Player.setMasterVolume(volume: Int, forceMusicResume: Boolean = false) {
        val nextVolume = normalizeUnmuteVolume(optionMaster, volume)
        setMasterSavedVolume(UNMUTE_VOLUME)
        val wasMusicMuted = optionMaster == 0 || optionMusic == 0
        val musicMuted = nextVolume == 0 || optionMusic == 0
        setMasterVolumeVar(nextVolume)
        if ((wasMusicMuted || forceMusicResume) && !musicMuted) {
            enableMusic()
        } else if (!wasMusicMuted && musicMuted) {
            disableMusic()
        }
    }

    private fun Player.toggleMusic() {
        val volume =
            when {
                optionMusic > 0 -> 0
                else -> UNMUTE_VOLUME
            }
        setMusicVolume(volume, forceMusicResume = volume > 0)
    }

    private fun Player.selectMusicSlider(comsub: Int) {
        val volume = min(100, comsub * 5)
        setMusicVolume(volume, forceMusicResume = volume > 0)
    }

    private fun Player.setMusicVolume(volume: Int, forceMusicResume: Boolean = false) {
        val nextVolume = normalizeUnmuteVolume(optionMusic, volume)
        setMusicSavedVolume(UNMUTE_VOLUME)
        val wasMusicMuted = optionMaster == 0 || optionMusic == 0
        val musicMuted = optionMaster == 0 || nextVolume == 0
        setMusicVolumeVar(nextVolume)
        if ((wasMusicMuted || forceMusicResume) && !musicMuted) {
            enableMusic()
        } else if (!wasMusicMuted && musicMuted) {
            disableMusic()
        }
    }

    private fun Player.toggleSounds() {
        val volume =
            when {
                optionSounds > 0 -> 0
                else -> UNMUTE_VOLUME
            }
        setSoundsVolume(volume)
    }

    private fun Player.selectSoundSlider(comsub: Int) {
        val volume = min(100, comsub * 5)
        setSoundsVolume(volume)
    }

    private fun Player.setSoundsVolume(volume: Int) {
        val nextVolume = normalizeUnmuteVolume(optionSounds, volume)
        setSoundsSavedVolume(UNMUTE_VOLUME)
        setSoundsVolumeVar(nextVolume)
    }

    private fun Player.toggleAreaSounds() {
        val volume =
            when {
                optionAreaSounds > 0 -> 0
                else -> UNMUTE_VOLUME
            }
        setAreaSoundsVolume(volume)
    }

    private fun Player.selectAreaSoundSlider(comsub: Int) {
        val volume = min(100, comsub * 5)
        setAreaSoundsVolume(volume)
    }

    private fun Player.setAreaSoundsVolume(volume: Int) {
        val nextVolume = normalizeUnmuteVolume(optionAreaSounds, volume)
        setAreaSoundsSavedVolume(UNMUTE_VOLUME)
        setAreaSoundsVolumeVar(nextVolume)
    }

    private fun Player.setSettingsVolume(option: Int, volume: Int) {
        when (option) {
            MASTER_VOLUME_OPTION -> setMasterVolume(volume, forceMusicResume = volume > 0)
            MUSIC_VOLUME_OPTION -> setMusicVolume(volume, forceMusicResume = volume > 0)
            SOUND_VOLUME_OPTION -> setSoundsVolume(volume)
            AREA_SOUND_VOLUME_OPTION -> setAreaSoundsVolume(volume)
        }
    }

    private fun normalizeUnmuteVolume(currentVolume: Int, requestedVolume: Int): Int =
        if (currentVolume == 0 && requestedVolume > 0) {
            UNMUTE_VOLUME
        } else {
            requestedVolume
        }

    private fun Player.setMasterVolumeVar(volume: Int) {
        optionMaster = volume
        optionMasterLegacy = volume
    }

    private fun Player.setMasterSavedVolume(volume: Int) {
        optionMasterSaved = volume
        optionMasterSavedLegacy = volume
    }

    private fun Player.setMusicVolumeVar(volume: Int) {
        optionMusic = volume
        optionMusicLegacy = volume
    }

    private fun Player.setMusicSavedVolume(volume: Int) {
        optionMusicSaved = volume
        optionMusicSavedLegacy = volume
    }

    private fun Player.setSoundsVolumeVar(volume: Int) {
        optionSounds = volume
        optionSoundsLegacy = volume
    }

    private fun Player.setSoundsSavedVolume(volume: Int) {
        optionSoundsSaved = volume
        optionSoundsSavedLegacy = volume
    }

    private fun Player.setAreaSoundsVolumeVar(volume: Int) {
        optionAreaSounds = volume
        optionAreaSoundsLegacy = volume
    }

    private fun Player.setAreaSoundsSavedVolume(volume: Int) {
        optionAreaSoundsSaved = volume
        optionAreaSoundsSavedLegacy = volume
    }

    private fun Player.enableMusic() {
        softTimer("timer.music_resume", cycles = 1)
    }

    private fun Player.disableMusic() {
        musicPlayer.stop(this)
    }

    private companion object {
        private const val MASTER_VOLUME_SCRIPT = -1213750982 // 3081216314
        private const val AUDIO_VOLUME_SCRIPT = 406545006
        private const val SOUND_VOLUME_SCRIPT = -1069744872 // 3225222424
        private const val AREA_SOUND_VOLUME_SCRIPT = -1801605717 // 2493361579
        private const val SETTINGS_VOLUME_SCRIPT = -878888337 // 3416078959

        private const val MASTER_VOLUME_OPTION = 319
        private const val MUSIC_VOLUME_OPTION = 30
        private const val SOUND_VOLUME_OPTION = 31
        private const val AREA_SOUND_VOLUME_OPTION = 32
        private const val UNMUTE_VOLUME = 5
    }
}
