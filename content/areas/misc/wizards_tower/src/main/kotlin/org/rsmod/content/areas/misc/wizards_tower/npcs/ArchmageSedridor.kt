package org.rsmod.content.areas.misc.wizards_tower.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.quest.area.lumbridge.RuneMysteriesQuest
import org.rsmod.content.quest.area.lumbridge.rmBackstory
import org.rsmod.content.quest.area.lumbridge.rmKnowName
import org.rsmod.content.quest.area.lumbridge.rmKnowOthers
import org.rsmod.content.quest.area.lumbridge.rmNotesGiven
import org.rsmod.content.quest.area.lumbridge.rmOwedTalisman
import org.rsmod.content.quest.area.lumbridge.rmPackage
import org.rsmod.content.quest.area.lumbridge.rmTalismanGiven
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.quest.manager.menu
import org.rsmod.content.skills.runecrafting.essence.teleportToRuneEssenceMine
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Archmage_Sedridor
// https://oldschool.runescape.wiki/w/Transcript:Rune_Mysteries
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:treasure_trails] beginner clue "Well done." dialogue
class ArchmageSedridor @Inject constructor(private val runeMysteries: RuneMysteriesQuest) : PluginScript() {

    private var Player.toldOfPersten by boolVarBit("varbit.dt2_sedridor_persten_dialogue")
    private var Player.toldOfStoneOfJas by boolVarBit("varbit.dt2_sedridor_stone_dialogue")

    private val quest
        get() = runeMysteries.quest

    private val doubtedIdentity
        get() = runeMysteries.doubtedIdentity

    override fun ScriptContext.startup() {
        onOpNpc1("npc.head_wizard") { startSedridorDialogue(it.npc) }
        onOpNpc3("npc.head_wizard") { teleportToRuneEssenceMine(it.npc) }
    }

    private suspend fun ProtectedAccess.startSedridorDialogue(npc: Npc) {
        startDialogue(npc) {
            sedridorDialogue(npc)
        }
    }

    private suspend fun Dialogue.sedridorDialogue(npc: Npc) {
        when {
            quest.isQuestCompleted(player) -> sedridorAfterQuest(npc)
            quest.getQuestStage(player) == 0 -> sedridorBeforeQuest(npc)
            else -> sedridorDuringQuest(npc)
        }
    }

    private suspend fun Dialogue.sedridorBeforeQuest(npc: Npc) {
        chatNpc(
            happy,
            "Welcome adventurer, to the world renowned Wizards' Tower, home to the Order of " +
                "Wizards. How may I help you?",
        )
        chatPlayer(quiz, "I'm just looking around.")
        chatNpc(
            happy,
            "Well, take care adventurer. You stand on the ruins of the old destroyed Wizards' " +
                "Tower. Strange and powerful magicks lurk here.",
        )
    }

    private suspend fun Dialogue.sedridorDuringQuest(npc: Npc) {
        val stage = quest.getQuestStage(player)
        when {
            stage == RuneMysteriesQuest.STAGE_TALISMAN -> sedridorReceiveTalisman(npc)
            stage == RuneMysteriesQuest.STAGE_TALISMAN_GIVEN && player.rmPackage ->
                sedridorAgreedWithoutPackage()
            stage == RuneMysteriesQuest.STAGE_TALISMAN_GIVEN -> sedridorOfferPackage(npc)
            stage == RuneMysteriesQuest.STAGE_PACKAGE -> sedridorPackageFollowUp(npc)
            // TODO(content): [verify:in_game] Sedridor's dialogue before Aubury hands over his
            // notes; the transcript only covers the notes stage
            stage >= RuneMysteriesQuest.STAGE_AWAITING_NOTES -> sedridorReceiveNotes(npc)
            else -> sedridorReceiveTalisman(npc)
        }
    }

