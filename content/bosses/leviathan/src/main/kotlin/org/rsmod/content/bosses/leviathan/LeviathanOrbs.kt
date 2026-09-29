package org.rsmod.content.bosses.leviathan

import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.ImpactRounding
import org.rsmod.api.bosses.spec.ProjectileConfig

internal object LeviathanOrbs {
    const val SOURCE_OFFSET = 2.5

    private const val START_SEQ = "seq.npc_leviathan_01_projectile_start_01"
    private const val LOOP_SEQ = "seq.npc_leviathan_01_projectile_loop_01"
    private const val SINGLE_SEQ = "seq.npc_leviathan_01_projectile_single_01"
    private const val START_HEIGHT = 548
    private const val END_HEIGHT = 100
    private const val CURVE = 30
    private const val PROGRESS = 124
    private const val IMPACT_HEIGHT = 100

    private const val ENRAGED_DELAY = 30
    private const val ENRAGED_TRAVEL = 120
    private const val ENRAGED_PENETRATION = 25
    val VOLLEY_STAGES =
        listOf(
            VolleyStage(interval = 3, shots = 6, allStyles = false, orbDelay = 30, orbTravel = 120),
            VolleyStage(interval = 2, shots = 8, allStyles = false, orbDelay = 30, orbTravel = 120),
            VolleyStage(interval = 2, shots = 8, allStyles = true, orbDelay = 30, orbTravel = 90),
            VolleyStage(interval = 1, shots = 12, allStyles = false, orbDelay = 30, orbTravel = 60),
            VolleyStage(interval = 1, shots = 10, allStyles = true, orbDelay = 30, orbTravel = 60),
            VolleyStage(interval = 1, shots = 10, allStyles = true, orbDelay = 30, orbTravel = 60),
            VolleyStage(interval = 1, shots = 12, allStyles = true, orbDelay = 30, orbTravel = 60),
        )

    fun volleyStage(stage: VolleyStage, rockfall: Effect, recovery: Int): Effect {
        val chained = stage.interval == 1
        return sequence(
            faceTarget(),
            addVarn(LeviathanVarns.VOLLEYS, 1),
            shot(stage, follow = false),
            repeat(stage.shots - 1, effect = shot(stage, follow = chained)),
            rockfall,
            addVarn(LeviathanVarns.VOLLEY_STAGE, 1, max = VOLLEY_STAGES.lastIndex),
            wait(recovery),
        )
    }

    fun enragedOrb(penetrationWhen: Condition): Effect =
        sequence(
            faceTarget(),
            oneOf(OrbStyle.DISTANCED.map { orb(it, ENRAGED_DELAY, ENRAGED_TRAVEL, follow = false, penetrationWhen) }),
            wait(1),
            anim(SINGLE_SEQ),
        )

    private fun shot(stage: VolleyStage, follow: Boolean): Effect {
        val styles = if (stage.allStyles) OrbStyle.ALL else OrbStyle.DISTANCED
        val fire = oneOf(styles.map { orb(it, stage.orbDelay, stage.orbTravel, follow) })
        return if (stage.interval > 1) {
            sequence(fire, wait(1), anim(SINGLE_SEQ), wait(stage.interval - 1))
        } else {
            sequence(fire, wait(1))
        }
    }

    private fun orb(
        style: OrbStyle,
        delay: Int,
        travel: Int,
        follow: Boolean,
        penetrationWhen: Condition? = null,
    ): Effect =
        sequence(
            anim(if (follow) LOOP_SEQ else START_SEQ),
            spotanim(if (follow) style.followSpotanim else style.launchSpotanim),
            projectile(
                spotanim = style.projectile,
                from = toward(Centre, CurrentTarget, SOURCE_OFFSET),
                config = ProjectileConfig.fixed(START_HEIGHT, END_HEIGHT, delay, travel, CURVE, PROGRESS),
                resolveOnImpact = true,
                impactRounding = ImpactRounding.Up,
                hit =
                    hit {
                        damage(0..style.maxHit).roll()
                        type(style.type)
                        spotanim(style.impactSpotanim, height = IMPACT_HEIGHT, unlessPraying = true)
                        if (penetrationWhen != null) penetration(ENRAGED_PENETRATION, penetrationWhen)
                    },
            ),
            soundTo(style.synth),
        )
}
