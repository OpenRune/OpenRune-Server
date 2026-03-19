package org.rsmod.api.combat.commons.fx

import dev.openrune.types.SequenceServerType
import dev.openrune.types.aconverted.SynthType
import org.rsmod.api.combat.commons.CombatStance
import org.rsmod.api.config.aliases.ParamSeq
import org.rsmod.api.config.aliases.ParamSynth
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.synths

public data class MeleeAnimationAndSound(
    val animParam: ParamSeq,
    val soundParam: ParamSynth,
    val defaultAnim: SequenceServerType,
    val defaultSound: SynthType,
) {
    public companion object {
        private val stance1 =
            MeleeAnimationAndSound(
                animParam = params.attack_anim_stance1,
                soundParam = params.attack_sound_stance1,
                defaultAnim = seqs.human_unarmedpunch,
                defaultSound = synths.human_unarmedpunch,
            )

        private val stance2 =
            MeleeAnimationAndSound(
                animParam = params.attack_anim_stance2,
                soundParam = params.attack_sound_stance2,
                defaultAnim = seqs.human_unarmedkick,
                defaultSound = synths.human_unarmedkick,
            )

        private val stance3 =
            MeleeAnimationAndSound(
                animParam = params.attack_anim_stance3,
                soundParam = params.attack_sound_stance3,
                defaultAnim = seqs.human_unarmedpunch,
                defaultSound = synths.human_unarmedpunch,
            )

        private val stance4 =
            MeleeAnimationAndSound(
                animParam = params.attack_anim_stance4,
                soundParam = params.attack_sound_stance4,
                defaultAnim = seqs.human_unarmedpunch,
                defaultSound = synths.human_unarmedpunch,
            )

        public fun from(stance: CombatStance?): MeleeAnimationAndSound =
            when (stance) {
                CombatStance.Stance1 -> stance1
                CombatStance.Stance2 -> stance2
                CombatStance.Stance3 -> stance3
                CombatStance.Stance4 -> stance4
                else -> stance1
            }
    }
}
