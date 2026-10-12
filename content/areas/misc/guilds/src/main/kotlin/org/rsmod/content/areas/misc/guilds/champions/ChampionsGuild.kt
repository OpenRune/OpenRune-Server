package org.rsmod.content.areas.misc.guilds.champions

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class ChampionsGuild
@Inject
constructor(private val shops: Shops, private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.guildmaster") { startDialogue(it.npc) { guildmaster() } }
        onOpNpc1("npc.champions_doorman") { startDialogue(it.npc) { larxus() } }
        onOpNpc1("npc.scavvo") { startDialogue(it.npc) { scavvo(it.npc) } }
        onOpNpc3("npc.scavvo") { player.openRuneStore(it.npc) }
        onOpNpc1("npc.valaine") { startDialogue(it.npc) { valaine(it.npc) } }
        onOpNpc3("npc.valaine") { player.openChampionShop(it.npc) }
    }

    private fun Player.openRuneStore(npc: Npc) {
        shops.open(this, npc, "Scavvo's Rune Store", "inv.runiteshop")
    }

    private fun Player.openChampionShop(npc: Npc) {
        shops.open(this, npc, "Valaine's Shop of Champions.", "inv.championshop")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Guildmaster
    // https://oldschool.runescape.wiki/w/Transcript:Dragon_Slayer_I (not started)
    // TODO(content): [quest:dragon_slayer_i] quest start, progress and the maze key option during
    //  the quest
    // TODO(content): [system:champions_challenge] "I've been given a challenge. What now?"
    private suspend fun Dialogue.guildmaster() {
        chatNpc(happy, "Greetings!")
        if (QuestRequirements.hasCompleted(player, DRAGON_SLAYER_I)) {
            guildmasterAfterQuest()
            return
        }
        when (choice2("What is this place?", 1, "Can I have a quest?", 2)) {
            1 -> {
                chatPlayer(quiz, "What is this place?")
                chatNpc(
                    neutral,
                    "This is the Champions' Guild. Only adventurers who have proved themselves " +
                        "worthy by gaining influence from quests are allowed in here.",
                )
            }
            2 -> {
                chatPlayer(quiz, "Can I have a quest?")
                chatNpc(happy, "Aha!")
                chatNpc(
                    happy,
                    "Yes! A mighty and perilous quest fit only for the most powerful champions! " +
                        "And, at the end of it, you will earn the right to wear the legendary " +
                        "rune platebody!",
                )
            }
        }
    }

    private suspend fun Dialogue.guildmasterAfterQuest() {
        val option =
            choice2("What is this place?", 1, "Can I have another key to Melzar's Maze?", 2)
        if (option == 1) {
            chatPlayer(quiz, "What is this place?")
            chatNpc(
                neutral,
                "This is the Champions' Guild. Only adventurers who have proved themselves " +
                    "worthy by gaining influence from quests are allowed in here.",
            )
            return
        }
        chatPlayer(quiz, "Can I have another key to Melzar's Maze?")
        // TODO(content): [system:key_ring] "I think you've already got a key. Maybe you should
        //  check your key ring."
        if (MAZE_KEY in player.inv || access.bank.count(MAZE_KEY) > 0) {
            chatNpc(neutral, "I think you've already got a key somewhere.")
            return
        }
        chatNpc(happy, "Of course! Here you go.")
        player.invAddOrDrop(objRepo, MAZE_KEY)
        mesbox("You receive a maze key.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Larxus
    // TODO(content): [system:champions_challenge] champion scrolls, Leon d'Cour's challenge and
    //  the rewards chest
    private suspend fun Dialogue.larxus() {
        chatNpc(neutral, "Is there something I can help you with?")
        val option =
            choice3(
                "What is this place?",
                1,
                "Have you any rewards for me?",
                2,
                "Nothing thanks.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What is this place?")
                chatNpc(
                    neutral,
                    "This is the champions' arena. The champions of various races use it to duel " +
                        "those they deem worthy of the honour. If you find a challenge scroll in " +
                        "your travels, bring it here to face your challenger.",
                )
            }
            2 -> {
                chatPlayer(quiz, "Have you any rewards for me?")
                chatNpc(neutral, "No, you've not earned any.")
            }
            3 -> chatPlayer(neutral, "Nothing thanks.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Scavvo
    private suspend fun Dialogue.scavvo(npc: Npc) {
        chatNpc(happy, "'Ello matey! D'ya wanna buy some exciting new toys?")
        val option =
            choice4(
                "No, toys are for kids.",
                1,
                "Let's have a look then.",
                2,
                "Ooh, goody goody, toys!",
                3,
                "Why do you sell most rune armour but not platebodies?",
                4,
            )
        when (option) {
            1 -> chatPlayer(neutral, "No, toys are for kids.")
            2 -> player.openRuneStore(npc)
            3 -> {
                chatPlayer(happy, "Ooh goody goody, toys!")
                player.openRuneStore(npc)
            }
            4 -> {
                chatPlayer(quiz, "Why do you sell most rune armour but not platebodies?")
                chatNpc(
                    neutral,
                    "Oh, you have to complete a special quest in order to wear rune platemail. " +
                        "You should talk to the Guildmaster downstairs about that.",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Valaine
    private suspend fun Dialogue.valaine(npc: Npc) {
        chatNpc(happy, "Hello there. Want to have a look at what we're selling today?")
        when (choice2("Yes please.", 1, "No thank you.", 2)) {
            1 -> player.openChampionShop(npc)
            2 -> chatPlayer(neutral, "No thank you.")
        }
    }

    private companion object {
        const val DRAGON_SLAYER_I = "quest_dragonslayer1"
        const val MAZE_KEY = "obj.melzarkey"
    }
}
