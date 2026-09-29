package org.rsmod.content.bosses.barrows

import org.rsmod.api.player.output.MiscOutput
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

internal val SKELETONS =
    listOf(
        "npc.barrows_skeleton_unarmed",
        "npc.barrows_skeleton_unarmed2",
        "npc.barrows_skeleton_armed",
        "npc.barrows_skeleton_armed2",
    )

internal const val BLOODWORM = "npc.barrows_bloodworm"
internal const val CRYPT_RAT = "npc.barrows_rat"

private val CRYPT_MONSTERS =
    SKELETONS +
        listOf(
            BLOODWORM,
            CRYPT_RAT,
            "npc.barrows_giantrat",
            "npc.barrows_giantrat2",
            "npc.barrows_giantrat3",
            "npc.barrows_spider",
            "npc.barrows_giantspider",
        )

internal fun Npc.brother(): BarrowsBrother? = BarrowsBrother.entries.firstOrNull { type.isType(it.npc) }

internal fun Npc.isBrother(): Boolean = brother() != null

internal fun Npc.isCryptMonster(): Boolean = CRYPT_MONSTERS.any { type.isType(it) }

internal fun Npc.isBarrowsNpc(): Boolean = isBrother() || isCryptMonster()

internal fun Player.hintArrow(npc: Npc) {
    MiscOutput.hintArrowNpc(this, npc.slotId)
}

internal fun Player.clearHintArrow() {
    MiscOutput.hintArrowReset(this)
}
