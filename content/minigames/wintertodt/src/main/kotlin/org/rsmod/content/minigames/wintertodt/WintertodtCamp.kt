package org.rsmod.content.minigames.wintertodt

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import kotlin.math.min
import org.rsmod.api.config.Constants
import org.rsmod.api.invtx.invDel
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.events.interact.NpcUDefaultEvents
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseFiremakingLvl
import org.rsmod.api.random.GameRandom
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

internal const val IGNISIA = "npc.wint_master_pyromancer"
internal const val EXTRA_CRATE = "obj.wint_reroll_box"
internal const val BURNT_PAGE = "obj.wint_burnt_page"
internal const val EMPTY_TOME = "obj.tome_of_fire_uncharged"
internal const val BRUMA_TORCH = Wintertodt.TORCH
internal const val BRUMA_TORCH_OFFHAND = Wintertodt.TORCH_OFFHAND
internal const val WARM_GLOVES = "obj.pyromancer_gloves"

internal val PYROMANCER_PIECES =
    listOf(
        "obj.pyromancer_top",
        "obj.pyromancer_hood",
        "obj.pyromancer_bottom",
        "obj.pyromancer_boots",
    )

internal suspend fun ProtectedAccess.talkToIgnisia(npc: Npc) {
    startDialogue(npc) { ignisiaDialogue() }
}

