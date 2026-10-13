package org.rsmod.content.raids.toa.raid.encounter.apmeken.baba

import org.rsmod.map.CoordGrid

internal object BabaNpcs {
    const val BABA = "npc.toa_baba"
    const val DIGGING = "npc.toa_baba_digging"
    const val COFFIN = "npc.toa_baba_coffin"
    const val BOULDER = "npc.toa_baba_boulder"
    const val BOULDER_WEAK = "npc.toa_baba_boulder_weak"
    const val RUBBLE = "npc.toa_baba_rubble"
    const val BABOON = "npc.toa_baba_baboon"
    const val SARCOPHAGUS = "npc.toa_baba_sarc_npc"
    const val FREED_APMEKEN = "npc.toa_apmeken_freed"
}

internal object BabaLocs {
    const val COFFIN_CLOSED = "loc.toa_baba_coffin_closed"
    const val COFFIN_OPEN = "loc.toa_baba_coffin_open"
    const val BANANA_PEEL = "loc.toa_baba_banana_peel"
}

internal object BabaApes {
    const val SARCOPHAGUS_HP = 60
    const val BABOON_HP = 10
    const val BABOON_ATTACK_RATE = 4
    const val SARC_XS_FIRST = 3803
    const val SARC_X_STEP = 5
    const val SARC_COLUMNS = 4
    const val SOUTH_Z = 5400
    const val NORTH_Z = 5416
    const val SPAWN_MIN_X = 3803
    const val SPAWN_MAX_X = 3818
    const val SPAWN_MIN_Z = 5403
    const val SPAWN_MAX_Z = 5413
    const val SARC_HIT = 20
    const val RANGED_DAMAGE = 12
    const val ENERGY_INTERVAL = 4
    const val ENERGY_LENGTH = 3
    const val ENERGY_START = 2
    const val ENERGY_DAMAGE = 8
    const val ENERGY_SPREAD = 1
    const val ENERGY_STEP_CYCLES = 6
    const val FAITH_PRAYER_DIVISOR = 5
    const val PEEL_DAMAGE = 5
    const val PIT_MIN_Z = 5406
    const val PIT_MAX_Z = 5410
    const val PRAYER_STAT = "stat.prayer"
}

internal object BabaObjs {
    const val BANANA = "obj.toa_baba_banana"
    const val BOOK = "obj.toa_book_baba"
}

internal object BabaVarbits {
    const val BOOK_OWNED = "varbit.toa_book_baba"
}

internal object BabaVarns {
    const val ROLLS = "varn.toa_baba_rolls"
    const val ROWS = "varn.toa_baba_rows"
    const val ROLL_EXIT = "varn.toa_baba_roll_exit"
    const val WEAK_LANE = "varn.toa_baba_weak_lane"
    const val AUTOS = "varn.toa_baba_autos"
    const val THROW_AT = "varn.toa_baba_throw_at"
    const val ROCKFALL_READY = "varn.toa_baba_rockfall_ready"
    const val LAST_SPECIAL = "varn.toa_baba_last_special"
    const val UNDER = "varn.toa_baba_under"
}

internal object BabaSeqs {
    const val SPAWN = "seq.npc_mandrill_spawn01"
    const val MELEE = "seq.npc_baba_attack_melee"
    const val DEATH = "seq.npc_mandrill_despawn01"
    const val JUMP = "seq.npc_mandrill_attack_special_jump01"
    const val CAST = "seq.npc_mandrill_attack_special_ranged01"
    const val END = "seq.npc_mandrill_attack_special_end01"
    const val BOULDER_ROLL = "seq.toa_boulder_rolling"
    const val KNOCKDOWN = "seq.human_knockdown"
    const val FLYBACK = "seq.human_troll_flyback_merge"
    const val THROW = "seq.npc_mandrill_attack_ranged_01"
    const val DISCHARGE = "seq.npc_mandrill_attack_magic"
    const val BABOON_MELEE = "seq.npc_mandrill_attack_melee"
    const val BABOON_RANGED = "seq.npc_mandrill_attack_ranged_02"
}

internal object BabaSpots {
    const val BALLS_TRAVEL = "spotanim.toa_baba_balls_travel"
    const val STUNNED = "spotanim.stunned_thieving"
    const val ROCK_TRAVEL = "spotanim.toa_baba_ranged_travel"
    const val ENERGY = "spotanim.toa_baba_skull_proj"
    const val SHOCK_IMPACT = "spotanim.darkbow_smoke_arrow_impact"

