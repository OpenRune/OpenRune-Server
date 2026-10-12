package org.rsmod.content.quest.area.varrock.gertrudescat.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.area.varrock.gertrudescat.GertrudesCatQuest
import org.rsmod.content.quest.area.varrock.gertrudescat.GertrudesCatQuest.Companion.KidsFee
import org.rsmod.content.quest.area.varrock.gertrudescat.GertrudesCatQuest.Companion.STAGE_PAID_KIDS
import org.rsmod.content.quest.area.varrock.gertrudescat.GertrudesCatQuest.Companion.STAGE_STARTED
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GertrudesKids @Inject constructor(private val gertrudesCat: GertrudesCatQuest) :
    PluginScript() {

    private val quest
        get() = gertrudesCat.quest

    override fun ScriptContext.startup() {
        onOpNpc1(SHILOP) { startDialogue(it.npc) { kid(brother = WILOUGH, brotherName = "Wilough") } }
        onOpNpc1(WILOUGH) { startDialogue(it.npc) { kid(brother = SHILOP, brotherName = "Shilop") } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gertrude%27s_Cat
    // https://oldschool.runescape.wiki/w/Transcript:Shilop
    // TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
    private suspend fun Dialogue.kid(brother: String, brotherName: String) {
        when (quest.getQuestStage(player)) {
            0 -> {
                chatPlayer(happy, "Hello youngster!")
                chatNpc(angry, "I don't talk to strange old people.")
                chatPlayer(angry, "Hey who you calling old?! And strange?!")
            }
            STAGE_STARTED -> secretPlayArea(brother, brotherName)
            STAGE_PAID_KIDS -> {
                chatPlayer(quiz, "Where did you say you saw Fluffs?")
                chatNpc(
                    bored,
                    "Weren't you listening? I saw the flea bag in the old lumber mill just north " +
                        "east of here. Just walk past the Jolly Boar Inn and you should find it.",
                )
            }
            else -> toughGuy(brother, brotherName)
        }
    }

    private suspend fun Dialogue.secretPlayArea(brother: String, brotherName: String) {
        chatPlayer(happy, "Hello there, I've been looking for you.")
        chatNpc(worried, "I didn't mean to take it! I just forgot to pay.")
        chatPlayer(confused, "What? I'm trying to help your mum find Fluffs.")
        // TODO(content): [verify:in_game] The transcript has a shorter "I might be able to help."
        //  variant "if the player first talked to Shilop"; which boy says which is unclear.
        chatNpc(
            neutral,
            "Ohh...well, in that case I might be able to help. Fluffs followed me to my secret " +
                "play area, I haven't seen her since.",
        )
        chatPlayer(quiz, "Where is this play area?")
        chatNpc(shifty, "If I told you that, it wouldn't be a secret.")
        when (
            choice3(
                "Tell me sonny, or I will hurt you.", 1,
                "What will make you tell me?", 2,
                "Well never mind, it's Fluffs' loss.", 3,
            )
        ) {
            1 -> {
                chatPlayer(angry, "Tell me sonny, or I will hurt you.")
                chatNpc(
                    shocked,
                    "W..wh..what?! Y..you wouldn't! A young lad like me! I'd have you behind " +
                        "bars before nightfall!",
                )
                mesbox("You decided it's best not to hurt the boy.")
            }
            2 -> haggle(brother, brotherName)
            3 -> {
                chatPlayer(neutral, "Well, never mind, it's Fluffs' loss.")
                chatNpc(neutral, "I'm sure my mum will get over it.")
            }
        }
    }

    private suspend fun Dialogue.haggle(brother: String, brotherName: String) {
        chatPlayer(quiz, "What will make you tell me?")
        chatNpc(shifty, "Well...now you ask, I am a bit short on cash.")
        chatPlayer(quiz, "How much?")
        chatNpc(neutral, "10 coins.")
        chatNpcSpecific(brotherName, brother, shocked, "10 coins?!")
        chatNpcSpecific(brotherName, brother, neutral, "I'll handle this.")
        chatNpcSpecific(brotherName, brother, shifty, "$KidsFee coins should cover it.")
        chatPlayer(shocked, "$KidsFee coins! Why should I pay you?")
        chatNpcSpecific(
            brotherName,
            brother,
            neutral,
            "You shouldn't, but we won't help otherwise. We never liked that cat anyway, so what " +
                "do you say?",
        )
        when (
            choice2(
                "I'm not paying you a penny.", 1,
                "Okay then, I'll pay.", 2,
            )
        ) {
            1 -> {
                chatPlayer(angry, "I'm not paying you a penny.")
                chatNpc(neutral, "Okay then, I'll find another way to make money.")
            }
            2 -> pay()
        }
    }

    private suspend fun Dialogue.pay() {
        chatPlayer(neutral, "Okay then, I'll pay.")
        // TODO(content): [verify:in_game] The transcript has no lines for paying without the coins.
        if (player.inv.count(COINS) < KidsFee) {
            chatPlayer(sad, "Oh. I don't seem to have $KidsFee coins on me.")
            chatNpc(bored, "Then come back when you do. No coins, no secret.")
            return
        }
        if (!takeFeeAndAdvance()) {
            return
        }
        access.soundSynth("synth.coins_jingle_1")
        mesbox("You give the lad $KidsFee coins.")
        chatPlayer(quiz, "There you go, now where did you see Fluffs ?")
        chatNpc(
            happy,
            "I play at an abandoned lumber mill to the north east. Just beyond the Jolly Boar " +
                "Inn. I saw Fluffs running around in there.",
        )
        chatPlayer(quiz, "Anything else?")
        chatNpc(neutral, "Well, you'll have to find the broken fence to get in. I'm sure you can manage that.")
    }

    private fun Dialogue.takeFeeAndAdvance(): Boolean {
        if (!access.invTakeFee(KidsFee)) {
            return false
        }
        quest.setQuestStage(access, STAGE_PAID_KIDS)
        return true
    }

    private suspend fun Dialogue.toughGuy(brother: String, brotherName: String) {
        chatPlayer(happy, "Hello again.")
        chatNpc(angry, "You think you're tough do you?")
        chatPlayer(confused, "Pardon?")
        chatNpc(angry, "I can beat anyone up!")
        chatNpcSpecific(brotherName, brother, happy, "He can you know!")
        chatPlayer(quiz, "Really?")
        mesbox(
            "The boy begins to jump around with his fists up. You decide it's best not to kill " +
                "him just yet."
        )
    }

    private companion object {
        const val SHILOP = "npc.shilop"
        const val WILOUGH = "npc.wilough"
        const val COINS = "obj.coins"
    }
}
