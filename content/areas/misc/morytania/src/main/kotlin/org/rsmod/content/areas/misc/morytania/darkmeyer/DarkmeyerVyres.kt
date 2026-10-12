package org.rsmod.content.areas.misc.morytania.darkmeyer

import dev.openrune.types.MesAnimType
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:vyre_disguise] the disguised dialogue before The Blood Moon Rises;
//  without it every vyre sends the player back to the slave pens
class DarkmeyerVyres : PluginScript() {
    override fun ScriptContext.startup() {
        for (vyre in GENERIC_VYRES) {
            onOpNpc1(vyre) { startDialogue(it.npc) { vyre() } }
        }
        for (drinker in CRYPT_DRINKERS) {
            onOpNpc1(drinker) { startDialogue(it.npc) { cryptDrinker() } }
        }
        onOpNpc1("npc.alek_constantine") { startDialogue(it.npc) { alek() } }
        onOpNpc1("npc.caninelle_draynar") { startDialogue(it.npc) { draynars() } }
        onOpNpc1("npc.pipistrelle_draynar") { startDialogue(it.npc) { draynars() } }
        onOpNpc1("npc.natalidae_shadum") { startDialogue(it.npc) { natalidae() } }
        onOpNpc1("npc.vlad_diaemus") { startDialogue(it.npc) { vladDiaemus() } }
        onOpNpc1("npc.von_van_von") { startDialogue(it.npc) { vonVanVon() } }
        onOpNpc1("npc.vonnetta_varnis") { startDialogue(it.npc) { vonnetta() } }
    }

    private fun Dialogue.freedFromTithes(): Boolean =
        QuestRequirements.hasCompleted(player, BLOOD_MOON_RISES)

