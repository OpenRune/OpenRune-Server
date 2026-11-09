package org.rsmod.api.net.rsprot

import dev.openrune.util.Wearpos
import net.rsprot.protocol.game.outgoing.info.npcinfo.NpcAvatar
import org.rsmod.api.net.rsprot.ext.setFaceTarget
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcInfoProtocol
import org.rsmod.game.entity.npc.OpVisibility
import org.rsmod.game.entity.util.EntityAnimationFreeze
import org.rsmod.game.entity.util.EntityFaceTarget
import org.rsmod.game.entity.util.EntityTinting
import org.rsmod.game.entity.util.EntityTransparency
import org.rsmod.game.headbar.Headbar
import org.rsmod.game.hit.Hitmark
import org.rsmod.game.type.getInvObj

class RspNpcInfo(val rspAvatar: NpcAvatar) : NpcInfoProtocol {
    override fun setSequence(seq: Int, delay: Int) {
        rspAvatar.extendedInfo.setSequence(seq, delay)
    }

    override fun setSpotanim(spotanim: Int, delay: Int, height: Int, slot: Int) {
        setSpotanim(spotanim, delay, height, slot, loop = false)
    }

    override fun setSpotanim(spotanim: Int, delay: Int, height: Int, slot: Int, loop: Boolean) {
        rspAvatar.extendedInfo.setSpotAnim(slot, spotanim, delay, height, loop)
    }

    override fun setSay(text: String) {
        rspAvatar.extendedInfo.setSay(text)
    }

    override fun setFacePathingEntity(slot: Int) {
        rspAvatar.extendedInfo.setFaceTarget(EntityFaceTarget(slot))
    }

    override fun setFaceAngle(angle: Int, instant: Boolean) {
        rspAvatar.extendedInfo.setFaceAngle(angle, instant)
    }

    override fun setTransmog(type: Int) {
        rspAvatar.setId(type)
        rspAvatar.extendedInfo.setTransmogrification(type)
    }

    override fun resetTransmog(originalType: Int) {
        rspAvatar.setId(originalType)
        rspAvatar.extendedInfo.setTransmogrification(originalType)
    }

    override fun setBodyModel(model: Int) {
        rspAvatar.extendedInfo.setBodyCustomisation(listOf(model), emptyList(), emptyList())
    }

    override fun setBodyModels(models: List<Int>) {
        rspAvatar.extendedInfo.setBodyCustomisation(models, emptyList(), emptyList())
    }

    override fun setBodyRecolours(recolours: List<Int>) {
        rspAvatar.extendedInfo.setBodyCustomisation(emptyList(), recolours, emptyList())
    }

    override fun setBodyColour(colour: Int) {
        rspAvatar.extendedInfo.setBodyCustomisation(
            emptyList(),
            emptyList(),
            emptyList(),
            recolAll = colour,
        )
    }

    override fun resetBodyModel() {
        rspAvatar.extendedInfo.resetBodyCustomisations()
    }

    override fun mirrorPlayerAppearance() {
        rspAvatar.extendedInfo.setBodyCustomisationMirrored()
    }

    override fun setPlayerAppearance(player: Player) {
        val info = rspAvatar.extendedInfo
        val appearance = player.appearance
        info.resetBodyCustomisations()
        info.setBodyCustomisationBodyType(appearance.bodyType)
        appearance.identKitSnapshot().forEachIndexed { slot, kit ->
            info.setBodyCustomisationIdentKit(slot, kit.toInt())
        }
        appearance.coloursSnapshot().forEachIndexed { slot, colour ->
            info.setBodyCustomisationColour(slot, colour.toUByte().toInt())
        }
        for (wearpos in Wearpos.visibleWearpos) {
            val obj = player.worn[wearpos.slot] ?: continue
            val type = getInvObj(obj)
            info.setBodyCustomisationWornObj(wearpos.slot, obj.id, type.wearpos2, type.wearpos3)
        }
    }

    override fun setTransparency(effect: EntityTransparency) {
        rspAvatar.extendedInfo.setTransparency(
            effect.startTime, effect.endTime, effect.startTransparency, effect.endTransparency,
            effect.useStartTransparency,
        )
    }

    override fun setAnimationFreeze(effect: EntityAnimationFreeze) {
        rspAvatar.extendedInfo.setFreeze(effect.delay, effect.duration, effect.cancelSequence)
    }

    override fun showHeadbar(headbar: Headbar) {
        rspAvatar.extendedInfo.addHeadBar(
            sourceIndex = if (headbar.isNoSource) -1 else headbar.sourceSlot,
            sourceType = headbar.self,
            otherType = if (headbar.isPrivate) -1 else headbar.public,
            startFill = headbar.startFill,
            endFill = headbar.endFill,
            startTime = headbar.startTime,
            endTime = headbar.endTime,
        )
    }

    override fun removeHeadbar(id: Int) {
        rspAvatar.extendedInfo.removeHeadBar(id)
    }

    override fun showHitmark(hitmark: Hitmark) {
        rspAvatar.extendedInfo.addHitMark(
            sourceIndex = if (hitmark.isNoSource) -1 else hitmark.sourceSlot,
            sourceType = hitmark.source,
            otherType = if (hitmark.isPrivate) -1 else hitmark.public,
            value = hitmark.damage,
            delay = hitmark.delay,
        )
    }

    override fun toggleOps(ops: OpVisibility) {
        rspAvatar.extendedInfo.setVisibleOps(ops.packed.toByte())
    }

    override fun setHeadIcon(slot: Int, graphic: Int, index: Int) {
        rspAvatar.extendedInfo.setHeadIconChange(slot, graphic, index)
    }

    override fun setTinting(tinting: EntityTinting) {
        rspAvatar.extendedInfo.setTinting(
            startTime = tinting.startCycle,
            endTime = tinting.endCycle,
            hue = tinting.hue and 0xFF,
            saturation = tinting.saturation and 0xFF,
            lightness = tinting.lightness and 0xFF,
            weight = tinting.weight and 0xFF,
        )
    }

    override fun exactMove(
        deltaX1: Int,
        deltaZ1: Int,
        deltaX2: Int,
        deltaZ2: Int,
        delay1: Int,
        delay2: Int,
        direction: Int,
    ) {
        rspAvatar.extendedInfo.setExactMove(
            deltaX1 = deltaX1,
            deltaZ1 = deltaZ1,
            delay1 = delay1,
            deltaX2 = deltaX2,
            deltaZ2 = deltaZ2,
            delay2 = delay2,
            angle = direction,
        )
    }

    override fun crawl(deltaX: Int, deltaZ: Int) {
        rspAvatar.crawl(deltaX, deltaZ)
    }

    override fun walk(deltaX: Int, deltaZ: Int) {
        rspAvatar.walk(deltaX, deltaZ)
    }

    override fun run(deltaX1: Int, deltaZ1: Int, deltaX2: Int, deltaZ2: Int) {
        rspAvatar.walk(deltaX1, deltaZ1)
        rspAvatar.walk(deltaX2, deltaZ2)
    }

    override fun teleport(x: Int, z: Int, level: Int, jump: Boolean) {
        rspAvatar.teleport(level, x, z, jump)
    }

    override fun disable() {
        rspAvatar.setInaccessible(true)
    }

    override fun hide() {
        rspAvatar.setInaccessible(true)
    }

    override fun reveal() {
        rspAvatar.setInaccessible(false)
    }

    override fun isActive(): Boolean {
        return rspAvatar.isActive()
    }
}
