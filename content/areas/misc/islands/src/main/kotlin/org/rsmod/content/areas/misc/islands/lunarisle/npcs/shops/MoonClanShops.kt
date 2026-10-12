package org.rsmod.content.areas.misc.islands.lunarisle.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.content.areas.misc.islands.lunarisle.npcs.hasSealOfPassage
import org.rsmod.content.areas.misc.islands.lunarisle.npcs.refuseWithoutSeal
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class MoonClanShops @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(BABA_YAGA) { talk(it.npc) { babaYaga(it.npc) } }
        onOpNpc3(BABA_YAGA) { trade(it.npc, BABA_YAGA_SHOP, BABA_YAGA_INV) }
        onOpNpc1(RIMAE) { talk(it.npc) { rimaeSirsalis(it.npc) } }
        onOpNpc3(RIMAE) { trade(it.npc, CLOTHES_SHOP, CLOTHES_INV) }
        onOpNpc1(MELANA) { talk(it.npc) { melanaMoonlander(it.npc) } }
        onOpNpc3(MELANA) { trade(it.npc, GENERAL_STORE, GENERAL_INV) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc, dialogue: suspend Dialogue.() -> Unit) {
        if (!hasSealOfPassage()) {
            startDialogue(npc) { refuseWithoutSeal() }
            return
        }
        startDialogue(npc) { dialogue() }
    }

    private suspend fun ProtectedAccess.trade(npc: Npc, title: String, inv: String) {
        if (!hasSealOfPassage()) {
            startDialogue(npc) { refuseWithoutSeal() }
            return
        }
        shops.open(player, npc, title, inv)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Baba_Yaga
    // TODO(content): [quest:lunar_diplomacy] the pre-quest stock before the quest is complete;
    // the server data for inv.lunar_runeshop holds a crossbow shop's stock, not runes
    private suspend fun Dialogue.babaYaga(npc: Npc) {
        chatPlayer(happy, "Hello there.")
        val questDone = QuestRequirements.hasCompleted(player, LUNAR_DIPLOMACY)
        if (questDone) {
            chatNpc(
                happy,
                "Ah! ${player.displayName}, I'm glad to see you've returned. How can I help?",
            )
        } else {
            chatNpc(happy, "Ah, a stranger to our island. How can I help?")
        }
        val canLearnOurania = questDone && vars[OURANIA_TELEPORT] == 0
        val option =
            if (canLearnOurania) {
                choice4(
                    "Have you got anything to trade?",
                    1,
                    "It's a very interesting house you have here.",
                    2,
                    "There is a lunar spell I can't use in my spell book - can you help me?",
                    3,
                    "I'm good, thanks, bye.",
                    4,
                )
            } else {
                choice3(
                    "Have you got anything to trade?",
                    1,
                    "It's a very interesting house you have here.",
                    2,
                    "I'm good, thanks, bye.",
                    4,
                )
            }
        when (option) {
            1 -> shops.open(player, npc, BABA_YAGA_SHOP, BABA_YAGA_INV)
            2 -> {
                chatPlayer(
                    quiz,
                    "It's a very interesting house you have here. Does he have a name?",
                )
                chatNpc(happy, "Why of course. It's Berty.")
                chatPlayer(confused, "Berty? Berty the Chicken leg house?")
                chatNpc(happy, "Yes.")
                chatPlayer(quiz, "May I ask why?")
                chatNpc(
                    laugh,
                    "It just has that certain ring to it, don't you think? Beeerteeee!",
                )
                chatPlayer(confused, "You're ins...")
                chatNpc(madlaugh, "Insane? Very.")
            }
            3 -> ourania()
            4 -> chatPlayer(happy, "I'm good thanks, bye.")
        }
    }

    // TODO(content): [verify:in_game] 1 is the unlocked value of varbit.rc_zmi_lunar_spell
    private suspend fun Dialogue.ourania() {
        chatPlayer(
            quiz,
            "There's a lunar spell in my spell book that I can't use. Could you...?",
        )
        chatNpc(
            happy,
            "Help you? Certainly. It's one I've just rediscovered! I can teach it to you if you " +
                "like, but you have to listen to a story first.",
        )
        val listen =
            choice2(
                "Okay, I don't mind hearing stories.",
                true,
                "I don't really have time right now.",
                false,
            )
        if (!listen) {
            chatPlayer(neutral, "I don't really have ti-")
            chatNpc(bored, "*Sigh* Very well. Another time, perhaps.")
            chatPlayer(
                confused,
                "Why do you ask questions if you're going to read my mind anyway?",
            )
            chatNpc(neutral, "I was being polite, silly foreigner.")
            return
        }
        chatPlayer(happy, "Okay, I don't mind hearing sto-")
        chatNpc(happy, "Wonderful!")
        chatNpc(
            neutral,
            "Well, I was going through some ancient documents recently, expanding my knowledge " +
                "of our great ancestors...",
        )
        chatNpc(
            neutral,
            "Hmm, actually, I think I had better go further back, so you will understand.",
        )
        chatNpc(quiz, "You know of our ancient war with the Fremennik that you recently healed?")
        chatPlayer(happy, "Of course! I am a great diplomat, aren't I?")
        chatNpc(bored, "Some diplomat. Did you even discover WHY we were at war for so long?")
        chatPlayer(
            confused,
            "Errr... You couldn't see eye to eye about magic, or something like that.",
        )
        chatNpc(neutral, "Not too far off; it goes deeper than that.")
        chatNpc(
            neutral,
            "The Fremennik were actually the first to discover how to make runes, but there was " +
                "disagreement within the tribe about the implications of making such powerful " +
                "objects.",
        )
        chatNpc(
            neutral,
            "Most Fremennik felt we were stealing power from the gods. They could not see past " +
                "that to the great things we could do with such power, so we split from them " +
                "and became the Moon Clan. Our ancient leader, who first",
        )
        chatNpc(
            neutral,
            "discovered the rune essence mine here on Lunar Isle, travelled far and wide " +
                "searching for more places of power. He and his followers built altars all over " +
                "Gielinor - which you use today to create runes - and at their",
        )
        chatNpc(
            neutral,
            "height they made runes by the crateful. They had perfected the process and even " +
                "advanced it, creating altars that could create any type of rune regardless of " +
                "the crafter's ability. But the strife between the Moon",
        )
        chatNpc(
            sad,
            "Clan and the Fremennik heightened. The Fremennik had grown strong in body, while " +
                "our clan had grown stong in mind. They overpowered our tribe and we retreated " +
                "to Lunar Isle, where we had lived ever since.",
        )
        chatNpc(
            sad,
            "The Fremennik then swept over Gielinor destroying the altars we had made, in an " +
                "attempt to remove all trace of magic from the land.",
        )
        chatPlayer(bored, "That's all very interesting, but what's it got to do with this spell?")
        chatNpc(angry, "I'm getting to that!")
        chatNpc(
            neutral,
            "The Fremennik were not as thorough as they thought. They destroyed only the " +
                "entrances to the altars, for the altars themselves were on a different plane, " +
                "but they were successful in wiping out our ability to create",
        )
        chatNpc(
            neutral,
            "runes. That ability has only recently been rediscovered on the mainland, so I hear.",
        )
        chatNpc(
            neutral,
            "Our people were left isolated on Lunar Isle but we used it to our advantage. Our " +
                "control over Magic advanced beyond the need for runes and all knowledge of the " +
                "altar locations passed from memory. Fortunately,",
        )
        chatNpc(
            neutral,
            "some records survived... The manuscript I discovered contains the teleportation " +
                "spell to an advanced altar called Ourania. It's an altar that allows the user, " +
                "regardless of their Runecrafting ability, to craft any",
        )
        chatNpc(
            neutral,
            "type of rune upon it. Ourania is a powerful altar indeed. What state it is in after " +
                "so many millennia, I know not, but it's interesting all the same.",
        )
        chatPlayer(quiz, "So, will you teach me the spell?")
        chatNpc(happy, "Certainly.")
        chatNpc(shifty, "*Baby Yaga whispers in your ear*")
        vars[OURANIA_TELEPORT] = 1
        chatNpc(happy, "And now you know the teleportation spell, off with you! Go explore.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Rimae_Sirsalis
    private suspend fun Dialogue.rimaeSirsalis(npc: Npc) {
        chatPlayer(happy, "Hello there.")
        chatNpc(happy, "Welcome to the clothes store. How might I help you?")
        val option =
            choice3(
                "What can you sell me?",
                1,
                "It's a very interesting island you have here.",
                2,
                "I'm good thanks, bye.",
                3,
            )
        when (option) {
            1 -> shops.open(player, npc, CLOTHES_SHOP, CLOTHES_INV)
            2 -> {
                chatPlayer(happy, "It's a very interesting island you have here.")
                chatNpc(
                    happy,
                    "Why thank you. It's been our haven for a great many generations.",
                )
                chatPlayer(quiz, "Everything seems to have a magical feel to it.")
                chatNpc(
                    neutral,
                    "Of course. We integrate magic into all areas of our lives. It is a part of " +
                        "everyone, so why deny it? It's best to make the most of this innate " +
                        "gift we have all been given.",
                )
                chatPlayer(quiz, "What sort of things do you use your magic for?")
                chatNpc(
                    neutral,
                    "Take a look around. We use it in our day to day lives, from making a cup of " +
                        "tea, to travelling around the island. You see our ancestors were the " +
                        "ones that found the first rune essence and put it to use! The various",
                )
                chatNpc(
                    neutral,
                    "factions were eventually created, with people having different ideas on " +
                        "how the essence should be used (or not in some cases!)",
                )
                chatPlayer(quiz, "What has kept you so secluded on this island?")
                chatNpc(
                    neutral,
                    "It may be a bit beyond you, but although magic comes from within, we are " +
                        "all very strongly linked to the moon and the effects it has on us " +
                        "cannot be denied!",
                )
                chatPlayer(confused, "It can't?")
                chatNpc(
                    neutral,
                    "Of course not. This very island has a great link to our moon, which helps " +
                        "us understand ourselves - especially our dreams, which is the path to " +
                        "understanding magic.",
                )
                chatPlayer(neutral, "I think I will just have to take your word on that.")
            }
            3 -> chatPlayer(happy, "I'm good thanks, bye.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Melana_Moonlander
    private suspend fun Dialogue.melanaMoonlander(npc: Npc) {
        chatPlayer(happy, "Hello there.")
        chatNpc(happy, "Hi. Welcome to the general store. How might I help you?")
        val option =
            choice3(
                "What can you sell me?",
                1,
                "I have a question...",
                2,
                "I'm good thanks, bye.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What can you sell me?")
                shops.open(player, npc, GENERAL_STORE, GENERAL_INV)
            }
            2 -> {
                chatPlayer(quiz, "I have a question...")
                chatNpc(neutral, "About magic of course.")
                chatPlayer(confused, "Sorry?")
                chatNpc(
                    neutral,
                    "I said about magic of course. You know, in response to your question.",
                )
                chatPlayer(confused, "But I didn't ask anything yet.")
                chatNpc(happy, "Yes, but you were thinking of asking me how I was floating.")
                chatPlayer(shocked, "That's true!")
                chatPlayer(quiz, "How could you possibly know that?")
                chatNpc(neutral, "Don't you realise we can read your mind.")
                chatPlayer(quiz, "Oh, of course. How do you manage to do that?")
                chatNpc(
                    neutral,
                    "It's quite simple, everyone has a resonance that is responded to by the " +
                        "moon. This resonance changes depending on what we are thinking. You can " +
                        "tune yourself in to listen to this resonance with practice - it's",
                )
                chatNpc(
                    neutral,
                    "a life long quest for the members of the Moon Clan, but its especially easy " +
                        "to read with outsiders like yourself, as you are far louder and " +
                        "unguarded.",
                )
                chatPlayer(worried, "I see. I best be careful with what I think of then.")
                chatPlayer(silent, "...")
                chatPlayer(silent, "...")
                chatNpc(angry, "That's disgusting!")
                chatPlayer(sad, "Sorry.")
            }
            3 -> chatPlayer(happy, "I'm good thanks, bye.")
        }
    }

    private companion object {
        const val BABA_YAGA = "npc.lunar_moonclan_baba_yaga"
        const val RIMAE = "npc.lunar_moonclan_monk5"
        const val MELANA = "npc.lunar_moonclan_monk3"
        const val BABA_YAGA_SHOP = "Baba Yaga's Magic Shop."
        const val BABA_YAGA_INV = "inv.lunar_runeshop_compl"
        const val CLOTHES_SHOP = "Moon Clan Fine Clothes."
        const val CLOTHES_INV = "inv.lunar_clotheshop"
        const val GENERAL_STORE = "Moon Clan General Store."
        const val GENERAL_INV = "inv.lunar_general"
        const val OURANIA_TELEPORT = "varbit.rc_zmi_lunar_spell"
        const val LUNAR_DIPLOMACY = "quest_lunardiplomacy"
    }
}
