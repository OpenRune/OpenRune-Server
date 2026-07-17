package org.rsmod.content.skills.runecrafting.essence

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid

private val RUNE_ESSENCE_MINE = CoordGrid(2912, 4838)

suspend fun Dialogue.teleportToRuneEssenceMine() {
    chatNpc(happy, "Senventior disthine molenko!")
    access.spotanim("spotanim.curse_impact", height = 92)
    delay(2)
    access.telejump(RUNE_ESSENCE_MINE)
}

suspend fun ProtectedAccess.teleportToRuneEssenceMine(npc: Npc) {
    startDialogue(npc) { teleportToRuneEssenceMine() }
}
