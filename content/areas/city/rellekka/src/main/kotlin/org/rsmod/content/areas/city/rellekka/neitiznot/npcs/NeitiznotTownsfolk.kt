package org.rsmod.content.areas.city.rellekka.neitiznot.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.areas.city.rellekka.fremennikName
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class NeitiznotTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.frisb_n_pesant_f") { startDialogue(it.npc) { anneIsaakson() } }
        onOpNpc1("npc.frisb_n_pesant_m") { startDialogue(it.npc) { gunnarHoldstrom() } }
        onOpNpc1("npc.frisb_n_pesant_f2") { startDialogue(it.npc) { lisseIsaakson() } }
        onOpNpc1("npc.frisb_n_pesant_m2") { startDialogue(it.npc) { mortenHoldstrom() } }
        onOpNpc1("npc.fris_spymaster") { startDialogue(it.npc) { slugHemligssen() } }
        onOpNpc1("npc.fris_r_gaurdcaptain") { startDialogue(it.npc) { fridleifShieldson() } }
        onOpNpc1("npc.frisb_bouncer_1") { startDialogue(it.npc) { trollKingSlayer() } }
        onOpNpc1("npc.frisb_bouncer_2") { startDialogue(it.npc) { trollKingSlayer() } }
        onOpNpc1("npc.frisd_iznot_viking_shouter") { startDialogue(it.npc) { watchtowerGuard() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Anne_Isaakson
    private suspend fun Dialogue.anneIsaakson() {
        chatNpc(happy, "Hello visitor, how are you?")
        chatPlayer(
            happy,
            "Better than expected. It's a lot...nicer...here than I was expecting. Everyone " +
                "seems pretty happy.",
        )
        chatNpc(happy, "Of course, the Burgher is strong and wise, and looks after us well.")
        chatPlayer(
            neutral,
            "I think some of those Jatizso citizens have got the wrong idea about this place.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gunnar_Holdstrom
    private suspend fun Dialogue.gunnarHoldstrom() {
        chatNpc(happy, "Ah, isn't it a lovely day?")
        chatPlayer(quiz, "It's not bad. What puts you in such a good mood?")
        chatNpc(
            happy,
            "Oh, I just love my job. The smell of the sea breeze, the smell of the arctic pine " +
                "sap, the smell of the yaks. I find it all so bracing.",
        )
        chatPlayer(confused, "Bracing? Hmmm. I think I might have chosen a different word.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lisse_Isaakson
    // TODO(content): [system:treasure_trails] elite clue challenge scroll
    private suspend fun Dialogue.lisseIsaakson() {
        chatNpc(happy, "Hello, visitor!")
        chatPlayer(quiz, "Hello. What are you up to?")
        chatNpc(happy, "Ah, I was about to collect some yak's milk to make yak cheese.")
        chatPlayer(quiz, "Eughr! Though I am curious. Can I try some?")
        chatNpc(neutral, "Sorry, no. The last outlander who ate my cheese was ill for a month.")
        chatPlayer(quiz, "So why don't you get ill as well?")
        chatNpc(
            happy,
            "Well, we eat yak milk products every day, from when we're born. So I suppose we're " +
                "used to it. Anyway I should stop yakking - haha - and get on with my work.",
        )
        chatPlayer(
            laugh,
            "I'm glad to see that puns are common everywhere in Gielinor; even here.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Morten_Holdstrom
    private suspend fun Dialogue.mortenHoldstrom() {
        chatNpc(happy, "Good day to you.")
        chatPlayer(quiz, "Hello. What are you up to?")
        chatNpc(
            happy,
            "Ah, today is a surströmming day! The herring I buried six months ago is ready to " +
                "be dug up.",
        )
        chatPlayer(quiz, "Eughr! What are you going to do with it?")
        chatNpc(happy, "Eat it, of course! It will be fermented just-right by now.")
        chatPlayer(shocked, "Fermented? You eat rotten fish?")
        chatNpc(happy, "Hmmm, tasty. I'm guessing you don't want to come round and try it?")
        chatPlayer(neutral, "You guess correctly.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Slug_Hemligssen (standard dialogue)
    // TODO(content): [quest:the_fremennik_isles] Operative Budgie briefing and the jester
    // infiltration lines during the quest
    private suspend fun Dialogue.slugHemligssen() {
        chatNpc(shifty, "Shhh. Go away. I'm not allowed to talk to you.")
        chatPlayer(bored, "Fine, whatever ...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fridleif_Shieldson
    // TODO(content): [quest:the_fremennik_isles] dialogue before and during the quest
    private suspend fun Dialogue.fridleifShieldson() {
        chatNpc(
            happy,
            "Congratulations, Champion ${fremennikName()}. Thank you for defeating the Troll King.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Kjedelig_Uppsen
    // https://oldschool.runescape.wiki/w/Transcript:Trogen_Konungarde
    // TODO(content): [quest:the_fremennik_isles] dialogue before and during the quest
    private suspend fun Dialogue.trollKingSlayer() {
        chatNpc(happy, "Ho, ${fremennikName()}! The brave warrior returns!")
        chatPlayer(happy, "It was a hard-fought battle, but we Fremennik prevailed!")
        chatNpc(
            happy,
            "I expect we'll be hearing songs about the 'Slayer of the Troll King' for many " +
                "years to come.",
        )
        chatPlayer(happy, "It would be an honour to be remembered in such a way! Thanks.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Guard_(Neitiznot_watchtower)
    // TODO(content): [system:cutscene] the shouting cutscene with the Jatizso watchtower guard,
    // after "Certainly." and on the Watch-shouting op
    private suspend fun Dialogue.watchtowerGuard() {
        chatPlayer(quiz, "What's going on here?")
        chatNpc(
            neutral,
            "Ah, this is a strange ritual that has been going on for generations.",
        )
        chatNpc(
            neutral,
            "One of our clan stands on this tower and shouts insults at the other side.",
        )
        chatPlayer(quiz, "I'm surprised your Burgher approves of this sort of thing.")
        chatNpc(
            neutral,
            "He suggested stopping it, but there was resistance from the rest of the village. " +
                "We find it therapeutic.",
        )
        val option =
            choice2(
                "Can I watch? I like odd rituals.",
                1,
                "Oh well, I'd better get going",
                2,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "Can I watch? I like odd rituals.")
                chatNpc(happy, "Certainly.")
            }
            2 -> chatPlayer(neutral, "Oh well, I'd better get going")
        }
    }
}
