package org.rsmod.content.areas.misc.deathsoffice

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfSubType
import jakarta.inject.Inject
import org.rsmod.api.death.PlayerDeathCleanupHook
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.content.other.gravestones.inDeathTutorial
import org.rsmod.content.other.gravestones.metDeath
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

private var Player.heardGravestones by boolVarBit("varbit.death_tutorial_heard_gravestones")
private var Player.heardFees by boolVarBit("varbit.death_tutorial_heard_fees")
private var Player.heardTimer by boolVarBit("varbit.death_tutorial_heard_timer")
private var Player.heardKeptOnDeath by boolVarBit("varbit.death_tutorial_heard_kept_on_death")

/**
 * The first death that loses items sends the player to Death's Office, where Death explains
 * graves (https://oldschool.runescape.wiki/w/Death%27s_Office; live capture, rev 240,
 * 20260811T142735-59bcb26@2019-2080). It runs after the respawn teleport, on the death's last
 * tick, as live arrives on the tick of "Oh dear, you are dead!".
 */
class DeathTutorialArrival @Inject constructor(private val offices: DeathsOffices) :
    PlayerDeathCleanupHook {
    override fun cleanup(player: Player) {
        if (!player.inDeathTutorial || player.metDeath) {
            return
        }
        if (!offices.enter(player, DeathTutorial.ARRIVAL)) {
            // Only with every instance slot taken; the tutorial waits for the next death.
            player.inDeathTutorial = false
            return
        }
        // live capture, rev 240, 20260811T142735-59bcb26@2020.
        player.metDeath = true
        // live capture, rev 240, 20260811T142735-59bcb26@2022: Death speaks three ticks later.
        player.queue(DeathTutorial.QUEUE, DeathTutorial.SPEAK_DELAY)
    }
}

internal object DeathTutorial {
    const val QUEUE = "queue.death_office_tutorial"
    const val SPEAK_DELAY = 3
    val ARRIVAL = CoordGrid(3171, 5726)

    /** Live capture, rev 240, 20260811T142735-59bcb26@2022-2038. */
    suspend fun Dialogue.introduction() {
        chatNpc(
            bored,
            "So, mortal... you have died, as all mortals do. As you have suffered the loss of some " +
                "items on this occasion, I have brought you here to explain for you how that works.",
        )
        chatPlayer(quiz, "I lost items? Can I get them back?")
        explainGravestones(again = false)
        menu()
    }

    /** Live capture, rev 241, 20261013T214438-234ad72@2-20: after logging back in. */
    suspend fun Dialogue.resume() {
        chatNpc(
            angry,
            "I believe I was explaining gravestones to you. We still have some more topics to cover.",
        )
        menu()
    }

    /** Live capture, rev 240, 20260811T142735-59bcb26@2075-2078. */
    suspend fun Dialogue.reminder() {
        chatNpc(quiz, "Would you like me to remind you about gravestones?")
        menu()
    }

    /**
     * Live capture, rev 240, 20260811T142735-59bcb26@2040-2080: options already heard are struck
     * through, and picking one plays it again.
     */
    private suspend fun Dialogue.menu() {
        while (true) {
            when (
                choice5(
                    heard(player.heardGravestones, "Tell me about gravestones again."),
                    1,
                    heard(player.heardFees, "How do I pay a gravestone fee?"),
                    2,
                    heard(player.heardTimer, "How long do I have to return to my gravestone?"),
                    3,
                    heard(player.heardKeptOnDeath, "How do I know what will happen to my items when I die?"),
                    4,
                    "I think I'm done here.",
                    5,
                )
            ) {
                1 -> {
                    chatPlayer(happy, "Tell me about gravestones again.")
                    explainGravestones(again = true)
                }
                2 -> payingFees()
                3 -> howLong()
                4 -> keptOnDeath()
                5 -> {
                    chatPlayer(bored, "I think I'm done here.")
                    // UNVERIFIED: the reply the second time (the capture walked away).
                    chatNpc(
                        happy,
                        "Very well. Once you pass through my portal, you will have 15 minutes to " +
                            "reach your gravestone.",
                    )
                    return
                }
            }
        }
    }

    private fun heard(heard: Boolean, option: String): String =
        if (heard) "<str>$option</str>" else option

