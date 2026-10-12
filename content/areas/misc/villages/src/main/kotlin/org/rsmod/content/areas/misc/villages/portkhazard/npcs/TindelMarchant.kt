package org.rsmod.content.areas.misc.villages.portkhazard.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Tindel_Marchant
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:rusty_sword_identification] identifying rusty swords and scimitars,
//  the Give-Sword op and using them on Tindel
class TindelMarchant : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.tindel_marchant") { startDialogue(it.npc) { tindel() } }
    }

    private suspend fun Dialogue.tindel() {
        chatNpc(happy, "Hello there! Welcome to my special antiques boutique.")
        while (true) {
            val option =
                choice5(
                    "What do you do here?",
                    1,
                    "What's involved?",
                    2,
                    "What do I get from this?",
                    3,
                    "Ok, I'll give it a go!",
                    4,
                    "Ok, thanks.",
                    5,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What do you do here?")
                    chatNpc(
                        happy,
                        "I'm a specialist at identifying rare and antique weapons, specifically " +
                            "swords, but I plan to branch out. If you have any old and rusty " +
                            "weapons that you want me to check out, just show them to me, pay me " +
                            "100 Gold and",
                    )
                    chatNpc(happy, "I'll see if you have an antique on your hands.")
                    chatNpc(
                        neutral,
                        "I can also repair some antique weapons and armours, just show me the " +
                            "item and I'll let you know if I can repair it and for how much.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "What's involved?")
                    chatNpc(
                        neutral,
                        "Well, pay me 100 Gold and I'll see if any rusty swords that you've found " +
                            "are actually worth anything. Some of them might be worth some money! " +
                            "If it's an antique item though, I reserve the right to purchase it",
                    )
                    chatNpc(
                        neutral,
                        "immediately for adding to my own personal collection. I'll give you fair " +
                            "price for it.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "What do I get from this?")
                    chatNpc(
                        neutral,
                        "If I can reclaim the sword with my own specialist skills, I'll return it " +
                            "to you in peak condition. If it's an antique, I'll just give you what " +
                            "I think it's worth and I generally pay quite well. However, if it's " +
                            "just a piece of junk, I'll",
                    )
                    chatNpc(
                        neutral,
                        "simply give you the bad news and get rid of the item for you.",
                    )
                }
                4 -> {
                    giveItAGo()
                    return
                }
                5 -> {
                    chatPlayer(happy, "Ok, thanks.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.giveItAGo() {
        chatPlayer(happy, "Ok, I'll give it a go!")
        if (RUSTY_SWORD !in player.inv && RUSTY_SCIMITAR !in player.inv) {
            chatNpc(
                neutral,
                "Sorry my friend, but you don't seem to have any swords that need to be " +
                    "identified.",
            )
            return
        }
        if (player.inv.count(COINS) < FEE) {
            chatNpc(
                neutral,
                "Sorry my friend, but you don't seem to have enough money to cover my fee!",
            )
        }
    }

    private companion object {
        const val COINS = "obj.coins"
        const val RUSTY_SWORD = "obj.digsitesword"
        const val RUSTY_SCIMITAR = "obj.deal_rusty_scimitar"
        const val FEE = 100
    }
}
