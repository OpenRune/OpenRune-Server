package org.rsmod.content.areas.city.ardougne.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [skill:construction] house ownership, hiring, dismissing and paying servants
// TODO(content): [verify:in_game] servant "bows" animation and whether the level check is boostable
class ServantsGuild : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.poh_chief_servant") { startDialogue(it.npc) { chiefServant() } }
        onOpNpc1("npc.poh_servant_multi_dogsbody") { startDialogue(it.npc) { servant(RICK) } }
        onOpNpc1("npc.poh_servant_multi_waiter_woman") { startDialogue(it.npc) { servant(maid()) } }
        onOpNpc1("npc.poh_servant_multi_cook_woman") { startDialogue(it.npc) { servant(COOK) } }
        onOpNpc1("npc.poh_servant_multi_maitre_d_man") {
            startDialogue(it.npc) { servant(butler()) }
        }
        onOpNpc1("npc.poh_servant_multi_demon") { startDialogue(it.npc) { servant(DEMON_BUTLER) } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Chief_servant
    private suspend fun Dialogue.chiefServant() {
        chatNpc(happy, "Welcome to the Ardougne Domestic Service Agency!")
        val option =
            choice3(
                "Tell me about servants.",
                1,
                "Can I hire a servant?",
                2,
                "Thank you.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Tell me about servants.")
                chatNpc(
                    happy,
                    "Do you need help around your house? Our trained, efficient workers are " +
                        "ready to serve you.",
                )
                chatNpc(
                    happy,
                    "They can cook meals for you and your friends, make tea, serve drinks, greet " +
                        "guests, and even take items to and from the bank for you.",
                )
                chatNpc(
                    happy,
                    "The more expensive servants will be able to make trips to the bank more " +
                        "quickly. Some of them can also go to the sawmill to change logs to " +
                        "planks for you.",
                )
                chatNpc(
                    neutral,
                    "Remember, you'll need to keep paying your servant. You pay them once when " +
                        "you hire them, and then they'll periodically ask for wages. You can " +
                        "dismiss your servant at any time.",
                )
            }
            2 -> {
                chatPlayer(quiz, "Can I hire a servant?")
                chatNpc(
                    neutral,
                    "Do you even own a house? These are strictly domestic servants; they won't " +
                        "follow you around the world!",
                )
            }
            3 -> chatPlayer(happy, "Thank you.")
        }
    }

    private suspend fun Dialogue.servant(servant: Servant) {
        chatNpc(happy, servant.greeting)
        val option =
            choice3(
                "What can you do?",
                1,
                "Tell me about your previous jobs",
                2,
                "You're hired!",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What can you do?")
                for (line in servant.skills) {
                    chatNpc(happy, line)
                }
            }
            2 -> {
                chatPlayer(quiz, "Tell me about your previous jobs.")
                for (line in servant.previousJobs) {
                    chatNpc(neutral, line)
                }
            }
            3 -> {
                if (player.statBase("stat.construction") < servant.level) {
                    chatNpc(neutral, servant.refusal(player.statBase("stat.construction")))
                    return
                }
                mesbox("You need a second bedroom with a bed for your servant to sleep in.")
            }
        }
    }

    private fun Dialogue.title(): String = if (access.isBodyTypeA()) "sir" else "madam"

    // https://oldschool.runescape.wiki/w/Transcript:Maid
    private fun Dialogue.maid(): Servant {
        val title = title()
        return Servant(
            level = 25,
            greeting =
                "Oh! Please hire me, $title! I'm very good, well, I'm not bad, and my fee's only " +
                    "1000 coins.",
            skills =
                listOf(
                    "Well, I can, um. I can cook meals and make tea and everything, and I can " +
                        "even take things to and from the bank for you. I won't make any " +
                        "mistakes this time and everything will be fine!"
                ),
            previousJobs =
                listOf(
                    "Oh! Oh! I, well, I, er. It wasn't really my fault, I mean, it was, but not " +
                        "really. I mean, how was I to know that that plate was so valuable? It " +
                        "was just lying around and I don't know art, it just looked like a " +
                        "pretty pattern and",
                    "I just had to use it because there weren't enough plates. And no one had " +
                        "told me that Dennis was off sick and hadn't fed the dogs so I wasn't " +
                        "expecting them to jump up at me when I was carrying the food and it, it",
                    "went all over the place, gravy and potatoes and bits of fine porcelain and " +
                        "I'm so sorry. It won't happen again, I promise.",
                ),
            refusal = {
                "Oh! Oh dear! I'm terribly sorry, $title, but I'm not allowed to work for " +
                    "anyone unless they have level 25 Construction. If a house is poorly made I " +
                    "might damage it by mistake!"
            },
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Butler
    private fun Dialogue.butler(): Servant {
        val title = title()
        return Servant(
            level = 40,
            greeting = "Good day, $title. Would $title care to hire a good butler for 5000 coins?",
            skills =
                listOf(
                    "I can fulfill all $title's domestic service needs with efficiency and " +
                        "impeccable manners. I hate to boast, but I can say with confidence that " +
                        "no mortal can make trips to the bank or sawmill faster than I!"
                ),
            previousJobs =
                listOf(
                    "From a humble beginning as a dish-washer I have worked my way up through " +
                        "the ranks of domestic service in the households of nobles from Varrock " +
                        "and Ardougne. As a life-long servant I have naturally",
                    "suppressed any personality of my own and trained myself never to use the " +
                        "second person when talking to a superior. I have usually worked in the " +
                        "large households of the aristocracy, but now that such a large",
                    "number of private persons are building their own houses I decided to offer " +
                        "them my services.",
                ),
            refusal = {
                "I must respectfully decline, $title. I offer a very exclusive service and will " +
                    "only work for people with over level 40 Construction."
            },
        )
    }

    private class Servant(
        val level: Int,
        val greeting: String,
        val skills: List<String>,
        val previousJobs: List<String>,
        val refusal: (Int) -> String,
    )

    private companion object {
        // https://oldschool.runescape.wiki/w/Transcript:Rick
        val RICK =
            Servant(
                level = 20,
                greeting = "'Allo mate! Got a job going? Only 500 coins!",
                skills =
                    listOf(
                        "I'm a great cook, me! I used to work with a rat-catcher, I used to cook " +
                            "for him. There's a dozen different ways you can cook rat!"
                    ),
                previousJobs =
                    listOf(
                        "Well, city warder Bravek once threw a chair at me and yelled at me to " +
                            "get him a hangover cure. So I made it and I think it worked, 'cause " +
                            "then he threw another chair at me and that one hit!"
                    ),
                refusal = {
                    "Sorry mate, but I'm not allowed to work for anyone without level 20 " +
                        "Construction. It's a safety 'azard!"
                },
            )

        // https://oldschool.runescape.wiki/w/Transcript:Cook_(servant)
        val COOK =
            Servant(
                level = 30,
                greeting =
                    "You're not aristocracy but I suppose you'd do. Do you want a good cook for " +
                        "3000 coins?",
                skills =
                    listOf(
                        "I am the finest cook in all Gielinor! I can also make good time going " +
                            "to the bank or the sawmill."
                    ),
                previousJobs =
                    listOf(
                        "I used to be the cook for the old Duke of Lumbridge. Visiting " +
                            "dignitaries praised me for my fine banquets!",
                        "But then someone found a rule that said that only one family could hold " +
                            "that post.",
                        "Overnight I was fired and replaced by some fool who can't even bake a " +
                            "cake without help!",
                    ),
                refusal = {
                    "Hmph! I don't work for just anyone, you know! I refuse to work for someone " +
                        "who's below level 30 Construction!"
                },
            )

        // https://oldschool.runescape.wiki/w/Transcript:Demon_butler
        val DEMON_BUTLER =
            Servant(
                level = 50,
                greeting =
                    "Greetings! I am Alathazdrar, butler to the Demon Lords, and I offer thee my " +
                        "services for a mere 10000 coins!",
                skills =
                    listOf(
                        "I have learned my trade under the leash of some of the harshest masters " +
                            "of the Demon Dimensions. I can cook to satisfy the most infernal " +
                            "stomachs, and fly on wings of flame to deposit thine items in the " +
                            "bank or bring",
                        "planks from the sawmill in seconds.",
                    ),
                previousJobs =
                    listOf(
                        "For millennia I have served and waited on the mighty Demon Lords of the " +
                            "Infernal Dimensions. I began as a humble footman in the household " +
                            "of Lord Thammaron, and for several centuries I was the private " +
                            "valet to",
                        "Delrith. I have also worked in the Grim Underworld, escorting the souls " +
                            "of the dead to their final abodes. But the incessant shadows and " +
                            "hellfire weary me, so I have come to serve mortal masters in the " +
                            "realms of light.",
                    ),
                refusal = { level ->
                    "You, a mere mortal with level $level Construction, wish to employ ME, " +
                        "butler to Great Ones of the Outer Darkness? Ha ha ha! I refuse to work " +
                        "for anyone below level 50 Construction."
                },
            )
    }
}
