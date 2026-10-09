package org.rsmod.content.quest.area.baxtorianfalls.waterfall.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.GOLRIE_KEY
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.GOLRIE_NPC
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.PEBBLE
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.metGolrie
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.ownsAnywhere
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Golrie
@Inject
constructor(private val waterfall: WaterfallQuest, private val objRepo: ObjRepository) :
    PluginScript() {

    override fun ScriptContext.startup() {
        onOpNpc1(GOLRIE_NPC) { startDialogue(it.npc) { golrie() } }
    }

    private suspend fun Dialogue.golrie() {
        when {
            waterfall.stage(player) == 0 -> {
                chatNpc(
                    angry,
                    "What are you doing down here? Leave before you get yourself into trouble.",
                )
            }
            !player.metGolrie -> firstMeeting()
            player.ownsAnywhere(PEBBLE) -> {
                chatPlayer(happy, "Hello, Golrie.")
                chatNpc(happy, "Hello again.")
                chatPlayer(quiz, "Any luck getting out?")
                chatNpc(
                    neutral,
                    "Not yet, but don't worry. I'm sure I'll work something out. I just need " +
                        "some time to think.",
                )
                chatPlayer(happy, "Well good luck.")
            }
            else -> {
                chatPlayer(happy, "Hello, Golrie.")
                chatNpc(happy, "Hello again.")
                chatPlayer(quiz, "Do you mind if I have another look through this stuff?")
                chatNpc(happy, "No, of course not.")
                findPebble(takeKey = false)
            }
        }
    }

    private suspend fun Dialogue.firstMeeting() {
        chatPlayer(quiz, "Who are you, and what are you doing here?")
        chatNpc(
            neutral,
            "I'm Golrie, and this is my home. Thing is, as you can see, I've got a small " +
                "problem. Those hob-gobs are all over the place trying to steal my family's " +
                "heirlooms. I've been stuck here for ages!",
        )
        chatPlayer(quiz, "Do you need some help?")
        chatNpc(
            happy,
            "Oh don't worry, I'm sure I'll work something out. I just need some time to think.",
        )
        chatPlayer(quiz, "In that case, do you mind if I have a look around?")
        chatNpc(happy, "No, of course not.")
        val handedKey = GOLRIE_KEY in player.inv
        if (!findPebble(takeKey = true)) {
            return
        }
        chatPlayer(quiz, "Could I take this old pebble?")
        chatNpc(neutral, "Oh that. Yes, have it. It's just some old elven junk I believe.")
        if (handedKey) {
            mesbox("You give Golrie the key.")
            chatNpc(happy, "Ah, thanks a lot for the key, traveller.")
        }
        chatPlayer(happy, "No problem. Take care, Golrie.")
    }

    /** The pebble, the key hand-in and the met flag change together, before any dialogue line. */
    private suspend fun Dialogue.findPebble(takeKey: Boolean): Boolean {
        val keyTaken = takeKey && access.invDel(access.inv, GOLRIE_KEY).success
        if (player.inv.isFull()) {
            mesbox(
                "You look amongst the junk on the floor and find Glarial's pebble but you " +
                    "don't have enough room to take it."
            )
            if (keyTaken) {
                access.invAddOrDrop(objRepo, GOLRIE_KEY)
            }
            return false
        }
        access.invAddOrDrop(objRepo, PEBBLE)
        player.metGolrie = true
        mesbox("You look amongst the junk on the floor and find Glarial's pebble.")
        return true
    }
}
