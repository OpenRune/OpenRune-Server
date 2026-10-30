package org.rsmod.content.bosses.hueycoatl

import org.rsmod.map.CoordGrid

internal object HueycoatlNpcs {
    const val HEAD = "npc.huey_head"
    const val HEAD_INVULNERABLE = "npc.huey_head_invulnerable"
    const val HEAD_DEFEATED = "npc.huey_head_defeated"
    const val HEAD_ENRAGED = "npc.huey_head_enraged"
    const val HEAD_RESPAWN_PLACEHOLDER = "npc.huey_head_respawn_placeholder"
    const val TAIL = "npc.huey_tail"
    const val TAIL_BROKEN = "npc.huey_tail_broken"
    const val BODY = "npc.huey_body_part"
    const val BODY_BROKEN = "npc.huey_body_part_broken"
    const val SEER = "npc.huey_seer"
    const val VICTORY_WORKER_1 = "npc.huey_victory_worker_1"
    const val VICTORY_WORKER_2 = "npc.huey_victory_worker_2"
    const val HUEY_PET = "npc.huey_pet"
    const val LOBBY_LEADER = "npc.huey_lobby_leader"
    const val LOBBY_SEER = "npc.huey_lobby_seer"
    const val LOBBY_WORKER_1 = "npc.huey_lobby_worker_1"
    const val LOBBY_WORKER_2 = "npc.huey_lobby_worker_2"
    const val LOBBY_CHEF = "npc.huey_lobby_chef_multibase"
    const val LOBBY_GUARD = "npc.huey_lobby_guard"
    const val LOBBY_GUARD_VIERO = "npc.huey_lobby_guard_viero_multibase"
    const val NELTA = "npc.huey_nelta"

    val LOBBY_NPCS =
        listOf(
            LOBBY_LEADER,
            LOBBY_SEER,
            LOBBY_WORKER_1,
            LOBBY_WORKER_2,
            LOBBY_CHEF,
            LOBBY_GUARD,
            LOBBY_GUARD_VIERO,
            NELTA,
        )
}

internal object HueycoatlLocs {
    const val BARRIER = "loc.huey_p2_barrier"
    const val BARRIER_RETRACTED = "loc.huey_p2_barrier_retracted"
    const val BARRIER_RESPAWN = "loc.huey_p2_barrier_respawn"
    const val ATTACK_TILE = "loc.huey_attack_tiles_loc"
    const val BRAZIER_ON_MAGIC = "loc.huey_prayer_pillar_magic_brazier_on"
    const val BRAZIER_OFF_MAGIC = "loc.huey_prayer_pillar_magic_brazier_off"
    const val EMPTY_MAGIC = "loc.huey_prayer_pillar_magic_empty"
    const val BRAZIER_ON_RANGED = "loc.huey_prayer_pillar_ranged_brazier_on"
    const val BRAZIER_OFF_RANGED = "loc.huey_prayer_pillar_ranged_brazier_off"
    const val EMPTY_RANGED = "loc.huey_prayer_pillar_ranged_empty"
    const val BRAZIER_ON_MELEE = "loc.huey_prayer_pillar_melee_brazier_on"
    const val BRAZIER_OFF_MELEE = "loc.huey_prayer_pillar_melee_brazier_off"
    const val EMPTY_MELEE = "loc.huey_prayer_pillar_melee_empty"
    const val CAVE = "loc.df_cave_snow01_entrance03"
    const val CAVE_DIAG = "loc.df_cave_snow01_entrance03_diag"
    const val CAVE_HUEY = "loc.df_cave_snow01_entrance03_huey"
    const val CAVE_HUEY_RESPAWN = "loc.df_cave_snow01_entrance03_huey_respawn"
    const val CAVE_DIAG_HUEY = "loc.df_cave_snow01_entrance03_diag_huey"
    const val CAVE_DIAG_HUEY_RESPAWN = "loc.df_cave_snow01_entrance03_diag_huey_respawn"
    const val FIGHT_ENTRANCE = "loc.huey_fight_entrance"
    const val EXIT_SHORTCUT = "loc.huey_fight_exit_shortcut"
    const val MEETING_FLAG = "loc.huey_holding_instance_creator"
    const val MEETING_FLAG_INSTANCED = "loc.huey_holding_instance_creator_instanced"
    const val HOLDING_ENTRANCE = "loc.huey_holding_entrance_instanced"
    const val SCOREBOARD = "loc.huey_scoreboard"
}

