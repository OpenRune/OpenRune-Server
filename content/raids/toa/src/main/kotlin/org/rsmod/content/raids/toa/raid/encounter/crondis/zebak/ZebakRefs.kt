package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

/** Gameval names used by the Zebak room. Unnamed synths and projanims: gamevals.toml. */
internal object ZebakNpcs {
    const val ZEBAK = "npc.toa_zebak"
    const val ZEBAK_ENRAGED = "npc.toa_zebak_enraged"
    const val ZEBAK_DEAD = "npc.toa_zebak_dead"
    const val TAIL = "npc.toa_zebak_tail"
    const val TAIL_DEAD = "npc.toa_zebak_tail_dead"
    const val SPLIT_HELPER = "npc.spotanim_zebak_ranged01_npc"
    const val WATER_CROC = "npc.toa_zebak_watercroc"
    const val BLOOD_CLOUD = "npc.toa_zebak_blood_cloud"
    const val BLOOD_CLOUD_SMALL = "npc.toa_zebak_blood_cloud_small"
    const val JUG = "npc.toa_zebak_jug"
    const val JUG_ROLLING = "npc.toa_zebak_jug_rolling"
    const val BOULDER = "npc.toa_zebak_safespot"
    const val WAVE = "npc.toa_zebak_wave"
    const val WAVE_BLOODY = "npc.toa_zebak_wave_bloody"
}

internal object ZebakLocs {
    const val BLOCKER = "loc.invisible_type8_blocking_size9"
    const val BOULDER_BLOCKER = "loc.invisible_type8_blocking_active"
    const val CLIMBING_ROCK = "loc.toa_zebak_climbing_rock"

    /** Left by a bleeding player; a wave washing one away turns bloody. */
    val BLOOD_SPLATS = listOf("loc.bloodsplatter1", "loc.bloodsplatter2", "loc.bloodsplatter3")
    val POISON =
        listOf(
            "loc.toa_zebak_vomit01",
            "loc.toa_zebak_vomit02",
            "loc.toa_zebak_vomit03",
            "loc.toa_zebak_vomit04",
            "loc.toa_zebak_vomit05",
            "loc.toa_zebak_vomit06",
        )
}

internal object ZebakSeqs {
    const val MELEE = "seq.npc_zebak01_attack_melee"
    const val TAIL_MELEE = "seq.npc_zebak02_attack_melee"
    const val MELEE_ENRAGED = "seq.npc_zebak01_attack_melee_enraged"
    const val TAIL_MELEE_ENRAGED = "seq.npc_zebak02_attack_melee_enraged"
    const val RANGED = "seq.npc_zebak01_attack_ranged"
    const val TAIL_RANGED = "seq.npc_zebak02_attack_ranged"
    const val ROAR = "seq.npc_zebak01_attack_roar"
    const val TAIL_ROAR = "seq.npc_zebak02_attack_roar"
    const val CALL_WAVES = "seq.npc_zebak01_attack_tail"
    const val TAIL_CALL_WAVES = "seq.npc_zebak02_attack_tail"
    const val DEATH = "seq.npc_zebak01_death"
    const val TAIL_DEATH = "seq.npc_zebak02_death"
    const val PLAYER_PUSHED = "seq.warguild_parry_defend"
    const val PLAYER_KNOCKED = "seq.agilityarena_player_spikedback"
    const val PLAYER_MOVE_JUG = "seq.human_leverdown"
    const val SWIM_READY = "seq.human_swim_ready"
    const val SWIM = "seq.human_swim"

    /** Preloaded on entry with client script 1846 (seq_prefetch), as Offline_Scape did. */
    val PRELOAD: List<Int> = (9618..9646).toList() + listOf(9532, 9533, 9534, 9541)
}

