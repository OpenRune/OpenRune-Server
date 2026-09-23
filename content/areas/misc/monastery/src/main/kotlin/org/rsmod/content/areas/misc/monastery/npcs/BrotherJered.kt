package org.rsmod.content.areas.misc.monastery.npcs

import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.basePrayerLvl
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Brother Jered, upstairs in Edgeville Monastery: blesses symbols and sells the Prayer cape. */
class BrotherJered : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.brother_jered") { startDialogue(it.npc) { menu() } }
    }

    private suspend fun Dialogue.menu() {
        while (true) {
            val help =
                choice2("What can you do to help a bold adventurer like myself?", true, "Praise be to Saradomin!", false)
            if (!help) {
                chatPlayer(happy, "Praise be to Saradomin!")
                chatNpc(happy, "Yes! Praise he who brings life to this world.")
                return
            }
            chatPlayer(quiz, "What can you do to help a bold adventurer like myself?")
            when {
                player.basePrayerLvl >= MAX_PRAYER -> {
                    if (sellCape()) return
                }
                access.inv.count(UNBLESSED) > 0 -> {
                    if (blessOrExplain()) return
                }
                else -> {
                    chatNpc(happy, "I can tell you about holy symbols or the Skillcape of Prayer.")
                    if (choice2("Tell me about holy symbols.", true, "Tell me about the Skillcape of Prayer.", false)) {
                        holySymbols()
                        return
                    }
                    if (!skillcape()) return
                }
            }
        }
    }

    private suspend fun Dialogue.blessOrExplain(): Boolean {
        chatNpc(happy, "Well I can bless that star of Saradomin you have, or I could tell you about the Skillcape of Prayer!")
        when (choice3("Bless star, please.", 1, "Tell me about the Skillcape of Prayer.", 2, "None of those, thanks.", 3)) {
            1 -> {
                chatPlayer(happy, "Yes please.")
                bless()
                return true
            }
            2 -> return !skillcape()
            else -> {
                chatPlayer(neutral, "No thank you.")
                return true
            }
        }
    }

    private suspend fun Dialogue.bless() {
        if (access.invDel(access.inv, UNBLESSED, 1).failure) {
            return
        }
        access.invAdd(access.inv, BLESSED)
        mesbox(
            "You give Jered the symbol. Jered closes his eyes and places his hand on the symbol. " +
                "He softly chants. Jered passes you the holy symbol.",
        )
    }

    private suspend fun Dialogue.holySymbols() {
        chatPlayer(neutral, "Tell me about holy symbols.")
        chatNpc(
            happy,
            "If you have a silver star, which is the holy symbol of Saradomin, then I can bless " +
                "it. Then if you are wearing it, it will help you when you are praying.",
        )
    }

    /** Returns true when the player asked to go back to the opening menu. */
    private suspend fun Dialogue.skillcape(): Boolean {
        chatPlayer(quiz, "Tell me about the Skillcape of Prayer.")
        chatNpc(
            neutral,
            "The Skillcape of Prayer is the hardest of all the skillcapes to get; it requires " +
                "much devotion to acquire but also imbues the wearer with the ability to briefly fly!",
        )
        chatNpc(
            neutral,
            "The Cape of Prayer also increases the amount of Prayer points restored from drinking " +
                "potions when it is equipped. Is there something else I can do for you?",
        )
        val again =
            choice2("Yes, what else can you do for a bold adventurer like myself?", true, "No, thank you.", false)
        if (!again) {
            chatPlayer(neutral, "No thank you.")
            return false
        }
        return true
    }

    /** Returns true once the skillcape conversation has run its course. */
    private suspend fun Dialogue.sellCape(): Boolean {
        chatNpc(
            neutral,
            "Well, seeing as you are so devout in praising the gods, I could sell you a Skillcape " +
                "of Prayer, which increases the amount of Prayer points restored when drinking potions.",
        )
        val buying =
            choice2(
                "Yes, please. So few people have Skillcapes of Prayer!",
                true,
                "I'm afraid I can't afford that.",
                false,
            )
        if (!buying) {
            chatPlayer(neutral, "No thanks, I can't afford one of those.")
            chatNpc(neutral, "I am sorry to hear that. If you should find yourself in wealthier times come back and see me.")
            return true
        }
        chatPlayer(happy, "Yes, please. So few people have Skillcapes of Prayer!")
        chatNpc(
            neutral,
            "One as pious as you has certainly earned the right to wear one, but the monastery " +
                "requires a donation of $CAPE_PRICE coins for the privilege.",
        )
        if (!choice2("I am always happy to contribute towards the monastery's upkeep.", true, "I'm afraid I can't afford that.", false)) {
            chatPlayer(neutral, "No thanks, I can't afford one of those.")
            return true
        }
        chatPlayer(happy, "I am always happy to contribute towards the monastery's upkeep.")
        if (access.inv.count(COINS) < CAPE_PRICE) {
            chatPlayer(sad, "But, unfortunately, I don't have enough money with me.")
            chatNpc(neutral, "Well, come back and see me when you do.")
            return true
        }
        if (access.inv.freeSpace() < CAPE_SLOTS && access.inv.count(COINS) != CAPE_PRICE) {
            chatNpc(
                neutral,
                "Unfortunately all Skillcapes are only available with a free hood, it's part of a " +
                    "skill promotion deal; buy one get one free, you know. So you'll need to free " +
                    "up some inventory space before I can sell you one.",
            )
            return true
        }
        if (access.invDel(access.inv, COINS, CAPE_PRICE).failure) {
            return true
        }
        access.invAdd(access.inv, CAPE)
        access.invAdd(access.inv, HOOD)
        return true
    }

    private companion object {
        private const val MAX_PRAYER = 99
        private const val CAPE_PRICE = 99000
        private const val CAPE_SLOTS = 2
        private const val COINS = "obj.coins"
        private const val CAPE = "obj.skillcape_prayer"
        private const val HOOD = "obj.skillcape_prayer_hood"
        private const val UNBLESSED = "obj.stringstar"
        private const val BLESSED = "obj.blessedstar"
    }
}