private suspend fun Dialogue.ignisiaDialogue() {
    val title =
        if (player.appearance.bodyType == Constants.bodytype_a) "brother" else "sister"
    if (player.wintertodtIntroSeen == 0) {
        chatNpc(happy, "Welcome, $title.")
        chatPlayer(quiz, "Er... Are we related?")
        chatNpc(neutral, "We are all related, $title, for we are all children of the flame.")
        chatPlayer(neutral, "Okayyy...")
        chatPlayer(quiz, "So what is this place?")
        chatNpc(
            neutral,
            "You stand before the Doors of Dinh. Behind them lies the prison of the Wintertodt.",
        )
        chatPlayer(quiz, "What's a Wintertodt?")
        chatNpc(
            neutral,
            "We do not know what it is. All we know is that it threatens to snuff out every " +
                "flame in this land and plunge the world into eternal darkness.",
        )
        chatPlayer(quiz, "Oh dear... We should probably do something about that.")
        chatNpc(
            neutral,
            "That is why we are here, $title. Ever since the Wintertodt was imprisoned, " +
                "members of the Order of the Sacred Flame, or Pyromancers as we're often known, " +
                "have stood vigil over this place.",
        )
        chatNpc(neutral, "But alas, despite our best efforts, the Wintertodt is growing in power again.")
        chatPlayer(quiz, "Well that's just no good. Is there anything I can do to help?")
        chatNpc(
            neutral,
            "Our order searches tirelessly for a way to defeat the Wintertodt. Until we discover " +
                "it, the Wintertodt must be held at bay.",
        )
        if (player.baseFiremakingLvl < Wintertodt.FM_LEVEL) {
            chatNpc(
                neutral,
                "However, I fear your abilities are not great enough to withstand the cold of the " +
                    "Wintertodt. Learn the ways of the sacred flame and then return to me.",
            )
            mesbox("You require at least ${Wintertodt.FM_LEVEL} Firemaking to take on the Wintertodt.")
        } else {
            chatNpc(
                neutral,
                "Pass through the Doors of Dinh to the prison within. You will find the finest of " +
                    "our order channeling the power of the sacred flame into the Wintertodt.",
            )
            chatNpc(
                neutral,
                "Help them keep the holy fires lit so that they can keep the Wintertodt subdued.",
            )
            chatPlayer(quiz, "How can I do that?")
            chatNpc(
                neutral,
                "You will find the roots of the great Bruma tree have crept into the prison. Chop " +
                    "the roots and use them to keep the sacred flame alive.",
            )
            chatPlayer(quiz, "Anything else?")
            chatNpc(
                neutral,
                "You can use the herbs of the Bruma tree to make Rejuvenation potions. Give these " +
                    "to any of our order who fall to the cold of the Wintertodt before they " +
                    "succumb to the darkness.",
            )
        }
        chatNpc(neutral, "How else may I be of assistance?")
        player.wintertodtIntroSeen = 1
    } else {
        chatNpc(happy, "Welcome back, $title. How may I be of assistance?")
    }
    val seen = mutableSetOf<Int>()
    while (true) {
        val options =
            buildList {
                if (1 !in seen) add("Tell me about the Wintertodt." to 1)
                if (2 !in seen) add("Tell me about the Pyromancers." to 2)
                if (3 !in seen) add("Tell me about yourself." to 3)
                if (4 !in seen) add("What can I do to help?" to 4)
                add("I'm fine thanks." to 5)
            }
        when (menu(options)) {
            1 -> {
                chatPlayer(quiz, "Tell me about the Wintertodt.")
                chatNpc(
                    neutral,
                    "Do you feel the winds of winter as they lick across the land? " +
                        "This is the power of the Wintertodt. We know not what it is nor from " +
                        "where it came. Only that it wields the power of Winter itself.",
                )
                chatNpc(
                    neutral,
                    "The ground on which we stand was once full of light, full of life. " +
                        "But then the Wintertodt came. Now the fires of these lands burn no more.",
                )
                chatNpc(neutral, "How else may I be of assistance?")
                seen += 1
            }
            2 -> {
                chatPlayer(quiz, "Tell me about the Pyromancers.")
                chatNpc(
                    neutral,
                    "We are the Order of the Sacred Flame, formed over a thousand years ago " +
                        "when the Wintertodt first threatened Great Kourend.",
                )
                chatNpc(
                    neutral,
                    "We fought against the Wintertodt until we managed to lure it behind " +
                        "Dinh's great Doors. Our task did not end there though, for we always " +
                        "knew that the Wintertodt would rise again.",
                )
                chatNpc(neutral, "How else may I be of assistance?")
                seen += 2
            }
            3 -> {
                chatPlayer(quiz, "Tell me about yourself.")
                chatNpc(
                    neutral,
                    "I am Ignisia, Grand Master of the Order of the Sacred Flame. I have served " +
                        "our Order faithfully since I passed the Trials of Fire before the " +
                        "Ascent of Arceuus.",
                )
                chatNpc(neutral, "How else may I be of assistance?")
                seen += 3
            }
            4 -> {
                chatPlayer(quiz, "What can I do to help?")
                if (player.baseFiremakingLvl < Wintertodt.FM_LEVEL) {
                    chatNpc(
                        neutral,
                        "You are not currently strong enough to face the Wintertodt. " +
                            "Learn the ways of the sacred flame and then return here.",
                    )
                    mesbox("You require at least ${Wintertodt.FM_LEVEL} Firemaking to take on the Wintertodt.")
                } else {
                    chatNpc(
                        neutral,
                        "We must hold back the Wintertodt until we can find a way to defeat it. " +
                            "Head into the prison and keep the holy fires lit so that our order " +
                            "may keep the evil at bay.",
                    )
                    chatPlayer(quiz, "How can I do that?")
                    chatNpc(
                        neutral,
                        "You will find the roots of the great Bruma tree have crept into the " +
                            "prison. Chop the roots and use them to keep the sacred flame alive.",
                    )
                    chatPlayer(quiz, "Anything else?")
                    chatNpc(
                        neutral,
                        "You can use the herbs of the Bruma tree to make Rejuvenation potions. " +
                            "Give these to any of our order who fall to the cold of the " +
                            "Wintertodt before they succumb to the darkness.",
                    )
                    chatNpc(neutral, "How else may I be of assistance?")
                }
                seen += 4
            }
            else -> {
                chatPlayer(happy, "I'm fine thanks.")
                chatNpc(happy, "May the sacred flame guide you, $title.")
                return
            }
        }
    }
}

internal suspend fun ProtectedAccess.exchangeWithIgnisia(npc: Npc) {
    startDialogue(npc) { exchangeDialogue() }
}

