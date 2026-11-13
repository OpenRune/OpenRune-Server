package org.rsmod.content.skills.agility

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onOpNpc5
import org.rsmod.api.shops.Shops
import org.rsmod.api.table.agility.AgilityLapListRow
import org.rsmod.content.skills.agility.LapCounter.lapCounterOff
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GraceScript @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(GRACE) { startDialogue(it.npc) { talk() } }
        onOpNpc3(GRACE) { openShop() }
        onOpNpc4(GRACE) { viewLaps() }
        onOpNpc5(GRACE) { startDialogue(it.npc) { toggleCounter() } }
    }

    private suspend fun Dialogue.talk() {
        chatNpc(quiz, "What can I do for you?")
        val topic =
            choice5(
                PITCH,
                Topic.Pitch,
                SHOP,
                Topic.Shop,
                LAPS,
                Topic.Laps,
                TOGGLE,
                Topic.Toggle,
                LEAVE,
                Topic.Leave,
                title = MENU_TITLE,
            )
        when (topic) {
            Topic.Pitch -> pitch()
            Topic.Shop -> shop()
            Topic.Laps -> {
                chatPlayer(quiz, LAPS)
                access.viewLaps()
            }
            Topic.Toggle -> {
                chatPlayer(quiz, TOGGLE)
                toggleCounter()
            }
            Topic.Leave -> chatPlayer(bored, LEAVE)
        }
    }

    private suspend fun Dialogue.pitch() {
        chatPlayer(quiz, PITCH)
        chatNpc(
            laugh,
            "A good question indeed! I'm selling special clothing for Agility enthusiasts.",
        )
        chatNpc(
            neutral,
            "Sometimes, when you're exploring the hidden rooftops of our cities, you'll find one " +
                "of my Marks.",
        )
        chatNpc(
            happy,
            "Once you've got enough Marks, I'll exchange them for my Graceful clothing. So, does " +
                "that interest you?",
        )
        // UNVERIFIED: the player lines after this menu; no capture answered it.
        when (choice2(SHOP, Topic.Shop, LEAVE, Topic.Leave, title = MENU_TITLE)) {
            Topic.Shop -> shop()
            else -> chatPlayer(bored, LEAVE)
        }
    }

    private suspend fun Dialogue.shop() {
        chatPlayer(quiz, SHOP)
        access.openShop()
    }

    private suspend fun Dialogue.toggleCounter() {
        val off = !player.lapCounterOff
        player.lapCounterOff = off
        if (off) {
            chatNpc(
                happy,
                "I've disabled your lap counter messages, if you want to turn it back on, so you " +
                    "can see your total laps after you finish one, speak to me again.",
            )
        } else {
            chatNpc(
                happy,
                "I've turned on your lap counter, you'll get a message when you complete a lap " +
                    "on a course.<br>To help you keep track.",
            )
        }
    }

    private fun ProtectedAccess.openShop() {
        shops.open(
            player = player,
            title = TITLE,
            shopInv = SHOP_INV,
            buyPercentage = BUY_PERCENTAGE,
            sellPercentage = SELL_PERCENTAGE,
            changePercentage = 0.0,
            currency = MARKS_OF_GRACE,
        )
    }

    private fun ProtectedAccess.viewLaps() {
        val sections =
            SECTIONS.map { row ->
                val section = AgilityLapListRow.getRow(row)
                val courses =
                    section.courses.joinToString("") { course ->
                        val laps = vars[RSCM.getReverseMapping(RSCMType.VARP, course.t1)]
                        "${course.t0}$laps Laps<br>"
                    }
                "${section.title}<br><br>$courses"
            }
        ifOpenMainModal(LAP_SCROLL)
        val setup = SCROLL_SETUP.asRSCM(RSCMType.CLIENTSCRIPT)
        runClientScript(setup, SCROLL_TITLE, sections.joinToString("<br>"))
    }

    private enum class Topic {
        Pitch,
        Shop,
        Laps,
        Toggle,
        Leave,
    }

    private companion object {
        const val GRACE = "npc.rooftops_grace"
        const val TITLE = "Grace's Graceful Clothing"
        const val SHOP_INV = "inv.skill_guide_agility_courses"
        const val MARKS_OF_GRACE = "currency.marks_of_grace"
        const val BUY_PERCENTAGE = 80.0
        const val SELL_PERCENTAGE = 100.0
        const val LAP_SCROLL = "interface.longscroll"
        const val SCROLL_SETUP = "clientscript.[clientscript,longscroll_setup]"
        const val SCROLL_TITLE = "Agility Course Lap Counts"
        const val MENU_TITLE = "What would you like to say?"
        const val PITCH = "I don't know. What can you do for me?"
        const val SHOP = "Can I see what you're selling?"
        const val LAPS = "Can I see how many laps I've completed?"
        const val TOGGLE = "Can I toggle my lap counter?"
        const val LEAVE = "I'm alright, thanks."
        val SECTIONS = listOf("dbrow.agility_lap_list_rooftops", "dbrow.agility_lap_list_misc")
    }
}
