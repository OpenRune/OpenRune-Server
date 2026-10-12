package org.rsmod.content.areas.city.desert.pollnivneach.npcs

import dev.openrune.types.MesAnimType
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Ali_the_Camel_Man
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [verify:in_game] what Trade does; no shop inv exists for the camel store
class AliTheCamelMan : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(CAMEL_MAN) { startDialogue(it.npc) { camelMan() } }
    }

    private suspend fun Dialogue.camelMan() {
        if (QuestRequirements.hasCompleted(player, THE_FEUD)) {
            samiAfterFeud()
            return
        }
        chatNpc(happy, "Welcome to my discount camel store. Can I help you with anything?")
        while (true) {
            val option =
                choice3(
                    "A discount camel store?",
                    1,
                    "Tell me about this town.",
                    2,
                    "Lovely day isn't it?",
                    3,
                )
            val keepTalking =
                when (option) {
                    1 -> discountStore()
                    2 -> town()
                    else -> lovelyDay()
                }
            if (!keepTalking) {
                return
            }
            chatNpc(happy, "Is there anything else I can help you with?")
            val more =
                choice2(
                    "Well yes actually I'd like to ask about something else.",
                    true,
                    "No but thanks for your time.",
                    false,
                )
            if (!more) {
                chatPlayer(happy, "No but thanks for your time.")
                return
            }
        }
    }

    private suspend fun Dialogue.samiAfterFeud() {
        chatSami(
            happy,
            "Hello! Welcome back to my discount camel store. How are those camels getting along?",
        )
        chatPlayer(happy, "Hey Sami!")
        chatSami(happy, "Hello adventurer.")
        chatPlayer(quiz, "Any news?")
        chatSami(
            happy,
            "Not much really, I just want to thank you for what you have done for this town. I " +
                "might even stick around now that business is better!",
        )
        chatPlayer(quiz, "Any grand plans?")
        chatSami(
            neutral,
            "Well I've been debating whether I should diversify into camel rental, too. Although " +
                "I do have all these cages lying around... do you think there's a market for " +
                "discount jackals?",
        )
        chatPlayer(happy, "Maybe! Best of luck, in any case.")
        chatSami(happy, "Thank you.")
    }

    private suspend fun Dialogue.chatSami(mesanim: MesAnimType, text: String) =
        chatNpcSpecific(SAMI_NAME, SAMI, mesanim, text)

    private suspend fun Dialogue.discountStore(): Boolean {
        chatPlayer(quiz, "A discount camel store?")
        chatNpc(
            happy,
            "Yes - it's my great idea - selling camels at discounted prices so that the common man " +
                "can experience the joys of owning a camel of their very own! They're not just a " +
                "source of kebab meat you know!",
        )
        val option =
            choice5(
                "So can I buy a camel then? I'm hungry!",
                1,
                "Yes, camels are beautiful creatures.",
                2,
                "Why would I want a camel? All they do is spit and...",
                3,
                "Actually, I think I was a camel in a previous life.",
                4,
                "So is the discount camel business good then?",
                5,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "So can I buy a camel then? I'm hungry!")
                chatNpc(
                    angry,
                    "I would never sell one to the likes of you! They are beautiful, majestic " +
                        "creatures, not food! Get out of my shop.",
                )
                return false
            }
            2 -> {
                chatPlayer(happy, "Yes, camels are beautiful creatures.")
                chatNpc(
                    happy,
                    "Ah, wondrous camels! Best of all Saradomin's creations. Ships of the desert, " +
                        "far more useful than any horse or donkey. Truly, they are a man's best " +
                        "friend.",
                )
                chatNpc(
                    happy,
                    "Perhaps you'd like a camel of your own? You're in the right place, if so...",
                )
            }
            3 -> {
                chatPlayer(confused, "Why would I want a camel? All they do is spit and...")
                chatNpc(
                    angry,
                    "Pah! I've seen mummies with more brains than you! If you treat camels badly " +
                        "then of course they're going to spit at you. May your robes never be " +
                        "clean again!",
                )
            }
            4 -> {
                chatPlayer(laugh, "Actually, I think I was a camel in a previous life.")
                chatNpc(sad, "You're teasing me now! You rogue!")
                chatNpc(sad, "Everyone laughs at the poor Discount Camel Seller.")
            }
            5 -> {
                chatPlayer(quiz, "So is the discount camel business good then?")
                chatNpc(
                    happy,
                    "Business is booming! Pollnivneach has never seen this number of visitors.",
                )
                chatNpc(
                    happy,
                    "In fact I'm thinking of expanding and trying to break into the camel hire " +
                        "market.",
                )
                chatNpc(
                    confused,
                    "If only I could figure out how to get people to return the camels...",
                )
            }
        }
        return true
    }

    private suspend fun Dialogue.town(): Boolean {
        chatPlayer(quiz, "Tell me about this town.")
        chatNpc(
            neutral,
            "Well, Pollnivneach is a funny little place. It's a small town and you'd think it'd be " +
                "nice and quiet.",
        )
        chatNpc(sad, "But those Menaphites and bandits are making the place quite unbearable.")
        val option =
            choice3(
                "In what way?",
                1,
                "Well the weak will always be trodden on. Why not stand up to them?",
                2,
                "Perhaps I could help.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "In what way?")
                chatNpc(
                    angry,
                    "Well, one group is as bad as the other! They steal, they fight, they cause " +
                        "trouble with us locals. Just yesterday one of those bandits came looking " +
                        "for protection money!",
                )
                chatPlayer(quiz, "Why don't the authorities stop this?")
                chatNpc(
                    sad,
                    "We're a small town. We don't have the resources to keep the gangs off our " +
                        "backs! Menaphos and Al Kharid are no help either.",
                )
                chatNpc(
                    neutral,
                    "To tell the truth we live in hope that an outsider, someone with no ties to " +
                        "either group, could broker a deal to halt the hostilities between them.",
                )
                chatPlayer(neutral, "Hmm...")
            }
            2 -> {
                chatPlayer(
                    neutral,
                    "Well the weak will always be trodden on. Why not stand up to them?",
                )
                chatNpc(
                    neutral,
                    "That's a little unfair, my friend. We can't all be brave adventurers. If we " +
                        "were, where would you purchase quality camels at discounted prices?",
                )
                chatNpc(
                    neutral,
                    "You might think about that. Anyway, isn't it the job of the brave adventurer " +
                        "to protect the weak and innocent from evil and harm?",
                )
            }
            3 -> perhapsICouldHelp()
        }
        return true
    }

    private suspend fun Dialogue.perhapsICouldHelp() {
        chatPlayer(happy, "Perhaps I could help.")
        chatNpc(neutral, "Yes, perhaps you could.")
        chatPlayer(quiz, "You know the politics better than I do. What do you recommend?")
        chatNpc(
            confused,
            "Me?! I am but a humble discount camel seller. You're the adventurer, don't you know " +
                "what to do?",
        )
        val knows =
            choice2(
                "Indeed I should know, and indeed I do! I was just testing you!",
                true,
                "Perhaps I should know, but offered advice should never be shunned.",
                false,
            )
        if (knows) {
            chatPlayer(happy, "Of course I know what to do! I was just testing you!")
            chatNpc(
                happy,
                "That is reassuring to hear. Too often one meets brainless adventurers whose " +
                    "solution to every problem is to hack, maim and murder.",
            )
            chatNpc(happy, "I'm glad to know you're not of that ilk.")
            return
        }
        chatPlayer(
            neutral,
            "Perhaps I should know, but offered advice should never be shunned.",
        )
        chatNpc(
            bored,
            "The state of adventurers today. They need everything spelled out for them.",
        )
        chatNpc(bored, "What, you want me to put a little note in your quest journal?")
        chatNpc(
            neutral,
            "But fine, I'll give you a hint. Try and see if you can get the gangs to agree to a " +
                "truce by offering them each a token gift as an act of reconciliation.",
        )
        chatNpc(
            neutral,
            "If that fails to work, you must consider trying to influence the gangs from within.",
        )
    }

    private suspend fun Dialogue.lovelyDay(): Boolean {
        chatPlayer(happy, "Lovely day isn't it?")
        chatNpc(
            happy,
            "It's a beautiful day today, but word has it that it will rain tomorrow.",
        )
        chatPlayer(shocked, "Really?")
        chatNpc(laugh, "No! When have you ever known it to rain here in Gielinor?")
        chatPlayer(neutral, "Oh. Anyway...")
        return true
    }

    private companion object {
        const val CAMEL_MAN = "npc.feud_ali_the_discount_camel_seller_multi"
        const val SAMI = "npc.feud_ali_the_discount_camel_seller_postquest"
        const val SAMI_NAME = "Sami the Camel Man"
        const val THE_FEUD = "quest_feud"
    }
}