private suspend fun Dialogue.exchangeDialogue() {
    chatNpc(neutral, "Do you have something for me?")
    val pieces = PYROMANCER_PIECES.associateWith { player.inv.count(it) }.filterValues { it > 0 }
    val others =
        listOf(EMPTY_TOME, BRUMA_TORCH, BRUMA_TORCH_OFFHAND, WARM_GLOVES)
            .associateWith { player.inv.count(it) }
            .filterValues { it > 0 }
    if (pieces.isEmpty() && others.isEmpty()) {
        return
    }
    if (pieces.isNotEmpty()) {
        val choice =
            choice2(
                "Crates.",
                1,
                "Burnt Pages.",
                2,
                title = "What would you like to trade your Pyromancer pieces for?",
            )
        if (choice == 1) exchangePiecesForCrates(pieces) else exchangePiecesForPages(pieces)
        return
    }
    val item = others.keys.first()
    val count = others.getValue(item)
    val choice = choice2("Extra Crate.", 1, "Burnt Pages.", 2, title = "What would you like to trade for?")
    if (choice == 1) {
        exchangeForCrate(item, count)
    } else {
        exchangeForPages(item, count)
    }
}

private suspend fun Dialogue.exchangePiecesForCrates(pieces: Map<String, Int>) {
    val total = pieces.values.sum()
    if (pieces.size == PYROMANCER_PIECES.size) {
        val set =
            choice2(
                "Yes.",
                true,
                "No.",
                false,
                title = "Would you like to trade in a entire set?",
            )
        if (set) {
            exchange(
                remove = PYROMANCER_PIECES.associateWith { 1 },
                addItem = EXTRA_CRATE,
                addCount = 1,
                addVars = 5,
            )
            return
        }
    }
    exchange(remove = pieces, addItem = EXTRA_CRATE, addCount = total, addVars = 1)
}

private suspend fun Dialogue.exchangePiecesForPages(pieces: Map<String, Int>) {
    if (pieces.size == PYROMANCER_PIECES.size) {
        val set =
            choice2(
                "Yes.",
                true,
                "No.",
                false,
                title = "Would you like to trade in a entire set?",
            )
        if (set) {
            exchange(
                remove = PYROMANCER_PIECES.associateWith { 1 },
                addItem = BURNT_PAGE,
                addCount = 250,
            )
            return
        }
    }
    exchange(
        remove = pieces,
        addItem = BURNT_PAGE,
        addCount = pieces.values.sum() * 50,
    )
}

private suspend fun Dialogue.exchangeForCrate(item: String, count: Int) {
    val name = itemName(item)
    val confirm =
        choice2(
            "Yes.",
            true,
            "No.",
            false,
            title = "Trade in ${count}x $name for ${count}x extra supply crate?",
        )
    if (!confirm) return
    exchange(remove = mapOf(item to count), addItem = EXTRA_CRATE, addCount = count, addVars = 1)
}

private suspend fun Dialogue.exchangeForPages(item: String, count: Int) {
    val name = itemName(item)
    val pages = if (item == EMPTY_TOME) count * 250 else count * 50
    val confirm =
        choice2(
            "Yes.",
            true,
            "No.",
            false,
            title = "Trade in ${count}x $name for ${pages}x burnt pages?",
        )
    if (!confirm) return
    exchange(remove = mapOf(item to count), addItem = BURNT_PAGE, addCount = pages)
}

private fun itemName(item: String): String =
    when (item) {
        EMPTY_TOME -> "Tome of fire (empty)"
        BRUMA_TORCH -> "Bruma torch"
        BRUMA_TORCH_OFFHAND -> "Bruma torch (off-hand)"
        WARM_GLOVES -> "Warm gloves"
        "obj.pyromancer_top" -> "Pyromancer garb"
        "obj.pyromancer_hood" -> "Pyromancer hood"
        "obj.pyromancer_bottom" -> "Pyromancer robe"
        "obj.pyromancer_boots" -> "Pyromancer boots"
        else -> item.removePrefix("obj.")
    }

