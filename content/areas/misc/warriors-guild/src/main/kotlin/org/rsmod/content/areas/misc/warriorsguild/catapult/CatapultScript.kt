package org.rsmod.content.areas.misc.warriorsguild.catapult

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.lefthand
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onAiConTimer
import org.rsmod.api.script.onGameStartup
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.script.onPlayerTimer
import org.rsmod.api.table.WarguildCatapultShotsRow
import org.rsmod.content.areas.misc.warriorsguild.TokenLedger
import org.rsmod.content.areas.misc.warriorsguild.catapultStance
import org.rsmod.content.areas.misc.warriorsguild.scripts.TokenClaimScript.Companion.claimTokens
import org.rsmod.game.entity.Controller
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.isType
import org.rsmod.game.loc.LocShape
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Gamfred's catapult room. The catapult fires every 8 ticks at whoever stands on the target; with
 * the defensive shield up, picking the matching defence in the shield tab blocks the shot.
 */
class CatapultScript
@Inject
constructor(
    private val conRepo: ControllerRepository,
    private val locRepo: LocRepository,
    private val worldRepo: WorldRepository,
    private val players: PlayerList,
    private val random: GameRandom,
) : PluginScript() {
    private var cycleTick = 0
    private var loaded: WarguildCatapultShotsRow? = null

    override fun ScriptContext.startup() {
        onGameStartup {
            val controller = Controller(CONTROLLER, CATAPULT)
            conRepo.add(controller, Int.MAX_VALUE)
            controller.aiTimer(1)
        }
        onAiConTimer(CONTROLLER) { tick() }
        onPlayerQueueWithArgs<Int>(SHOT_QUEUE) { defend(it.args) }

        onOpHeld2(SHIELD) { wieldShield(it.slot) }
        onPlayerTimer(SHIELD_TIMER) { watchShield() }
        onPlayerLogin { if (player.holdsShield()) player.timer(SHIELD_TIMER, 1) }
        STANCE_BUTTONS.forEachIndexed { stance, button ->
            onIfOverlayButton(button) { player.catapultStance = stance }
        }

        onOpNpc1(GAMFRED) { startDialogue(it.npc) { gamfred() } }
        onOpNpc4(GAMFRED) { startDialogue(it.npc) { claimShield() } }
    }

    private fun tick() {
        val step = cycleTick
        cycleTick = (cycleTick + 1) % FIRE_EVERY
        when (step) {
            0 -> {
                val catapult = locRepo.findExact(CATAPULT, CATAPULT_LOC_SHAPE) ?: return
                worldRepo.locAnim(catapult, "seq.warguild_catapult_fire")
            }
            LAUNCH_TICK -> {
                val shot = WarguildCatapultShotsRow.all().random()
                loaded = shot
                val defender = players.firstOrNull { it.coords == TARGET }
                val proj =
                    if (defender != null) {
                        ProjAnim.fromCoordToPlayer(LAUNCH, defender, shot.spotanim, PROJANIM)
                    } else {
                        ProjAnim.fromCoordToCoord(LAUNCH, TARGET, shot.spotanim, PROJANIM)
                    }
                worldRepo.projAnim(proj)
            }
            LAND_TICK -> {
                val shot = loaded ?: return
                loaded = null
                for (player in players) {
                    if (player.coords == TARGET) player.queue(SHOT_QUEUE, 1, shot.rowId)
                }
            }
        }
    }

    private suspend fun ProtectedAccess.defend(rowId: Int) {
        if (coords != TARGET) return
        val shot = WarguildCatapultShotsRow.getRow(rowId)
        if (!player.holdsShield()) {
            walk(coords.translateX(PUSH_STEPS.random()))
            startDialogue {
                chatNpcSpecific(
                    "Gamfred",
                    GAMFRED,
                    neutral,
                    "Watch out! You'll need to equip the shield as soon as you're on the target " +
                        "spot else you could get hit! Speak to me to get one, and make sure both " +
                        "your hands are free to equip it.",
                )
            }
            return
        }
        if (player.catapultStance == shot.stance) {
            anim(RSCM.getReverseMapping(RSCMType.SEQ, shot.defendSeq.id))
            statAdvance("stat.defence", DEFENCE_XP)
            TokenLedger.credit(player, TOKENS)
            mes("You successfully defend against the ${shot.name}.")
        } else {
            anim(RSCM.getReverseMapping(RSCMType.SEQ, shot.failSeq.id))
            queueHit(1, HitType.Typeless, random.of(MIN_FAIL_DAMAGE, MAX_FAIL_DAMAGE))
            mes("You fail defending against the ${shot.name}.")
        }
    }

    private fun ProtectedAccess.wieldShield(slot: Int) {
        if (coords != TARGET) {
            mes("You can only wield this shield while standing on the target.")
            return
        }
        if (player.righthand != null) {
            mes("You need both hands free to wield this shield.")
            return
        }
        invEquip(slot)
        if (!player.holdsShield()) return
        showDefenceTab()
        timer(SHIELD_TIMER, 1)
    }

    private fun ProtectedAccess.watchShield() {
        if (!player.holdsShield()) {
            restoreTabs()
            return
        }
        if (coords == TARGET) {
            timer(SHIELD_TIMER, 1)
            return
        }
        wornUnequip(Wearpos.LeftHand.slot)
        if (player.holdsShield()) {
            timer(SHIELD_TIMER, 1)
            return
        }
        restoreTabs()
    }

    private fun ProtectedAccess.showDefenceTab() {
        ifOpenOverlay(DEFENCE_TAB, COMBAT_TAB)
        for (button in STANCE_BUTTONS) ifSetEvents(button, -1..-1, IfEvent.Op1)
        for ((interf, _) in HIDDEN_TABS) ifCloseSub(interf)
    }

    private fun ProtectedAccess.restoreTabs() {
        ifOpenOverlay("interface.combat_interface", COMBAT_TAB)
        for ((interf, target) in HIDDEN_TABS) ifOpenOverlay(interf, target)
    }

    private fun Player.holdsShield(): Boolean = lefthand?.isType(SHIELD) == true

    private fun Player.hasShield(): Boolean = holdsShield() || SHIELD in inv

    private suspend fun Dialogue.gamfred() {
        chatNpc(
            happy,
            "Ello there. I'm Gamfred, the engineer in this here guild. Have you seen my catapult?",
        )
        when (
            choice5(
                "That's not a catapult, it's a large crossbow.",
                Topic.Crossbow,
                "Yes, beautiful piece of engineering.",
                Topic.Beautiful,
                "No, where is it?",
                Topic.Where,
                "May I claim my tokens please?",
                Topic.Tokens,
                "Bye!",
                Topic.Bye,
            )
        ) {
            Topic.Crossbow -> {
                chatPlayer(neutral, "That's not a catapult, it's a large crossbow.")
                chatNpc(
                    angry,
                    "WHAT!? I'll have you know that is the finest piece of dwarven engineering " +
                        "for miles around! How DARE you insult my work!",
                )
            }
            Topic.Beautiful -> beautiful()
            Topic.Where -> {
                chatPlayer(quiz, "No, where is it?")
                val kid = if (player.appearance.bodyType == Constants.bodytype_a) "lad" else "lass"
                chatNpc(
                    neutral,
                    "Are ye blind $kid? Tis over there in the next room with me assistant " +
                        "working it!",
                )
            }
            Topic.Tokens -> claimTokens()
            else -> {
                chatPlayer(neutral, "Bye!")
                chatNpc(happy, "Come back soon! My catapult needs more test subjects.")
            }
        }
    }

    private suspend fun Dialogue.beautiful() {
        chatPlayer(happy, "Yes, beautiful piece of engineering.")
        chatNpc(happy, "Nice to meet someone who appreciates fine work, have you tried it out yet?")
        when (choice2("Yes.", Topic.Beautiful, "No, how do I do that?", Topic.Where)) {
            Topic.Beautiful -> {
                chatPlayer(neutral, "Yes.")
                chatNpc(quiz, "What did you think?")
                when (
                    choice4(
                        "It was ok I guess.",
                        Topic.Crossbow,
                        "It was fun!",
                        Topic.Beautiful,
                        "I didn't like it.",
                        Topic.Where,
                        "May I have a shield please?",
                        Topic.Tokens,
                    )
                ) {
                    Topic.Crossbow -> {
                        chatPlayer(neutral, "It was ok I guess.")
                        chatNpc(neutral, "Well I guess not everyone will like it.")
                    }
                    Topic.Beautiful -> {
                        chatPlayer(happy, "It was fun!")
                        chatNpc(happy, "Glad ta hear it. Try it again sometime. We have more tests to run.")
                    }
                    Topic.Where -> {
                        chatPlayer(sad, "I didn't like it.")
                        chatNpc(
                            neutral,
                            "Well I guess not everyone will like it. But give it another chance " +
                                "before you go.",
                        )
                    }
                    else -> claimShield()
                }
            }
            else -> {
                chatPlayer(quiz, "No, how do I do that?")
                chatNpc(
                    neutral,
                    "Well ye take the big defence shield in both hands and watch the catapult. My " +
                        "assistant will fire different things at you and you need to defend " +
                        "against them.",
                )
                when (choice2("May I have a shield please?", Topic.Tokens, "Sounds boring.", Topic.Bye)) {
                    Topic.Tokens -> claimShield()
                    else -> {
                        chatPlayer(neutral, "Sounds boring.")
                        chatNpc(sad, "Your loss...")
                    }
                }
            }
        }
    }

    private suspend fun Dialogue.claimShield() {
        chatPlayer(quiz, "May I have a shield please?")
        if (player.hasShield()) {
            chatNpc(happy, "Silly muffin, you have one already!")
            return
        }
        if (player.inv.isFull()) {
            chatNpc(neutral, "I'd be happy ta give you a shield but you've no space for it.")
            return
        }
        chatNpc(happy, "Of course!")
        if (player.invAdd(player.inv, SHIELD).success) mesbox("The dwarf hands you a large shield.")
    }

    private enum class Topic {
        Crossbow,
        Beautiful,
        Where,
        Tokens,
        Bye,
    }

    private companion object {
        const val CONTROLLER = "controller.warguild_catapult"
        const val SHOT_QUEUE = "queue.warguild_catapult_shot"
        const val SHIELD_TIMER = "timer.warguild_shield"
        const val PROJANIM = "projanim.warguild_catapult"
        const val SHIELD = "obj.warguild_shield"
        const val GAMFRED = "npc.warguild_gamfred"
        const val DEFENCE_TAB = "interface.warguild_defence_mini"
        const val COMBAT_TAB = "component.toplevel_osrs_stretch:side0"
        const val FIRE_EVERY = 8
        const val LAUNCH_TICK = 3
        const val LAND_TICK = 6
        const val DEFENCE_XP = 10.0
        const val TOKENS = 1
        const val MIN_FAIL_DAMAGE = 1
        const val MAX_FAIL_DAMAGE = 5

        val CATAPULT = CoordGrid(2840, 3552, 1)
        val LAUNCH = CoordGrid(2842, 3554, 1)
        val TARGET = CoordGrid(2842, 3545, 1)
        val CATAPULT_LOC_SHAPE = LocShape.CentrepieceStraight
        val PUSH_STEPS = listOf(-1, 1)

        val STANCE_BUTTONS =
            listOf(
                "component.warguild_defence_mini:stab",
                "component.warguild_defence_mini:crush",
                "component.warguild_defence_mini:slash",
                "component.warguild_defence_mini:magic",
            )

        val HIDDEN_TABS =
            listOf(
                "interface.side_journal" to "component.toplevel_osrs_stretch:side2",
                "interface.prayerbook" to "component.toplevel_osrs_stretch:side5",
                "interface.magic_spellbook" to "component.toplevel_osrs_stretch:side6",
                "interface.settings_side" to "component.toplevel_osrs_stretch:side11",
                "interface.emote" to "component.toplevel_osrs_stretch:side12",
                "interface.music" to "component.toplevel_osrs_stretch:side13",
            )
    }
}
