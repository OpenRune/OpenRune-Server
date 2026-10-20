package org.rsmod.content.other.treasuretrails

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import kotlin.random.Random
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

object ClueLightBox {
    const val BOX = "obj.light_puzzle_box"
    const val INTERFACE = "interface.light_puzzle"
    const val BUTTONS = 8
    private const val LIGHTS = "varp.trail_lights"
    private const val SEED = "varp.trail_lightbox_seed"
    private const val LIGHT_COUNT = 25
    private const val FLIP_PERCENT = 31
    private const val SOLVE_SYNTH = 580

    fun button(index: Int): String = "component.light_puzzle:button${index + 1}"

    fun held(player: Player): ItemServerType? =
        if (player.inv.count(BOX) > 0) TrailTiers.type(BOX.asRSCM(RSCMType.OBJ)) else null

    fun solved(player: Player): Boolean = player.vars[SEED] != 0 && player.vars[LIGHTS] == 0

    fun reset(player: Player) {
        VarPlayerIntMapSetter.set(player, SEED, 0)
        VarPlayerIntMapSetter.set(player, LIGHTS, 0)
    }

    fun masks(seed: Int): IntArray {
        val random = Random(seed)
        return IntArray(BUTTONS) {
            var mask: Int
            do {
                mask = 0
                for (light in 0 until LIGHT_COUNT) {
                    if (random.nextInt(100) < FLIP_PERCENT) mask = mask or (1 shl light)
                }
            } while (mask == 0)
            mask
        }
    }

    fun deal(random: Random): Pair<Int, Int> {
        while (true) {
            val seed = random.nextInt(1, Int.MAX_VALUE)
            val masks = masks(seed)
            val pressed = random.nextInt(1, 1 shl BUTTONS)
            val lights =
                masks.indices.fold(0) { acc, button ->
                    if (((pressed shr button) and 1) == 1) acc xor masks[button] else acc
                }
            if (lights != 0) return seed to lights
        }
    }

    fun ProtectedAccess.open() {
        if (player.vars[SEED] == 0) {
            val (seed, lights) = deal(Random.Default)
            VarPlayerIntMapSetter.set(player, SEED, seed)
            VarPlayerIntMapSetter.set(player, LIGHTS, lights)
        }
        mes("You open the light box's control panel.")
        ifOpenMainModal(INTERFACE)
        for (index in 0 until BUTTONS) {
            ifSetEvents(button(index), -1..-1, IfEvent.Op1)
        }
    }

    fun ProtectedAccess.press(index: Int) {
        if (solved(player)) return
        val seed = player.vars[SEED]
        if (seed == 0) return
        val lights = player.vars[LIGHTS] xor masks(seed)[index]
        VarPlayerIntMapSetter.set(player, LIGHTS, lights)
        if (lights == 0) {
            mes("As the last light turns on, you hear the latch release.")
            soundSynth(SOLVE_SYNTH)
            ifClose()
        }
    }
}
