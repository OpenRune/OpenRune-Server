package org.rsmod.content.other.special.weapons.magic

import kotlin.math.max

class StaffSpell(
    val anim: String,
    val castSpot: String,
    val projectile: String,
    val impact: String,
    val castHeight: Int = 92,
    val impactHeight: Int = 92,
    val projanim: String = "projanim.powered_staff",
    val castSound: String = "synth.shadow_cast",
    val hitSound: String = "synth.contact_darkness_impact",
    val leechImpact: String? = null,
    val leechHeight: Int = 92,
)

class StaffMaterial(val obj: String, val perCharge: Int)

class PoweredStaffType(
    val name: String,
    val spell: StaffSpell,
    val maxHit: (Int) -> Int,
    val charged: List<String>,
    val uncharged: List<String> = emptyList(),
    val alwaysFull: Map<String, String> = emptyMap(),
    val varobj: String? = null,
    val maxCharges: Int = 0,
    val costs: List<StaffMaterial> = emptyList(),
    val refunds: List<StaffMaterial> = costs,
    val chargeOp: Boolean = false,
    val checkSuffix: String = "",
    val warnEvery: Int = 0,
    val lifeLeech: Boolean = false,
    val alwaysHits: Boolean = false,
    val noChargesMessage: String = "",
    val unchargeTitle: String = "Fully uncharge your $name?",
    val attackDelay: Int? = null,
    val venom: Boolean = false,
    val unchargedChargeOp: Int = 3,
    val customCharging: Boolean = false,
) {
    val chargeable: Boolean
        get() = varobj != null

    val allObjs: List<String>
        get() = charged + uncharged + alwaysFull.keys
}

object PoweredStaffTypes {
    const val CHARGES_13_BIT = "varobj.charges_16383"
    const val CHARGES_15_BIT = "varobj.tumeken_charges"
    const val CHARGES_16_BIT = "varobj.charges_65535"

    private const val DEATH_RUNE = "obj.deathrune"
    private const val CHAOS_RUNE = "obj.chaosrune"
    private const val FIRE_RUNE = "obj.firerune"
    private const val EARTH_RUNE = "obj.earthrune"
    private const val BLOOD_RUNE = "obj.bloodrune"
    private const val COINS = "obj.coins"
    private const val ZULRAH_SCALE = "obj.snakeboss_scale"

    private const val TRIDENT_MAX_CHARGES = 2_500
    private const val ENHANCED_TRIDENT_MAX_CHARGES = 20_000

    private val SEAS_SPELL =
        StaffSpell(
            anim = "seq.human_castwave_staff_walkmerge",
            castSpot = "spotanim.slayer_tots_casting",
            projectile = "spotanim.slayer_tots_projectile",
            impact = "spotanim.slayer_tots_impact",
        )

    private val SWAMP_SPELL =
        StaffSpell(
            anim = "seq.human_castwave_staff_walkmerge",
            castSpot = "spotanim.toxic_tots_casting",
            projectile = "spotanim.toxic_tots_projectile",
            impact = "spotanim.toxic_tots_impact",
        )

    private val SEAS_COSTS =
        listOf(
            StaffMaterial(DEATH_RUNE, 1),
            StaffMaterial(CHAOS_RUNE, 1),
            StaffMaterial(FIRE_RUNE, 5),
            StaffMaterial(COINS, 10),
        )

    private val SWAMP_COSTS =
        listOf(
            StaffMaterial(DEATH_RUNE, 1),
            StaffMaterial(CHAOS_RUNE, 1),
            StaffMaterial(FIRE_RUNE, 5),
            StaffMaterial(ZULRAH_SCALE, 1),
        )

    private val SEAS_REFUNDS = SEAS_COSTS.filter { it.obj != COINS }

    private const val SEAS_NO_CHARGES =
        "Your trident has no charges! You need to charge it with death runes, " +
            "chaos runes, fire runes and coins."

    private const val SWAMP_NO_CHARGES =
        "Your trident has no charges! You need to charge it with death runes, " +
            "chaos runes, fire runes and Zulrah's scales."

