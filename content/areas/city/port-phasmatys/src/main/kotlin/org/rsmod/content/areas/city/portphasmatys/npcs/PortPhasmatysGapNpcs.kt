package org.rsmod.content.areas.city.portphasmatys.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class PortPhasmatysGapNpcs @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(ALICE) { startDialogue(it.npc) { alice(it.npc) } }
        onOpNpc3(ALICE) { player.openFarmingShop(it.npc) }
        onOpNpc3(AK_HARANU) {
            shops.open(player, it.npc, "Ak-Haranu's Exotic Shop.", "inv.ahoy_akharanu_shop")
        }
        onOpNpc1("npc.ahoy_oldman") { startDialogue(it.npc) { oldMan() } }
        onOpNpc1("npc.sarah_corvo") { startDialogue(it.npc) { sarah() } }
    }

    private fun Player.openFarmingShop(npc: Npc) {
        shops.open(this, npc, "Alice's Farming shop.", "inv.farming_shop_1")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Alice
    private suspend fun Dialogue.alice(npc: Npc) {
        if (QuestRequirements.hasCompleted(player, ANIMAL_MAGNETISM)) {
            chatNpc(
                happy,
                "Hello, friend, and thanks for allowing me to talk to my husband Malcolm. How " +
                    "can I help you?",
            )
        } else {
            chatNpc(happy, "Hello. How can I help you?")
        }
        val option =
            choice3(
                "What are you selling?",
                1,
                "Can you give me any Farming advice?",
                2,
                "I'm okay, thank you.",
                3,
            )
        when (option) {
            1 -> player.openFarmingShop(npc)
            2 -> {
                chatPlayer(quiz, "Can you give me any Farming advice?")
                chatNpc(neutral, "Yes - ask a gardener.")
            }
            3 -> chatPlayer(neutral, "I'm okay, thank you.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Old_man
    // TODO(content): [quest:ghosts_ahoy] the in-quest dialogue
    private suspend fun Dialogue.oldMan() {
        if (QuestRequirements.hasCompleted(player, GHOSTS_AHOY)) {
            chatPlayer(quiz, "How is it going?")
            chatNpc(happy, "Wonderful, wonderful! Mother's coming to get me!")
            return
        }
        chatPlayer(quiz, "What are you doing on this shipwreck?")
        chatNpc(shocked, "Shipwreck?!? Not shipwreck, surely not! Just in port, that's all!")
        chatPlayer(confused, "Don't be silly - half the ship's missing!")
        chatNpc(
            neutral,
            "No no no - the captain's just waiting for the wind to change, then we're off!",
        )
        chatPlayer(confused, "You mean this skeleton sitting here in this chair?")
        chatNpc(angry, "You must show more respect to the Captain.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sarah_(Port_Phasmatys)
    // TODO(content): [quest:dragon_slayer_ii] the in-quest dialogue
    // TODO(content): [verify:in_game] whether Sarah needs a ghostspeak amulet like the other ghosts
    private suspend fun Dialogue.sarah() {
        if (!player.understandsGhosts()) {
            return wail()
        }
        if (QuestRequirements.hasCompleted(player, GHOSTS_AHOY)) {
            chatNpc(
                happy,
                "You're the one who stopped Necrovarus. Our people are forever grateful to you, " +
                    "even if we don't always show it.",
            )
            return
        }
        chatNpc(neutral, "What do you want, mortal?")
        chatPlayer(confused, "I'm not sure - what can you offer?")
        chatNpc(neutral, "Not much, I'm dead.")
        chatPlayer(neutral, "Fair enough.")
    }

    private companion object {
        const val ALICE = "npc.farming_shopkeeper_4"
        const val AK_HARANU = "npc.ahoy_akharanu"
        const val ANIMAL_MAGNETISM = "quest_animalmagnetism"
        const val GHOSTS_AHOY = "quest_ghostsahoy"
    }
}
