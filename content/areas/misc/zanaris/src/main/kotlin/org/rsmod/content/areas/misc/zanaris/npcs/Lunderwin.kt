package org.rsmod.content.areas.misc.zanaris.npcs

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Lunderwin
// https://oldschool.runescape.wiki/w/Lunderwin: buys both cabbage types for 100 coins each.
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class Lunderwin : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.fairy_lunderwin") { startDialogue(it.npc) { lunderwin() } }
    }

    private suspend fun Dialogue.lunderwin() {
        chatNpc(
            happy,
            "Buying cabbage am I, not have such thing where I from. Will pay money much handsome " +
                "for wondrous object, cabbage you called.",
        )
        chatNpc(happy, "Say I 100 gold coins each fair price to be giving yes?")
        val cabbages = access.invTotal(access.inv, CABBAGE)
        val magicCabbages = access.invTotal(access.inv, MAGIC_CABBAGE)
        if (cabbages + magicCabbages == 0) {
            chatPlayer(sad, "Alas, I have no cabbages either...")
            chatNpc(sad, "Pity be that, I want badly do.")
            return
        }
        val option =
            choice2(
                "Yes, I will sell you all my cabbages.",
                1,
                "No, I will keep my cabbbages.",
                2,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "Yes, I'll sell you all my cabbages.")
                if (sellCabbages(cabbages, magicCabbages)) {
                    chatNpc(
                        happy,
                        "Business good doing with you! Please, again come, buying always.",
                    )
                }
            }
            2 -> chatPlayer(neutral, "No, I will keep my cabbbages.")
        }
    }

    private fun Dialogue.sellCabbages(cabbages: Int, magicCabbages: Int): Boolean {
        val result =
            player.invTransaction(player.inv) {
                val inv = select(player.inv)
                for ((obj, count) in listOf(CABBAGE to cabbages, MAGIC_CABBAGE to magicCabbages)) {
                    if (count == 0) {
                        continue
                    }
                    delete {
                        from = inv
                        this.obj = obj.asRSCM(RSCMType.OBJ)
                        strictCount = count
                    }
                }
                insert {
                    into = inv
                    obj = COINS.asRSCM(RSCMType.OBJ)
                    strictCount = (cabbages + magicCabbages) * PRICE
                }
            }
        return result.success
    }

    private companion object {
        const val CABBAGE = "obj.cabbage"
        const val MAGIC_CABBAGE = "obj.magic_cabbage"
        const val COINS = "obj.coins"
        const val PRICE = 100
    }
}
