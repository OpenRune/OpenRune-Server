package org.rsmod.content.areas.misc.villages.piscatoris.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onOpNpc5
import org.rsmod.api.shops.Shops
import org.rsmod.content.interfaces.bank.tryOpenBank
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Arnold_Lydspor
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:treasure_trails] hard clue puzzle box
class ArnoldLydspor
@Inject
constructor(private val shops: Shops, private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(ARNOLD) { startDialogue(it.npc) { arnold(it.npc) } }
        onOpNpc3(ARNOLD) { player.openShop(it.npc) }
        onOpNpc4(ARNOLD) { tryOpenBank() }
        onOpNpc5(ARNOLD) { openCollectionBox() }
    }

    private fun Player.openShop(npc: Npc) {
        shops.open(this, npc, "Arnold's Eclectic Supplies.", "inv.skill_guide_crafting_leather")
    }

    private fun ProtectedAccess.openCollectionBox() {
        ifOpenMainModal("interface.ge_collect")
    }

    private suspend fun Dialogue.arnold(npc: Npc) {
        chatNpc(happy, "Ah, you come back! What you want from Arnold, heh?")
        val option =
            choice5(
                "Can you open my bank account, please?",
                1,
                "I'd like to check my Bank PIN settings.",
                2,
                "I'd like to collect items.",
                3,
                "Would you like to trade?",
                4,
                "Nothing, I just came to chat.",
                5,
            )
        when (option) {
            1 -> access.tryOpenBank()
            2 -> access.ifOpenMainModal("interface.bankpin_settings")
            3 -> access.openCollectionBox()
            4 -> {
                chatPlayer(quiz, "Would you like to trade?")
                chatNpc(happy, "Ja, I have wide range of stock...")
                player.openShop(npc)
            }
            5 -> {
                chatPlayer(happy, "Nothing, I just came to chat.")
                chatNpc(
                    happy,
                    "Ah, that is nice - always I like to chat, but Herr Caranos tell me to get " +
                        "back to work! Here, you been nice, so have a present:",
                )
                player.invAddOrDrop(objRepo, CABBAGE)
                chatPlayer(confused, "A cabbage?")
                chatNpc(happy, "Ja, cabbage is good for you!")
                chatPlayer(neutral, "Um... thanks!")
            }
        }
    }

    private companion object {
        const val ARNOLD = "npc.swan_arnold"
        const val CABBAGE = "obj.cabbage"
    }
}
