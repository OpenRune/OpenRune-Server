package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class TanglerootDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(BASE, "Talk-to") { talk(it, 3) }
        onPetOp(CRYSTAL, "Talk-to") { talk(it, 4) { talkCrystal() } }
        onPetOp(DRAGONFRUIT, "Talk-to") { talk(it, 4) { talkDragonfruit() } }
        onPetOp(HERB, "Talk-to") { talk(it, 4) { talkHerb() } }
        onPetOp(LILY, "Talk-to") { talk(it, 4) { talkLily() } }
        onPetOp(REDWOOD, "Talk-to") { talk(it, 4) { talkRedwood() } }
    }

    private suspend fun ProtectedAccess.talk(
        npc: Npc,
        branches: Int,
        variant: suspend Dialogue.() -> Unit = {},
    ) = startDialogue(npc) {
        when (access.random.of(branches)) {
            0 -> {
                chatPlayer(quiz, "How are you doing today?")
                chatNpc(happy, "I am Tangleroot!")
            }
            1 -> {
                chatPlayer(happy, "Hello there pretty plant.")
                chatNpc(happy, "I am Tangleroot!")
            }
            2 -> {
                chatPlayer(happy, "I am Tangleroot!")
                chatNpc(happy, "I am ${player.displayName}!")
            }
            else -> variant()
        }
    }

    private suspend fun Dialogue.talkCrystal() {
        chatPlayer(happy, "Everything is crystal clear now.")
        chatNpc(happy, "I am Tangleroot!")
    }

    private suspend fun Dialogue.talkDragonfruit() {
        chatPlayer(happy, "I can't believe they made dragonfruit trees into a real thing!")
        chatNpc(quiz, "I am Tangleroot?")
        chatPlayer(neutral, "Nothing.")
    }

    private suspend fun Dialogue.talkHerb() {
        chatPlayer(quiz, "Are you related to Herbiboar now?")
        chatNpc(neutral, "I am Tangleroot.")
        chatPlayer(neutral, "I should have guessed.")
    }

    private suspend fun Dialogue.talkLily() {
        chatPlayer(happy, "I love your new hair cut!")
        chatNpc(happy, "I am Tangleroot!")
    }

    private suspend fun Dialogue.talkRedwood() {
        chatPlayer(shocked, "Oh dear, you've gone all red!")
        chatNpc(quiz, "I am Tangleroot?")
        chatPlayer(neutral, "It must be something you ate.")
    }

    private companion object {
        const val BASE = "npc.skillpet_farming"
        const val CRYSTAL = "npc.skillpet_farming_crystal"
        const val DRAGONFRUIT = "npc.skillpet_farming_dragon"
        const val HERB = "npc.skillpet_farming_herb"
        const val LILY = "npc.skillpet_farming_lily"
        const val REDWOOD = "npc.skillpet_farming_redwood"
    }
}
