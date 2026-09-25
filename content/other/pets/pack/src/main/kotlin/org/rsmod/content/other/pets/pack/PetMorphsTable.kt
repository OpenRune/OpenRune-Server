package org.rsmod.content.other.pets.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object PetMorphsTable {
    const val PET = 0
    const val ITEM = 1
    const val FORM = 2
    const val UNLOCKS = 3
    const val REQUIRES = 4
    const val COUNT = 5
    const val CONSUME = 6
    const val HELD = 7
    const val LABEL = 8

    const val FIRELIGHTERS = 250
    const val FUNKY_LOG = "varbit.pet_beaver_funky_log"

    val BEAVER_COLOURS =
        mapOf(
            "obj.logs" to "obj.skillpetwc",
            "obj.oak_logs" to "obj.skillpet_wc_oak",
            "obj.willow_logs" to "obj.skillpet_wc_willow",
            "obj.maple_logs" to "obj.skillpet_wc_maple",
            "obj.yew_logs" to "obj.skillpet_wc_yew",
            "obj.magic_logs" to "obj.skillpet_wc_magic",
            "obj.redwood_logs" to "obj.skillpet_wc_redwood",
            "obj.teak_logs" to "obj.skillpet_wc_teak",
            "obj.mahogany_logs" to "obj.skillpet_wc_mahogany",
            "obj.arctic_pine_log" to "obj.skillpet_wc_arctic",
            "obj.camphor_logs" to "obj.skillpet_wc_camphor",
            "obj.ironwood_logs" to "obj.skillpet_wc_ironwood",
            "obj.jatoba_logs" to "obj.skillpet_wc_jatoba",
            "obj.rosewood_logs" to "obj.skillpet_wc_rosewood",
        )

    val ROCK_GOLEM_ORES =
        mapOf(
            "obj.rock" to "obj.skillpetmining",
            "obj.tin_ore" to "obj.skillpetmining_tin",
            "obj.copper_ore" to "obj.skillpetmining_copper",
            "obj.iron_ore" to "obj.skillpetmining_iron",
            "obj.blurite_ore" to "obj.skillpetmining_blurite",
            "obj.silver_ore" to "obj.skillpetmining_silver",
            "obj.coal" to "obj.skillpetmining_coal",
            "obj.gold_ore" to "obj.skillpetmining_gold",
            "obj.mithril_ore" to "obj.skillpetmining_mithril",
            "obj.enakh_granite_tiny" to "obj.skillpetmining_granite",
            "obj.enakh_granite_small" to "obj.skillpetmining_granite",
            "obj.enakh_granite_medium" to "obj.skillpetmining_granite",
            "obj.adamantite_ore" to "obj.skillpetmining_adamantite",
            "obj.runite_ore" to "obj.skillpetmining_runite",
            "obj.amethyst" to "obj.skillpetmining_amethyst",
            "obj.lovakite_ore" to "obj.skillpetmining_lovakite",
            "obj.elemental_workshop_ore" to "obj.skillpetmining_elemental",
            "obj.sang_jas_ore" to "obj.skillpetmining_daeyalt",
            "obj.lead_ore" to "obj.skillpetmining_lead",
            "obj.rubium_splinters" to "obj.skillpetmining_rubium",
            "obj.nickel_ore" to "obj.skillpetmining_nickel",
        )

    fun morphs() = dbTable("dbtable.pet_morphs", serverOnly = true) {
        column("pet", PET, VarType.STRING)
        column("item", ITEM, VarType.OBJ)
        column("form", FORM, VarType.OBJ)
        column("unlocks", UNLOCKS, VarType.INT)
        column("requires", REQUIRES, VarType.INT)
        column("count", COUNT, VarType.INT)
        column("consume", CONSUME, VarType.BOOLEAN)
        column("held", HELD, VarType.BOOLEAN)
        column("label", LABEL, VarType.STRING)

        fun morph(
            pet: String,
            item: String,
            form: String? = null,
            unlocks: List<String> = emptyList(),
            requires: String? = null,
            count: Int = 1,
            consume: Boolean = true,
            held: Boolean = false,
            label: String? = null,
        ) {
            row("dbrow.pet_morph_${pet}_${item.removePrefix("obj.")}") {
                column(PET, pet)
                columnRSCM(ITEM, item)
                if (form != null) {
                    columnRSCM(FORM, form)
                }
                if (unlocks.isNotEmpty()) {
                    columnRSCM(UNLOCKS, unlocks)
                }
                if (requires != null) {
                    columnRSCM(REQUIRES, requires)
                }
                column(COUNT, count)
                column(CONSUME, consume)
                column(HELD, held)
                if (label != null) {
                    column(LABEL, label)
                }
            }
        }

        morph("baby_mole", "obj.mole_claw", form = "obj.molepet_naked")
        morph("baby_mole", "obj.mole_skin", form = "obj.molepet")

        morph("rocky", "obj.redberries", form = "obj.skillpetthieving_panda")
        morph("rocky", "obj.poisonivy_berries", form = "obj.skillpetthieving_tanuki")
        morph("rocky", "obj.white_berries", form = "obj.skillpetthieving")

        morph("tangleroot", "obj.guam_seed", form = "obj.skillpetfarming_herb")
        morph("tangleroot", "obj.dragonfruit_tree_seed", form = "obj.skillpetfarming_dragon")
        morph("tangleroot", "obj.redwood_tree_seed", form = "obj.skillpetfarming_redwood")
        morph("tangleroot", "obj.white_lily_seed", form = "obj.skillpetfarming_lily")
        morph("tangleroot", "obj.crystal_tree_seed", form = "obj.skillpetfarming_crystal")
        morph("tangleroot", "obj.acorn", form = "obj.skillpetfarming")

        for ((ore, form) in ROCK_GOLEM_ORES) {
            morph("rock_golem", ore, form = form, consume = false, held = true)
        }

        morph("beaver", "obj.forestry_funky_shaped_log", unlocks = listOf(FUNKY_LOG))
        morph(
            "beaver",
            "obj.forestry_fox_pet_whistle",
            form = "obj.skillpet_wc_fox",
            unlocks = listOf("varbit.pet_beaver_fox"),
        )
        morph(
            "beaver",
            "obj.forestry_pheasant_pet_egg",
            form = "obj.skillpet_wc_pheasant",
            unlocks = listOf("varbit.pet_beaver_pheasant"),
        )
        for ((logs, form) in BEAVER_COLOURS) {
            morph("beaver", logs, form = form, requires = FUNKY_LOG)
        }

        morph(
            "giant_squirrel",
            "obj.dark_acorn",
            form = "obj.skillpetagility_dark",
            unlocks = listOf("varbit.hallowed_skillpet_dark_unlocked"),
        )
        morph(
            "giant_squirrel",
            "obj.calcified_acorn",
            form = "obj.skillpetagility_bone",
            unlocks = listOf("varbit.agility_skillpet_bone_unlocked"),
        )

        morph(
            "phoenix",
            "obj.gnomish_firelighter_blue",
            form = "obj.phoenixpet_blue",
            unlocks = listOf("varbit.pet_phoenix_blue"),
            count = FIRELIGHTERS,
            label = "Blue",
        )
        morph(
            "phoenix",
            "obj.gnomish_firelighter_green",
            form = "obj.phoenixpet_green",
            unlocks = listOf("varbit.pet_phoenix_green"),
            count = FIRELIGHTERS,
            label = "Green",
        )
        morph(
            "phoenix",
            "obj.trail_gnomish_firelighter_purple",
            form = "obj.phoenixpet_purple",
            unlocks = listOf("varbit.pet_phoenix_purple"),
            count = FIRELIGHTERS,
            label = "Purple",
        )
        morph(
            "phoenix",
            "obj.trail_gnomish_firelighter_white",
            form = "obj.phoenixpet_white",
            unlocks = listOf("varbit.pet_phoenix_white"),
            count = FIRELIGHTERS,
            label = "White",
        )

        morph(
            "sraracha",
            "obj.hosdun_orange_egg_sac",
            form = "obj.sarachnispet_orange",
            unlocks = listOf("varbit.sarachnispet_orange_transmog"),
        )
        morph(
            "sraracha",
            "obj.hosdun_blue_egg_sac",
            form = "obj.sarachnispet_blue",
            unlocks = listOf("varbit.sarachnispet_blue_transmog"),
        )

        morph("muphin", "obj.muspah_pet_morph", form = "obj.muspahpet_melee", unlocks = listOf("varbit.pet_muphin_forms"))
        morph("nid", "obj.araxxor_pet_morph", form = "obj.araxxorpet_cute", unlocks = listOf("varbit.pet_nid_rax"))
        morph("lil_zik", "obj.tob_hardmode_dust", unlocks = listOf("varbit.pet_lil_zik_morphs"))
        morph("olmlet", "obj.raids_challenge_morph", unlocks = listOf("varbit.pet_olmlet_morphs"))
        morph(
            "little_nightmare",
            "obj.nightmare_challenge_morph",
            form = "obj.nightmarepet_parasite",
            unlocks = listOf("varbit.nightmare_pet_parasite_unlocked"),
        )

        morph(
            "tumeken_s_guardian",
            "obj.toa_pet_morph_wardens",
            unlocks =
                listOf(
                    "varbit.toa_unlocked_pet_morph_destroyed_tumeken",
                    "varbit.toa_unlocked_pet_morph_destroyed_elidinis",
                ),
        )
        morph(
            "tumeken_s_guardian",
            "obj.toa_pet_morph_akkha",
            form = "obj.wardenpet_akkha",
            unlocks = listOf("varbit.toa_unlocked_pet_morph_akkha"),
        )
        morph(
            "tumeken_s_guardian",
            "obj.toa_pet_morph_baba",
            form = "obj.wardenpet_baba",
            unlocks = listOf("varbit.toa_unlocked_pet_morph_baba"),
        )
        morph(
            "tumeken_s_guardian",
            "obj.toa_pet_morph_kephri",
            form = "obj.wardenpet_kephri",
            unlocks = listOf("varbit.toa_unlocked_pet_morph_kephri"),
        )
        morph(
            "tumeken_s_guardian",
            "obj.toa_pet_morph_zebak",
            form = "obj.wardenpet_zebak",
            unlocks = listOf("varbit.toa_unlocked_pet_morph_zebak"),
        )

        morph(
            "rift_guardian",
            "obj.guardians_eye",
            form = "obj.skillpetrunecrafting_gotr",
            unlocks = listOf("varbit.skillpet_runecrafting_gotr_unlocked"),
        )
    }
}