    // TODO(content): [system:met_flags] "Welcome back" only after a first visit without the
    // talisman; a first visit without it should play the full greeting, and a return visit with
    // it should go "Here you go." straight to the hand-over
    private suspend fun Dialogue.sedridorReceiveTalisman(npc: Npc) {
        val returningWithoutTalisman =
            quest.getQuestStage(player) == RuneMysteriesQuest.STAGE_TALISMAN && !hasAirTalisman(player)

        if (returningWithoutTalisman) {
            chatNpc(quiz, "Welcome back, adventurer. Do you have that talisman now?")
            chatPlayer(sad, "Not yet.")
            chatNpc(happy, "Well come back when you have it.")
            return
        }

        chatNpc(
            happy,
            "Welcome adventurer, to the world renowned Wizards' Tower, home to the Order of " +
                "Wizards. We are the oldest and most prestigious group of wizards around. Now, " +
                "how may I help you?",
        )

        chatPlayer(quiz, "Are you Sedridor?")
        chatNpc(quiz, "Sedridor? What is it you want with him?")
        chatPlayer(
            happy,
            "The Duke of Lumbridge sent me to find him. I have this talisman he found. He said " +
                "Sedridor would be interested in it.",
        )
        chatNpc(quiz, "Did he now? Well hand it over then, and we'll see what all the hubbub is about.")

        when (
            choice3(
                "Okay, here you are.",
                1,
                "No, I'll only give it to Sedridor.",
                2,
                "No, I don't think you are Sedridor.",
                3,
            )
        ) {
            1 -> handTalismanToSedridor(doubted = false)
            2 -> {
                chatPlayer(angry, "No, I'll only give it to Sedridor.")
                chatNpc(
                    happy,
                    "Well good news, for I am Sedridor! Now, hand it over and let me have a proper " +
                        "look at it, hmm?",
                )
                handTalismanToSedridor(doubted = false)
            }
            3 -> {
                chatPlayer(angry, "No, I don't think you are Sedridor.")
                chatNpc(
                    quiz,
                    "Hmm... Well, I admire your caution adventurer. Perhaps I can prove myself? I " +
                        "will use my mental powers to discover...",
                )
                chatNpc(happy, "Your name is... ${player.displayName}!")
                chatPlayer(shocked, "You're right! How did you know that?")
                chatNpc(
                    happy,
                    "Well I am the Archmage you know! You don't get to my position without learning " +
                        "a few tricks along the way!",
                )
                chatNpc(quiz, "So now that I have proved myself to you, why don't you hand over that talisman, hmm?")
                doubtedIdentity.set(player, true)
                handTalismanToSedridor(doubted = true)
            }
        }
    }

    private suspend fun Dialogue.handTalismanToSedridor(doubted: Boolean) {
        chatPlayer(happy, "Okay, here you are.")
        if (access.invDel(access.inv, RuneMysteriesQuest.AIR_TALISMAN).failure) {
            chatNpc(confused, "...")
            chatPlayer(confused, "...")
            chatNpc(angry, "Well?")
            chatPlayer(sad, "I don't seem to have it with me.")
            chatNpc(confused, "Hmm? You are a very odd person. Come back again when you have it.")
            return
        }
        quest.advanceQuestStage(access)
        player.rmTalismanGiven = true
        sedridorExamineTalisman(doubted)
    }

    private suspend fun Dialogue.sedridorExamineTalisman(doubted: Boolean) {
        mesbox("You hand the talisman to Sedridor.")
        chatNpc(
            quiz,
            "Hmm... Doesn't seem to be anything too special. Just a normal air talisman by the " +
                "looks of things. Still, looks can be deceiving. Let me take a closer look...",
        )
        mesbox("Sedridor murmurs some sort of incantation and the talisman glows slightly.")
        chatNpc(
            happy,
            "How interesting... It would appear I spoke too soon. There's more to this talisman " +
                "than meets the eye. In fact, it may well be the last piece of the puzzle.",
        )
        chatPlayer(quiz, "Puzzle?")
        chatNpc(
            happy,
            "Indeed! The lost legacy of the first tower. This talisman may in fact be key to " +
                "finding the forgotten essence mine!",
        )
        chatPlayer(confused, "First tower? Forgotten essence mine? What are you on about?")
        chatNpc(happy, "Ah, my apologies, adventurer. Allow me to fill you in.")

        when (
            choice2(
                "Go ahead.",
                1,
                "Actually, I'm not interested.",
                2,
            )
        ) {
            1 -> sedridorLoreThenPackage(doubted)
            2 -> {
                chatPlayer(bored, "Actually, I'm not interested.")
                chatNpc(
                    sad,
                    "Oh... Well I guess the short of it is that this talisman could be key to " +
                        "helping us rediscover an important teleportation incantation.",
                )
                chatNpc(
                    happy,
                    "With it, we'll be able to access a hidden essence mine, our lost source of " +
                        "rune essence.",
                )
                player.rmBackstory = true
                sedridorAskDeliverPackage(doubted)
            }
        }
    }

