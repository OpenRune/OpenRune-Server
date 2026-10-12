package org.rsmod.content.areas.zeah.hosidius.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:fairytale_i_growing_pains] "Are you a member of the Group of Advanced
//  Gardeners?" while Fairytale I is in progress
// TODO(content): [skill:farming] patch protection payments, Pay ops and tool sales
// TODO(content): [system:treasure_trails] Marisi's medium challenge scroll and Lammy Langle's
//  hard puzzle box
class Gardeners : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.hosidius_allotment_gardener") { startDialogue(it.npc) { marisi() } }
        onOpNpc1("npc.farming_gardener_spirit_tree_4") { startDialogue(it.npc) { lammyLangle() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Marisi
    private suspend fun Dialogue.marisi() {
        while (true) {
            when (menu()) {
                1 -> {
                    chatPlayer(quiz, "Would you look after my crops for me?")
                    chatNpc(neutral, "I might - which patch were you thinking of?")
                    choice2(
                        "The northeastern allotment",
                        1,
                        "The southwestern allotment",
                        2,
                    )
                    chatNpc(
                        neutral,
                        "You don't have any seeds planted in that patch. Plant some and I might " +
                            "agree to look after it for you.",
                    )
                    return
                }
                2 -> {
                    chatPlayer(quiz, "Can you give me any farming advice?")
                    advice()
                }
                3 -> {
                    sellMeSomething()
                    return
                }
                else -> {
                    chatPlayer(neutral, "I'll come back another time.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lammy_Langle
    // TODO(content): [verify:in_game] Lammy Langle's farming advice is missing from the wiki; the
    //  standard gardener advice is assumed
    private suspend fun Dialogue.lammyLangle() {
        while (true) {
            when (menu()) {
                1 -> {
                    chatPlayer(quiz, "Would you look after my crops for me?")
                    chatNpc(
                        neutral,
                        "You don't have a sapling planted in that patch. Plant one and I might " +
                            "agree to look after it for you.",
                    )
                    return
                }
                2 -> {
                    chatPlayer(quiz, "Can you give me any farming advice?")
                    advice()
                }
                3 -> {
                    sellMeSomething()
                    return
                }
                else -> {
                    chatPlayer(neutral, "I'll come back another time.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.menu(): Int =
        choice4(
            "Would you look after my crops for me?",
            1,
            "Can you give me any farming advice?",
            2,
            "Can you sell me something?",
            3,
            "I'll come back another time.",
            4,
        )

    private suspend fun Dialogue.sellMeSomething() {
        chatPlayer(quiz, "Can you sell me something?")
        chatNpc(
            neutral,
            "That depends on whether I have it to sell. What is it that you're looking for?",
        )
    }

    private suspend fun Dialogue.advice() {
        val crafter = if (access.isBodyTypeA()) "craftsman" else "craftswoman"
        when (access.random.of(17)) {
            0 ->
                chatNpc(
                    neutral,
                    "The only way to cure a bush or tree of disease is to prune away the " +
                        "diseased leaves with a pair of secateurs. For all other crops I would " +
                        "just apply some plant-cure.",
                )
            1 ->
                chatNpc(
                    neutral,
                    "Tree seeds must be grown in a plantpot of soil into a sapling, and then " +
                        "transferred to a tree patch to continue growing to adulthood.",
                )
            2 ->
                chatNpc(
                    neutral,
                    "You can put up to ten potatoes, cabbages or onions in vegetable sacks, " +
                        "although you can't have a mix in the same sack.",
                )
            3 ->
                chatNpc(
                    neutral,
                    "You can buy all the farming tools from farming shops, which can be found " +
                        "close to the allotments.",
                )
            4 ->
                chatNpc(
                    neutral,
                    "Bittercap mushrooms can only be grown in a special patch in Morytania, near " +
                        "the Mort Myre swamp. There the ground is especially dank and suited to " +
                        "growing poisonous fungii.",
                )
            5 ->
                chatNpc(
                    neutral,
                    "Supercompost is far better than normal compost, but more expensive to make. " +
                        "You need to rot the right type of item; show me an item, and I'll tell " +
                        "you if it's super- compostable or not.",
                )
            6 ->
                chatNpc(
                    neutral,
                    "There is a special patch for growing Belladonna - I believe that it is " +
                        "somewhere near Draynor Manor, where the ground is a tad 'unblessed'.",
                )
            7 ->
                chatNpc(
                    neutral,
                    "Applying compost to a patch will not only reduce the chance that your crops " +
                        "will get diseased, but you will also grow more crops to harvest.",
                )
            8 ->
                chatNpc(
                    neutral,
                    "If you want to grow fruit trees you could try a few places: Catherby, " +
                        "Brimhaven, Gnome Stronghold and the Farming Guild all have fruit tree " +
                        "patches.",
                )
            9 ->
                chatNpc(
                    neutral,
                    "You can fill plantpots with soil from any empty patch, if you have a " +
                        "gardening trowel.",
                )
            10 ->
                chatNpc(
                    neutral,
                    "Hops are good for brewing ales. I believe there's a brewery up in Keldagrim " +
                        "somewhere, and I've heard rumours that a place called Phasmatys used to " +
                        "be good for that type of thing. 'Fore they all died, of course.",
                )
            11 ->
                chatNpc(
                    neutral,
                    "Vegetables, hops and flowers need constant watering - if you ignore my " +
                        "advice, you will sooner or later find yourself in possession of a dead " +
                        "farming patch.",
                )
            12 ->
                chatNpc(
                    neutral,
                    "You can put up to five tomatoes, strawberries, apples, bananas or oranges " +
                        "into a fruit basket, although you can't have a mix in the same basket.",
                )
            13 ->
                chatNpc(
                    neutral,
                    "Don't just throw away your weeds after you've raked a patch - put them in a " +
                        "compost bin and make some compost.",
                )
            14 -> farmingAreas()
            15 ->
                chatNpc(
                    neutral,
                    "If you want to make your own sacks and baskets you'll need to use the loom " +
                        "that's near the Farming shop in Falador. If you're a good enough " +
                        "$crafter, that is.",
                )
            else ->
                chatNpc(
                    neutral,
                    "You don't have to buy all your plantpots you know, you can make them " +
                        "yourself on a pottery wheel. If you're a good enough $crafter, that is.",
                )
        }
    }

    private suspend fun Dialogue.farmingAreas() {
        if (!QuestRequirements.hasCompleted(player, SONG_OF_THE_ELVES)) {
            chatNpc(
                neutral,
                "There are five other Farming areas like this one - Elstan's area near Falador, " +
                    "Dantaera's area near Catherby, Lyra's area in Morytania, Marisi's area in " +
                    "Hosidius and Alan's area in the Farming Guild.",
            )
            return
        }
        chatNpc(
            neutral,
            "There are five other Farming areas like this one - Elstan's area near Falador, " +
                "Dantaera's area near Catherby, Lyra's area in Morytania, Marisi's area in " +
                "Hosidius, Alan's area in the Farming Guild and Oswallt's",
        )
        chatNpc(neutral, "area in Prifddinas.")
    }

    private companion object {
        const val SONG_OF_THE_ELVES = "quest_songoftheelves"
    }
}
