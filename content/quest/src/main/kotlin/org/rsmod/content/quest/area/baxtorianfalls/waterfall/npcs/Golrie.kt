package org.rsmod.content.quest.area.baxtorianfalls.waterfall.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.GolrieKey
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.GolrieNpc
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.Pebble
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.ReadBook
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.metGolrie
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.ownsAnywhere
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Golrie, the gnome who locked himself in his store room beneath the Tree Gnome Village to keep
 * the hobgoblins away from his family's heirlooms.
 */
class Golrie
@Inject
constructor(private val waterfall: WaterfallQuest, private val objRepo: ObjRepository) :
    PluginScript() {

    override fun ScriptContext.startup() {
        onOpNpc1(GolrieNpc) { startDialogue(it.npc) { golrie() } }
    }

    private suspend fun Dialogue.golrie() {
        when {
            waterfall.stage(player) < ReadBook -> {
                chatNpc(
                    angry,
                    "What are you doing down here? Leave before you land yourself in trouble.",
                )
            }
            !player.metGolrie -> firstMeeting()
            player.ownsAnywhere(Pebble) -> {
                chatPlayer(happy, "Hello, Golrie.")
                chatNpc(happy, "Hello again.")
                chatPlayer(quiz, "Had any luck getting out?")
                chatNpc(
                    neutral,
                    "Not yet, but don't you worry. I'll think of something, I just need a " +
                        "little more time.",
                )
                chatPlayer(happy, "Well, good luck.")
            }
            else -> {
                chatPlayer(happy, "Hello, Golrie.")
                chatNpc(happy, "Hello again.")
                chatPlayer(quiz, "Mind if I have another rummage through this stuff?")
                chatNpc(happy, "Not at all.")
                findPebble(takeKey = false)
            }
        }
    }

    private suspend fun Dialogue.firstMeeting() {
        chatPlayer(quiz, "Who are you, and what are you doing down here?")
        chatNpc(
            neutral,
            "I'm Golrie, and this is my home. The trouble is, those hob-gobs keep trying to " +
                "pinch my family's heirlooms, so I've been shut in here for ages!",
        )
        chatPlayer(quiz, "Can I help at all?")
        chatNpc(
            happy,
            "Oh, don't worry about me. I'll figure something out, I just need time to think.",
        )
        chatPlayer(quiz, "In that case, would you mind if I had a look around?")
        chatNpc(happy, "Of course not.")
        val handedKey = GolrieKey in player.inv
        if (!findPebble(takeKey = true)) {
            return
        }
        chatPlayer(quiz, "Could I have this old pebble?")
        chatNpc(neutral, "That? Help yourself. It's just a bit of old elven junk, I think.")
        if (handedKey) {
            objbox(GolrieKey, "You hand Golrie the key.")
            chatNpc(happy, "Ah, thank you for bringing my key back, traveller.")
        }
        chatPlayer(happy, "No problem. Look after yourself, Golrie.")
    }

    /** The pebble, the key hand-in and the met flag change together, before any dialogue line. */
    private suspend fun Dialogue.findPebble(takeKey: Boolean): Boolean {
        val keyTaken = takeKey && access.invDel(access.inv, GolrieKey).success
        if (player.inv.isFull()) {
            objbox(
                Pebble,
                "Among the junk on the floor you spot Glarial's pebble, but you don't have " +
                    "room to carry it.",
            )
            if (keyTaken) {
                access.invAddOrDrop(objRepo, GolrieKey)
            }
            return false
        }
        access.invAddOrDrop(objRepo, Pebble)
        player.metGolrie = true
        objbox(Pebble, "Among the junk on the floor you find Glarial's pebble.")
        return true
    }
}
