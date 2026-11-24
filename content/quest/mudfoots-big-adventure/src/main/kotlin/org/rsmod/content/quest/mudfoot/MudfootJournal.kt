package org.rsmod.content.quest.mudfoot

internal object MudfootJournal {
    val ENTRIES: List<String> =
        listOf(
            "I spoke with Captain Harlan north of Lumbridge Castle. He told me about a bounty for " +
                "some Thieves who robbed the General Store. According to him, some Goblins east " +
                "of the River Lum in Lumbridge were the prime suspects.",
            "I went to investigate the Goblins east of the River Lum in Lumbridge, and " +
                "encountered one named Mudfoot. He revealed that the Goblins weren't responsible, " +
                "and had instead been set up by some Humans. We decided to work together to bring " +
                "them to justice.",
            "Mudfoot and I found the first Thief by the General Store. Mudfoot tried to attack " +
                "him, but didn't have any luck. The Thief then left. Captain Harlan came to see " +
                "what was going on. Upon explaining the situation, he suggested that because of " +
                "the Ranger Armour worn by the Thief, Mudfoot would have better luck if he used a " +
                "Stabbing Weapon. Mudfoot left to get one, and told me to meet him near the " +
                "Windmill north of Lumbridge, where the Thief was seen going.",
            "Mudfoot and I found the first Thief again near the Windmill north of Lumbridge. This " +
                "time, we were able to defeat him. Mudfoot told me the next Thief was last seen " +
                "by a Ruined Building south of the Windmill, and that she was using Magic, meaning " +
                "Ranged would work best against her.",
            "Mudfoot and I found and defeated the second Thief by a Ruined Building south of the " +
                "Windmill. Mudfoot then told me the last Thief was seen in the Lumbridge " +
                "Graveyard, and that he was using Melee, meaning Magic would work best against " +
                "him. That said, Mudfoot also told me that the Thief was seen using Prayer to " +
                "block damage from certain attacks. To counter this, we agreed that Mudfoot would " +
                "continue to use Melee, while I would use Magic.",
            "Mudfoot and I found and defeated the last Thief in the Lumbridge Graveyard. With all " +
                "three dealt with, we can now return to Captain Harlan north of Lumbridge Castle.",
        )

    private val HIGHLIGHTS =
        listOf(
            "Lumbridge Graveyard",
            "Lumbridge Castle",
            "Captain Harlan",
            "General Store",
            "Ruined Building",
            "Ranger Armour",
            "Stabbing Weapon",
            "River Lum",
            "Lumbridge",
            "Mudfoot",
            "Windmill",
            "Thieves",
            "Thief",
            "Goblins",
            "Humans",
            "Ranged",
            "Magic",
            "Melee",
            "Prayer",
        )

    private val HIGHLIGHT_PATTERN = Regex(HIGHLIGHTS.joinToString("|") { Regex.escape(it) })

    /** Index of the entry describing the player's current step, or past the end when done. */
    fun currentEntry(stage: Int): Int =
        when {
            stage < MudfootStage.SIDEKICK -> 0
            stage < MudfootStage.MET_HARLAN -> 1
            stage < MudfootStage.RANGER_DEAD -> 2
            stage < MudfootStage.MAGE_DEAD -> 3
            stage < MudfootStage.WARRIOR_DEAD -> 4
            stage < MudfootStage.COMPLETE -> 5
            else -> ENTRIES.size
        }

    fun highlighted(index: Int): String =
        HIGHLIGHT_PATTERN.replace(ENTRIES[index]) { "<red>${it.value}</red>" }
}
