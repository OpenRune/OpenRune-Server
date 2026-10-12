package org.rsmod.content.minigames.giantsfoundry

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Kovac: commissions and hand-ins. https://oldschool.runescape.wiki/w/Kovac */
class KovacScript @Inject constructor(private val random: GameRandom) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(KOVAC) { talk(it.npc) }
        onOpNpc3(KOVAC) { handIn(it.npc) }
        onOpNpc4(KOVAC) { commission(it.npc) }
        // TODO: using reward items on Kovac to sell them back (80% of their cost).
    }

    /** Dialogue from https://oldschool.runescape.wiki/w/Transcript:Kovac */
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
        while (true) {
            val topic =
                choice5(
                    "How do I use the crucible?",
                    1,
                    "What does the mould jig do?",
                    2,
                    "How do I control the preform's temperature?",
                    3,
                    "When should I use a tool?",
                    4,
                    "Actually, I'm okay for now.",
                    5,
                )
            when (topic) {
                1 -> {
                    chatPlayer(neutral, "How do I use the crucible?")
                    chatNpc(neutral, "Crucible just a big bucket... We put metal in and it melt it down to make swords.")
                    chatNpc(neutral, "Crucible let us add differing metals... and can store up to 28 metal bar worth, Kovac think.")
                    chatNpc(neutral, "We can put other metal thing in too, like sword and armour.")
                }
                2 -> {
                    chatPlayer(neutral, "What does the mould jig do?")
                    chatNpc(neutral, "Mould jig very handy... It let us make differing type of sword.")
                    chatNpc(neutral, "We want use mould parts to fit condi... commission... so when setting up mould, find parts that match.")
                    chatNpc(neutral, "Some match commission better than other... so find best one for job.")
                }
                3 -> {
                    chatPlayer(neutral, "How do I control the preform's temperature?")
                    chatNpc(neutral, "That very easy... If preform too cold, dunk in lava... If preform too hot, hold to waterfall.")
                    chatNpc(neutral, "Preform need to be right temperature to work with tool.")
                    chatNpc(neutral, "Trip hammer need preform to be hot... Grindstone need preform to be warm... Polishing wheel need preform to be cold.")
                }
                4 -> {
                    chatPlayer(neutral, "When should I use a tool?")
                    chatNpc(neutral, "Hmm... Kovac think it a gut feeling thing...")
                    chatPlayer(neutral, "That doesn't help me.")
                    chatNpc(neutral, "Hmm... that is true.")
                    mesbox("Progress on your sword can be tracked via the display on screen. You can track which tool you need to use via the completion bar.")
                    // TODO: the transcript's three tool-icon boxes (trip hammer, grindstone, polishing wheel).
                }
                else -> {
                    chatPlayer(neutral, "Actually, I'm okay for now.")
                    chatNpc(neutral, "Okay.")
                    return
                }
            }
            chatNpc(neutral, "Need help with anything else?")
        }
    }

    private suspend fun ProtectedAccess.commission(npc: Npc) {
        startDialogue(npc) {
            if (player.commissionWord1 == 0) {
                giveCommission()
            } else {
                chatNpc(neutral, "Kovac give commission already. Go make me ${styleText()} sword.")
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

    // UNVERIFIED: the transcript only gives "[sword style]" in red (#ef1020); the casing and
    // wording of the two words here are a guess.
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

    /**
     * Hand-in rewards: Smithing XP, twice that in coins (to the bank), reputation equal to the
     * quality, and half the run energy back.
     * https://oldschool.runescape.wiki/w/Giants%27_Foundry#Rewards
     */
    private suspend fun Dialogue.reward(quality: Int) {
        val xp = FoundryFormulas.handInXp(quality)
        val coins = xp * 2
        val start = player.commissionStart
        val seconds = if (start == 0) 0 else (System.currentTimeMillis() / 1000).toInt() - start

        player.removePreform()
        access.closeHud()
        player.clearSword()

        access.statAdvance("stat.smithing", xp.toDouble())
        if (!access.invAdd(access.bank, "obj.coins", coins).success) {
            access.invAdd(access.inv, "obj.coins", coins)
        }
        player.foundryReputation += quality
        player.swordsMade += 1
        player.runEnergy = (player.runEnergy + RUN_ENERGY_MAX / 2).coerceAtMost(RUN_ENERGY_MAX)
        UpdateRun.energy(player, player.runEnergy)

        val firstSword = player.swordsMade == 1
        if (
            firstSword ||
                quality > player.bestQuality ||
                (quality == player.bestQuality && seconds < player.bestTime)
        ) {
            player.bestQuality = quality
            player.bestTime = seconds
        }
        mesbox(
            "Sword completed in: ${formatTime(seconds)} at quality: $quality<br>" +
                "Best completed in: ${formatTime(player.bestTime)} at quality: ${player.bestQuality}<br>" +
                "You're awarded: $xp Smithing XP and $coins coins.<br>" +
                "Total swords made in the Giants Foundry: ${player.swordsMade}"
        )
    }

    // UNVERIFIED: the transcript shows only "[time]"; minutes:seconds is a guess.
    private fun formatTime(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

    private companion object {
        const val KOVAC = "npc.giants_foundry_kovac"
        const val RUN_ENERGY_MAX = 10_000
    }
}
