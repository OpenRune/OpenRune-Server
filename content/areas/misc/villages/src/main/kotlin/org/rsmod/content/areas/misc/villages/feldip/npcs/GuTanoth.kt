package org.rsmod.content.areas.misc.villages.feldip.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:watchtower] city guards, ogre guards and the skavid caves
class GuTanoth @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(DAL) { startDialogue(it.npc) { dal(it.npc) } }
        onOpNpc3(DAL) { player.openDal(it.npc) }
        onOpNpc1(GRUD) { startDialogue(it.npc) { grud(it.npc) } }
        onOpNpc3(GRUD) { player.openGrud(it.npc) }
        onOpNpc1("npc.ogre_chieftan") { startDialogue(it.npc) { chieftain() } }
        onOpNpc1(FOOD_TRADER) { startDialogue(it.npc) { foodStall() } }
        onOpNpc3(FOOD_TRADER) { startDialogue(it.npc) { foodStall() } }
        onOpNpc1(EMPTY_STALL_TRADER) { startDialogue(it.npc) { emptyStallTalk() } }
        onOpNpc3(EMPTY_STALL_TRADER) { startDialogue(it.npc) { emptyStallTrade() } }
        onOpNpc1("npc.enclave_guard") { startDialogue(it.npc) { enclaveGuard() } }
    }

    private fun Player.openDal(npc: Npc) {
        shops.open(this, npc, "Dal's General Ogre Supplies", "inv.generalshopogre")
    }

    private fun Player.openGrud(npc: Npc) {
        shops.open(this, npc, "Grud's Herblore Stall.", "inv.herbloreshopogre")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ogre_trader_(Dal)
    private suspend fun Dialogue.dal(npc: Npc) {
        chatNpc(neutral, "What the human be wantin'?")
        when (choice2("Can I see what you are selling?", 1, "I don't need anything.", 2)) {
            1 -> {
                chatPlayer(quiz, "Can I see what you are selling?")
                chatNpc(neutral, "I suppose so.")
                player.openDal(npc)
            }
            2 -> {
                chatPlayer(neutral, "I don't need anything.")
                chatNpc(neutral, "As you wish.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ogre_merchant
    private suspend fun Dialogue.grud(npc: Npc) {
        chatNpc(neutral, "Does the little creature want to buy sumfin'?")
        when (choice2("Yes I do.", 1, "No I don't.", 2)) {
            1 -> {
                chatPlayer(happy, "Yes I do.")
                chatNpc(neutral, "Welcome to Grud's Herblore Stall.")
                player.openGrud(npc)
            }
            2 -> {
                chatPlayer(neutral, "No, I don't.")
                chatNpc(neutral, "Suit yourself.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ogre_chieftain
    private suspend fun Dialogue.chieftain() {
        chatNpc(angry, "Grrr! Go away from me, vermin!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ogre_trader
    // TODO(content): [verify:in_game] which of the two ogre traders stands at the food stall
    private suspend fun Dialogue.foodStall() {
        chatNpc(
            angry,
            "Arr, small thing wants my food, does it? I'll teach you to deal with ogres!",
        )
    }

    private suspend fun Dialogue.emptyStallTalk() {
        chatNpc(angry, "Grrr, little animal; I shall destroy you!")
    }

    private suspend fun Dialogue.emptyStallTrade() {
        chatNpc(angry, "Trade with me? Who do you think you are?")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Enclave_guard
    // TODO(content): [system:npc_aggression] the enclave guard attacks the player after replying
    private suspend fun Dialogue.enclaveGuard() {
        chatNpc(angry, "What do you want?")
        when (choice2("I want to go in there.", 1, "I want to rid the world of ogres!", 2)) {
            1 -> {
                chatPlayer(neutral, "I want to go in there.")
                chatNpc(angry, "Oh you do, do you? How about 'no'?")
            }
            2 -> {
                chatPlayer(angry, "I want to rid the world of ogres.")
                chatNpc(angry, "You dare mock me, creature!")
            }
        }
    }

    private companion object {
        const val DAL = "npc.ogre_trader1"
        const val GRUD = "npc.ogre_merchant"
        const val FOOD_TRADER = "npc.ogre_trader2"
        const val EMPTY_STALL_TRADER = "npc.falseogretrader"
    }
}