    private suspend fun Dialogue.sedridorLoreThenPackage(doubted: Boolean) {
        player.rmBackstory = true
        chatPlayer(happy, "Go ahead.")
        chatNpc(happy, "As you are likely aware, when we cast spells, we do so using the power of runes.")
        chatNpc(
            happy,
            "These runes are crafted from a highly unique material, and then imbued with magical " +
                "power from various runic altars. Different altars create different runes with " +
                "different magical effects.",
        )
        chatNpc(
            happy,
            "The process of imbuing runes is called runecrafting. Legend has it that this was once " +
                "a common art, but the secrets of how to do it were lost until just under two " +
                "hundred years ago.",
        )
        chatNpc(
            happy,
            "The rediscovery of runecrafting had such a large impact on the world, that it marked " +
                "the dawn of the Fifth Age. It also resulted in the birth of our order, and the " +
                "construction of the first Wizards' Tower.",
        )
        chatPlayer(quiz, "If it was the first tower, I'm guessing it doesn't exist anymore? What happened?")
        chatNpc(
            sad,
            "It was burnt down by traitorous members of our own order. They followed the evil god " +
                "of chaos, Zamorak, and they wished to claim our magical discoveries in his name.",
        )
        chatNpc(
            sad,
            "When the tower burnt down, much was lost, including an important incantation. A spell " +
                "that could be used to teleport to a hidden essence mine.",
        )
        chatPlayer(quiz, "The essence mine you mentioned earlier, I assume?")
        chatNpc(
            happy,
            "Precisely. Rune essence is the material used to make runes, but it is incredibly rare. " +
                "That essence mine was the only place it could be found that our order knew of.",
        )
        chatNpc(sad, "Since the incantation was lost, we have struggled to maintain our stocks of rune essence.")
        chatNpc(
            sad,
            "There are seemingly those out there that still know where to find some, but while they " +
                "have been willing to sell essence to us, they have refused to share knowledge on " +
                "how to find it ourselves.",
        )
        chatPlayer(
            quiz,
            "I'm starting to see why this is so important. So you think this talisman can help you " +
                "rediscover that incantation?",
        )
        chatNpc(
            happy,
            "I do! All magic leaves traces, and from what I can tell, this talisman was used heavily " +
                "during the time of the first tower.",
        )
        chatNpc(
            happy,
            "It would have been taken to the essence mine many times, and the magical energies there " +
                "will have left an imprint on it. To think that it was hidden in Lumbridge all this time!",
        )
        chatPlayer(quiz, "So what happens now?")
        sedridorAskDeliverPackage(doubted)
    }

    private suspend fun Dialogue.sedridorAskDeliverPackage(doubted: Boolean) {
        chatNpc(
            happy,
            "It is critical I share this discovery with my associate, Aubury, as soon as possible. " +
                "He's not much of a wizard, but he's an expert on runecrafting, and his insight will " +
                "be essential.",
        )
        chatNpc(
            quiz,
            "Would you be willing to visit him for me? I would go myself, but I wish to study this " +
                "talisman some more.",
        )
        when (choice2("Yes, certainly.", 1, "No, I'm busy.", 2)) {
            1 -> giveResearchPackage(doubted)
            2 -> {
                chatPlayer(sad, "No, I'm busy.")
                chatNpc(
                    sad,
                    "As you wish adventurer. I will continue to study this talisman you have brought " +
                        "me. Return here if you find yourself with some spare time to help me.",
                )
            }
        }
    }

    private suspend fun Dialogue.sedridorOfferPackage(npc: Npc) {
        chatPlayer(quiz, "So is that talisman of any use to you?")
        sedridorExamineTalisman(doubtedIdentity.get(player))
    }

    private suspend fun Dialogue.sedridorAgreedWithoutPackage() {
        chatNpc(
            happy,
            "Hello again, adventurer. Please, take this package of research notes to Aubury in " +
                "Varrock. He runs a rune shop in the south east of the city.",
        )
        handResearchPackage(doubtedIdentity.get(player))
    }

    private suspend fun Dialogue.giveResearchPackage(doubted: Boolean) {
        chatPlayer(happy, "Yes, certainly.")
        player.rmPackage = true
        chatNpc(
            happy,
            "He runs a rune shop in the south east of Varrock. Please, take this package of research " +
                "notes to him. If all goes well, the secrets of the essence mine may soon be ours " +
                "once more!",
        )
        handResearchPackage(doubted)
    }

