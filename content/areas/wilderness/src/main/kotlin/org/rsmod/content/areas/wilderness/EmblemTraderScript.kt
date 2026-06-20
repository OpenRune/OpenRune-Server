package org.rsmod.content.areas.wilderness

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc5
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class EmblemTraderScript @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.emblem_trader") { startDialogue(it.npc) }
        onOpNpc5("npc.emblem_trader") { startDialogue(it.npc) { requestPkSkull() } }
    }

    private suspend fun ProtectedAccess.startDialogue(npc: Npc) {
        startDialogue(npc) { emblemTraderDialogue(npc) }
    }

    private suspend fun Dialogue.emblemTraderDialogue(npc: Npc) {
        chatNpc(
            neutral,
            "Don't suppose you've come across any strange emblems or artefacts along your journey? " +
                "Ancient artefacts?",
        )
        chatPlayer(neutral, "Nothing to report now.")
        chatNpc(
            neutral,
            "If you find any, please bring them to me. I am happy to offer rewards for such items.",
        )
        showMainOptions()
    }

    private suspend fun Dialogue.showMainOptions() {
        if (!player.isSkulled()) {
            when (
                choice3(
                    "Let's trade for rewards.",
                    1,
                    "Can I have a PK skull, please?",
                    2,
                    "I'll leave you alone.",
                    3,
                )
            ) {
                1 -> tradeRewards()
                2 -> requestPkSkull()
            }
            return
        }

        if (player.isSkullEquipLocked()) {
            when (choice2("Let's trade for rewards.", 1, "I'll leave you alone.", 2)) {
                1 -> tradeRewards()
            }
            return
        }

        when (
            choice3(
                "Let's trade for rewards.",
                1,
                "Can you make my PK skull last longer?",
                2,
                "I'll leave you alone.",
                3,
            )
        ) {
            1 -> tradeRewards()
            2 -> extendPkSkull()
        }
    }

    private suspend fun Dialogue.tradeRewards() {
        chatPlayer(neutral, "Let's trade for rewards.")
        chatNpc(neutral, "I don't carry rewards out here, it is far too dangerous.")
        chatNpc(neutral, "Meet me in Daimon's Crater if you would like to buy rewards.")
    }

    private suspend fun Dialogue.requestPkSkull() {
        chatPlayer(neutral, "Can I have a PK skull, please?")
        mesbox("A PK skull means you drop ALL your items on death.")
        when (choice2("Give me a PK skull.", 1, "Cancel.", 2)) {
            1 -> {
                player.applyVoluntarySkull()
                access.mes("You are now skulled.")
            }
        }
    }

    private suspend fun Dialogue.extendPkSkull() {
        chatPlayer(neutral, "Can you make my PK skull last longer?")
        if (player.extendEmblemTraderSkull()) {
            access.mes("Your PK skull will now last for the full 20 minutes.")
        } else {
            chatNpc(neutral, "Sorry, I can't extend your PK skull beyond its existing duration.")
        }
    }
}
