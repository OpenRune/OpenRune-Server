package org.rsmod.content.skills.hunter

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.shops.Shops
import org.rsmod.api.table.hunter.HunterLeonBoltsRow
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.content.skills.skillMulti
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class LeonScript @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(LEON) { startDialogue(it.npc) { leon(it.npc) } }
        onOpNpc3(LEON) { openShop(it.npc) }
        onOpNpc4(LEON) { ammo() }
    }

    private fun ProtectedAccess.openShop(npc: Npc) {
        shops.open(player, npc, SHOP_TITLE, SHOP)
    }

    private suspend fun Dialogue.leon(npc: Npc) {
        if (CROSSBOW !in player.inv && CROSSBOW !in player.worn) {
            mesbox("Leon is gazing intently at the crossbow in his hands.")
            menu(npc)
            return
        }
        chatNpc(happy, "Oh, hey, you have one of my crossbows! How's it working for you?")
        val choice =
            choice5(
                "It's okay, thanks.",
                1,
                "What is this place?",
                2,
                "Can you tell me about your crossbow?",
                3,
                "Tell me about the ammo for your crossbow.",
                4,
                "I'll be off now, excuse me.",
                5,
            )
        when (choice) {
            1 -> {
                chatPlayer(neutral, "It's okay, thanks.")
                menu(npc)
            }
            2 -> place(npc)
            3 -> crossbow(npc)
            4 -> ammoTalk(npc)
            else -> leave()
        }
    }

    private suspend fun Dialogue.menu(npc: Npc) {
        val choice =
            choice4(
                "What is this place?",
                1,
                "Can you tell me about your crossbow?",
                2,
                "Tell me about the ammo for your crossbow.",
                3,
                "I'll be off now, excuse me.",
                4,
            )
        when (choice) {
            1 -> place(npc)
            2 -> crossbow(npc)
            3 -> ammoTalk(npc)
            else -> leave()
        }
    }

    private suspend fun Dialogue.place(npc: Npc) {
        chatPlayer(quiz, "What is this place?")
        chatNpc(
            neutral,
            "This is Aleck's Hunter Emporium. Basically, it's just a shop with a fancy name; you can buy various " +
                "weapons and traps here.",
        )
        val choice =
            choice4(
                "Can I buy some equipment from the shop then?",
                1,
                "Can you tell me about your crossbow?",
                2,
                "Tell me about the ammo for your crossbow.",
                3,
                "I'll be off now, excuse me.",
                4,
            )
        when (choice) {
            1 -> {
                chatPlayer(quiz, "Can I buy some equipment from the shop then?")
                chatNpc(neutral, "Oh, this isn't my shop; the owner is Aleck over there behind the counter.")
                chatNpc(
                    neutral,
                    "I experiment with weapon designs. I'm here because I've been trying to convince people to back " +
                        "my research and maybe sell some of my products - like this one I'm holding - in their shops.",
                )
                chatNpc(neutral, "Aleck doesn't seem to be interested, though.")
                others(npc)
            }
            2 -> crossbow(npc)
            3 -> ammoTalk(npc)
            else -> leave()
        }
    }

    private suspend fun Dialogue.others(npc: Npc) {
        val choice =
            choice3(
                "Can you tell me about your crossbow?",
                1,
                "Tell me about the ammo for your crossbow.",
                2,
                "I'll be off now, excuse me.",
                3,
            )
        when (choice) {
            1 -> crossbow(npc)
            2 -> ammoTalk(npc)
            else -> leave()
        }
    }

    private suspend fun Dialogue.crossbow(npc: Npc) {
        chatPlayer(quiz, "Can you tell me about your crossbow?")
        chatNpc(
            happy,
            "It's good, isn't it? I designed it to incorporate the bones of various animals in its construction.",
        )
        chatNpc(
            neutral,
            "It's a fair bit faster than an ordinary crossbow too; it'll take you far less time to reload between " +
                "shots.",
        )
        if (choice2("That's crazy, it'll never work!", true, "That sounds good. Let's trade.", false)) {
            chatPlayer(confused, "That's crazy, it'll never work!")
            chatNpc(neutral, "That's what they said about my wind-powered mouse traps, too.")
            chatPlayer(quiz, "And did they work?")
            chatNpc(
                neutral,
                "Well, they only ran into problems because people kept insisting on trying to use them indoors.",
            )
            chatNpc(neutral, "Anyway, I think my crossbow invention is showing a lot more promise.")
            others(npc)
            return
        }
        chatPlayer(happy, "That sounds good. Let's trade.")
        access.openShop(npc)
    }

    private suspend fun Dialogue.ammoTalk(npc: Npc) {
        chatPlayer(quiz, "Tell me about the ammo for your crossbow.")
        chatNpc(neutral, "Ah, I admit as a result of its... er... unique construction, it won't take just any old bolts.")
        chatNpc(neutral, "If you can supply the materials and a token fee, I'd be happy to make some for you.")
        chatNpc(
            neutral,
            "I need kebbit spikes, lots of 'em. Not all kebbits have spikes, mind you. You'll be wanting prickly " +
                "kebbits or, even better, razor-backed kebbits to get material hard enough.",
        )
        if (choice2("Can't I just make my own?", true, "Okay, can you make ammo for me?", false)) {
            chatPlayer(quiz, "Can't I just make my own?")
            chatNpc(neutral, "Yes, I suppose you could, although you'll need a steady hand with a knife and a chisel.")
            chatNpc(
                neutral,
                "The bolts have an unusual diameter, but basically you'll just need to be able to carve kebbit " +
                    "spikes into straight shafts.",
            )
            chatNpc(neutral, "Meanwhile, since you're here, I can make some for you if you have the materials.")
            others(npc)
            return
        }
        chatPlayer(quiz, "Okay, can you make ammo for me?")
        access.ammo()
    }

    private suspend fun Dialogue.leave() {
        chatPlayer(neutral, "I'll be off now, excuse me.")
        chatNpc(neutral, "Well, if you ever find yourself in need of that innovative edge, you can always find me here.")
        chatPlayer(neutral, "... thanks.")
    }

    private suspend fun ProtectedAccess.ammo() {
        val menu = skillMulti {
            verb("make")
            for (bolt in HunterLeonBoltsRow.all()) {
                entry(bolt.bolts.internalName) {
                    material(bolt.spike.internalName)
                    material(COINS, bolt.fee)
                }
            }
        }
        val config = menu.copy(maxCountProvider = { inv, entry -> maxOf(1, entry.maxCount(inv)) })
        openSkillMulti(config) { selection ->
            val bolt = HunterLeonBoltsRow.all().first { it.bolts.internalName == selection.entry.internal }
            make(bolt, selection.amount)
        }
    }

    private suspend fun ProtectedAccess.make(bolt: HunterLeonBoltsRow, amount: Int) {
        val spike = bolt.spike.name.lowercase()
        val bolts = bolt.bolts.name.lowercase()
        val count = minOf(amount, inv.count(bolt.spike.internalName), inv.count(COINS) / bolt.fee)
        if (count <= 0) {
            mesbox("You need 1 $spike and ${bolt.fee} coins to make $BOLTS_PER_SPIKE $bolts.")
            return
        }
        val made =
            player.invTransaction(inv) {
                val target = select(inv)
                delete(target, bolt.spike.id, count)
                delete(target, COINS.asRSCM(RSCMType.OBJ), count * bolt.fee)
                add(target, bolt.bolts.id, count * BOLTS_PER_SPIKE)
            }
        if (made.failure) {
            mes("You don't have enough inventory space.")
            return
        }
        objbox(
            bolt.bolts.internalName,
            "You hand the weapon designer one ${spike.replace("kebbit ", "")} and ${bolt.fee} " +
                "coins. In return he presents you with $BOLTS_PER_SPIKE ${bolts.replace("kebbit ", "")}.",
        )
    }

    private companion object {
        const val LEON = "npc.hunting_weapon_designer"
        const val SHOP = "inv.skill_guide_hunting_eagles"
        const val SHOP_TITLE = "Leon's Prototype Crossbow."
        const val CROSSBOW = "obj.hunting_crossbow"
        const val COINS = "obj.coins"
        const val BOLTS_PER_SPIKE = 12
    }
}
