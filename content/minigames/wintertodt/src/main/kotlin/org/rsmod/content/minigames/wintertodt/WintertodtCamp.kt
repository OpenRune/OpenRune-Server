package org.rsmod.content.minigames.wintertodt

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.config.Constants
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
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
    chatNpc(happy, "Welcome, $title. How may I be of assistance?")
    while (true) {
        val choice =
            choice5(
                "Tell me about the Wintertodt.",
                1,
                "Tell me about the Pyromancers.",
                2,
                "Tell me about yourself.",
                3,
                "What can I do to help?",
                4,
                "I'm fine thanks.",
                5,
            )
        when (choice) {
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
    val pieces = PYROMANCER_PIECES.associateWith { player.inv.count(it) }.filterValues { it > 0 }
    val others = listOf(EMPTY_TOME, BRUMA_TORCH, WARM_GLOVES).associateWith { player.inv.count(it) }.filterValues { it > 0 }
    if (pieces.isEmpty() && others.isEmpty()) {
        chatNpc(neutral, "Do you have something for me?")
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
