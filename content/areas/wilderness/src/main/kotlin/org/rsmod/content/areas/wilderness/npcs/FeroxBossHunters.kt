package org.rsmod.content.areas.wilderness.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:wilderness_weapon_upgrades] the "Ask about rare item." option, combining
//  boss weapons for 500,000 coins and the replies to boss items used on these npcs
class FeroxBossHunters : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.wildy_skeleton_hunter") { startDialogue(it.npc) { androsMai() } }
        onOpNpc1("npc.wildy_bear_hunter") { startDialogue(it.npc) { derseVenator() } }
        onOpNpc1("npc.wildy_spider_hunter") { startDialogue(it.npc) { phabelleBile() } }
        onOpNpc1("npc.wbr_voidwakermaker") { startDialogue(it.npc) { madamSikaro() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Andros_Mai (no Vet'ion kills)
    // TODO(content): [boss:vetion] the reply for players with a Vet'ion kill
    private suspend fun Dialogue.androsMai() {
        chatPlayer(happy, "Well met, fellow adventurer.")
        chatNpc(
            angry,
            "Adventurer? I am no mere Adventurer, I am Andros Mai, brother of the Holy Order of " +
                "Saradomin. You would do well to greet me with respect.",
        )
        chatPlayer(
            confused,
            "Andros Mai? Sorry, but I can't say I've heard of you I'm afraid.",
        )
        chatNpc(
            neutral,
            "I wouldn't have expected someone of your... stature to know of me.",
        )
        chatNpc(neutral, "Tell me, what do you know of the vile Vet'ion?")
        chatPlayer(neutral, "Nothing, but I have a feeling you're about to fill me in.")
        chatNpc(
            angry,
            "Vet'ion was a devout follower of the heretical god Zamorak. It is the will of my " +
                "order and my duty as a follower of the true god Saradomin to end his tyranny.",
        )
        chatNpc(
            neutral,
            "After many years in my order's libraries, I stumbled upon an old tome which " +
                "mentioned Vet'ion's final resting place, which I believe to be his source of " +
                "power.",
        )
        chatNpc(
            angry,
            "His very existence is an insult to my lord Saradomin and I will not suffer it to " +
                "exist any longer.",
        )
        chatPlayer(
            quiz,
            "Then why are you waiting here? Seems to me that you don't want to face Vet'ion.",
        )
        chatNpc(angry, "I have no fear of Vet'ion!")
        chatNpc(
            neutral,
            "I am merely meditating and preparing for the battle. I need to focus my mind " +
                "before going out to find his resting place.",
        )
        chatPlayer(
            laugh,
            "It's okay, sometimes I pretend to be busy when there's something I don't want to " +
                "do, too.",
        )
        chatNpc(verymad, "This conversation is over! Leave me before I lose my temper.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Derse_Venator (no Callisto kills)
    // TODO(content): [boss:callisto] the reply for players with a Callisto kill
    private suspend fun Dialogue.derseVenator() {
        chatPlayer(neutral, "You look ready for combat.")
        chatNpc(neutral, "Aye, indeed I am.")
        chatPlayer(
            quiz,
            "What brings you to this place? I can't imagine anyone coming here by choice.",
        )
        chatNpc(
            neutral,
            "You're right, I did not come here by choice. I came here for vengeance.",
        )
        chatPlayer(
            laugh,
            "Vengeance? Against whom? Did someone take your gardening equipment?",
        )
        chatNpc(angry, "My mission is not against another man and I will not have it mocked.")
        chatPlayer(
            sad,
            "Okay, sorry, I was just trying to lighten the mood. Care to tell me what it is " +
                "against then?",
        )
        chatNpc(neutral, "Callisto, the great bear of the wilderness.")
        chatPlayer(neutral, "Can't say I've come across it before.")
        chatNpc(
            neutral,
            "I'm not surprised, you'd do well to avoid him. Callisto is a great bear, his " +
                "strength and size enhanced by the corruption of the Wilderness.",
        )
        chatPlayer(quiz, "Why are you so intent on taking him down?")
        chatNpc(
            sad,
            "Callisto killed my father. After hearing of a great beast in the heart of the " +
                "Wilderness, he took it upon himself to bring him down.",
        )
        chatNpc(sad, "He left our family home in Hosidius to hunt it down and...")
        chatNpc(
            sad,
            "... I haven't seen my father since. I swore I would avenge him and have dedicated " +
                "my whole life to training for that moment.",
        )
        chatPlayer(quiz, "What happens after? When you do take Callisto down that is.")
        chatNpc(confused, "Oh... uh... I never thought about that...")
        chatNpc(
            confused,
            "My whole life has been spent preparing to avenge my father, I never thought about " +
                "anything else...",
        )
        chatPlayer(
            neutral,
            "You seem like you've got a lot to think about then. I'll leave you to it.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Phabelle_Bile (no Venenatis kills)
    // TODO(content): [boss:venenatis] the reply for players with a Venenatis kill
    private suspend fun Dialogue.phabelleBile() {
        chatPlayer(shocked, "Wow, didn't expect to find a scientist out here.")
        chatNpc(
            angry,
            "I'm out here on a mission, do you think I chose to hang around these lowlifes?",
        )
        chatPlayer(neutral, "I'm not one to judge.")
        chatPlayer(quiz, "What is this 'mission' you mentioned?")
        chatNpc(
            neutral,
            "I'm out here to conduct research, I've been told a giant spider lurks in the heart " +
                "of the Wilderness.",
        )
        chatNpc(neutral, "Her venom is priceless and it would aid me greatly in my research.")
        chatPlayer(
            confused,
            "Research? What could you possibly want with a giant spider's venom?",
        )
        chatNpc(
            neutral,
            "Venenatis is the last of her kind. Lots of giant spiders used to dwell deep in the " +
                "mountains until the savage dwarves killed them all.",
        )
        chatNpc(neutral, "She is the last one left and I must have a sample of her venom.")
        chatNpc(angry, "What I want with the specimen however, is none of your business!")
        chatPlayer(
            worried,
            "Woah sorry, didn't mean to touch a nerve. Fighting a giant spider sounds like a " +
                "great idea.",
        )
        chatNpc(
            neutral,
            "I've tried to put it off for as long as I could, but this is the final component " +
                "needed for my research.",
        )
        chatPlayer(quiz, "Then what are you waiting around here for? Go out there and fight her.")
        chatNpc(
            worried,
            "I would go out and fight her now, but I must admit, I am also a little scared. I've " +
                "never been in a fight before.",
        )
        chatPlayer(happy, "I'm sure you'll be alright, it's just a gigantic spider!")
        chatNpc(sad, "Thanks for your words of confidence...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Madam_Sikaro
    // TODO(content): [system:voidwaker] the weapon part options, assembling the Voidwaker for
    //  500,000 coins, the money question afterwards and the Voidwaker used on her
    private suspend fun Dialogue.madamSikaro() {
        chatNpc(neutral, "Ah, greetings. What can I do for you?")
        val option =
            choice3(
                "Who are you?",
                1,
                "What are you doing here?",
                2,
                "Goodbye.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(neutral, "I'm Madam Sikaro.")
                chatPlayer(neutral, "...")
                chatNpc(neutral, "...")
                chatPlayer(confused, "...Is that it?")
                chatNpc(
                    neutral,
                    "Why shouldn't it be? You asked who I was, and I answered.",
                )
                chatPlayer(
                    angry,
                    "I was hoping to learn something about you, like what you were doing here...!",
                )
                chatNpc(neutral, "Well, perhaps next time you'll ask a more direct question.")
            }
            2 -> {
                chatPlayer(quiz, "What are you doing here?")
                chatNpc(neutral, "Making a wizard hideout.")
                chatPlayer(confused, "A wizard... hideout?")
                chatNpc(
                    happy,
                    "A wizard hideout! You know how some wizards have their own towers? Think " +
                        "that but underground.",
                )
                chatPlayer(quiz, "So why not just get a tower?")
                chatNpc(
                    angry,
                    "Land price. Kingdoms want you to pay an extortionate amount of money to " +
                        "build on 'their' land, it turns out.",
                )
                chatPlayer(neutral, "So instead you ended up in the basement of a tavern.")
                chatNpc(
                    neutral,
                    "Yep. Camarst said I could have it as long as I don't hurt any of his " +
                        "customers too badly.",
                )
                chatNpc(neutral, "Not that I actually plan to hurt any of his customers.")
            }
            3 -> {
                chatPlayer(neutral, "Goodbye.")
                chatNpc(
                    confused,
                    "You walked up to me just to say goodbye? Ok, sure, goodbye.",
                )
            }
        }
    }
}
