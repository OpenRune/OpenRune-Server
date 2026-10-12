package org.rsmod.content.skills.fishing.scripts

import org.rsmod.api.config.Constants
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.fishingLvl
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Kylie_Minnow
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class KylieMinnow : PluginScript() {

    override fun ScriptContext.startup() {
        for (npc in listOf(KYLIE_UNLOCKED, KYLIE_LOCKED, KYLIE_MULTI)) {
            onOpNpc1(npc) { talkTo(it.npc) }
            onOpNpc3(npc) { openTrade(it.npc) }
        }
    }

    private suspend fun ProtectedAccess.talkTo(npc: Npc) {
        startDialogue(npc) {
            if (player.vars[ACCESS_VARBIT] == ACCESS_GRANTED) {
                trade()
            } else {
                askForAccess()
            }
        }
    }

    private suspend fun ProtectedAccess.openTrade(npc: Npc) {
        startDialogue(npc) { exchange() }
    }

    private suspend fun Dialogue.askForAccess() {
        if (player.vars[ACCESS_VARBIT] == 0) {
            chatNpc(angry, "Strewth! Nippy little blighters!")
            chatPlayer(confused, "Excuse me?")
            chatNpc(happy, "Oh G'day! Sorry, didn't see you there!")
            chatPlayer(neutral, "That's okay. What seems to be the problem?")
            chatNpc(
                sad,
                "Oh, well I wanted to catch some minnows so I could use 'em as bait, but the " +
                    "little blighters are just too quick! I managed to get 'em all rounded up over " +
                    "on my fishing platform, but I still can't catch 'em!",
            )
            chatNpc(
                angry,
                "Then, to make things worse, any time I do finally catch some, those bloody " +
                    "flying fish jump out at me and gobble up me minnows!",
            )
            chatNpc(neutral, "What I need is someone who can catch 'em for me!")
            chatPlayer(happy, "Well, maybe I could help out?")
            chatNpc(
                neutral,
                "Well I don't know. I don't want to be letting just anyone onto me fishing " +
                    "platform! But I guess if you could show me that you are a worthy fisher than " +
                    "that would be okay. I can even give you shark in exchange",
            )
            chatNpc(neutral, "for the minnows you catch, 'cos them I can catch no problem!")
            chatPlayer(quiz, "Sounds fair. What can I do to show that I am worthy?")
            chatNpc(
                neutral,
                "Well you should be very experienced in the art of fishing, own the proper " +
                    "attire, and be recognised by your peers as a great fisherman.",
            )
            mesbox(
                "You must have level 82 Fishing, be wearing the complete Angler's outfit and " +
                    "have completed Fishing Contest in order to suitably impress Kylie."
            )
            VarPlayerIntMapSetter.set(player, ACCESS_VARBIT, 1)
        }
        chatPlayer(quiz, "So, how about letting me out onto your fishing platform?")
        // TODO(content): [verify:in_game] Which line wins when several requirements are missing.
        val contestDone = QuestRequirements.hasCompleted(player, FISHING_CONTEST)
        if (player.fishingLvl >= 82 && access.hasFullAngler() && contestDone) {
            chatNpc(
                happy,
                "Ripper! You certainly seem to be the real deal, so sure, go ahead and take the " +
                    "boat over there. Oh, and don't worry about wearing that angling gear if yous " +
                    "don't want to, I just wanted to see that you took fishing",
            )
            chatNpc(happy, "serious enough to have the right attire.")
            VarPlayerIntMapSetter.set(player, ACCESS_VARBIT, ACCESS_GRANTED)
            return
        }
        when {
            player.fishingLvl < 82 ->
                chatNpc(sad, "Sorry, but I just don't think you are experienced enough.")
            !access.hasFullAngler() ->
                chatNpc(sad, "Sorry, but I just don't think you are wearin' suitable fishing gear.")
            else -> {
                val title =
                    if (player.appearance.bodyType == Constants.bodytype_a) {
                        "fisherman"
                    } else {
                        "fisherwoman"
                    }
                chatNpc(sad, "Sorry, but I just don't think you are renowned enough as a $title.")
            }
        }
        mesbox(
            "You must have level 82 Fishing, be wearing the complete Angler's outfit and have " +
                "completed Fishing Contest in order to suitably impress Kylie."
        )
    }

    private suspend fun Dialogue.trade() {
        chatNpc(
            happy,
            "Have you got any minnows for trade? I have plenty of nice, fresh sharks waiting for ya!",
        )
        val option = choice2("Yes, let's trade.", 1, "No thanks.", 2)
        if (option != 1) {
            chatPlayer(neutral, "No thanks.")
            return
        }
        chatPlayer(happy, "Yes, let's trade.")
        exchange(greet = true)
    }

    private suspend fun Dialogue.exchange(greet: Boolean = false) {
        val minnows = access.invTotal(access.inv, MINNOW)
        if (minnows < 40) {
            chatNpc(
                sad,
                "You'll be needing at least 40 minnows to trade for a shark! " +
                    "Come back and see me when you have some more!",
            )
            return
        }
        if (access.inv.isFull()) {
            chatNpc(neutral, "I can't trade you any sharks while you don't have any space for them!")
            return
        }

        // TODO(content): [verify:in_game] The Trade option opens the minnow exchange without this line.
        if (greet) {
            chatNpc(
                happy,
                "I can give you a shark for every 40 minnows that you give me. How many sharks " +
                    "would you like?",
            )
        }
        val maxSharks = minnows / 40
        val requested = access.countDialog("How many sharks? (max $maxSharks)").coerceIn(0, maxSharks)
        if (requested <= 0) return

        var given = 0
        while (given < requested && access.invTotal(access.inv, MINNOW) >= 40) {
            access.invDel(access.inv, MINNOW, 40)
            val result = access.invAdd(access.inv, RAW_SHARK, 1)
            if (result.failure) {
                access.invAdd(access.inv, MINNOW, 40)
                break
            }
            given++
        }

        // TODO(content): [verify:in_game] Kylie's line after the exchange.
        if (given > 0) {
            chatNpc(happy, "There you go! Enjoy your shark${if (given == 1) "" else "s"}!")
        } else {
            chatNpc(neutral, "I can't trade you any sharks while you don't have any space for them!")
        }
    }

    private companion object {
        private const val KYLIE_UNLOCKED = "npc.minnow_fisherman_unlocked"
        private const val KYLIE_LOCKED = "npc.minnow_fisherman_locked"
        private const val KYLIE_MULTI = "npc.minnow_fisherman_multi"
        private const val MINNOW = "obj.minnow"
        private const val RAW_SHARK = "obj.raw_shark"
        private const val ACCESS_VARBIT = "varbit.minnow_access"
        private const val FISHING_CONTEST = "quest_fishingcontest"
        private const val ACCESS_GRANTED = 2
    }
}
