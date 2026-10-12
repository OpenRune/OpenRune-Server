package org.rsmod.content.areas.zeah.quidamortem.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private var Player.mountainGuideUnlocked: Int by intVarBit("varbit.raids_guide_travel_unlock")

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class QuidamortemTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.raids_temple_captain") { startDialogue(it.npc) { captainRimor() } }
        onOpNpc1("npc.raids_temple_gnosi") { startDialogue(it.npc) { gnosi() } }
        onOpNpc1("npc.raids_temple_duffy") { startDialogue(it.npc) { historianDuffy() } }
        onOpNpc1("npc.raids_temple_historian") { startDialogue(it.npc) { naturalHistorian() } }
        for (type in MOUNTAIN_GUIDES) {
            onOpNpc1(type) { startDialogue(it.npc) { mountainGuide() } }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Captain_Rimor
    // TODO(content): [boss:chambers_of_xeric] Claim-cape (Xeric's guard/warrior...) and
    //  Set-difficulty ops
    private suspend fun Dialogue.captainRimor() {
        chatNpc(neutral, "Beware the Chambers of Xeric, adventurer.")
        chatNpc(
            neutral,
            "These dungeons and the perils within them are unique. You will encounter both " +
                "dangerous and mysterious creatures here.",
        )
        chatNpc(
            neutral,
            "Remember that you may be kicked out at any time - any items dropped on the ground " +
                "will be lost inside.",
        )
        chatNpc(neutral, "Do not allow your curiosity to get the better of you.")
        chatPlayer(neutral, "Thanks for the warning.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gnosi
    private suspend fun Dialogue.gnosi() {
        if (QuestRequirements.hasCompleted(player, TALE_OF_THE_RIGHTEOUS)) {
            chatPlayer(happy, "Hello. How's it going?")
            chatNpc(
                happy,
                "The discovery of this cave could change everything. We might finally be able to " +
                    "discover the link between the lizardmen, the Chambers of Xeric and Xeric " +
                    "himself.",
            )
        } else {
            gnosiIntro()
        }
        val topic =
            choice3(
                "Who is Xeric?",
                GnosiTopic.Xeric,
                "What are the Chambers of Xeric?",
                GnosiTopic.Chambers,
                "Goodbye.",
                GnosiTopic.Goodbye,
            )
        gnosiTopics(topic)
    }

    private suspend fun Dialogue.gnosiIntro() {
        chatPlayer(quiz, "Hello. What do you do here?")
        chatNpc(
            neutral,
            "My name is Gnosi. I am here to assist the Varrock Museum in their research.",
        )
        chatPlayer(
            quiz,
            "I didn't know the Varrock Museum was familiar with this continent. What research " +
                "are you conducting?",
        )
        chatNpc(
            neutral,
            "We're studying the history of the region, origins of the lizardmen and how it all " +
                "connects to Xeric.",
        )
        chatNpc(
            neutral,
            "We've wanted to study this place for a while, but haven't been able to explore due " +
                "to lizardmen blocking the way.",
        )
        chatNpc(
            happy,
            "But now that the Shayzien Army have cleared the path and discovered the Chambers of " +
                "Xeric in the process, there is much to explore!",
        )
    }

    private suspend fun Dialogue.gnosiTopics(first: GnosiTopic) {
        var topic = first
        while (true) {
            topic =
                when (topic) {
                    GnosiTopic.Xeric -> {
                        chatPlayer(quiz, "Who is Xeric?")
                        chatNpc(
                            neutral,
                            "Xeric was an incredibly powerful dark mage. He ruled Great Kourend " +
                                "for ten decades.",
                        )
                        chatNpc(
                            neutral,
                            "If you'd like to find out more you should visit the Historical " +
                                "Archive at the Grand Library.",
                        )
                        chambersMenu()
                    }
                    GnosiTopic.Chambers -> {
                        chatPlayer(quiz, "What are the Chambers of Xeric?")
                        chatNpc(
                            neutral,
                            "At the summit of this mountain we've found an ancient temple and a " +
                                "vast network of caves that we believe were formed by Xeric and " +
                                "his followers.",
                        )
                        chatNpc(
                            neutral,
                            "The discovery of the Chambers tells us Xeric invested significant " +
                                "time and resources here, yet it was never documented.",
                        )
                        chatNpc(
                            neutral,
                            "While much of Kourend's history under Xeric remains unknown, it is " +
                                "very surprising to discover such a huge area that was " +
                                "previously undiscovered to us.",
                        )
                        chambersMenu()
                    }
                    GnosiTopic.Explore -> {
                        chatPlayer(quiz, "Can I explore the Chambers?")
                        chatNpc(
                            neutral,
                            "Of course, though be careful. The dangers of the Chambers of Xeric " +
                                "are as yet unknown. You should take a strong group of " +
                                "companions with you.",
                        )
                        chambersMenu()
                    }
                    GnosiTopic.Purpose -> {
                        xericPurpose()
                        chambersMenu()
                    }
                    GnosiTopic.Goodbye -> return chatPlayer(neutral, "Goodbye.")
                }
        }
    }

    // TODO(content): [verify:in_game] which menu "Who is Xeric?" returns to
    private suspend fun Dialogue.chambersMenu(): GnosiTopic =
        choice4(
            "Can I explore the Chambers?",
            GnosiTopic.Explore,
            "What do you think Xeric used this area for?",
            GnosiTopic.Purpose,
            "Who is Xeric?",
            GnosiTopic.Xeric,
            "Goodbye.",
            GnosiTopic.Goodbye,
        )

    private suspend fun Dialogue.xericPurpose() {
        chatPlayer(quiz, "What do you think Xeric used this area for?")
        chatNpc(
            neutral,
            "When he ruled Kourend, we know Xeric performed experiments on the living, using " +
                "power drawn from the Dark Altar. After he was overthrown, he would have needed " +
                "a new source of power.",
        )
        if (QuestRequirements.hasCompleted(player, TALE_OF_THE_RIGHTEOUS)) {
            chatNpc(
                neutral,
                "We believe this altar is that source. It seems to act as an amplifier for the " +
                    "Dark Altar.",
            )
            chatNpc(
                neutral,
                "As for what he used the Chambers for, that we still don't know. There's a lot " +
                    "of creatures in the Chambers. Maybe he used them in his experiments.",
            )
            return
        }
        chatNpc(
            neutral,
            "We believe he came here to seek it. Xeric would also have required a source of many " +
                "creatures for these experiments, and the Chambers certainly contain many of " +
                "those!",
        )
        chatNpc(
            neutral,
            "He must have also had some form of teleportation in order to reach the temple " +
                "unnoticed.",
        )
        chatNpc(
            neutral,
            "With time and careful study we will uncover these secrets and be able to answer " +
                "these questions.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Historian_Duffy
    private suspend fun Dialogue.historianDuffy() {
        if (QuestRequirements.hasCompleted(player, TALE_OF_THE_RIGHTEOUS)) {
            chatPlayer(quiz, "How are things going?")
            chatNpc(
                happy,
                "Good thanks. Now that we've found this cave, we're learning more about the " +
                    "lizardmen than ever before.",
            )
            return
        }
        chatPlayer(quiz, "Hello. What are you doing here?")
        chatNpc(
            neutral,
            "Duffy's the name. I'm a natural historian from Varrock Museum. My team and I are " +
                "here to explore this mountain.",
        )
        chatPlayer(
            quiz,
            "It's just an old mountain isn't it? What natural history are you hoping to find " +
                "here?",
        )
        chatNpc(
            laugh,
            "Bahaha, you adventurers are all the same, never seeing past the fights and " +
                "treasure...",
        )
        chatNpc(
            neutral,
            "This Mountain is steeped in natural history, but our main focus is to study the " +
                "lizard creatures that surround this area.",
        )
        chatPlayer(quiz, "You're studying lizardmen?")
        chatNpc(
            happy,
            "Indeed, they're a fascinating species! We're hoping to learn about their origins " +
                "and how they've evolved.",
        )
        chatNpc(
            neutral,
            "However, we're not very familiar with this region so we've enlisted the help of the " +
                "Kourend Grand Library to aid us in our research.",
        )
        chatPlayer(quiz, "What have you learned so far?")
        chatNpc(
            neutral,
            "Well, we have a theory that lizard creatures aren't natural at all, but were " +
                "created by dark magical experiments.",
        )
        chatNpc(
            neutral,
            "Right now we're looking for evidence to prove this theory, but's it's proving " +
                "somewhat difficult.",
        )
        chatPlayer(quiz, "Can I help at all?")
        chatNpc(
            neutral,
            "No thanks adventurer, natural history studies are delicate and precise... but if we " +
                "ever need something haphazardly destroyed we'll let you know.",
        )
        chatPlayer(
            angry,
            "Not all adventurers only care about fighting, you know... I was only offering to " +
                "help.",
        )
        chatNpc(
            neutral,
            "We don't need your help. Run along now, go and play in the dungeons with the other " +
                "adventurers. I hear they're full of monsters to fight!",
        )
        chatNpc(laugh, "Try not to get yourself killed, it would be such a tragedy!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Natural_Historian_(Mount_Quidamortem)
    private suspend fun Dialogue.naturalHistorian() {
        chatPlayer(happy, "Hello.")
        when (access.random.of(5)) {
            0 -> chatNpc(worried, "I've lost my notes... Duffy, have you seen my notes?")
            1 -> chatNpc(worried, "My wife will kill me if I don't take her back a souvenir.")
            2 ->
                chatNpc(
                    sad,
                    "I've had such a terrible headache since we arrived on this mountain.",
                )
            3 ->
                chatNpc(
                    happy,
                    "Thank Saradomin for that mountain guide. We couldn't have made it here " +
                        "without him!",
                )
            else -> chatNpc(worried, "I can't find my rock pick!")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Mountain_Guide
    // TODO(content): [system:mountain_guide_travel] the Travel op and the trips to the Shayzien
    //  Outpost, Mount Quidamortem and south of Quidamortem
    private suspend fun Dialogue.mountainGuide() {
        if (player.mountainGuideUnlocked == 0) {
            return mountainGuideFirstMeeting()
        }
        chatPlayer(quiz, "Can you take me somewhere?")
        chatNpc(neutral, "Of course. Where would you like to go?")
        val option =
            choice4(
                "The Shayzien Outpost.",
                1,
                "Mount Quidamortem.",
                2,
                "South of Quidamortem.",
                3,
                "Nowhere.",
                4,
            )
        when (option) {
            1 -> chatPlayer(neutral, "The Shayzien Outpost.")
            2 -> chatPlayer(neutral, "Mount Quidamortem.")
            3 -> chatPlayer(neutral, "South of Quidamortem.")
            else -> {
                chatPlayer(neutral, "Nowhere.")
                chatNpc(neutral, "Okay. Come back if you change your mind.")
            }
        }
    }

    private suspend fun Dialogue.mountainGuideFirstMeeting() {
        chatPlayer(quiz, "Hello. What you are doing here?")
        chatNpc(
            neutral,
            "Hello, I am a mountain guide for this historical expedition. I led the team to this " +
                "summit.",
        )
        chatPlayer(quiz, "Would you be able to guide me up and down the mountain?")
        chatNpc(
            neutral,
            "I don't see why not. I've not much to do while the historians are exploring.",
        )
        chatNpc(
            neutral,
            "I make many trips each day so whenever you'd like to travel, just speak to me here, " +
                "at the Shayzien outpost, or south of the mountain.",
        )
        player.mountainGuideUnlocked = 1
        chatPlayer(happy, "Thanks!")
    }

    private enum class GnosiTopic {
        Xeric,
        Chambers,
        Explore,
        Purpose,
        Goodbye,
    }

    private companion object {
        const val TALE_OF_THE_RIGHTEOUS = "quest_taleoftherighteous"
        val MOUNTAIN_GUIDES =
            listOf(
                "npc.raids_mountainguide_temple_multi",
                "npc.raids_mountainguide_wall_multi",
                "npc.raids_mountainguide_south_multi",
            )
    }
}
