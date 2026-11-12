package org.rsmod.content.skills.agility.arena

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.interfaces.omnishop.onOmnishopVirtualPurchase
import org.rsmod.content.interfaces.omnishop.openOmnishop
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Pirate Jackie the Fruit trades arena tickets and vouchers in the cache's Agility Arena Store
 * (https://oldschool.runescape.wiki/w/Pirate_Jackie_the_Fruit). Its Agility experience stock
 * hands over no obj: each ticket spent on it pays [TICKET_XP_TENTHS], or [GLOVES_XP_TENTHS] in
 * Karamja gloves 2 or better (https://oldschool.runescape.wiki/w/Agility_arena_ticket).
 */
class PirateJackieScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(JACKIE) { startDialogue(it.npc) { jackie() } }
        onOpNpc3(JACKIE) { openOmnishop(STORE) }
        onOmnishopVirtualPurchase(XP_STOCK) {
            val tickets = pay(requested)
            if (tickets > 0) {
                access.statAdvance(AGILITY, ticketXpTenths(tickets, access.wearsGloves()) / 10.0)
            }
        }
    }

    private fun ProtectedAccess.wearsGloves(): Boolean = GLOVES.any { worn.count(it) > 0 }

    // UNVERIFIED: the wiki gives this dialogue only as a paraphrase, bar the greeting.
    private suspend fun Dialogue.jackie() {
        chatNpc(happy, "Ahoy!")
        // TODO(content): the menu's second page, behind a "MORE" option live shows.
        val choice =
            choice4(
                "What is this place?",
                1,
                "What do you do?",
                2,
                "I'd like to trade in my tickets, please.",
                3,
                "I have a question about my Achievement Diary.",
                4,
            )
        when (choice) {
            1 -> {
                chatPlayer(quiz, "What is this place?")
                chatNpc(
                    happy,
                    "Welcome to the Brimhaven Agility Arena! Cap'n Izzy can tell you more of " +
                        "its history.",
                )
            }
            2 -> {
                chatPlayer(quiz, "What do you do?")
                chatNpc(
                    happy,
                    "I trade the tickets you collect in the arena for Agility experience and " +
                        "items you won't find anywhere else.",
                )
            }
            3 -> {
                chatPlayer(happy, "I'd like to trade in my tickets, please.")
                access.openOmnishop(STORE)
            }
            4 -> diary()
        }
    }

    private suspend fun Dialogue.diary() {
        chatPlayer(neutral, "I have a question about my Achievement Diary.")
        if (vars[EASY_DONE] != EASY_COMPLETE || vars[EASY_REWARD] != 0) {
            // TODO(content): the diary menu (what the diary is, its rewards, replacement gloves)
            // and the medium, hard and elite rewards.
            return
        }
        chatPlayer(happy, "I've done all the easy tasks in my Karamja Achievement Diary.")
        chatNpc(
            happy,
            "Arr, ye have that, I see yer list. I s'pose ye'll be wantin' yer reward then!",
        )
        chatPlayer(happy, "Yes please.")
        if (!access.claimEasyReward()) {
            // UNVERIFIED: wording guessed; no capture had a full inventory.
            chatNpc(neutral, "Ye'll be needin' more room in yer pack for that, matey.")
            return
        }
        chatNpc(
            neutral,
            "These 'ere Karamja gloves be a symbol of yer explorin' on the island. I'll 'ave a " +
                "word with some of the seafarin' folk who sail to Port Sarim and Ardougne, so " +
                "they'll take ye on board half price if ye be wearin' them.",
        )
        chatNpc(neutral, "Arrr, take this lamp I found washed ashore too.")
        chatPlayer(happy, "Wow, thanks!")
    }

    private fun ProtectedAccess.claimEasyReward(): Boolean {
        val given =
            player
                .invTransaction(inv) {
                    val into = select(inv)
                    add(into, EASY_GLOVES.asRSCM(RSCMType.OBJ), 1)
                    add(into, EASY_LAMP.asRSCM(RSCMType.OBJ), 1)
                }
                .success
        if (given) {
            vars[EASY_REWARD] = 1
        }
        return given
    }

    private companion object {
        const val JACKIE = "npc.agilityarena_tickettrader"
        const val STORE = "dbrow.agilityarena_rewards_newshop"
        const val XP_STOCK = "obj.agilityarena_xp_dummyitem"
        const val AGILITY = "stat.agility"
        const val EASY_DONE = "varbit.atjun_easy_done"
        const val EASY_REWARD = "varbit.atjun_easy_reward"
        const val EASY_GLOVES = "obj.atjun_gloves_easy"
        const val EASY_LAMP = "obj.atjun_easy_lamp"
        const val EASY_COMPLETE = 2

        val GLOVES =
            listOf("obj.atjun_gloves_med", "obj.atjun_gloves_hard", "obj.atjun_gloves_elite")
    }
}

internal const val TICKET_XP_TENTHS = 3450
internal const val GLOVES_XP_TENTHS = 3795

internal fun ticketXpTenths(tickets: Int, gloves: Boolean): Long =
    tickets.toLong() * if (gloves) GLOVES_XP_TENTHS else TICKET_XP_TENTHS
