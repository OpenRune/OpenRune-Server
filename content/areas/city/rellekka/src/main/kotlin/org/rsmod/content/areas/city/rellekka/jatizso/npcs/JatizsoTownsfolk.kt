package org.rsmod.content.areas.city.rellekka.jatizso.npcs

import dev.openrune.types.MesAnimType
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.content.areas.city.rellekka.fremennikName
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class JatizsoTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.frisd_beggertax") { startDialogue(it.npc) { eric() } }
        onOpNpc1("npc.fris_mine_guard") { startDialogue(it.npc) { gruvaPatrull() } }
        onOpNpc1("npc.frisd_village_miner") { startDialogue(it.npc) { miner() } }
        onOpNpc1("npc.frisd_village_miner2") { startDialogue(it.npc) { miner() } }
        onOpNpc1("npc.fris_r_guardizso") { startDialogue(it.npc) { throneRoomGuard() } }
        onOpNpc1("npc.fris_r_guardizso2") { startDialogue(it.npc) { gateGuards() } }
        onOpNpc1("npc.frisd_izso_viking_shouter") { startDialogue(it.npc) { watchtowerGuard() } }
        onOpNpc1("npc.fris_r_kingscat") { startDialogue(it.npc) { hrafn() } }
        onOpNpcU("npc.fris_r_kingscat") { startDialogue(it.npc) { hrafnGift() } }
        onOpNpc1("npc.fris_r_chancellor") { startDialogue(it.npc) { thorkel() } }
        onOpNpcU("npc.fris_r_chancellor") { startDialogue(it.npc) { thorkelGift() } }
        onOpNpc1("npc.fris_red_axe_secretary") { startDialogue(it.npc) { brendt() } }
        onOpNpc1("npc.fris_red_axe_trader") { startDialogue(it.npc) { grundt() } }
        onOpNpc1("npc.frisd_izso_viking_woman1") { startDialogue(it.npc) { citizen() } }
        onOpNpc1("npc.frisd_izso_viking_woman2") { startDialogue(it.npc) { citizen() } }
        onOpNpc1("npc.frisd_izso_viking_woman4") { startDialogue(it.npc) { citizen() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Eric_(Jatizso)
    // TODO(content): [quest:the_fremennik_isles] window and beard tax collecting (beard tax gives
    // a cabbage)
    private suspend fun Dialogue.eric() {
        val title = if (access.isBodyTypeA()) "mister" else "miss"
        chatNpc(sad, "Spare us a few coppers $title")
        chatPlayer(angry, "No, go away!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gruva_Patrull
    private suspend fun Dialogue.gruvaPatrull() {
        chatNpc(neutral, "Ho! Outlander.")
        chatPlayer(quiz, "What's down the scary-looking staircase?")
        chatNpc(
            neutral,
            "These are the stairs down to the mining caves. There are rich veins of many types " +
                "down there, and miners too. Though be careful; some of the trolls occasionally " +
                "sneak into the far end of the cave.",
        )
        chatPlayer(happy, "Thanks. I'll look out for them.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Miner_(Jatizso)
    private suspend fun Dialogue.miner() {
        when (access.random.of(6)) {
            0 -> {
                chatNpc(worried, "Gah! Trolls everywhere. There's no escape!")
                chatPlayer(neutral, "You could just leave the mine.")
                chatNpc(confused, "Oh, I'd never thought of that.")
            }
            1 -> {
                chatNpc(happy, "This place rocks!")
                chatPlayer(neutral, "No it doesn't, it stays perfectly still.")
                chatNpc(angry, "Bah! Be quiet, pedant.")
            }
            2 -> {
                chatNpc(happy, "High hoe, high hoe!")
                chatPlayer(quiz, "Why are you singing about farming implements at altitude?")
                chatNpc(
                    confused,
                    "I don't know, I've never thought about it. Ask my Dad, he taught me the song.",
                )
            }
            3 -> {
                chatPlayer(quiz, "How's your prospecting going?")
                chatNpc(happy, "Mine's going pretty well.")
                chatPlayer(neutral, "...")
                chatNpc(laugh, "Mine? Mine...you get it?")
                chatPlayer(bored, "Oh, I got it. That doesn't make it funny though.")
                chatNpc(neutral, "Suit yourself.")
            }
            4 -> {
                chatNpc(
                    worried,
                    "I've been here for two days straight. When I close my eyes I see rocks.",
                )
                chatPlayer(quiz, "Why would anyone stay here for so long?")
                chatNpc(worried, "I fear the outside. I fear the big light.")
                chatPlayer(
                    worried,
                    "Oh dear. Being underground for so long may have driven you mad.",
                )
            }
            else -> {
                chatNpc(sad, "My back is killing me!")
                chatPlayer(neutral, "Could be worse, it could be the trolls killing you.")
                chatNpc(bored, "How very droll.")
                chatPlayer(laugh, "No, troll.")
                chatNpc(angry, "Bah! I resist your attempts at jollity.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Guard_(Jatizso) (throne room)
    // TODO(content): [quest:the_fremennik_isles] "Halt, in the name of King Gjuki Sorvott IV." and
    // the other in-quest lines
    private suspend fun Dialogue.throneRoomGuard() {
        chatNpc(neutral, "Welcome, ${fremennikName()}.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Guard_(Jatizso) (guard in a pair)
    // TODO(content): [verify:in_game] which guard of the pair speaks as Rightie and the chat titles
    private suspend fun Dialogue.gateGuards() {
        chatNpc(quiz, "Are you all right? Leftie?")
        leftie(neutral, "No, I'm on the left.")
        chatNpc(
            neutral,
            "Only from your perspective. Someone entering the gate should call you Rightie, " +
                "right Leftie?",
        )
        leftie(quiz, "Right, Rightie. So you'd be Leftie not Rightie, right?")
        chatNpc(happy, "That's right Leftie, that's right.")
        leftie(quiz, "Rightie-oh Rightie, or should I call you Leftie?")
        chatNpc(happy, "No, Rightie's fine Leftie.")
        chatPlayer(
            angry,
            "Aaagh! Enough! If either of you mention left or right in my presence I'll have to " +
                "scream! Can I come through the gate?",
        )
        leftie(neutral, "Don't let us stop you.")
        val title = if (access.isBodyTypeA()) "sir" else "madam"
        chatNpc(happy, "Yes, head right on in, $title.")
        chatPlayer(verymad, "You said it! You said it! ARRRRRRRRGGH!")
    }

    private suspend fun Dialogue.leftie(
        mesanim: MesAnimType,
        text: String,
    ) = chatNpcSpecific("Guard", "npc.fris_r_guardizso2", mesanim, text)

    // https://oldschool.runescape.wiki/w/Transcript:Guard_(Jatizso_watchtower)
    // TODO(content): [system:tower_shouting] Watch-shouting op and the overhead insult exchange
    // with the Neitiznot tower guard after "IF YOU LIKE!"
    private suspend fun Dialogue.watchtowerGuard() {
        chatPlayer(quiz, "What are you doing here?")
        chatNpc(angry, "I'M ON SHOUTING DUTY!")
        chatPlayer(neutral, "No need to shout.")
        chatNpc(angry, "I'M SORRY I'VE BEEN SHOUTING INSULTS SO LONG I CAN'T HELP IT!")
        chatPlayer(quiz, "Who are you insulting?")
        chatNpc(angry, "THE TOWER IN NEITIZNOT. THEY SHOUT INSULTS BACK.")
        chatPlayer(confused, "Err, why?")
        chatNpc(angry, "THE KING TELLS US TO.")
        chatPlayer(neutral, "Your King is a strange person.")
        when (choice2("Can I watch? I'm curious.", 1, "Oh well, I'd better get going", 2)) {
            1 -> {
                chatPlayer(quiz, "Can I watch? I'm curious.")
                chatNpc(angry, "IF YOU LIKE!")
            }
            2 -> chatPlayer(neutral, "Oh well, I'd better get going")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:HRH_Hrafn
    private suspend fun Dialogue.hrafn() {
        chatPlayer(happy, "Hello cat.")
        if (CATSPEAK_AMULET !in player.worn) {
            chatNpc(neutral, "Meow!")
            return
        }
        chatNpc(bored, "One does not wish to be disturbed.")
        chatPlayer(confused, "What?")
    }

    // https://oldschool.runescape.wiki/w/Transcript:HRH_Hrafn (given any non-quest item)
    // TODO(content): [quest:the_fremennik_isles] raw tuna delivery
    private suspend fun Dialogue.hrafnGift() {
        mesbox(
            "Hrafn Glances at your gift with a disdain normally reserved for animal dung. He's " +
                "unimpressed."
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Thorkel_Silkbeard (after The Fremennik Isles)
    // TODO(content): [quest:the_fremennik_isles] pre-quest and in-quest dialogue
    private suspend fun Dialogue.thorkel() {
        chatNpc(
            worried,
            "I hear you killed the Troll King. Thanks you for that, however I'm afraid I can't " +
                "really talk to you any more, the King would be unhappy!",
        )
        chatPlayer(happy, "Good luck with dealing with the King. You'll need it!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Thorkel_Silkbeard (given any non-quest item)
    // TODO(content): [quest:the_fremennik_isles] tax bag and other quest items
    private suspend fun Dialogue.thorkelGift() {
        chatNpc(confused, "What on earth am I supposed to do with that? Take it away.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Brendt (after delivering their ore)
    // TODO(content): [quest:the_fremennik_isles] dialogue before the ore delivery
    private suspend fun Dialogue.brendt() {
        chatPlayer(quiz, "Why are you still here?")
        chatNpc(happy, "Well it seemed rude not to finish the food they gave us.")
        chatNpcSpecific(VANLIGGA_TITLE, VANLIGGA, angry, "Feel free to leave it. Please leave.")
        chatNpcSpecific("Grundt", GRUNDT, happy, "I'll just finish the roast off.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Grundt (after delivering their ore)
    // TODO(content): [quest:the_fremennik_isles] dialogue before the ore delivery
    private suspend fun Dialogue.grundt() {
        chatNpc(happy, "Ah, yes. Now that's what I call a meal.")
        chatNpcSpecific(
            VANLIGGA_TITLE,
            VANLIGGA,
            angry,
            "That's funny, I called it fourteen meals yesterday.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Freygerd_(Jatizso)
    // https://oldschool.runescape.wiki/w/Transcript:Lensa_(Jatizso)
    // https://oldschool.runescape.wiki/w/Transcript:Sassilik_(Jatizso)
    private suspend fun Dialogue.citizen() {
        when (access.random.of(4)) {
            0 -> {
                chatPlayer(happy, "How are you today?")
                chatNpc(sad, "**sigh**")
                chatPlayer(neutral, "That good? Everyone around here seems a little depressed.")
                chatNpc(sad, "**sigh**")
                chatPlayer(neutral, "And not particularly talkative.")
                chatNpc(sad, "**sigh**")
                chatPlayer(
                    neutral,
                    "I'll leave you to your sighing. It looks like you have plenty to do.",
                )
            }
            1 -> {
                chatPlayer(quiz, "It's a bit grey round here, isn't it?")
                chatNpc(
                    sad,
                    "It gets you down after a while, you know. There are 273 shades of grey, you " +
                        "know, and we have them all.",
                )
                chatPlayer(laugh, "That's grey-t.")
                chatNpc(
                    sad,
                    "That attempt at humour merely made me more depressed. Leave me alone.",
                )
            }
            2 -> {
                chatPlayer(quiz, "How's the King treating you then?")
                chatNpc(sad, "Like serfs.")
                chatPlayer(confused, "Serf?")
                chatNpc(
                    angry,
                    "Yes, you know - peons, plebs, the downtrodden. He treats us like his own " +
                        "personal possessions.",
                )
                chatPlayer(neutral, "You should leave this place.")
                chatNpc(
                    sad,
                    "I keep trying to save up enough to leave, but the King keeps taxing us! We " +
                        "have no money left.",
                )
                chatPlayer(sad, "Oh dear.")
            }
            else -> {
                chatPlayer(happy, "Cheer up! It's not the end of the world.")
                chatNpc(
                    angry,
                    "I'd prefer that, if it meant I didn't have to talk to people as inanely " +
                        "happy as you.",
                )
                chatPlayer(shocked, "Whoa! I think you need to get out more.")
            }
        }
    }

    private companion object {
        const val CATSPEAK_AMULET = "obj.ics_little_amulet_of_catspeak"
        const val VANLIGGA_TITLE = "Vanligga Gastfrihet"
        const val VANLIGGA = "npc.frisd_izso_landlady"
        const val GRUNDT = "npc.fris_red_axe_trader"
    }
}
