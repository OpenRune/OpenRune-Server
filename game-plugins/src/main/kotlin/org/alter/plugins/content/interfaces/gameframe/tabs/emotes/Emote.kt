package org.alter.plugins.content.interfaces.emotes

import org.alter.api.cfg.Varbit
import org.alter.rscm.RSCM

/**
 * @author Tom <rspsmods@gmail.com>
 */
enum class Emote(
    val slot: Int,
    val anim: String,
    val gfx: String = RSCM.NONE,
    val varbit: Int = -1,
    val requiredVarbitValue: Int = 1,
    val unlockDescription: String? = null,
) {
    YES(slot = 0, anim = "sequences.emote_yes"),
    NO(slot = 1, anim = "sequences.emote_no"),
    BOW(slot = 2, anim = "sequences.emote_bow"),
    ANGRY(slot = 3, anim = "sequences.emote_angry"),
    THINK(slot = 4, anim = "sequences.emote_think"),
    WAVE(slot = 5, anim = "sequences.emote_wave"),
    SHRUG(slot = 6, anim = "sequences.emote_shrug"),
    CHEER(slot = 7, anim = "sequences.emote_cheer"),
    BECKON(slot = 8, anim = "sequences.emote_beckon"),
    LAUGH(slot = 9, anim = "sequences.emote_laugh"),
    JUMP_FOR_JOY(slot = 10, anim = "sequences.emote_jump_with_joy"),
    YAWN(slot = 11, anim = "sequences.emote_yawn"),
    DANCE(slot = 12, anim = "sequences.emote_dance"),
    JIG(slot = 13, anim = "sequences.emote_dance_scottish"),
    SPIN(slot = 14, anim = "sequences.emote_dance_spin"),
    HEADBANG(slot = 15, anim = "sequences.emote_dance_headbang"),
    CRY(slot = 16, anim = "sequences.emote_cry"),
    BLOW_KISS(slot = 17, anim = "sequences.emote_blow_kiss", gfx = "spotanims.trollromance_heart"),
    PANIC(slot = 18, anim = "sequences.emote_panic"),
    RASPBERRY(slot = 19, anim = "sequences.emote_ya_boo_sucks"),
    CLAP(slot = 20, anim = "sequences.emote_clap"),
    SALUTE(slot = 21, anim = "sequences.emote_fremmenik_salute"),
    GOBLIN_BOW(slot = 22, anim = "sequences.human_cave_goblin_bow", varbit = Varbit.GOBLIN_EMOTES_VARBIT, requiredVarbitValue = 7),
    GOBLIN_SALUTE(slot = 23, anim = "sequences.human_cave_goblin_dance", varbit = Varbit.GOBLIN_EMOTES_VARBIT, requiredVarbitValue = 7),
    GLASS_BOX(slot = 24, anim = "sequences.emote_glass_box", varbit = Varbit.GLASS_BOX_EMOTE_VARBIT),
    CLIMB_ROPE(slot = 25, anim = "sequences.emote_climbing_rope", varbit = Varbit.CLIMB_ROPE_EMOTE_VARBIT),
    LEAN(slot = 26, anim = "sequences.emote_mime_lean", varbit = Varbit.LEAN_EMOTE_VARBIT),
    GLASS_WALL(slot = 27, anim = "sequences.emote_glass_wall", varbit = Varbit.GLASS_WALL_EMOTE_VARBIT),
    IDEA(slot = 28, anim = "sequences.emote_lightbulb", gfx = "spotanims.emote_lightbulb_spot", varbit = Varbit.IDEA_EMOTE_VARBIT),
    STAMP(slot = 29, anim = "sequences.anger3", varbit = Varbit.STAMP_EMOTE_VARBIT),
    FLAP(slot = 30, anim = "sequences.emote_panic_flap", varbit = Varbit.FLAP_EMOTE_VARBIT),
    SLAP_HEAD(slot = 31, anim = "sequences.emote_slap_head", varbit = Varbit.SLAP_HEAD_EMOTE_VARBIT),
    ZOMBIE_WALK(slot = 32, anim = "sequences.zombie_walk_emote", varbit = Varbit.ZOMBIE_WALK_EMOTE_VARBIT),
    ZOMBIE_DANCE(slot = 33, anim = "sequences.zombie_dance", varbit = Varbit.ZOMBIE_DANCE_EMOTE_VARBIT),
    SCARED(slot = 34, anim = "sequences.terrified_emote", varbit = Varbit.SCARED_EMOTE_VARBIT),
    RABBIT_HOP(slot = 35, anim = "sequences.rabbit_emote", varbit = Varbit.RABBIT_HOP_EMOTE_VARBIT), //
    SIT_UP(slot = 36, anim = "sequences.situps_5", varbit = Varbit.EXERCISE_EMOTES),
    PUSH_UP(slot = 37, anim = "sequences.pushups_5", varbit = Varbit.EXERCISE_EMOTES),
    STAR_JUMP(slot = 38, anim = "sequences.starjump_5", varbit = Varbit.EXERCISE_EMOTES),
    JOG(slot = 39, anim = "sequences.run_on_spot", varbit = Varbit.EXERCISE_EMOTES),
    FLEX(slot = 40, anim = "sequences.emote_flex", gfx = RSCM.NONE, varbit = Varbit.FLEX_EMOTE_VARBIT),
    ZOMBIE_HAND(slot = 41, anim = "sequences.hw07_arm_from_the_ground_emote", gfx = "spotanims.hw07_arm_from_the_ground_spotanim", varbit = Varbit.ZOMBIE_HAND_EMOTE_VARBIT),
    HYPERMOBILE_DRINKER(slot = 42, anim = "sequences.ash_emote", varbit = Varbit.HYPERMOBILE_DRINKER_EMOTE_VARBIT),
    SKILLCAPE(slot = 43, anim = RSCM.NONE), // @TODO
    AIR_GUITAR(slot = 44, anim = "sequences.emote_air_guitar", gfx = "spotanims.air_guitar_spotanim", varbit = Varbit.AIR_GUITAR_EMOTE_VARBIT),
    URI_TRANSFORM(slot = 45, anim = RSCM.NONE, gfx = RSCM.NONE, varbit = Varbit.URI_TRANSFORM_EMOTE_VARBIT), // @TODO
    SMOOTH_DANCE(slot = 46, anim = "sequences.bday17_bling", varbit = Varbit.SMOOTH_DANCE_EMOTE_VARBIT),
    CRAZY_DANCE(slot = 47, anim = "sequences.bday17_style", varbit = Varbit.CRAZY_DANCE_EMOTE_VARBIT),

    // bronze, silver, and gold shield, referencing the 3, 6, and 12 month packages from the Premier Club.
    PREMIER_SHIELD(slot = 48, anim = "sequences.premier_club_emote", gfx = "spotanims.premier_club_emote_spotanim_bronze", varbit = Varbit.PREMIER_SHIELD_EMOTE_VARBIT),
    EXPLORE(slot = 49, anim = "sequences.emote_explore", varbit = Varbit.EXPLORE_VARBIT), // @TODO

    // Twisted gfx: 1749 | TrailBlazer gfx: 1835
    // Varbit: 11757 -> 3 for Twisted, 6 for trailblazer, 9 for shattered
    RELIC_UNLOCKED(slot = 50, anim = "sequences.human_relic_unlock", gfx = "spotanims.league_trailblazer_relic_unlock_spot", varbit = Varbit.RELIC_UNLOCKED_EMOTE_VARBIT, requiredVarbitValue = 9), // @TODO unlockDescription = "You can't use that emote unless you have stored a tier 3 relichunter \n outfit on the outfitstand in your player owned house League Hall."
    PARTY(slot = 51, anim = "sequences.emote_party", gfx = "spotanims.fx_emote_party01_active", varbit = Varbit.PARTY_EMOTE_VARBIT),
    ;

    companion object {
        val values = enumValues<Emote>()
    }
}
