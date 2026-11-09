package org.rsmod.game.entity.npc

import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.EntityAnimationFreeze
import org.rsmod.game.entity.util.EntityTinting
import org.rsmod.game.entity.util.EntityTransparency
import org.rsmod.game.headbar.Headbar
import org.rsmod.game.hit.Hitmark

public interface NpcInfoProtocol {
    public fun setSequence(seq: Int, delay: Int)

    public fun setSpotanim(spotanim: Int, delay: Int, height: Int, slot: Int)

    /**
     * Starts or replaces a spot animation in the NPC's visual [slot].
     *
     * Set [loop] to keep playing the animation when its sequence supports looping.
     */
    public fun setSpotanim(spotanim: Int, delay: Int, height: Int, slot: Int, loop: Boolean) {
        setSpotanim(spotanim, delay, height, slot)
    }

    public fun setSay(text: String)

    public fun setFacePathingEntity(slot: Int)

    public fun setFaceAngle(angle: Int, instant: Boolean)

    public fun setTransmog(type: Int)

    public fun resetTransmog(originalType: Int)

    public fun setBodyModel(model: Int)

    public fun setBodyModels(models: List<Int>) {}

    public fun setBodyRecolours(recolours: List<Int>)

    /**
     * Applies [colour] to every triangle in the NPC's body model.
     *
     * Use [setBodyRecolours] when replacing individual cache-defined colours.
     */
    public fun setBodyColour(colour: Int) {}

    public fun resetBodyModel()

    public fun showHeadbar(headbar: Headbar)

    public fun removeHeadbar(id: Int)

    public fun showHitmark(hitmark: Hitmark)

    public fun toggleOps(ops: OpVisibility)

    public fun setHeadIcon(slot: Int, graphic: Int, index: Int)

    public fun setTinting(tinting: EntityTinting)

    /**
     * Sends a transparency transition for the NPC's model to clients that can see it.
     *
     * [effect] supplies the start and end times in client cycles and the transparency endpoints.
     */
    public fun setTransparency(effect: EntityTransparency) {}

    /**
     * Pauses the NPC's displayed animation for the interval described by [effect].
     *
     * Gameplay movement and timers continue independently of this visual effect.
     */
    public fun setAnimationFreeze(effect: EntityAnimationFreeze) {}

    /**
     * Displays the NPC with the local player's body and worn equipment on each observing client.
     *
     * Observers can see different appearances. Use [setPlayerAppearance] to show the same
     * player's appearance to everyone.
     */
    public fun mirrorPlayerAppearance() {}

    /**
     * Displays a snapshot of [player]'s body type, ident kits, colours, and visible equipment
     * on the NPC for all observers.
     *
     * Call again to reflect subsequent equipment or appearance changes. Use [resetBodyModel]
     * to restore the NPC's own model.
     */
    public fun setPlayerAppearance(player: Player) {}

    public fun exactMove(
        deltaX1: Int,
        deltaZ1: Int,
        deltaX2: Int,
        deltaZ2: Int,
        delay1: Int,
        delay2: Int,
        direction: Int,
    )

    public fun crawl(deltaX: Int, deltaZ: Int)

    public fun walk(deltaX: Int, deltaZ: Int)

    public fun run(deltaX1: Int, deltaZ1: Int, deltaX2: Int, deltaZ2: Int)

    public fun teleport(x: Int, z: Int, level: Int, jump: Boolean)

    public fun disable()

    public fun hide()

    public fun reveal()

    public fun isActive(): Boolean
}

public data object NoopNpcInfo : NpcInfoProtocol {
    override fun setSequence(seq: Int, delay: Int) {}

    override fun setSpotanim(spotanim: Int, delay: Int, height: Int, slot: Int) {}

    override fun setSay(text: String) {}

    override fun setFacePathingEntity(slot: Int) {}

    override fun setFaceAngle(angle: Int, instant: Boolean) {}

    override fun setTransmog(type: Int) {}

    override fun resetTransmog(originalType: Int) {}

    override fun setBodyModel(model: Int) {}

    override fun setBodyRecolours(recolours: List<Int>) {}

    override fun resetBodyModel() {}

    override fun showHeadbar(headbar: Headbar) {}

    override fun removeHeadbar(id: Int) {}

    override fun showHitmark(hitmark: Hitmark) {}

    override fun toggleOps(ops: OpVisibility) {}

    override fun setHeadIcon(slot: Int, graphic: Int, index: Int) {}

    override fun setTinting(tinting: EntityTinting) {}

    override fun exactMove(
        deltaX1: Int,
        deltaZ1: Int,
        deltaX2: Int,
        deltaZ2: Int,
        delay1: Int,
        delay2: Int,
        direction: Int,
    ) {}

    override fun crawl(deltaX: Int, deltaZ: Int) {}

    override fun walk(deltaX: Int, deltaZ: Int) {}

    override fun run(deltaX1: Int, deltaZ1: Int, deltaX2: Int, deltaZ2: Int) {}

    override fun teleport(x: Int, z: Int, level: Int, jump: Boolean) {}

    override fun disable() {}

    override fun hide() {}

    override fun reveal() {}

    override fun isActive(): Boolean = false
}
