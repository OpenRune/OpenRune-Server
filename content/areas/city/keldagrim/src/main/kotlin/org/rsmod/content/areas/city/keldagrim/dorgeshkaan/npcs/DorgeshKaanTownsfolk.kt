package org.rsmod.content.areas.city.keldagrim.dorgeshkaan.npcs

import dev.openrune.types.MesAnimType
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class DorgeshKaanTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.dorgesh_male_ironmonger") { startDialogue(it.npc) { bartak() } }
        onOpNpc1("npc.dorgesh_male_bone_seller") { startDialogue(it.npc) { barlak() } }
        onOpNpc1("npc.dorgesh_male_patrol1") { startDialogue(it.npc) { crateGoblin() } }
        onOpNpc1("npc.dorgesh_goblin_frogspit") { startDialogue(it.npc) { spitGoblin() } }
        onOpNpc1("npc.dorgesh_childminder") { startDialogue(it.npc) { mernik() } }
        for (child in CHILDREN) {
            onOpNpc1(child) { startDialogue(it.npc) { caveGoblinChild() } }
        }
        for (baby in BABIES) {
            onOpNpc1(baby) { startDialogue(it.npc) { baby() } }
        }
        for (gourmet in GOURMETS) {
            onOpNpc1(gourmet) { startDialogue(it.npc) { gourmet() } }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bartak
    private suspend fun Dialogue.bartak() {
        chatNpc(shocked, "Oh no! What's broken?")
        chatPlayer(confused, "What? Nothing's broken.")
        chatNpc(worried, "I'm sorry. I'm just a bit jumpy.")
        chatNpc(
            worried,
            "I'm in charge of all the metalworking of Dorgesh-Kaan. It's a big responsibility!",
        )
        chatNpc(
            worried,
            "If something metal breaks I have to fix it. And lots of things are made of metal!",
        )
        chatPlayer(happy, "Don't worry, I'm sure you're up to the task!")
        chatNpc(worried, "I hope you're right.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Barlak
    // TODO(content): [system:barlak_bones] buying long and curved bones and giant shells
    private suspend fun Dialogue.barlak() {
        chatNpc(happy, "Bones!")
        chatPlayer(confused, "What?")
        chatNpc(sad, "I don't have any bones!")
        var asked = STAND_UP
        while (true) {
            val topics = BARLAK_TOPICS.filter { it != asked }
            val option =
                when (asked) {
                    STAND_UP ->
                        choice5(
                            STAND_UP,
                            STAND_UP,
                            topics[0],
                            topics[0],
                            topics[1],
                            topics[1],
                            topics[2],
                            topics[2],
                            GOODBYE,
                            GOODBYE,
                        )
                    "" ->
                        choice4(
                            topics[0],
                            topics[0],
                            topics[1],
                            topics[1],
                            topics[2],
                            topics[2],
                            GOODBYE,
                            GOODBYE,
                        )
                    else -> choice3(topics[0], topics[0], topics[1], topics[1], GOODBYE, GOODBYE)
                }
            when (option) {
                STAND_UP -> {
                    chatPlayer(quiz, "Then how do you stand up?")
                    chatNpc(confused, "What?")
                    chatPlayer(
                        laugh,
                        "How do you stand up if you have no bones? Shouldn't you collapse into " +
                            "a gelatinous blob?",
                    )
                    chatNpc(laugh, "Ha, ha, ha, ha, ha!")
                    asked = ""
                    continue
                }
                NEED_BONES -> {
                    chatPlayer(quiz, NEED_BONES)
                    chatNpc(
                        laugh,
                        "To stand up properly. Otherwise I'd collapse into a gelatinous blob!",
                    )
                    chatNpc(laugh, "Ha, ha, ha, ha, ha!")
                    chatNpc(
                        neutral,
                        "No, seriously, I need bones to use as a Construction material. We " +
                            "always need big bones to prop up the mine shafts and to make other " +
                            "temporary structures.",
                    )
                }
                KIND_OF_BONES -> {
                    chatPlayer(quiz, KIND_OF_BONES)
                    chatNpc(
                        neutral,
                        "Enormous bones, as big as you can get. Not just any big bones will do.",
                    )
                    chatNpc(
                        neutral,
                        "Sometimes you might find a particularly long, straight bone. That's " +
                            "the kind of thing I need; I'll give you 1,000gp for one of them.",
                    )
                    chatNpc(
                        neutral,
                        "Occasionally, you might find a long curved bone - these are especially " +
                            "valuable - I'll give you 2,000gp for them.",
                    )
                    chatNpc(
                        neutral,
                        "I'll also teach you a bit about how we use the bones in Construction, " +
                            "if you like.",
                    )
                }
                BESIDES_BONES -> {
                    chatPlayer(quiz, BESIDES_BONES)
                    chatNpc(
                        neutral,
                        "Well, I've had a few people bring me some interesting giant shells. " +
                            "They say they came from giant snails and giant tortoises.",
                    )
                    chatNpc(
                        neutral,
                        "Of course, like the bones, some shells are better than others. I'll " +
                            "give you 250gp for ordinary shells, but what I really need are " +
                            "perfect shells. I'll pay double for them and give you a few tips " +
                            "about Crafting too.",
                    )
                }
                else -> {
                    chatPlayer(neutral, GOODBYE)
                    chatNpc(neutral, GOODBYE)
                    return
                }
            }
            asked = option
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Crate_goblin
    private suspend fun Dialogue.crateGoblin() {
        chatNpc(neutral, "Excuse me, I need to deliver this.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Spit_goblin
    private suspend fun Dialogue.spitGoblin() {
        chatPlayer(happy, "That smells nice!")
        chatNpc(happy, "Mmm, yes. Some hunters caught it earlier today.")
        chatPlayer(quiz, "Can I have some?")
        chatNpc(neutral, "Oh, it's not cooked yet. But they sell frogburgers in the market!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Mernik
    // TODO(content): [quest:another_slice_of_ham] the child asking if you helped Zanik
    // TODO(content): [verify:in_game] which cave goblin child's chathead answers for the class
    private suspend fun Dialogue.mernik() {
        chatNpc(happy, "Oh, hello! Children, say hello to the surface-dweller.")
        when (access.random.of(4)) {
            0 -> {
                child(laugh, "Haha! You look funny!")
                chatNpc(neutral, "I'm sure you look just as funny to the surface-dweller!")
            }
            1 -> {
                child(laugh, "You smell!")
                chatNpc(angry, "There's no need to be rude!")
                chatNpc(sad, "I'm sorry about that, surface-dweller.")
            }
            2 -> {
                child(quiz, "Are you really from the surface?")
                chatPlayer(happy, "Yes I am.")
            }
            else -> {
                child(angry, "Shan't!")
                chatNpc(angry, "Well don't blame me if he doesn't want to talk to you either!")
                chatNpc(sad, "Sorry about that.")
            }
        }
    }

    private suspend fun Dialogue.child(
        mesanim: MesAnimType,
        text: String,
    ) {
        chatNpcSpecific("Cave goblin child", CHILDREN.first(), mesanim, text)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Cave_goblin_child
    // TODO(content): [quest:another_slice_of_ham] "Did you help Zanik save the city?"
    private suspend fun Dialogue.caveGoblinChild() {
        when (access.random.of(4)) {
            0 -> {
                chatNpc(shifty, "Shh! Don't tell anyone!")
                chatPlayer(confused, "Don't tell anyone what?")
                chatNpc(angry, "SHHH!")
            }
            1 -> {
                chatNpc(quiz, "Are you a surface-dweller?")
                chatPlayer(neutral, "Yes...")
                chatNpc(laugh, "Haha! You look funny! All tall and skinny with tiny eyes!")
            }
            2 -> {
                chatNpc(shocked, "Help! Help! The surface people are attacking!")
                chatPlayer(worried, "It's alright, I'm friendly!")
            }
            else -> chatNpc(worried, "Sorry, I'm not meant to talk to strangers.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Nipper
    // https://oldschool.runescape.wiki/w/Transcript:Tyke
    // https://oldschool.runescape.wiki/w/Transcript:Young_%27un
    private suspend fun Dialogue.baby() {
        val line =
            when (access.random.of(5)) {
                0 -> "Ga ga?"
                1 -> "Heeheehee!"
                2 -> "*cough*"
                3 -> "*gurgle*"
                else -> "*hic*"
            }
        chatNpc(happy, line)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gourmet
    // TODO(content): [system:dorgesh_kaan_market] selling surface food to the gourmets
    private suspend fun Dialogue.gourmet() {
        chatNpc(
            happy,
            "Ooh, a surface-dweller! Have you got any exotic surface foods to sell?",
        )
        val option =
            choice3("Not at the moment.", 1, "Yes!", 2, "What kind of foods do you like?", 3)
        when (option) {
            1 -> {
                chatPlayer(neutral, "Not at the moment.")
                chatNpc(
                    sad,
                    "A pity. Dorgeshuun food seems so dull now I've tasted surface food.",
                )
            }
            2 -> {
                chatPlayer(happy, "Yes!")
                chatNpc(happy, "Splendid! Show me what you've got!")
            }
            else -> {
                chatPlayer(quiz, "What kinds of food do you like?")
                chatNpc(
                    happy,
                    "All kinds! I don't want to keep eating the same food, even if I like it. I " +
                        "want to try new tastes!",
                )
                chatNpc(happy, "Why not try something on me and see if I like it?")
            }
        }
    }

    private companion object {
        const val STAND_UP = "Then how do you stand up?"
        const val NEED_BONES = "What do you need bones for?"
        const val KIND_OF_BONES = "What kind of bones do you need?"
        const val BESIDES_BONES = "Will you buy anything besides bones?"
        const val GOODBYE = "Goodbye."

        val BARLAK_TOPICS = listOf(NEED_BONES, KIND_OF_BONES, BESIDES_BONES)

        val CHILDREN =
            listOf(
                "npc.dorgesh_male_child1",
                "npc.dorgesh_male_child1_ball",
                "npc.dorgesh_male_child2",
                "npc.dorgesh_male_child2_ball",
                "npc.dorgesh_male_child3",
                "npc.dorgesh_male_child3_ball",
                "npc.dorgesh_male_child4",
                "npc.dorgesh_male_child4_ball",
                "npc.dorgesh_female_child1",
                "npc.dorgesh_female_child1_ball",
                "npc.dorgesh_female_child2",
                "npc.dorgesh_female_child2_ball",
                "npc.dorgesh_female_child3",
                "npc.dorgesh_female_child3_ball",
                "npc.dorgesh_female_child4",
                "npc.dorgesh_female_child4_ball",
            )

        val BABIES =
            listOf(
                "npc.dorgesh_baby1",
                "npc.dorgesh_baby2",
                "npc.dorgesh_baby3",
                "npc.dorgesh_baby4",
            )

        val GOURMETS =
            listOf(
                "npc.dorgesh_gourmet_male1",
                "npc.dorgesh_gourmet_male2",
                "npc.dorgesh_gourmet_female1",
                "npc.dorgesh_gourmet_female2",
            )
    }
}
