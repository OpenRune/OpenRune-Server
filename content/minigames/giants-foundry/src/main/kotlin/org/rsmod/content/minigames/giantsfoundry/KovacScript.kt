package org.rsmod.content.minigames.giantsfoundry

import jakarta.inject.Inject
import java.util.Locale
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.ironman.isUltimateIronman
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal const val KOVAC = "npc.giants_foundry_kovac"

class KovacScript @Inject constructor(private val random: GameRandom) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(KOVAC) { talk(it.npc) }
        onOpNpc3(KOVAC) { handIn(it.npc) }
        onOpNpc4(KOVAC) { commission(it.npc) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) {
        startDialogue(npc) {
            chatNpc(neutral, "What human need help with?")
            if (player.commissionWord1 == 0) {
                when (
                    choice4(
                        "Can I get a new commission?",
                        1,
                        "I'm not sure what something does.",
                        2,
                        "I'd like to buy something.",
                        3,
                        "I'm okay for now.",
                        4,
                    )
                ) {
                    1 -> giveCommission()
                    2 -> help()
                    3 -> openShop()
                    4 -> okayForNow()
                }
            } else {
                when (
                    choice5(
                        "What's my current commission?",
                        1,
                        "I'm not sure what something does, can you help me?",
                        2,
                        "Can I scrap my current commission?",
                        3,
                        "I'd like to buy something.",
                        4,
                        "I'm okay for now.",
                        5,
                    )
                ) {
                    1 -> {
                        chatPlayer(neutral, "What's my current commission?")
                        chatNpc(neutral, "Go make ${styleText()} sword.")
                    }
                    2 -> help()
                    3 -> scrap()
                    4 -> openShop()
                    5 -> okayForNow()
                }
            }
        }
    }

    private suspend fun Dialogue.okayForNow() {
        chatPlayer(neutral, "I'm okay for now.")
        chatNpc(neutral, "Okay friend, Kovac here to help.")
    }

    private suspend fun Dialogue.scrap() {
        chatPlayer(neutral, "Can I scrap my current commission?")
        chatNpc(neutral, "Why want that?")
        chatPlayer(neutral, "I don't think I'm really feeling it at the moment.")
        chatNpc(
            neutral,
            "That okay, Kovac will give reward even if sword bad. Maybe you put in storage for " +
                "working later, but Kovac no scrap swords.",
        )
    }

    private suspend fun Dialogue.openShop() {
        chatPlayer(neutral, "I'd like to buy something.")
        chatNpc(neutral, "Here's what Kovac have.")
        access.openRewardShop()
    }

    private suspend fun Dialogue.help() {
        chatPlayer(neutral, "I'm not sure what something does.")
        chatNpc(neutral, "What do not understand, little one?")
        var last: HelpTopic? = null
        while (true) {
            val topic = chooseTopic(last) ?: break
            chatPlayer(neutral, topic.question)
            when (topic) {
                HelpTopic.Crucible -> {
                    chatNpc(neutral, "Crucible just a big bucket... We put metal in and it melt it down to make swords.")
                    chatNpc(neutral, "Crucible let us add differing metals... and can store up to 28 metal bar worth, Kovac think.")
                }
                HelpTopic.MouldJig -> {
                    chatNpc(neutral, "Mould jig very handy... It let us make differing type of sword.")
                    chatNpc(neutral, "We want use mould parts to fit condi... commission... so when setting up mould, find parts that match.")
                    chatNpc(neutral, "Some match commission better than other... so find best one for job.")
                }
                HelpTopic.Temperature -> {
                    chatNpc(neutral, "That very easy... If preform too cold, dunk in lava... If preform too hot, hold to waterfall.")
                    chatNpc(neutral, "Preform need to be right temperature to work with tool.")
                    chatNpc(neutral, "Trip hammer need preform to be hot... Grindstone need preform to be warm... Polishing wheel need preform to be cold.")
                }
                HelpTopic.Tool -> {
                    chatNpc(neutral, "Hmm... Kovac think it a gut feeling thing...")
                    chatPlayer(neutral, "That doesn't help me.")
                    chatNpc(neutral, "Hmm... that is true.")
                    mesbox(
                        "Progress on your sword can be tracked via the display on screen. You can " +
                            "track which tool you need to use via the completion bar."
                    )
                    for (tool in FoundryTool.entries) {
                        access.graphicbox(
                            tool.row.icon,
                            "Use the preform on the ${tool.row.name} when your<br>completion " +
                                "tracker is over this icon's bar.",
                        )
                    }
                }
            }
            last = topic
            chatNpc(neutral, "Need help with anything else?")
        }
        chatPlayer(neutral, "Actually, I'm okay for now.")
        chatNpc(neutral, "Okay.")
    }

    /** The topic asked last is left out of the next menu. */
    private suspend fun Dialogue.chooseTopic(last: HelpTopic?): HelpTopic? {
        val topics = HelpTopic.entries.filter { it != last }
        return if (topics.size == HelpTopic.entries.size) {
            choice5(
                topics[0].question,
                topics[0],
                topics[1].question,
                topics[1],
                topics[2].question,
                topics[2],
                topics[3].question,
                topics[3],
                HELP_DONE,
                null,
            )
        } else {
            choice4(
                topics[0].question,
                topics[0],
                topics[1].question,
                topics[1],
                topics[2].question,
                topics[2],
                HELP_DONE,
                null,
            )
        }
    }

    private suspend fun ProtectedAccess.commission(npc: Npc) {
        startDialogue(npc) {
            if (player.commissionWord1 == 0) {
                giveCommission()
            } else {
                chatNpc(neutral, "Kovac give commission already. Go make me ${styleTextSplit()} sword.")
            }
        }
    }

    private suspend fun ProtectedAccess.handIn(npc: Npc) {
        startDialogue(npc) { handInDialogue() }
    }

    private suspend fun Dialogue.giveCommission() {
        val (first, second) = FoundryData.commissions[random.of(0, FoundryData.commissions.lastIndex)]
        player.commissionWord1 = first.id
        player.commissionWord2 = second.id
        player.commissionStart = (System.currentTimeMillis() / 1000).toInt()
        chatNpc(neutral, "Make ${styleText()} sword.")
    }

    private fun Dialogue.styleTextSplit(): String {
        val first = CommissionWord.forId(player.commissionWord1)?.label ?: ""
        val second = CommissionWord.forId(player.commissionWord2)?.label ?: ""
        return "<col=ef1020>$first <col=ef1020>$second</col>"
    }

    private fun Dialogue.styleText(): String {
        val first = CommissionWord.forId(player.commissionWord1)?.label ?: ""
        val second = CommissionWord.forId(player.commissionWord2)?.label ?: ""
        return "<col=ef1020>$first $second</col>"
    }

    private suspend fun Dialogue.handInDialogue() {
        if (player.commissionWord1 == 0) {
            chatNpc(neutral, "You no sword to hand over. I give new commission if want?")
            offerCommission()
            return
        }
        if (!player.holdsPreform()) {
            chatNpc(neutral, "You no sword to hand over. Kovac give commission already. Go make me ${styleText()} sword.")
            return
        }
        val ruined = player.preformQuality == 0
        if (!ruined && player.preformCompletion < FoundryFormulas.SCALE) {
            chatNpc(neutral, "Hmm... sword not finished yet. Keep working.")
            return
        }
        chatNpc(neutral, if (ruined) "Sword no good. Give it here." else "Hmm... sword look good. Hand over.")
        reward(if (ruined) 0 else player.preformQuality)
        offerCommission()
    }

    private suspend fun Dialogue.offerCommission() {
        if (choice2("Yes.", true, "No.", false, title = "Receive another commission?")) {
            giveCommission()
        }
    }

    private suspend fun Dialogue.reward(quality: Int) {
        val xp = FoundryFormulas.handInXp(quality)
        val coins = xp * 2
        val start = player.commissionStart
        val seconds = if (start == 0) 0 else (System.currentTimeMillis() / 1000).toInt() - start

        if (player.isUltimateIronman) {
            // UNVERIFIED: no message for the inventory payout (guess).
            access.invAdd(access.inv, "obj.coins", coins)
        } else if (access.invAdd(access.bank, "obj.coins", coins).success) {
            access.mes("Kovac deposits ${formatCoins(coins)} coins into your bank as payment.")
        } else {
            // UNVERIFIED: a full bank is undocumented; the coins go to the inventory (guess).
            access.invAdd(access.inv, "obj.coins", coins)
        }
        player.clearSword()
        player.removePreform()
        access.closeHud()

        access.statAdvance("stat.smithing", xp.toDouble())
        player.foundryReputation += quality
        player.swordsMade += 1
        player.runEnergy = (player.runEnergy + RUN_ENERGY_MAX / 2).coerceAtMost(RUN_ENERGY_MAX)
        UpdateRun.energy(player, player.runEnergy)

        val timed = start != 0
        val bestTimed = player.bestTime > 0
        if (
            quality > player.bestQuality ||
                (quality == player.bestQuality && timed && (!bestTimed || seconds < player.bestTime))
        ) {
            player.bestQuality = quality
            // UNVERIFIED: a better sword with no known time clears the best time (guess).
            player.bestTime = if (timed) seconds else 0
        }
        // UNVERIFIED: minutes keep counting past 60, and a known time against a record without
        // one shows the timed lines (guess).
        val timeLines =
            if (timed) "Sword completed in: ${formatTime(seconds)} at quality: $quality<br>" else
                "Sword quality: $quality<br>"
        val bestLine =
            if (player.bestTime > 0) {
                "Best completed in: ${formatTime(player.bestTime)} at quality: ${player.bestQuality}<br>"
            } else {
                "Best quality ${player.bestQuality}<br>"
            }
        mesbox(
            timeLines +
                bestLine +
                "You're awarded: $xp Smithing XP and ${formatCoins(coins)} coins.<br>" +
                "Total swords made in the Giants Foundry: ${player.swordsMade}"
        )
        access.mes("Total swords made in the Giants Foundry: ${player.swordsMade}")
    }

    private fun formatTime(seconds: Int): String = "${seconds / 60}m ${seconds % 60}s"

    private fun formatCoins(coins: Int): String = String.format(Locale.US, "%,d", coins)

    private companion object {
        const val RUN_ENERGY_MAX = 10_000
        const val HELP_DONE = "Actually, I'm okay for now."
    }
}

private enum class HelpTopic(val question: String) {
    Crucible("How do I use the crucible?"),
    MouldJig("What does the mould jig do?"),
    Temperature("How do I control the preform's temperature?"),
    Tool("When should I use a tool?"),
}
