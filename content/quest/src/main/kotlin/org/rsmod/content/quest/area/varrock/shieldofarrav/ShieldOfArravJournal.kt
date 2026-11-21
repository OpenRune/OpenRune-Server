package org.rsmod.content.quest.area.varrock.shieldofarrav

import org.rsmod.content.quest.manager.QuestJournalBuilder
import org.rsmod.game.entity.Player

internal object ShieldOfArravJournal {
    fun write(journal: QuestJournalBuilder, player: Player) {
        with(journal) {
            when (player.phoenixGang) {
                0,
                PHOENIX_STARTED -> {
                    line(START)
                    return
                }
                PHOENIX_BOOK_READ -> {
                    line("$READ_BOOK I should discuss this with <red>Reldo</red>.")
                    return
                }
            }
            strike(plain(READ_BOOK))
            val baraek = player.phoenixGang >= PHOENIX_BARAEK_PAID
            val charlie = player.metCharlie
            if (!baraek && !charlie) {
                line(RELDO_GANGS)
                return
            }
            strike(plain(RELDO_GANGS_DONE))
            val phoenixFirst =
                player.isPhoenixMember ||
                    (!player.isBlackArmMember && player.phoenixGang > PHOENIX_BARAEK_PAID)
            if (phoenixFirst) {
                phoenixTrack(player)
                blackArmTrack(player, secondary = true)
            } else {
                blackArmTrack(player, secondary = false)
                phoenixTrack(player, secondary = true)
            }
        }
    }

    fun writeComplete(journal: QuestJournalBuilder, player: Player) {
        with(journal) {
            val phoenix = player.phoenixGang >= PHOENIX_COMPLETE
            strike(plain(museum(phoenix, MuseumStep.CERTIFICATE_MADE, finished = true)))
            strike(plain(ROALD_REWARDED))
        }
    }

    private fun QuestJournalBuilder.blackArmTrack(player: Player, secondary: Boolean = false) {
        if (player.isPhoenixMember) {
            return
        }
        if (!player.metCharlie) {
            if (!secondary || player.phoenixGang >= PHOENIX_BARAEK_PAID) line(CHARLIE_HINT)
            return
        }
        if (player.blackArmGang < BLACKARM_TASK) {
            line(CHARLIE_MET)
            return
        }
        strike(plain(CHARLIE_MET))
        if (player.blackArmGang == BLACKARM_TASK) {
            when {
                player.inv.count(CROSSBOW) >= 2 -> line(KATRINE_CROSSBOWS_GOT)
                player.charliePaid == 1 -> {
                    strike(plain(KATRINE_TASK))
                    line(CHARLIE_DEPOT)
                }
                else -> line(KATRINE_TASK)
            }
            return
        }
        strike(plain(KATRINE_TASK_DONE))
        val step = museumStep(player, BLACKARM_SHIELD)
        if (step == MuseumStep.NONE) {
            line("$BLACKARM_JOINED $SEARCH_HQ")
            return
        }
        strike(plain(BLACKARM_JOINED))
        line(museum(phoenix = false, step))
    }

    private fun QuestJournalBuilder.phoenixTrack(player: Player, secondary: Boolean = false) {
        if (player.isBlackArmMember) {
            return
        }
        if (player.phoenixGang < PHOENIX_BARAEK_PAID) {
            if (!secondary || player.metCharlie) line(BARAEK_HINT)
            return
        }
        if (player.phoenixGang < PHOENIX_TASK) {
            line(BARAEK_PAID)
            return
        }
        strike(plain(BARAEK_PAID))
        if (player.phoenixGang == PHOENIX_TASK) {
            if (player.owns(INTEL_REPORT)) {
                strike(plain(STRAVEN_TASK_DONE))
                line(JONNY_KILLED)
            } else {
                line(STRAVEN_TASK)
            }
            return
        }
        strike(plain(STRAVEN_TASK_DONE))
        val step = museumStep(player, PHOENIX_SHIELD)
        if (step == MuseumStep.NONE) {
            line("$PHOENIX_JOINED $SEARCH_HQ")
            return
        }
        strike(plain(PHOENIX_JOINED))
        line(museum(phoenix = true, step))
    }

    private enum class MuseumStep {
        NONE,
        FOUND,
        GIVEN,
        SWAPPED,
        CERTIFICATE_MADE,
    }

    private fun museumStep(player: Player, half: String): MuseumStep {
        val otherHalf = if (half == PHOENIX_SHIELD) BLACKARM_HALF else PHOENIX_HALF
        return when {
            player.shieldGiven != 0 && player.owns(CERTIFICATE) -> MuseumStep.CERTIFICATE_MADE
            player.shieldGiven != 0 && player.owns(otherHalf) -> MuseumStep.SWAPPED
            player.shieldGiven != 0 -> MuseumStep.GIVEN
            player.owns(half) -> MuseumStep.FOUND
            else -> MuseumStep.NONE
        }
    }

