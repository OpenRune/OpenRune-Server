package org.rsmod.content.quest.mudfoot

import dev.openrune.types.MesAnimType
import org.rsmod.api.player.dialogue.Dialogue

internal const val MUDFOOT_TITLE = "Mudfoot"
internal const val MUDFOOT_VIS = "npc.mba_mudfoot_vis"
internal const val MUDFOOT_SPEAR_VIS = "npc.mba_mudfoot_spear_vis"
internal const val HARLAN_TITLE = "Captain Harlan"
internal const val HARLAN = "npc.captain_harlan"
internal const val HARLAN_CUTSCENE = "npc.captain_harlan_cutscene"
internal const val OUTLAW_TITLE = "Outlaw"

internal val Dialogue.disgruntled: MesAnimType
    get() = mesanim("mesanim.mba_disgruntled")

internal val Dialogue.suspecting: MesAnimType
    get() = mesanim("mesanim.mba_suspecting")

internal val Dialogue.confusedIdle: MesAnimType
    get() = mesanim("mesanim.mba_confused_idle")

/** Mudfoot speaking without being the npc the dialogue is bound to. */
internal suspend fun Dialogue.mudfoot(anim: MesAnimType, text: String, spear: Boolean = false) {
    chatNpcSpecific(MUDFOOT_TITLE, if (spear) MUDFOOT_SPEAR_VIS else MUDFOOT_VIS, anim, text)
}

internal suspend fun Dialogue.harlan(anim: MesAnimType, text: String) {
    chatNpcSpecific(HARLAN_TITLE, HARLAN, anim, text)
}

/**
 * A chatbox options menu; returns the picked 1-based index. Past five entries the menu pages with
 * "More options..." like Jagex's own long menus.
 */
internal suspend fun Dialogue.chatMenu(options: List<String>, title: String = SELECT_AN_OPTION): Int {
    if (options.size > MAX_MENU_OPTIONS) {
        var first = 0
        while (true) {
            val page = options.subList(first, minOf(first + PAGE_OPTIONS, options.size))
            val more = first + PAGE_OPTIONS < options.size
            val shown = if (more) page + MORE_OPTIONS else page + FIRST_OPTIONS
            val picked = chatMenu(shown, title)
            if (picked <= page.size) {
                return first + picked
            }
            first = if (more) first + PAGE_OPTIONS else 0
        }
    }
    val o = options
    return when (o.size) {
        2 -> choice2(o[0], 1, o[1], 2, title = title)
        3 -> choice3(o[0], 1, o[1], 2, o[2], 3, title = title)
        4 -> choice4(o[0], 1, o[1], 2, o[2], 3, o[3], 4, title = title)
        5 -> choice5(o[0], 1, o[1], 2, o[2], 3, o[3], 4, o[4], 5, title = title)
        else -> error("A chat menu needs two to five options, not ${o.size}.")
    }
}

internal const val SELECT_AN_OPTION = "Select an option"
private const val MAX_MENU_OPTIONS = 5
private const val PAGE_OPTIONS = 4
private const val MORE_OPTIONS = "More options..."
private const val FIRST_OPTIONS = "Previous options..."
