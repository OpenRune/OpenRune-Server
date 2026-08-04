package org.rsmod.content.skills.crafting.util

object CraftingConstants {

    const val STAT_CRAFTING = "stat.crafting"

    /** Level cap for Crafting. Used for any level 99 crafting checks like the guild chest, crafting tutor dialog, cape teleport, etc. */
    const val MAX_CRAFTING_LEVEL = 99

    /** Recipe xp is stored multiplied by this so the int columns can carry fractional xp. */
    const val FINE_XP_DIVISOR = 10

    /** Shared production queue used by every crafting section. */
    const val QUEUE_CRAFTING_MAKE = "queue.crafting_make"

    /** Crafts per spool of thread. */
    const val THREAD_USES_PER_SPOOL = 5

    const val NEEDLE = "obj.needle"
    const val THREAD = "obj.thread"
    const val CHISEL = "obj.chisel"
    const val HAMMER = "obj.hammer"
    const val KNIFE = "obj.knife"
    const val GLASSBLOWING_PIPE = "obj.glassblowingpipe"

    const val IMCANDO_HAMMER = "obj.imcando_hammer"
    const val IMCANDO_HAMMER_OFFHAND = "obj.imcando_hammer_offhand"

    const val CLOCKWORK = "obj.poh_clockwork_mechanism"

    const val ANIM_BIRDHOUSE = "seq.birdhouse_make"
    const val ANIM_BIRDHOUSE_IMCANDO = "seq.birdhouse_make_imcando_hammer"

    const val COINS = "obj.coins"

    const val GOLD_BAR = "obj.gold_bar"
    const val SILVER_BAR = "obj.silver_bar"

    const val CLAY = "obj.clay"
    const val SOFT_CLAY = "obj.softclay"
    const val BUCKET_EMPTY = "obj.bucket_empty"
    const val SODA_ASH = "obj.soda_ash"
    const val BUCKET_OF_SAND = "obj.bucket_sand"


    const val COSTUME_NEEDLE = "obj.costumeneedle"


    const val CATEGORY_FURNACE = "category.furnace"

    val SPINNING_WHEELS: List<String> = listOf(
        "loc.viking_spinningwheel",
        "loc.elf_village_spinning_wheel",
        "loc.spinningwheel",
        "loc.contact_spinning_wheel",
        "loc.iznot_spinning_wheel",
        "loc.kr_spinningwheel",
        "loc.murder_qip_spinning_wheel",
        "loc.fossil_spinning_wheel_built",
        "loc.sw_spinningwheel_fixed",
        "loc.spinningwheel_quetzacali",
        "loc.spinningwheel_2",
        "loc.amenity_spinning_wheel_built",
    )

    /** Loom locs. */
    val LOOMS: List<String> = listOf(
        "loc.loom",
        "loc.regicide_loom",
        "loc.fossil_loom_built",
        "loc.amenity_loom_built",
    )

    /** Potter's wheel locs. */
    val POTTERY_WHEELS: List<String> = listOf( //Note: Category 377 are pottery wheels, but includes unbuilt and broken
        "loc.viking_potterywheel",
        "loc.potterywheel",
        "loc.contact_potterywheel",
        "loc.darkm_poor_potterywheel",
        "loc.sw_potterywheel_fixed",
        "loc.potterywheel_2",
        "loc.amenity_potterywheel_built",
    )

    /** Pottery oven locs. */
    val POTTERY_OVENS: List<String> = listOf( //Note: No category data on the ovens
        "loc.potteryoven",
        "loc.viking_potteryoven",
        "loc.amenity_potteryoven_built",
        "loc.fai_barbarian_pottery_oven",
        "loc.darkm_poor_pottery_oven",
    )

    /** Thakkrad Sigmundson */
    const val YAK_CURER = "npc.fris_r_engineer"

    const val CRAFTING_TUTOR = "npc.aide_tutor_crafting"

    const val ANIM_SPINNING_60 = "seq.human_spinningwheel_60"
    const val ANIM_SPINNING_90 = "seq.human_spinningwheel_90"
    const val ANIM_SPINNING = ANIM_SPINNING_90

    /** Wheel locs that use the 60-frame spinning animation instead of the default 90-frame one. */
    val SPINNING_WHEELS_60: Set<String> = setOf(
        //Currently empty
    )

