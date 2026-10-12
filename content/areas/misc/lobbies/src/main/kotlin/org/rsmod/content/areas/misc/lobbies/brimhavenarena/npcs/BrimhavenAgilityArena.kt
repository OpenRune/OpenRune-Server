package org.rsmod.content.areas.misc.lobbies.brimhavenarena.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class BrimhavenAgilityArena : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.agilityarena_clerk") { startDialogue(it.npc) { izzy() } }
        onOpNpc1("npc.agilityarena_tickettrader") { startDialogue(it.npc) { jackie() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Cap'n_Izzy_No-Beard (without 99 Agility)
    // TODO(content): [system:brimhaven_agility_arena] taking the 200 coin fee and letting the
    //  player in ("Okay, here's 200 coins." and the Pay option)
    // TODO(content): [system:skillcapes] "Is it true you sell Skillcapes of Agility?" with 99
    //  Agility
    // TODO(content): [system:treasure_trails] hard clue step with the parrot
    // TODO(content): [verify:in_game] the parrot's chathead (no separate parrot npc exists)
    private suspend fun Dialogue.izzy() {
        chatPlayer(happy, "Ahoy Cap'n!")
        chatNpc(happy, "Ahoy there!")
        parrot("Avast ye scurvy swabs!")
        chatPlayer(confused, "Huh?")
        chatNpc(laugh, "Don't mind me parrot, he's Cracked Jenny's Tea Cup!")
        while (true) {
            val option =
                choice5(
                    "What is this place?",
                    1,
                    "What do I do in the arena?",
                    2,
                    "I'd like to use the Agility Arena, please.",
                    3,
                    "Can you tell me a bit about the Skillcape of Agility, please?",
                    4,
                    "See you later.",
                    5,
                )
            when (option) {
                1 -> izzyWhatIsThisPlace()
                2 -> izzyWhatDoIDo()
                3 -> {
                    useTheArena()
                    return
                }
                4 -> {
                    chatPlayer(
                        quiz,
                        "Can you tell me a bit about the Skillcape of Agility, " +
                            "please?",
                    )
                    chatNpc(
                        happy,
                        "Aye, to be sure! The Skillcape of Agility be the symbol of the master " +
                            "of dexterity! Providing weight reduction, one who wears it can " +
                            "climb like a cat, run like the wind and jump like...err, well, jump " +
                            "like a jumping thing!",
                    )
                    chatNpc(happy, "Now, be there anything else ye'd like to know?")
                }
                5 -> {
                    chatPlayer(neutral, "See you later.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.izzyWhatIsThisPlace() {
        chatPlayer(quiz, "What is this place?")
        chatNpc(happy, "This, me hearty, is the entrance to the Brimhaven Agility Arena!")
        chatNpc(
            happy,
            "I were diggin for buried treasure when I found it! Amazed I was! It was a sight to " +
                "behold!",
        )
        parrot("Buried treasure!")
        chatNpc(
            happy,
            "It were the biggest thing I'd ever seen! It must've been at least a league from " +
                "side to side!",
        )
        chatNpc(happy, "It made me list, I were that excited!")
        parrot("Get on with it!")
        chatNpc(
            neutral,
            "I'd found a huge cave with all these platforms. I reckon it be an ancient " +
                "civilisation that made it. I had to be mighty careful as there was these traps " +
                "everywhere! Dangerous it was!",
        )
        parrot("Danger! Danger!")
        chatNpc(happy, "Entrance is only 200 coins!")
    }

    private suspend fun Dialogue.izzyWhatDoIDo() {
        chatPlayer(quiz, "What do I do in the arena?")
        chatNpc(
            neutral,
            "Well, me hearty, it's simple. Ye can cross between two platforms by using the traps " +
                "or obstacles strung across 'em. Try and make your way to the pillar that is " +
                "indicated by the flashing arrow.",
        )
        chatNpc(
            neutral,
            "Ye receive tickets for tagging more than one pillar in a row. So ye won't get a " +
                "ticket from the first pillar but ye will for every platform ye tag in a row " +
                "after that.",
        )
        chatNpc(
            neutral,
            "If ye miss a platform ye will miss out on the next ticket so try and get every " +
                "platform you can! When ye be done, take the tickets to Jackie over there and " +
                "she'll exchange them for more stuff!",
        )
        parrot("Tag when green light means tickets! Tag when red light means green light!")
        chatPlayer(happy, "Thanks!")
    }

    private suspend fun Dialogue.useTheArena() {
        chatPlayer(happy, "I'd like to use the Agility Arena, please.")
        chatNpc(happy, "Aye, entrance be 200 coins.")
        parrot("Pieces of eight!")
        chatNpc(
            worried,
            "A word of warning me hearty! There are dangerous traps down there!",
        )
        if (access.invTotal(player.inv, COINS) < ENTRANCE_FEE) {
            chatPlayer(sad, "I don't have the money on me at the moment.")
            parrot("*squawk*")
            chatNpc(angry, "No coins, no entrance!")
        }
    }

    private suspend fun Dialogue.parrot(text: String) {
        chatNpcSpecific(PARROT_TITLE, PARROT, neutral, text)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Pirate_Jackie_the_Fruit
    // TODO(content): [system:brimhaven_agility_arena] the ticket exchange (Trade op and "I'd
    //  like to trade in my tickets, please."), old ticket conversion and graceful recolours
    // TODO(content): [diary:karamja] tier reward claims, "Can I have another set of Karamja
    //  gloves?" and the metal dragon bar drops (Bars op)
    // TODO(content): [verify:in_game] the order of the diary menu and its MORE page
    private suspend fun Dialogue.jackie() {
        chatNpc(happy, "Ahoy!")
        while (true) {
            val option =
                choice4(
                    "What is this place?",
                    1,
                    "What do you do?",
                    2,
                    "I'd like to trade in my tickets, please.",
                    3,
                    "I have a question about my Achievement Diary.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What is this place?")
                    chatNpc(happy, "Welcome to the Brimhaven Agility Arena!")
                    chatNpc(neutral, "If ye want to know more talk to Cap'n Izzy, he found it!")
                }
                2 -> {
                    chatPlayer(quiz, "What do you do?")
                    chatNpc(
                        happy,
                        "I be the Jack o' Tickets. I exchange the tickets ye collect in the " +
                            "Agility Arena for more stuff. Ye can obtain more agility experience " +
                            "or some items ye won't find anywhere else!",
                    )
                    chatPlayer(happy, "Sounds good!")
                }
                3 -> {
                    chatPlayer(happy, "I'd like to trade in my tickets, please.")
                    chatNpc(happy, "Aye, ye be on the right tack.")
                    return
                }
                4 -> {
                    chatPlayer(quiz, "I have a question about my Achievement Diary.")
                    diaryMenu()
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.diaryMenu() {
        while (true) {
            val option =
                choice4(
                    "What is the Achievement Diary?",
                    1,
                    "What are the rewards?",
                    2,
                    "See you later.",
                    3,
                    "MORE",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What is the Achievement Diary?")
                    chatNpc(
                        neutral,
                        "Arrr, it be a diary that tracks yer achievements on Karamja. Once ye've " +
                            "explored enough, I give ye some enchanted gloves, an' the people of " +
                            "Karamja reward ye for yer hard work.",
                    )
                }
                2 -> rewards()
                3 -> {
                    chatPlayer(neutral, "See you later.")
                    return
                }
                4 -> if (!moreMenu()) return
            }
        }
    }

    private suspend fun Dialogue.moreMenu(): Boolean {
        while (true) {
            val option =
                choice3(
                    "What potions do these new herbs make?",
                    1,
                    "See you later.",
                    2,
                    "BACK",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What potions do these new herbs make?")
                    chatNpc(
                        neutral,
                        "I be no herbalist but I were given the recipes to go with these " +
                            "herbs... Toadflax + toad legs = Agility potion. Snapdragon + red " +
                            "spiders' eggs = Super restore potion.",
                    )
                    chatPlayer(happy, "Thanks!")
                    return true
                }
                2 -> {
                    chatPlayer(neutral, "See you later.")
                    return false
                }
                3 -> return true
            }
        }
    }

    private suspend fun Dialogue.rewards() {
        chatPlayer(quiz, "What are the rewards?")
        chatNpc(neutral, "Arrr, that depends what tier ye wants to know about.")
        while (true) {
            val option =
                choice5(
                    "Easy rewards",
                    1,
                    "Medium rewards",
                    2,
                    "Hard rewards",
                    3,
                    "Elite rewards",
                    4,
                    "Cancel",
                    5,
                )
            when (option) {
                1 ->
                    chatNpc(
                        neutral,
                        "Ye gets a pair of special Karamja gloves. The seafarin' folk who sail " +
                            "to Port Sarim and Ardougne will take ye on board half price if ye " +
                            "be wearin' them.",
                    )
                2 ->
                    chatNpc(
                        neutral,
                        "Arrr, I upgrade the Karamja gloves so ye gets a little extra benefit in " +
                            "our Agility Arena with 'em on, and now they'll take ye to the " +
                            "special mine under Shilo Village, an ye still gets the easy " +
                            "rewards too.",
                    )
                3 ->
                    chatNpc(
                        neutral,
                        "Arrr, I upgrade the Karamja gloves so they can take ye to a special " +
                            "mine under Shilo Village infinitely. Ye still gets the easy and " +
                            "medium rewards too.",
                    )
                4 -> {
                    chatNpc(
                        neutral,
                        "Arrr, ye can get double TokKul from the Fight Cave, Inferno an' " +
                            "TzHaar-Ket-Rak's Challenges, an' have a chance for double tickets " +
                            "in our Agility Arena 'ere.",
                    )
                    chatNpc(
                        neutral,
                        "Ye can resurrect in the Fight Cave once per day, an' even enter some " +
                            "places fer free.",
                    )
                    chatNpc(
                        neutral,
                        "I can make the metal dragons under Brimhaven give ye bars as banknotes " +
                            "too, if ye likes. Ye still gets the easy, medium an' hard rewards " +
                            "too.",
                    )
                }
                5 -> return
            }
        }
    }

    private companion object {
        const val COINS = "obj.coins"
        const val ENTRANCE_FEE = 200
        const val PARROT = "npc.lunar_captains_parrot"
        const val PARROT_TITLE = "Parrot"
    }
}
