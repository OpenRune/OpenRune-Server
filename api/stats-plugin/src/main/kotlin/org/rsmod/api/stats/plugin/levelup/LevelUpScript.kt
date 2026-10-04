package org.rsmod.api.stats.plugin.levelup

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.StatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.advanced.onAdvanceCombat
import org.rsmod.api.script.advanced.onAdvanceStat
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class LevelUpScript : PluginScript() {
    private val unlocks by lazy { SkillUnlocks.load() }

    override fun ScriptContext.startup() {
        onAdvanceStat { advanceStat(it.args) }
        onAdvanceCombat { advanceCombat(it.args) }
    }

    private suspend fun ProtectedAccess.advanceStat(type: StatType) {
        val stat = LevelUpStat.of(RSCM.getReverseMapping(RSCMType.STAT, type.id)) ?: return
        val level = statBase(stat.stat)
        val guideList = vars[GUIDE_LIST_DISABLED_VARBIT] == 0
        val jingle =
            stat.jingle.select(level, type.maxLevel, unlocks.contains(type.id, level), guideList)

        mes(LevelUpText.message(stat.label, level, maxed = level >= type.maxLevel))
        spotanim(fireworks(level, type.maxLevel), height = FIREWORKS_HEIGHT)
        soundSynth(FIREWORKS_SYNTH)
        midiJingle(jingle)

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
        midiJingle(LevelUpStat.COMBAT_JINGLE)

        if (vars[POPUP_DISABLED_VARBIT] == 0) {
            levelUpDisplay(
                LevelUpStat.COMBAT_LAYER,
                LevelUpText.title(label),
                LevelUpText.level("Your $label level is now", level),
            )
        }
    }

    private fun ProtectedAccess.fireworks(level: Int, maxLevel: Int): String =
        when {
            level < maxLevel -> "spotanim.levelup_anim"
            LevelUpStat.entries.all { statBase(it.stat) >= maxLevel } -> "spotanim.levelup_max"
            else -> "spotanim.levelup_99_anim"
        }

    private companion object {
        const val FIREWORKS_HEIGHT = 124
        const val FIREWORKS_SYNTH = "synth.firework"
        const val POPUP_DISABLED_VARBIT = "varbit.option_level_up_message_disabled"
        const val GUIDE_LIST_DISABLED_VARBIT = "varbit.option_level_up_guide_list_disabled"
    }
}
