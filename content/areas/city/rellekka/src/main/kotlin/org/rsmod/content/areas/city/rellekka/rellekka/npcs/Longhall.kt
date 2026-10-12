package org.rsmod.content.areas.city.rellekka.rellekka.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.content.areas.city.rellekka.fremennikName
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:the_fremennik_trials] the hecklers' overhead lines during the bard's
//  trial
class Longhall @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.viking_reveller") { startDialogue(it.npc) { drunkReveller() } }
        onOpNpc1("npc.viking_reveller_2") { startDialogue(it.npc) { drunkReveller() } }
        onOpNpc1("npc.viking_heckler") { startDialogue(it.npc) { waitingForShow() } }
        onOpNpc1("npc.viking_heckler_2") { startDialogue(it.npc) { waitingForShow() } }
        onOpNpc1("npc.viking_heckler_4") { startDialogue(it.npc) { waitingForShow() } }
        onOpNpc1("npc.viking_ant_hambling") { startDialogue(it.npc) { hambling() } }
        onOpNpc1("npc.viking_longhall_bouncer") { startDialogue(it.npc) { bouncer() } }
        onOpNpc1("npc.viking_longhall_barkeep") { startDialogue(it.npc) { thora(it.npc) } }
        onOpNpc3("npc.viking_longhall_barkeep") {
            if (QuestRequirements.hasCompleted(player, FREMENNIK_TRIALS)) {
                player.openBar(it.npc)
            } else {
                mes("Only Fremenniks may buy drinks here.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bjorn
    // https://oldschool.runescape.wiki/w/Transcript:Eldgrim
    private suspend fun Dialogue.drunkReveller() {
        if (isFremennik()) {
            chatNpc(drunk, "Hey! Itsh you again! Whatshyerfashe!")
            chatPlayer(confused, "${player.displayName}?")
            chatNpc(drunk, "Nah nah nah, not them, the other one, whatshyerfashe!")
            chatPlayer(confused, "${fremennikName()}?")
            chatNpc(drunk, "Thatsh what I said diddle I?")
            chatPlayer(neutral, "Um.... okay. I'll leave you to your drinking.")
            chatNpc(drunk, "Thanksh pal! You're alright!")
            return
        }
        chatPlayer(happy, "Hello there.")
        chatNpc(
            drunk,
            "Hey! scheck it out! Itsh an outerlandub! Yer shud go shpeak to the chieftain!",
        )
        chatPlayer(confused, "The who?")
        chatNpc(drunk, "That guy over there by that stuff! (hic) Yeh, abshoultely! He's da bosh!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ospak
    // https://oldschool.runescape.wiki/w/Transcript:Styrmir
    // https://oldschool.runescape.wiki/w/Transcript:Fridgeir
    private suspend fun Dialogue.waitingForShow() {
        chatNpc(angry, "Shhh! I'm waiting for the show!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hambling
    private suspend fun Dialogue.hambling() {
        chatNpc(happy, "I can't wait for the show to start - our bards are always brilliant!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Longhall_Bouncer
    // TODO(content): [quest:the_fremennik_trials] the same line on opening the backstage door,
    //  and the after-bard's-trial reply while the rest of the Trials is in progress
    private suspend fun Dialogue.bouncer() {
        if (isFremennik()) {
            chatNpc(angry, "You can't go through there. Talent only, backstage.")
            chatPlayer(angry, "But I'm a Bard!")
            chatNpc(
                angry,
                "No you're not. I saw your performance. I was paid well to keep you from ever " +
                    "setting foot on stage here again.",
            )
            return
        }
        chatNpc(angry, "Hey, outerlander. You can't go through there. Talent only, backstage.")
    }

    private fun Player.openBar(npc: Npc) {
        shops.open(this, npc, "Rellekka Longhall Bar", "inv.viking_bar")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Thora_the_Barkeep
    private suspend fun Dialogue.thora(npc: Npc) {
        if (isFremennik()) {
            chatNpc(
                neutral,
                "Hello again ${fremennikName()}. I suppose you want a drink? Or are you going to " +
                    "try another scam with that terrible Askeladden again?",
            )
            chatPlayer(quiz, "Scam? Oh... You mean his promise not to come in here anymore?")
            chatNpc(
                angry,
                "Yes. Yes I do. I should never have trusted him not to come in here, even with " +
                    "that written promise.",
            )
            chatNpc(quiz, "Anyway, do you want a drink or not?")
            if (choice2("Yes please", true, "No thanks", false)) {
                player.openBar(npc)
            } else {
                chatPlayer(neutral, "No thanks.")
                chatNpc(neutral, "Okay then.")
            }
            return
        }
        chatPlayer(happy, "Hello there.")
        chatNpc(
            neutral,
            "Hello yourself, outerlander. A little out of the way up here, aren't you?",
        )
        chatNpc(
            neutral,
            "I would love to stop and chat with you, but unfortunately we have a custom that no " +
                "Fremennik may speak with the outerlanders without the permission of our " +
                "chieftain.",
        )
        chatNpc(
            neutral,
            "Don't take it personally. The Chieftain is stood just over there, his name is " +
                "Brundt. I suggest you go and introduce yourself.",
        )
        chatNpc(
            quiz,
            "You wouldn't want to go making any enemies because you weren't aware of our " +
                "customs now, would you?",
        )
        chatPlayer(neutral, "No, I guess not. Thanks anyway.")
        chatNpc(
            happy,
            "Not a problem, outerlander. It's always nice to see a new face in the Longhall; it " +
                "happens so rarely.",
        )
    }

    private fun Dialogue.isFremennik(): Boolean =
        QuestRequirements.hasCompleted(player, FREMENNIK_TRIALS)

    private companion object {
        const val FREMENNIK_TRIALS = "quest_fremenniktrials"
    }
}