    private suspend fun Dialogue.handResearchPackage(doubted: Boolean) {
        if (access.invAdd(access.inv, RuneMysteriesQuest.RESEARCH_PACKAGE).failure) {
            // TODO(content): [verify:in_game] Sedridor's line when the inventory is full
            chatNpc(
                sad,
                "You don't have enough inventory space for the package. Come back when you do.",
            )
            return
        }
        if (quest.getQuestStage(player) == RuneMysteriesQuest.STAGE_TALISMAN_GIVEN) {
            quest.advanceQuestStage(access)
        }
        mesbox("Sedridor hands you a package.")
        chatNpc(happy, "Best of luck, ${player.displayName}.")
        if (!doubted) {
            player.rmKnowName = true
            chatPlayer(confused, "I don't remember telling you my name... How do you know it?")
            chatNpc(happy, "Really now? I am the Archmage you know.")
        }
    }

    private suspend fun Dialogue.sedridorPackageFollowUp(npc: Npc) {
        chatNpc(quiz, "Hello again, adventurer. Did you take that package to Aubury?")
        if (!player.inv.contains(RuneMysteriesQuest.RESEARCH_PACKAGE)) {
            chatPlayer(sad, "I lost it. Could I have another?")
            chatNpc(happy, "Well it's a good job I have copies of everything.")
            handResearchPackage(doubtedIdentity.get(player))
        } else {
            chatPlayer(sad, "Not yet.")
            chatNpc(
                happy,
                "He runs a rune shop in the south east of Varrock. Please deliver it to him soon.",
            )
        }
    }

    private suspend fun Dialogue.sedridorReceiveNotes(npc: Npc) {
        chatNpc(
            happy,
            "Ah, ${player.displayName}. How goes your quest? Have you delivered my research to Aubury yet?",
        )
        chatPlayer(happy, "Yes, I have. He gave me some notes to give to you.")
        chatNpc(happy, "Wonderful! Let's have a look then.")

        if (access.invDel(access.inv, RuneMysteriesQuest.RESEARCH_NOTES).failure) {
            chatPlayer(sad, "Err, you're not going to believe this...")
            chatNpc(angry, "What?")
            chatPlayer(sad, "I don't have them.")
            chatNpc(
                angry,
                "Right... You're rather careless aren't you. I suggest you go and speak to Aubury " +
                    "once more. With luck he will have made copies.",
            )
            return
        }

        player.rmNotesGiven = true
        mesbox("You hand the notes to Sedridor.")
        chatNpc(happy, "Alright, let's see what Aubury has for us...")
        chatNpc(happy, "Yes, this is it! The lost incantation!")
        chatPlayer(quiz, "So you'll be able to access that essence mine now?")
        chatNpc(
            happy,
            "That's right! Because of you, our order finally has a proper source of rune essence " +
                "again! Thank you, friend.",
        )
        chatNpc(
            happy,
            "If you ever want to access the essence mine yourself, just let me know. It's the least " +
                "I can do.",
        )
        chatNpc(
            happy,
            "I will also share the incantation with others, including Aubury. When I do, I'll let " +
                "them know that you are to be given unlimited access to the mine.",
        )
        chatNpc(
            happy,
            "Oh, and you can have this air talisman back as well. I have no further need of it, and " +
                "I'm sure you will find it useful.",
        )
        if (access.invAdd(access.inv, RuneMysteriesQuest.AIR_TALISMAN).failure) {
            player.rmOwedTalisman = true
            // TODO(content): [verify:in_game] Sedridor's line when the inventory is full
            chatNpc(
                sad,
                "Ah - your inventory is full. Take care of that and come speak to me again for the talisman.",
            )
        } else {
            player.rmOwedTalisman = false
        }
        chatNpc(
            happy,
            "In case you didn't know, the talisman can be used to craft air runes. Just take it to " +
                "the Air Altar south of Falador along with some rune essence.",
        )
        chatNpc(
            happy,
            "Don't worry if you can't find the altar. The talisman can guide you there. You may " +
                "find talismans for other altars as well while adventuring. They'll let you craft " +
                "other types of rune.",
        )
        chatPlayer(happy, "Great! Thanks!")
        chatNpc(happy, "My pleasure!")

        val remaining = quest.maxSteps - quest.getQuestStage(player)
        if (remaining > 0) {
            quest.advanceQuestStage(access, remaining)
        }

        sedridorTopics(GET_GOING)
    }

