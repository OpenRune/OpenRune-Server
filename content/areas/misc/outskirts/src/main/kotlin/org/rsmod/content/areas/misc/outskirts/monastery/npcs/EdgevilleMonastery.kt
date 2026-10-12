package org.rsmod.content.areas.misc.outskirts.monastery.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.basePrayerLvl
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class EdgevilleMonastery : PluginScript() {
    private var Player.joinedOrder by intVarBit("varbit.monastery_enter")

    override fun ScriptContext.startup() {
        onOpNpc1("npc.abbot_langley") { startDialogue(it.npc) { abbotLangley() } }
        onOpNpc1("npc.monk") { startDialogue(it.npc) { monk() } }
        onOpNpc1("npc.brother_jered") { startDialogue(it.npc) { brotherJered() } }
        onOpNpc1("npc.brother_althric") { startDialogue(it.npc) { brotherAlthric() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Abbot_Langley
    // TODO(content): [system:spirit_shield_sigils] "Can you help with spirit shields?" sigil
    // removal and attachment
    // TODO(content): [system:treasure_trails] hard and master clue dialogue
    // TODO(content): [system:wise_old_man_tasks] delivering a letter from the Wise Old Man
    // TODO(content): [miniquest:curse_of_the_empty_lord] corrupted helm exchange
    private suspend fun Dialogue.abbotLangley() {
        chatNpc(happy, "Greetings traveller.")
        val option =
            if (player.joinedOrder == 0) {
                choice3(
                    "Can you heal me? I'm injured.",
                    1,
                    "Isn't this place built a bit out of the way?",
                    2,
                    "How do I get further into the monastery?",
                    3,
                )
            } else {
                choice2(
                    "Can you heal me? I'm injured.",
                    1,
                    "Isn't this place built a bit out of the way?",
                    2,
                )
            }
        when (option) {
            1 -> {
                chatPlayer(sad, "Can you heal me? I'm injured.")
                chatNpc(happy, "Ok.")
                mesbox("Abbot Langley places his hands on your head. You feel a little better.")
                player.statHeal(HITPOINTS, constant = 4, percent = 12)
            }
            2 -> outOfTheWay()
            3 -> {
                chatPlayer(quiz, "How do I get further into the monastery?")
                chatNpc(
                    neutral,
                    "I'm sorry but only members of our order are allowed in the second level of " +
                        "the monastery.",
                )
                joinOrder()
            }
        }
    }

    private suspend fun Dialogue.joinOrder() {
        val option = choice2("Well can I join your order?", 1, "Oh, sorry.", 2)
        if (option == 2) {
            chatPlayer(sad, "Oh, sorry.")
            return
        }
        chatPlayer(quiz, "Well can I join your order?")
        if (player.basePrayerLvl < ORDER_PRAYER_LEVEL) {
            chatNpc(
                sad,
                "No. I am sorry, but I feel you are not devout enough. You need a prayer level of " +
                    "31 to join the order.",
            )
            return
        }
        player.joinedOrder = 1
        chatNpc(happy, "Ok, I see you are someone suitable for our order. You may join.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Monk (Edgeville Monastery, members world)
    private suspend fun Dialogue.monk() {
        chatNpc(happy, "Greetings traveller.")
        val option =
            choice3(
                "Can you heal me? I'm injured.",
                1,
                "Isn't this place built a bit out of the way?",
                2,
                "How do I get further into the monastery?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(sad, "Can you heal me? I'm injured.")
                chatNpc(happy, "Ok.")
                player.statHeal(HITPOINTS, constant = 2, percent = 20)
            }
            2 -> outOfTheWay()
            3 -> {
                chatPlayer(quiz, "How do I get further into the monastery?")
                chatNpc(
                    neutral,
                    "You'll need to talk to Abbot Langley about that. He's usually to be found " +
                        "walking the halls of the monastery.",
                )
            }
        }
    }

    private suspend fun Dialogue.outOfTheWay() {
        chatPlayer(quiz, "Isn't this place built a bit out of the way?")
        chatNpc(
            happy,
            "We like it that way actually! We get disturbed less. We still get rather a large " +
                "amount of travellers looking for sanctuary and healing here as it is!",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Brother_Jered
    // TODO(content): [system:skillcapes] Skillcape of Prayer sale at 99 Prayer
    // TODO(content): [verify:in_game] which greeting wins when the player has 99 Prayer and an
    // unblessed symbol
    private suspend fun Dialogue.brotherJered() {
        val option =
            choice2(
                "What can you do to help a bold adventurer like myself?",
                1,
                "Praise be to Saradomin!",
                2,
            )
        if (option == 2) {
            chatPlayer(happy, "Praise be to Saradomin!")
            chatNpc(happy, "Yes! Praise he who brings life to this world.")
            return
        }
        jeredHelp()
    }

    private suspend fun Dialogue.jeredHelp() {
        chatPlayer(quiz, "What can you do to help a bold adventurer like myself?")
        if (UNBLESSED_SYMBOL in player.inv) {
            chatNpc(
                happy,
                "Well I can bless that star of Saradomin you have, or I could tell you about the " +
                    "Skillcape of Prayer!",
            )
            val option =
                choice3(
                    "Bless star, please.",
                    1,
                    "Tell me about the Skillcape of Prayer.",
                    2,
                    "None of those, thanks.",
                    3,
                )
            when (option) {
                1 -> blessSymbol()
                2 -> jeredSkillcape()
                3 -> chatPlayer(neutral, "No thank you.")
            }
            return
        }
        chatNpc(happy, "I can tell you about holy symbols or the Skillcape of Prayer.")
        val option =
            choice2(
                "Tell me about holy symbols.",
                1,
                "Tell me about the Skillcape of Prayer.",
                2,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Tell me about holy symbols.")
                chatNpc(
                    happy,
                    "If you have a silver star, which is the holy symbol of Saradomin, then I can " +
                        "bless it. Then if you are wearing it, it will help you when you are " +
                        "praying.",
                )
            }
            2 -> jeredSkillcape()
        }
    }

    private suspend fun Dialogue.blessSymbol() {
        chatPlayer(happy, "Yes please.")
        if (access.invReplace(access.inv, UNBLESSED_SYMBOL, 1, HOLY_SYMBOL).failure) {
            return
        }
        mesbox("You give Jered the symbol.")
        mesbox("Jered closes his eyes and places his hand on the symbol. He softly chants.")
        mesbox("Jered passes you the holy symbol.")
    }

    private suspend fun Dialogue.jeredSkillcape() {
        chatPlayer(quiz, "Tell me about the Skillcape of Prayer.")
        chatNpc(
            happy,
            "The Skillcape of Prayer is the hardest of all the skillcapes to get; it requires " +
                "much devotion to acquire but also imbues the wearer with the ability to briefly " +
                "fly!",
        )
        chatNpc(
            happy,
            "The Cape of Prayer also increases the amount of Prayer points restored from " +
                "drinking potions when it is equipped.",
        )
        val option =
            choice2(
                "Yes, what else can you do for a bold adventurer like myself?",
                1,
                "No, thank you.",
                2,
                title = "Is there something else I can do for you?",
            )
        when (option) {
            1 -> jeredHelp()
            2 -> chatPlayer(neutral, "No thank you.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Brother_Althric
    // TODO(content): [quest:garden_of_tranquillity] quest help dialogue
    // TODO(content): [quest:fairytale_i_growing_pains] quest help dialogue
    private suspend fun Dialogue.brotherAlthric() {
        chatPlayer(happy, "Very nice rosebushes you have here.")
        chatNpc(
            happy,
            "Yes, it has taken me many long hours in this garden to bring them to this state of " +
                "near-perfection.",
        )
    }

    private companion object {
        const val HITPOINTS = "stat.hitpoints"
        const val ORDER_PRAYER_LEVEL = 31
        const val UNBLESSED_SYMBOL = "obj.stringstar"
        const val HOLY_SYMBOL = "obj.blessedstar"
    }
}
