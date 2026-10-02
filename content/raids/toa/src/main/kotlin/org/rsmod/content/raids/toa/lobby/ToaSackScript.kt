package org.rsmod.content.raids.toa.lobby

import dev.openrune.types.MesAnimType
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.ironman.isUltimateIronman
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ToaSackScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(LobbyLocs.SACK) { searchSack() }
    }

    private suspend fun ProtectedAccess.searchSack() {
        if (!inv.hasFreeSpace() || !findsSomething()) {
            caught()
            return
        }
        if (random.randomBoolean()) findNeedle() else findGrain()
    }

    private fun ProtectedAccess.findsSomething(): Boolean =
        statRandom(LobbySack.STAT, LobbySack.LOW, LobbySack.HIGH, invisibleBoost = 0)

    private suspend fun ProtectedAccess.caught() {
        if (LobbyObjs.CAMULET !in worn) {
            mesbox(
                "You go to search the sack, but the bank camel glares at you menacingly and " +
                    "spits in your direction."
            )
            return
        }
        startDialogue { camelScolds() }
    }

    private suspend fun Dialogue.camelScolds() {
        camel(
            angry,
            "Hey, get your hands off my food! Unless you'd like me to start eating the " +
                "contents of your bank instead!",
        )
        if (!player.isUltimateIronman) return
        chatPlayer(laugh, "Jokes on you! I don't have a bank!")
        camel(
            angry,
            "Well I hope you're very proud of yourself. Your arbitrary restrictions don't " +
                "make you better than everyone else, you know.",
        )
        chatPlayer(sad, "Wow, that's harsh.")
    }

    private suspend fun ProtectedAccess.findNeedle() {
        invAdd(inv, LobbyObjs.NEEDLE)
        startDialogue {
            objbox(LobbyObjs.NEEDLE, "You search the sack and find a needle.")
            chatPlayer(shocked, "Wow! A needle in a hay sack?")
            chatPlayer(confused, "Wait, isn't it supposed to be a stack, not a sack?")
            chatPlayer(
                confused,
                "And now that I think about it, this sack is full of grain, not hay...",
            )
            chatPlayer(neutral, "Ah well, never mind.")
        }
    }

    private suspend fun ProtectedAccess.findGrain() {
        invAdd(inv, LobbyObjs.GRAIN)
        startDialogue {
            objbox(
                LobbyObjs.GRAIN,
                "You successfully take some grain while the bank camel isn't looking.",
            )
            chatPlayer(neutral, "It's just grain...")
            chatPlayer(neutral, "Well, I'm not entirely sure what else I expected.")
        }
    }

    private suspend fun Dialogue.camel(mesanim: MesAnimType, text: String) {
        chatNpcSpecific(LobbySack.CAMEL_TITLE, LobbyNpcs.BANK_CAMEL, mesanim, text)
    }
}
