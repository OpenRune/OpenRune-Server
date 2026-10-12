package org.rsmod.content.areas.city.varlamore.camtorum.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class CamTorumShops @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        for (shop in SHOPS) {
            onOpNpc3(shop.npc) { open(it.npc, shop) }
        }
        onOpNpc1(PUB.npc) { startDialogue(it.npc) { bartender(it.npc) } }
        onOpNpc1(BLACKSMITH.npc) { startDialogue(it.npc) { blacksmith(it.npc) } }
        onOpNpc1(JEWELLERY.npc) { startDialogue(it.npc) { conara(it.npc) } }
        onOpNpc1(HERBALIST.npc) { startDialogue(it.npc) { huito(it.npc) } }
        onOpNpc1(MAGIC.npc) { startDialogue(it.npc) { nahta(it.npc) } }
        onOpNpc1(GENERAL.npc) { startDialogue(it.npc) { shopkeeper(it.npc) } }
        onOpNpc1(MINING.npc) { startDialogue(it.npc) { tizoro(it.npc) } }
        onOpNpc1(BAKER.npc) { startDialogue(it.npc) { yarnio(it.npc) } }
    }

    private fun ProtectedAccess.open(npc: Npc, shop: Shop) {
        shops.open(player, npc, shop.title, shop.inv)
    }

    private fun Dialogue.open(npc: Npc, shop: Shop) {
        access.open(npc, shop)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bartender_(The_Lost_Pickaxe)
    private suspend fun Dialogue.bartender(npc: Npc) {
        chatNpc(
            neutral,
            "A human in The Lost Pickaxe? My old pa would be fuming. Still, business is business " +
                "I guess. What can I get you?",
        )
        when (choice2("What do you have?", 1, "Nothing thanks.", 2)) {
            1 -> {
                chatPlayer(quiz, "What do you have?")
                chatNpc(neutral, "Take a look.")
                open(npc, PUB)
            }
            2 -> {
                chatPlayer(neutral, "Nothing thanks.")
                chatNpc(
                    angry,
                    "So you're just lurking here? Pa always said your kind were freaks.",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Blacksmith_(Cam_Torum)
    private suspend fun Dialogue.blacksmith(npc: Npc) {
        chatNpc(angry, "A human? Either buy something or get out!")
        when (choice2("What do you have?", 1, "I'll get going.", 2)) {
            1 -> {
                chatPlayer(quiz, "What do you have?")
                chatNpc(angry, "In a blacksmith? What do you think? Weapons!")
                open(npc, BLACKSMITH)
            }
            2 -> {
                chatPlayer(neutral, "I'll get going.")
                chatNpc(angry, "Good.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Conara
    private suspend fun Dialogue.conara(npc: Npc) {
        chatNpc(happy, "Hey there. Looking to buy or sell some jewellery?")
        when (choice2("Sure.", 1, "No thanks.", 2)) {
            1 -> {
                chatPlayer(happy, "Sure.")
                chatNpc(happy, "Good choice.")
                open(npc, JEWELLERY)
            }
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(neutral, "Well come back if you change your mind.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Huito
    private suspend fun Dialogue.huito(npc: Npc) {
        chatNpc(neutral, "Yes? Can I help you?")
        when (choice2("What do you have for sale?", 1, "No thanks.", 2)) {
            1 -> {
                chatPlayer(quiz, "What do you have for sale?")
                chatNpc(neutral, "Only the finest herbal supplies.")
                open(npc, HERBALIST)
            }
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(angry, "Move along then.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Nahta
    private suspend fun Dialogue.nahta(npc: Npc) {
        chatNpc(neutral, "Are you in need of some runes?")
        while (true) {
            val option =
                choice3("Yes please.", 1, "Where do you get your runes from?", 2, "No thanks.", 3)
            when (option) {
                1 -> {
                    chatPlayer(happy, "Yes please.")
                    return open(npc, MAGIC)
                }
                2 -> runeSource()
                else -> {
                    chatPlayer(neutral, "No thanks.")
                    chatNpc(neutral, "Then we have nothing more to discuss.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.runeSource() {
        chatPlayer(quiz, "Where do you get your runes from?")
        chatNpc(angry, "That is not the kind of information I intend to give to a human.")
        chatPlayer(quiz, "Why?")
        chatNpc(
            neutral,
            "Because before the Varlamorians decided to open their lands up to visitors, Cam " +
                "Torum was the sole supplier of runes to the entire region.",
        )
        chatPlayer(
            neutral,
            "Okay... But that's not the case anymore. With the kingdom open, they can just get " +
                "their runes from the mainland.",
        )
        chatNpc(
            angry,
            "A fact I'm all too aware of, especially given the impact it is having on my prices.",
        )
        chatNpc(
            neutral,
            "Nevertheless, there is still a benefit to having a source of runes closer to home, " +
                "and I do not intend to share any information on that source.",
        )
        val charm =
            RING_OF_CHAROS in player.worn &&
                choice2(
                    "Fair enough.",
                    false,
                    "[Charm] You can trust me. I won't tell anyone else.",
                    true,
                )
        if (!charm) {
            chatPlayer(neutral, "Fair enough.")
            chatNpc(neutral, "Now, are you in need of some runes?")
            return
        }
        chatPlayer(happy, "You can trust me. I won't tell anyone else.")
        chatNpc(
            angry,
            "You display the typical arrogance of a human. I am a Dwarf of Cam Torum. My people " +
                "have mastered Smithing and Magic in equal measure.",
        )
        chatNpc(
            angry,
            "That little trinket of yours may well work wonders on a lesser mind, but you will " +
                "find it to be of little use here.",
        )
        chatPlayer(sad, "Oh...")
        chatNpc(neutral, "Now, are you in need of some runes?")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Shopkeeper_(Cam_Torum)
    private suspend fun Dialogue.shopkeeper(npc: Npc) {
        chatNpc(neutral, "Want something?")
        when (choice2("What do you have for sale?", 1, "No thanks.", 2)) {
            1 -> {
                chatPlayer(quiz, "What do you have for sale?")
                chatNpc(neutral, "Take a look.")
                open(npc, GENERAL)
            }
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(angry, "Then leave.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Tizoro
    private suspend fun Dialogue.tizoro(npc: Npc) {
        chatNpc(happy, "Pickaxes! Get your pickaxes here!")
        when (choice2("Let's see what you have.", 1, "I'll pass.", 2)) {
            1 -> {
                chatPlayer(happy, "Let's see what you have.")
                open(npc, MINING)
            }
            2 -> {
                chatPlayer(neutral, "I'll pass.")
                chatNpc(neutral, "Very well.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Yarnio
    private suspend fun Dialogue.yarnio(npc: Npc) {
        chatNpc(
            happy,
            "Fancy some pie? Mine are far better than any you'll find up on the surface.",
        )
        when (choice2("I'll take a look.", 1, "No thanks.", 2)) {
            1 -> {
                chatPlayer(happy, "I'll take a look.")
                open(npc, BAKER)
            }
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(angry, "Fine. Enjoy your human muck then.")
            }
        }
    }

    private class Shop(val npc: String, val title: String, val inv: String)

    private companion object {
        const val RING_OF_CHAROS = "obj.ring_of_charos_unlocked"

        val PUB = Shop("npc.cam_torum_bartender", "The Lost Pickaxe", "inv.pub_cam_torum")
        val BLACKSMITH =
            Shop(
                "npc.cam_torum_shop_blacksmith",
                "Cam Torum Blacksmith",
                "inv.cam_torum_shop_blacksmith",
            )
        val JEWELLERY =
            Shop(
                "npc.cam_torum_shop_jewellery",
                "Conara's Jewels",
                "inv.cam_torum_shop_jewellery",
            )
        val HERBALIST =
            Shop(
                "npc.cam_torum_shop_herbalist",
                "Huito's Herbal Supplies",
                "inv.cam_torum_shop_herbalist",
            )
        val MAGIC =
            Shop("npc.cam_torum_shop_magic", "The Runic Emporium", "inv.cam_torum_shop_magic")
        val GENERAL =
            Shop(
                "npc.cam_torum_shop_general",
                "Cam Torum General Store",
                "inv.cam_torum_shop_general",
            )
        val MINING =
            Shop("npc.cam_torum_shop_mining", "Tizoro's Pickaxes", "inv.cam_torum_shop_mining")
        val BAKER =
            Shop("npc.cam_torum_shop_baker", "Yarnio's Baked Goods", "inv.cam_torum_shop_baker")

        val SHOPS = listOf(PUB, BLACKSMITH, JEWELLERY, HERBALIST, MAGIC, GENERAL, MINING, BAKER)
    }
}
