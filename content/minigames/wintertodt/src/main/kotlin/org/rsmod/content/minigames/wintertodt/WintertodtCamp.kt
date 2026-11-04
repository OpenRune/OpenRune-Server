package org.rsmod.content.minigames.wintertodt

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import kotlin.math.min
import org.rsmod.api.config.Constants
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.events.interact.NpcUDefaultEvents
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseFiremakingLvl
import org.rsmod.game.entity.Npc

internal const val IGNISIA = "npc.wint_master_pyromancer"
internal const val EXTRA_CRATE = "obj.wint_reroll_box"
internal const val BURNT_PAGE = "obj.wint_burnt_page"
internal const val EMPTY_TOME = "obj.tome_of_fire_uncharged"
internal const val BRUMA_TORCH = "obj.wint_torch"
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
    chatNpc(happy, "Welcome back, $title. How may I be of assistance?")
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
                        "where it came. Only that it wields the power of Winter itself",
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
                        "The Wintertodt is no longer growing in strength but it still needs to " +
                            "be held back. Head into the prison and keep the holy fires lit so " +
                            "that our order may keep the evil at bay.",
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
    val others = listOf(EMPTY_TOME, BRUMA_TORCH, WARM_GLOVES).associateWith { player.inv.count(it) }.filterValues { it > 0 }
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
                chatNpc(neutral, "Ignisia appears to be quite happy with you.")
                seen += KaltTopic.Scores
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
                    doubleobjbox(
                        Wintertodt.POTION4,
                        400,
                        Wintertodt.POTION4,
                        400,
                        "Brew'ma quickly mixes zero Rejuvenation potions.",
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
        doubleobjbox(
            Wintertodt.POTION4,
            400,
            Wintertodt.POTION4,
            400,
            "Brew'ma quickly mixes $amount Rejuvenation potions.",
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
