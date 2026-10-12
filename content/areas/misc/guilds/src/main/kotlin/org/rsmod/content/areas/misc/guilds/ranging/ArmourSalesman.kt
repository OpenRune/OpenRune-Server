package org.rsmod.content.areas.misc.guilds.ranging

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.baseRangedLvl
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.content.areas.misc.guilds.SKILLCAPE_LEVEL
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:skillcapes] the free hood with a shop-bought cape and the trimmed cape
//  stock (inv.ranging_guild_armourshop_skillcape_trimmed)
class ArmourSalesman @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(ARMOUR_SALESMAN) { startDialogue(it.npc) { armourSalesman(it.npc) } }
        onOpNpc3(ARMOUR_SALESMAN) { player.openAppendages(it.npc) }
    }

    private fun Player.openAppendages(npc: Npc) {
        val inv = if (baseRangedLvl >= SKILLCAPE_LEVEL) SKILLCAPE_STOCK else STOCK
        shops.open(this, npc, "Aaron's Archery Appendages.", inv)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Armour_salesman
    private suspend fun Dialogue.armourSalesman(npc: Npc) {
        chatPlayer(happy, "Good day to you.")
        chatNpc(happy, "And to you. Can I help you?")
        while (true) {
            val option =
                choice4(
                    "What do you do here?",
                    1,
                    "I'd like to see what you sell.",
                    2,
                    "Can you tell me about your cape?",
                    3,
                    "I've seen enough, thanks.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What do you do here?")
                    chatNpc(
                        neutral,
                        "I am a supplier of leather armours and accessories. Ask and I will tell " +
                            "you what I know.",
                    )
                    supplies()
                    return
                }
                2 -> {
                    chatPlayer(happy, "I'd like to see what you sell.")
                    chatNpc(happy, "Indeed, cast your eyes on my wares, adventurer.")
                    player.openAppendages(npc)
                    return
                }
                3 -> cape()
                4 -> {
                    seenEnough()
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.supplies() {
        while (true) {
            val option =
                choice3(
                    "Tell me about your armours.",
                    1,
                    "Tell me about your accessories.",
                    2,
                    "I've seen enough, thanks.",
                    3,
                )
            when (option) {
                1 -> armours()
                2 -> accessories()
                3 -> {
                    seenEnough()
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.armours() {
        chatPlayer(quiz, "Tell me about your armours.")
        chatNpc(neutral, "I have normal, studded and hard types.")
        while (true) {
            val option =
                choice4(
                    "Tell me about normal leather.",
                    1,
                    "What's studded leather?",
                    2,
                    "What's hard leather?",
                    3,
                    "Enough about armour.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Tell me about normal leather.")
                    chatNpc(
                        neutral,
                        "Indeed, leather armour is excellent for archers. It's supple and not " +
                            "very heavy.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "What's studded leather?")
                    chatNpc(
                        neutral,
                        "Ah, now that's leather covered with studs. It's more protective than " +
                            "ordinary leather.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "What's hard leather?")
                    chatNpc(
                        neutral,
                        "Hard leather is specially treated using oils and drying methods to " +
                            "create a hard-wearing armour.",
                    )
                }
                4 -> {
                    chatPlayer(neutral, "Enough about armour.")
                    chatNpc(neutral, "As you wish, traveller.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.accessories() {
        chatPlayer(quiz, "Tell me about your accessories.")
        chatNpc(
            happy,
            "Ah yes we have a new range of accessories in stock. Essential items for an archer " +
                "like you.",
        )
        chatNpc(neutral, "We have vambraces, chaps, cowls and coifs.")
        while (true) {
            val option =
                choice5(
                    "Tell me about vambraces.",
                    1,
                    "Tell me about chaps.",
                    2,
                    "Tell me about cowls.",
                    3,
                    "Tell me about coifs.",
                    4,
                    "Enough about accessories.",
                    5,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Tell me about vambraces.")
                    chatNpc(neutral, "Ah yes, vambraces. These useful items are for your arms.")
                    chatNpc(
                        neutral,
                        "A protective sheath that favours the bow and arrow. An essential " +
                            "purchase.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Tell me about chaps.")
                    chatNpc(
                        neutral,
                        "Chaps have two functions: firstly to protect your legs, and secondly " +
                            "for ease of reloading arrows.",
                    )
                    chatNpc(neutral, "I can highly recommend these to you for quick archery.")
                }
                3 -> {
                    chatPlayer(quiz, "Tell me about cowls.")
                    chatNpc(
                        neutral,
                        "The cowl is a soft leather hat, ideal for protection with " +
                            "maneuverability.",
                    )
                    chatNpc(neutral, "These are highly favoured with our guards.")
                }
                4 -> {
                    chatPlayer(quiz, "Tell me about coifs.")
                    chatNpc(
                        neutral,
                        "The coif is a specialized cowl, that has extra chain protection to keep " +
                            "your neck and shoulders safe.",
                    )
                    chatNpc(neutral, "An excellent addition to our range, traveller.")
                }
                5 -> {
                    chatPlayer(neutral, "Enough about accessories.")
                    chatNpc(neutral, "As you wish.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.cape() {
        chatPlayer(quiz, "Can you tell me about your cape?")
        chatNpc(
            happy,
            "Certainly! Skillcapes are a symbol of achievement. Only people who have mastered a " +
                "skill and reached level 99 can get their hands on them and gain the benefits " +
                "they carry.",
        )
        chatNpc(
            neutral,
            "The Cape of Ranging has been modified by Ava to function as one of her accumulator " +
                "devices. Is there anything else I can help you with?",
        )
    }

    private suspend fun Dialogue.seenEnough() {
        chatPlayer(neutral, "I've seen enough, thanks.")
        chatNpc(happy, "Very good, adventurer.")
    }

    private companion object {
        const val ARMOUR_SALESMAN = "npc.ranging_guild_armour_salesman"
        const val STOCK = "inv.ranging_guild_armourshop"
        const val SKILLCAPE_STOCK = "inv.ranging_guild_armourshop_skillcape"
    }
}