    // TODO(content): [miniquest:enter_the_abyss] "I've heard a different take on how the tower
    // burnt down." after the old tower story
    private suspend fun Dialogue.sedridorAfterQuest(npc: Npc) {
        chatPlayer(happy, "Hello there.")
        chatNpc(happy, "Hello again, ${player.displayName}. What can I do for you?")
        if (player.rmOwedTalisman) {
            giveOwedAirTalisman()
        }
        sedridorTopics(LOOKING_AROUND)
    }

    // TODO(content): [verify:in_game] how Sedridor returns the air talisman owed from a full
    // inventory; the transcript has no lines for it
    private suspend fun Dialogue.giveOwedAirTalisman() {
        chatNpc(
            happy,
            "Ah, before I forget - I still have that air talisman for you.",
        )
        if (access.invAdd(access.inv, RuneMysteriesQuest.AIR_TALISMAN).failure) {
            chatNpc(
                sad,
                "Your inventory is still full. Free up some space and speak to me again.",
            )
            return
        }
        player.rmOwedTalisman = false
        objbox(RuneMysteriesQuest.AIR_TALISMAN, "Sedridor hands you an air talisman.")
        chatNpc(happy, "There you are. I'm sure you will find it useful.")
    }

    // TODO(content): [verify:in_game] the menu while both the Persten and Temple of the Eye
    //  options show; a chatmenu holds five options, so "Who else knows..." is left out then
    private suspend fun Dialogue.sedridorTopics(farewell: String) {
        val persten =
            QuestRequirements.hasCompleted(player, DESERT_TREASURE_II) && !player.toldOfPersten
        val templeOfTheEye = QuestRequirements.hasCompleted(player, TEMPLE_OF_THE_EYE)
        val option =
            menu(
                buildList {
                    add("Can you teleport me to the Rune Essence Mine?" to 1)
                    if (persten) add("I have news on Persten." to 5)
                    if (templeOfTheEye) add("How are things at the Temple of the Eye?" to 6)
                    if (!persten || !templeOfTheEye) {
                        add("Who else knows the teleport to the Rune Essence Mine?" to 2)
                    }
                    add("Could you tell me about the old Wizards' Tower?" to 3)
                    add(farewell to 4)
                }
            )
        when (option) {
            1 -> sedridorTeleport()
            2 -> sedridorWhoElseKnows(farewell)
            3 -> {
                sedridorOldTower()
                sedridorTopics(farewell)
            }
            5 -> {
                sedridorPersten()
                sedridorTopics(farewell)
            }
            6 -> sedridorTempleOfTheEye()
            4 -> {
                chatPlayer(quiz, farewell)
                if (farewell == LOOKING_AROUND) {
                    chatNpc(
                        happy,
                        "Well, take care. You stand on the ruins of the old destroyed Wizards' " +
                            "Tower. Strange and powerful magicks lurk here.",
                    )
                }
            }
        }
    }

    private suspend fun Dialogue.sedridorPersten() {
        chatPlayer(neutral, "I have news on Persten.")
        chatNpc(quiz, "Yes?")
        chatPlayer(sad, "I'm sorry, but she's gone.")
        chatNpc(sad, "I see. It was always unlikely that she'd survive the Abyss.")
        chatPlayer(
            neutral,
            "It wasn't the Abyss. I think she might have been dead long before then. The Persten " +
                "we spoke to was an imposter.",
        )
        chatNpc(shocked, "An imposter? What do you mean?")
        chatPlayer(neutral, "A Mahjarrat called Sliske was disguised as her.")
        chatNpc(shocked, "I can't believe it. Persten and I had our differences, but this...")
        chatNpc(sad, "I'd better share this news with the others. Thank you for bringing it to me.")
        player.toldOfPersten = true
    }

