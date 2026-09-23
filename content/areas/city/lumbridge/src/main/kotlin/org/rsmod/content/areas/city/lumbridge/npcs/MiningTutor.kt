package org.rsmod.content.areas.city.lumbridge.npcs

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseMiningLvl
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MiningTutor : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.aide_tutor_mining") { startDialogue(it.npc) }
    }

    private suspend fun ProtectedAccess.startDialogue(npc: Npc) {
        startDialogue(npc) { tutorMenu() }
    }

    private suspend fun Dialogue.tutorMenu() {
        val choice =
            choice4(
                adviceOption(),
                1,
                "Are there any mining related quests?",
                2,
                "Tell me about different rocks and picks.",
                3,
                "Goodbye.",
                4,
            )
        when (choice) {
            1 -> levelAdvice()
            2 -> miningQuests()
            3 -> rocksAndPicks()
            4 -> chatPlayer(neutral, "Goodbye.")
        }
    }

    private fun Dialogue.adviceOption(): String =
        when {
            player.baseMiningLvl >= ADVANCED -> "Any advice for an advanced miner?"
            player.baseMiningLvl >= INTERMEDIATE ->
                "I already know about the basics of mining, got any tips?"
            else -> "Can you teach me the basics of mining please?"
        }

    private suspend fun Dialogue.levelAdvice() {
        chatPlayer(quiz, adviceOption())
        when {
            player.baseMiningLvl >= ADVANCED -> advancedAdvice()
            player.baseMiningLvl >= INTERMEDIATE -> intermediateAdvice()
            else -> basicAdvice()
        }
        tutorMenu()
    }

    private suspend fun Dialogue.givePickIfRequired() {
        if (PICKAXE in player.inv || PICKAXE in player.worn) {
            chatNpc(happy, "As you already have a pick, you can mine the rocks around me.")
            return
        }
        if (player.invAdd(player.inv, "obj.bronze_pickaxe").success) {
            chatNpc(
                happy,
                "As you're already here, have a pick so that you can mine the rocks around me.",
            )
        } else {
            chatNpc(
                sad,
                "I'd give you a pick to mine the rocks around me, but you don't have room in your inventory.",
            )
        }
    }

    private suspend fun Dialogue.basicAdvice() {
        objbox(
            "obj.mining_site_icon_dummy",
            "Look for this icon on your minimap to find mining rocks.",
        )
        givePickIfRequired()
        chatNpc(
            neutral,
            "You can tell the ore you'll get from a rock by looking at it. The colour of the rock helps, too.",
        )
        chatNpc(
            neutral,
            "To mine, simply click on the rock to mine it, while you've got a pickaxe with you.",
        )
        chatNpc(
            neutral,
            "When you have a full inventory, take it to the bank. " +
                "There's a bank on the roof of the castle in Lumbridge.",
        )
        objbox(
            "obj.bank_icon_dummy",
            "Look for this symbol on your minimap after climbing the stairs of the Lumbridge Castle to the top.",
        )
    }

    private suspend fun Dialogue.intermediateAdvice() {
        givePickIfRequired()
        chatNpc(
            happy,
            "You can get different rocks in different places throughout the land, explore and find them.",
        )
        chatNpc(
            happy,
            "You could start by traveling to the Al Kharid mine. Follow the " +
                "path north and pay the 10 coins toll to go through the gate. " +
                "Head north after passing through the gate and you will find the mine.",
        )
        chatNpc(worried, "Be careful though, the scorpions in the mine will attack you.")
        chatNpc(quiz, "I could show you where Al Kharid mine is on your map if you like?")
        if (choice2("Yes please.", true, "No thanks.", false)) {
            chatPlayer(neutral, "Yes please.")
            access.showMiningSite(AL_KHARID_MINE)
        } else {
            chatPlayer(neutral, "No thanks.")
        }
    }

    private suspend fun Dialogue.advancedAdvice() {
        givePickIfRequired()
        chatNpc(
            happy,
            "Investigate the mining guild, the entrance to this is in the " +
                "east of Falador down some steps. It could be most useful to you!",
        )
        chatNpc(
            happy,
            "You might also like to check out the coal mine west of Seers' " +
                "village. This will enable you to use the coal trucks.",
        )
        chatPlayer(neutral, "Thank you, I'll remember that.")
        chatNpc(quiz, "I could show you where either of the places are on your map if you like?")
        val choice =
            choice3(
                "Can you show me where the mining guild is?",
                1,
                "Can you show me where the coal mine is?",
                2,
                "No thanks.",
                3,
            )
        when (choice) {
            1 -> {
                chatPlayer(quiz, "Can you show me where the mining guild is?")
                access.showMiningSite(MINING_GUILD)
            }
            2 -> {
                chatPlayer(quiz, "Can you show me where the coal mine is?")
                access.showMiningSite(COAL_TRUCKS)
            }
            3 -> chatPlayer(neutral, "No thanks.")
        }
    }

    private suspend fun Dialogue.miningQuests() {
        chatNpc(
            neutral,
            "Oh yes, if you haven't already, speak to Doric who can be " +
                "found around the anvils north of Falador. I'm sure he can help you out.",
        )
        chatNpc(
            neutral,
            "You could investigate the Dig Site east of Varrock, I'm sure you'll find something rewarding there.",
        )
        tutorMenu()
    }

    private suspend fun Dialogue.rocksAndPicks() {
        chatPlayer(quiz, "Tell me about different rocks and picks.")
        chatNpc(neutral, "Ok then.. here goes....")
        rocksAndPicksMenu()
    }

    private suspend fun Dialogue.rocksAndPicksMenu() {
        when (choice3("Rocks and Ores.", 1, "Pickaxes.", 2, "Teach me about mining.", 3)) {
            1 -> rocksMenu()
            2 -> picksMenu()
            3 -> tutorMenu()
        }
    }

    private suspend fun Dialogue.rocksMenu() {
        val choice =
            choice5(
                "Clay and Rune Essence.",
                1,
                "Copper and Tin.",
                2,
                "Iron and Coal.",
                3,
                "Teach me about mining.",
                4,
                "More...",
                5,
            )
        when (choice) {
            1 -> clayAndEssence()
            2 -> copperAndTin()
            3 -> ironAndCoal()
            4 -> tutorMenu()
            5 -> moreRocksMenu()
        }
    }

    private suspend fun Dialogue.moreRocksMenu() {
        val choice =
            choice4(
                "Silver and Gold.",
                1,
                "Mithril, Adamantite and Runite.",
                2,
                "Teach me about mining.",
                3,
                "Back...",
                4,
            )
        when (choice) {
            1 -> silverAndGold()
            2 -> highTierOres()
            3 -> tutorMenu()
            4 -> rocksMenu()
        }
    }

    private suspend fun Dialogue.afterRockTopic() {
        when (choice2("Teach me about mining.", 1, "Teach me about different rocks.", 2)) {
            1 -> tutorMenu()
            2 -> rocksMenu()
        }
    }

    private suspend fun Dialogue.afterMoreRockTopic() {
        when (choice2("Teach me about mining.", 1, "Teach me about different rocks.", 2)) {
            1 -> tutorMenu()
            2 -> moreRocksMenu()
        }
    }

    private suspend fun Dialogue.clayAndEssence() {
        objbox(
            "obj.clay",
            "Clay can be mined in many places. Try south west of Varrock " +
                "or the crafting guild. See the crafting tutor upstairs in " +
                "Lumbridge Castle for more information on what you can make with clay.",
        )
        objbox(
            "obj.rune_essence",
            "Rune Essence can only be mined at secret places... I don't " +
                "know where these are but you can speak to the Duke of " +
                "Lumbridge to find out more.",
        )
        chatNpc(
            neutral,
            "You can make all sorts of things from clay, if you have the " +
                "crafting skill. Rune Essence is used for magic to cast spells.",
        )
        afterRockTopic()
    }

    private suspend fun Dialogue.copperAndTin() {
        objbox(
            "obj.copper_ore",
            "Rocks which yield copper ore can be found around here, and " +
                "other places such as Al Kharid, Rimmington or Varrock " +
                "mines. They're quite common.",
        )
        objbox(
            "obj.tin_ore",
            "Tin ore is much the same as copper. It can be found around " +
                "this area as well as Varrock, Al Kharid and the Barbarian Village.",
        )
        afterRockTopic()
    }

    private suspend fun Dialogue.ironAndCoal() {
        objbox(
            "obj.iron_ore",
            "Iron can be mined around Rimmington, Al Kharid and Varrock. " +
                "It's tricky to smelt though - see the boy at the furnace in Lumbridge for tips.",
        )
        objbox(
            "obj.furnace_icon_dummy",
            "Look for this icon on your minimap to find the furnace.",
        )
        objbox(
            "obj.coal",
            "You'll need to be a fairly good miner before you can start " +
                "mining coal. You can find it in the Dwarf mine, Al Kharid " +
                "and quite a few other places.",
        )
        afterRockTopic()
    }

    private suspend fun Dialogue.silverAndGold() {
        objbox(
            "obj.silver_ore",
            "Silver can be mined to help you with your crafting. You can " +
                "find it in the Crafting guild, Al Kharid and many other mines.",
        )
        objbox(
            "obj.gold_ore",
            "Gold requires quite a lot of experience in mining before you " +
                "can successfully extract it from the rocks. It can be found " +
                "in Al Kharid, the Crafting guild, Rimmington and other places.",
        )
        afterMoreRockTopic()
    }

    private suspend fun Dialogue.highTierOres() {
        doubleobjbox(
            "obj.mithril_ore",
            "obj.adamantite_ore",
            "Mithril and Adamantite rocks are fairly rare and require great " +
                "skill to mine. You can find them in Al Kharid, the Dwarven " +
                "mines, Lumbridge Swamps and other places.",
        )
        objbox(
            "obj.runite_ore",
            "Runite is the most prized of ores and is very rare. I'll leave you to find where to mine it.",
        )
        afterMoreRockTopic()
    }

    private suspend fun Dialogue.picksMenu() {
        val choice =
            choice3(
                "Bronze, Iron and Steel.",
                1,
                "Mithril, Adamantite and Runite.",
                2,
                "Teach me about mining.",
                3,
            )
        when (choice) {
            1 -> lowTierPicks()
            2 -> highTierPicks()
            3 -> tutorMenu()
        }
    }

    private suspend fun Dialogue.lowTierPicks() {
        objbox(
            "obj.bronze_pickaxe",
            "The Bronze Pick is your basic tool of the trade. You can buy " +
                "one from Bob's Axe shop very cheaply, that's north of here " +
                "in Lumbridge, I also heard that exploring the castle walls " +
                "may yield a surprise or two.",
        )
        doubleobjbox(
            "obj.iron_pickaxe",
            "obj.steel_pickaxe",
            "The Iron and Steel Picks are faster than the bronze pickaxe, " +
                "but you will need to progress with your mining a little before " +
                "you can use them. You can buy them from Nurmof in the Dwarven mines.",
        )
        picksMenu()
    }

    private suspend fun Dialogue.highTierPicks() {
        doubleobjbox(
            "obj.mithril_pickaxe",
            "obj.rune_pickaxe",
            "Mithril, Adamantite, and Rune Picks may be used as you " +
                "progress with your skill of mining. They can be obtained " +
                "from Nurmof in the Dwarven mines.",
        )
        picksMenu()
    }

    private fun ProtectedAccess.showMiningSite(coord: CoordGrid) {
        runClientScript(WORLDMAP_TRANSMIT_DATA, player.coords.packed)
        ifOpenOverlay("interface.worldmap")
        ifSetEvents("component.worldmap:toggles", 0..4, IfEvent.Op1)
        runClientScript(
            WORLDMAP_FLAG_ELEMENT,
            "component.worldmap:map_noclick".asRSCM(RSCMType.COMPONENT),
            coord.packed,
            MINING_SITE_ELEMENT,
        )
    }

    private companion object {
        private const val PICKAXE = "content.mining_pickaxe"
        private const val INTERMEDIATE = 25
        private const val ADVANCED = 39

        private const val WORLDMAP_TRANSMIT_DATA = 1749
        private const val WORLDMAP_FLAG_ELEMENT = 3331
        private const val MINING_SITE_ELEMENT = 8

        private val MINING_GUILD = CoordGrid(3046, 3339, 0)
        private val COAL_TRUCKS = CoordGrid(2581, 3480, 0)
        private val AL_KHARID_MINE = CoordGrid(3300, 3310, 0)
    }
}
