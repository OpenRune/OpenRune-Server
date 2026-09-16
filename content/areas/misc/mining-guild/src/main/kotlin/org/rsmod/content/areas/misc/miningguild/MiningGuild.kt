package org.rsmod.content.areas.misc.miningguild

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.miningLvl

internal object MiningGuild {
    const val ENTRY_LEVEL = 60
    const val DWARF_TITLE = "Dwarf"

    fun ProtectedAccess.meetsEntryLevel(): Boolean = player.miningLvl >= ENTRY_LEVEL

    suspend fun ProtectedAccess.denyEntry(dwarf: String) {
        startDialogue {
            chatNpcSpecific(DWARF_TITLE, dwarf, neutral, "Sorry, but you're not experienced enough to go in there.")
            mesbox("You need a Mining level of $ENTRY_LEVEL to access the Mining Guild.")
        }
    }
}
