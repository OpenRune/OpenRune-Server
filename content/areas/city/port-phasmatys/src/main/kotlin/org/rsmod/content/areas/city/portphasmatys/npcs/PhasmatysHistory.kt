package org.rsmod.content.areas.city.portphasmatys.npcs

import org.rsmod.api.player.dialogue.Dialogue

// https://oldschool.runescape.wiki/w/Transcript:Ghosts_Ahoy (Velorina)
// https://oldschool.runescape.wiki/w/Transcript:Ghost_innkeeper
internal suspend fun Dialogue.phasmatysHistory() {
    chatNpc(quiz, "Do you know why ghosts exist?")
    chatPlayer(
        neutral,
        "A ghost is a soul left in limbo, unable to pass over to the next world; they might " +
            "have something left to do in this world that torments them, or they might just " +
            "have died in a state of confusion.",
    )
    chatNpc(
        neutral,
        "Yes, that is normally the case. But here in Port Phasmatys, we of this town once " +
            "chose freely to become ghosts!",
    )
    chatPlayer(shocked, "Why on earth would you do such a thing?")
    for (line in HISTORY) {
        chatNpc(sad, line)
    }
}

private val HISTORY =
    listOf(
        "It is a long story. Many years ago, this was a thriving port, a trading centre to the " +
            "eastern lands of Gielinor. We became rich on the profits made from the traders " +
            "that came across the eastern seas.",
        "We were very happy... until Lord Drakan noticed us.",
        "He sent unholy creatures to demand that a blood-tithe be paid to the Lord Vampyre, as " +
            "is required from all in the domain of Morytania. We had no choice but to agree to " +
            "his demands.",
        "As the years went by, our numbers dwindled and many spoke of abandoning the town for " +
            "safer lands. Then, Necrovarus came to us.",
        "He came from the eastern lands, but of more than that, little is known. Some say he " +
            "was once a mage, some say a priest. Either way, he was in possession of knowledge " +
            "totally unknown to even the most learned among us.",
        "Necrovarus told us that he had been brought by a vision he'd had of an underground " +
            "source of power. He inspired us to delve beneath the town, promising us the power " +
            "to repel the vampyres.",
        "Deep underneath Phasmatys, we found a pool of green slime that Necrovarus called " +
            "ectoplasm. He showed us how to build the Ectofuntus, which would turn the " +
            "ectoplasm into the power he had promised us.",
        "Indeed, this Ectopower did repel the vampyres; they would not enter Phasmatys once the " +
            "Ectofuntus began working. But little did we know that we had exchanged one evil " +
            "for yet another - Ectopower.",
        "Little by little, we began to lose any desire for food or water, and our desire for " +
            "the Ectopower grew until it dominated our thoughts entirely. Our bodies shrivelled " +
            "and, one by one, we died.",
        "The Ectofuntus and the power it emanates keeps us here as ghosts - some, like myself, " +
            "content to remain in this world; some becoming tortured souls who we do not allow " +
            "to pass our gates.",
        "We would be able to pass over into the next world but Necrovarus has used his power " +
            "to create a psychic barrier, preventing us.",
        "We must remain here for all eternity, even unto the very end of the world.",
    )