    private fun museum(phoenix: Boolean, step: MuseumStep, finished: Boolean = false): String {
        val hq = if (phoenix) "<red>Phoenix Gang Headquarters</red>" else "<red>Black Arm Gang Headquarters</red>"
        if (step == MuseumStep.FOUND) {
            return "I searched the $hq and found what seems to be half of the " +
                "<red>Shield of Arrav</red>. I should take this <red>Shield Half</red> to the " +
                "<red>Varrock Museum</red> to see if it's genuine."
        }
        val found =
            "I searched the $hq and found half of the <red>Shield of Arrav</red>. I took this " +
                "<red>Shield Half</red> to the <red>Varrock Museum</red> and " +
                "<red>Curator Haig Halen</red> confirmed it to be genuine."
        val helper =
            if (phoenix) "a <red>Fellow Adventurer</red>"
            else "the <red>Fellow Adventurer</red> who helped me earlier"
        return when (step) {
            MuseumStep.GIVEN -> {
                val agreed =
                    if (phoenix) "any <red>Fellow Adventurer</red> able to recover"
                    else "the <red>Fellow Adventurer</red> who helped me earlier if they can recover"
                "$found He then gave me two <red>Half-Certificates</red> and agreed to do the " +
                    "same for $agreed the other <red>Shield Half</red>. At that point, the " +
                    "<red>Fellow Adventurer</red> and I can swap the <red>Half-Certificates</red> " +
                    "to make a <red>Full Certificate</red> each."
            }
            MuseumStep.SWAPPED ->
                "$found He then gave me two <red>Half-Certificates</red>. Meanwhile, $helper " +
                    "was able to recover the other <red>Shield Half</red> and obtain two " +
                    "<red>Half-Certificates</red> for themselves. We swapped these " +
                    "<red>Half-Certificates</red>, meaning we can now both make a " +
                    "<red>Full Certificate</red> each."
            else -> {
                val text =
                    "$found He then gave me two <red>Half-Certificates</red>. Meanwhile, $helper " +
                        "was able to recover the other <red>Shield Half</red> and obtain two " +
                        "<red>Half-Certificates</red> for themselves. We swapped these " +
                        "<red>Half-Certificates</red>, allowing us to make a " +
                        "<red>Full Certificate</red> each."
                if (finished) text
                else
                    "$text I should now take my <red>Certificate</red> to <red>King Roald</red> " +
                        "in <red>Varrock Palace</red> to collect my reward."
            }
        }
    }

    private val Player.metCharlie: Boolean
        get() = charlieMet == 1 || blackArmGang >= BLACKARM_TASK

    private fun plain(text: String): String = text.replace("<red>", "").replace("</red>", "")

