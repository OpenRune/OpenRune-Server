package org.rsmod.content.areas.misc.islands.waterbirth.npcs

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class WaterbirthIsland : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.viking_dagganoth_cave_waterbirth_island") {
            startDialogue(it.npc) { jarvald() }
        }
        onOpNpc1("npc.viking_askelapen_dagganoth_mountain") {
            startDialogue(it.npc) { askeladden() }
        }
        onOpNpc1(BARDUR) { startDialogue(it.npc) { bardur() } }
        onOpNpcU(BARDUR) { bardurUse(it.npc, it.objType) }
    }

    private val Dialogue.fremennikDone: Boolean
        get() = QuestRequirements.hasCompleted(player, FREMENNIK_TRIALS)

    // TODO(content): [system:fremennik_name] the player's Fremennik name from The Fremennik
    // Trials; until then this placeholder fills the [Fremennik name] slot
    private fun fremennikName(): String = "outerlander"

    // https://oldschool.runescape.wiki/w/Transcript:Jarvald (Waterbirth Island)
    // TODO(content): [system:ship_travel] sailing back to Rellekka and the Rellekka option
    private suspend fun Dialogue.jarvald() {
        if (fremennikDone) {
            chatNpc(
                happy,
                "Ah, ${fremennikName()}! Such glorious battle makes you feel glad of life, does " +
                    "it not?",
            )
            chatNpc(
                quiz,
                "So what say you, stay here for the hunt, or return home to sweet Rellekka to " +
                    "feast and drink with your tribe?",
            )
        } else {
            chatNpc(happy, "Ah, you live yet, outerlander!")
            chatNpc(
                quiz,
                "Have you had your fill of the hunt and wish to return, or are you still feeling " +
                    "the joy of the cull?",
            )
        }
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
        if (fremennikDone) {
            askeladdenAfterQuest()
            return
        }
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
        sellKegsOffer(KEG_PRICE)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Askeladden (Waterbirth Island, after The
    // Fremennik Trials)
    private suspend fun Dialogue.askeladdenAfterQuest() {
        chatNpc(happy, "Hi buddy! Or do ya want me to call ya ${fremennikName()} now?")
        val option =
            choice2(
                "Ask what Askeladden is doing here",
                1,
                "Ask for a new pet rock",
                2,
                title = "Select an Option",
            )
        if (option == 2) {
            petRock()
            return
        }
        chatPlayer(
            quiz,
            "Askeladden, what on Gielinor are you doing up here? And by the way, the name is " +
                "${player.displayName}.",
        )
        chatNpc(happy, "Sure thing, buddy!")
        chatNpc(
            happy,
            "I was down in that cave killing dagger-mouths! It was fun! But then I got thirsty, " +
                "so came up here for a bit of a rest...",
        )
        chatNpc(
            happy,
            "Hey buddy, I'll tell you what, if you got any kegs of beer on ya, I'll buy them off " +
                "ya for 700 gold coins each!",
        )
        chatNpc(
            happy,
            "If ya found any animal parts killing dagger-mouths I'll trade ya for them too!",
        )
        if (KEG_OF_BEER !in player.inv) {
            chatPlayer(sad, "No... I don't have anything like that with me...")
            chatNpc(
                sad,
                "Well, I'm not going back down there until I get a drink buddy! Come see me if " +
                    "you find me a keg, I pay good money!",
            )
            return
        }
        chatNpc(quiz, "Wanna sell me kegs of beer for 700 gold coins each buddy?")
        chatPlayer(worried, "Um... Aren't you a little... ...young to be drinking so much?")
        chatNpc(
            angry,
            "Hey, don't talk to me like that buddy! I've done all the trials you have and I'm " +
                "just as mucha  Fremennik as you are!",
        )
        chatNpc(
            angry,
            "Now are ya gonna sell me some beer, or are you going to cry about it all day?",
        )
        sellKegsOffer(FREMENNIK_KEG_PRICE)
    }

    private suspend fun Dialogue.petRock() {
        if (player.inv.isFull()) {
            mesbox("Your inventory is too full to accept another 'pet rock'.")
            return
        }
        chatPlayer(quiz, "Can I have another pet rock? I lost mine...")
        player.invAdd(player.inv, PET_ROCK)
        chatNpc(
            happy,
            "Well it's sure a weird place to ask for it buddy, but I have hundreds of them! Go " +
                "wild!",
        )
    }

    private suspend fun Dialogue.sellKegsOffer(price: Int) {
        val sell =
            choice2("YES", true, "NO", false, title = "Would you like to sell your kegs of beer?")
        if (!sell) {
            return
        }
        chatNpc(quiz, "You wanna sell me the lot buddy, or one at a time?")
        val sellAll =
            choice2("Sell all Kegs", true, "Sell One Keg", false, title = "Select an Option")
        if (sellAll) {
            if (sellKegs(player.inv.count(KEG_OF_BEER), price)) {
                player.mes("You sell Askeladden your Kegs of Beer.")
            }
            return
        }
        while (true) {
            if (!sellKegs(1, price)) {
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

    private fun Dialogue.sellKegs(count: Int, price: Int): Boolean {
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
                    strictCount = count * price
                }
            }
        return result.success
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bardur
    private suspend fun Dialogue.bardur() {
        if (fremennikDone) {
            chatPlayer(happy, "Hello there Bardur. How's it going?")
            chatNpc(
                neutral,
                "I have been here all the long day, slaughtering the daggermouth spawns so that " +
                    "none may pass!",
            )
            chatNpc(
                neutral,
                "My sword arm grows weary, and the blood of my foes hangs heavy upon my clothing!",
            )
            chatPlayer(quiz, "Can you tell me anything about the caves down here?")
            chatNpc(
                happy,
                "Aye, that I can ${fremennikName()}! Yonder lies the lair of the daggermouth " +
                    "kings, the three beasts of legend!",
            )
            chatNpc(
                neutral,
                "I train my skills upon its foul brood, to prepare myself for the fight ahead!",
            )
            chatPlayer(quiz, "Okay, thanks Bardur. Do you have any food with you?")
            chatNpc(quiz, "Ah, you did not come prepared ${fremennikName()}?")
            chatNpc(
                neutral,
                "I have nothing to spare, but my equipment grows weaker under the onslaught of " +
                    "the dagger-mouth spawns!",
            )
            chatNpc(
                neutral,
                "I will trade you a finely cooked shark if you have a replacement for my helm, " +
                    "my shield or my blade...",
            )
            chatNpc(
                neutral,
                "Give me any equipment you wish to trade, and I will pay you a shark for it.",
            )
            return
        }
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

    // https://oldschool.runescape.wiki/w/Transcript:Bardur
    // TODO(content): [verify:in_game] the message for other items before The Fremennik Trials;
    // the transcript only gives the Fremennik equipment line
    private suspend fun ProtectedAccess.bardurUse(npc: Npc, obj: ItemServerType) {
        val equipment = obj.internalName in BARDUR_TRADES
        val questDone = QuestRequirements.hasCompleted(player, FREMENNIK_TRIALS)
        if (!questDone && !equipment) {
            player.mes("Nothing interesting happens.")
            return
        }
        startDialogue(npc) {
            if (!questDone) {
                chatNpc(
                    angry,
                    "I do not trust you outerlander, I will not accept your gifts, no matter " +
                        "what your intention...",
                )
                return@startDialogue
            }
            if (!equipment) {
                chatNpc(
                    neutral,
                    "No ${fremennikName()}, I will trade you my finely cooked shark for a trusty " +
                        "Fremennik blade, a sturdy Fremennik helm, or a mighty Fremennik shield...",
                )
                chatNpc(
                    neutral,
                    "I have no interest in that... thing... you are trying to offload on me.",
                )
                return@startDialogue
            }
            chatNpc(
                happy,
                "Ah, just what I was looking for! You wish to trade me that for a cooked shark?",
            )
            if (!choice2("YES", true, "NO", false, title = "Trade with Bardur?")) {
                chatNpc(
                    neutral,
                    "As you wish, ${fremennikName()} My weapons have lasted me this long, I will " +
                        "keep my trust in them yet.",
                )
                return@startDialogue
            }
            if (!tradeForShark(obj.internalName)) {
                return@startDialogue
            }
            chatNpc(
                happy,
                "Ah, this will serve me well as a backup! My thanks to you ${fremennikName()}, I " +
                    "trust we will one day sing songs together of glorious battles in the " +
                    "Rellekka longhall!",
            )
        }
    }

    private fun Dialogue.tradeForShark(obj: String): Boolean {
        val result =
            player.invTransaction(player.inv) {
                val inv = select(player.inv)
                delete {
                    from = inv
                    this.obj = obj.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
                insert {
                    into = inv
                    this.obj = SHARK.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
            }
        return result.success
    }

    private companion object {
        const val KEG_OF_BEER = "obj.keg_of_beer"
        const val COINS = "obj.coins"
        const val PET_ROCK = "obj.vt_useless_rock"
        const val SHARK = "obj.shark"
        const val BARDUR = "npc.jb_viking_dude"
        const val KEG_PRICE = 500
        const val FREMENNIK_KEG_PRICE = 700
        const val FREMENNIK_TRIALS = "quest_fremenniktrials"
        val BARDUR_TRADES = setOf("obj.viking_sword", "obj.viking_helmet", "obj.viking_shield")
    }
}
