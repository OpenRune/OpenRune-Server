package org.rsmod.content.areas.city.keldagrim.keldagrim.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class Consortium : PluginScript() {
    override fun ScriptContext.startup() {
        for (company in COMPANIES) {
            onOpNpc1(company.director) { startDialogue(it.npc) { director(company.name) } }
            onOpNpc1(company.secretary) { startDialogue(it.npc) { secretary(company) } }
        }
        for (director in RED_AXE_DIRECTORS) {
            onOpNpc1(director) { startDialogue(it.npc) { redAxeDirector() } }
        }
        for (secretary in RED_AXE_SECRETARIES) {
            onOpNpc1(secretary) { startDialogue(it.npc) { redAxeSecretary() } }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Blue_Opal_Director (one page per company)
    // TODO(content): [quest:the_giant_dwarf] dialogue for the company the player joined
    private suspend fun Dialogue.director(company: String) {
        val dismissal =
            when (access.random.of(10)) {
                0 -> "I'm too important to talk to the likes of you, leave."
                1 -> "My secretary will deal with any inquiries you have about the $company."
                2 -> "My secretary is sitting just over there, thank you."
                3 -> "I'm busy, please leave."
                4 -> "Today is not a good day, can you come back tomorrow?"
                5 -> "If you're looking for the market, it's downstairs."
                6 -> "Do you mind? You're blocking my view."
                7 -> "How did you get in here? I don't have time to talk to you!"
                8 -> "I can't help you, go and see my secretary."
                else -> "Talk to my secretary, I don't have time to deal with you."
            }
        chatNpc(angry, dismissal)
        chatPlayer(confused, "But...")
        if (access.random.of(2) == 0) {
            chatNpc(angry, "I said go!")
        } else {
            chatNpc(angry, "I told you to leave, human!")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Blue_Opal_Secretary (one page per company)
    // TODO(content): [verify:in_game] Purple Pewter compliment for players wearing H.A.M. robes
    private suspend fun Dialogue.secretary(company: Company) {
        val name = company.name
        val greeting =
            when (access.random.of(6)) {
                0 -> "What can I do for you, human?"
                1 -> "Can I help you with anything?"
                2 -> "Was there anything you wanted?"
                3 -> "What did you need of the ${company.inquiryName}?"
                4 -> "Is there anything you need assistance with?"
                else -> "And what can I do for you?"
            }
        chatNpc(neutral, greeting)
        while (true) {
            val option =
                choice4(
                    "Who are you?",
                    1,
                    "Where am I?",
                    2,
                    "Is there anything I can help you with?",
                    3,
                    "I have to go!",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(neutral, "I am the head secretary of the ${company.inquiryName}.")
                    chatPlayer(confused, "The what?")
                    chatNpc(neutral, company.introduction)
                }
                2 -> secretaryOffices(name)
                3 -> {
                    // TODO(content): [quest:the_giant_dwarf] the company task from the secretary
                    return
                }
                else -> {
                    if (company.saysGoodbye) {
                        chatPlayer(neutral, "I have to go!")
                    }
                    val farewell =
                        when (access.random.of(3)) {
                            0 -> "Goodbye, human."
                            1 -> "See you later."
                            else -> "Okay."
                        }
                    chatNpc(neutral, farewell)
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.secretaryOffices(company: String) {
        chatPlayer(quiz, "Where am I?")
        val place =
            when (access.random.of(3)) {
                0 -> "These are the offices of the $company."
                1 -> "This is the official residence of the $company."
                else -> "Why, you are in the headquarters of the $company."
            }
        chatNpc(neutral, place)
        chatNpc(
            neutral,
            "We share this floor of the palace with the other companies of the Consortium. And " +
                "trade with them on the Trade Octagon.",
        )
        chatPlayer(quiz, "The Trade Octagon?")
        chatNpc(
            neutral,
            "See those traders out there, just out the door? They're all standing in the Trade " +
                "Octagon. There's an awful lot of trade going on in the palace, you know.",
        )
        chatPlayer(quiz, "Anything else of interest in the palace?")
        chatNpc(
            neutral,
            "You will have to find that out on your own, I'm afraid. I do have more work to do.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Red_Axe_Director
    private suspend fun Dialogue.redAxeDirector() {
        chatNpc(angry, "I am conducting a meeting here, begone!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Red_Axe_Secretary
    private suspend fun Dialogue.redAxeSecretary() {
        chatNpc(angry, "Don't bother me!")
    }

    private data class Company(
        val name: String,
        val director: String,
        val secretary: String,
        val introduction: String,
        val inquiryName: String = name,
        val saysGoodbye: Boolean = true,
    )

    private companion object {
        val COMPANIES =
            listOf(
                Company(
                    name = "Blue Opal",
                    director = "npc.dwarf_city_director_blue_opal",
                    secretary = "npc.dwarf_city_secretary_blue_opal",
                    introduction =
                        "We are the Blue Opal. We are part of the Consortium, rulers of " +
                            "Keldagrim and beyond.",
                ),
                Company(
                    name = "Purple Pewter",
                    director = "npc.dwarf_city_director_purple_pewter",
                    secretary = "npc.dwarf_city_secretary_purple_pewter",
                    introduction =
                        "I said, the Purple Pewter. Together with the other companies, we rule " +
                            "Keldagrim and the dwarven realm.",
                ),
                Company(
                    name = "Yellow Fortune",
                    director = "npc.dwarf_city_director_yellow_fortune",
                    secretary = "npc.dwarf_city_secretary_yellow_fortune",
                    introduction =
                        "The Yellow Fortune! We are the grandest of the mining companies that " +
                            "make up the Consortium!",
                ),
                Company(
                    name = "Green Gemstone",
                    director = "npc.dwarf_city_director_green_gemstone",
                    secretary = "npc.dwarf_city_secretary_green_gemstone",
                    introduction =
                        "The Green Gemstone. We are part of the Consortium of mining companies " +
                            "that rule the dwarven realm.",
                ),
                Company(
                    name = "White Chisel",
                    director = "npc.dwarf_city_director_white_chisel",
                    secretary = "npc.dwarf_city_secretary_white_chisel",
                    introduction =
                        "The White Chisel. We are one of the most powerful companies in the " +
                            "whole of the dwarven realm, we are. We have grown so powerful that " +
                            "we are now part of the Consortium.",
                    saysGoodbye = false,
                ),
                Company(
                    name = "Silver Cog",
                    director = "npc.dwarf_city_director_silver_cog",
                    secretary = "npc.dwarf_city_secretary_silver_cog",
                    introduction =
                        "The Silver Cog! A shining example of how a mining company should be " +
                            "run to join the Consortium.",
                    inquiryName = "White Chisel",
                    saysGoodbye = false,
                ),
                Company(
                    name = "Brown Engine",
                    director = "npc.dwarf_city_director_brown_engine",
                    secretary = "npc.dwarf_city_secretary_brown_engine",
                    introduction =
                        "The Brown Engine. We are one of the eight major mining companies, and " +
                            "one of the finest if I do say so myself!",
                    inquiryName = "White Chisel",
                    saysGoodbye = false,
                ),
            )

        val RED_AXE_DIRECTORS =
            listOf("npc.dwarf_city_director_red_axe", "npc.dwarf_city_director_red_axe_multi")
        val RED_AXE_SECRETARIES =
            listOf("npc.dwarf_city_secretary_red_axe", "npc.dwarf_city_secretary_red_axe_multi")
    }
}
