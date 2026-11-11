package org.rsmod.content.quest.area.morytania.priestinperil

internal object PriestInPerilJournal {
    private val HISTORY = listOf(
        "<str>I spoke to King Roald in Varrock Palace and asked him if he",
        "<str>needed help with anything. He told me about Paterdomus, a",
        "<str>temple east of Varrock guarding the only passage into the",
        "<str>dark vampyre-infested land of Morytania. He asked me to",
        "<str>head to Paterdomus and check in with Drezel, the priest living",
        "<str>there.",
        "<str>I travelled to Paterdomus and spoke to someone claiming to",
        "<str>be Drezel. At his request, I headed into the Mausoleum below",
        "<str>the temple and killed a Dog-like Thing causing trouble down",
        "<str>there.",
        "<str>I returned to King Roald in Varrock Palace, but he was furious",
        "<str>when I told him what happened. He revealed that the Dog-like",
        "<str>Thing was guarding the passage into Morytania and that",
        "<str>someone must have pretended to be Drezel to get me to kill it.",
        "<str>I headed back to Paterdomus and found that the real Drezel",
        "<str>had been imprisoned there by some Monks of Zamorak. Drezel",
        "<str>explained to me that the vampyres of Morytania had been",
        "<str>unable to invade Misthalin because of blessings placed on",
        "<str>the River Salve, the river marking the border between the two",
        "<str>regions, but that the Monks of Zamorak were likely trying to",
        "<str>sabotage the blessings.",
        "<str>I found a Key down in the Mausoleum and used it to unlock the",
        "<str>Cell that Drezel was imprisoned in. However, he was still",
        "<str>unable to leave due to a Vampyre trapped in a Coffin that the",
        "<str>Monks of Zamorak had brought with them. I obtained a Bucket",
        "<str>of Water from the River Salve and Drezel blessed it for me. I",
        "<str>then used it to deal with the Vampyre.",
        "<str>With the Vampyre dealt with, Drezel and I headed down into",
        "<str>the Mausoleum. Drezel found that the Monks of Zamorak had",
        "<str>polluted the river, putting the blessings on it at risk. However,",
        "<str>we were able to undo the damage. With Misthalin safe once",
        "<str>more, Drezel thanked me and gave me permission to travel",
        "<str>into Morytania.",
    )

