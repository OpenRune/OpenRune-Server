package org.rsmod.api.stats.plugin.levelup

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.StatType
import jakarta.inject.Inject
import org.rsmod.api.player.midiJingle
import org.rsmod.api.player.midiSong
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.advanced.onAdvanceCombat
import org.rsmod.api.script.advanced.onAdvanceStat
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class LevelUpScript @Inject constructor(private val worldRepo: WorldRepository) :
    PluginScript() {
    private val unlocks by lazy { SkillUnlocks.load() }

    override fun ScriptContext.startup() {
        onAdvanceStat { advanceStat(it.args) }
        onAdvanceCombat { advanceCombat(it.args) }
    }

    private suspend fun ProtectedAccess.advanceStat(type: StatType) {
        val stat = LevelUpStat.of(type) ?: return
        val level = statBase(type.internalName)
        val guideList = vars[GUIDE_LIST_DISABLED_VARBIT] == 0
        val jingle =
            stat.jingle.select(level, type.maxLevel, unlocks.contains(type.id, level), guideList)

        mes(LevelUpText.message(stat.label, level, maxed = level >= type.maxLevel))
        fireworks(fireworksSpotanim(level, type.maxLevel))
        playJingle(jingle)

        if (vars[POPUP_DISABLED_VARBIT] == 0) {
            levelUpDisplay(
                stat.layer,
                LevelUpText.title(stat.label),
                LevelUpText.level(stat.levelPrefix, level),
            )
        }
    }

    private suspend fun ProtectedAccess.advanceCombat(level: Int) {
        val label = LevelUpStat.COMBAT_LABEL
        mes(LevelUpText.message(label, level, maxed = false))
        fireworks(LEVELUP_SPOTANIM)
        playJingle(LevelUpStat.COMBAT_JINGLE.asRSCM(RSCMType.JINGLE))

        if (vars[POPUP_DISABLED_VARBIT] == 0) {
            levelUpDisplay(
                ServerCacheManager.fromComponent(LevelUpStat.COMBAT_LAYER),
                LevelUpText.title(label),
                LevelUpText.level("Your $label level is now", level),
            )
        }
    }

    private fun ProtectedAccess.fireworks(spotanim: String) {
        spotanim(spotanim, height = FIREWORKS_HEIGHT)
        worldRepo.soundArea(player.coords, FIREWORKS_SYNTH, radius = FIREWORKS_RADIUS)
        worldRepo.soundArea(
            player.coords,
            FIREWORKS_TAIL_SYNTH,
            delay = FIREWORKS_TAIL_DELAY,
            radius = FIREWORKS_RADIUS,
        )
    }

    private fun ProtectedAccess.playJingle(jingle: Int) {
        player.midiSong(STOP_MUSIC)
        player.midiJingle(jingle)
    }

    private fun ProtectedAccess.fireworksSpotanim(level: Int, maxLevel: Int): String =
        when {
            level < maxLevel -> LEVELUP_SPOTANIM
            LevelUpStat.all.all { statBase(it.stat.internalName) >= maxLevel } ->
                "spotanim.levelup_max"
            else -> "spotanim.levelup_99_anim"
        }

    private companion object {
        const val LEVELUP_SPOTANIM = "spotanim.levelup_anim"
        const val FIREWORKS_HEIGHT = 124
        const val FIREWORKS_SYNTH = "synth.firework"
        const val FIREWORKS_TAIL_SYNTH = "synth.firework_tail"
        const val FIREWORKS_TAIL_DELAY = 25
        const val FIREWORKS_RADIUS = 1
        const val STOP_MUSIC = "midi.stop_music"
        const val POPUP_DISABLED_VARBIT = "varbit.option_level_up_message_disabled"
        const val GUIDE_LIST_DISABLED_VARBIT = "varbit.option_level_up_guide_list_disabled"
    }
}
