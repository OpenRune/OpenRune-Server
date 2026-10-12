package org.rsmod.content.areas.city.varlamore.civitas.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.areas.city.varlamore.npcs.isWounded
import org.rsmod.content.areas.city.varlamore.npcs.ralosHeal
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class CivitasTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        for (npc in ACADEMICS) onOpNpc1(npc) { startDialogue(it.npc) { academic() } }
        for (npc in WEALTHY_CITIZENS) onOpNpc3(npc) { startDialogue(it.npc) { wealthyCitizen() } }
        for (npc in STREET_URCHINS) onOpNpc1(npc) { startDialogue(it.npc) { streetUrchin() } }
        onOpNpc1("npc.varlamore_thieving_house_owner_1") {
            startDialogue(it.npc) { houseOwner(sirOrMadam = "madam") }
        }
        onOpNpc1("npc.varlamore_thieving_house_owner_2") {
            startDialogue(it.npc) { houseOwner(sirOrMadam = "sir") }
        }
        onOpNpc1("npc.varlamore_thieving_house_owner_3") {
            startDialogue(it.npc) { houseOwner(sirOrMadam = "sir") }
        }
        onOpNpc1("npc.fortis_teoki_ralos") { startDialogue(it.npc) { cuali() } }
        onOpNpc1("npc.fortis_teoki_ralos_vis") { startDialogue(it.npc) { cuali() } }
        onOpNpc1("npc.fortis_teoki_ranul") { startDialogue(it.npc) { hegio() } }
        onOpNpc1("npc.fortis_teoki_ranul_vis") { startDialogue(it.npc) { hegio() } }
        onOpNpc1("npc.fortis_barracks_captain") { startDialogue(it.npc) { guardCaptain() } }
        onOpNpc1("npc.fortis_prison_warden") { startDialogue(it.npc) { warden() } }
        onOpNpc1("npc.fortis_palace_advisor") { startDialogue(it.npc) { helvia() } }
        onOpNpc1("npc.afl_atza") { startDialogue(it.npc) { atza() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Academic
    private suspend fun Dialogue.academic() {
        chatPlayer(happy, "Hello there.")
        when (access.random.of(5)) {
            0 -> chatNpc(happy, "Nilsal! Welcome to the Grand Museum.")
            1 -> chatNpc(happy, "Hello there! Hope you're enjoying the museum.")
            2 ->
                chatNpc(
                    neutral,
                    "Sorry, I'd love to stop and talk, but I have some studying to do.",
                )
            3 -> chatNpc(happy, "Have you seen the new Old One display? Fascinating stuff!")
            else ->
                chatNpc(
                    happy,
                    "Isn't this place wonderful? There's just so much knowledge to be found!",
                )
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Wealthy_citizen
    private suspend fun Dialogue.wealthyCitizen() {
        chatPlayer(happy, "Hello there.")
        chatNpc(happy, "Hello. How are you liking the city?")
        chatPlayer(happy, "It's truly spectacular.")
        chatNpc(
            happy,
            "I'd recommend the Colosseum. Me and a few associates always wager bets on the " +
                "contenders.",
        )
        chatNpc(
            happy,
            "As you can tell, I'm doing quite well winning! Plenty of coin to pass around.",
        )
        chatPlayer(quiz, "Got any spare for me?")
        chatNpc(angry, "No! We don't care for begging in the city!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Leo_(Civitas_illa_Fortis)
    // https://oldschool.runescape.wiki/w/Transcript:Julia
    // https://oldschool.runescape.wiki/w/Transcript:Aurelia
    // TODO(content): [verify:in_game] Cassius has no transcript; he uses the other urchins' lines
    // TODO(content): [system:wealthy_citizen_distractions] "Now leave me be. I'm already
    //  distracting!" while the urchin is distracting a wealthy citizen
    private suspend fun Dialogue.streetUrchin() {
        chatPlayer(quiz, "What are you doing here?")
        chatNpc(shifty, "I'm blending in. Scopin' out the place.")
        chatPlayer(confused, "Are you preparing for a heist? You're only a kid.")
        chatNpc(shifty, "Let's just say there are some wealthy people about, with full pockets.")
        chatPlayer(quiz, "Any tips? I'm not having much luck myself.")
        chatNpc(
            shifty,
            "I'm quite sneaky. Every now and then I'll get their attention so their pockets " +
                "aren't being so carefully watched.",
        )
        chatPlayer(happy, "That is sneaky. I'll keep an eye out for your... distractions.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lavinia
    // https://oldschool.runescape.wiki/w/Transcript:Victor_(Civitas_illa_Fortis)
    // https://oldschool.runescape.wiki/w/Transcript:Caius
    // TODO(content): [system:stealing_valuables] the "outside the residence" lines once the
    //  homeowners leave their houses
    private suspend fun Dialogue.houseOwner(sirOrMadam: String) {
        when (access.random.of(5)) {
            0 -> {
                chatNpc(confused, "Have I seen you before?")
                chatPlayer(shifty, "I don't think so... I've just got one of those faces.")
                chatNpc(neutral, "You're probably right.")
            }
            1 -> {
                chatNpc(
                    neutral,
                    "I'm rather busy at the moment. I've got to organise my cupboards of jewels.",
                )
                chatPlayer(happy, "Want any help?")
                chatNpc(angry, "Absolutely not! You stay away from my jewels.")
            }
            2 -> {
                chatNpc(quiz, "Interested in buying a house like mine?")
                chatPlayer(neutral, "Not sure I've got enough coins to do that.")
                chatNpc(laugh, "Silly me! I mistook you for one of us wealthy folk.")
                chatPlayer(angry, "Well that's rude.")
                chatNpc(neutral, "You'll catch on.")
            }
            3 -> {
                chatNpc(happy, "Hello there. Are you liking the city?")
                chatPlayer(happy, "It's beautiful!")
                chatNpc(happy, "It really is.")
            }
            else -> {
                chatNpc(confused, "Huh? Are you an encyclopedia salesman?")
                chatPlayer(neutral, "No, $sirOrMadam, I'm a burglar. I burgle people.")
                chatNpc(confused, "Hmm... I think you're an encyclopedia salesman?")
                chatPlayer(neutral, "Not at all. I just want to ransack the place. Honestly.")
                chatNpc(neutral, "Hmm... Alright then.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Cuali,_Teoki_of_Ralos
    private suspend fun Dialogue.cuali() {
        chatNpc(happy, "Nilsal, my child. Welcome to this holy temple of Ralos.")
        chatPlayer(happy, "Thank you.")
        chatNpc(happy, "If there is anything I can do for you, please just let me know.")
        while (true) {
            val option =
                choice3(
                    "Can you heal me? I'm injured.",
                    1,
                    "What is it you do here?",
                    2,
                    "I'd best get going.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(sad, "Can you heal me? I'm injured.")
                    if (!isWounded()) {
                        chatNpc(neutral, "Sorry, but I cannot.")
                    } else {
                        chatNpc(happy, "Of course.")
                        ralosHeal()
                        mesbox("The teoki whispers a prayer and you suddenly feel a bit better.")
                    }
                }
                2 -> {
                    chatPlayer(quiz, "What is it you do here?")
                    chatNpc(happy, "I am one of the seven teoki in service to Ralos.")
                    chatPlayer(quiz, "Seven? That's quite a specific number.")
                    chatNpc(
                        happy,
                        "Indeed. Along with our counterparts from the Sect of Ranul, each of us " +
                            "is responsible for one of seven key holy sites across the region.",
                    )
                    chatNpc(
                        happy,
                        "As you have likely guessed, my duty is to maintain the temple here in " +
                            "Fortis.",
                    )
                }
                else -> {
                    chatPlayer(neutral, "I'd best get going.")
                    chatNpc(happy, "Timoiva, and may the sun light your way.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hegio,_Teoki_of_Ranul
    private suspend fun Dialogue.hegio() {
        chatNpc(
            neutral,
            "Nilsal. Here to pay respects, or just here to appreciate the calmness of this crypt?",
        )
        chatPlayer(neutral, "Just looking around really.")
        chatNpc(
            neutral,
            "Please do. Many find this a nice place to come from time to time. The bustle of the " +
                "city can be overwhelming. Here you can contemplate many things in peace, " +
                "watched over by Ranul himself.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Guard_Captain_(Civitas_illa_Fortis)
    private suspend fun Dialogue.guardCaptain() {
        chatNpc(angry, "What are you doing here? You're not one of the guards.")
        chatPlayer(quiz, "I was just exploring. Is that okay?")
        chatNpc(confused, "I suppose, but why would you want to explore down here?")
        chatPlayer(quiz, "Well why not?")
        chatNpc(neutral, "It's cold for a start.")
        chatPlayer(
            neutral,
            "I'd say it's still pretty warm compared to some places I've been.",
        )
        chatNpc(neutral, "If you say so. Just don't go causing any trouble.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Warden_(Civitas_illa_Fortis)
    // TODO(content): [quest:the_final_dawn] the after-quest lines about the newest guest
    private suspend fun Dialogue.warden() {
        chatNpc(neutral, "Yes? Do you have business with one of the prisoners?")
        chatPlayer(quiz, "I don't think so. What are they in for?")
        chatNpc(neutral, "Does it matter? They are criminals.")
        chatPlayer(quiz, "Are all criminals the same?")
        chatNpc(neutral, "They will be if they're found guilty.")
        chatPlayer(quiz, "What happens if they are?")
        chatNpc(neutral, "In most cases, death.")
        chatPlayer(shocked, "Ah, nice. Bit brutal though, right?")
        chatNpc(neutral, "Brutal? Yes. Effective? Yes.")
        chatPlayer(neutral, "Fair enough.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Helvia (before The Final Dawn)
    // TODO(content): [quest:the_final_dawn] the after-quest line about speaking with the Queen
    private suspend fun Dialogue.helvia() {
        chatPlayer(happy, "Hello!")
        chatNpc(neutral, "You do not have an appointment.")
        chatPlayer(confused, "Sorry, what?")
        chatNpc(neutral, "You do not have an appointment.")
        chatPlayer(confused, "Appointment for what?")
        chatNpc(neutral, "An appointment to see the Queen.")
        chatPlayer(quiz, "How do you know that?")
        chatNpc(
            neutral,
            "First, because it is my job. Second, because she is not in Fortis at the moment.",
        )
        chatPlayer(quiz, "Oh. Well where is she then?")
        chatNpc(
            neutral,
            "She has important business elsewhere in the kingdom. Perhaps when she returns, " +
                "you'll be able to see her, or perhaps you won't.",
        )
        chatPlayer(neutral, "Yes, those generally are the two options.")
        chatNpc(neutral, "Now, if there's nothing else, please move along.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Atza (before At First Light)
    // TODO(content): [quest:at_first_light] the after-quest lines about Fox's material
    private suspend fun Dialogue.atza() {
        chatNpc(happy, "Hey there. How's it going?")
        chatPlayer(happy, "Good thanks.")
        chatNpc(happy, "Glad to hear it.")
    }

    private companion object {
        val ACADEMICS =
            listOf(
                "npc.fortis_academic_01",
                "npc.fortis_academic_02",
                "npc.fortis_academic_03",
                "npc.fortis_academic_04",
            )

        val WEALTHY_CITIZENS =
            listOf(
                "npc.varlamore_wealthy_citizen_a",
                "npc.varlamore_wealthy_citizen_b",
                "npc.varlamore_wealthy_citizen_c",
                "npc.varlamore_wealthy_citizen_d",
            )

        val STREET_URCHINS =
            listOf(
                "npc.varlamore_thieving_street_urchin_a",
                "npc.varlamore_thieving_street_urchin_b",
                "npc.varlamore_thieving_street_urchin_c",
                "npc.varlamore_thieving_street_urchin_d",
            )
    }
}
