package org.rsmod.content.raids.toa.raid.encounter

import org.rsmod.game.entity.Npc

internal class ToaFight(
    val boss: () -> Npc?,
    val combatants: List<ToaCombatant.Spec>,
    val music: String? = null,
    val firstAttackDelay: Int = 0,
    val attackRate: (() -> Int)? = null,
    val death: ToaDeath? = null,
    val bar: () -> Npc? = boss,
)

internal class ToaDeath(
    val anim: String,
    val dead: String,
    val modelDelay: Int,
    val shake: Shake? = null,
    val parts: List<Part> = emptyList(),
) {
    class Part(val npc: () -> Npc?, val anim: String, val dead: String)

    class Shake(val delay: Int, val leftRight: Int, val upDown: Int, val forwards: Int)
}
