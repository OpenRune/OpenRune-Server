package org.rsmod.content.bosses.hueycoatl

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.prayerLvl
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.content.interfaces.bank.tryOpenBank
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.interact.InteractionLocOp
import org.rsmod.game.interact.InteractionOp
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.ScriptContext

internal class HueycoatlEntry @Inject constructor(
    private val fights: HueycoatlFightManager,
    private val deps: BossDeps,
    private val worldStats: HueycoatlWorldStats,
) {
    fun register(ctx: ScriptContext) {
        worldStats.load()
        with(ctx) {
            onOpLoc1(HueycoatlLocs.FIGHT_ENTRANCE) { enter() }
            onOpLoc2(HueycoatlLocs.FIGHT_ENTRANCE) { quickJoin() }
            onOpLoc3(HueycoatlLocs.FIGHT_ENTRANCE) { peek() }
            onOpLoc1(HueycoatlLocs.EXIT_SHORTCUT) { leave() }
            onOpLoc2(HueycoatlLocs.EXIT_SHORTCUT) { quickSlide() }
            onPlayerQueue(SLOPE_EXIT_QUEUE) { watchSlopeExit() }
            onOpLoc1(HueycoatlLocs.SCOREBOARD) { openScoreboard() }
            onOpLoc1("loc.bank_buffalo") { tryOpenBank() }
            onOpLoc3("loc.bank_buffalo") { ifOpenMainModal("interface.ge_collect") }
            onOpNpc1(HueycoatlNpcs.LOBBY_LEADER) { taalaDialogue(it.npc) }
            onOpNpc1(HueycoatlNpcs.LOBBY_SEER) { seerDialogue(it.npc) }
            onOpNpc1(HueycoatlNpcs.LOBBY_WORKER_2) { workerDialogue(it.npc) }
            onOpNpc1(HueycoatlNpcs.LOBBY_GUARD) {
                mesbox("The guard stares at you suspiciously.")
            }
            onIfModalButton("${HueycoatlScoreboard.COMPONENT}:close") { ifClose() }
        }
    }

    private suspend fun ProtectedAccess.enter(quick: Boolean = false) {
        if (player.vars["varbit.huey_in_area"] == 1) {
            leaveViaChain()
            return
        }
        if (player.vars["varbit.huey_intro"] == 0) {
            taalaWarning()
            return
        }
        if (!QuestRequirements.hasCompleted(player, QUEST)) {
            mes("You must complete Children of the Sun before you can fight the Hueycoatl.")
            return
        }
        val fight = fights.fightForPlayer(player)
        if (fight.participantsInParticipation().size >= MAX_PLAYERS) {
            mes("The Hueycoatl's arena is already full.")
            return
        }
        if (!quick && (player.combatLevel < MIN_COMBAT || player.prayerLvl < MIN_PRAYER)) {
            val proceed =
                choice2(
                    "Enter anyway.", true,
                    "Cancel.", false,
                    title = "Recommended: $MIN_COMBAT+ Combat and $MIN_PRAYER+ Prayer",
                )
            if (!proceed) return
        }
        VarPlayerIntMapSetter.set(player, "varbit.huey_in_area", 1)
        VarPlayerIntMapSetter.set(player, "varbit.huey_encountered", 1)
        fight.addParticipant(player)
        player.clearQueue(SLOPE_EXIT_QUEUE)
        player.queue(SLOPE_EXIT_QUEUE, 1)
        player.soundSynth(2677)
        PathingEntityCommon.telejump(player, deps.collision, fight.resolveStatic(HueycoatlCoords.ENTRY))
        fight.startIfIdle()
    }

    private suspend fun ProtectedAccess.quickJoin() {
        enter(quick = true)
    }

    private fun ProtectedAccess.peek() {
        val fight = fights.fightForPlayer(player)
        val count = fight.participantsInParticipation().size
        val phase =
            when (fight.phase) {
                HueyPhase.Idle -> "The Hueycoatl is preparing itself."
                HueyPhase.Body -> "The Hueycoatl's body can be seen on the mountainside."
                HueyPhase.Summit -> "They have reached the summit."
                HueyPhase.Shielded -> "The Hueycoatl is defending itself with its tail."
                HueyPhase.Final -> "The Hueycoatl is making its final stand."
                HueyPhase.Defeated -> "The Hueycoatl is recovering."
            }
        mes("$phase There ${if (count == 1) "is" else "are"} $count player${if (count == 1) "" else "s"} in the arena.")
    }

    private suspend fun ProtectedAccess.taalaWarning() {
        val leader = lobbyNpc(HueycoatlNpcs.LOBBY_LEADER)
        val warning =
            "Excuse me, human! Perhaps you shouldn't just go wandering through there " +
                "without knowing what you're dealing with!"
        if (leader == null) {
            mes("Taala: $warning")
            return
        }
        startDialogue(leader) { chatNpc(shocked, warning) }
    }

    private fun ProtectedAccess.lobbyNpc(symbol: String): Npc? {
        val id = symbol.asRSCM(RSCMType.NPC)
        return deps.npcRepo
            .findAll(ZoneKey.from(player.coords), zoneRadius = 1)
            .firstOrNull { it.id == id }
    }

    private suspend fun ProtectedAccess.taalaDialogue(npc: Npc) =
        startDialogue(npc) {
            if (player.vars["varbit.huey_intro"] == 1) taalaSubsequent() else taalaInitial()
        }

    private suspend fun Dialogue.taalaInitial() {
        chatPlayer(neutral, "Hello! Are you the leader of this expedition?")
        chatNpc(neutral, "Indeed I am, human! What's on your mind?")
        chatPlayer(neutral, "What's going on here?")
        taalaLore()
        chatPlayer(neutral, "How can I help?")
        taalaHelp()
    }

    private suspend fun Dialogue.taalaSubsequent() {
        while (true) {
            chatNpc(neutral, "What's on your mind, human?")
            val choice =
                choice4(
                    "Remind me, what's going on here?", 1,
                    "How can I help?", 2,
                    "Are there any rewards for helping?", 3,
                    "Goodbye.", 4,
                )
            when (choice) {
                1 -> taalaLore()
                2 -> taalaHelp()
                3 -> taalaRewards()
                else -> {
                    chatPlayer(neutral, "Goodbye.")
                    chatNpc(neutral, "Bye.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.taalaLore() {
        chatNpc(neutral, "Well, the Hueycoatl has awoken.")
        chatPlayer(quiz, "The Huey- what?")
        chatNpc(
            neutral,
            "Huey-co-at... oh, I don't have time for this! The thing you need to know is " +
                "that it's dangerous and it's coiled throughout this entire mountain!",
        )
        chatPlayer(neutral, "Do you know where it came from?")
        chatNpc(
            neutral,
            "We're not sure. There are some records of our ancestors having dealings with " +
                "it, but the details have been lost, assuming they were ever recorded.",
        )
        chatPlayer(neutral, "So it's old?")
        chatNpc(neutral, "Does it really matter how old it is?")
        chatPlayer(
            neutral,
            "I suppose not. I guess I just want to get a better understanding of what " +
                "this thing is.",
        )
        chatNpc(
            neutral,
            "We don't know how old it is. It could be extremely old, or this could be " +
                "just one in a lineage of them. Still, we haven't heard of any others so " +
                "maybe it is some sort of ancient creature.",
        )
        chatPlayer(neutral, "And given that you're here, I assume it poses some sort of threat?")
        chatNpc(
            neutral,
            "Now you're getting it! While it doesn't seem to be innately aggressive, our " +
                "concern is the potential risk of it tunnelling into Cam Torum.",
        )
        chatNpc(
            neutral,
            "Oh, and given that it does lash out from time to time, I suppose it does pose " +
                "a threat to your nearby human villages. A lesser priority for us, but " +
                "maybe something for you to care about.",
        )
        chatNpc(
            neutral,
            "Anyway, for now, our only option is to keep it in check while we figure out " +
                "what to do in the long term.",
        )
        chatNpc(
            neutral,
            "Unfortunately, that means recruiting all the help we can get, even from you humans...",
        )
    }

    private suspend fun Dialogue.taalaHelp() {
        if (QuestRequirements.hasCompleted(player, "quest_perilousmoons")) {
            chatNpc(
                neutral,
                "Well, as you've already proven yourself useful to Cam Torum, I'd be glad to " +
                    "have you help here!",
            )
        } else {
            chatNpc(
                neutral,
                "Well, despite what some of us may think of your kind, we need all the help " +
                    "we can get.",
            )
        }
        if (player.combatLevel < MIN_COMBAT) {
            chatNpc(
                neutral,
                "However, I'm not sure you have enough combat experience to face the Hueycoatl.",
            )
            mesbox(
                "To face the Hueycoatl, it's recommended you have a combat level of 85 and a " +
                    "Prayer level of 43.",
            )
            VarPlayerIntMapSetter.set(player, "varbit.huey_intro", 1)
            return
        }
        chatNpc(
            neutral,
            "We need to keep the Hueycoatl under control which, until we find a more " +
                "permanent solution, means wearing it out through combat.",
        )
        chatNpc(
            neutral,
            "Don't go killing it, mind you! Not that I think you could, but such a " +
                "magnificent creature doesn't deserve destruction.",
        )
        chatPlayer(neutral, "Do you have any tips for surviving?")
        chatNpc(
            neutral,
            "We've found our prayers can protect us from its shards, so keep an eye out for those.",
        )
        chatNpc(
            neutral,
            "To reach the Hueycoatl's head, you'll need to get it to move its great body " +
                "out of the way. We've found we can persuade it by giving the exposed " +
                "parts a helpful nudge!",
        )
        chatNpc(
            neutral,
            "Once you reach its head, you'll need to work with your comrades and the seer " +
                "to balance your prayers and weaken the beast.",
        )
        chatNpc(neutral, "But I'm sure you'll figure it out!")
        VarPlayerIntMapSetter.set(player, "varbit.huey_intro", 1)
        chatPlayer(
            neutral,
            "Move the body, then attack the head while keeping prayers balanced. Got it!",
        )
    }

    private suspend fun Dialogue.taalaRewards() {
        chatPlayer(neutral, "Are there any rewards for helping?")
        chatNpc(neutral, "Ugh, you greedy humans.")
        chatNpc(
            neutral,
            "The Hueycoatl displaces a lot of materials as it moves, including all sorts of " +
                "items and treasures.",
        )
        chatNpc(
            neutral,
            "We collect as much as we can, so I suppose you can have a share for helping.",
        )
        chatPlayer(neutral, "That sounds good, thank you.")
    }

    private suspend fun ProtectedAccess.seerDialogue(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(quiz, "Excuse me.")
            chatNpc(quiz, "What is it, human?")
            chatPlayer(neutral, "What are you doing up here? It doesn't seem very safe.")
            chatNpc(neutral, "On the contrary, it would not be safe if we were not up here!")
            chatNpc(
                neutral,
                "You see, that creature up there has a powerful divine energy about it. That " +
                    "energy is used to protect itself and interfere with our own divine capabilities.",
            )
            chatPlayer(quiz, "Divine capabilities? You mean praying to the gods?")
            chatNpc(neutral, "Praying? Yes. To the gods? That is open to interpretation.")
            chatPlayer(quiz, "It is?")
            chatNpc(
                neutral,
                "I do not have the time nor desire to explain the advanced concepts of theology " +
                    "to you, but you should know that the Dwarves of Cam Torum take a different " +
                    "approach to worship than your kind.",
            )
            chatNpc(
                neutral,
                "Anyway, the point is that without experienced seers to aid you, you may find " +
                    "yourself unable to utilise your prayers as you normally would.",
            )
            chatPlayer(neutral, "Interesting. So we need to work together to subdue this creature?")
            chatNpc(neutral, "Indeed, just follow our lead and you'll be fine... probably.")
            chatPlayer(happy, "Great! I'm happy to help!")
            chatNpc(
                neutral,
                "Good. If you have any further questions about your role here, speak to Taala.",
            )
            chatPlayer(neutral, "I will, thank you.")
        }

    private suspend fun ProtectedAccess.workerDialogue(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(neutral, "Hello!")
            chatNpc(silent, "...")
            chatNpc(confused, "Hmm? Did you say something?")
            chatPlayer(neutral, "I said hello.")
            chatNpc(neutral, "Oh, that's nice.")
            chatPlayer(neutral, "Are you okay?")
            chatNpc(neutral, "Why wouldn't I be? I'm not the one lost in the mountain!")
            chatPlayer(quiz, "Excuse me? Who's lost in the mountain?")
            chatNpc(shocked, "Karlo! Oh sweet Karlo... An inspiration to us all!")
        }

    // The escape slope only has a diagonal walkable approach, which loc op range does not accept.
    // Poll the pending op until the player reaches that tile, then run the normal exit flow.
    private suspend fun ProtectedAccess.watchSlopeExit() {
        if (player.vars["varbit.huey_in_area"] != 1) return
        val fight = fights.existingFightForPlayer(player) ?: return
        val origin = fight.resolveStatic(HueycoatlCoords.ARENA_ORIGIN)
        if (!HueycoatlArena.inParticipation(origin, player.coords)) return
        player.queue(SLOPE_EXIT_QUEUE, 1)
        val interaction = player.interaction as? InteractionLocOp ?: return
        val target = interaction.target
        if (target.id != SLOPE_LOC_ID || player.coords.level != target.level) return
        if (!player.routeDestination.isEmpty()) return
        if (abs(player.coords.x - target.x) != 1 || abs(player.coords.z - target.z) != 1) return
        player.clearInteraction()
        player.abortRoute()
        when (interaction.op) {
            InteractionOp.Op1 -> leave()
            InteractionOp.Op2 -> quickSlide()
            else -> Unit
        }
    }

    private suspend fun ProtectedAccess.leave() {
        val fight = fights.existingFightForPlayer(player)
        if (fight == null) {
            mes("You are not in the Hueycoatl encounter.")
            return
        }
        val streak = player.vars["varp.huey_perfect_streak"]
        val leave =
            choice2(
                "Leave.", true,
                "Stay.", false,
                title = "Leave the encounter? Your perfect streak ($streak) will be lost.",
            )
        if (!leave) return
        finishSlide(fight)
    }

    private fun ProtectedAccess.quickSlide() {
        val fight = fights.existingFightForPlayer(player)
        if (fight == null) {
            mes("You are not in the Hueycoatl encounter.")
            return
        }
        finishSlide(fight)
    }

    private fun ProtectedAccess.finishSlide(fight: HueycoatlFight) {
        fight.removeParticipant(player)
        VarPlayerIntMapSetter.set(player, "varbit.huey_in_area", 0)
        VarPlayerIntMapSetter.set(player, "varp.huey_perfect_streak", 0)
        PathingEntityCommon.telejump(player, deps.collision, fight.resolveStatic(HueycoatlCoords.EXIT))
        mes("You slide down the slope...")
    }

    private fun ProtectedAccess.leaveViaChain() {
        val fight = fights.existingFightForPlayer(player)
        fight?.removeParticipant(player)
        VarPlayerIntMapSetter.set(player, "varbit.huey_in_area", 0)
        VarPlayerIntMapSetter.set(player, "varp.huey_perfect_streak", 0)
        val destination =
            fight?.resolveStatic(HueycoatlCoords.CHAIN_OUTSIDE) ?: HueycoatlCoords.CHAIN_OUTSIDE
        PathingEntityCommon.telejump(player, deps.collision, destination)
        mes("You climb down the chain.")
    }

    private fun ProtectedAccess.openScoreboard() {
        val personalBest =
            player.vars["varp.huey_best_time"].let {
                if (it <= 0) NO_TIME else HueycoatlArena.formatDuration(it)
            }
        val worldBest =
            worldStats.bestTicks.let {
                if (it <= 0) NO_TIME else HueycoatlArena.formatDuration(it)
            }
        ifOpenMainModal(HueycoatlScoreboard.INTERFACE)
        ifSetText(
            "${HueycoatlScoreboard.COMPONENT}:pkc_content",
            number(player.vars["varp.total_huey_kills"].toLong()),
        )
        ifSetText(
            "${HueycoatlScoreboard.COMPONENT}:pd_content",
            number(player.vars["varp.huey_personal_deaths"].toLong()),
        )
        ifSetText("${HueycoatlScoreboard.COMPONENT}:pbt_content", personalBest)
        ifSetText("${HueycoatlScoreboard.COMPONENT}:gkc_content", number(worldStats.kills))
        ifSetText("${HueycoatlScoreboard.COMPONENT}:gd_content", number(worldStats.deaths))
        ifSetText("${HueycoatlScoreboard.COMPONENT}:gbt_content", worldBest)
        ifSetEvents("${HueycoatlScoreboard.COMPONENT}:close", 0..1, dev.openrune.definition.type.widget.IfEvent.Op1)
    }

    private fun number(value: Long): String = NumberFormat.getIntegerInstance(Locale.UK).format(value)

    private companion object {
        private const val QUEST = "quest_childrenofthesun"
        private const val MAX_PLAYERS = 20
        private const val MIN_COMBAT = 85
        private const val MIN_PRAYER = 43
        private const val NO_TIME = "0:00.00"
        private const val SLOPE_EXIT_QUEUE = "queue.huey_slope_exit"
        private val SLOPE_LOC_ID = HueycoatlLocs.EXIT_SHORTCUT.asRSCM(RSCMType.LOC)
    }
}