    private const val START =
        "I spoke to <red>Reldo</red> in the <red>Varrock Palace Library</red> and asked him if " +
            "he had any quests for me. He suggested I read a book called " +
            "<red>The Shield of Arrav</red>. The book should be somewhere in the " +
            "<red>Library</red>."
    private const val READ_BOOK =
        "I spoke to <red>Reldo</red> in the <red>Varrock Palace Library</red>. At his " +
            "suggestion, I read a book called <red>The Shield of Arrav</red>. The book detailed " +
            "the theft of the legendary <red>Shield of Arrav</red> and the reward on offer to " +
            "anyone able to recover it."
    private const val RELDO_GANGS =
        "I spoke to <red>Reldo</red> about the missing <red>Shield of Arrav</red>. He gave me " +
            "details on how I can find the <red>Phoenix Gang</red> and the " +
            "<red>Black Arm Gang</red>, the two groups suspected of being in possession of the " +
            "<red>Shield</red>. Apparently, <red>Baraek</red> in <red>Varrock Square</red> can " +
            "give me details on the <red>Phoenix Gang</red> and <red>Charlie the Tramp</red> " +
            "near the <red>Varrock South Gate</red> can do the same for the " +
            "<red>Black Arm Gang</red>."
    private const val RELDO_GANGS_DONE =
        "I spoke to Reldo about the missing Shield of Arrav. He gave me details on the Phoenix " +
            "Gang and the Black Arm Gang, the two groups suspected of being in possession of " +
            "the Shield."
    private const val CHARLIE_MET =
        "Following advice from <red>Reldo</red>, I spoke to <red>Charlie the Tramp</red> near " +
            "the <red>Varrock South Gate</red> about the <red>Black Arm Gang</red>. He directed " +
            "me to speak to <red>Katrine</red> in the <red>Gang Headquarters</red> to the west " +
            "of the <red>Varrock South Gate</red>."
    private const val CHARLIE_HINT =
        "According to <red>Reldo</red>, <red>Charlie the Tramp</red> near the " +
            "<red>Varrock South Gate</red> can give me details on the <red>Black Arm Gang</red>."
    private const val BARAEK_PAID =
        "Following advice from <red>Reldo</red>, I spoke to <red>Baraek</red> in " +
            "<red>Varrock Square</red> about the <red>Phoenix Gang</red>. He told me I'd find the " +
            "<red>Gang Headquarters</red> in the <red>VTAM Building</red> south east of the " +
            "<red>Blue Moon Inn</red>."
    private const val BARAEK_HINT =
        "According to <red>Reldo</red>, <red>Baraek</red> in <red>Varrock Square</red> can give " +
            "me details on the <red>Phoenix Gang</red>."
    private const val KATRINE_TASK_DONE =
        "I spoke to Katrine in the Black Arm Gang Headquarters to the west of the Varrock South " +
            "Gate. She offered to let me join the Gang in return for stealing two Phoenix " +
            "Crossbows from the Phoenix Gang."
    private const val KATRINE_TASK =
        "I spoke to <red>Katrine</red> in the <red>Black Arm Gang Headquarters</red> to the west " +
            "of the <red>Varrock South Gate</red>. She offered to let me join the " +
            "<red>Gang</red> in return for stealing two <red>Phoenix Crossbows</red> from the " +
            "<red>Phoenix Gang</red>. She suggested I speak to <red>Charlie the Tramp</red> " +
            "about where to find these <red>Crossbows</red>."
    private const val KATRINE_CROSSBOWS_GOT =
        "I spoke to <red>Katrine</red> in the <red>Black Arm Gang Headquarters</red> to the west " +
            "of the <red>Varrock South Gate</red>. She offered to let me join the Gang in return " +
            "for stealing two <red>Phoenix Crossbows</red> from the <red>Phoenix Gang</red>. With " +
            "the help of a <red>Fellow Adventurer</red>, I was able to obtain these " +
            "<red>Crossbows</red>. I should now return them to <red>Katrine</red>."
    private const val CHARLIE_DEPOT =
        "I spoke to <red>Charlie the Tramp</red> and he told me I'd find the " +
            "<red>Phoenix Gang Headquarters</red> in the <red>VTAM Building</red> south east of " +
            "the <red>Blue Moon Inn</red>. However, he suspected I'd be unable to gain access on " +
            "my own. He suggested I seek help by asking a <red>Fellow Adventurer</red> to " +
            "infiltrate the <red>Phoenix Gang</red>."
    private const val BLACKARM_JOINED =
        "With the help of a <red>Fellow Adventurer</red>, I was able to obtain two " +
            "<red>Phoenix Crossbows</red>. I gave them to <red>Katrine</red> and she allowed me " +
            "to join the <red>Black Arm Gang</red>."
    private const val STRAVEN_TASK =
        "I spoke to <red>Straven</red> in the <red>Phoenix Gang Headquarters</red> south east of " +
            "the <red>Blue Moon Inn</red>. He offered to let me join the <red>Gang</red> in " +
            "return for killing <red>Jonny the Beard</red> and recovering an " +
            "<red>Intelligence Report</red> from him. I can find <red>Jonny</red> in the " +
            "<red>Blue Moon Inn</red>."
    private const val STRAVEN_TASK_DONE =
        "I spoke to Straven in the Phoenix Gang Headquarters south east of the Blue Moon Inn. " +
            "He offered to let me join the Gang in return for killing Jonny the Beard and " +
            "recovering an Intelligence Report from him."
    private const val JONNY_KILLED =
        "I confronted and killed <red>Jonny the Beard</red> in the <red>Blue Moon Inn</red> and " +
            "took an <red>Intelligence Report</red> from him. I should take this " +
            "<red>Report</red> to <red>Straven</red>."
    private const val PHOENIX_JOINED =
        "I confronted and killed <red>Jonny the Beard</red> in the <red>Blue Moon Inn</red> and " +
            "took an <red>Intelligence Report</red> from him. I gave this <red>Report</red> to " +
            "<red>Straven</red> and he allowed me to join the <red>Phoenix Gang</red>."
    private const val SEARCH_HQ =
        "I can now search the <red>Gang Headquarters</red> for the <red>Shield of Arrav</red>."
    private const val ROALD_REWARDED =
        "I took my Certificate to King Roald in Varrock Palace and he rewarded me for helping " +
            "to return the Shield of Arrav."
}