internal object HueycoatlSeqs {
    const val ATTACK_MELEE = "seq.huey_attack_melee"
    const val ATTACK_RANGED = "seq.huey_attack_range"
    const val ATTACK_MAGIC = "seq.huey_attack_magic"
    const val SHIELD_MELEE = "seq.huey_shield_attack_melee"
    const val SHIELD_RANGED = "seq.huey_shield_attack_range"
    const val SHIELD_MAGIC = "seq.huey_shield_attack_magic"
    const val SHIELD_ACTIVATION = "seq.huey_shield_activation"
    const val SCREECH = "seq.huey_screech"
    const val SCREECH_RESET = "seq.huey_screech_reset"
    const val KNOCKOUT = "seq.huey_knockout"
    const val AWAKEN = "seq.huey_awaken"
    const val AWAKEN_RESET = "seq.huey_awaken_reset"
    const val TAIL_APPEAR = "seq.npc_huey_tail_appear_01"
    const val TAIL_DISAPPEAR = "seq.npc_huey_tail_disappear_01"
    const val TAIL_BREAK = "seq.npc_huey_tail_break_01"
    const val BODY_DEATH = "seq.huey_body_death"
    const val BODY_DEATH_DIAG = "seq.huey_body_death_diag"
    const val BARRIER_TRANSITION = "seq.huey_p2_barrier_transition"
    const val BARRIER_DAMAGE = "seq.huey_p2_barrier_damage_01"
    const val HUMAN_KNOCKDOWN = "seq.human_knockdown_loop"
    const val HUMAN_GETUP = "seq.human_getup"
}

internal object HueycoatlGfx {
    const val MELEE_LAUNCH = "spotanim.vfx_huey_attack_melee_spotanim_01"
    const val MELEE_PROJECTILE = "spotanim.vfx_huey_attack_melee_projanim_01"
    const val MELEE_IMPACT = "spotanim.vfx_huey_attack_melee_impactanim_01"
    const val RANGED_LAUNCH = "spotanim.vfx_huey_attack_ranged_spotanim_01"
    const val RANGED_PROJECTILE = "spotanim.vfx_huey_attack_ranged_projanim_01"
    const val RANGED_IMPACT = "spotanim.vfx_huey_attack_ranged_impactanim_01"
    const val MAGIC_LAUNCH = "spotanim.vfx_huey_attack_magic_spotanim_01"
    const val MAGIC_PROJECTILE = "spotanim.vfx_huey_attack_magic_projanim_01"
    const val MAGIC_IMPACT = "spotanim.vfx_huey_attack_magic_impactanim_01"
    const val PRAYER = "spotanim.vfx_hueycoatl_prayer_02"
    const val LOOT_BEAM = "spotanim.vfx_huey_loot_beam"
    const val TAIL_WAVE_SOUTH = "spotanim.vfx_huey_tail_slam_shockwave_south"
    const val TAIL_WAVE_SOUTHWEST = "spotanim.vfx_huey_tail_slam_shockwave_southwest"
    const val TAIL_WAVE_WEST = "spotanim.vfx_huey_tail_slam_shockwave_west"
    const val TAIL_WAVE_NORTHWEST = "spotanim.vfx_huey_tail_slam_shockwave_northwest"
    const val TAIL_WAVE_NORTH = "spotanim.vfx_huey_tail_slam_shockwave_north"
    const val TAIL_WAVE_NORTHEAST = "spotanim.vfx_huey_tail_slam_shockwave_northeast"
    const val TAIL_WAVE_EAST = "spotanim.vfx_huey_tail_slam_shockwave_east"
    const val TAIL_WAVE_SOUTHEAST = "spotanim.vfx_huey_tail_slam_shockwave_southeast"
}

