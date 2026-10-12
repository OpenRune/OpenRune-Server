package org.rsmod.content.areas.city.rellekka.rellekka.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.areas.city.rellekka.choose
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class LarryRellekka @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.peng_larry_rell") { startDialogue(it.npc) { larry() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Larry (At Rellekka boat)
    private suspend fun Dialogue.larry() {
        if (QuestRequirements.hasCompleted(player, COLD_WAR)) {
            afterColdWar()
            return
        }
        chatNpc(angry, "What? Did you come to tell me I'm crazy too?")
        chatPlayer(neutral, "No, I...")
        chatNpc(angry, "Oh, I don't have time for your lies. Go away")
    }

    // TODO(content): [system:ship_travel] the boat to the iceberg
    private suspend fun Dialogue.afterColdWar() {
        chatNpc(
            neutral,
            "I can't speak right now. I have too much to do. I will contact you when the time " +
                "is right.",
        )
        val options = mutableListOf("I'd like to travel to the iceberg." to Topic.Iceberg)
        if (!access.playerContainsObj(CLOCKWORK_BOOK)) {
            options += "I've lost the clockwork book, could I have another?" to Topic.Book
        }
        when (choose(options)) {
            Topic.Iceberg -> {
                chatNpc(
                    neutral,
                    "On the way I'll tell you the 42 ways penguins can torture you using a " +
                        "feather...",
                )
            }
            Topic.Book -> {
                chatPlayer(sad, "I've lost the clockwork book, could I have another?")
                access.invAddOrDrop(objRepo, CLOCKWORK_BOOK)
                chatNpc(
                    neutral,
                    "Well, lucky for you I did make another copy. But be more careful!",
                )
            }
        }
    }

    private enum class Topic {
        Iceberg,
        Book,
    }

    private companion object {
        const val COLD_WAR = "quest_coldwar"
        const val CLOCKWORK_BOOK = "obj.peng_book"
    }
}
