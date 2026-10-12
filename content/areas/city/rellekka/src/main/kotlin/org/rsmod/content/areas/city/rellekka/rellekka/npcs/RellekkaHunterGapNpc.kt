package org.rsmod.content.areas.city.rellekka.rellekka.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class RellekkaHunterGapNpc : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.hunting_polar_information") { startDialogue(it.npc) { huntingExpert() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hunting_expert_(Rellekka_Hunter_area)
    // TODO(content): [system:hunter_kit] "Can you give me some hunting traps?" one-time Hunter kit
    private suspend fun Dialogue.huntingExpert() {
        chatNpc(happy, "Ah, this is a wonderful area for a spot of hunting, don't you think?")
        val option =
            choice3(
                "I don't know, is it?",
                1,
                "Can you teach me how to hunt?",
                2,
                "Sorry, I'm not that keen on hunting.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "I don't know, is it?")
                chatNpc(
                    happy,
                    "Oh absolutely; there's a huge variety of creatures to catch around here. " +
                        "You've got butterflies, polar kebbits, sabre-toothed kebbits, birds and " +
                        "even kyatts! Surely one of those piques your interest?",
                )
                creatureMenu()
            }
            2 -> teachMe()
            else -> {
                chatPlayer(neutral, "Sorry, I'm not that keen on hunting.")
                chatNpc(
                    sad,
                    "Really? What a shame, you looked like someone who could have had some " +
                        "promise.",
                )
            }
        }
    }

    private suspend fun Dialogue.teachMe() {
        chatPlayer(quiz, "Can you teach me how to hunt?")
        chatNpc(
            happy,
            "Why certainly. I reached the peak of my abilities some time ago, but I'm more than " +
                "happy to help newcomers to the field. Was there anything in particular you had " +
                "in mind?",
        )
        val option =
            choice3(
                "Where's the best place to start?",
                1,
                "What sort of creatures are there to hunt around here?",
                2,
                "How can I improve my chances of capturing a creature?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Where's the best place to start?")
                chatNpc(
                    neutral,
                    "I always say a good starting point is to learn how to follow the creatures " +
                        "you want to capture. Around here, that means practicing on polar kebbits.",
                )
                chatPlayer(quiz, "So how do I track them down, then?")
                polarKebbitTracking()
            }
            2 -> {
                chatPlayer(quiz, "What sort of creatures are there to hunt around here?")
                chatNpc(
                    happy,
                    "Well, there's pretty much anything you could want to hunt. You've got " +
                        "butterflies, polar kebbits, sabre-toothed kebbits, birds and even " +
                        "kyatts! Surely one of those interests you?",
                )
                creatureMenu()
            }
            else -> improveChances()
        }
    }

    private suspend fun Dialogue.creatureMenu() {
        while (true) {
            val option =
                choice5(
                    "Where would I find polar kebbits?",
                    1,
                    "How do you hunt birds?",
                    2,
                    "What's so special about butterflies?",
                    3,
                    "How can I capture a sabre-toothed kebbit?",
                    4,
                    "Oooh, kyatts sounds interesting.",
                    5,
                )
            when (option) {
                1 -> {
                    polarKebbits()
                    return
                }
                2 -> birds()
                3 -> butterflies()
                4 -> sabreToothedKebbits()
                else -> kyatts()
            }
            val next =
                choice3(
                    "I'd like to ask about another creature.",
                    1,
                    "How can I improve my chances of capturing a creature?",
                    2,
                    "Thanks for your help.",
                    3,
                )
            when (next) {
                1 -> continue
                2 -> improveChances()
                else -> {
                    chatPlayer(happy, "Thanks for your help.")
                    chatNpc(happy, "You're welcome, good luck with the hunting.")
                }
            }
            return
        }
    }

    private suspend fun Dialogue.polarKebbits() {
        chatPlayer(quiz, "Where would I find polar kebbits?")
        chatNpc(
            neutral,
            "Funnily enough, you've just put your finger right on the problem. With polar " +
                "kebbits, finding them is the hardest part! You'll have to track them down.",
        )
        chatPlayer(quiz, "How would I go about doing that, then?")
        polarKebbitTracking()
    }

    private suspend fun Dialogue.polarKebbitTracking() {
        chatNpc(
            neutral,
            "The easiest way to get started is by looking somewhere you can be sure a kebbit " +
                "will have been and then working from there. You see the holes around here " +
                "leading down into the ice? They're kebbit burrows!",
        )
        chatNpc(
            neutral,
            "You'll be hard pressed to catch a polar kebbit in its home, but if you search " +
                "around the entrance to a burrow then you might be able to find a trail.",
        )
        chatNpc(
            neutral,
            "Now, kebbit trails are usually fairly easy to follow once you've found them, but if " +
                "the trail does peter out, you should try inspecting the surrounding scenery for " +
                "clues as to where the creature may have gone.",
        )
        chatNpc(
            neutral,
            "Good places to look for clues include hollow logs in which the kebbit may have " +
                "hidden or vegetation which may have been chewed or damaged.",
        )
        chatNpc(
            neutral,
            "There are also tunnels running through the ice here that the kebbits use, so you " +
                "should check those out as well.",
        )
        chatNpc(
            neutral,
            "What's more, these tunnels can often help to shelter a trail from further snow, " +
                "making it even easier for you to pick them up again.",
        )
        chatNpc(
            neutral,
            "Eventually, of course, the kebbit will have probably hidden somewhere. Polar " +
                "kebbits don't hang about in the open for long for fear of their natural " +
                "predators.",
        )
        chatNpc(
            neutral,
            "If you suspect a kebbit is close by, try looking around for signs of disturbed " +
                "snow. Polar kebbits will often bury themselves in snow drifts as a means of " +
                "hiding.",
        )
        chatNpc(
            neutral,
            "If you think you've found a kebbit, use a noose wand to haul the creature out from " +
                "the snow.",
        )
        chatPlayer(shocked, "Wow, that's quite a lot to remember.")
        chatNpc(
            happy,
            "Don't worry; if you find yourself struggling to follow a trail, just come and ask " +
                "me and I'll see if I can help.",
        )
        chatNpc(
            neutral,
            "But remember, the best place to start is by searching around their burrows.",
        )
        chatPlayer(happy, "Great, thanks.")
    }

    private suspend fun Dialogue.birds() {
        chatPlayer(quiz, "How do you hunt birds?")
        chatNpc(
            neutral,
            "Ah, you can get cunning little bird snares for just that. You should be able to " +
                "find them in specialist Hunter shops. Try to lay them in fairly open areas with " +
                "lots of birds.",
        )
        chatPlayer(quiz, "Do I need to place any bait on these traps?")
        chatNpc(
            neutral,
            "No, that's not necessary. The idea is that the bird will see the trap as somewhere " +
                "to perch. If the bird lands on the trigger stick, it should drop away and a " +
                "small loop of wire should close around the bird's feet.",
        )
        chatNpc(
            neutral,
            "And remember, it's important to collect any captures from your traps fairly " +
                "quickly; otherwise, you risk losing them to various wild scavengers.",
        )
    }

    private suspend fun Dialogue.butterflies() {
        chatPlayer(quiz, "What's so special about butterflies?")
        chatNpc(neutral, "That depends entirely on the butterfly.")
        chatNpc(
            neutral,
            "I'll admit that the activities of many butterflies may be of no interest at all. " +
                "However, we are lucky in having around here some fine examples of rare thaumic " +
                "butterflies.",
        )
        chatNpc(
            neutral,
            "These butterflies reputedly have magical powers that, when released, can make a " +
                "person stronger or heal otherwise critical wounds.",
        )
        chatNpc(
            neutral,
            "At this point, I should offer a note of caution, however: it is also said that " +
                "these powers will not manifest themselves if the butterflies are captured for " +
                "selfish purposes.",
        )
        chatNpc(
            neutral,
            "Rather, they are gifts that must be used by one person to aid another. Of course, " +
                "you'll have to catch them first.",
        )
        chatPlayer(quiz, "And how do I do that?")
        chatNpc(
            happy,
            "Why, with a butterfly net of course! You should also carry some empty butterfly " +
                "jars with you in which to keep the butterflies.",
        )
    }

    private suspend fun Dialogue.sabreToothedKebbits() {
        chatPlayer(quiz, "How can I capture a sabre-toothed kebbit?")
        chatNpc(
            neutral,
            "Ah, now for these creatures I would recommend using a deadfall trap. Deadfalls are " +
                "quite easy to set up, and yet you can catch some very reasonably sized " +
                "creatures with them.",
        )
        chatNpc(
            neutral,
            "Deadfall traps basically work by having a large dead weight, such as a boulder, " +
                "positioned so as to be delicately balanced above some form of bait.",
        )
        chatNpc(
            neutral,
            "You will need to make sure you have a knife and some wood with you to create " +
                "appropriate supports and a trigger for this sort trap.",
        )
        chatNpc(
            neutral,
            "In the case of sabre-toothed kebbits you should use some form of raw meat to bait " +
                "your trap, but other animals may prefer other baits, such as fish or vegetables.",
        )
    }

    private suspend fun Dialogue.kyatts() {
        chatPlayer(happy, "Oooh, kyatts sounds interesting. I want to catch one of those.")
        chatNpc(
            neutral,
            "Well, you've set your sights high, kyatts are incredibly fearsome creatures, but " +
                "with determination like that I'm sure you'll make it.",
        )
        chatNpc(
            neutral,
            "Now, you're not going to be able to catch a kyatt with just a small box or a noose " +
                "on a stick. You need a much bigger trap.",
        )
        chatNpc(
            neutral,
            "Pit traps are a good bet. You'd be best off finding a natural dip in the ground as " +
                "the basis for your trap. If you dig a pit yourself, you risk creatures noticing " +
                "the disturbed ground nearby.",
        )
        chatNpc(
            neutral,
            "Once you've found an appropriate spot, you'll need some logs and a knife. The logs " +
                "serve two purposes.",
        )
        chatNpc(
            neutral,
            "Some can be carved into sharp skewers for the bottom of the pit; hopefully the " +
                "kyatt will die when it falls onto these. Others can be used to conceal the " +
                "trap.",
        )
        chatNpc(
            neutral,
            "Of course, even then the pit is going to look suspicious, so you're going to need " +
                "to lure the kyatt onto the branches in some way. What actually tends to work " +
                "best for this is, well, yourself!",
        )
        chatPlayer(worried, "What? I don't like the sound of that.")
        chatNpc(
            laugh,
            "Yes, that's the normal response, but I kid you not. If you really want a kyatt to " +
                "follow you over that pit then just give it a good poke!",
        )
        chatNpc(
            neutral,
            "Of course, I suggest you use something long to poke it with. Poking a kyatt with " +
                "your finger is likely to end in unfortunate limb loss.",
        )
        chatNpc(
            neutral,
            "Fortunately, you can get very good sticks for poking from most Hunter shops.",
        )
        chatNpc(
            neutral,
            "Once you have...got the creature's attention, try to lead it to one of your traps. " +
                "Make sure to jump the branches when you reach the trap yourself, of course; it " +
                "would be embarrassing to fall into your own trap.",
        )
        chatNpc(
            neutral,
            "Then, if the kyatt is giving you all of its attention, hopefully it won't notice " +
                "the trap, and with any luck, it will stumble right into the pit.",
        )
        chatPlayer(worried, "That sounds a bit hit or miss, and kind of suicidal too!")
        chatNpc(
            happy,
            "Well that's what makes it exciting, isn't it? But honestly, if you want to capture " +
                "a kyatt, that's your best bet.",
        )
    }

    private suspend fun Dialogue.improveChances() {
        chatPlayer(quiz, "How can I improve my chances of capturing a creature?")
        chatNpc(neutral, "Ah, the key there is concealment, both of yourself and your traps.")
        chatNpc(
            neutral,
            "The most obvious point is to stay out of sight of your own traps. If possible, hide " +
                "around a wall, hedge or similar. Failing that, make sure you're a good few feet " +
                "away from your trap.",
        )
        chatNpc(
            neutral,
            "You can hide yourself even better by wearing clothing appropriate to the area in " +
                "which you are hunting.",
        )
        chatNpc(
            neutral,
            "Your best bet is to use the skins of creatures native to the area. There is a very " +
                "good tailor in east Varrock who may be able to help you with making clothing " +
                "out of such skins.",
        )
        chatNpc(
            neutral,
            "Finally, a lot of creatures may avoid your traps if they can smell a human scent on " +
                "them. This is where smoking a trap can be useful.",
        )
        chatNpc(
            neutral,
            "It takes a bit of practice, but by using the smoke from a torch, you can mask any " +
                "other scents that may surround a trap.",
        )
        chatPlayer(happy, "Wow, thanks for your help.")
    }
}
