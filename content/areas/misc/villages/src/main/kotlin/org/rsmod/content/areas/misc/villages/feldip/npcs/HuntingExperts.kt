package org.rsmod.content.areas.misc.villages.feldip.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class HuntingExperts : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.hunting_jungle_information") { startDialogue(it.npc) { huntingExpert() } }
        onOpNpc1("npc.chinchompa_area_guide") { startDialogue(it.npc) { chinchompaGuide() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hunting_expert (without 99 Hunter)
    // TODO(content): [system:skillcapes] "Skillcape of Hunter." menu with 99 Hunter
    // TODO(content): [system:hunter_kit] "Can you give me some hunting traps?" one-time Hunter kit
    // TODO(content): [quest:while_guthix_sleeps] training a broav
    private suspend fun Dialogue.huntingExpert() {
        chatNpc(
            neutral,
            "Whoah there, stranger, careful where you're walking. I almost clobbered you there; " +
                "thought you were a larupia or something.",
        )
        val option =
            choice4(
                "What are you doing here?",
                1,
                "What's a larupia?",
                2,
                "What is that cool cape you're wearing?",
                3,
                "Ahh, leave me alone you crazy killing person.",
                4,
            )
        when (option) {
            1 -> whatAreYouDoing()
            2 -> larupia()
            3 -> {
                chatPlayer(quiz, "What is that cool cape you're wearing?")
                chatNpc(
                    happy,
                    "This? It's a Hunter's Skillcape. It shows that I am a true master of the art " +
                        "of Hunting and provides five daily teleports to either the Feldip Hills " +
                        "or Wilderness Hunter areas. If you ever manage to train your",
                )
                chatNpc(
                    happy,
                    "Hunter skill to its maximum level then I could sell you one.",
                )
            }
            4 -> {
                chatPlayer(angry, "Ahh, leave me alone you crazy killing person.")
                chatNpc(laugh, "Heh, well I guess it's not everyone's thing.")
            }
        }
    }

    private suspend fun Dialogue.whatAreYouDoing() {
        chatPlayer(quiz, "What are you doing here?")
        chatNpc(
            happy,
            "Me? Oh, I'm just having some fun, capturing wild animals, living on the edge, stuff " +
                "like that.",
        )
        chatNpc(
            happy,
            "Hey, want me to teach you how to catch the critters around here? Ain't come across " +
                "anything yet that I couldn't capture.",
        )
        val option =
            choice5(
                "Okay, where's a good place to start?",
                1,
                "What sort of things are there to catch around here?",
                2,
                "How can I improve my chances of making a capture?",
                3,
                "Where can I get clothes and equipment like yours?",
                4,
                "I think I'll give it a miss right now.",
                5,
            )
        when (option) {
            1 -> goodPlaceToStart()
            2 -> creatures()
            3 -> improveChances()
            4 -> clothes()
            5 -> {
                chatPlayer(neutral, "I think I'll give it a miss right now, thanks.")
                chatNpc(neutral, "Suit yourself!")
            }
        }
    }

    private suspend fun Dialogue.goodPlaceToStart() {
        chatPlayer(quiz, "Ok, where's a good place to start?")
        chatNpc(
            neutral,
            "Your easiest bet around here is probably to go for some of the birds. Go for the " +
                "crimson swifts; they're the bright red ones that hang around near the coast. " +
                "They're as obliging as can be, or maybe they're just",
        )
        chatNpc(
            neutral,
            "plain dumb, but either way they just seem to throw themselves into traps half the " +
                "time.",
        )
        chatPlayer(quiz, "What sort of traps should I use?")
        chatNpc(
            neutral,
            "Just use a standard bird snare and you'll be fine. You can get them in any decent " +
                "shop that sells Hunter gear.",
        )
        chatPlayer(quiz, "How do they work, then?")
        snareWorks()
        chatNpc(
            happy,
            "Further, as the perch drops, it releases a noose that tightens around the bird's " +
                "feet and captures it. Neat huh?",
        )
        chatNpc(quiz, "So, was there anything else you wanted to ask about?")
        val option =
            choice2(
                "What other sorts of things are there to catch around here?",
                1,
                "Thanks for your help.",
                2,
            )
        when (option) {
            1 -> creatures()
            2 -> thanks()
        }
    }

    private suspend fun Dialogue.snareWorks() {
        chatNpc(
            neutral,
            "Well, they're kind of sneaky, really. You've got a tall post with what appears to be " +
                "a little perch sticking out the side. Now, the bird will see this as somewhere " +
                "to sit, but the perch is actually rigged such that when the bird",
        )
        chatNpc(neutral, "lands on it, it'll drop away.")
    }

    private suspend fun Dialogue.thanks() {
        chatPlayer(happy, "Thanks for your help.")
        chatNpc(happy, "Not a problem, any time.")
    }

    private suspend fun Dialogue.creatures() {
        chatPlayer(quiz, "What sort of things are there to catch around here?")
        chatNpc(
            neutral,
            "What're you interested in? You've got anything from birds for beginners up to " +
                "larupias for the pros, not to mention weasels, butterflies, barb-tails and " +
                "chinchompas.",
        )
        creatureMenu()
    }

    private suspend fun Dialogue.creatureMenu() {
        while (true) {
            val option =
                choice4(
                    "How would I go about catching birds?",
                    1,
                    "How do I get hold of a weasel?",
                    2,
                    "Well, seeing as you seem to like larupias so much...",
                    3,
                    "More...",
                    4,
                )
            when (option) {
                1 -> birds()
                2 -> weasel()
                3 -> larupias()
                4 -> {
                    val more =
                        choice4(
                            "Barb-tails sound interesting.",
                            1,
                            "What on earth is a chinchompa?",
                            2,
                            "Somehow butterflies don't strike me as your sort of thing.",
                            3,
                            "Back...",
                            4,
                        )
                    when (more) {
                        1 -> barbTails()
                        2 -> chinchompas()
                        3 -> butterflies()
                        4 -> continue
                    }
                }
            }
            if (!askAgain()) {
                return
            }
        }
    }

    private suspend fun Dialogue.askAgain(): Boolean {
        val option =
            choice4(
                "I'd like to ask about another creature...",
                1,
                "How can I improve my chances of making a capture?",
                2,
                "Where can I get clothes and equipment like yours?",
                3,
                "Thanks for your help.",
                4,
            )
        when (option) {
            1 -> return true
            2 -> improveChances()
            3 -> clothes()
            4 -> thanks()
        }
        return false
    }

    private suspend fun Dialogue.birds() {
        chatPlayer(quiz, "How would I go about catching birds?")
        chatNpc(
            neutral,
            "There are a couple of varieties that you can try your hand at catching around here, " +
                "but the technique for both is essentially the same. You need to set up a bird " +
                "snare.",
        )
        chatPlayer(quiz, "How do they work, then?")
        snareWorks()
        chatNpc(
            happy,
            "Further, as the perch drops, it releases a noose that tightens around the bird's " +
                "feet and captures it. Neat, huh?",
        )
        chatNpc(quiz, "So, was there anything else you wanted to ask about?")
    }

    private suspend fun Dialogue.weasel() {
        chatPlayer(quiz, "How do I get hold of a weasel?")
        chatNpc(
            neutral,
            "Not with large quantities of rice and treacle as many people would have you believe. " +
                "Besides, the Feldip weasel isn't technically a weasel, but, as is typical with " +
                "any sort of colloquial name, it's been called that often enough",
        )
        chatNpc(neutral, "now that it's stuck.")
        chatNpc(
            neutral,
            "Anyway, the Feldip weasel's main survival technique is in hiding; other than that, " +
                "it's pretty stupid. So, if you can actually find one, then you've probably got " +
                "a fairly good chance of catching one.",
        )
        chatPlayer(quiz, "And how would I actually go about finding one?")
        chatNpc(
            neutral,
            "The best place to start would probably be just south of here. You'll find lots of " +
                "kebbit burrows there. Take a close look at some of them, follow the trails and " +
                "you can't go far wrong.",
        )
    }

    private suspend fun Dialogue.larupias() {
        chatPlayer(neutral, "Well, seeing as you seem to like larupias so much...")
        chatNpc(happy, "Ah yes, yes, larupias are fun; just think of them as big kittens.")
        chatPlayer(worried, "Uh huh, big kittens that just want to rip your throat out!")
        chatNpc(
            neutral,
            "Indeed, and you can use this to your advantage. If a larupia is keen to chase you " +
                "then let it; that way you can happily lead it straight into your trap without " +
                "any fuss or waiting around.",
        )
        chatPlayer(quiz, "Into what sort of trap should I lead it?")
        chatNpc(
            neutral,
            "A pit trap should do you. There are some appropriate pits to the east of here that " +
                "you can use. Cover one with a few logs and branches, tease the kitty and then " +
                "lead it to the pit. Simple.",
        )
    }

    private suspend fun Dialogue.barbTails() {
        chatPlayer(happy, "Barb-tails sound interesting.")
        chatNpc(
            neutral,
            "They do sound interesting, don't they? Unfortunately, they're actually just another " +
                "type of kebbit; those things get everywhere. Still, the tails of these " +
                "particular kebbits can be useful for fishing.",
        )
        chatPlayer(quiz, "So, how would I go about catching one them?")
        chatNpc(neutral, "Just squash them with a boulder, quick and simple.")
        chatPlayer(
            confused,
            "What, you mean just pick up some boulders and start throwing them at kebbits? That " +
                "doesn't sound simple.",
        )
        chatNpc(
            laugh,
            "No silly, you let the creature squash itself. Just use some sticks to hold a boulder " +
                "in a nice precarious position, place some bait underneath and let the creature " +
                "knock itself out.",
        )
    }

    private suspend fun Dialogue.chinchompas() {
        chatPlayer(quiz, "What on earth is a chinchompa?")
        chatNpc(
            happy,
            "Awwww, chinchompas are great. They're highly volatile little balls of fluff. Cute as " +
                "you like, but for some reason they do have this tendency to spontaneously " +
                "combust.",
        )
        chatPlayer(shocked, "What? Why?")
        chatNpc(
            neutral,
            "Probably something to do with the fact they'll eat anything and everything, " +
                "especially spicy food.",
        )
        chatNpc(
            neutral,
            "Anyways, if you want to capture a chinchompa, I suggest using a box trap. That way, " +
                "if the little fluffball does explode, then at least most of the blast will be " +
                "contained within the box.",
        )
    }

    private suspend fun Dialogue.butterflies() {
        chatPlayer(neutral, "Somehow butterflies don't strike me as your sort of thing.")
        chatNpc(
            neutral,
            "Not really, but some people like them. The ones around here are at least vaguely " +
                "interesting, though. The black ones are supposedly magical butterflies.",
        )
        chatPlayer(quiz, "Doesn't that make them harder to catch?")
        chatNpc(
            neutral,
            "Not particularly; you can still catch them using an ordinary butterfly net. Oh, and " +
                "you'll probably need some butterfly jars in which to keep them as well.",
        )
    }

    private suspend fun Dialogue.improveChances() {
        chatPlayer(quiz, "How can I improve my chances of making a capture?")
        chatNpc(
            neutral,
            "There are four key factors that will affect your chances: what bait you use, how " +
                "close to the trap you are, your appearance and your smell.",
        )
        while (true) {
            val option =
                choice4(
                    "How does bait help?",
                    1,
                    "Why is it important how close I am to the trap?",
                    2,
                    "Why would the creatures care about my appearance?",
                    3,
                    "What's wrong with my smell!?",
                    4,
                )
            when (option) {
                1 -> bait()
                2 -> distance()
                3 -> appearance()
                4 -> smell()
            }
        }
    }

    private suspend fun Dialogue.bait() {
        chatPlayer(quiz, "How does bait help?")
        chatNpc(
            neutral,
            "Unfortunately, creatures don't just jump into traps for the fun of it nearly as " +
                "often as we as Hunters might like. However, it is possible to improve those " +
                "odds. If you can find the right bait to use on your trap,",
        )
        chatNpc(
            neutral,
            "something your prey really craves, then hunger overrides fear and the creature is " +
                "just that little bit more likely to bounce to its furry little doom.",
        )
    }

    private suspend fun Dialogue.distance() {
        chatPlayer(quiz, "Why is it important how close I am to the trap?")
        chatNpc(
            neutral,
            "However well you might be blending in, if you're standing with your nose pressed " +
                "right up against your own trap, the poor little critters ain't going to go " +
                "anywhere near it. So, you need to make sure you put",
        )
        chatNpc(
            neutral,
            "a sensible distance - I'd say at least four strides - between yourself and your " +
                "trap.",
        )
    }

    private suspend fun Dialogue.appearance() {
        chatPlayer(quiz, "Why would the creatures care about my appearance?")
        chatNpc(
            neutral,
            "You're a human. From the point of view of a lot of creatures, you're big and scary. " +
                "So, it'd be best if the furry little monsters didn't notice you. You need to " +
                "blend in like the creatures themselves.",
        )
        chatPlayer(quiz, "And how do I do that?")
        chatNpc(
            neutral,
            "Easy, dress up like one of them. Even easier, dress up using the furs of the " +
                "creatures themselves; then you're bound to get the colour right. And, I " +
                "suppose, if you don't fancy just slinging a dead fur over your",
        )
        chatNpc(
            neutral,
            "shoulders, you can probably get them made into something more appealing. I hear " +
                "there are tailors in Varrock who are good at that sort of thing.",
        )
    }

    private suspend fun Dialogue.smell() {
        chatPlayer(angry, "What's wrong with my smell!?")
        chatNpc(
            neutral,
            "I'm sure there's nothing wrong with your smell...from a human point of view. But to " +
                "your prey, any trap that you've set up will reek of human, and that's going to " +
                "work against you if you don't do anything about it.",
        )
        chatPlayer(quiz, "So what should I do?")
        chatNpc(
            neutral,
            "You need to mask that lovely human scent of yours with something else. Smoke works " +
                "well. Take a lit torch and waft it around the trap a bit. You'll quickly " +
                "overpower most of the other smells around the trap.",
        )
    }

    private suspend fun Dialogue.clothes() {
        chatPlayer(quiz, "Where can I get clothes and equipment like yours?")
        chatNpc(
            neutral,
            "Well, the equipment isn't too hard to get hold of. You've got specialist shops like " +
                "Aleck's in Yanille that'll see you right. He's a bit pricey, mind, but it's " +
                "pretty decent kit.",
        )
        chatNpc(
            neutral,
            "The clothing is a tad trickier to get hold of, the materials required being rather " +
                "difficult to seperate from their reluctant owners.",
        )
        chatNpc(
            neutral,
            "But, if you can get the furs, there are shops like the tailor's in east Varrock that " +
                "can help you.",
        )
    }

    private suspend fun Dialogue.larupia() {
        chatPlayer(quiz, "What's a larupia?")
        chatNpc(
            shocked,
            "What's a larupia? You don't know? You mean you really don't recognise the " +
                "significance of these clothes I'm wearing?",
        )
        chatPlayer(neutral, "Well, they're certainly decorative...what about them?")
        chatNpc(
            happy,
            "Well, these are the furs from a larupia. Caught the creature myself I did, and these " +
                "make me blend in better, see?",
        )
        chatNpc(
            happy,
            "If you want to be successful as a hunter you've got to be as stealthy as a kyatt, as " +
                "quiet as a mouse and blend in like a, well, like a larupia, I guess!",
        )
        val option =
            choice3(
                "So, what's good to hunt around here?",
                1,
                "How can I improve my chances of making a capture?",
                2,
                "Where can I get clothes and equipment like yours?",
                3,
            )
        when (option) {
            1 -> creatures()
            2 -> improveChances()
            3 -> clothes()
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hunting_expert_(Red_chinchompa_hunting_ground)
    // TODO(content): [diary:western_provinces] entry after the hard tier
    private suspend fun Dialogue.chinchompaGuide() {
        chatPlayer(quiz, "What is this cave here for?")
        chatNpc(
            neutral,
            "Ah, a few of the most hunted creatures around here have been captured and secured in " +
                "this cave to allow the most esteemed hunters to catch them with ease.",
        )
        chatPlayer(quiz, "Can I use the cave?")
        chatNpc(
            neutral,
            "Unfortunately not. Only those who have completed the hard tier of the Western " +
                "Provinces achievement diary may enter.",
        )
        chatNpc(neutral, "Come back when you have and I shall allow you entry.")
    }
}