private fun Dialogue.exchange(
    remove: Map<String, Int>,
    addItem: String,
    addCount: Int,
    addVars: Int = 0,
) {
    val result =
        player.invTransaction(player.inv) {
            val from = select(player.inv)
            for ((item, count) in remove) {
                delete {
                    this.from = from
                    this.obj = item.asRSCM(RSCMType.OBJ)
                    this.count = count
                }
            }
            insert {
                this.into = from
                this.obj = addItem.asRSCM(RSCMType.OBJ)
                this.count = addCount
                this.vars = addVars
            }
        }
    if (result.failure) {
        player.mes("You don't have enough inventory space to do that.")
    }
}

internal suspend fun ProtectedAccess.talkToUndor(npc: Npc) {
    startDialogue(npc) { undorDialogue() }
}

private suspend fun Dialogue.undorDialogue() {
    val title = if (player.appearance.bodyType == Constants.bodytype_a) "laddie" else "lass"
    chatNpc(neutral, "Hello there $title.")
    val seen = mutableSetOf<UndorTopic>()
    while (true) {
        val options =
            buildList {
                for (topic in UndorTopic.entries) {
                    if (topic != UndorTopic.Leave && topic !in seen) add(topic.label to topic)
                }
                add("Goodbye." to UndorTopic.Leave)
            }
        when (menu(options)) {
            UndorTopic.Wintertodt -> {
                chatPlayer(quiz, "Tell me about the Wintertodt.")
                chatNpc(neutral, "They say it's some kind of powerful fire spirit.")
                chatPlayer(quiz, "Don't you mean ice?")
                chatNpc(neutral, "Aye that as well.")
                chatNpc(
                    neutral,
                    "What it is doesn't bother me, just as long as we keep it behind the Doors.",
                )
                seen += UndorTopic.Wintertodt
            }
            UndorTopic.Doors -> {
                chatPlayer(quiz, "Tell me about the Doors of Dinh.")
                chatNpc(
                    neutral,
                    "The Doors were built by Dinh to contain the Wintertodt. This was over a " +
                        "thousand years ago and they still stand today.",
                )
                chatPlayer(quiz, "Who was Dinh?")
                chatNpc(
                    neutral,
                    "Dinh was the greatest smith that the world has ever seen. I am the finest " +
                        "smith around today and even I cannot match his abilities.",
                )
                seen += UndorTopic.Doors
            }
            UndorTopic.Yourself -> {
                chatPlayer(quiz, "Tell me about yourself.")
                chatNpc(
                    neutral,
                    "The name's Undor. You'll struggle to find a dwarf in this land who knows " +
                        "more about metalwork than me.",
                )
                chatNpc(neutral, "One day I hope to be as good as the legendary Dinh himself.")
                chatPlayer(quiz, "What are you doing here?")
                chatNpc(
                    neutral,
                    "They say the doors are failing and the Wintertodt is trying to escape. I'm " +
                        "here to prove to them that the Doors are fine.",
                )
                chatPlayer(quiz, "And are they?")
                chatNpc(neutral, "Of course they are! They were made by Dinh himself.")
                seen += UndorTopic.Yourself
            }
            UndorTopic.Leave -> {
                chatPlayer(neutral, "Goodbye.")
                chatNpc(neutral, "Until next time $title.")
                return
            }
        }
    }
}

private enum class UndorTopic(val label: String) {
    Wintertodt("Tell me about the Wintertodt."),
    Doors("Tell me about the Doors of Dinh."),
    Yourself("Tell me about yourself."),
    Leave("Goodbye."),
}

internal suspend fun ProtectedAccess.talkToEsther(npc: Npc) {
    startDialogue(npc) { estherDialogue() }
}

