package org.rsmod.content.skills.crafting.util

object CraftingConstants {
    const val STAT_CRAFTING = "stat.crafting"

    const val MAX_CRAFTING_LEVEL = 99

    const val FINE_XP_DIVISOR = 10

    const val QUEUE_CRAFTING_MAKE = "queue.crafting_make"

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

    const val CONTENT_SPINNING_WHEEL = "content.crafting_spinning_wheel"
    const val CONTENT_LOOM = "content.crafting_loom"
    const val CONTENT_POTTERY_WHEEL = "content.crafting_pottery_wheel"
    const val CONTENT_POTTERY_OVEN = "content.crafting_pottery_oven"

    const val YAK_CURER = "npc.fris_r_engineer"

    const val CRAFTING_TUTOR = "npc.aide_tutor_crafting"

    const val ANIM_SPINNING = "seq.human_spinningwheel_90"

    const val LOC_ANIM_SPINNING = "seq.spinningwheel"
    const val SOUND_SPINNING = "synth.spinning"

    const val ANIM_WEAVING = "seq.farming_useloom"
    const val SOUND_WEAVING = "synth.loom_weave"

    const val ANIM_POTTERY_WHEEL = "seq.human_potterywheel"
    const val LOC_ANIM_POTTERY_WHEEL = "loc.potterywheel"
    const val SOUND_POTTERY_WHEEL = "synth.crafting_pottery_wheel_craft"
    const val ANIM_POTTERY_OVEN = "seq.potteryoven_quick"

    const val ANIM_LEATHER_CRAFT = "seq.human_leather_crafting"
    const val SOUND_LEATHER_CRAFT = "synth.stiching"

    const val ANIM_PHEASANT_COSTUME = "seq.human_pheasant_feathers_crafting"

    const val ANIM_GEM_CUTTING = "seq.human_cutting"
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

    const val ANIM_SAND_PIT = "seq.human_fillbucket_sandpit"
    const val SOUND_SAND_BUCKET = "synth.sand_bucket"
    const val CONTENT_SAND_PIT = "content.sandpit"

    val CRAFTING_SKILLCAPES: Set<String> =
        setOf("obj.skillcape_crafting", "obj.skillcape_crafting_trimmed")

    const val CRAFTING_HOOD = "obj.skillcape_crafting_hood"

    val GUILD_APRONS: Set<String> = setOf("obj.brown_apron", "obj.golden_apron")

    const val CONTENT_MAX_CAPE = "content.max_cape"

    const val VARP_MAKEX_CRAFTING = "varp.makexcrafting"
}