    private suspend fun Dialogue.slavePens() {
        chatNpc(angry, "Human! Get back to the slave pens before I call the guards!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Vyre
    // TODO(content): [verify:in_game] the transcript gives two endings to the theatre line under
    //  an unknown condition; the first is used
    private suspend fun Dialogue.vyre() {
        if (!freedFromTithes()) {
            return slavePens()
        }
        when (access.random.of(4)) {
            0 -> {
                chatPlayer(happy, "Hey.")
                chatNpc(neutral, "Yes?")
                chatPlayer(quiz, "What's there to do around here?")
                chatNpc(neutral, "Eat, drink and...")
                chatPlayer(happy, "Sounds great! I'll be sure to try those things.")
                chatNpc(confused, "Okay...")
            }
            1 -> {
                chatPlayer(happy, "Hey there.")
                chatNpc(neutral, "Hmmm?")
                chatPlayer(quiz, "What are you doing?")
                chatNpc(neutral, "Nothing.")
                chatPlayer(happy, "Sounds fun.")
                chatNpc(neutral, "It is.")
            }
            2 -> {
                chatNpc(angry, "What are you looking at?")
                chatPlayer(neutral, "You.")
                chatNpc(confused, "Why?")
                chatPlayer(neutral, "Why not?")
                chatNpc(neutral, "I guess. It's a bit weird though.")
                chatPlayer(
                    neutral,
                    "Many would say the same about drinking blood. What do they know though?",
                )
                chatNpc(neutral, "Well it's their loss.")
            }
            else -> {
                chatNpc(
                    bored,
                    "There's nothing to do in this city. Might be time to give the theatre a " +
                        "visit.",
                )
                chatPlayer(happy, "I hear it's all the rage.")
                chatNpc(
                    happy,
                    "Indeed it is. I'd recommend going yourself if you haven't been before.",
                )
                chatPlayer(
                    happy,
                    "I've been a few times. Always worth another trip though. See you around.",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Grigor_Rasputin
    // https://oldschool.runescape.wiki/w/Transcript:Crimsonette_van_Marr
    // https://oldschool.runescape.wiki/w/Transcript:Vormar_Vakan
    private suspend fun Dialogue.cryptDrinker() {
        if (!freedFromTithes()) {
            return slavePens()
        }
        if (access.random.of(2) == 0) {
            chatPlayer(quiz, "What are you drinking?")
            chatNpc(drunk, "Blood!")
            chatPlayer(confused, "How are you getting drunk on blood?")
            chatNpc(
                drunk,
                "They used to keep some of the tithes drunk all the time and then take it from " +
                    "them. Now the aranei that supply it intentionally get drunk first instead.",
            )
            chatPlayer(neutral, "How very creative.")
            chatNpc(drunk, "I know right! Bloody brilliant!")
        } else {
            chatPlayer(happy, "Hello.")
            chatNpc(drunk, "I'll have a pint please.")
            chatPlayer(neutral, "I don't work here.")
            chatNpc(drunk, "Sure you do!")
            chatPlayer(neutral, "No, I really don't.")
            chatNpc(drunk, "Well clear off then.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Alek_Constantine
    private suspend fun Dialogue.alek() {
        if (!freedFromTithes()) {
            return slavePens()
        }
        chatNpc(quiz, "Who are you?")
        chatPlayer(neutral, "${player.displayName}.")
        chatNpc(neutral, "Not the one I'm expecting. Move along.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Caninelle_Draynar
    private suspend fun Dialogue.draynars() {
        if (!freedFromTithes()) {
            return slavePens()
        }
        chatPlayer(happy, "Hello there.")
        caninelle(quiz, "Darling, who's this?")
        pipistrelle(neutral, "I don't know, darling. Why don't you ask them?")
        caninelle(happy, "Yes, good idea!")
        caninelle(quiz, "Who are you?")
        chatPlayer(neutral, "I'm ${player.displayName}.")
        caninelle(
            happy,
            "Hello, ${player.displayName}. I'm Caninelle Draynar and this is Pipistrelle Draynar.",
        )
        pipistrelle(happy, "Hello there!")
        chatPlayer(quiz, "Draynar? Any connection to Draynor?")
        pipistrelle(
            neutral,
            "Oh yes. The Draynar family has been around since before vampyres came to this world.",
        )
        caninelle(
            neutral,
            "Over time, some people took the name and started pronouncing it differently. Hence " +
                "Draynor.",
        )
        pipistrelle(
            neutral,
            "Draynor is more commonly used as a first name though, with Draynar as a last name.",
        )
        chatPlayer(
            happy,
            "Thanks for the information. I wonder if there's a Draynor Draynar out there.",
        )
        pipistrelle(laugh, "Now that would be funny.")
        chatPlayer(happy, "See you around.")
    }

    private suspend fun Dialogue.caninelle(mesanim: MesAnimType, text: String) {
        chatNpcSpecific("Caninelle Draynar", "npc.caninelle_draynar", mesanim, text)
    }

    private suspend fun Dialogue.pipistrelle(mesanim: MesAnimType, text: String) {
        chatNpcSpecific("Pipistrelle Draynar", "npc.pipistrelle_draynar", mesanim, text)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Natalidae_Shadum
    private suspend fun Dialogue.natalidae() {
        if (!freedFromTithes()) {
            return slavePens()
        }
        chatNpc(quiz, "Hey there. Have you seen Frank?")
        chatPlayer(confused, "Frank?")
        chatNpc(
            neutral,
            "Yes, Frank. He's my pet bloodveld. He runs off a lot so I was wondering if you'd " +
                "seen him.",
        )
        chatPlayer(neutral, "Frank doesn't seem like a very fitting name for a bloodveld.")
        chatNpc(
            neutral,
            "Oh, he was named after my last pet. That one wasn't a bloodveld though, that one " +
                "was a human.",
        )
        chatPlayer(shocked, "You had a pet human?")
        chatNpc(happy, "Yes! A pet human called Frank.")
        chatNpc(
            sad,
            "Frank was great. He died though. Mother said it was my fault, that I didn't feed " +
                "him properly. I'm not allowed pet humans now.",
        )
        chatNpc(
            happy,
            "It's okay though. Now I have Frank the bloodveld. He's great. I don't need to feed " +
                "him. He feeds himself.",
        )
        chatPlayer(neutral, "What a nice happy ending.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Vlad_Diaemus
    private suspend fun Dialogue.vladDiaemus() {
        if (!freedFromTithes()) {
            return slavePens()
        }
        chatNpc(angry, "I don't want whatever it is you're selling.")
        chatPlayer(confused, "But I'm not selling anything.")
        chatNpc(angry, "You think I believe that? Why else would you be here?")
        chatPlayer(neutral, "Just seeing the sights?")
        chatNpc(angry, "What sights? What's wrong with you.")
        chatPlayer(neutral, "Nothing...")
        chatNpc(angry, "Get out, you weirdo!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Von_van_Von
    private suspend fun Dialogue.vonVanVon() {
        if (!freedFromTithes()) {
            return slavePens()
        }
        chatPlayer(happy, "Hey! I'm ${player.displayName}. What's your name?")
        chatNpc(neutral, "Von van Von.")
        chatPlayer(confused, "I'm sorry... What?")
        chatNpc(angry, "Did I stutter? My name is Von van Von.")
        chatPlayer(confused, "Your name is Von van Von?")
        chatNpc(neutral, "Yes.")
        chatPlayer(confused, "Sorry, just to clarify. Von van Von? That's your name?")
        chatNpc(angry, "Yes! Is there a problem?")
        chatPlayer(neutral, "Well... I guess not.")
        chatNpc(angry, "Good! Now leave me alone!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Vonnetta_Varnis
    private suspend fun Dialogue.vonnetta() {
        if (!freedFromTithes()) {
            return slavePens()
        }
        chatNpc(angry, "Get out of my house!")
        chatPlayer(worried, "But I...")
        chatNpc(angry, "But nothing! Get out!")
    }

    private companion object {
        const val BLOOD_MOON_RISES = "quest_bloodmoonrises"

        val CRYPT_DRINKERS =
            listOf("npc.grigor_rasputin", "npc.crimsonette_van_marr", "npc.vormar_vakan")

        val GENERIC_VYRES =
            listOf(
                "npc.carnivus_belamorta",
                "npc.diphylla_bechstein",
                "npc.draconis_sanguine",
                "npc.episcula_helsing",
                "npc.haemas_lamescus",
                "npc.lasenna_rasputin",
                "npc.misdrievus_shadum",
                "npc.mort_nightshade",
                "npc.mortina_daubenton",
                "npc.nakasa_jovkai",
                "npc.noctillion_lugosi",
                "npc.remus_kaninus",
                "npc.valentin_rasputin",
                "npc.valentina_diaemus",
                "npc.vallessia_dracyula",
                "npc.vallessia_von_pitt",
                "npc.vampyressa_van_von",
                "npc.vampyrus_diaemus",
                "npc.violetta_sanguine",
                "npc.vlad_bechstein",
            )
    }
}
