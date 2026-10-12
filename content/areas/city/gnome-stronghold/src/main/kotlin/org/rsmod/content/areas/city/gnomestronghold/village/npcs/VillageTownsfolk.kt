package org.rsmod.content.areas.city.gnomestronghold.village.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class VillageTownsfolk @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.king_bolren") { startDialogue(it.npc) { kingBolren() } }
        onOpNpc1("npc.elkoy") { startDialogue(it.npc) { elkoyMaze() } }
        onOpNpc1("npc.elkoy_village") { startDialogue(it.npc) { elkoyVillage() } }
        onOpNpc1("npc.remsai") { startDialogue(it.npc) { remsai() } }
        onOpNpc1("npc.lostgnome") { startDialogue(it.npc) { kalron() } }
        onOpNpc1("npc.chantergnome") { startDialogue(it.npc) { localGnome() } }
        onOpNpc1("npc.commander_montai") { startDialogue(it.npc) { commanderMontai() } }
        onOpNpc1("npc.tracker1") { startDialogue(it.npc) { trackerGnome1() } }
        onOpNpc1("npc.tracker2") { startDialogue(it.npc) { trackerGnome2() } }
        onOpNpc1("npc.tracker3") { startDialogue(it.npc) { trackerGnome3() } }
        onOpNpc1("npc.gnome_tortoise_commander") { startDialogue(it.npc) { schepbur() } }
    }

    // TODO(content): [system:treasure_trails] medium clue step
    // TODO(content): [system:gnome_restaurant] delivery hand-in dialogue
    private suspend fun Dialogue.kingBolren() {
        if (QuestRequirements.hasCompleted(player, TREE_GNOME_VILLAGE)) {
            kingBolrenAfterQuest()
            return
        }
        kingBolrenBeforeQuest()
    }

    // https://oldschool.runescape.wiki/w/Transcript:King_Bolren
    // TODO(content): [quest:the_path_of_glouphrie] quest start for a player with the
    //  requirements, and quest stages
    // TODO(content): [system:met_flags] the one-time "Any luck in the quest for Arposandra?" after
    //  The Path of Glouphrie;
    //  it needs a flag for having heard it
    private suspend fun Dialogue.kingBolrenAfterQuest() {
        if (!access.playerContainsObj(GNOME_AMULET)) {
            chatNpc(happy, "Here, I think this belongs to you.")
            player.invAddOrDrop(objRepo, GNOME_AMULET)
            objbox(GNOME_AMULET, "King Bolren gives you an amulet.")
        }
        if (QuestRequirements.hasCompleted(player, PATH_OF_GLOUPHRIE)) {
            chatPlayer(happy, "Hello again.")
            chatNpc(happy, "Hello there.")
            chatPlayer(quiz, "How are things here?")
            chatNpc(
                neutral,
                "I fear the war against the Khazard Army is set to outlive us all. We keep " +
                    "making progress, but for every foe we defeat, Khazard sends two more.",
            )
            chatNpc(happy, "Still, we thankfully haven't had any more Glouphrie problems at least.")
            chatPlayer(happy, "That's good to hear. All the best.")
            return
        }
        chatPlayer(happy, "Hello again, Bolren.")
        chatNpc(happy, "Hello there. It's good to see you again.")
        chatPlayer(quiz, "How are things here?")
        chatNpc(
            happy,
            "Those Khazard troops continue to be a menace, but things are much better now. All " +
                "thanks to you, of course.",
        )
        mesbox("You do not meet all of the requirements to start The Path of Glouphrie quest.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Tree_Gnome_Village (Tree Gnome King)
    // TODO(content): [quest:tree_gnome_village] quest start after "How can I help?", and quest
    //  stages
    private suspend fun Dialogue.kingBolrenBeforeQuest() {
        chatPlayer(happy, "Hello.")
        chatNpc(
            happy,
            "Well hello stranger. My name's Bolren, I'm the king of the tree gnomes.",
        )
        chatNpc(happy, "I'm surprised you made it in, maybe I made the maze too easy.")
        chatPlayer(neutral, "Maybe.")
        chatNpc(worried, "I'm afraid I have more serious concerns at the moment. Very serious.")
        if (!choice2("Can I help at all?", true, "I'll leave you to it then.", false)) {
            chatPlayer(neutral, "I'll leave you to it then.")
            chatNpc(neutral, "Ok, take care.")
            return
        }
        chatPlayer(quiz, "Can I help at all?")
        chatNpc(happy, "I'm glad you asked.")
        chatNpc(
            worried,
            "The truth is my people are in grave danger. We have always been protected by the " +
                "Spirit Tree. No creature of dark can harm us while its three orbs are in place.",
        )
        chatNpc(
            sad,
            "We are not a violent race, but we fight when we must. Many gnomes have fallen " +
                "battling the dark forces of Khazard to the North.",
        )
        chatNpc(
            sad,
            "We became desperate, so we took one orb of protection to the battlefield. It was a " +
                "foolish move.",
        )
        chatNpc(sad, "Khazard troops seized the orb. Now we are completely defenceless.")
        chatPlayer(quiz, "How can I help?")
        chatNpc(
            neutral,
            "You would be a huge benefit on the battlefield. If you would go there and try to " +
                "retrieve the orb, my people and I will be forever grateful.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Elkoy
    // TODO(content): [quest:tree_gnome_village] guiding the player through the maze once the
    //  quest has started
    // TODO(content): [system:elkoy_maze_guide] the trip through the maze on "Yes please." and
    //  the Follow op
    private suspend fun Dialogue.elkoyMaze() {
        if (QuestRequirements.hasCompleted(player, TREE_GNOME_VILLAGE)) {
            chatPlayer(happy, "Hello Elkoy.")
            chatNpc(
                happy,
                "Hi there, I hope life is treating you well. Would you like me to show you the " +
                    "way to the village?",
            )
            if (choice2("Yes please.", true, "No thanks Elkoy.", false)) {
                chatPlayer(happy, "Yes please.")
            } else {
                chatPlayer(neutral, "No thanks Elkoy.")
                chatNpc(happy, "Ok then, take care.")
            }
            return
        }
        chatPlayer(happy, "Hello there.")
        chatNpc(happy, "Hello, welcome to our maze. I'm Elkoy the tree gnome.")
        chatPlayer(quiz, "I haven't heard of your sort.")
        elkoyHistory()
    }

    // https://oldschool.runescape.wiki/w/Transcript:Elkoy
    // TODO(content): [system:elkoy_maze_guide] the trip out of the maze on "Yes please." and the
    //  Follow op
    private suspend fun Dialogue.elkoyVillage() {
        if (QuestRequirements.hasCompleted(player, TREE_GNOME_VILLAGE)) {
            chatPlayer(happy, "Hello Elkoy.")
            chatNpc(
                happy,
                "Hi there, I hope life is treating you well. Would you like me to show you the " +
                    "way out of the village?",
            )
            if (choice2("Yes please.", true, "Not now, thanks.", false)) {
                chatPlayer(happy, "Yes please.")
            } else {
                chatPlayer(neutral, "Not now, thanks.")
            }
            return
        }
        chatPlayer(happy, "Hello there.")
        chatNpc(happy, "Hello, welcome to our village. I'm Elkoy the tree gnome.")
        chatPlayer(quiz, "I haven't heard of your sort before.")
        elkoyHistory()
    }

    private suspend fun Dialogue.elkoyHistory() {
        chatNpc(
            sad,
            "There's not many of us left. Once you could find tree gnomes anywhere in the world, " +
                "now we hide in small groups to avoid capture.",
        )
        chatPlayer(quiz, "Capture by whom?")
        chatNpc(
            sad,
            "Tree gnomes have been hunted for so called 'fun' since as long as I can remember.",
        )
        chatNpc(
            neutral,
            "Our main threat nowadays are General Khazard's troops. They know no mercy, but are " +
                "also very dense. They'll never find their way through our maze.",
        )
        chatNpc(happy, "Have fun.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Remsai
    private suspend fun Dialogue.remsai() {
        chatPlayer(happy, "Hello.")
        if (QuestRequirements.hasCompleted(player, TREE_GNOME_VILLAGE)) {
            chatNpc(happy, "Hi there traveller.  You're a legend around these parts.")
            chatPlayer(happy, "Thanks Remsai.")
            return
        }
        chatNpc(happy, "Well done, well done. Not many find their way in here.")
        chatNpc(
            happy,
            "I'm Remsai, a tree gnome. We live in this maze for our protection. Have a look " +
                "around and enjoy.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Kalron
    private suspend fun Dialogue.kalron() {
        if (QuestRequirements.hasCompleted(player, TREE_GNOME_VILLAGE)) {
            chatPlayer(happy, "Hello there, you look lost.")
            chatNpc(angry, "Are you trying to be funny?")
            chatPlayer(neutral, "No.")
            chatNpc(confused, "Hmmm.")
            return
        }
        chatPlayer(happy, "Hello.")
        chatNpc(
            worried,
            "Gotta find a way out. We built this maze for protection but I can't get used to it. " +
                "I'm always getting lost.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Local_Gnome
    private suspend fun Dialogue.localGnome() {
        chatPlayer(happy, "Hello.")
        if (QuestRequirements.hasCompleted(player, TREE_GNOME_VILLAGE)) {
            chatNpc(happy, "You're the best!")
            chatPlayer(happy, "Thanks.")
            chatNpc(laugh, "Well I'm better. Hee hee.")
            return
        }
        chatNpc(happy, "Lardi dee, lardi da.")
        chatPlayer(quiz, "Are you alright?")
        chatNpc(happy, "Hee hee, lardi da, lardi dee. (The gnome appears to be singing.)")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Commander_Montai
    // TODO(content): [quest:tree_gnome_village] quest stages
    private suspend fun Dialogue.commanderMontai() {
        if (QuestRequirements.hasCompleted(player, TREE_GNOME_VILLAGE)) {
            chatPlayer(happy, "Hello Montai, how are you?")
            chatNpc(
                neutral,
                "I'm ok, this battle is going to take longer to win than I expected. The Khazard " +
                    "troops won't give up even without the orb.",
            )
            chatPlayer(neutral, "Hang in there.")
            return
        }
        chatPlayer(happy, "Hello.")
        chatNpc(
            angry,
            "I can't talk now. Can't you see we're trying to win a battle here? If we can't hold " +
                "back Khazard's men we're all doomed.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Tracker_gnome_1
    // TODO(content): [quest:tree_gnome_village] quest stages
    private suspend fun Dialogue.trackerGnome1() {
        chatPlayer(happy, "Hello.")
        if (QuestRequirements.hasCompleted(player, TREE_GNOME_VILLAGE)) {
            chatNpc(sad, "When will this battle end? I feel like I've been fighting forever.")
            return
        }
        chatNpc(angry, "I can't talk now. Can't you see we're trying to win a battle here?")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Tracker_gnome_2
    // TODO(content): [quest:tree_gnome_village] quest stages
    private suspend fun Dialogue.trackerGnome2() {
        chatPlayer(happy, "Hello.")
        if (QuestRequirements.hasCompleted(player, TREE_GNOME_VILLAGE)) {
            chatNpc(
                sad,
                "When will this battle end? I feel like I've been locked up my whole life.",
            )
            return
        }
        chatNpc(worried, "I can't talk now. If the guards catch me I'll be dead gnome meat.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Tracker_gnome_3
    // TODO(content): [quest:tree_gnome_village] quest stages
    private suspend fun Dialogue.trackerGnome3() {
        chatPlayer(happy, "Hello.")
        if (QuestRequirements.hasCompleted(player, TREE_GNOME_VILLAGE)) {
            chatNpc(confused, "I feel dizzy, where am I? Oh dear, oh dear I need some rest.")
            chatPlayer(neutral, "I think you do.")
            return
        }
        chatNpc(angry, "I can't talk now. Can't you see we're trying to win a battle here?")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lieutenant_Schepbur
    // TODO(content): [system:gnome_restaurant] delivery hand-in dialogue
    private suspend fun Dialogue.schepbur() {
        chatNpc(angry, "Move into position lads! eh? Who are you and what do you want?")
        chatPlayer(quiz, "Who are you then?")
        chatNpc(
            neutral,
            "Lieutenant Shepbur, commanding officer of the new Armoured Tortoise Regiment.",
        )
        chatPlayer(
            confused,
            "There's only two tortoises here, that's hardly a regiment.",
        )
        chatNpc(
            angry,
            "This is just the beginning! Gnome breeders and trainers are already working to " +
                "expand the number of units.  Soon we'll have hundreds of these beauties, nay " +
                "thousands! And they will not only carry mages and",
        )
        chatNpc(
            angry,
            "archers but other fiendish weapons of destruction of gnome devising. An army of " +
                "giant tortoises will march upon this battlefield and rain the fire of our wrath " +
                "upon all our enemies! Nothing will be able to stop us!",
        )
        chatPlayer(worried, "Oooookayy...... I'll leave you to it then....")
    }

    private companion object {
        const val TREE_GNOME_VILLAGE = "quest_treegnomevillage"
        const val PATH_OF_GLOUPHRIE = "quest_pathofglouphrie"
        const val GNOME_AMULET = "obj.gnome_amulet"
    }
}
