package org.rsmod.content.areas.misc.lobbies.tempoross.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.fishingLvl
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Captain_Pudi (at Ruins of Unkah)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:met_flags] "Hello again!" greeting once the player has heard the
//  introduction with 35 Fishing (the other Unkah talkers change their dialogue from then on)
// TODO(content): [system:tempoross] lobby overheads, boarding check and Tempoross Cove dialogue
class CaptainPudi : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.tempoross_lobby_host") { startDialogue(it.npc) { pudi() } }
    }

    private suspend fun Dialogue.pudi() {
        chatPlayer(quiz, "Hello, who are you?")
        chatNpc(
            happy,
            "Ahoy! I'm Cap'n Pudi, currently assisting the Spirit Anglers in their effort " +
                "against Tempoross.",
        )
        chatPlayer(confused, "Tempoross? Spirit Anglers? Who are they?")
        chatNpc(
            shocked,
            "You 'aven't heard?! The freak storms? Sailors running into trouble? Ye really must " +
                "be a landlubber!",
        )
        chatNpc(
            neutral,
            "Tempoross is some kind o' great sea spirit that's been spookin' sailors and causing " +
                "crazy storms!",
        )
        chatNpc(
            sad,
            "Taiman and his crew were one of the first t' run into it. Washed up outside Al " +
                "Kharid amongst piles of debris, they did. 'Tis a troublin' time to be a cap'n!",
        )
        chatPlayer(worried, "Oh that doesn't sound good at all!")
        chatNpc(
            happy,
            "It isn't, but worry not. The Spirit Anglers 'ave been formed t' tackle this " +
                "beastie! We're recruiting all skilled sailors and fisherfolk t' help us.",
        )
        chatPlayer(happy, "Well I can definitely fish!")
        // TODO(content): [verify:in_game] whether a boosted Fishing level passes this check
        if (player.fishingLvl < TEMPOROSS_FISHING_LEVEL) {
            chatNpc(
                neutral,
                "Are you sure? It's pretty rough out there and you look like you could do with " +
                    "more experience.",
            )
            mesbox("You need a Fishing level of 35 to fight Tempoross.")
            return
        }
        chatNpc(happy, "Excellent!")
        islandInstructions()
        chatNpc(
            neutral,
            "When Tempoross submerges, get up close and clear the fish from its whirlpools; " +
                "they're how it gets its energy.",
        )
        chatPlayer(happy, "Well, that sounds easy enough!")
        chatNpc(laugh, "Aye, that be the easy part!")
        chatNpc(
            neutral,
            "The storm of Tempoross will rage around you, so you better be ready to deal with " +
                "lightning strikes and avoid getting zapped when Tempoross attacks the cannons.",
        )
        chatNpc(
            neutral,
            "Also, watch out for those mighty waves! You'll want t' tether yourself t' " +
                "something solid so you don't find yourself lost in the deep blue when they " +
                "come crashing down!",
        )
        chatPlayer(worried, "Oh... Well, I suppose I can do that.")
        chatPlayer(quiz, "Will I need any special equipment?")
        chatNpc(
            happy,
            "Everythin' you need can be found on-board. Though if you have a favourite harpoon, " +
                "you can always bring that!",
        )
        chatNpc(
            neutral,
            "The complete Spirit Anglers outfit also has an 'andy harness built-in, so you won't " +
                "need a rope if you're wearin' it. You can speak t' Gita about getting one.",
        )
        chatNpc(neutral, "Ye can find Gita just east of 'ere, next t' some fishin' equipment.")
        chatNpc(happy, "When you're ready, just board the ship and we'll set sail soon!")
        chatPlayer(happy, "Aye aye cap'n!")
        options()
    }

    private suspend fun Dialogue.options() {
        while (true) {
            val option =
                choice4(
                    "Can you remind me what to do on the island?",
                    1,
                    "Are there any benefits to helping the Spirit Anglers?",
                    2,
                    "Can you tell me more about the Spirit Anglers?",
                    3,
                    "Goodbye.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Can you remind me what to do on the island?")
                    islandInstructions()
                }
                2 -> benefits()
                3 -> aboutSpiritAnglers()
                4 -> {
                    chatPlayer(neutral, "Goodbye.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.islandInstructions() {
        chatNpc(
            neutral,
            "The ships on the dock regularly embark to Tempoross Cove. Once there, you'll need " +
                "to keep the cannons loaded with Harpoonfish t' weaken the Spirit. You can fish " +
                "'em on the island.",
        )
    }

    private suspend fun Dialogue.benefits() {
        chatPlayer(quiz, "Are there any benefits to helping the Spirit Anglers?")
        chatNpc(angry, "Other than maintainin' safe passage and savin' lives?!")
        chatPlayer(worried, "Well...")
        mesbox("The captain laughs.")
        chatNpc(laugh, "I'm just messin' with ye! Of course there are benefits!")
        chatNpc(
            neutral,
            "We find all sorts washed up by the storms. Aside from the usual junk, we often find " +
                "old chests from merchant supply ships, as well as the odd artefact here and " +
                "there.",
        )
        chatNpc(happy, "If it's not needed for the Spirit Anglers' research, it's yours to keep!")
        chatNpc(
            neutral,
            "Some of the more resourceful Spirit Anglers also discovered a fishing use for the " +
                "spirit energised scales shed by the fish within Tempoross. You're free t' keep " +
                "any you find.",
        )
    }

    private suspend fun Dialogue.aboutSpiritAnglers() {
        chatPlayer(quiz, "Can you tell me more about the Spirit Anglers?")
        chatNpc(
            neutral,
            "Well, as I mentioned before, Taiman and his fishing crew from Al Kharid 'ad a " +
                "tussle with Tempoross. They barely made it back.",
        )
        chatNpc(
            neutral,
            "I was sitting down by the shore enjoying one of Karim's kebabs at the time, when " +
                "suddenly the crew washed up amongst a pile of debris. Looked like they'd just " +
                "seen a ghost!",
        )
        chatNpc(
            neutral,
            "As soon as they could talk sense they went to the Chancellor and within days more " +
                "shockin' tales of the Sea Spirit's wrath came in.",
        )
        chatNpc(
            neutral,
            "The Chancellor called a meeting for all us local seafarin' folk and we agreed " +
                "action must be taken!",
        )
        chatNpc(
            angry,
            "Ain't nothin' goin' t' take the seas from me, even if it is a powerful " +
                "spirit!",
        )
        chatNpc(
            neutral,
            "Anyway, it was decided that we'd setup a base 'ere amongst these ruins, and so the " +
                "Spirit Anglers was formed!",
        )
        chatNpc(
            laugh,
            "Don't ask me what the ruins used to be. And it's not like Menaphos can 'ave a say!",
        )
        chatNpc(happy, "Whatever they are, they make a fine base for the Spirit Anglers!")
        chatNpc(
            neutral,
            "Since moving in, we've been recruiting fisherfolk from all over, and some of the " +
                "readin' types 'ave been studyin' Tempoross durin' our trips to subdue it.",
        )
        chatNpc(
            happy,
            "I like t' get stuck in, me-self; face off toe t' toe with the beastie. I'll be " +
                "right by your side when you go out there!",
        )
        chatPlayer(worried, "Well that's encouraging, I think...")
        chatNpc(laugh, "I'm still 'ere, ain't I?")
        chatNpc(
            neutral,
            "Anyway, there's plenty o' research going on, so maybe you can find some notes lyin' " +
                "around if ye want to know more.",
        )
    }

    private companion object {
        const val TEMPOROSS_FISHING_LEVEL = 35
    }
}