internal object HueycoatlSounds {
    const val BODY_AMBIENT = 9594
    const val HEAD_AMBIENT = 9596
    const val LIGHTNING_IMPACT = 9612
    const val TAIL_SLAM = 9568
    const val TAIL_APPEAR = 9573
    const val BODY_DEATH = 9664
    const val BARRIER_TRANSITION = 9602
    const val SHIELD_ACTIVATION = 9609
    const val TAIL_BREAK = 9678
    const val HEAD_PROJECTILE = 9671
    const val SEER_CHARGE_A = 10313
    const val SEER_CHARGE_B = 10322
    const val SEER_CHARGE_C = 10319
    const val SEER_CHARGE_D = 10311
}

internal object HueycoatlCoords {
    val ARENA_ORIGIN = CoordGrid(1504, 3268)
    val HEAD = CoordGrid(1509, 3290)
    val SEER_BASE = CoordGrid(1530, 3284)
    val SEER_BODY = CoordGrid(1527, 3276)
    val SEER_WAIT = CoordGrid(1518, 3271)
    val SEER_SUMMIT = CoordGrid(1506, 3288)
    val SEER_BODY_PATH =
        listOf(
            CoordGrid(1525, 3275),
            CoordGrid(1523, 3273),
            CoordGrid(1521, 3272),
            CoordGrid(1519, 3272),
            CoordGrid(1518, 3271),
        )
    val SEER_START_PATH =
        listOf(
            CoordGrid(1530, 3282),
            CoordGrid(1530, 3280),
            CoordGrid(1529, 3278),
            CoordGrid(1527, 3276),
        )
    val SEER_SUMMIT_PATH =
        listOf(
            CoordGrid(1516, 3271),
            CoordGrid(1514, 3271),
            CoordGrid(1512, 3271),
            CoordGrid(1510, 3272),
            CoordGrid(1510, 3274),
            CoordGrid(1510, 3276),
            CoordGrid(1510, 3278),
            CoordGrid(1510, 3280),
            CoordGrid(1510, 3282),
            CoordGrid(1510, 3284),
            CoordGrid(1508, 3286),
            CoordGrid(1506, 3288),
        )
    val BARRIER = CoordGrid(1515, 3268)
    val TAIL_WEST = CoordGrid(1502, 3281)
    val TAIL_EAST = CoordGrid(1518, 3281)
    val ENTRY = CoordGrid(1528, 3284)
    val CHAIN_OUTSIDE = CoordGrid(1528, 3287)
    val EXIT = CoordGrid(1524, 3290)
    val RESET = CoordGrid(1530, 3283)

    val BODY_PARTS =
        listOf(
            CoordGrid(1520, 3273),
            CoordGrid(1524, 3277),
            CoordGrid(1527, 3273),
            CoordGrid(1530, 3276),
            CoordGrid(1524, 3270),
        )

    val MAGIC_PILLAR = CoordGrid(1504, 3287)
    val RANGED_PILLAR = CoordGrid(1505, 3289)
    val MELEE_PILLAR = CoordGrid(1507, 3290)

    val CAVE_BY_BODY =
        mapOf(
            CoordGrid(1530, 3276) to (CoordGrid(1530, 3275) to 1),
            CoordGrid(1524, 3277) to (CoordGrid(1523, 3276) to 3),
            CoordGrid(1527, 3273) to (CoordGrid(1527, 3271) to 1),
            CoordGrid(1520, 3273) to (CoordGrid(1519, 3273) to 0),
            CoordGrid(1524, 3270) to (CoordGrid(1523, 3269) to 2),
        )

    val CAVE_DIAGONAL = setOf(CoordGrid(1527, 3273))
}
