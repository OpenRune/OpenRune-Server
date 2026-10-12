package org.rsmod.content.areas.city.desert.nardah.npcs

import org.rsmod.api.config.Constants
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class NardahTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.elid_mayor") { startDialogue(it.npc) { awusah() } }
        onOpNpc1("npc.elid_ghaslor") { startDialogue(it.npc) { ghaslor() } }
        onOpNpc1("npc.elid_shiratti") { startDialogue(it.npc) { shiratti() } }
        onOpNpc1("npc.elid_villager1") { startDialogue(it.npc) { usi() } }
        onOpNpc1("npc.elid_villager2") { startDialogue(it.npc) { nkuku() } }
        onOpNpc1("npc.elid_villager3") { startDialogue(it.npc) { garai() } }
        onOpNpc1("npc.elid_villager4") { startDialogue(it.npc) { notHappy(greet = true) } }
        onOpNpc1("npc.elid_villager5") { startDialogue(it.npc) { notHappy(greet = true) } }
        onOpNpc1("npc.elid_villager6") { startDialogue(it.npc) { notHappy(greet = false) } }
        onOpNpc1("npc.elid_herbalist") { startDialogue(it.npc) { zahur() } }
        onOpNpc1("npc.pestlemortar_crusher") { startDialogue(it.npc) { wesley() } }
        onOpNpc1("npc.bim_willow_postquest") { startDialogue(it.npc) { willow() } }
        onOpNpc1("npc.bim_marley_postquest") { startDialogue(it.npc) { marley() } }
        onOpNpc1("npc.bim_burntof_postquest") { startDialogue(it.npc) { burntof() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Spirits_of_the_Elid
    // TODO(content): [quest:spirits_of_the_elid] quest start, in-progress dialogue and
    //  https://oldschool.runescape.wiki/w/Transcript:Awusah_the_Mayor after the quest
    private suspend fun Dialogue.awusah() {
        chatNpc(happy, "Many salutations to you. What brings you to our little town?")
        val option =
            choice3(
                "I am an adventurer in search of quests.",
                1,
                "I'm just looking around.",
                2,
                "I'm passing through on my way to Menaphos.",
                3,
            )
        when (option) {
            1 -> awusahCurse()
            2 -> {
                chatPlayer(neutral, "I'm just looking around.")
                chatNpc(
                    neutral,
                    "Well we've not fallen on the best of times here, but there are a few shops " +
                        "to look around and we have someone selling a marvelous new invention " +
                        "called a choc ice..",
                )
                chatNpc(neutral, "Oh, and there's the old temple if you're interested in a bit of history.")
            }
            3 -> {
                chatPlayer(neutral, "I'm passing through on my way to Menaphos.")
                chatNpc(worried, "Oh you don't want to go there. I hear rumours of plagues and the like.")
                chatPlayer(neutral, "Thanks for the warning, but I might go and investigate anyway.")
            }
        }
    }

    private suspend fun Dialogue.awusahCurse() {
        chatPlayer(happy, "I am an adventurer in search of quests.")
        chatNpc(
            neutral,
            "Hmm, well that could be quite fortuitous. We really could do with some help here. Our " +
                "town seems to have come under a curse, and we are desperate to get it removed.",
        )
        chatPlayer(quiz, "A curse? What sort of curse?")
        chatNpc(
            sad,
            "Well first of all our fountain, which has served us for many years, suddenly dried " +
                "up. We then started transporting water from the river, but what we carried would " +
                "always have completely dried up by the time we",
        )
        chatNpc(sad, "got it back to the village.")
        while (true) {
            val option =
                choice3(
                    "So what do you do for water now?",
                    1,
                    "Any idea how you got this curse?",
                    2,
                    "I'm sorry to hear of your plight, but I must be going.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "So what do you do for water now?")
                    chatNpc(
                        neutral,
                        "Well it's tough. There is a guy who calls himself Ali the carter down in " +
                            "the market. He's from Pollnivneach, so the curse doesn't seem to have " +
                            "affected him. He's got a cart full of water which he's selling to the " +
                            "people here.",
                    )
                    chatNpc(
                        angry,
                        "Unfortunately in typical Pollnivneach style he's taking the opportunity " +
                            "to sell the water at outrageous prices because he knows we are " +
                            "desperate. Oh and Rokuh has developed something quite remarkable, " +
                            "called a choc ice",
                    )
                    chatNpc(neutral, "which is helping people stay cool.")
                }
                2 -> {
                    chatPlayer(quiz, "Any idea how you got this curse?")
                    chatNpc(
                        sad,
                        "Well we think it was because we were negligent in our duty to protect a " +
                            "priest of Saradomin. He came to us a few weeks ago from the north and " +
                            "we welcomed him into our town. He taught us much of the wisdom of",
                    )
                    chatNpc(
                        sad,
                        "Saradomin. Then about a week ago, we found him dead by the river. It " +
                            "seems unlikely that it is coincidence we were then cursed. If you look " +
                            "around the town you might be able to find out more.",
                    )
                    return
                }
                3 -> {
                    chatPlayer(sad, "I'm sorry to hear of your plight, but I must be going.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Spirits_of_the_Elid
    // TODO(content): [quest:spirits_of_the_elid] curse question and the ballad of Jareesh during
    //  the quest, and https://oldschool.runescape.wiki/w/Transcript:Ghaslor_the_Elder after it
    // TODO(content): [verify:in_game] whether "How are things?" is still a one-option menu before
    //  the quest starts
    private suspend fun Dialogue.ghaslor() {
        val title = if (player.appearance.bodyType == Constants.bodytype_a) "man" else "lady"
        chatNpc(neutral, "Good day to you young $title.")
        chatPlayer(quiz, "How are things?")
        chatNpc(
            angry,
            "Lousy, young people have no respect for their elders, there's too many confusing " +
                "newfangled magic carpets and snake charmers around these days and now we don't " +
                "even have any water.",
        )
        chatPlayer(neutral, "Well I hope things pick up for you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Shiratti_the_Custodian
    // TODO(content): [quest:spirits_of_the_elid] quest and after-quest dialogue
    private suspend fun Dialogue.shiratti() {
        if (choice2("Good day to you.", true, "What is this building?", false)) {
            chatPlayer(happy, "Good day to you.")
            chatNpc(
                angry,
                "Be Careful in here and don't touch anything! There's some old and valuable " +
                    "artefacts around here. Careless outsiders.. you're always coming in and " +
                    "making a mess of things!",
            )
            chatPlayer(worried, "Ok calm down, I'm not going to touch anything.")
            chatNpc(neutral, "Yes, good don't.")
            return
        }
        chatPlayer(quiz, "What is this building?")
        chatNpc(neutral, "This is our local shrine to She.")
        chatPlayer(confused, "What do you mean by She?")
        chatNpc(
            angry,
            "Such ignorance! I suppose I should come to expect it. People are forgetting the old " +
                "ways of the desert around here. But then.. What do those bureaucratic fools in " +
                "Menaphos expect! If they're going to take so long to",
        )
        chatNpc(
            angry,
            "send us a new priestess of She. Do you know we haven't had one for five years now!",
        )
        chatPlayer(confused, "Errm you still haven't told me what you mean by She.")
        chatNpc(
            neutral,
            "She! Otherwise know as Elidinis, Goddess of growth and fertility, Lady of the river, " +
                "Wife of Tumeken, Flower of the desert, Mother of Icthlarin.",
        )
        chatPlayer(
            confused,
            "That all sounds very confusing, I might just call her Elidinis for short.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Usi
    // TODO(content): [quest:spirits_of_the_elid] quest and after-quest dialogue
    private suspend fun Dialogue.usi() {
        chatPlayer(happy, "Good day to you.")
        chatNpc(sad, "It's not that good, it's a bit warm.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Nkuku
    // TODO(content): [quest:spirits_of_the_elid] quest and after-quest dialogue
    private suspend fun Dialogue.nkuku() {
        chatPlayer(happy, "Good day to you.")
        chatNpc(happy, "May Saradomin be with you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Garai
    // TODO(content): [quest:spirits_of_the_elid] quest and after-quest dialogue
    private suspend fun Dialogue.garai() {
        chatPlayer(happy, "Good day to you.")
        chatNpc(happy, "And a good day to you too.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Habibah
    // https://oldschool.runescape.wiki/w/Transcript:Meskhenet
    // https://oldschool.runescape.wiki/w/Transcript:Zahra
    // TODO(content): [quest:spirits_of_the_elid] quest and after-quest dialogue
    private suspend fun Dialogue.notHappy(greet: Boolean) {
        if (greet) {
            chatPlayer(happy, "Good day to you.")
        }
        chatNpc(sad, "Hello.")
        chatPlayer(quiz, "You don't look too happy.")
        chatNpc(sad, "True. We've not fallen on the best of times here.")
        chatPlayer(quiz, "Any way that I can help?")
        chatNpc(
            neutral,
            "Possibly. I'd go talk to Awusah the Mayor of Nardah. He's in the big house on the " +
                "east side of the town square.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Zahur
    // TODO(content): [system:met_flags] "Hello again. Do you need anything?" on later visits
    // TODO(content): [system:herb_cleaning] "Please clean all my herbs." and the Clean option
    // TODO(content): [system:potion_decanting] "Please decant my potions." and the Decant option
    private suspend fun Dialogue.zahur() {
        chatNpc(
            happy,
            "Hi there. Do you need anything? I can clean herbs for a fee of 200 coins per herb. I " +
                "can clean all of your herbs at once, or I can do individual herbs if you show " +
                "them to me.",
        )
        chatNpc(
            happy,
            "I can also decant your potion vials to into different doses. That service is free so " +
                "long as you have the empty vials needed. If not, it's just a few coins.",
        )
        while (true) {
            val option =
                choice5(
                    "Please clean all my herbs.",
                    1,
                    "Please decant my potions.",
                    2,
                    "Why would I pay for you to clean herbs?",
                    3,
                    "Who's that kid outside your shop?",
                    4,
                    "Maybe another time.",
                    5,
                )
            when (option) {
                1,
                2 -> return
                3 -> {
                    chatPlayer(quiz, "Why would I pay for you to clean herbs?")
                    chatNpc(
                        neutral,
                        "Many of my customers wish to sell the herbs which they can't clean to " +
                            "other adventurers. They might hope for a better price by doing this.",
                    )
                }
                4 -> {
                    chatPlayer(quiz, "Who's that kid outside your shop?")
                    chatNpc(
                        neutral,
                        "I do not know where he came from. He came to Nardah recently, and said " +
                            "his business might help my customers, so I let him have some space " +
                            "out there.",
                    )
                    chatNpc(
                        neutral,
                        "He seems very lost in the world, but I hope his work will be a success " +
                            "for him.",
                    )
                    chatNpc(
                        neutral,
                        "Now, would you like me to clean your herbs or combine your potions?",
                    )
                }
                5 -> {
                    chatPlayer(neutral, "Maybe another time.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Wesley
    // TODO(content): [system:item_crushing] items used on Wesley, 50 coins per item
    // TODO(content): [system:npc_overhead] random "Grind it!" shouts
    private suspend fun Dialogue.wesley() {
        chatNpc(happy, "Got anything you want me to crush?")
        while (true) {
            val option =
                choice4(
                    "Okay, I'd like you to crush something.",
                    1,
                    "What sort of things do you crush?",
                    2,
                    "Who are you?",
                    3,
                    "I'm not interested, thanks.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(happy, "Okay, I'd like you to crush something.")
                    chatNpc(happy, "Hand it to me, and I'll get to work. I charge 50 coins per item.")
                    return
                }
                2 -> {
                    chatPlayer(quiz, "What sort of things do you crush?")
                    chatNpc(
                        happy,
                        "All kinds of things! Unicorn horns, goat horns, blue dragon scales and " +
                            "much more besides. Hand me your stuff, and I'll try to crush it. I " +
                            "charge 50 coins per item.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(
                        neutral,
                        "I'm Wesley. I used to work on a big ship, exploring the world with my " +
                            "mother, but then I didn't get on very well at the fleet's training " +
                            "school, so I quit all that.",
                    )
                    chatNpc(
                        happy,
                        "So I moved to Nardah and took up this job crushing stuff instead. It's " +
                            "always been a passion of mine, crushing things. I'm a lot happier now.",
                    )
                    chatNpc(
                        happy,
                        "The world is so much simpler when you're crushing. You know you just " +
                            "have to grind for long enough until you get the job done. It might " +
                            "take hours, but you just grind until you're done.",
                    )
                }
                4 -> {
                    chatPlayer(neutral, "I'm not interested, thanks.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Willow_(Below_Ice_Mountain)
    private suspend fun Dialogue.willow() {
        chatPlayer(angry, "So this is where you scampered off to after the monster awoke?")
        chatNpc(worried, "Look ${player.displayName}, it was a simple misunderstanding, that's all...")
        chatPlayer(angry, "Oh really? You threatening to kill me was just a misunderstanding?")
        chatNpc(
            worried,
            "Things got a little heated down there, but we all made it out alive, right?",
        )
        chatPlayer(angry, "Barely... I had to defeat that monster on my own!")
        chatPlayer(quiz, "What are you doing out here, anyway?")
        chatNpc(
            neutral,
            "We thought you'd rat us out to the authorities, so we came out here to hide.",
        )
        chatNpc(
            shifty,
            "Plus, we heard there are some sealed tombs in the desert that haven't been touched in " +
                "hundreds of years...",
        )
        chatPlayer(neutral, "Ah, of course... scoping out your next grave to rob.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Marley
    private suspend fun Dialogue.marley() {
        chatPlayer(laugh, "Wow, the desert really suits you Marley.")
        chatNpc(happy, "${player.displayName} my old pal! How are you doing?")
        chatPlayer(
            neutral,
            "Better now I don't have to deal with you. Enjoying life in the desert?",
        )
        chatNpc(
            sad,
            "Not really. I'm not suited to such harsh climates. No steak sandwiches here either...",
        )
        chatPlayer(
            laugh,
            "Oh what a shame! Poor Marley's going to starve as he doesn't have anyone to make his " +
                "sandwiches... So sad.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Burntof
    private suspend fun Dialogue.burntof() {
        val lad = if (player.appearance.bodyType == Constants.bodytype_a) "lad" else "lass"
        chatPlayer(
            quiz,
            "How you holding up Burntof? Must have had quite the fright when you woke that " +
                "monster.",
        )
        chatNpc(neutral, "You're right $lad. It did give me a little fright.")
        chatNpc(neutral, "But I saw some interesting things down in the ruins...")
        chatPlayer(quiz, "What do you mean?")
        chatNpc(
            neutral,
            "Some of the markings on the wall look very familiar to me. I've seen them before.",
        )
        chatPlayer(
            neutral,
            "Well, when I defeated the monster, an Imcando dwarf opened the barred door...",
        )
        chatNpc(
            shocked,
            "An Imcando dwarf! I thought they were all but extinct! You've found something truly " +
                "amazing ${player.displayName}!",
        )
        chatNpc(sad, "I wish I had a drink to celebrate, but this desert is bone dry.")
    }
}
