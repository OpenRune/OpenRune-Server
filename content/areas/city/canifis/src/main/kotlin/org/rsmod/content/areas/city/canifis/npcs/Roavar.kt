package org.rsmod.content.areas.city.canifis.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Roavar
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:creature_of_fenkenstrain] selling the pickled brain for 50 coins
// TODO(content): [system:treasure_trails] medium clue step reward
class Roavar @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(ROAVAR) { startDialogue(it.npc) { roavar() } }
        onOpNpc3(ROAVAR) { tradeMead() }
    }

    // https://oldschool.runescape.wiki/w/Roavar
    // TODO(content): [verify:in_game] the message when Trade is used without 5 coins
    private fun ProtectedAccess.tradeMead() {
        if (!player.buyMead()) {
            player.mes("You don't have enough coins.")
        }
    }

    private suspend fun Dialogue.roavar() {
        chatPlayer(happy, "Hello there!")
        chatNpc(
            happy,
            "Greetings traveller. Welcome to 'The Hair Of The Dog' Tavern. What can I do you for?",
        )
        val option =
            choice4(
                "Can I buy a beer?",
                1,
                "Can I hear some gossip?",
                2,
                "Can I hear a story?",
                3,
                "Nothing thanks.",
                4,
            )
        when (option) {
            1 -> beer()
            2 -> gossip()
            3 -> story()
            4 -> {
                chatPlayer(neutral, "Nothing thanks.")
                chatNpc(
                    confused,
                    "...I don't know why you talked to me if you don't want anything then...",
                )
            }
        }
    }

    private suspend fun Dialogue.beer() {
        chatPlayer(quiz, "Can I buy a beer?")
        chatNpc(
            happy,
            "Well that's my speciality! The local brew's named 'Moonlight Mead' and will set you " +
                "back 5 gold. Whaddya say? Fancy a pint?",
        )
        when (choice2("Yes please.", 1, "Actually, no thanks.", 2)) {
            1 -> {
                chatPlayer(happy, "Yes please.")
                if (access.inv.count(COINS) < MEAD_PRICE) {
                    chatPlayer(
                        shifty,
                        "I don't have the money on me right now though... can I start a tab?",
                    )
                    chatNpc(neutral, "You see that sign behind me there?")
                    chatPlayer(
                        confused,
                        "The one that says; 'Please Do Not Ask For Credit As Being Attacked By A " +
                            "Large Angry Werewolf Inn Keeper Often Offends'?",
                    )
                    chatNpc(happy, "Bingo.")
                    return
                }
                chatNpc(happy, "Here ya go pal. Enjoy!")
                player.buyMead()
            }
            2 -> {
                chatPlayer(neutral, "Actually, no thanks.")
                chatNpc(
                    neutral,
                    "Eh, suit yourself. You're missing out on a genuine taste experience.",
                )
            }
        }
    }

    private fun Player.buyMead(): Boolean {
        if (!invTakeFee(MEAD_PRICE)) {
            return false
        }
        invAddOrDrop(objRepo, MOONLIGHT_MEAD)
        return true
    }

    private suspend fun Dialogue.gossip() {
        chatPlayer(quiz, "Can I hear some gossip?")
        chatNpc(
            neutral,
            "Well, I dunno... The village is kind of on the fringe out here, I dunno how up to " +
                "date the stuff I hear about is...",
        )
        val option =
            choice4(
                "Tell me about this village.",
                1,
                "Tell me about the land of Morytania.",
                2,
                "Tell me about the shopkeepers here.",
                3,
                "Tell me about the temple to the West.",
                4,
            )
        when (option) {
            1 -> village()
            2 -> morytania()
            3 -> shopkeepers()
            4 -> temple()
        }
    }

    private suspend fun Dialogue.village() {
        chatPlayer(quiz, "Tell me about this village.")
        chatNpc(
            neutral,
            "You want to know about Canifis? I dunno why, not a lot happens here. We're just " +
                "your typical everyday down at earth werewolf folk, after all...",
        )
        chatPlayer(confused, "So... everyone here is a werewolf?")
        chatNpc(happy, "Yep. We are as Zamorak made us!")
        chatPlayer(quiz, "You mentioned Zamorak...")
        chatNpc(
            happy,
            "Yeah, he's great isn't he? Every year we hold a big festival to give thanks to " +
                "Zamorak for keeping us well fed and happy here.",
        )
        chatNpc(
            confused,
            "I hear over in the West they worship Saradomin, but I dunno why! What's he ever " +
                "done to help out us werewolves, huh?",
        )
        chatNpc(
            confused,
            "I hear he has all these crazy followers who say things like we shouldn't kill " +
                "people and eat them! What's up with that?",
        )
        chatPlayer(quiz, "...So when is this festival?")
        chatNpc(
            happy,
            "Aaaah, not for a good few months yet. Come and ask me again nearer to the time, " +
                "I'll keep some extra meat and mead back from the rest for ya pal.",
        )
    }

    private suspend fun Dialogue.morytania() {
        chatPlayer(quiz, "Tell me about the land of Morytania.")
        if (QuestRequirements.hasCompleted(player, BLOOD_MOON_RISES)) {
            return morytaniaAfterBloodMoon()
        }
        chatNpc(
            neutral,
            "Well, I don't know what to tell you really... This village is called Canifis and " +
                "lies on the border between Morytania and Misthalin... so we're kind of on the " +
                "front line if those Saradominists to the west ever decide to",
        )
        chatNpc(neutral, "attack us...")
        chatPlayer(neutral, "That doesn't seem likely...")
        chatNpc(
            neutral,
            "No? The Saradominists hate us. It's only thanks to Lord Drakan that these lands are " +
                "kept safe.",
        )
        chatPlayer(quiz, "Lord Drakan? Who is that?")
        chatNpc(
            neutral,
            "Ahhh... you must be new to these parts if you haven't heard of Lord Drakan! He's " +
                "the lord of this land, and we all pledge allegiance to him.",
        )
        chatNpc(
            neutral,
            "In return for our allegiance, and the tithe of course, he keeps our land safe from " +
                "any invaders and the Saradominists who want to kill us all.",
        )
        chatPlayer(quiz, "Tithe? What do you mean?")
        chatNpc(
            neutral,
            "Ah, well, in return for his protection, we have to give Lord Drakan a share of " +
                "blood every week. If we don't have any to spare from our hunts, then we need to " +
                "pick a member of the village to give their life in return",
        )
        chatNpc(neutral, "for the blood so that the tithe is fulfilled.")
        chatPlayer(shocked, "You mean you kill someone you know in order to meet the tithe???")
        chatNpc(
            neutral,
            "That's right, but only if we haven't managed to get enough spare blood from our " +
                "hunts. It's kind of severe if you look at it that way, but frankly, I think the " +
                "price is fair in return for his protection and his tolerance of",
        )
        chatNpc(
            neutral,
            "our village. He could probably kill us all if he wanted us gone, so keeping on his " +
                "good side is worth the sacrifice to us. Lucky we're not human really!",
        )
        chatPlayer(quiz, "Why's that?")
        if (QuestRequirements.hasCompleted(player, VAMPYRE_SLAYER)) {
            chatNpc(
                neutral,
                "He hates humans! Apparently long ago his brother Draynor was trapped in " +
                    "Misthalin and lost much of his powers, and was recently killed by some human!",
            )
        } else {
            chatNpc(
                neutral,
                "He hates humans! Apparently his brother Draynor is trapped in Misthalin, and " +
                    "adventurous little human wannabe-heroes keep trying to kill him!",
            )
        }
        chatNpc(
            laugh,
            "Man, I'd hate to be a human around here, that's for sure. Drakan would really enjoy " +
                "hunting them down and killing them!",
        )
        chatPlayer(worried, "Uh... thanks for the info...")
    }

    private suspend fun Dialogue.morytaniaAfterBloodMoon() {
        chatNpc(
            neutral,
            "Well, I don't know what to tell you really... This village is called Canifis and " +
                "lies on the border between Morytania and Misthalin... so we're kind of on the " +
                "front line if those Saradominists to the west ever decide to",
        )
        chatNpc(neutral, "attack us...")
        chatPlayer(neutral, "That doesn't seem likely...")
        chatNpc(
            angry,
            "No? The Saradominists hate us. It was only thanks to Lord Drakan that these lands " +
                "were kept safe, but now he's gone and we've got some new overlord who " +
                "apparently wants us to all be friends! Like that's ever",
        )
        chatNpc(angry, "going to happen!")
        chatPlayer(quiz, "So... Are you going to do something about that?")
        chatNpc(neutral, "Me? Of course not! Someone really should though...")
        chatPlayer(neutral, "Okay...")
    }

    private suspend fun Dialogue.shopkeepers() {
        chatPlayer(quiz, "Tell me about the shopkeepers here.")
        chatNpc(quiz, "Hmmm? Why, who did you want to hear the gossip about?")
        val option =
            choice4(
                "Sbott the Tanner.",
                1,
                "Rufus the food seller.",
                2,
                "Barker the clothes seller.",
                3,
                "Fidelio the general store owner.",
                4,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Tell me what you know about Sbott the Tanner.")
                chatNpc(
                    happy,
                    "Hey, I won't hear a word said bad about that guy! He's an honest and " +
                        "hard-working wolf if I ever met one! He charges a lot for his job, but " +
                        "he's one of the best tanners I've ever seen - and I'm over four hundred",
                )
                chatNpc(happy, "years old! You need stuff tanning, I recommend him!")
            }
            2 -> {
                chatPlayer(quiz, "Know anything interesting about Rufus the food seller?")
                chatNpc(
                    happy,
                    "Ah yeah... good old Rufus... He's kind of getting on in years and doesn't " +
                        "like to come out of his wolf form so often, but let me tell you this: " +
                        "that guy is a hunter through and through. You seen how much food he",
                )
                chatNpc(
                    happy,
                    "catches a day? That takes real dedication! Some of the young pups think " +
                        "just because a wolf has a bit of grey in his fur then he's past it, but " +
                        "I've seen him put those pups to shame in a hunt! He's a real " +
                        "inspiration to us",
                )
                chatNpc(happy, "all!")
            }
            3 -> {
                chatPlayer(quiz, "Got anything to share about Barker the clothes seller?")
                chatNpc(
                    neutral,
                    "Eh, I don't like the guy much, but you can't knock the quality of his " +
                        "stock. They're some fine quality threads you can get yourself there.",
                )
                chatPlayer(quiz, "What's wrong with him?")
                chatNpc(
                    confused,
                    "Eh... I can't really tell you to be honest. Something about the guy just " +
                        "gets my hackles up everytime he opens his yap, know what I mean?",
                )
                chatPlayer(neutral, "No, not really.")
                chatNpc(
                    neutral,
                    "Lucky for you. I can't really explain it, I just don't like the guy. Does " +
                        "great clothes though, you got to give him that.",
                )
            }
            4 -> {
                chatPlayer(
                    shifty,
                    "I bet you have some juicy gossip about Fidelio the general store owner.",
                )
                chatNpc(
                    laugh,
                    "That nut job? Oh sure, we all know about him. He was a real firebrand " +
                        "daredevil when he was younger, always taking risks to make himself a " +
                        "quick buck... he got himself caught up in the smuggling trade, sneaking " +
                        "over",
                )
                chatNpc(
                    neutral,
                    "the border into Misthalin and pretending to be a human so he could buy " +
                        "exotic items to sell.",
                )
                chatPlayer(quiz, "Exotic items? From Misthalin? Like what?")
                chatNpc(
                    happy,
                    "Oh man, you name it! Flour... buckets... tinderboxes... chisels... he could " +
                        "get the lot! He's been doing it so long now though, his nerves are shot.",
                )
                chatNpc(
                    sad,
                    "Nowadays he's a disgrace, he can barely string a sentence together and " +
                        "jumps at his own shadow. Makes you feel ashamed to be a werewolf.",
                )
            }
        }
    }

    private suspend fun Dialogue.temple() {
        chatPlayer(quiz, "Tell me about the temple to the west.")
        chatNpc(
            sad,
            "Well, I'm not old enough to remember the full story behind it, but it was a " +
                "terrible day for these lands when it was built there.",
        )
        chatNpc(
            neutral,
            "Apparently, Morytania was once a strong and mighty kingdom, with lands spreading " +
                "far further west than they do today, and south into the desert as well.",
        )
        chatNpc(
            angry,
            "That was all until the day when a sneak attack by the hated human forces who " +
                "worship Saradomin burned our villages and slaughtered our peoples in mass. To " +
                "rub salt into the wound, they then cursed the river as well!",
        )
        chatNpc(
            angry,
            "Luckily they seem to have calmed down somewhat recently, and tend to stay on their " +
                "side of the river... but the wrong they have done my people will never be " +
                "forgotten, and will never be forgiven!",
        )
        chatPlayer(worried, "So you hate humans?")
        chatNpc(
            verymad,
            "Absolutely! If I ever met one I would gobble him up where he stands! And then chew " +
                "the bones for dessert!",
        )
        chatPlayer(worried, "Um, okay, thanks, bye.")
    }

    private suspend fun Dialogue.story() {
        chatPlayer(quiz, "Can I hear a story?")
        chatNpc(
            happy,
            "A story??? Heh, well the only one I can think of right now is one my dear old mammy " +
                "told me as a pup...",
        )
        chatNpc(neutral, "Now how did it go... Ah yes!")
        for (line in STORY) {
            chatNpc(neutral, line)
        }
        chatNpc(quiz, "And do you know what the moral of this story is?")
        chatPlayer(confused, "Um... no, not really.")
        chatNpc(
            happy,
            "It's 'Never trust humans' of course! My dear old mammy told me that story when I " +
                "was a pup, and I'm still alive and well to tell it to you today!",
        )
        chatNpc(happy, "Pretty good story huh?")
        chatPlayer(shifty, "Um... yeah. It was great. Really.")
    }

    private companion object {
        const val ROAVAR = "npc.werewolfinnkeeper"
        const val COINS = "obj.coins"
        const val MOONLIGHT_MEAD = "obj.moonlight_mead"
        const val MEAD_PRICE = 5
        const val BLOOD_MOON_RISES = "quest_bloodmoonrises"
        const val VAMPYRE_SLAYER = "quest_vampyreslayer"

        val STORY =
            listOf(
                "Once upon a time a brave young wolf was walking through a forest, when he came " +
                    "upon a human dressed all in red. 'Aha!' he thought to himself, 'Here's a " +
                    "nice easy meal!' But the human talked to him, and as he was",
                "always taught to be a polite wolf, he spoke back to it.",
                "Well, this cunning human told him that there was a better meal that would not " +
                    "run away in a house in the woods. As the brave young wolf could see that " +
                    "this human was not fully grown, he figured maybe he'd",
                "better get a better meal at this house, so he ran as fast as his paws could " +
                    "carry him to the house the human told him about... inside he found an old " +
                    "human lying in bed, and although the meat was a little tough, as the",
                "human was older than he thought, he had a good meal and decided to sleep it off " +
                    "in the house for awhile. He had not been asleep long though, when he was " +
                    "woken by a knocking at the door. The human in red had followed",
                "him to the house! Suspecting a human trap, the brave young wolf put on the old " +
                    "humans clothes and jumped into the bed, so that he could pretend to be " +
                    "human and escape from this terrible trap! Well now, this human",
                "dressed in red came into the house, and pretended to believe the brave wolf was " +
                    "really a human, and began to talk to him. But then the human started " +
                    "asking strange questions of the wolf, because the human knew that it",
                "was not a human at all! The brave young wolf decided to try and escape, for it " +
                    "was only a young human, and not very strong, so the brave young wolf said " +
                    "he would eat the human if they did not let him escape! As he said",
                "this however, the human in red shouted out at him: 'Aha! You are a wolf!' and " +
                    "as the human shouted this another bigger human ran into the room from " +
                    "outside! This bigger human was much stronger and was",
                "carrying an axe, and the poor young wolf died in this terrible trap made for " +
                    "him by the humans.",
            )
    }
}
