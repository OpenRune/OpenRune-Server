package org.rsmod.content.areas.misc.outskirts.icemountain.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class IceMountain : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.oracle") { startDialogue(it.npc) { oracle() } }
        onOpNpc1("npc.bim_willow_outside") { startDialogue(it.npc) { willow() } }
        for (guard in FORTRESS_GUARDS) {
            onOpNpc1(guard) { startDialogue(it.npc) { fortressGuard() } }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Oracle
    // TODO(content): [quest:dragon_slayer_i] asking about the map piece behind the magic door
    // TODO(content): [system:treasure_trails] medium clue challenge scroll dialogue
    // TODO(content): [system:wise_old_man_tasks] delivering the Wise Old Man's message
    private suspend fun Dialogue.oracle() {
        chatPlayer(quiz, "Can you impart your wise knowledge to me, O Oracle?")
        chatNpc(neutral, ORACLE_SAYINGS[access.random.of(ORACLE_SAYINGS.size)])
    }

    // https://oldschool.runescape.wiki/w/Transcript:Below_Ice_Mountain
    // TODO(content): [quest:below_ice_mountain] the 16 quest point check ("You do not meet all
    // of the requirements..."), the quest start, and the in-progress and after-quest dialogue
    private suspend fun Dialogue.willow() {
        chatPlayer(quiz, "Hello there. What are you doing all the way out here?")
        chatNpc(shifty, "Just... looking around.")
        chatPlayer(quiz, "Oh, did you lose something?")
        chatNpc(shifty, "No... I found something. A door.")
        chatPlayer(quiz, "A door? I see doors all the time. What makes this one so special?")
        chatNpc(
            neutral,
            "It's buried beneath a tonne of rocks. Could be the entrance to some ancient ruins...",
        )
        chatPlayer(happy, "Wow! Ancient ruins? Are you an archaeologist or something?")
        chatNpc(confused, "Archaeologist? Me? Hmm...")
        chatNpc(
            happy,
            "Of course I am! I'm the greatest archaeologist of our time. The name's Willow! Have " +
                "you not heard of me?",
        )
        chatPlayer(
            neutral,
            "I'm afraid I haven't. Are you looking for a way into the ruins?",
        )
        chatNpc(sad, "You bet I am, but I don't think there's any other way in.")
        chatPlayer(quiz, "Oh dear. Do you have another plan to get in?")
        chatNpc(
            neutral,
            "Potentially, but I won't be able to do it on my own. Nor can I leave the area. If " +
                "I'm not here to guard the entrance, some other archaeologist might come along " +
                "and steal the glory from me!",
        )
        chatNpc(
            happy,
            "Maybe you could give me a hand? There could be untold riches inside the ruins!",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fortress_Guard
    // TODO(content): [quest:black_knights_fortress] door and meeting room dialogue
    private suspend fun Dialogue.fortressGuard() {
        if (DISGUISE.all { it in player.worn }) {
            chatNpc(angry, "Hey! Get back on duty!")
            chatPlayer(worried, "Uh...")
            return
        }
        chatNpc(angry, "Get lost. This is private property.")
        chatPlayer(neutral, "Yes, but I work here!")
        chatNpc(
            neutral,
            "Well, this is the guards' entrance. I might be new here but I can tell you're not a " +
                "guard.",
        )
        chatNpc(neutral, "You're not even wearing proper guards uniform!")
        when (choice2("Oh pleeeaaase let me in!", 1, "So what is this uniform?", 2)) {
            1 -> {
                chatPlayer(sad, "Oh pleeeaaase let me in!")
                chatNpc(angry, "Go away. You're getting annoying.")
            }
            2 -> {
                chatPlayer(quiz, "So what is this uniform?")
                chatNpc(
                    neutral,
                    "Well you can see me wearing it. It's an iron chainbody and a medium bronze " +
                        "helm.",
                )
                chatPlayer(
                    shifty,
                    "Hmmm... I wonder if I can make that or get some in the local towns....",
                )
                chatNpc(quiz, "What was that you muttered?")
                chatPlayer(happy, "Oh, nothing important!")
            }
        }
    }

    private companion object {
        val FORTRESS_GUARDS =
            listOf(
                "npc.fortressguard",
                "npc.fortressguard_01",
                "npc.fortressguard_02",
                "npc.fortressguard_03",
                "npc.fortressguard_04",
            )
        val DISGUISE = listOf("obj.iron_chainbody", "obj.bronze_med_helm")
        val ORACLE_SAYINGS =
            listOf(
                "Capes are always in fashion!",
                "The light at the end of the tunnel is the demon-infested lava pit.",
                "Beware the cabbage: it is both green AND leafy.",
                "They say that ham does not mix well with other kinds of meat.",
                "A woodchuck does not chuck wood.",
                "No. I'm not in the mood.",
                "It's not you; it's me.",
                "Too many cooks spoil the anchovy pizza.",
                "The God Wars are over...as long as the thing they were fighting over remains " +
                    "hidden.",
                "He who uses the power of custard mixes it with his tears.",
                "Nothing like a tasty fish.",
                "Don't judge a book by its cover - judge it on its' grammar and, punctuation.",
                "Yes, I can. But I'm not going to.",
                "A bird in the hand can make a tasty snack.",
                "Pies...they're great, aren't they?",
                "Who guards the guardsmen?",
                "The great snake of Guthix guards more than she knows.",
                "Do not fear the dragons...fear their kin.",
                "Everyone you know will one day be dead.",
                "If a tree falls in the forest and no one is around, then nobody gets " +
                    "Woodcutting xp.",
                "Sometimes you get lucky, sometimes you don't.",
                "The chicken came before the egg.",
                "Is it time to wake up? I am not sure...",
                "When in Asgarnia, do as the Asgarnians do.",
                "An answer is unimportant; it is the question that matters.",
                "The goblins will never make up their minds on their own.",
                "Jas left a stone behind.",
                "Many secrets are buried under this land.",
                "There are no crisps at the party.",
                "Help wanted? Enquire within.",
            )
    }
}