    fun progress(stage: Int, remaining: Int): List<String> {
        val (done, current) = when (stage) {
            1 -> 0 to listOf(
                "<col=000080>I spoke to <col=800000>King Roald<col=000080> in <col=800000>Varrock Palace<col=000080> and asked him if he",
                "<col=000080>needed help with anything. He told me about <col=800000>Paterdomus<col=000080>, a",
                "<col=000080>temple east of <col=800000>Varrock<col=000080> guarding the only passage into the",
                "<col=000080>dark vampyre-infested land of <col=800000>Morytania<col=000080>. He asked me to",
                "<col=000080>head to <col=800000>Paterdomus<col=000080> and check in with <col=800000>Drezel<col=000080>, the priest living",
                "<col=000080>there.",
            )
            2 -> 6 to listOf(
                "<col=000080>I travelled to <col=800000>Paterdomus<col=000080> and spoke to someone claiming to",
                "<col=000080>be <col=800000>Drezel<col=000080>. He asked me to head into the <col=800000>Mausoleum<col=000080> below the",
                "<col=000080>temple to kill a <col=800000>Dog-like Thing<col=000080> causing trouble down there.",
                "<col=000080>However, something seemed a bit off about the whole thing. I",
                "<col=000080>could go down into the <col=800000>Mausoleum<col=000080> and do as requested,",
                "<col=000080>though it might be wiser for me to head back to <col=800000>Varrock<col=000080> and",
                "<col=000080>check in with <col=800000>King Roald<col=000080> first.",
            )
            3 -> 6 to listOf(
                "<col=000080>I travelled to <col=800000>Paterdomus<col=000080> and spoke to someone claiming to",
                "<col=000080>be <col=800000>Drezel<col=000080>. At his request, I headed into the <col=800000>Mausoleum<col=000080> below",
                "<col=000080>the temple and killed a <col=800000>Dog-like Thing<col=000080> causing trouble down",
                "<col=000080>there. I should now report back to <col=800000>King Roald<col=000080> in <col=800000>Varrock",
                "<col=800000>Palace<col=000080>.",
            )
            4 -> 10 to listOf(
                "<col=000080>I returned to <col=800000>King Roald<col=000080> in <col=800000>Varrock Palace<col=000080>, but he was furious",
                "<col=000080>when I told him what happened. He revealed that the <col=800000>Dog-like",
                "<col=800000>Thing<col=000080> was guarding the passage into <col=800000>Morytania<col=000080> and that",
                "<col=000080>someone must have pretended to be <col=800000>Drezel<col=000080> to get me to kill it.",
                "<col=000080>I need to head back to <col=800000>Paterdomus<col=000080> and find out what's",
                "<col=000080>happened to the real <col=800000>Drezel<col=000080>.",
            )
            5 -> 14 to listOf(
                "<col=000080>I headed back to <col=800000>Paterdomus<col=000080> and found that the real <col=800000>Drezel<col=000080>",
                "<col=000080>had been imprisoned there by some <col=800000>Monks of Zamorak<col=000080>. <col=800000>Drezel<col=000080>",
                "<col=000080>explained to me that the vampyres of <col=800000>Morytania<col=000080> have been",
                "<col=000080>unable to invade <col=800000>Misthalin<col=000080> because of blessings placed on",
                "<col=000080>the <col=800000>River Salve<col=000080>, the river that marks the border between the",
                "<col=000080>two regions, but that the <col=800000>Monks of Zamorak<col=000080> are likely trying",
                "<col=000080>to sabotage the blessings. To stop them, I'll first need to free",
                "<col=000080><col=800000>Drezel<col=000080>. I killed the leader of the <col=800000>Monks of Zamorak<col=000080> and took a",
                "<col=000080><col=800000>Key<col=000080> from him. I should see if this <col=800000>Key<col=000080> unlocks the <col=800000>Cell<col=000080> that",
                "<col=000080><col=800000>Drezel<col=000080> has been imprisoned in.",
            )
            6 -> 21 to listOf(
                "<col=000080>I found a <col=800000>Key<col=000080> down in the <col=800000>Mausoleum<col=000080> and used it to unlock the",
                "<col=000080><col=800000>Cell<col=000080> that <col=800000>Drezel<col=000080> was imprisoned in. However, he is still unable",
                "<col=000080>to leave due to a <col=800000>Vampyre<col=000080> trapped in a <col=800000>Coffin<col=000080> that the <col=800000>Monks",
                "<col=800000>of Zamorak<col=000080> had brought with them. I obtained a <col=800000>Bucket of",
                "<col=800000>Water<col=000080> from the <col=800000>River Salve<col=000080>. I should check with <col=800000>Drezel<col=000080> and see",
                "<col=000080>if it can be used against the <col=800000>Vampyre<col=000080>.",
            )
            8 -> 27 to listOf(
                "<col=000080>With the <col=800000>Vampyre<col=000080> dealt with, <col=800000>Drezel<col=000080> and I can now head down",
                "<col=000080>into the <col=800000>Mausoleum<col=000080> to see what damage the <col=800000>Monks of",
                "<col=800000>Zamorak<col=000080> have done. I should meet him down there when ready.",
            )
            7 -> 27 to listOf("<col=000080>I should tell Drezel that the Vampyre has been dealt with.")
            else -> {
                val objective = if (remaining == 50) listOf(
                    "<col=000080>to bring <col=800000>Drezel<col=000080> some <col=800000>Rune Essence<col=000080>. He needs <col=800000>50 Essence<col=000080> in",
                    "<col=000080>total.",
                ) else listOf(
                    "<col=000080>to bring <col=800000>Drezel<col=000080> some <col=800000>Rune Essence<col=000080>. He still needs <col=800000>$remaining Essence<col=000080>.",
                )
                27 to listOf(
                    "<col=000080>With the <col=800000>Vampyre<col=000080> dealt with, <col=800000>Drezel<col=000080> and I headed down into",
                    "<col=000080>the <col=800000>Mausoleum<col=000080>. <col=800000>Drezel<col=000080> found that the <col=800000>Monks of Zamorak<col=000080> had",
                    "<col=000080>polluted the <col=800000>River Salve<col=000080> using the <col=800000>Well<col=000080> down there, meaning",
                    "<col=000080>the blessings upon it are now at risk. To address this, I'll need",
                ) + objective
            }
        }
        return HISTORY.take(done) + current
    }

    fun completed(blessed: Boolean): List<String> = if (blessed) {
        HISTORY + listOf("", "<col=ff0000>QUEST COMPLETE!")
    } else {
        HISTORY.take(30) + listOf(
            "<str>we were able to undo the damage, ensuring Misthalin would be",
            "<str>safe once more.",
            "",
            "<col=ff0000>QUEST COMPLETE!",
            "",
            "<col=000080>If I wish to travel into <col=800000>Morytania<col=000080>, I should speak to <col=800000>Drezel<col=000080> in",
            "<col=000080>the <col=800000>Paterdomus Mausoleum<col=000080>.",
        )
    }
}
