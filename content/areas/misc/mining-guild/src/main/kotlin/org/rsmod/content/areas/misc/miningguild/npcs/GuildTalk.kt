package org.rsmod.content.areas.misc.miningguild.npcs

import org.rsmod.api.player.dialogue.Dialogue

internal suspend fun Dialogue.describeGuild() {
    chatNpc(
        happy,
        "All sorts of things! There's plenty of coal rocks along with some iron, mithril and " +
            "adamantite as well.",
    )
    chatNpc(
        happy,
        "Deeper in the guild you'll find even more rocks including some runite! The best bit " +
            "though is our amethyst mine, the only one in the land!",
    )
    chatNpc(happy, "There's no better mining site anywhere!")
}

internal suspend fun Dialogue.describeOreUse() {
    chatNpc(
        neutral,
        "What do you think? We smelt it into bars, smith the metal to make armour and weapons, " +
            "then we exchange them for goods and services.",
    )
    chatPlayer(quiz, "I don't see many dwarves selling armour or weapons here.")
    chatNpc(
        neutral,
        "No, this is only a mining outpost. We dwarves don't much like to settle in human cities. " +
            "Most of the ore is carted off to Keldagrim, the great dwarven city. They've got a " +
            "special blast furnace up there - it makes",
    )
    chatNpc(
        neutral,
        "smelting the ore so much easier. There are plenty of dwarven traders working in " +
            "Keldagrim. Anyway, can I help you with anything else?",
    )
}
