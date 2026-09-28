package org.rsmod.content.skills.magic.spell.attacks.ancient

internal enum class Element {
    Smoke,
    Shadow,
    Blood,
    Ice,
}

internal enum class Tier(val anim: String, val multiTarget: Boolean) {
    Rush("seq.zaros_casting_walkmerge", multiTarget = false),
    Burst("seq.zaros_vertical_casting_walkmerge", multiTarget = true),
    Blitz("seq.zaros_casting_walkmerge", multiTarget = false),
    Barrage("seq.zaros_vertical_casting_walkmerge", multiTarget = true),
}

internal sealed class Travel {
    data object None : Travel()

    data class FromCaster(val spotanim: String, val endHeight: Int) : Travel()

    data class FromTarget(val spotanim: String, val endHeight: Int) : Travel()
}

internal enum class AncientSpell(
    val obj: String,
    val element: Element,
    val tier: Tier,
    val maxHit: Int,
    val travel: Travel,
    val impact: String,
    val impactHeight: Int,
    val hitSound: String,
    val launch: String? = null,
    val effectStrength: Int,
) {
    SmokeRush(
        obj = "obj.50_smoke_rush",
        element = Element.Smoke,
        tier = Tier.Rush,
        maxHit = 13,
        travel = Travel.FromCaster("spotanim.smoke_rush_travel", endHeight = 124),
        impact = "spotanim.smoke_rush_impact",
        impactHeight = 124,
        hitSound = "synth.smoke_rush_impact",
        effectStrength = 10,
    ),
    ShadowRush(
        obj = "obj.52_shadow_rush",
        element = Element.Shadow,
        tier = Tier.Rush,
        maxHit = 14,
        travel = Travel.FromCaster("spotanim.shadow_rush_travel", endHeight = 0),
        impact = "spotanim.shadow_rush_impact",
        impactHeight = 0,
        hitSound = "synth.shadow_rush_impact",
        effectStrength = 10,
    ),
    BloodRush(
        obj = "obj.56_blood_rush",
        element = Element.Blood,
        tier = Tier.Rush,
        maxHit = 15,
        travel = Travel.None,
        impact = "spotanim.blood_rush_impact",
        impactHeight = 0,
        hitSound = "synth.blood_rush_impact",
        effectStrength = 25,
    ),
    IceRush(
        obj = "obj.58_ice_rush",
        element = Element.Ice,
        tier = Tier.Rush,
        maxHit = 16,
        travel = Travel.FromCaster("spotanim.ice_rush_travel", endHeight = 0),
        impact = "spotanim.ice_rush_impact",
        impactHeight = 0,
        hitSound = "synth.ice_rush_impact",
        effectStrength = 8,
    ),
    SmokeBurst(
        obj = "obj.62_smoke_burst",
        element = Element.Smoke,
        tier = Tier.Burst,
        maxHit = 17,
        travel = Travel.FromTarget("spotanim.smoke_burst_travel", endHeight = 124),
        impact = "spotanim.smoke_burst_impact",
        impactHeight = 124,
        hitSound = "synth.smoke_burst_impact",
        effectStrength = 10,
    ),
    ShadowBurst(
        obj = "obj.64_shadow_burst",
        element = Element.Shadow,
        tier = Tier.Burst,
        maxHit = 18,
        travel = Travel.None,
        impact = "spotanim.shadow_burst_impact",
        impactHeight = 0,
        hitSound = "synth.shadow_burst_impact",
        effectStrength = 10,
    ),
    BloodBurst(
        obj = "obj.68_blood_burst",
        element = Element.Blood,
        tier = Tier.Burst,
        maxHit = 21,
        travel = Travel.None,
        impact = "spotanim.spell_blood_burst_impact",
        impactHeight = 0,
        hitSound = "synth.blood_burst_impact",
        effectStrength = 25,
    ),
    IceBurst(
        obj = "obj.70_ice_burst",
        element = Element.Ice,
        tier = Tier.Burst,
        maxHit = 22,
        // Cache names for the ice burst/blitz spotanims are swapped relative to their use.
        travel = Travel.FromTarget("spotanim.ice_burst_travel", endHeight = 0),
        impact = "spotanim.ice_blitz_impact",
        impactHeight = 0,
        hitSound = "synth.ice_burst_impact",
        effectStrength = 16,
    ),
    SmokeBlitz(
        obj = "obj.74_smoke_blitz",
        element = Element.Smoke,
        tier = Tier.Blitz,
        maxHit = 23,
        travel = Travel.FromCaster("spotanim.smoke_blitz_travel", endHeight = 124),
        impact = "spotanim.smoke_blitz_impact",
        impactHeight = 124,
        hitSound = "synth.smoke_blitz_impact",
        effectStrength = 20,
    ),
    ShadowBlitz(
        obj = "obj.76_shadow_blitz",
        element = Element.Shadow,
        tier = Tier.Blitz,
        maxHit = 24,
        travel = Travel.FromCaster("spotanim.shadow_blitz_travel", endHeight = 0),
        impact = "spotanim.shadow_blitz_impact",
        impactHeight = 0,
        hitSound = "synth.shadow_blitz_impact",
        effectStrength = 15,
    ),
    BloodBlitz(
        obj = "obj.80_blood_blitz",
        element = Element.Blood,
        tier = Tier.Blitz,
        maxHit = 25,
        travel = Travel.FromCaster("spotanim.blood_blitz_travel", endHeight = 0),
        impact = "spotanim.blood_blitz_impact",
        impactHeight = 0,
        hitSound = "synth.blood_blitz_impact",
        effectStrength = 25,
    ),
    IceBlitz(
        obj = "obj.82_ice_blitz",
        element = Element.Ice,
        tier = Tier.Blitz,
        maxHit = 26,
        travel = Travel.None,
        impact = "spotanim.ice_burst_impact",
        impactHeight = 0,
        hitSound = "synth.ice_blitz_impact",
        launch = "spotanim.ice_burst_travel",
        effectStrength = 24,
    ),
    SmokeBarrage(
        obj = "obj.86_smoke_barrage",
        element = Element.Smoke,
        tier = Tier.Barrage,
        maxHit = 27,
        travel = Travel.FromTarget("spotanim.smoke_barrage_travel", endHeight = 124),
        impact = "spotanim.smoke_barrage_impact",
        impactHeight = 124,
        hitSound = "synth.smoke_barrage_impact",
        effectStrength = 20,
    ),
    ShadowBarrage(
        obj = "obj.88_shadow_barrage",
        element = Element.Shadow,
        tier = Tier.Barrage,
        maxHit = 28,
        travel = Travel.None,
        impact = "spotanim.shadow_barrage_impact",
        impactHeight = 0,
        hitSound = "synth.shadow_barrage_impact",
        effectStrength = 15,
    ),
    BloodBarrage(
        obj = "obj.92_blood_barrage",
        element = Element.Blood,
        tier = Tier.Barrage,
        maxHit = 29,
        travel = Travel.None,
        impact = "spotanim.spell_blood_barrage_impact",
        impactHeight = 0,
        hitSound = "synth.blood_barrage_impact",
        effectStrength = 25,
    ),
    IceBarrage(
        obj = "obj.94_ice_barrage",
        element = Element.Ice,
        tier = Tier.Barrage,
        maxHit = 30,
        travel = Travel.FromTarget("spotanim.ice_barrage_travel", endHeight = 0),
        impact = "spotanim.ice_barrage_impact",
        impactHeight = 0,
        hitSound = "synth.ice_barrage_impact",
        effectStrength = 32,
    );

    val castSound: String
        get() =
            when (element) {
                Element.Smoke -> "synth.smoke_cast"
                Element.Shadow -> "synth.shadow_cast"
                Element.Blood -> "synth.blood_cast"
                Element.Ice -> "synth.ice_cast"
            }
}