    val SHOCK_TELEGRAPH =
        listOf(
            "spotanim.gargboss_debris_shadow_150",
            "spotanim.gargboss_debris_shadow_120",
            "spotanim.gargboss_debris_shadow_90",
            "spotanim.gargboss_debris_shadow_60",
        )

    val ROCK_FALL =
        listOf(
            "spotanim.toa_baba_rock_fall",
            "spotanim.toa_baba_rock_fall_faster",
            "spotanim.toa_baba_rock_fall_fastest",
        )
}

internal object BabaCoords {
    val BABA = CoordGrid(3815, 5406, 0)
}

internal object BabaRocks {
    const val BLOCKER = "loc.invisible_type8_blocking_size3"
    const val NORTH_Z = 5414
    const val SOUTH_Z = 5402
    const val EDGE_MIN_X = 3803
    const val EDGE_MAX_X = 3810
    const val BASE_FALL_TICKS = 6
    const val TICKS_PER_TIER = 2
    const val MARK_DELAY_CYCLES = 20
    const val IMPACT_DAMAGE = 19
    const val IMPACT_SPREAD = 5
    const val RUBBLE_HP_PER_PLAYER = 10
    const val THROW_DELAY = 13
    const val THROW_RESOLVE_DELAY = 6
    const val COOLDOWN = 50
    const val SPECIAL_AUTOS = 5
    const val CAST_MIN = 2
    const val CAST_MAX = 5
    const val THROW_DAMAGE = 24
    const val THROW_TEXT = "<col=ff289d>Ba-Ba throws a large boulder at you.</col>"
}

internal object BabaShock {
    const val LENGTH = 2
    const val LENGTH_SHAKING = 3
    const val TELEGRAPH_TICKS = 5
    const val STOMP_WARNING_TICKS = 2
    const val STOMP_TRIGGER_TICKS = 2
    const val BOSS_SIZE = 5
    const val BOSS_CENTRE = 2
    const val TELEGRAPH_STEP_CYCLES = 30
    const val IMPACT_STEP_CYCLES = 6
    const val BASE_DAMAGE = 17
    const val DAMAGE_SPREAD = 5
    const val FALLOFF_PERCENT = 10
    const val ATTACK_PAUSE = 3
}

internal object BabaRolling {
    const val CASTS = 10
    const val CAST_GAP = 8
    const val LANES = 5
    const val LANE_SIZE = 3
    const val LANE_BASE_Z = 5401
    const val ROW_X = 3813
    const val WALL_X = 3798
    const val KNOCKBACK_TILES = 4
    const val KNOCKBACK_IMMUNITY = 2
    const val FLYBACK_CYCLES = 120
    const val FLYBACK_HEIGHT = 124
    const val TOP_ZONE_X = 3815
    const val BOULDER_BASE_DAMAGE = 14
    const val BOULDER_DASH_GAP = 4
    const val PEEL_STUN_TICKS = 5
    val BOULDER_HP_BY_PARTY = intArrayOf(25, 36, 47, 55, 62, 70, 77, 85)
    val BOULDER_HP_PATH_PERCENT = intArrayOf(100, 110, 125)
    const val BOULDER_DAMAGE_SPREAD = 4
    const val WEAK_LANE_SPREAD = 2
    const val ROW_DELAY = 2
    const val SCREAM_TEXT = "<col=ff289d>Ba-Ba screams and knocks you back!</col>"
}

internal object BabaMidis {
    const val FIGHT = "midi.toa_boss_baba"
}

internal object BabaPhases {
    const val NORMAL = "normal"
    const val ROLLING = "rolling"
    const val NORMAL_2 = "normal_2"
    const val FINAL = "final"
}

internal object BabaAbilities {
    const val AUTO = "auto"
    const val ROCK_THROW = "rock_throw"
    const val ROLL_ENTRY = "roll_entry"
    const val ROLL_END = "roll_end"
    const val ROLL_FLAG = "roll_flag"
    const val SPECIAL = "special"
    const val ROCKFALL = "rockfall"
    const val SHOCKWAVE = "shockwave"
    const val STOMP = "stomp"
    const val DISCHARGE = "discharge"
}

internal object BabaExternals {
    const val RETARGET = "baba.retarget"
    const val FLYBACK = "baba.flyback"
    const val BOULDER_ROW = "baba.boulder_row"
    const val ROLL_RESET = "baba.roll_reset"
    const val THROW_RESOLVE = "baba.throw_resolve"
    const val RUBBLE_SPAWN = "baba.rubble_spawn"
    const val BABOONS_SPAWN = "baba.baboons_spawn"
}
