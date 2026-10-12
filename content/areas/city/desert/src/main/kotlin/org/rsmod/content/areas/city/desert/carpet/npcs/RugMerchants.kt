package org.rsmod.content.areas.city.desert.carpet.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:magic_carpet_travel] flying to the chosen station, the fare (free with
// the hard Desert diary) and the right-click Travel option
// https://oldschool.runescape.wiki/w/Transcript:Rug_Merchant
class RugMerchants : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.magic_carpet_seller1") { talk(it.npc, Station.ShantayPass) }
        onOpNpc1("npc.magic_carpet_seller2") { talk(it.npc, Station.Bedabin) }
        onOpNpc1("npc.magic_carpet_seller3") { talk(it.npc, Station.SouthPollnivneach) }
        onOpNpc1("npc.magic_carpet_seller4") { talk(it.npc, northPollnivneachOrUzer(it.npc)) }
        onOpNpc1("npc.magic_carpet_seller8") { talk(it.npc, nardahSophanemOrMenaphos(it.npc)) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc, station: Station) =
        startDialogue(npc) {
            chatPlayer(happy, "Hello.")
            chatNpc(
                happy,
                "Greetings, desert traveller. Do you require the services of Ali Morrisane's " +
                    "flying carpet fleet?",
            )
            mainMenu(station)
        }

    private suspend fun Dialogue.mainMenu(station: Station) {
        while (true) {
            val option =
                choice5(
                    "Yes please.",
                    1,
                    "Tell me about Ali Morrisane.",
                    2,
                    "Tell me about this magic carpet fleet.",
                    3,
                    "I have some questions.",
                    4,
                    "No thanks.",
                    5,
                    title = SELECT,
                )
            when (option) {
                1 -> return travel(station)
                2 -> aliMorrisane()
                3 -> fleet()
                4 -> questions(station)
                else -> return chatNpc(happy, "Come back anytime.")
            }
        }
    }

    private suspend fun Dialogue.travel(station: Station) {
        chatPlayer(happy, "Yes please.")
        when (station) {
            Station.ShantayPass -> {
                chatNpc(
                    happy,
                    "From here you can travel to Uzer, to the Bedabin camp or to the North of " +
                        "Pollnivneach.",
                )
                chatNpc(
                    happy,
                    "The second major carpet hub station, to the south of Pollnivneach is in " +
                        "easy walking distance from there.",
                )
                val option =
                    choice4(
                        "I want to travel to Uzer.",
                        1,
                        "I want to travel to the Bedabin Camp.",
                        2,
                        "I want to travel to Pollnivneach.",
                        3,
                        "I don't want to travel to any of those places.",
                        4,
                        title = SELECT,
                    )
                when (option) {
                    1 -> chatPlayer(happy, "I want to travel to Uzer.")
                    2 -> chatPlayer(happy, "I want to travel to the Bedabin Camp.")
                    3 -> chatPlayer(happy, "I want to travel to Pollnivneach.")
                    else -> {
                        chatPlayer(neutral, "I don't want to travel to any of those places.")
                        enjoyTheWalk()
                    }
                }
            }
            Station.Uzer -> {
                chatNpc(happy, "You can travel from here back to the Shantay Pass.")
                singleDestination("That sounds good, take me there.", "I want to travel to Uzer.")
            }
            Station.Bedabin -> {
                chatNpc(happy, "From here you can travel to the Shantay Pass.")
                singleDestination("Take me there.", "Take me there.")
            }
            Station.NorthPollnivneach -> {
                chatNpc(
                    happy,
                    "From here you can travel to the Shantay Pass - the Southern gate of Al " +
                        "Kharid.",
                )
                singleDestination("Take me to the Pass then.", "Take me to the Pass then.")
            }
            Station.SouthPollnivneach -> {
                chatNpc(
                    happy,
                    "From here you can travel to Nardah and the Menaphite cities of Sophanem and " +
                        "Menaphos.",
                )
                val option =
                    choice4(
                        "I want to travel to Nardah.",
                        1,
                        "I want to travel to Menaphos.",
                        2,
                        "I want to travel to Sophanem.",
                        3,
                        "I don't want to travel to any of those places.",
                        4,
                        title = SELECT,
                    )
                when (option) {
                    1 -> chatPlayer(happy, "I want to travel to Nardah.")
                    2 -> chatPlayer(happy, "I want to travel to Menaphos.")
                    3 -> chatPlayer(happy, "I want to travel to Sophanem.")
                    else -> {
                        chatPlayer(neutral, "I don't want to travel to any of those places.")
                        enjoyTheWalk()
                    }
                }
            }
            Station.Nardah,
            Station.Menaphos -> {
                chatNpc(happy, "The carpets here will take you to the south of Pollnivneach.")
                singleDestination("Let's go then.", "Let's go then.")
            }
            Station.Sophanem -> {
                chatNpc(
                    happy,
                    "The carpets here will take you to the south of Pollnivneach. Do you want to " +
                        "take a lift?",
                )
                singleDestination("Pollnivneach will do.", "Pollnivneach will do.")
            }
        }
    }

    private suspend fun Dialogue.singleDestination(accept: String, acceptLine: String) {
        val option = choice2(accept, 1, "I don't want to travel there.", 2, title = SELECT)
        if (option == 1) {
            chatPlayer(happy, acceptLine)
            return
        }
        chatPlayer(neutral, "I don't want to travel there.")
        enjoyTheWalk()
    }

    private suspend fun Dialogue.enjoyTheWalk() {
        chatNpc(neutral, "Fair enough, magic carpet travel isn't for everyone. Enjoy the walk.")
    }

    // TODO(content): [quest:the_feud] the lines after completing The Feud
    private suspend fun Dialogue.aliMorrisane() {
        chatPlayer(quiz, "Tell me about Ali Morrisane.")
        chatNpc(
            happy,
            "What, you haven't heard of Ali M? Possibly the greatest salesman of the Kharidian " +
                "empire if not all Gielinor?",
        )
        chatPlayer(
            neutral,
            "I can't say that I have, but he must be the ambitious type to try and set up his " +
                "own airline.",
        )
        chatNpc(
            happy,
            "You know something, I reckon that he's trying to take on those gnomes at their own " +
                "game and I'd bet good money that he'll probably win.",
        )
        chatPlayer(confused, "Hah? I think you've gone and lost me now.")
        chatNpc(neutral, "You know those small little guys, not the dwarves now mind.")
        chatPlayer(neutral, "Ya... gnomes, I'm with you that far.")
        chatNpc(neutral, "Well they have already established an Airline, Gnome Air...")
        chatPlayer(neutral, "Go on...")
        chatNpc(
            happy,
            "Anyway I think that Ali M's setup here will prove really successful and maybe once " +
                "we're properly established we could try compete with those gnomes.",
        )
        chatPlayer(happy, "I'll watch this space.")
    }

    private suspend fun Dialogue.fleet() {
        chatPlayer(quiz, "Tell me about this Magic Carpet fleet.")
        chatNpc(
            happy,
            "The latest idea from the great Ali Morrisane. Desert travel will never be the same " +
                "again.",
        )
        chatPlayer(quiz, "So how does it work?")
        chatNpc(quiz, "The carpet or the whole enterprise?")
        val option =
            choice2(
                "Tell me about how the carpet works.",
                1,
                "Tell me about the enterprise then.",
                2,
                title = SELECT,
            )
        if (option == 1) carpet() else enterprise()
    }

    private suspend fun Dialogue.carpet() {
        chatPlayer(quiz, "Tell me about how the carpet works.")
        chatNpc(
            neutral,
            "I'm not really too sure, it's just an enchanted rug really, made out of special " +
                "Ugthanki hair. It flies to whatever destination its owner commands.",
        )
        chatPlayer(quiz, "Are they for sale then?")
        chatNpc(
            angry,
            "Do you think I'm mad? Do you think that Ali Morrisane would throw his magic carpet " +
                "monopoly away?",
        )
        chatPlayer(quiz, "Well perhaps if I offered the right price?")
        chatNpc(
            neutral,
            "Not a hope. Could you imagine the mess there'd be if people were constantly zooming " +
                "through Al Kharid and Pollnivneach? It would be chaos. This way, we can keep " +
                "the carpet traffic outside towns and other busy places.",
        )
        chatPlayer(neutral, "I suppose getting stuck in a carpet jam could get a bit tiresome.")
        chatNpc(
            neutral,
            "Just think of the friction burns you would get if you were in a carpet crash.",
        )
    }

    private suspend fun Dialogue.enterprise() {
        chatPlayer(quiz, "Tell me about the enterprise then.")
        chatNpc(
            neutral,
            "It's quite simple really, Ali Morrisane has hired myself and a few others to set up " +
                "carpet stations at some of the desert's more populated places and run flights " +
                "between the stations.",
        )
        chatPlayer(quiz, "So why has he limited the service to just the desert?")
        chatNpc(
            neutral,
            "I don't think Ali is prepared to take on Gnome Air just yet, their gliders are much " +
                "faster than our carpets, besides that I think we are in the short haul " +
                "business, something that would only work in harsh conditions like",
        )
        chatNpc(neutral, "the desert.")
        chatPlayer(quiz, "Why is that?")
        chatNpc(
            neutral,
            "I suppose because people would just walk. Getting lost isn't too much of a problem " +
                "generally, but it's a different matter when you're in the middle of the " +
                "Kharidian desert with a dry waterskin and no idea",
        )
        chatNpc(neutral, "which direction to go in.")
        chatPlayer(quiz, "You're right I guess. How's the business going then?")
        chatNpc(
            neutral,
            "Not too bad, the hubs are generally quite busy. But the stations in Uzer and the " +
                "Bedabin camp could do with a bit more traffic.",
        )
        chatPlayer(neutral, "A growth market I guess.")
    }

    private suspend fun Dialogue.questions(station: Station) {
        chatPlayer(quiz, "I have some questions.")
        chatNpc(happy, "I'll try to help you as much as I can.")
        val option =
            choice3(
                "What are you doing here?",
                1,
                "Is that your pet monkey nearby?",
                2,
                "Where did you get that hat?",
                3,
                title = SELECT,
            )
        when (option) {
            1 -> doingHere(station)
            2 -> monkey()
            else -> hat()
        }
    }

    private suspend fun Dialogue.doingHere(station: Station) {
        chatPlayer(quiz, "What are you doing here?")
        when (station) {
            Station.ShantayPass ->
                chatNpc(
                    neutral,
                    "Well this is a good position for desert traffic. Shantay seems to have a " +
                        "nice little money spinner setup, but I reckon, this could turn out even " +
                        "better.",
                )
            Station.Uzer -> {
                chatNpc(neutral, "You mightn't realise it, but this is quite a busy station.")
                chatPlayer(quiz, "Who would want to come here?")
                chatNpc(
                    neutral,
                    "Well you for one, and besides that we get quite a few archaeologists from " +
                        "the dig site passing through to examine the golem.",
                )
            }
            Station.Bedabin ->
                chatNpc(
                    neutral,
                    "Well besides the obvious - looking after this station, I'm trying to figure " +
                        "out how these Tentis manage to cultivate such delicious pineapples.",
                )
            Station.NorthPollnivneach -> {
                chatNpc(
                    neutral,
                    "Well Pollnivneach is the ideal location for setting up a carpet station.",
                )
                chatPlayer(quiz, "Why's that?")
                chatNpc(neutral, HALFWAY)
            }
            Station.SouthPollnivneach -> {
                chatNpc(
                    neutral,
                    "I work here renting out magic carpets. I'm from Pollnivneach so it is a " +
                        "handy job, I don't have to commute too far to work every day.",
                )
                chatPlayer(quiz, "Why's that?")
                chatNpc(neutral, HALFWAY)
                chatPlayer(quiz, "So I suppose you're called Ali then.")
                chatNpc(
                    neutral,
                    "Not the most remarkable of names, not that it matters, you see everyone in " +
                        "town knows me as Flash.",
                )
                chatPlayer(quiz, "Really?")
                chatNpc(neutral, "No.")
                chatPlayer(neutral, "........")
                chatNpc(neutral, "........")
                chatPlayer(neutral, "Oh right.")
            }
            Station.Nardah -> {
                chatNpc(
                    neutral,
                    "Well I'd preferred to have been running one of the carpet stations at a hub " +
                        "such as Pollnivneach. I was a bit slow off the mark to get that gig " +
                        "though. Still business in Nardah isn't bad for a terminal. At least",
                )
                chatNpc(neutral, "people come here for the bank and to see the herbalist.")
            }
            Station.Sophanem -> {
                chatNpc(
                    laugh,
                    "I look after the carpet station here. The place is a bit dead though. Ha! " +
                        "I'm just too much.",
                )
                chatPlayer(confused, "What?")
                chatNpc(laugh, "You know, Sophanem, city of the dead and all that?")
                chatPlayer(neutral, "...")
                chatNpc(sad, "Aw come on, the joke wasn't that bad.")
                chatPlayer(neutral, "...")
            }
            Station.Menaphos -> {
                chatNpc(
                    sad,
                    "Until recently I was looking after one of the busiest carpet stations. But " +
                        "that all changed since Menaphos closed its gates.",
                )
                chatNpc(
                    sad,
                    "Right now I have to fill my day trying to come up with reasons why Ali M " +
                        "should keep this place open.",
                )
                chatPlayer(quiz, "So have you come up with any good ideas then?")
                chatNpc(
                    neutral,
                    "Not really. The only reason I have come up with to date is that by keeping " +
                        "the station open, people become familiar with it.",
                )
                chatNpc(
                    neutral,
                    "Maybe once Menaphos opens her gates again, the station will make a fortune " +
                        "once more.",
                )
            }
        }
    }

    private suspend fun Dialogue.monkey() {
        chatPlayer(quiz, "Is that your pet monkey nearby?")
        chatNpc(angry, "He's his own monkey, he does whatever suits him, a total nuisance.")
        chatPlayer(neutral, "I detect a degree of hostility being directed towards the monkey.")
        chatNpc(
            neutral,
            "I shouldn't say this really, but sometimes I begin to question some of Ali " +
                "Morrisane's ideas, he says that associating a monkey with any product will " +
                "increase sales. I just don't know, what will be next?",
        )
        chatPlayer(quiz, "Frogs?")
        chatNpc(neutral, "I doubt it, amphibians don't have the same cutesy factor as monkeys.")
        chatPlayer(confused, "I'm confused. I thought you didn't like monkeys.")
        chatNpc(
            worried,
            "I don't dislike monkeys, it's just that monkey. I don't know, I might just be " +
                "paranoid but I think he's... well... evil.",
        )
        chatPlayer(neutral, "Hmmm... Interesting.")
        if (MONKEYSPEAK_AMULET in player.inv) {
            chatPlayer(
                happy,
                "Hang on a second, I have an amulet of monkeyspeak in my bag, perhaps I could " +
                    "have a word with him.",
            )
        } else if (playerContainsObj(MONKEYSPEAK_AMULET)) {
            chatPlayer(
                happy,
                "Hang on a minute, I think I have an amulet of monkeyspeak stored somewhere. " +
                    "Perhaps I could get it and see what it has to say for itself.",
            )
        } else {
            return
        }
        chatNpc(
            worried,
            "Would you? It sometimes gives me these stares... ~A visible shiver runs down his " +
                "back.",
        )
        chatNpc(happy, "I would be really grateful if you could get that monkey off my back.")
    }

    private suspend fun Dialogue.hat() {
        chatPlayer(quiz, "Where did you get that hat?")
        chatNpc(
            neutral,
            "My fez? I got it from Ali Morrisane, it's a uniform of sorts, apparently it makes " +
                "us more visible, but I'm not too sure about it.",
        )
        chatPlayer(neutral, "Well it is quite distinctive.")
        chatNpc(
            neutral,
            "Do you like it? I haven't really made my mind up about it yet. You see it's not all " +
                "that practical for desert conditions.",
        )
        chatPlayer(quiz, "How so?")
        chatNpc(
            sad,
            "Well it doesn't keep the sun out of my eyes and after a while sitting out in the " +
                "desert they really begin to burn.",
        )
    }

    private fun northPollnivneachOrUzer(npc: Npc): Station =
        if (npc.coords.x >= UZER_MIN_X) Station.Uzer else Station.NorthPollnivneach

    private fun nardahSophanemOrMenaphos(npc: Npc): Station =
        when {
            npc.coords.z >= NARDAH_MIN_Z -> Station.Nardah
            npc.coords.x >= SOPHANEM_MIN_X -> Station.Sophanem
            else -> Station.Menaphos
        }

    private enum class Station {
        ShantayPass,
        Uzer,
        Bedabin,
        NorthPollnivneach,
        SouthPollnivneach,
        Nardah,
        Sophanem,
        Menaphos,
    }

    private companion object {
        const val SELECT = "Select an option"
        const val MONKEYSPEAK_AMULET = "obj.mm_amulet_of_monkey_speak"
        const val UZER_MIN_X = 3400
        const val NARDAH_MIN_Z = 2880
        const val SOPHANEM_MIN_X = 3270
        const val HALFWAY =
            "You see it's located halfway between Al Kharid, and the Menaphite cities and close " +
                "enough to Nardah too, so we get more than enough traffic to keep the business " +
                "running."
    }
}
