package org.rsmod.content.areas.misc.morytania.meiyerditch

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:vyre_disguise] "Oh... Sorry, we're not meant to talk to the nobles."
//  and Old Man Ral's "Ah! vampyre!" while wearing vyre noble clothing
class MeiyerditchLocals
@Inject
constructor(private val objRepo: ObjRepository, private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        for (miner in 1..MINERS) {
            onOpNpc1("npc.myreque_pt3_miner${miner}_vis") { startDialogue(it.npc) { miner() } }
        }
        onOpNpc1(OLD_MAN_RAL) { startDialogue(it.npc) { oldManRal() } }
        onOpNpc1(TRADER_SVEN) { startDialogue(it.npc) { traderSven(it.npc) } }
        onOpNpc3(TRADER_SVEN) { player.openSvensShop(it.npc) }
    }

    private fun Player.openSvensShop(npc: Npc) {
        shops.open(this, npc, "Trader Sven's Black-market Goods.", "inv.meiyerditch_black_market")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Meiyerditch_miner
    private suspend fun Dialogue.miner() {
        chatPlayer(happy, "Hello there.")
        chatNpc(neutral, "Oh, hello, you must be new here.")
        while (true) {
            val option =
                if (carriesPickaxe()) {
                    choice5(
                        "Where are we?",
                        1,
                        "What do you do here?",
                        2,
                        "How do you get out of here?",
                        4,
                        "What is it that you're mining?",
                        5,
                        "Okay, thanks.",
                        6,
                    )
                } else {
                    choice5(
                        "Where are we?",
                        1,
                        "What do you do here?",
                        2,
                        "Do you have a spare pick?",
                        3,
                        "How do you get out of here?",
                        4,
                        "Okay, thanks.",
                        6,
                    )
                }
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Where are we?")
                    chatNpc(
                        confused,
                        "What, you can't tell from your surroundings that you're in a mine? You " +
                            "must have been hit hard on the head to not notice that.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "What do you do here?")
                    chatNpc(
                        sad,
                        "We're miners, perhaps that might give you a clue! We're forced to mine " +
                            "for those stinking vyres. Once we've filled the cart, they " +
                            "sometimes let us go back to Meiyerditch, but then we get tithed. " +
                            "Don't know if I",
                    )
                    chatNpc(sad, "prefer it in here or out there!")
                }
                3 -> {
                    chatPlayer(quiz, "Do you have a spare pick?")
                    chatNpc(neutral, "Sure, got one here you can use.")
                    player.invAddOrDrop(objRepo, BRONZE_PICKAXE)
                    objbox(BRONZE_PICKAXE, "The miner hands you a rustic-looking pickaxe.")
                }
                4 -> {
                    chatPlayer(quiz, "How do you get out of here?")
                    chatNpc(
                        neutral,
                        "They can't keep you in here forever because they have to tithe you, so " +
                            "once you've filled the cart, they let you go. Takes about fifteen " +
                            "loads to get the cart filled.",
                    )
                }
                5 -> return daeyalt()
                6 -> {
                    chatPlayer(neutral, "Okay, thanks.")
                    return
                }
            }
        }
    }

    private fun Dialogue.carriesPickaxe(): Boolean =
        access.inv.any { it != null && getInvObj(it).isContentType(PICKAXE_CONTENT) }

    private suspend fun Dialogue.daeyalt() {
        chatPlayer(quiz, "What is it that you're mining?")
        chatNpc(
            neutral,
            "We're mining for daeyalt. Don't rightly know what it does - we don't ask too many " +
                "questions - but I've been doing it since I was a kid. The carts keep getting " +
                "filled, and they keep going along the tracks. Goes to some",
        )
        chatNpc(
            neutral,
            "huge refinery place, it does - massive, like another city under the earth.",
        )
        chatPlayer(shocked, "What? Have you seen this place?")
        chatNpc(
            neutral,
            "Not myself, like, but other people I know of have seen it and they wouldn't lie!",
        )
        chatNpcSpecific(
            "Juvinate guard",
            MINE_GUARD,
            angry,
            "You two shut up and get back to work!",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Old_Man_Ral
    // TODO(content): [quest:the_blood_moon_rises] the supplies shop, tome and Drakan's medallion
    //  reclaims once Ivan leaves the Myreque hideout
    // TODO(content): [system:treasure_trails] master clue step puzzle box
    private suspend fun Dialogue.oldManRal() {
        val sinsOfTheFather = QuestRequirements.hasCompleted(player, SINS_OF_THE_FATHER)
        val tasteOfHope = QuestRequirements.hasCompleted(player, A_TASTE_OF_HOPE)
        val okayThanks = if (sinsOfTheFather || tasteOfHope) "Okay, thanks." else "Okay, thanks"
        chatPlayer(happy, "Hello again!")
        chatNpc(angry, "Hmph!")
        while (true) {
            val option =
                choice5(
                    "What do you do here?",
                    1,
                    "I'm lost... Can you help me?",
                    2,
                    "Do you know about the Myreque?",
                    3,
                    "Can you help me?",
                    4,
                    okayThanks,
                    5,
                    title = "Select an Option",
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What do you do here?")
                    chatNpc(
                        angry,
                        "Hmph! Why? What do I know? I'm just an old man; what would anyone want " +
                            "with an old man?",
                    )
                }
                2 -> {
                    chatPlayer(worried, "I'm lost... Can you help me?")
                    chatNpc(
                        angry,
                        "Hmph! Who says I'm not lost, eh? Ever think of that, did you?",
                    )
                    chatNpc(
                        angry,
                        "In a rush to find out all the answers, not thinking for a second that " +
                            "someone else might be just as lost, if not more lost, than you! And " +
                            "what do you care about them, huh?",
                    )
                }
                3 -> ralMyreque(sinsOfTheFather, tasteOfHope)
                4 -> ralHelp(sinsOfTheFather, tasteOfHope)
                5 -> {
                    chatPlayer(neutral, okayThanks)
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.ralMyreque(sinsOfTheFather: Boolean, tasteOfHope: Boolean) {
        chatPlayer(quiz, "Do you know about the Myreque?")
        if (sinsOfTheFather) {
            chatNpc(
                angry,
                "Hmph! Dead! That's what I know about the Myreque. You're lucky to not be dead " +
                    "as well. Don't talk about them any more!",
            )
            return
        }
        chatNpc(
            angry,
            "Hmph! Why should I know about the Myreque, eh? What use have they ever been to me, " +
                "huh? I ask you!",
        )
        if (tasteOfHope) {
            chatPlayer(confused, "But they live right under your house?")
            chatNpc(angry, "Hmph! Suppose they do. Stupid Myreque...")
        }
    }

    private suspend fun Dialogue.ralHelp(sinsOfTheFather: Boolean, tasteOfHope: Boolean) {
        chatPlayer(quiz, "Can you help me?")
        chatNpc(
            angry,
            "Hmph! What makes you think that I can help you, eh? Do I look like a fountain of " +
                "knowledge, eh? Do I?",
        )
        if (sinsOfTheFather || tasteOfHope) {
            chatPlayer(neutral, "Well, you've helped me before! You helped me find the Myreque!")
            val ending =
                if (sinsOfTheFather) "they're all dead! Hmph!" else "they're right below us. Hmph!"
            chatNpc(
                angry,
                "Hmph! Yes of course, follow the symbols and all that. Pointless now though, " +
                    ending,
            )
            return
        }
        chatPlayer(
            neutral,
            "Well, you've helped me before! You said something about symbols? To find the Myreque!",
        )
        chatNpc(angry, "Hmph! Yes of course, that's how to get to them. Follow the symbols.")
        chatPlayer(quiz, "Which symbols?")
        chatNpc(angry, "The symbol of the Myreque - the silver sickle!")
        chatPlayer(quiz, "Oh, and where would that be then?")
        chatNpc(angry, "Around and about; it should be an aid.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Trader_Sven
    // TODO(content): [verify:in_game] what sets the "learned Trader Sven has a shop" flag; this
    //  reads varbit.myq5_trader_sven_met and never sets it
    private suspend fun Dialogue.traderSven(npc: Npc) {
        val freed = QuestRequirements.hasCompleted(player, BLOOD_MOON_RISES)
        chatNpc(happy, "Oh, hello. How's everything?")
        chatPlayer(neutral, "Not too bad thanks, but I wanted to ask a question.")
        chatNpc(neutral, "Go ahead.")
        while (true) {
            val knowsShop = player.svenMet
            val option =
                if (knowsShop) {
                    choice4(
                        "What have you got for sale?",
                        0,
                        "What do you do here?",
                        1,
                        "Where am I?",
                        2,
                        "Actually, I'm good.",
                        3,
                        title = "Select an Option",
                    )
                } else {
                    choice3(
                        "What do you do here?",
                        1,
                        "Where am I?",
                        2,
                        "Actually, I'm good.",
                        3,
                        title = "Select an Option",
                    )
                }
            when (option) {
                0 -> {
                    chatPlayer(quiz, "What have you got for sale?")
                    chatNpc(neutral, "Take a look.")
                    return player.openSvensShop(npc)
                }
                1 -> return svenWares(npc, freed)
                2 -> {
                    chatPlayer(quiz, "Where am I?")
                    chatNpc(
                        neutral,
                        "You're in sector one of Meiyerditch. And don't forget, we're all here " +
                            "forever.",
                    )
                }
                3 -> {
                    chatPlayer(neutral, "Actually, I'm good.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.svenWares(npc: Npc, freed: Boolean) {
        chatPlayer(quiz, "What do you do here?")
        chatNpc(neutral, "I'm a black-market trader. Fancy taking a look at my wares?")
        val option =
            choice3(
                "Yes, I'll take a look at your wares.",
                1,
                "What sort of stuff have you got?",
                2,
                "I'm good.",
                3,
                title = "Select an Option",
            )
        if (option == 1) {
            return svenOpen(npc)
        }
        if (option == 3) {
            chatPlayer(neutral, "I'm good.")
            return
        }
        chatPlayer(quiz, "What sort of stuff have you got?")
        val address = if (access.isBodyTypeA()) "lad" else "lass"
        if (freed) {
            chatNpc(
                neutral,
                "Well then, young $address, that is a good question. In fact, I specifically " +
                    "deal in vyrewatch uniforms. Used to be quite useful in getting them to " +
                    "leave you alone, but that's not an issue any more.",
            )
            chatNpc(
                neutral,
                "Still, some people prefer to wear them even now. Makes them feel safer I guess, " +
                    "or maybe they find them fashionable? Speaking of which, do you wanna have a " +
                    "look at my stuff?",
            )
        } else {
            chatNpc(
                neutral,
                "Well then, young $address, that is a good question. In fact, I specifically " +
                    "deal in vyrewatch uniforms. They might leave you alone a bit more if they " +
                    "think you're one of them!",
            )
            chatNpc(
                neutral,
                "Makes it easier to move around the sectors without them demanding a blood tithe " +
                    "every time you dare breathe. So then, do you wanna have a look at my stuff?",
            )
        }
        val look =
            choice2(
                "Yes, I'll take a look at your wares.",
                true,
                "I'm good.",
                false,
                title = "Select an Option",
            )
        if (look) {
            return svenOpen(npc)
        }
        chatPlayer(neutral, "I'm good.")
    }

    private suspend fun Dialogue.svenOpen(npc: Npc) {
        chatPlayer(happy, "Yes, I'll take a look at your wares.")
        chatNpc(happy, "Good on ya!")
        player.openSvensShop(npc)
    }

    private companion object {
        const val SINS_OF_THE_FATHER = "quest_sinsofthefather"
        const val A_TASTE_OF_HOPE = "quest_tasteofhope"
        const val BLOOD_MOON_RISES = "quest_bloodmoonrises"
        const val OLD_MAN_RAL = "npc.sanguinesti_old_man_ral"
        const val TRADER_SVEN = "npc.myq3_black_market_trader"
        const val MINE_GUARD = "npc.sang_myq3_mine_guard_juvinate_male"
        const val BRONZE_PICKAXE = "obj.bronze_pickaxe"
        const val PICKAXE_CONTENT = "content.mining_pickaxe"
        const val MINERS = 6

        val Player.svenMet: Boolean by boolVarBit("varbit.myq5_trader_sven_met")
    }
}