private suspend fun Dialogue.estherDialogue() {
    chatNpc(neutral, "Are you here to fight the cold?")
    chatPlayer(quiz, "Umm... I guess?")
    chatNpc(happy, "Great! Ask me anything you like about the Bruma tree, I'm an expert.")
    val seen = mutableSetOf<EstherTopic>()
    while (true) {
        val options =
            buildList {
                for (topic in EstherTopic.entries) {
                    if (topic != EstherTopic.Leave && topic !in seen) add(topic.label to topic)
                }
                add("See you later." to EstherTopic.Leave)
            }
        when (menu(options)) {
            EstherTopic.Wintertodt -> {
                chatPlayer(quiz, "Tell me about the Wintertodt.")
                chatNpc(
                    neutral,
                    "Some would say that ice preserves while fire destroys, but no amount of " +
                        "fire is destroying the Wintertodt. That's a cold that will freeze the " +
                        "air in your lungs.",
                )
                chatNpc(
                    neutral,
                    "Even the heat generated from burning the Bruma roots only weakens it, and " +
                        "that doesn't last very long nowadays.",
                )
                chatPlayer(neutral, "Looks like an angry snow storm to me...")
                chatNpc(
                    neutral,
                    "What you see in that pit is not the Wintertodt at full strength. It never " +
                        "will be while the Bruma tree still burns and the Doors of Dinh remain closed.",
                )
                seen += EstherTopic.Wintertodt
            }
            EstherTopic.Bruma -> {
                chatPlayer(quiz, "Tell me about the Bruma tree.")
                chatNpc(
                    neutral,
                    "The Bruma tree is what keeps the Wintertodt weak. The prison was built here " +
                        "by Dinh because of its usefulness against the Wintertodt.",
                )
                chatNpc(
                    neutral,
                    "Some legends say that Xeric is the cause for the Doors weakening, or maybe " +
                        "he made the Wintertodt stronger?",
                )
                chatPlayer(quiz, "Is Xeric really such a bad guy?")
                chatNpc(
                    neutral,
                    "It's a known fact that he was interested in the Bruma tree. For what " +
                        "purpose... there are only rumours.",
                )
                chatNpc(
                    neutral,
                    "Regardless, the Bruma tree definitely contains powerful magic. I've been " +
                        "studying the sap for years and it has truly astounding properties.",
                )
                chatNpc(neutral, "There's not many fires that burn green now, are there?")
                chatPlayer(quiz, "What about gnomish fire lighters?")
                chatNpc(neutral, "...")
                seen += EstherTopic.Bruma
            }
            EstherTopic.Yourself -> {
                chatPlayer(quiz, "Tell me about yourself.")
                chatNpc(
                    neutral,
                    "Me? I'm a member of the Woodcutting Guild. More of a researcher though... " +
                        "I'm currently tasked with growing a new Bruma tree.",
                )
                chatPlayer(quiz, "How's that going?")
                chatNpc(
                    neutral,
                    "Poorly. I believe the Bruma tree can only be grown in a freezing cold climate.",
                )
                chatPlayer(quiz, "Well, isn't that here?")
                chatNpc(
                    neutral,
                    "It's just an idea really. For all I know, it could be magic way beyond me, " +
                        "beyond even the Pyromancers.",
                )
                seen += EstherTopic.Yourself
            }
            EstherTopic.Leave -> {
                chatPlayer(neutral, "See you later.")
                chatNpc(happy, "Goodbye!")
                return
            }
        }
    }
}

private enum class EstherTopic(val label: String) {
    Wintertodt("Tell me about the Wintertodt."),
    Bruma("Tell me about the Bruma tree."),
    Yourself("Tell me about yourself."),
    Leave("See you later."),
}

internal suspend fun ProtectedAccess.talkToWinterSoldier(npc: Npc, random: GameRandom) {
    val lines =
        listOf(
            "This could be worse I guess. Oh wait... No it can't.",
            "I'm hungry...",
            "Sometimes I wish this thing would just escape. At least we'd have something to do then.",
            "It's so cold...",
        )
    startDialogue(npc) { chatNpc(neutral, lines[random.of(lines.size)]) }
}

private const val TUNA = "obj.tuna"
private const val CATSPEAK_AMULET = "obj.ics_little_amulet_of_catspeak"
private const val DRAGON_SLAYER_2 = "quest_dragonslayer2"

