package org.rsmod.api.stats.plugin.levelup

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.midiJingle
import org.rsmod.api.player.midiSong
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.advanced.onAdvanceCombat
import org.rsmod.api.script.advanced.onAdvanceStat
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.game.entity.Player
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.game.stat.StatAdvance
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class LevelUpScript
@Inject
constructor(private val worldRepo: WorldRepository, private val worldQueues: WorldQueueList) :
    PluginScript() {
    private val unlocks by lazy { SkillUnlocks.load() }
    private val guideUnlocks by lazy { LevelUpUnlocks.load() }

    override fun ScriptContext.startup() {
        onAdvanceStat { advanceStat(it.args) }
        onAdvanceCombat { advanceCombat(it.args) }
        onIfOpen("interface.levelup_display") {
            VarPlayerIntMapSetter.set(player, "varbit.busy", 1)
        }
        onIfClose("interface.levelup_display") {
            VarPlayerIntMapSetter.set(player, "varbit.busy", 0)
        }
        onPlayerLogin { VarPlayerIntMapSetter.set(player, FLASHSIDE_VARBIT, 0) }
        onIfOverlayButton(STATS_STONE) { VarPlayerIntMapSetter.set(player, FLASHSIDE_VARBIT, 0) }
    }

    private suspend fun ProtectedAccess.advanceStat(advance: StatAdvance) {
        val type = advance.stat
        val stat = LevelUpStat.of(type) ?: return
        val level = statBase(type.internalName)
        val guideList = vars[GUIDE_LIST_DISABLED_VARBIT] == 0
        val jingle =
            stat.jingle.select(level, type.maxLevel, unlocks.contains(type.id, level), guideList)

        val guideUnlocked = guideUnlocks.between(stat.guide, advance.previousLevel, level)
        if (guideUnlocked.isNotEmpty()) {
            skillGuidePrompt(stat, advance.previousLevel, guideUnlocked, guideList)
        }
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

    private fun ProtectedAccess.skillGuidePrompt(
        stat: LevelUpStat,
        previousLevel: Int,
        unlocked: List<Int>,
        guideList: Boolean,
    ) {
        // UNVERIFIED: the guide list setting turning off the list, flash and hint (guess)
        if (guideList) {
            if (player.vars[stat.listVarbit] == 0) {
                vars[stat.listVarbit] = previousLevel + 1
            }
            vars[FLASHSIDE_VARBIT] = FLASHSIDE_STATS
        }
        val chatboxList = vars[CHATBOX_LIST_VARBIT] == 1
        val player = player
        worldQueues.add(GUIDE_PROMPT_DELAY) {
            if (!player.isSlotAssigned) {
                return@add
            }
            if (!chatboxList) {
                if (guideList) {
                    player.mes(LevelUpText.guideUnlocks(stat.guide, stat.label), ChatType.LevelUpMessage)
                }
                return@add
            }
            player.chatboxList(stat.guide, unlocked)
            if (guideList) {
                worldQueues.add(1) { player.chatboxListHint(stat, unlocked.size) }
            }
        }
    }

    private fun Player.chatboxList(guide: Int, unlocked: List<Int>) {
        val rows =
            IntArray(CHATBOX_LIST_SLOTS) {
                if (it < CHATBOX_LIST_SHOWN && it < unlocked.size) unlocked[it] else -1
            }
        runClientScript(CHATBOX_LIST_SCRIPT.asRSCM(RSCMType.CLIENTSCRIPT), guide, rows)
    }

    private fun Player.chatboxListHint(stat: LevelUpStat, unlocked: Int) {
        if (!isSlotAssigned) {
            return
        }
        val hint =
            if (unlocked > CHATBOX_LIST_SHOWN) {
                LevelUpText.guideMoreUnlocks(stat.guide, stat.label)
            } else {
                LevelUpText.guideInformation(stat.guide, stat.label)
            }
        mes(hint, ChatType.LevelUpMessage)
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
        const val CHATBOX_LIST_VARBIT = "varbit.option_level_up_chatbox_list"
        const val FLASHSIDE_VARBIT = "varbit.flashside"
        const val FLASHSIDE_STATS = 2
        const val STATS_STONE = "component.toplevel_osrs_stretch:stone1"
        const val CHATBOX_LIST_SCRIPT = "clientscript.[clientscript,script7781]"
        const val CHATBOX_LIST_SLOTS = 4
        const val CHATBOX_LIST_SHOWN = 3
        const val GUIDE_PROMPT_DELAY = 2
    }
}
