package org.rsmod.content.areas.city.yanille.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class SouthKandarinGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.grandtree_hazelmere") { startDialogue(it.npc) { hazelmere() } }
        onOpNpc1("npc.rc_zmi_chaosmonk") { startDialogue(it.npc) { mageOfZamorak() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hazelmere
    private suspend fun Dialogue.hazelmere() {
        if (QuestRequirements.hasCompleted(player, PATH_OF_GLOUPHRIE)) {
            mesbox(meld(MIND_MELD))
            chatPlayer(happy, meld("Hello, Hazelmere."))
            chatNpc(angry, meld("You're back. Come to disturb me, have you?"))
            chatPlayer(
                laugh,
                meld("Ah, good old Hazelmere. Glad to see you're back to your grumpy self."),
            )
            chatNpc(
                angry,
                meld(
                    "Do you actually want something from me, or are you simply making my life a " +
                        "misery again?"
                ),
            )
            chatPlayer(
                happy,
                meld("I just wanted to have a chat. You know, just check how you're doing."),
            )
            chatNpc(neutral, meld("Well I'm fine, thank you. Now leave me in peace."))
            return
        }
        mesbox("The mage starts to speak but all you hear is:")
        chatNpc(neutral, "Blah. Blah, blah, blah, blah...blah!")
        if (QuestRequirements.hasCompleted(player, EYES_OF_GLOUPHRIE)) {
            hazelmereAfterEyesOfGlouphrie()
        }
    }

    private suspend fun Dialogue.hazelmereAfterEyesOfGlouphrie() {
        mesbox(meld(MIND_MELD))
        chatNpc(
            neutral,
            meld(
                "Well, I never expected to be impressed by a human, but you have done a good " +
                    "deed for the gnomes - even if most gnomes won't recognise it.  You humans " +
                    "are not as stupid as I first thought..."
            ),
        )
        chatPlayer(
            happy,
            meld(
                "Well, that's probably as close to a compliment I'm ever likely to get from you, " +
                    "so thanks!"
            ),
        )
        if (CRYSTAL_SAW_SEED in player.inv) {
            chatNpc(neutral, meld("So you want to know what your crystal seed does?"))
            chatPlayer(confused, meld("How did you.. ? Oh I forgot about this mind meld thing."))
            chatNpc(
                neutral,
                meld(
                    "The seed is a piece of elven magic, all elven magic comes from the " +
                        "manipulation of seeds of crystal. The elves give it divine properties. " +
                        "Elves sing to their crystals and they form into the desired shape."
                ),
            )
            chatPlayer(quiz, meld("So how do I turn it into something useful?"))
            chatNpc(
                neutral,
                meld(
                    "Either talk to an elf or find some other way to sing the crystal. If this " +
                        "came from Oaknock, he must have found a way..."
                ),
            )
            chatPlayer(
                happy,
                meld(
                    "Thanks, I'll have a look round Oaknock's old workshop. I can have a chat " +
                        "with Brimstail while I'm at it! Thanks!"
                ),
            )
            return
        }
        if (CRYSTAL_SAW in player.inv) {
            chatNpc(neutral, meld("So you want to know what your crystal saw does?"))
            chatPlayer(confused, meld("How did you.. ? Oh I forgot about this mind meld thing."))
            chatNpc(
                neutral,
                meld(
                    "Well, I do remember seeing Oaknock using the saw. He said it allowed him to " +
                        "build devices more complicated that even he was capable of " +
                        "constructing. The device he built must have been very complicated indeed."
                ),
            )
            chatPlayer(
                happy,
                meld("I'd say! Anyway, the saw looks like it could be pretty useful. Thanks."),
            )
            objbox(
                CRYSTAL_SAW,
                "The saw will add three levels to your effective construction level when you are " +
                    "building something that requires a saw.",
            )
        }
    }

    private fun meld(text: String): String = "<col=000080>$text</col>"

    // https://oldschool.runescape.wiki/w/Transcript:Mage_of_Zamorak_(Ourania)
    private suspend fun Dialogue.mageOfZamorak() {
        chatPlayer(quiz, "What's that ladder next to the altar for?")
        chatNpc(shifty, "An...archeological dig. Yeah, a dig.")
        chatNpc(quiz, "Why?")
        chatPlayer(
            quiz,
            "Next to a Chaos Altar? That's a bit odd. Can I go down and have a look?",
        )
        chatNpc(
            neutral,
            "Well...I suppose. See, we found this ancient altar, but it was pretty broken so the " +
                "Z.M.I. sent some of its researchers to try and repair it.",
        )
        chatPlayer(quiz, "How'd that work out for them?")
        chatNpc(
            neutral,
            "Pretty well - only one died. Still, they got it working...sort of. This ancient " +
                "technology can be tricky.",
        )
        chatPlayer(quiz, "What do you mean, 'sort of'? Altars either work or don't work.")
        chatNpc(
            neutral,
            "This one works, just not as you'd expect. You put pure essence in, but get random " +
                "runes back. Some of them we don't even know how to craft!",
        )
        chatPlayer(happy, "Sounds like a pretty good deal. Can I give it a go?")
        chatNpc(
            neutral,
            "I don't think so. The mages don't like outsiders going in; they'll probably attack " +
                "you. They only patrol the short path, though, so if you used the long path, " +
                "through a tunnel near the entrance, you'd probably be safe.",
        )
        chatPlayer(happy, "Thanks.")
    }

    private companion object {
        const val EYES_OF_GLOUPHRIE = "quest_eyesofglouphrie"
        const val PATH_OF_GLOUPHRIE = "quest_pathofglouphrie"
        const val CRYSTAL_SAW = "obj.eyeglo_crystal_saw"
        const val CRYSTAL_SAW_SEED = "obj.crystal_seed_old_small"
        const val MIND_MELD =
            "You feel very strange - as if Hazelmere has started poking around in your head."
    }
}