internal suspend fun ProtectedAccess.talkToWintertodtCat(npc: Npc) {
    if (!catspeak(player)) {
        startDialogue(npc) { chatNpc(neutral, "Meow.") }
        return
    }
    startDialogue(npc) { catDialogue() }
}

private fun catspeak(player: Player): Boolean =
    CATSPEAK_AMULET in player.worn ||
        QuestRequirements.isOnQuest(player, DRAGON_SLAYER_2) ||
        QuestRequirements.hasCompleted(player, DRAGON_SLAYER_2)

private suspend fun Dialogue.catDialogue() {
    chatPlayer(quiz, "Hello puss.")
    chatNpc(neutral, "Want a mouse?")
    chatPlayer(neutral, "No thanks.")
    chatNpc(neutral, "Want this half dead bird?")
    chatPlayer(neutral, "Errrr... no.")
    chatNpc(neutral, "Want this crunched up spider?")
    chatPlayer(neutral, "Um... not really.")
    chatNpc(neutral, "Oh well. Got any tuna?")
    if (player.inv.count(TUNA) <= 0) {
        chatPlayer(neutral, "No.")
        chatNpc(neutral, "Hmmmph.")
        return
    }
    chatPlayer(happy, "Why yes I do have some tuna.")
    chatNpc(happy, "Give! *purr*")
    val give = choice2("Give a tuna to the cat.", true, "Keep the tuna for yourself.", false)
    if (!give) {
        chatPlayer(neutral, "Nah... my tuna.")
        chatNpc(neutral, "Hissss...")
        return
    }
    player.invDel(player.inv, TUNA, 1)
    objbox(TUNA, "You give some tuna to the cat. It devours it and purrs loudly.")
    chatNpc(neutral, "Catch toad for me.")
    chatPlayer(quiz, "What? I think not...")
}

internal suspend fun ProtectedAccess.checkKaltScores(npc: Npc) {
    startDialogue(npc) { kaltScoreDialogue() }
}

private suspend fun Dialogue.kaltScoreDialogue() {
    val kills = player.wintertodtKills
    when {
        kills <= 0 ->
            chatNpc(neutral, "Go play in the snow and come back when you've done something.")
        kills < 10 -> chatNpc(neutral, "You appear to have tried a little.")
        else -> chatNpc(neutral, "Ignisia appears to be quite happy with you.")
    }
    if (kills > 0) {
        mesbox(
            "You have subdued the Wintertodt $kills times.<br>" +
                "Your lifetime score: ${player.wintertodtLifetimeScore}<br>" +
                "Your highest score: ${player.wintertodtHighestScore}",
        )
    }
}

internal suspend fun ProtectedAccess.talkToIsh(npc: Npc) {
    startDialogue(npc) { ishDialogue() }
}

