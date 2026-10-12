package org.rsmod.content.areas.city.falador.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class MalignusMortiferGapNpc : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.elemental_wizard_boss") { startDialogue(it.npc) { malignius() } }
    }

    private fun Dialogue.completed(quest: String): Boolean =
        QuestRequirements.hasCompleted(player, quest)

    // https://oldschool.runescape.wiki/w/Transcript:Malignius_Mortifer
    // TODO(content): [quest:fairytale_i_growing_pains] "About those enchanted secateurs again..."
    // TODO(content): [quest:swan_song] the brown apron, bone seeds and skeletal magi options
    // TODO(content): [verify:in_game] the first menu only holds "... MORE ..." before those
    //  quests; this goes straight to the "... MORE ..." menu
    private suspend fun Dialogue.malignius() {
        chatNpc(
            neutral,
            "So, ${player.displayName}, your curiosity leads you to speak to me?",
        )
        val option =
            choice4(
                "Who are you and what are you doing here?",
                1,
                "Can you teach me something about magic?",
                2,
                "Where can I get clothes like those?",
                3,
                "Actually, I don't want to talk to you.",
                4,
                title = "What would you like to say?",
            )
        when (option) {
            1 -> whoAreYou()
            2 -> teachMagic()
            3 -> clothes()
            else -> {
                chatPlayer(neutral, "Actually, I don't want to talk to you.")
                chatNpc(angry, "Bah! Then go away!")
            }
        }
    }

    private suspend fun Dialogue.whoAreYou() {
        chatPlayer(quiz, "Who are you and what are you doing here?")
        chatNpc(
            neutral,
            "I am the great Malignius Mortifer, wielder of strange and terrible powers. These " +
                "lowly followers of mine are dedicated students of the magical arts. Their " +
                "business is to follow me and learn all they can.",
        )
        chatPlayer(neutral, "They don't look very tough.")
        chatNpc(
            neutral,
            "You may believe that, but even if you strike one down, another will rise up within " +
                "minutes.",
        )
        chatPlayer(neutral, "Yeah, right.")
        chatNpc(
            neutral,
            "Each of my followers is a master of his chosen element. His life becomes bound to " +
                "that element in a way you could not hope to understand.",
        )
        chatPlayer(quiz, "And what do you do?")
        chatNpc(
            neutral,
            "I am mastering a branch of magic that few dare to attempt: Necromancy! The fools in " +
                "the Guild of Wizards shun anyone who practices this art, but there are a few in " +
                "this land who know the rudiments.",
        )
        if (completed(ZOGRE_FLESH_EATERS)) {
            chatNpc(
                neutral,
                "Grayzag, Invrigar, Sithik... Even Melzar studied the methods of necromancy, " +
                    "until an accident affected his mind. Now his spells tend to result in...",
            )
        } else {
            chatNpc(
                neutral,
                "Grayzag and Invrigar... Even Melzar studied the methods of necromancy, until an " +
                    "accident affected his mind. Now his spells tend to result in...",
            )
        }
        chatNpc(
            laugh,
            "... well, let us simply say that he does NOT raise armies of undead minions.",
        )
        necromancyMenu()
    }

    // TODO(content): [system:god_alignment] the Guthix, Saradomin and Zamorak equipment answers to
    //  "Even a mighty hero like me?"
    private suspend fun Dialogue.necromancyMenu() {
        while (true) {
            val option =
                choice4(
                    "Is this like Summoning?",
                    1,
                    "What happened to Melzar?",
                    2,
                    "Why do you keep making fungi appear?",
                    3,
                    "Thanks, I'll be off now.",
                    4,
                    title = "What would you like to say?",
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Is this like Summoning? Can you teach me?")
                    chatNpc(
                        neutral,
                        "The obscure magical art of necromancy has nothing to do with Summoning.",
                    )
                    chatNpc(
                        neutral,
                        "Through necromancy, I may resurrect the bodies of those who have " +
                            "fallen, and bend them to my will, and even command them to tell me " +
                            "hidden knowledge.",
                    )
                    chatNpc(
                        angry,
                        "I shall not pass on my secrets lightly to any passer-by!",
                    )
                    chatPlayer(happy, "Even a mighty hero like me?")
                    chatNpc(
                        neutral,
                        "Perhaps one day you shall learn ways to summon creatures to your side, " +
                            "but my own work is of a different sort.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "What happened to Melzar?")
                    chatNpc(
                        neutral,
                        "The forces required to wield power over the dead are very difficult to " +
                            "control. Melzar's concentration slipped, and his spell rebounded " +
                            "upon him.",
                    )
                    chatNpc(
                        laugh,
                        "Although he did not die, his mind broke. Now he dwells amidst the " +
                            "beasts that fill his strange home, and when he attempts to do magic " +
                            "he gets... amusing results.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Why do you keep making fungi appear?")
                    chatNpc(
                        neutral,
                        "Necromancy is... still not fully understood. So few people dare to " +
                            "study it. Although my spells usually raise whichever undead " +
                            "creature I desire, they sometimes misfire. As Melzar has learnt!",
                    )
                }
                else -> {
                    chatPlayer(neutral, "Thanks, I'll be off now.")
                    chatNpc(
                        neutral,
                        "I am sure your curiosity will eventually draw you to speak to me again, " +
                            "${player.displayName}.",
                    )
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.teachMagic() {
        chatPlayer(quiz, "Can you teach me something about magic?")
        if (!completed(RUNE_MYSTERIES)) {
            chatNpc(
                neutral,
                "I see no glimmer of understanding in your eyes; my time would be wasted trying " +
                    "to teach you of the subtle arts.",
            )
            chatNpc(
                neutral,
                "If you are truly interested in magic, perhaps you should go and speak to the " +
                    "Duke of Lumbridge, who lives east of this place. Perchance he will have a " +
                    "mission for you that will teach you something.",
            )
            return
        }
        chatNpc(
            neutral,
            "Ah, you are an inquisitive fellow. I shall speak of the great Wizards' Tower, " +
                "destroyed by fire many years ago...",
        )
        chatNpc(
            neutral,
            "Many say it was the greatest building in the history of Gielinor, a magnificent " +
                "monument to human ingenuity.",
        )
        chatNpc(
            neutral,
            "Yet when humans are offered great power, they so often buy it at the cost of their " +
                "principles.",
        )
        chatNpc(
            angry,
            "Wizards who claimed allegiance to Saradomin began to insist that magic be " +
                "restricted to the few they deemed 'worthy' of such powers.",
        )
        chatNpc(
            angry,
            "Before long, those who did not share their fatuous obsession with Saradomin were " +
                "excluded from the Tower completely. This state of affairs could not continue.",
        )
        chatNpc(
            neutral,
            "One dark night, a brave band of magi loyal to the great Lord Zamorak stormed the " +
                "Tower. Despite heavy losses, they fought their way up the levels until they " +
                "reached the Great Library.",
        )
        chatNpc(
            neutral,
            "Alas, they were trapped and could not steal the precious records. But before they " +
                "could be defeated, the bravest of them took it upon himself to blast the " +
                "writings into nothingness, destroying the library.",
        )
        chatNpc(
            neutral,
            "The ensuing fire raced through the tower, and none escaped its fury. Some say that " +
                "Lord Zamorak himself fanned the flames to ensure the destruction of his enemies!",
        )
        if (completed(ENTER_THE_ABYSS)) {
            chatNpc(
                happy,
                "Much of that knowledge was lost, and some has never yet been rediscovered. " +
                    "Still, your efforts on behalf of my colleagues have not been wasted! Once " +
                    "again, the forces of Zamorak can create unlimited runestones!",
            )
            chatNpc(laugh, "May the world cower in its boots!")
            chatPlayer(neutral, "I see...")
            chatNpc(neutral, "I bid you farewell for now.")
            return
        }
        chatNpc(
            neutral,
            "Much of that knowledge was lost, and some has never yet been rediscovered. Still, " +
                "the power of magic remains in the world, and we who dedicate our lives to its " +
                "study may yet perform great works...",
        )
        chatPlayer(neutral, "Thanks, I'll be going now.")
        chatNpc(neutral, "As you wish.")
    }

    // TODO(content): [system:transformations] turning the player into a mushroom before "MMMmmph!"
    private suspend fun Dialogue.clothes() {
        chatPlayer(quiz, "Where can I get clothes like those?")
        chatNpc(
            angry,
            "Bah! Our garments are an outward sign of our dominance of the magical arts. You " +
                "cannot simply buy them in a shop!",
        )
        chatPlayer(quiz, "What happens if I kill you and take them?")
        chatNpc(angry, "Try it and see!")
        chatPlayer(
            quiz,
            "How about if you teach me enough about magic so I can wear those clothes too?",
        )
        chatNpc(
            angry,
            "How about if I turn you into a mushroom to make you stop bothering me?",
        )
        chatPlayer(shocked, "MMMmmph!")
    }

    private companion object {
        const val ZOGRE_FLESH_EATERS = "quest_zogreflesheaters"
        const val RUNE_MYSTERIES = "quest_runemysteries"
        const val ENTER_THE_ABYSS = "miniquest_entertheabyss"
    }
}