    private suspend fun Dialogue.explainGravestones(again: Boolean) {
        player.heardGravestones = true
        val lead = if (again) "Gladly..." else "Let me explain..."
        chatNpc(
            if (again) happy else bored,
            "$lead Across most of the land of Gielinor, when you die, some of the items you're " +
                "carrying are dropped.",
        )
        objbox(
            GRAVE_OBJ,
            "A gravestone appears on the ground <col=9f0000>near where you <col=9f0000>died</col>. " +
                "You can go there and retrieve your items by clicking on it.",
        )
        access.graphicbox(
            GRAVE_ARROW_GRAPHIC,
            ARROW_SIZE,
            "Your gravestone would be marked with an arrow to<br>help you spot it. A grave icon " +
                "appears on the <col=9f0000>world map</col><br>too, if your grave is in a mapped area.",
        )
        doubleobjbox(
            GRAVE_OBJ,
            "obj.coins_100",
            "Gravestones charge a <col=9f0000>small fee</col> to return especially valuable items, " +
                "but most things are returned free of charge. Conveniently, a gravestone can take " +
                "its fee <col=9f0000>straight from your bank</col>.",
        )
        objbox(
            GRAVE_OBJ,
            "A gravestone does not last forever. After <col=9f0000>about 15 minutes</col>, a " +
                "gravestone will collapse. Anything in it will be sent here, to me.",
        )
        chatNpc(
            bored,
            "I will look after such items, <col=9f0000>without a time limit</col>. However, I do " +
                "charge more for my services. It would be cheaper to retrieve them from the " +
                "gravestone directly, if you can reach it in time.",
        )
    }

    /** Live capture, rev 240, 20260811T142735-59bcb26@2052-2058. */
    private suspend fun Dialogue.payingFees() {
        player.heardFees = true
        chatPlayer(quiz, "How do I pay a gravestone fee?")
        chatNpc(
            happy,
            "Cash is always acceptable. Also, a gravestone can take it directly from your bank, in " +
                "case you didn't have enough with you.",
        )
        chatNpc(
            shifty,
            "Besides that, take a look at my Coffer here. You can bring your unwanted possessions, " +
                "and sacrifice them into my Coffer. I'll count their value against any future " +
                "gravestone fees you're charged.",
        )
        objbox(
            "obj.osb7_cat_hair",
            "Death's Coffer won't accept items worth less than <col=9f0000>10,000 coins per " +
                "item</col> - Death doesn't want cheap junk! But more valuable items can be " +
                "sacrificed to pay down future reclamation fees.",
        )
    }

    /** Live capture, rev 240, 20260811T142735-59bcb26@2060-2063. */
    private suspend fun Dialogue.howLong() {
        player.heardTimer = true
        chatPlayer(quiz, "How long do I have to return to my gravestone?")
        objbox(
            GRAVE_OBJ,
            "A gravestone lasts for <col=9f0000>at least 15 minutes</col> before it collapses. " +
                "However, that timer <col=9f0000>pauses if you become <col=9f0000>inactive</col>, " +
                "in case you have lost your connection to the world.",
        )
        doubleobjbox(
            GRAVE_OBJ,
            "obj.trail_watch",
            "A timer <col=9f0000>on your head-up display</col> shows the remaining time before " +
                "your gravestone will collapse.",
        )
    }

    /** Live capture, rev 240, 20260811T142735-59bcb26@2065-2068. */
    private suspend fun Dialogue.keptOnDeath() {
        player.heardKeptOnDeath = true
        chatPlayer(quiz, "How do I know what will happen to my items when I die?")
        chatNpc(
            happy,
            "There is a menu to predict these things for you. Take a look at your Worn Items " +
                "side-panel...",
        )
        access.graphicbox(
            KEPT_ON_DEATH_GRAPHIC,
            KEPT_ON_DEATH_SIZE,
            "The <col=9f0000>Items Kept on Death</col> menu tells you roughly what<br>your items " +
                "will do when you next die, including<br><col=9f0000>estimating gravestone " +
                "fees</col>.",
        )
    }

    /**
     * The `graphicbox` chatbox (live capture, rev 240, 20260811T142735-59bcb26@2032): the game
     * message, the box with its button, the graphic and the text, then a pause button.
     */
    private suspend fun ProtectedAccess.graphicbox(graphic: Int, size: Int, text: String) {
        mes(text.replace("<br>", " "), ChatType.Mesbox)
        runClientScript(clientScript("clientscript.[clientscript,toplevel_chatbox_resetbackground]"))
        ifOpenSub("interface.graphicbox", "component.chatbox:chatmodal", IfSubType.Modal)
        runClientScript(clientScript("clientscript.[clientscript,graphicbox_setbuttons]"), "Click here to continue")
        ifSetEvents("component.graphicbox:universe", 0..1, IfEvent.PauseButton)
        runClientScript(clientScript("clientscript.[clientscript,graphicbox_set_graphic]"), graphic, size, size)
        ifSetText("component.graphicbox:text", text)
        pauseButton()
    }

    private fun clientScript(name: String): Int = name.asRSCM(RSCMType.CLIENTSCRIPT)

    private const val GRAVE_OBJ = "obj.sirens_tome"

    // The cache names no sprites for gamevals: worldmap_marker,3 and options_icons,18.
    private const val GRAVE_ARROW_GRAPHIC = 2520
    private const val ARROW_SIZE = 52
    private const val KEPT_ON_DEATH_GRAPHIC = 912
    private const val KEPT_ON_DEATH_SIZE = 34
}
