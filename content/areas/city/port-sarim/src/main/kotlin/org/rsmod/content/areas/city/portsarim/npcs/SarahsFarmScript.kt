package org.rsmod.content.areas.city.portsarim.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SarahsFarmScript @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(SARAH) { startDialogue(it.npc) { sarah() } }
        onOpNpc3(SARAH) { player.openFarmingShop() }
        onOpNpc1("npc.dog") { petDog() }
    }

    private suspend fun Dialogue.sarah() {
        chatNpc(neutral, "Hello. How can I help you?")
        val topic =
            choice5(
                "What are you selling?",
                SarahTopic.Selling,
                "Can you give me any Farming advice?",
                SarahTopic.Advice,
                "Can you tell me how to use the loom?",
                SarahTopic.Loom,
                "That's a nice dog you have. What's its name?",
                SarahTopic.Dog,
                "I'm okay, thank you.",
                SarahTopic.Leave,
            )
        when (topic) {
            SarahTopic.Selling -> {
                chatPlayer(quiz, "What are you selling?")
                player.openFarmingShop()
            }
            SarahTopic.Advice -> {
                chatPlayer(quiz, "Can you give me any Farming advice?")
                chatNpc(neutral, "Yes - ask a gardener.")
            }
            SarahTopic.Loom -> loom()
            SarahTopic.Dog -> {
                chatPlayer(neutral, "That's a nice dog you have there. What's its name?")
                chatNpc(happy, "Oh that's Rosie, our resident sheepdog! She sure does like the fire.")
                chatNpc(neutral, "You can pet her if you like. She's very friendly.")
            }
            SarahTopic.Leave -> chatPlayer(neutral, "I'm okay, thank you.")
        }
    }

    private suspend fun Dialogue.loom() {
        chatPlayer(quiz, "Can you tell me how to use the loom?")
        chatNpc(
            neutral,
            "Well, it's actually my loom, but I don't mind you using it, if you like. You can use " +
                "it to weave sacks and baskets in which you can put vegetables and fruit.",
        )
        val topic =
            choice3(
                "What do I need to weave sacks?",
                LoomTopic.Sacks,
                "What do I need to weave baskets?",
                LoomTopic.Baskets,
                "Thank you, that's very kind.",
                LoomTopic.Thanks,
            )
        when (topic) {
            LoomTopic.Sacks -> {
                chatPlayer(quiz, "What do I need to weave sacks?")
                chatNpc(
                    neutral,
                    "Well, the best sacks are made with jute fibres; you can grow jute yourself in " +
                        "a hops patch. I'd say about 4 jute fibres should be enough to weave a sack.",
                )
            }
            LoomTopic.Baskets -> {
                chatPlayer(quiz, "What do I need to weave baskets?")
                chatNpc(
                    neutral,
                    "Well, the best baskets are made with young branches cut from a willow tree. " +
                        "You'll need a very young willow tree; otherwise, the branches will have " +
                        "grown too thick to be able to weave. I suggest growing your own.",
                )
                chatNpc(
                    neutral,
                    "You can cut the branches with a standard pair of secateurs. You will probably " +
                        "need about 6 willow branches to weave a complete basket.",
                )
            }
            LoomTopic.Thanks -> {}
        }
        chatPlayer(happy, "Thank you, that's very kind.")
    }

    private suspend fun ProtectedAccess.petDog() {
        arriveDelay()
        anim("seq.human_pickupfloor")
        delay(2)
        mes("He tries to lick your hands as you pet him.")
    }

    private fun Player.openFarmingShop() {
        shops.open(
            player = this,
            title = "Sarah's Farming shop.",
            shopInv = "inv.farming_shop_1",
            buyPercentage = 70.0,
            sellPercentage = 100.0,
            changePercentage = 1.0,
        )
    }

    private enum class SarahTopic {
        Selling,
        Advice,
        Loom,
        Dog,
        Leave,
    }

    private enum class LoomTopic {
        Sacks,
        Baskets,
        Thanks,
    }

    private companion object {
        const val SARAH = "npc.farming_shopkeeper_1"
    }
}