    private suspend fun Dialogue.sedridorTempleOfTheEye() {
        chatPlayer(quiz, "How are things at the Temple of the Eye?")
        chatNpc(
            neutral,
            "Between the Guardians of the Rift and ourselves, we are managing to keep the Abyss " +
                "at bay.",
        )
        chatNpc(
            sad,
            "However, despite all our research, we are no closer to working out how to close " +
                "the rift permanently.",
        )
        if (player.toldOfStoneOfJas) {
            chatPlayer(quiz, "Were you able to find any information on the Stone of Jas?")
            chatNpc(
                sad,
                "Not yet. So far we've still only been able to find a few small mentions of it, " +
                    "but they all call it the Eye.",
            )
            chatNpc(
                angry,
                "I'm sure the wizards of the first tower had plenty of research that could have " +
                    "helped. All that knowledge lost because of those accursed Zamorakians!",
            )
        } else if (QuestRequirements.hasCompleted(player, DESERT_TREASURE_II)) {
            chatPlayer(
                neutral,
                "I don't know if it will be much help, but I found out its real name. It's " +
                    "called the Stone of Jas.",
            )
            chatNpc(
                happy,
                "Hmm... That could be useful. I'll have a dig around and see if we can find any " +
                    "mention of it.",
            )
            player.toldOfStoneOfJas = true
        } else {
            chatNpc(
                neutral,
                "Gaining a better understanding of the artefact that was once there is key, but " +
                    "we have found little mention of this 'Eye'.",
            )
            chatNpc(
                neutral,
                "I'm sure the wizards of the first tower had plenty of research on it. After all, " +
                    "they knew enough about it to use its symbol as the symbol for our order.",
            )
            chatNpc(angry, "All that knowledge lost because of those accursed Zamorakians!")
        }
        chatNpc(
            happy,
            "Anyway, if you wish to help defend the temple, just head through the portal.",
        )
        when (
            choice3(
                "Can you teleport me to the Rune Essence Mine?",
                1,
                "Could you tell me about the old Wizards' Tower?",
                2,
                "I might do that, thanks.",
                3,
            )
        ) {
            1 -> sedridorTeleport()
            2 -> {
                sedridorOldTower()
                sedridorTopics(LOOKING_AROUND)
            }
            else -> chatPlayer(happy, "I might do that, thanks.")
        }
    }

    private suspend fun Dialogue.sedridorTeleport() {
        chatPlayer(quiz, "Can you teleport me to the Rune Essence Mine?")
        teleportToRuneEssenceMine()
    }

    private suspend fun Dialogue.sedridorWhoElseKnows(farewell: String) {
        chatPlayer(quiz, "Who else knows the teleport to the Rune Essence Mine?")
        player.rmKnowOthers = true
        chatNpc(
            happy,
            "Apart from myself, there's also Aubury in Varrock, Wizard Cromperty in East " +
                "Ardougne, Brimstail in the Tree Gnome Stronghold and Wizard Distentor in " +
                "Yanille's Wizards' Guild.",
        )
        when (
            choice3(
                "Can you teleport me to the Rune Essence Mine?",
                1,
                "Could you tell me about the old Wizards' Tower?",
                2,
                "Thanks for the information.",
                3,
            )
        ) {
            1 -> sedridorTeleport()
            2 -> {
                sedridorOldTower()
                sedridorTopics(farewell)
            }
            3 -> {
                chatPlayer(happy, "Thanks for the information.")
                chatNpc(happy, "My pleasure.")
            }
        }
    }

    private suspend fun Dialogue.sedridorOldTower() {
        chatPlayer(quiz, "Could you tell me about the old Wizards' Tower?")
        chatNpc(
            happy,
            "Of course. The first Wizards' Tower was built at the same time the Order of " +
                "Wizards was founded. It was at the dawn of the Fifth Age, when the secrets " +
                "of runecrafting were rediscovered.",
        )
        chatNpc(
            happy,
            "For years, the tower was a hub of magical research. Wizards of all races and " +
                "religions were welcomed into our order.",
        )
        chatNpc(
            sad,
            "Alas, that openness is what ultimately led to disaster. The wizards who served " +
                "Zamorak, the evil god of chaos, tried to claim our magical discoveries in " +
                "his name.",
        )
        chatNpc(
            sad,
            "They failed, but in retaliation, they burnt the entire tower to the ground. " +
                "Years of work was destroyed.",
        )
        chatNpc(
            happy,
            "The tower was soon rebuilt of course, but even now we are still trying to regain " +
                "knowledge that was lost.",
        )
        chatNpc(
            happy,
            "That's why I spend my time down here, in fact. This basement is all that is left " +
                "of the old tower, and I believe there are still some secrets to discover here.",
        )
        chatNpc(
            happy,
            "Of course, one secret I am no longer looking for is the teleportation " +
                "incantation to the Rune Essence Mine. We have you to thank for that.",
        )
    }

    private fun hasAirTalisman(player: Player): Boolean =
        player.inv.contains(RuneMysteriesQuest.AIR_TALISMAN)

    private companion object {
        const val LOOKING_AROUND = "Nothing thanks, I'm just looking around."
        const val GET_GOING = "I'd better get going."
        const val DESERT_TREASURE_II = "quest_deserttreasure2"
        const val TEMPLE_OF_THE_EYE = "quest_templeoftheeye"
    }
}