private suspend fun Dialogue.ishDialogue() {
    val seen = mutableSetOf<IshTopic>()
    chatNpc(neutral, "Be on your guard friend.")
    while (true) {
        val options =
            buildList {
                for (topic in IshTopic.entries) {
                    if (topic != IshTopic.Leave && topic !in seen) add(topic.label to topic)
                }
                add("I have to go." to IshTopic.Leave)
            }
        when (menu(options)) {
            IshTopic.Wintertodt -> {
                chatPlayer(quiz, "Tell me about the Wintertodt.")
                chatNpc(
                    neutral,
                    "The Wintertodt terrorised Great Kourend many centuries ago. It destroyed " +
                        "much of this northern land over many years before it was finally driven " +
                        "back into this prison.",
                )
                chatPlayer(quiz, "Why did it attack Kourend?")
                chatNpc(neutral, "No one knows for sure but I have a theory...")
                chatPlayer(quiz, "Which is?")
                chatNpc(
                    neutral,
                    "The Dark Altar. It felt the altar's power and wanted to consume it for " +
                        "itself. It wouldn't be the first time...",
                )
                chatPlayer(quiz, "Interesting...")
                seen += IshTopic.Wintertodt
            }
            IshTopic.Yourself -> {
                chatPlayer(quiz, "Tell me about yourself.")
                chatNpc(neutral, "I'm Ish, one of the navigators of Piscarilius.")
                chatPlayer(quiz, "What brings a navigator up here?")
                chatNpc(
                    neutral,
                    "Lady Piscarilius has sent me here to keep an eye on the situation. She " +
                        "doesn't trust the other houses and believes something sinister is " +
                        "going on.",
                )
                chatPlayer(quiz, "Like what?")
                chatNpc(neutral, "I don't know, but I intend to find out.")
                seen += IshTopic.Yourself
            }
            IshTopic.Pyromancers -> {
                chatPlayer(quiz, "What do you think of the Pyromancers?")
                chatNpc(
                    neutral,
                    "I've heard stories about them and the things they have done over the " +
                        "years. Be careful, they can't be trusted.",
                )
                chatPlayer(quiz, "You seem a bit paranoid.")
                chatNpc(
                    neutral,
                    "Better to be paranoid than a fool. Anyone with any sense knows that the " +
                        "Pyromancers are trouble. They say even Lord Arceuus doesn't trust them.",
                )
                seen += IshTopic.Pyromancers
            }
            IshTopic.Leave -> {
                chatPlayer(neutral, "I have to go.")
                chatNpc(neutral, "Stay vigilant friend.")
                return
            }
        }
    }
}

private enum class IshTopic(val label: String) {
    Wintertodt("Tell me about the Wintertodt."),
    Yourself("Tell me about yourself."),
    Pyromancers("What do you think of the Pyromancers?"),
    Leave("I have to go."),
}

internal suspend fun ProtectedAccess.talkToKalt(npc: Npc) {
    startDialogue(npc) { kaltDialogue() }
}

private suspend fun Dialogue.kaltDialogue() {
    val seen = mutableSetOf<KaltTopic>()
    chatNpc(neutral, "Hello there.")
    while (true) {
        val options =
            buildList {
                for (topic in KaltTopic.entries) {
                    if (topic != KaltTopic.Leave && topic !in seen) add(topic.label to topic)
                }
                add("See you later." to KaltTopic.Leave)
            }
        when (menu(options)) {
            KaltTopic.Wintertodt -> {
                chatPlayer(quiz, "Tell me about the Wintertodt.")
                chatNpc(neutral, "Ignisia claims it's some kind of evil spirit.")
                chatPlayer(quiz, "You don't believe her?")
                chatNpc(
                    neutral,
                    "Nah, I believe in things I can stab and kill. This is all just a storm in " +
                        "a teacup. I mean, how can wind and snow be evil? My toad is more of a " +
                        "threat!",
                )
                chatPlayer(quiz, "Your... toad?")
                chatNpc(
                    neutral,
                    "Yeah, just a bit of fun. I have a pet - he's around here somewhere, " +
                        "assuming that troublesome cat hasn't eaten him.",
                )
                chatPlayer(quiz, "That's a bit weird, but okay.")
                chatNpc(neutral, "No weirder than being scared of snow.")
                seen += KaltTopic.Wintertodt
            }
            KaltTopic.Yourself -> {
                chatPlayer(quiz, "Tell me about yourself.")
                chatNpc(neutral, "I'm Captain Kalt, proud member of the Shayzien Army.")
                chatPlayer(quiz, "How long have you been a soldier?")
                chatNpc(
                    neutral,
                    "Man and boy I've worn the uniform. Earned Captain a few years back and " +
                        "never looked back. Not too happy about babysitting delusional wizards " +
                        "though. But orders is orders.",
                )
                seen += KaltTopic.Yourself
            }
            KaltTopic.Scores -> {
                chatPlayer(quiz, "Tell me how I'm doing.")
                kaltScoreDialogue()
                return
            }
            KaltTopic.Leave -> {
                chatPlayer(neutral, "See you later.")
                chatNpc(neutral, "Goodbye.")
                return
            }
        }
    }
}

