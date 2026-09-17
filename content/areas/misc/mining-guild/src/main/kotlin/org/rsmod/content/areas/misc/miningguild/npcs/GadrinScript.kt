package org.rsmod.content.areas.misc.miningguild.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseMiningLvl
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GadrinScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.mguild_capeseller") { talk(it.npc) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) = startDialogue(npc) { greeting() }

    private suspend fun Dialogue.greeting() {
        chatNpc(
            happy,
            "Welcome to the Mining Guild. My name's Gadrin and I'm the Guildmaster. Can I help you " +
                "with anything?",
        )
        mainOptions()
    }

    private suspend fun Dialogue.mainOptions() {
        val master = player.baseMiningLvl >= MAX_LEVEL
        val capeOption =
            if (master) "Can I buy a Skillcape of Mining from you?" else "Can you tell me about your skillcape?"
        when (
            choice4(
                "What have you got in the guild?",
                Topic.Guild,
                "What do you dwarves do with the ore you mine?",
                Topic.Ore,
                capeOption,
                Topic.Cape,
                "No thanks, I'm fine.",
                Topic.Leave,
            )
        ) {
            Topic.Guild -> guild()
            Topic.Ore -> ore()
            Topic.Cape -> if (master) buyCape() else describeCape()
            Topic.Leave -> chatPlayer(neutral, "No thanks, I'm fine.")
        }
    }

    private suspend fun Dialogue.guild() {
        describeGuild()
        when (choice2("What do you dwarves do with the ore you mine?", Topic.Ore, "No thanks, I'm fine.", Topic.Leave)) {
            Topic.Ore -> ore()
            else -> chatPlayer(neutral, "No thanks, I'm fine.")
        }
    }

    private suspend fun Dialogue.ore() {
        chatPlayer(quiz, "What do you dwarves do with the ore you mine?")
        describeOreUse()
        when (choice2("What have you got in the guild?", Topic.Guild, "No thanks, I'm fine.", Topic.Leave)) {
            Topic.Guild -> guild()
            else -> chatPlayer(neutral, "No thanks, I'm fine.")
        }
    }

    private suspend fun Dialogue.describeCape() {
        chatPlayer(quiz, "Can you tell me about your skillcape?")
        chatNpc(
            happy,
            "Sure, this is a Skillcape of Mining. It shows my stature as a master miner! It has all " +
                "sorts of uses including a skill boost to my Mining skill and a chance of mining " +
                "extra ores. When you get to level 99 come and talk to",
        )
        chatNpc(happy, "me and I'll sell you one.")
        chatNpc(neutral, "Is there anything else I can help you with?")
        mainOptions()
    }

    private suspend fun Dialogue.buyCape() {
        chatPlayer(quiz, "I believe I can buy a Skillcape of Mining from you?")
        chatNpc(
            happy,
            "You believe right, miner. You have earned the right to wear one and when you do you'll " +
                "have a small chance of finding an extra ore, but I'll need 99000 coins from you first.",
        )
        if (choice2("Sorry, that's overpriced.", false, "Okay then.", true)) {
            completePurchase()
        } else {
            chatPlayer(sad, "Sorry, that's overpriced.")
            chatNpc(neutral, "I'm sorry you feel that way; come back if you change your mind.")
        }
    }

    private suspend fun Dialogue.completePurchase() {
        chatPlayer(happy, "Okay then.")
        val inv = access.inv
        if (inv.count("obj.coins") < CAPE_PRICE) {
            chatPlayer(sad, "But, unfortunately, I don't have enough money with me.")
            chatNpc(neutral, "Well, come back and see me when you do.")
            return
        }
        if (inv.freeSpace() < 2) {
            chatNpc(
                neutral,
                "Unfortunately all Skillcapes are only available with a free hood, it's part of a " +
                    "skill promotion deal; buy one get one free, you know. So you'll need to free up " +
                    "some inventory space before I can sell you one.",
            )
            return
        }
        if (access.invDel(inv, "obj.coins", count = CAPE_PRICE).failure) {
            return
        }
        access.invAdd(inv, "obj.skillcape_mining")
        access.invAdd(inv, "obj.skillcape_mining_hood")
        chatNpc(happy, "Thanks very much, master miner.")
    }

    private enum class Topic {
        Guild,
        Ore,
        Cape,
        Leave,
    }

    private companion object {
        const val MAX_LEVEL = 99
        const val CAPE_PRICE = 99_000
    }
}
