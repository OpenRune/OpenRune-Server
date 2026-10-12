package org.rsmod.content.areas.city.seersvillage.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class SeersOutskirts @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.stankers") { startDialogue(it.npc) { stankers() } }
        onOpNpc1("npc.brother_galahad") { startDialogue(it.npc) { galahad() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Stankers
    private suspend fun Dialogue.stankers() {
        chatNpc(happy, "Hello bold adventurer.")
        when (choice2("Are these your trucks?", 1, "Hello Mr. Stankers.", 2)) {
            1 -> {
                chatPlayer(quiz, "Are these your trucks?")
                chatNpc(
                    happy,
                    "Yes, I use them to transport coal over the river. I will let other people " +
                        "use them too, I'm a nice person like that...",
                )
                chatNpc(
                    happy,
                    "Just put coal in a truck and I'll move it down to my depot over the river.",
                )
            }
            2 -> {
                chatPlayer(happy, "Hello Mr. Stankers.")
                chatNpc(happy, "Would you like a poison chalice?")
                val option =
                    choice3("Yes please.", 1, "What's a poison chalice?", 2, "No thank you.", 3)
                when (option) {
                    1 -> takeChalice()
                    2 -> {
                        chatPlayer(quiz, "What's a poison chalice?")
                        chatNpc(
                            happy,
                            "It's an exciting drink I've invented. I don't know what it tastes " +
                                "like, I haven't tried it myself.",
                        )
                        when (choice2("Yes please.", 1, "No thank you.", 2)) {
                            1 -> takeChalice()
                            2 -> chatPlayer(neutral, "No thank you.")
                        }
                    }
                    3 -> chatPlayer(neutral, "No thank you.")
                }
            }
        }
    }

    private suspend fun Dialogue.takeChalice() {
        chatPlayer(happy, "Yes please.")
        player.invAddOrDrop(objRepo, POISON_CHALICE)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Galahad
    private suspend fun Dialogue.galahad() {
        chatNpc(
            happy,
            "Welcome to my home. It's rare for me to have guests! Would you like a cup of tea? " +
                "I'll just put the kettle on.",
        )
        mesbox("Brother Galahad hangs a kettle over the fire.")
        val option =
            choice2(
                "Are you any relation to Sir Galahad?",
                1,
                "Do you get lonely here on your own?",
                2,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Are you any relation to Sir Galahad?")
                chatNpc(neutral, "I AM Sir Galahad.")
                chatNpc(
                    neutral,
                    "Although I've retired as a Knight, and now live as a solitary monk. Also, I " +
                        "prefer to be known as brother rather than sir now. Half a moment, your " +
                        "cup of tea is ready.",
                )
            }
            2 -> {
                chatPlayer(quiz, "Do you get lonely out here on your own?")
                chatNpc(
                    sad,
                    "Sometimes I do, yes. Still not many people to share my solidarity with, as " +
                        "most of the religious men around here are worshippers of Saradomin. " +
                        "Half a moment, your cup of tea is ready.",
                )
            }
        }
        player.invAddOrDrop(objRepo, CUP_OF_TEA)
        objbox(CUP_OF_TEA, "Sir Galahad gives you a cup of tea.")
    }

    private companion object {
        const val POISON_CHALICE = "obj.poison_chalice"
        const val CUP_OF_TEA = "obj.cup_of_tea"
    }
}
