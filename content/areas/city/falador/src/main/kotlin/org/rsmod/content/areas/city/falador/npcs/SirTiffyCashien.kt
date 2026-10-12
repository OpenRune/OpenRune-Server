package org.rsmod.content.areas.city.falador.npcs

import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.shops.Shops
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.quest.manager.menu
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Sir_Tiffy_Cashien
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:respawn_points] "Can I switch respawns please?" (Falador/Lumbridge)
// TODO(content): [system:treasure_trails] Crack the Clue II
// TODO(content): [miniquest:knights_notes] using the knight's notes on him
class SirTiffyCashien
@Inject
constructor(private val shops: Shops, private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.rd_teleporter_guy") { startDialogue(it.npc) { sirTiffy() } }
    }

    private fun Dialogue.completed(quest: String): Boolean =
        QuestRequirements.hasCompleted(player, quest)

    private fun Dialogue.male(): Boolean = player.appearance.bodyType == Constants.bodytype_a

    private fun Dialogue.miladyOrSirrah(): String = if (male()) "sirrah" else "milady"

    private suspend fun Dialogue.sirTiffy() {
        when {
            completed(THE_SLUG_MENACE) -> proselyte()
            completed(WANTED) -> {
                chatNpc(happy, "Jolly good show on getting into the White Knights, what?")
                chatNpc(happy, "Now how can I help you old ${if (male()) "boy" else "girl"}?")
                templeKnightMenu(proselyte = false)
            }
            completed(RECRUITMENT_DRIVE) -> {
                val whatHo = if (male()) "What ho, sir." else "What ho, milady."
                chatNpc(happy, "$whatHo Jolly good show on the old training grounds thingy, what?")
                templeKnightMenu(proselyte = false)
            }
            else -> standard()
        }
    }

    private suspend fun Dialogue.standard() {
        val greeting = if (male()) "What ho, sir." else "What ho, milady."
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "$greeting Spiffing day for a walk in the park, what?")
        chatPlayer(confused, "...spiffing?")
        chatNpc(
            laugh,
            "Absolutely, top-hole! Well, can't stay and chat all day, dontchaknow! Ta-ta for now!",
        )
        chatPlayer(confused, "Erm... goodbye.")
    }

    private suspend fun Dialogue.proselyte() {
        if (!ownsCommOrb()) {
            chatPlayer(sad, "Hey Tiffy, I lost my CommOrb...")
            chatNpc(
                neutral,
                "Very careless of you, what? Here, take another, but be careful of this one!",
            )
            player.invAddOrDrop(objRepo, COMMORB)
            return
        }
        if (completed(WHILE_GUTHIX_SLEEPS)) {
            chatNpc(
                neutral,
                "Good day, Proselyte. I heard from Akrisae what happened with that Lucien " +
                    "business. Be sure to keep your chin up. We'll get the old boy sooner or later.",
            )
            chatPlayer(neutral, "Thanks, Tiffy.")
            chatNpc(
                happy,
                "Don't mention it, old bean. Now, is there anything I can do for you?",
            )
        } else {
            chatNpc(happy, "Good day Proselyte. Is there anything I can do for you?")
        }
        templeKnightMenu(proselyte = true)
    }

    private fun Dialogue.ownsCommOrb(): Boolean =
        access.inv.count(COMMORB) > 0 || access.bank.count(COMMORB) > 0

    private suspend fun Dialogue.templeKnightMenu(proselyte: Boolean) {
        while (true) {
            val option =
                menu(
                    buildList {
                        add("Do you have any jobs for me yet?" to JOBS)
                        if (proselyte && completed(SECRETS_OF_THE_NORTH)) {
                            add("I have news on that assassin who attacked Entrana." to ASSASSIN)
                        }
                        add("Can you explain the Gaze of Saradomin to me?" to GAZE)
                        add("Can I buy some armour?" to ARMOUR)
                        add("Goodbye." to GOODBYE)
                    }
                )
            when (option) {
                JOBS -> {
                    if (!proselyte) {
                        // TODO(content): [quest:wanted] quest start (after Recruitment Drive)
                        // TODO(content): [quest:the_slug_menace] quest start (after Wanted!)
                        return
                    }
                    chatPlayer(quiz, "Do you have any jobs for me yet?")
                    chatNpc(neutral, "Sorry old bean but we are still in the process of organising.")
                    chatNpc(
                        neutral,
                        "I'm sure that we will have something for you soon, so please feel free to " +
                            "check back later. Anything else I can do for you in the meantime?",
                    )
                }
                ASSASSIN -> assassinNews()
                GAZE -> gazeOfSaradomin()
                ARMOUR -> {
                    chatPlayer(quiz, "Can I buy some armour?")
                    val level = if (proselyte) "Proselyte" else "Initiate"
                    chatNpc(
                        happy,
                        "Of course old bean. I can sell you up to $level level items only I'm " +
                            "afraid.",
                    )
                    openArmoury(proselyte)
                    return
                }
                else -> {
                    chatPlayer(happy, "Well, see you around Tiffy.")
                    chatNpc(happy, "Ta-ta for now, old bean!")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.assassinNews() {
        chatPlayer(neutral, "I have news on that assassin who attacked Entrana.")
        chatNpc(neutral, "Ah yes, you've been helping out the Ardougne authorities I hear.")
        chatPlayer(shocked, "You knew?")
        chatNpc(
            happy,
            "Of course! Knowledge is my business, old bean, and it pays to know what's happening " +
                "across the other kingdoms.",
        )
        if (!completed(DESERT_TREASURE_II)) {
            chatNpc(
                neutral,
                "Now, as for your assassin, quite messy stuff, what? We've been trying to track " +
                    "him down ever since the Entrana incident, but he's proven quite the tricky one " +
                    "to pin down. Curious him showing up in Ardougne.",
            )
            chatPlayer(quiz, "What are your plans?")
            chatNpc(
                neutral,
                "Sadly, very little. Until we can learn more about what he's up to, we don't have " +
                    "much to go on. You have nothing to fear though, we'll find him sooner or later!",
            )
            chatNpc(happy, "Now, anything else you need, ${miladyOrSirrah()}?")
            return
        }
        chatPlayer(neutral, "Well did you also know he was a Mahjarrat?")
        chatNpc(shocked, "A Mahjarrat! You must be pulling on my leg, what?")
        chatPlayer(neutral, "His name is Sliske. Have you heard of him?")
        chatNpc(
            neutral,
            "Sliske, hmm? We have dossiers on many Mahjarrat, but I must confess, this is a new " +
                "name to me. Poor show! I'll have to see what we have hiding in the archives.",
        )
        chatPlayer(
            neutral,
            "While you're there, it might be worth looking up something called the Elder Horn. " +
                "It's an artefact that Sliske has now claimed. One that has the ability to control " +
                "minds.",
        )
        if (completed(WHILE_GUTHIX_SLEEPS)) {
            chatNpc(
                worried,
                "A mind controlling artefact you say? First Lucien and the Stone, and now this! " +
                    "The top brass will need to be made aware. Can't see them being too thrilled " +
                    "about it. Thank you for the update, old bean.",
            )
        } else {
            chatNpc(
                worried,
                "A mind controlling artefact you say? Sounds like I'll need to report this to the " +
                    "top brass. Can't see them being too thrilled about it. Thank you for the " +
                    "update, old bean.",
            )
        }
        chatNpc(happy, "Now, anything else you need, ${miladyOrSirrah()}?")
    }

    private suspend fun Dialogue.gazeOfSaradomin() {
        chatPlayer(
            confused,
            "I don't really understand this 'Gaze of Saradomin' thing... Do you think you could " +
                "explain what it does for me?",
        )
        chatNpc(
            happy,
            "Certainly ${miladyOrSirrah()}! As you know, we Temple Knights are personally favoured " +
                "by Saradomin himself.",
        )
        chatNpc(
            laugh,
            "And when I say personally favoured, I don't mean that sometime off in the future " +
                "he's going to buy us all a drink!",
        )
        chatNpc(
            neutral,
            "He watches over each of us, and when we die he catches us as we fall, and ensures we " +
                "arrive back at Falador castle safe and sound.",
        )
        chatNpc(
            neutral,
            "We usually lose some equipment when he does so, but it's a small price to pay to be " +
                "hale and hearty again, what?",
        )
        chatNpc(
            neutral,
            "Some lucky fellows have a similar system going already, but when they die they spawn " +
                "in that squalid little swamp village Lumbridge.",
        )
        chatPlayer(
            shifty,
            "Yeah, what kind of person would want to spawn there... Certainly not me, and I never " +
                "have! Honest!",
        )
        chatNpc(
            happy,
            "Well, you should be glad that we offer you a step up then! Falador is clearly a far " +
                "superior town to spend your time in!",
        )
        chatNpc(
            happy,
            "Was there something else you wanted to ask good old Tiffy, ${miladyOrSirrah()}?",
        )
    }

    // Prices: https://oldschool.runescape.wiki/w/Initiate_Temple_Knight_Armoury and
    // https://oldschool.runescape.wiki/w/Proselyte_Temple_Knight_Armoury (sells at 100%, buys at
    // 50%, change 0%), matching the invs' `buyMultiplier = 500` and `delta = 0`.
    private fun Dialogue.openArmoury(proselyte: Boolean) {
        shops.open(
            player = player,
            title =
                if (proselyte) "Proselyte Temple Knight Armoury"
                else "Initiate Temple Knight Armoury",
            shopInv = if (proselyte) PROSELYTE_ARMOURY else INITIATE_ARMOURY,
            buyPercentage = 50.0,
            sellPercentage = 100.0,
            changePercentage = 0.0,
        )
    }

    private companion object {
        const val RECRUITMENT_DRIVE = "quest_recruitmentdrive"
        const val WANTED = "quest_wanted"
        const val THE_SLUG_MENACE = "quest_slugmenace"
        const val WHILE_GUTHIX_SLEEPS = "quest_whileguthixsleeps"
        const val SECRETS_OF_THE_NORTH = "quest_secretsofthenorth"
        const val DESERT_TREASURE_II = "quest_deserttreasure2"

        const val COMMORB = "obj.slug2_crystal_ball"
        const val INITIATE_ARMOURY = "inv.templeknight_armoury1"
        const val PROSELYTE_ARMOURY = "inv.templeknight_armoury2"

        const val JOBS = 1
        const val ASSASSIN = 2
        const val GAZE = 3
        const val ARMOUR = 4
        const val GOODBYE = 5
    }
}
