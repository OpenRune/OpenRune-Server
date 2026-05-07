package org.rsmod.api.player.hit.processor

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.SynthType
import kotlin.math.min
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.BaseParams
import org.rsmod.api.player.events.PlayerHitpointsChangedEvent
import org.rsmod.api.player.headbar.InternalPlayerHeadbars
import org.rsmod.api.player.lefthand
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseHitpointsLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.torso
import org.rsmod.api.random.GameRandom
import org.rsmod.game.headbar.Headbar
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitType

public object StandardPlayerHitProcessor : QueuedPlayerHitProcessor {
    private val hitSoundsBodyA =
        listOf("synth.human_hit_1", "synth.human_hit_2", "synth.human_hit_3", "synth.human_hit_4").map {
            SynthType(it.asRSCM(RSCMType.SYNTH))
        }

    private val hitSoundsBodyB = listOf("synth.female_hit_1", "synth.female_hit_2").map {
        SynthType(it.asRSCM(RSCMType.SYNTH))
    }

    override fun ProtectedAccess.process(hit: Hit) {
        if (!hit.isValid(this)) {
            return
        }
        preventLogout("You can't log out until 10 seconds after the end of combat.", 16)

        // TODO(combat): Process degradation, ring of recoil, retribution, etc.

        val oldHitpoints = player.hitpoints
        val damage = min(oldHitpoints, hit.damage)
        if (damage > 0) {
            statSub("stat.hitpoints", constant = damage, percent = 0)
            publishPlayerHitpointsChangedEvent(oldHitpoints, hit)
        }

        playDefendSound(hit, random)

        val queueDeath = player.hitpoints == 0 && "queue.death" !in player.queueList
        if (queueDeath) {
            queueDeath()
        }

        player.showHitmark(hit.hitmark)

        val headbar = hit.createHeadbar(player.hitpoints, player.baseHitpointsLvl)
        player.showHeadbar(headbar)
    }

    private fun ProtectedAccess.publishPlayerHitpointsChangedEvent(oldHitpoints: Int, hit: Hit) {
        val event =
            PlayerHitpointsChangedEvent(
                player = player,
                oldHitpoints = oldHitpoints,
                newHitpoints = player.hitpoints,
                maxHitpoints = player.baseHitpointsLvl,
                hit = hit,
            )
        publish(event)
    }

    private fun Hit.isValid(access: ProtectedAccess): Boolean {
        // Currently, we only have evidence of this validation being applied to hits dealt by npcs.
        if (!isFromNpc) {
            return true
        }
        // Only melee-based hits can be invalidated here.
        if (type != HitType.Melee) {
            return true
        }
        // If the npc that dealt the hit can no longer be found, the hit is invalidated. This can
        // occur when the npc's internal `uid` is reassigned (e.g., due to transmogrification).
        val npc = access.findHitNpcSource(this) ?: return false
        return npc.hitpoints > 0
    }

    private fun ProtectedAccess.playDefendSound(hit: Hit, random: GameRandom) {
        val lefthandType = player.lefthand?.let(::ocType)
        val torsoType = player.torso?.let(::ocType)
        val bodyType = player.appearance.bodyType

        val defendSound = resolveDefendSound(lefthandType, torsoType, hit.damage, bodyType, random)
        soundSynth(defendSound, delay = 20)

        val playerSource = if (hit.isFromPlayer) findHitPlayerSource(hit) else null
        playerSource?.soundSynth(defendSound)
    }

    private fun resolveDefendSound(
        lefthand: ItemServerType?,
        torso: ItemServerType?,
        damage: Int,
        bodyType: Int,
        random: GameRandom,
    ): SynthType =
        when {
            damage == 0 -> resolveBlockSound(lefthand, torso, random)
            bodyType == constants.bodytype_a -> random.pick(hitSoundsBodyA)
            bodyType == constants.bodytype_b -> random.pick(hitSoundsBodyB)
            else -> throw NotImplementedError("Sound for body type is not implemented: $bodyType")
        }

    private fun resolveBlockSound(
        lefthand: ItemServerType?,
        torso: ItemServerType?,
        random: GameRandom,
    ): SynthType {
        val lefthandSound = lefthand?.randomBlockSound(random)
        if (lefthandSound != null) {
            return lefthandSound
        }

        val torsoSound = torso?.randomBlockSound(random)
        if (torsoSound != null) {
            return torsoSound
        }

        return SynthType("synth.human_block_1".asRSCM(RSCMType.SYNTH))
    }

    private fun ItemServerType.randomBlockSound(random: GameRandom): SynthType? {
        val sounds =
            listOfNotNull(
                paramOrNull(BaseParams.item_block_sound1),
                paramOrNull(BaseParams.item_block_sound2),
                paramOrNull(BaseParams.item_block_sound3),
                paramOrNull(BaseParams.item_block_sound4),
                paramOrNull(BaseParams.item_block_sound5),
            )
        return random.pickOrNull(sounds)
    }

    private fun Hit.createHeadbar(currHp: Int, maxHp: Int): Headbar =
        InternalPlayerHeadbars.createFromHitmark(hitmark, currHp, maxHp, "headbar.health_30")
}
