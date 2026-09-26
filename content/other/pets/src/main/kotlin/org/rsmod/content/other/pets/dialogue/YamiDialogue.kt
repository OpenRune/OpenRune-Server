package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class YamiDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(2)) {
                0 -> {
                    chatNpc(angry, "Yes, you fool?")
                    chatPlayer(shocked, "Excuse me?")
                    chatNpc(
                        angry,
                        "You looked like you wanted to speak to me, you halfwit. You dullard.",
                    )
                    chatPlayer(angry, "That's enough.")
                    chatNpc(
                        angry,
                        "My contract stipulates that I must follow you, you insufferable ignoramus. " +
                            "My contract does not stipulate that I must be polite, you ignorant " +
                            "buffoon.",
                    )
                    chatPlayer(confused, "What have I ever done to you?")
                    chatNpc(
                        verymad,
                        "How about parading me around like a trophy, you idiot clown? How about " +
                            "stuffing me in your rotten backpack, you inclement cretin? How about-",
                    )
                    chatNpc(verymad, "-locking me in your house, you absolutely intolerable moron.")
                    chatPlayer(bored, "Okay, I get the idea.")
                    chatNpc(angry, "Good. Release me, then.")
                    chatPlayer(angry, "Why would I do that after all of that rudeness?")
                    chatNpc(verymad, "Bah!")
                }
                else -> {
                    chatNpc(bored, "I do not have the patience for you today.")
                    chatPlayer(neutral, "I just want to chat.")
                    chatNpc(
                        angry,
                        "You want to 'chat'? It is not enough that I must stare at your backside all " +
                            "day, I must suffer your sorry attempts at conversation as well?",
                    )
                    chatPlayer(angry, "Well if you're going to be rude, I'll find somebody else.")
                    chatNpc(
                        angry,
                        "Somebody else? Who? Introduce me to this paragon of virtue, who can tolerate " +
                            "the torrent of spittle you call speech. The incoherent messes you call " +
                            "sentences.",
                    )
                    chatPlayer(angry, "I have plenty of friends!")
                    chatNpc(
                        shifty,
                        "Oh yes, your friends. And do these friends, perhaps, live very far away? Are " +
                            "they perhaps too busy to meet up frequently?",
                    )
                    chatPlayer(worried, "Well...")
                    chatNpc(
                        angry,
                        "If anybody wanted to 'chat' with you, you wouldn't be speaking to somebody " +
                            "contractually obliged to do so.",
                    )
                }
            }
        }

    private companion object {
        const val NPC = "npc.yama_pet"
    }
}