    const val LOC_ANIM_SPINNING = "seq.spinningwheel"
    const val SOUND_SPINNING = "synth.spinning"

    const val ANIM_WEAVING = "seq.farming_useloom"
    const val LOC_ANIM_WEAVING = "seq.loom"
    const val SOUND_WEAVING = "synth.loom_weave"

    const val ANIM_POTTERY_WHEEL = "seq.human_potterywheel"
    const val LOC_ANIM_POTTERY_WHEEL = "seq.potterywheel"
    const val SOUND_POTTERY_WHEEL = "synth.crafting_pottery_wheel_craft"
    const val ANIM_POTTERY_OVEN = "seq.potteryoven_quick"

    const val ANIM_LEATHER_CRAFT = "seq.human_leather_crafting"
    const val SOUND_LEATHER_CRAFT = "synth.stiching"

    const val ANIM_PHEASANT_COSTUME = "seq.human_pheasant_feathers_crafting"

    const val ANIM_GEM_CUTTING = "seq.human_gem_cutting"
    const val SOUND_GEM_CUTTING = "synth.chisel"
    const val SOUND_GEM_CRUSH = "synth.smash_gem"
    const val ANIM_AMETHYST_CUT = "seq.human_amethystcutting"
    const val ANIM_SNAIL_SHELL_CUT = "seq.human_snailshellcutting"
    const val ANIM_KNIFE_CUTTING = "seq.human_cutting_knife"
    const val ANIM_LIMESTONE_CUT = "seq.human_limestonecutting"

    const val ANIM_GLASSBLOWING = "seq.human_glassblowing"
    const val SOUND_GLASSBLOWING = "synth.glassblowing"

    const val ANIM_FURNACE = "seq.human_furnace"
    const val SOUND_FURNACE = "synth.furnace"

    const val ANIM_BATTLESTAFF = "seq.human_battlestaff_crafting"
    const val SOUND_BATTLESTAFF_ATTACH = "synth.attach_orb"

    const val SOUND_AMULET_STRINGING = "synth.stringing"

    /** Player animation for filling a bucket at a sand pit. */
    const val ANIM_SAND_PIT = "seq.human_fillbucket_sandpit"
    /** Sound played on each bucket fill. */
    const val SOUND_SAND_BUCKET = "synth.sand_bucket"
    /**  Content group shared by every sandpit loc. */
    const val CONTENT_SAND_PIT = "content.sandpit"

    /** The crafting cape and its trimmed variant. */
    val CRAFTING_SKILLCAPES: Set<String> =
        setOf("obj.skillcape_crafting", "obj.skillcape_crafting_trimmed")

    /** The crafting cape's hood. */
    const val CRAFTING_HOOD = "obj.skillcape_crafting_hood"

    /** Aprons that get a player through the guild door. */
    val GUILD_APRONS: Set<String> = setOf("obj.brown_apron", "obj.golden_apron")

    /** Max capes, which stand in for the crafting cape at the guild door. */
    val MAX_SKILLCAPES: Set<String> =
        setOf(
            "obj.skillcape_max",
            "obj.skillcape_max_firecape",
            "obj.skillcape_max_saradomin",
            "obj.skillcape_max_zamorak",
            "obj.skillcape_max_guthix",
            "obj.skillcape_max_anma",
            "obj.skillcape_max_worn",
            "obj.skillcape_max_ardy",
            "obj.skillcape_max_infernalcape",
            "obj.skillcape_max_saradomin2",
            "obj.skillcape_max_zamorak2",
            "obj.skillcape_max_guthix2",
            "obj.skillcape_max_assembler",
            "obj.skillcape_max_infernalcape_trouver",
            "obj.skillcape_max_firecape_trouver",
            "obj.skillcape_max_assembler_trouver",
            "obj.skillcape_max_saradomin2_trouver",
            "obj.skillcape_max_zamorak2_trouver",
            "obj.skillcape_max_guthix2_trouver",
            "obj.skillcape_max_mythical",
            "obj.skillcape_max_assembler_masori",
            "obj.skillcape_max_assembler_masori_trouver",
            "obj.skillcape_max_dizanas",
            "obj.skillcape_max_dizanas_trouver",
        )

    /** Selected make-quantity, shared by the `skillmain` quantity column across skill interfaces. */
    const val VARP_MAKEX_CRAFTING = "varp.makexcrafting"

}