    val ALL: List<PoweredStaffType> =
        listOf(
            PoweredStaffType(
                name = "Trident of the seas",
                spell = SEAS_SPELL,
                maxHit = { max(1, it / 3 - 5) },
                charged = listOf("obj.tots_charged", "obj.tots_charged_orn"),
                uncharged = listOf("obj.tots_uncharged", "obj.tots_uncharged_orn"),
                alwaysFull =
                    mapOf(
                        "obj.tots" to "obj.tots_uncharged",
                        "obj.tots_orn" to "obj.tots_uncharged_orn",
                    ),
                varobj = CHARGES_13_BIT,
                maxCharges = TRIDENT_MAX_CHARGES,
                costs = SEAS_COSTS,
                refunds = SEAS_REFUNDS,
                warnEvery = 100,
                noChargesMessage = SEAS_NO_CHARGES,
                unchargeTitle = "You will NOT get the coins back.",
            ),
            PoweredStaffType(
                name = "Trident of the seas (e)",
                spell = SEAS_SPELL,
                maxHit = { max(1, it / 3 - 5) },
                charged = listOf("obj.tots_i_charged", "obj.tots_i_charged_orn"),
                uncharged = listOf("obj.tots_i_uncharged", "obj.tots_i_uncharged_orn"),
                varobj = CHARGES_15_BIT,
                maxCharges = ENHANCED_TRIDENT_MAX_CHARGES,
                costs = SEAS_COSTS,
                refunds = SEAS_REFUNDS,
                warnEvery = 100,
                noChargesMessage = SEAS_NO_CHARGES,
                unchargeTitle = "You will NOT get the coins back.",
            ),
            PoweredStaffType(
                name = "Trident of the swamp",
                spell = SWAMP_SPELL,
                maxHit = { max(4, it / 3 - 2) },
                charged = listOf("obj.toxic_tots_charged", "obj.toxic_tots_charged_orn"),
                uncharged = listOf("obj.toxic_tots_uncharged", "obj.toxic_tots_uncharged_orn"),
                varobj = CHARGES_13_BIT,
                maxCharges = TRIDENT_MAX_CHARGES,
                venom = true,
                costs = SWAMP_COSTS,
                warnEvery = 100,
                noChargesMessage = SWAMP_NO_CHARGES,
            ),
            PoweredStaffType(
                name = "Trident of the swamp (e)",
                spell = SWAMP_SPELL,
                maxHit = { max(4, it / 3 - 2) },
                charged = listOf("obj.toxic_tots_i_charged", "obj.toxic_tots_i_charged_orn"),
                uncharged = listOf("obj.toxic_tots_i_uncharged", "obj.toxic_tots_i_uncharged_orn"),
                varobj = CHARGES_15_BIT,
                maxCharges = ENHANCED_TRIDENT_MAX_CHARGES,
                venom = true,
                costs = SWAMP_COSTS,
                warnEvery = 100,
                noChargesMessage = SWAMP_NO_CHARGES,
            ),
            PoweredStaffType(
                name = "Sanguinesti staff",
                spell =
                    StaffSpell(
                        anim = "seq.human_castwave_staff",
                        castSpot = "spotanim.sanguinesti_staff_casting",
                        projectile = "spotanim.sanguinesti_staff_travel",
                        impact = "spotanim.sanguinesti_staff_impact",
                        impactHeight = 60,
                        leechImpact = "spotanim.sanguinesti_staff_heal",
                    ),
                maxHit = { max(6, it / 3) },
                charged = listOf("obj.sanguinesti_staff", "obj.sanguinesti_staff_or"),
                uncharged =
                    listOf("obj.sanguinesti_staff_uncharged", "obj.sanguinesti_staff_uncharged_or"),
                varobj = CHARGES_15_BIT,
                maxCharges = 20_000,
                costs = listOf(StaffMaterial(BLOOD_RUNE, 2)),
                chargeOp = true,
                lifeLeech = true,
                noChargesMessage =
                    "Your Sanguinesti staff has no charges! You need to charge it with blood runes.",
            ),
            PoweredStaffType(
                name = "Warped sceptre",
                spell =
                    StaffSpell(
                        anim = "seq.pog_warped_sceptre_attack",
                        castSpot = "spotanim.vfx_warped_sceptre_cast",
                        projectile = "spotanim.vfx_warped_sceptre_projectile_projectile",
                        impact = "spotanim.vfx_warped_sceptre_projectile_impact",
                        castHeight = 100,
                        impactHeight = 0,
                        projanim = "projanim.warped_sceptre",
                    ),
                maxHit = { (8 * it + 96) / 37 },
                charged = listOf("obj.warped_sceptre"),
                uncharged = listOf("obj.warped_sceptre_uncharged"),
                varobj = CHARGES_15_BIT,
                maxCharges = 20_000,
                costs = listOf(StaffMaterial(CHAOS_RUNE, 2), StaffMaterial(EARTH_RUNE, 5)),
                chargeOp = true,
                unchargedChargeOp = 4,
                checkSuffix = " remaining",
                noChargesMessage =
                    "Your warped sceptre has no charges! " +
                        "You need to charge it with chaos runes and earth runes.",
            ),
            PoweredStaffType(
                name = "Eye of Ayak",
                spell =
                    StaffSpell(
                        anim = "seq.human_eye_of_ayak_normal",
                        castSpot = "spotanim.vfx_ayak_player_normal_spotanim",
                        projectile = "spotanim.vfx_ayak_normal_projectile",
                        impact = "spotanim.vfx_ayak_normal_impact",
                        castHeight = 0,
                        impactHeight = 124,
                        projanim = "projanim.eye_of_ayak",
                        hitSound = "synth.contact_darkness_impact",
                    ),
                maxHit = { max(0, it / 3 - 6) },
                charged = listOf("obj.eye_of_ayak"),
                uncharged = listOf("obj.eye_of_ayak_uncharged"),
                varobj = CHARGES_16_BIT,
                maxCharges = 50_000,
                attackDelay = 3,
                customCharging = true,
                noChargesMessage =
                    "The Eye of Ayak has no charges! " +
                        "You need to charge it with demon tears or death and chaos runes.",
            ),
            PoweredStaffType(
                name = "Dawnbringer",
                spell =
                    StaffSpell(
                        anim = "seq.human_castwave_staff",
                        castSpot = "spotanim.dawnbringer_casting",
                        projectile = "spotanim.dawnbringer_projectile",
                        impact = "spotanim.dawnbringer_impact",
                    ),
                maxHit = { max(2, (it / 3 - 2) / 2) },
                charged = listOf("obj.verzik_special_weapon"),
                alwaysHits = true,
            ),
        )
}
