package org.rsmod.content.quest.area.falador

import dev.openrune.types.MesAnimType
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestJournalBuilder
import org.rsmod.content.quest.manager.QuestLocGates
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.menu
import org.rsmod.content.quest.manager.rewards
import org.rsmod.content.quest.manager.startQuestPrompt
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.ScriptContext

class DoricsQuest :
    QuestScript(
        "quest_dorics",
        "varp.doricquest",
        rewards {
            xp("stat.mining", 1300.0)
            item("obj.coins", 180)
            extra("Use of Doric's Anvils")
        },
        ItemRewardDisplay("obj.steel_pickaxe"),
    ) {

    override fun ScriptContext.init() {
        onOpNpc1("npc.doric") { startDialogue(it.npc) { doric() } }
        onOpLoc1("loc.devious_whetstone") {
            arriveDelay()
            if (quest.isQuestCompleted(player)) {
                mes("Nothing interesting happens.")
            } else {
                mesbox("You should probably ask before using that.")
            }
        }
        QuestLocGates.register("loc.dorics_anvil") {
            if (quest.isQuestCompleted(player)) return@register true
            startDialogue { anvilWarning() }
            false
        }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Doric</col>, who is <col=800000>north of Falador</col>."

    override fun questLog(player: ProtectedAccess): String =
        questJournal(player) {
            line("I have spoken to <red>Doric</red>.")
            line("I need to collect some items and bring them to <red>Doric</red>.")
            material(access.player, CLAY, "6 Clay.")
            material(access.player, COPPER, "4 Copper Ore.")
            material(access.player, IRON, "2 Iron Ore.")
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line("I brought Doric 6 clay, 4 copper ore and 2 iron ore, and he now lets me use his anvils.")
        }

    private fun QuestJournalBuilder.material(
        player: Player,
        obj: Pair<String, Int>,
        text: String,
    ) {
        if (player.inv.count(obj.first) >= obj.second) strike(text) else line("<red>$text</red>")
    }

    private fun hasMaterials(player: Player): Boolean =
        MATERIALS.all { (obj, count) -> player.inv.count(obj) >= count }

    private fun hasExactMaterials(player: Player): Boolean =
        MATERIALS.all { (obj, count) -> player.inv.count(obj) == count }

    private suspend fun Dialogue.doric() {
        when {
            quest.isQuestCompleted(player) -> {
                chatNpc(quiz, "Hello traveller, how is your metalworking coming along?")
                chatPlayer(neutral, "Not too bad, Doric.")
                chatNpc(happy, "Good, the love of metal is a thing close to my heart.")
            }
            quest.isQuestInProgress(player) -> materialCheck()
            else -> introduction()
        }
    }

    private suspend fun Dialogue.introduction() {
        chatNpc(quiz, "Hello traveller, what brings you to my humble smithy?")
        when (
            menu(
                "I wanted to use your anvils." to 1,
                "I want to use your whetstone." to 2,
                "Mind your own business, shortstuff!" to 3,
                "I was just checking out the landscape." to 4,
                "What do you make here?" to 5,
            )
        ) {
            1 -> {
                chatPlayer(neutral, "I wanted to use your anvils.")
                chatNpc(
                    neutral,
                    "My anvils get enough work with my own use. I make pickaxes, and it takes a lot " +
                        "of hard work. If you could get me some more materials, then I could let you " +
                        "use them.",
                )
                offerQuest()
            }
            2 -> {
                chatPlayer(neutral, "I wanted to use your whetstone.")
                chatNpc(
                    neutral,
                    "The whetstone is for more advanced smithing, but I could let you use it as well " +
                        "as my anvils if you could get me some more materials.",
                )
                offerQuest()
            }
            3 -> {
                chatPlayer(angry, "Mind your own business, shortstuff!")
                chatNpc(
                    angry,
                    "How nice to meet someone with such pleasant manners. Do come again when you need " +
                        "to shout at someone smaller than you!",
                )
            }
            4 -> {
                chatPlayer(neutral, "I was just checking out the landscape.")
                chatNpc(
                    happy,
                    "Hope you like it. I do enjoy the solitude of my little home. If you get time, " +
                        "please say hi to my friends in the Dwarven Mine.",
                )
                if (menu("Dwarven Mine?" to true, "Will do!" to false)) {
                    chatPlayer(quiz, "Dwarven Mine?")
                    chatNpc(
                        happy,
                        "Yep, the entrance is in the side of Ice Mountain just to the east of here. " +
                            "They're a friendly bunch. Stop in at Nurmof's store and buy one of my " +
                            "pickaxes!",
                    )
                } else {
                    chatPlayer(happy, "Will do!")
                }
            }
            else -> {
                chatPlayer(quiz, "What do you make here?")
                chatNpc(
                    happy,
                    "I make pickaxes. I am the best maker of pickaxes in the whole of Gielinor.",
                )
                chatPlayer(quiz, "Do you have any to sell?")
                chatNpc(neutral, "Sorry, but I've got a running order with Nurmof.")
                if (menu("Who's Nurmof?" to true, "Ah, fair enough." to false)) {
                    chatPlayer(quiz, "Who's Nurmof?")
                    chatNpc(
                        neutral,
                        "Nurmof has a store over in the Dwarven Mine. You can find the entrance on the " +
                            "side of Ice Mountain to the east of here.",
                    )
                } else {
                    chatPlayer(neutral, "Ah, fair enough.")
                }
            }
        }
    }

    private suspend fun Dialogue.offerQuest() {
        if (access.statBase("stat.mining") < RECOMMENDED_MINING) {
            val warning =
                "Before starting this quest, be aware that one or more of your skill levels are " +
                    "lower than recommended."
            mesbox(warning)
        }
        if (startQuestPrompt(quest)) acceptQuest() else declineQuest()
    }

    private suspend fun Dialogue.anvilWarning() {
        chatDoric(
            angry,
            "Hey, who said you could use that? My anvils get enough work with my own use. I make " +
                "pickaxes, and it takes a lot of hard work.",
        )
        val ask =
            menu(
                "Sorry, would it be OK if I used your anvils?" to true,
                "I didn't want to use your anvils anyway." to false,
            )
        if (!ask) {
            chatPlayer(bored, "I didn't want to use your anvils anyway.")
            chatDoric(confused, "That is your choice.")
            return
        }
        chatPlayer(quiz, "Sorry, would it be OK if I used your anvils?")
        chatDoric(neutral, "If you could get me some more materials then I could let you use them.")
        if (quest.isQuestInProgress(player)) return
        val accept =
            menu(
                "Yes, I will get you materials." to true,
                "No, hitting rocks is for the boring people, sorry." to false,
            )
        if (accept) acceptQuest(npcSpeaker = false) else declineQuest(npcSpeaker = false)
    }

    private suspend fun Dialogue.acceptQuest(npcSpeaker: Boolean = true) {
        chatPlayer(happy, "Yes, I will get you the materials.")
        quest.advanceQuestStageTo(access, STARTED)
        access.invAdd(access.inv, "obj.bronze_pickaxe")
        val request =
            "Clay is what I use more than anything, to make casts. Could you get me 6 clay, 4 copper " +
                "ore, and 2 iron ore, please? I could pay a little, and let you use my anvils. Take " +
                "this pickaxe with you just in case you need it."
        if (npcSpeaker) chatNpc(neutral, request) else chatDoric(neutral, request)
        if (!hasMaterials(player)) {
            whereToFind(npcSpeaker)
            return
        }
        chatPlayer(happy, "You know, it's funny you should require those exact things!")
        say(npcSpeaker, quiz, "What do you mean?")
        chatPlayer(
            happy,
            "I can usually fit 28 things in my backpack and in a world full of quite literally " +
                "limitless possibilities, a complete coincidence has occurred!",
        )
        say(npcSpeaker, confused, "I don't quite understand what you're saying?")
        chatPlayer(
            happy,
            "Well, out of pure coincidence, despite definitely not knowing what you were about to " +
                "request, I just so happened to have carried those exact items!",
        )
        if (hasExactMaterials(player)) chatPlayer(happy, "In fact, in the exact quantities too!")
        say(
            npcSpeaker,
            happy,
            "Oh my, that is a coincidence! Pass them here, please. I can spare you some coins for " +
                "your trouble, and please use my anvils any time you want.",
        )
        handIn()
    }

    private suspend fun Dialogue.declineQuest(npcSpeaker: Boolean = true) {
        chatPlayer(neutral, "No, hitting rocks is for the boring people, sorry.")
        say(npcSpeaker, confused, "That is your choice. Nice to meet you anyway.")
    }

    private suspend fun Dialogue.materialCheck() {
        chatNpc(quiz, "Have you got my materials yet, traveller?")
        if (hasMaterials(player)) {
            chatPlayer(happy, "I have everything you need!")
            chatNpc(
                happy,
                "Many thanks! Pass them here, please. I can spare you some coins for your trouble, " +
                    "and please use my anvils any time you want.",
            )
            handIn()
            return
        }
        chatPlayer(sad, "Sorry, I don't have them all yet.")
        chatNpc(
            neutral,
            "Not to worry, stick at it. Remember, I need 6 clay, 4 copper ore, and 2 iron ore.",
        )
        whereToFind(npcSpeaker = true)
    }

    private suspend fun Dialogue.whereToFind(npcSpeaker: Boolean) {
        val ask =
            menu("Where can I find those?" to true, "Certainly, I'll be right back!" to false)
        if (!ask) {
            chatPlayer(happy, "Certainly, I'll be right back!")
            return
        }
        chatPlayer(quiz, "Where can I find those?")
        say(
            npcSpeaker,
            neutral,
            "You'll be able to find all those ores in the rocks just inside the Dwarven Mine. Head " +
                "east from here and you'll find the entrance in the side of Ice Mountain.",
        )
        if (access.statBase("stat.mining") >= IRON_MINING) return
        chatPlayer(sad, "But I'm not a good enough miner to get iron ore.")
        say(
            npcSpeaker,
            neutral,
            "Oh well, you could practice mining until you can. Can't beat a bit of mining - it's a " +
                "useful skill. Failing that, you might be able to find a more experienced adventurer " +
                "to buy the iron ore off.",
        )
    }

    private suspend fun Dialogue.handIn() {
        for ((obj, count) in MATERIALS) {
            access.invDel(access.inv, obj, count)
        }
        npc?.anim("seq.human_dwarf_recieve")
        objbox(COPPER.first, "You hand the clay, copper, and iron to Doric.")
        quest.advanceQuestStageTo(access, COMPLETE)
    }

    private suspend fun Dialogue.say(
        npcSpeaker: Boolean,
        mesanim: MesAnimType,
        text: String,
    ) = if (npcSpeaker) chatNpc(mesanim, text) else chatDoric(mesanim, text)

    private suspend fun Dialogue.chatDoric(mesanim: MesAnimType, text: String) =
        chatNpcSpecific("Doric", "npc.doric", mesanim, text)

    private companion object {
        const val STARTED = 10
        const val COMPLETE = 100
        const val RECOMMENDED_MINING = 15
        const val IRON_MINING = 15

        val CLAY = "obj.clay" to 6
        val COPPER = "obj.copper_ore" to 4
        val IRON = "obj.iron_ore" to 2
        val MATERIALS = listOf(CLAY, COPPER, IRON)
    }
}