private enum class KaltTopic(val label: String) {
    Wintertodt("Tell me about the Wintertodt."),
    Yourself("Tell me about yourself."),
    Scores("Tell me how I'm doing."),
    Leave("See you later."),
}

internal suspend fun ProtectedAccess.talkToBrewma(npc: Npc) {
    startDialogue(npc) {
        chatPlayer(quiz, "Who are you?")
        chatNpc(neutral, "My name is Brew'ma. I'm here to assist adventurers like you with potion making.")
        chatNpc(
            neutral,
            "If you hand me a herb from the Bruma tree and one of the unfinished potions " +
                "from the crate, I can combine the two if you lack the... talent to do so " +
                "yourself.",
        )
        chatPlayer(happy, "Thanks!")
    }
}

internal suspend fun ProtectedAccess.useItemOnBrewma(event: NpcUDefaultEvents.OpType) {
    val item = event.objType.internalName
    when (item) {
        Wintertodt.HERB -> {
            if (player.inv.count(Wintertodt.VIAL) <= 0) {
                startDialogue(event.npc) {
                    chatNpc(
                        neutral,
                        "You don't have any unfinished potions on you. I can't use the herbs " +
                            "without them.",
                    )
                }
            } else {
                startDialogue(event.npc) { mixPotions() }
            }
        }
        Wintertodt.VIAL -> {
            if (player.inv.count(Wintertodt.HERB) <= 0) {
                startDialogue(event.npc) {
                    chatNpc(
                        neutral,
                        "You don't have any Bruma herbs on you. I need them to finish the " +
                            "potions.",
                    )
                }
            } else {
                startDialogue(event.npc) { mixPotions() }
            }
        }
        else -> {
            startDialogue(event.npc) {
                chatNpc(neutral, "Sorry, I can't do anything with that.")
            }
        }
    }
}

private suspend fun Dialogue.mixPotions() {
    val amount =
        min(player.inv.count(Wintertodt.HERB), player.inv.count(Wintertodt.VIAL))
    if (amount <= 0) return
    val result =
        player.invTransaction(player.inv) {
            val from = select(player.inv)
            delete {
                this.from = from
                this.obj = Wintertodt.HERB.asRSCM(RSCMType.OBJ)
                this.strictCount = amount
            }
            delete {
                this.from = from
                this.obj = Wintertodt.VIAL.asRSCM(RSCMType.OBJ)
                this.strictCount = amount
            }
            insert {
                this.into = from
                this.obj = Wintertodt.POTION4.asRSCM(RSCMType.OBJ)
                this.count = amount
            }
        }
    if (result.failure) {
        player.mes("You don't have enough inventory space to do that.")
        return
    }
    if (amount == 1) {
        objbox(Wintertodt.POTION4, "Brew'ma quickly mixes a Rejuvenation potion.")
    } else {
        val label = if (amount == 2) "two" else amount.toString()
        doubleobjbox(
            Wintertodt.POTION4,
            400,
            Wintertodt.POTION4,
            400,
            "Brew'ma quickly mixes $label Rejuvenation potions.",
        )
    }
}

private suspend fun <T> Dialogue.menu(options: List<Pair<String, T>>): T {
    val o = options
    return when (o.size) {
        1 -> o[0].second
        2 -> choice2(o[0].first, o[0].second, o[1].first, o[1].second)
        3 ->
            choice3(
                o[0].first,
                o[0].second,
                o[1].first,
                o[1].second,
                o[2].first,
                o[2].second,
            )
        4 ->
            choice4(
                o[0].first,
                o[0].second,
                o[1].first,
                o[1].second,
                o[2].first,
                o[2].second,
                o[3].first,
                o[3].second,
            )
        5 ->
            choice5(
                o[0].first,
                o[0].second,
                o[1].first,
                o[1].second,
                o[2].first,
                o[2].second,
                o[3].first,
                o[3].second,
                o[4].first,
                o[4].second,
            )
        else -> error("Unsupported menu size: ${o.size}")
    }
}
