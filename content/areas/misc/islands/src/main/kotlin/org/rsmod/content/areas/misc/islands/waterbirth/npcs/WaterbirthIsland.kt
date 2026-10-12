package org.rsmod.content.areas.misc.islands.waterbirth.npcs

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:the_fremennik_trials] after-quest dialogue, using Fremennik names
class WaterbirthIsland : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.viking_dagganoth_cave_waterbirth_island") {
            startDialogue(it.npc) { jarvald() }
        }
        onOpNpc1("npc.viking_askelapen_dagganoth_mountain") {
            startDialogue(it.npc) { askeladden() }
        }
        onOpNpc1("npc.jb_viking_dude") { startDialogue(it.npc) { bardur() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jarvald (Waterbirth Island)
    // TODO(content): [system:ship_travel] sailing back to Rellekka and the Rellekka option
    private suspend fun Dialogue.jarvald() {
        chatNpc(happy, "Ah, you live yet, outerlander!")
        chatNpc(
            quiz,
            "Have you had your fill of the hunt and wish to return, or are you still feeling the " +
                "joy of the cull?",
        )
        val option =
            choice2(
                "I wish to return to Rellekka.",
                1,
                "I want to stay here.",
                2,
                title = "Select an Option",
            )
        when (option) {
            1 -> {
                chatPlayer(neutral, "I wish to return to Rellekka.")
                chatNpc(happy, "Then let us away; There will be death to bring here another day!")
            }
            2 -> {
                chatPlayer(happy, "I want to stay here.")
                chatNpc(laugh, "Ha Ha Ha! A true huntsman at heart!")
                chatNpc(
                    happy,
                    "I myself have killed over a hundred of the dagger- mouths, and did not " +
                        "think it too many!",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Askeladden (Waterbirth Island, before The
    // Fremennik Trials)
    // TODO(content): [system:askeladden_trades] trading dagannoth hides, rock-shell pieces,
    // wallasalki bones and lightened hides for cooked tuna, and using them on him
    private suspend fun Dialogue.askeladden() {
        chatPlayer(happy, "Hello there.")
        chatNpc(happy, "Hey there, buddy. You're an outerlander, huh?")
        chatNpc(
            happy,
            "Didn't think old Jarvald would let an outerlander come kill dagger-mouths with him!",
        )
        chatPlayer(neutral, "Well, obviously he did. So what are you doing up here?")
        chatNpc(
            happy,
            "Well buddy, let me tell you, killing those dagger-mouths is sure thirsty work!",
        )
        chatNpc(
            happy,
            "If you got a keg of beer on you I'll buy it off ya for 500 gold coins, buddy!",
        )
        chatNpc(happy, "Or if you got any animal parts, I'll trade ya something for 'em!")
        if (KEG_OF_BEER !in player.inv) {
            chatPlayer(sad, "No... I don't have anything like that with me...")
            chatNpc(
                sad,
                "Well, I'm not going back down there until I get a drink buddy! Come see me if " +
                    "you find me a keg, I pay good money!",
            )
            return
        }
        chatNpc(quiz, "Wanna sell me ya kegs of beer for 500 gold coins each outerlander?")
        chatPlayer(worried, "I don't know... Seems a little off to sell alcohol to a child...")
        chatNpc(
            angry,
            "Hey, don't judge our customs outerlander! Now you want the cash or you want to bug " +
                "me?",
        )
        val sell =
            choice2("YES", true, "NO", false, title = "Would you like to sell your kegs of beer?")
        if (!sell) {
            return
        }
        chatNpc(quiz, "You wanna sell me the lot buddy, or one at a time?")
        val sellAll =
            choice2("Sell all Kegs", true, "Sell One Keg", false, title = "Select an Option")
        if (sellAll) {
            if (sellKegs(player.inv.count(KEG_OF_BEER))) {
                player.mes("You sell Askeladden your Kegs of Beer.")
            }
            return
        }
        while (true) {
            if (!sellKegs(1)) {
                return
            }
            mesbox("You sell Askeladden a Keg of Beer.")
            if (!choice2("YES", true, "NO", false, title = "Sell another Keg?")) {
                return
            }
            if (KEG_OF_BEER !in player.inv) {
                chatPlayer(neutral, "But I don't have any kegs of beer to sell...")
                return
            }
        }
    }

    private fun Dialogue.sellKegs(count: Int): Boolean {
        if (count <= 0) {
            return false
        }
        val result =
            player.invTransaction(player.inv) {
                val inv = select(player.inv)
                delete {
                    from = inv
                    obj = KEG_OF_BEER.asRSCM(RSCMType.OBJ)
                    strictCount = count
                }
                insert {
                    into = inv
                    obj = COINS.asRSCM(RSCMType.OBJ)
                    strictCount = count * KEG_PRICE
                }
            }
        return result.success
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bardur (before The Fremennik Trials)
    // TODO(content): [quest:the_fremennik_trials] trading Fremennik equipment for sharks
    private suspend fun Dialogue.bardur() {
        chatNpc(
            angry,
            "Ah! Outerlander! Do not interrupt me in my business! I must cull these fiends!",
        )
        chatPlayer(quiz, "What are you doing down here?")
        chatNpc(
            angry,
            "Only an outerlander would ask such a question! Is it not obvious what I am doing?",
        )
        chatNpc(
            angry,
            "I kill the daggermouths for glory and for sport! When I have had my fill, I move on " +
                "to the daggermouths' lair!",
        )
        chatPlayer(neutral, "Erm... okay then. I'll leave you to it.")
    }

    private companion object {
        const val KEG_OF_BEER = "obj.keg_of_beer"
        const val COINS = "obj.coins"
        const val KEG_PRICE = 500
    }
}
