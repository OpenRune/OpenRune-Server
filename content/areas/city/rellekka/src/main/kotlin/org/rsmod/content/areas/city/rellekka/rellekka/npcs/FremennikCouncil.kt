package org.rsmod.content.areas.city.rellekka.rellekka.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:the_fremennik_trials] each council member's trial task and the
//  random after-quest greetings
// TODO(content): [skill:thieving] pickpocketing Agnar, Inga and Pontak; before the Trials
//  they say "You stay away from me outerlander!" and the player is too suspicious to steal
class FremennikCouncil : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.viking_man") { startDialogue(it.npc) { mustTalkToBrundt() } }
        onOpNpc1("npc.viking_woman_indoors") { startDialogue(it.npc) { mustTalkToBrundt() } }
        onOpNpc1("npc.viking_man5") { startDialogue(it.npc) { mustTalkToBrundt() } }
        onOpNpc1("npc.viking_fisherman1") { startDialogue(it.npc) { fisherman() } }
        onOpNpc1("npc.viking_reveller_3") { startDialogue(it.npc) { manni() } }
        onOpNpc1("npc.viking_olaf") { startDialogue(it.npc) { olaf() } }
        onOpNpc1("npc.viking_peer") { startDialogue(it.npc) { peer() } }
        onOpNpc3("npc.viking_peer") { startDialogue(it.npc) { peerDeposit() } }
        onOpNpc1("npc.viking_sigli") { startDialogue(it.npc) { sigli() } }
        onOpNpc1("npc.viking_hallifred") { startDialogue(it.npc) { swensen() } }
        onOpNpc1("npc.viking_thorvald") { startDialogue(it.npc) { thorvald() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Agnar
    // https://oldschool.runescape.wiki/w/Transcript:Inga
    // https://oldschool.runescape.wiki/w/Transcript:Pontak
    private suspend fun Dialogue.mustTalkToBrundt() {
        chatNpc(angry, "I cannot speak to you outerlander! Talk to Brundt, the Chieftain!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fisherman_(Rellekka)
    private suspend fun Dialogue.fisherman() {
        chatPlayer(happy, "Hello there.")
        chatNpc(
            angry,
            "Don't talk to me outerlander. Anything you have to say to me, you can say to the " +
                "Chieftain. Goodbye.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Manni_the_Reveller
    private suspend fun Dialogue.manni() {
        chatPlayer(happy, "Hello there!")
        chatNpc(
            neutral,
            "Do not think me rude outerlander, but our customs forbid me talking to you. All " +
                "contact with outerlanders must be vetted by our chieftain, Brundt.",
        )
        chatPlayer(quiz, "Where is this Brundt?")
        chatNpc(neutral, "He is standing just over there. He will speak for the tribe.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Olaf_the_Bard
    // TODO(content): [quest:the_fremennik_trials] Music cape and hood, basilisk jaw story and
    //  the lyre teleport lines after the Trials
    private suspend fun Dialogue.olaf() {
        chatPlayer(quiz, "Hello there. So you're a bard?")
        chatNpc(
            neutral,
            "I am afraid I cannot speak to outerlanders. Besides, I am busy composing an epic.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Peer_the_Seer
    // TODO(content): [quest:the_fremennik_trials] trial task, future readings, deposit box and
    //  skeletal armour crafting after the Trials
    // TODO(content): [quest:desert_treasure_ii] combining ring icons with ancient vestiges
    private suspend fun Dialogue.peer() {
        if (SKELETAL_PIECES.none { it in player.inv }) {
            chatNpc(neutral, "I have no interest in talking to you just now outerlander.")
            return
        }
        val armour =
            choice2(
                "Wallasalki Bone Armour",
                true,
                "The Fremennik Trials",
                false,
                title = "What would you like to ask about?",
            )
        if (!armour) {
            chatNpc(neutral, "I have no interest in talking to you just now outerlander.")
            return
        }
        chatPlayer(
            quiz,
            "Hello there. I have some wallasalki bone fragments, and I understand you may be " +
                "able to use them to make some armour for me...?",
        )
        chatNpc(
            neutral,
            "I see great things in your future outerlander, but I will not go against the " +
                "wishes of the tribe, and will not make armour for an outerlander.",
        )
        chatNpc(
            neutral,
            "You may try and gain a dispensation from Chieftain Brundt, but I suspect you will " +
                "be unsuccessful...",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Peer_the_Seer
    // TODO(content): [diary:fremennik] open the deposit box after the easy Fremennik diary
    private suspend fun Dialogue.peerDeposit() {
        chatNpc(
            neutral,
            "I cannot do that for you until you have completed the easy tier of the Fremennik " +
                "achievement diary.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sigli_the_Huntsman
    // TODO(content): [quest:the_fremennik_trials] trial task and spined armour crafting after
    //  the Trials
    private suspend fun Dialogue.sigli() {
        if (SPINED_HIDES.none { it in player.inv }) {
            sigliRefusal()
            return
        }
        val armour =
            choice2(
                "Dagannoth Spined Armour",
                true,
                "The Fremennik Trials",
                false,
                title = "What would you like to ask about?",
            )
        if (!armour) {
            sigliRefusal()
            return
        }
        chatPlayer(
            quiz,
            "Hello there. I have some spined dagannoth hides, and believe you might be able to " +
                "make an armour of them for me...?",
        )
        chatNpc(
            neutral,
            "My apologies outerlander, but we are forbidden by decree to communicate with those " +
                "not of our tribe, and this includes making armour for you.",
        )
        chatNpc(
            neutral,
            "I suggest you speak to Brundt, our chieftain. He resides in the longhall and speaks " +
                "for us all.",
        )
    }

    private suspend fun Dialogue.sigliRefusal() {
        chatNpc(
            neutral,
            "I do not speak to outerlanders. If you have anything of import to say, go and " +
                "speak to the Chieftain.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Swensen_the_Navigator
    private suspend fun Dialogue.swensen() {
        chatPlayer(happy, "Hello!")
        chatNpc(
            neutral,
            "Hello outerlander. This is my home, I would be grateful if you would leave.",
        )
        chatPlayer(sad, "Oh. Okay.")
        chatNpc(
            neutral,
            "I am sorry outerlander, I will not offer you hospitality until my Chieftain has " +
                "vouched for your honesty. This is our way.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Thorvald_the_Warrior
    private suspend fun Dialogue.thorvald() {
        chatNpc(angry, "Leave me be, outerlander. I have nothing to say to the likes of you.")
    }

    private companion object {
        val SKELETAL_PIECES =
            listOf(
                "obj.dagganoth_mage_head_part",
                "obj.dagganoth_mage_body_part",
                "obj.dagganoth_mage_leg_part",
            )

        val SPINED_HIDES =
            listOf(
                "obj.dagganoth_range_head_part",
                "obj.dagganoth_range_body_part",
                "obj.dagganoth_range_leg_part",
            )
    }
}