internal object ZebakSpots {
    const val MAGE_INITIAL = "spotanim.zebak_mage_projanim_initial"
    const val RANGE_INITIAL = "spotanim.zebak_range_projanim_initial"
    const val MAGE_SPLIT = "spotanim.zebak_mage_split"
    const val RANGE_SPLIT = "spotanim.zebak_ranged_split"
    const val MAGE_FRAGMENT = "spotanim.zebak_mage_projanim_split"
    const val RANGE_FRAGMENT = "spotanim.zebak_ranged_fragment01"
    const val MAGE_IMPACT = "spotanim.fireblast_impact"
    const val RANGE_IMPACT = "spotanim.darkbow_smoke_arrow_impact"
    const val BLOOD_BARRAGE = "spotanim.spell_blood_barrage_impact"
    const val POISON_SPREAD = "spotanim.zebak_vomit_projectile0"
    const val ACID = "spotanim.tob_xarpus_acidspit"
    const val BOULDER = "spotanim.zebak_safespot_travel"
    const val JUG = "spotanim.zebak_waterjug_travel"
    const val DUST = "spotanim.zebak_roar_wave_dust"
    const val JUG_BREAK = "spotanim.zebak_waterjug_break"
    const val JUG_SPLASH = "spotanim.zebak_waterjug_splash_travel"
    const val ACID_CLEARED = "spotanim.waterstrike_impact"
    const val WATER_SPLASH = "spotanim.watersplash"
    const val ROCK_FALL = "spotanim.zebak_rock_fall"
}

/** Projectile timings are in pack/.../configs/toa_zebak.toml. */
internal object ZebakProjs {
    const val INITIAL = "projanim.toa_zebak_initial"
    const val SPLIT = "projanim.toa_zebak_split"
    const val POISON_SPREAD = "projanim.toa_zebak_poison_spread"
    const val LOB = "projanim.toa_zebak_lob"
    const val LOB_FAR = "projanim.toa_zebak_lob_far"
    const val JUG_SPLASH = "projanim.toa_zebak_jug_splash"
}

internal object ZebakSynths {
    const val MAGE_SHOOT = "synth.toa_zebak_red_projectile_04"
    const val RANGE_SHOOT = "synth.toa_zebak_whoosh_projectile_02"
    const val MAGE_SPLIT = "synth.toa_zebak_redirected_jug_break_01"
    const val RANGE_SPLIT = "synth.toa_zebak_redirected_projectile_02"
    const val PROJECTILE_IMPACT = "synth.toa_zebak_projectile_impact_01"
    const val DAMAGED = "synth.toa_zebak_defend_01"
    const val FINAL_PHASE = "synth.fi_trollking_roar"
    const val BLOOD_BARRAGE = "synth.blood_barrage_impact"
    const val JUGS_SHOOT = "synth.toa_zebak_vomit_colours_projectile_10"
    const val ACID_LAND = "synth.toa_zebak_vomit_colours_projectile_splat_03"
    const val BOULDER_LAND = "synth.toa_zebak_debris_impact_01"
    const val PLAYER_PUSHED = "synth.toa_zebak_roar_single_tremor_03"
    const val RUMBLING = "synth.rumbling"
    const val WAVE_HIT = "synth.toa_zebak_death_second_floor_hit_01"

    /** (synth, delay) pairs, Offline_Scape SCREAM_SOUNDS. */
    val SCREAM =
        listOf(
            "synth.toa_zebak_attack_hand_stomp_first_01" to 16,
            "synth.toa_zebak_attack_roar_05" to 20,
            "synth.toa_zebak_attack_hand_stomp_01" to 32,
            "synth.toa_zebak_attack_roar_high_02" to 69,
            "synth.toa_zebak_attack_roar_bass_02" to 70,
            "synth.toa_zebak_attack_hand_stomp_final_01" to 79,
            "synth.toa_zebak_attack_jaw_shut_01" to 260,
        )

    /** (synth, delay) pairs, Offline_Scape WAVES_LAND_SOUNDS. */
    val WAVES_LAND =
        listOf(
            "synth.toa_zebak_falling_rocks_sweep_01" to 0,
            "synth.toa_zebak_fallling_rocks_water_impact_01" to 200,
            "synth.toa_zebak_tidal_wave_7600ms_01" to 200,
            "synth.toa_zebak_tidal_wave_7600ms_01" to 530,
            "synth.toa_zebak_tidal_wave_7600ms_01" to 920,
        )

    /** Capture: music from the challenge start. No gameval name confirmed for midis. */
    const val MIDI = 736
}

/** Invocation names (cache struct param 1160). */
internal object ZebakInvocations {
    const val NOT_JUST_A_HEAD = "Not Just a Head"
    const val ARTERIAL_SPRAY = "Arterial Spray"
    const val BLOOD_THINNERS = "Blood Thinners"
    const val UPSET_STOMACH = "Upset Stomach"
}
