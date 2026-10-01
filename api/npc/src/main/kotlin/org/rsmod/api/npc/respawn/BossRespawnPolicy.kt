package org.rsmod.api.npc.respawn

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType

/** Only actual bosses belong here: the BossDSL is also used by ordinary monsters and minions. */
public object BossRespawnPolicy {
    public const val TICK_MILLIS: Int = 600
    public const val GOD_WARS_TICKS: Int = 100

    // A tick cannot represent 20 seconds exactly. Round up, never respawn before 20 seconds.
    public const val OTHER_BOSS_TICKS: Int = 34

    private val godWars: Set<String> = setOf(
        "npc.godwars_bandos_avatar", "npc.godwars_armadyl_avatar",
        "npc.godwars_zamorak_avatar", "npc.godwars_saradomin_avatar",
        "npc.nex", "npc.nex_spawning", "npc.nex_soulsplit", "npc.nex_deflect", "npc.nex_dying",
    )
    private val other: Set<String> = setOf(
        "npc.king_dragon", "npc.callisto", "npc.callisto_singles",
        "npc.venenatis", "npc.venenatis_singles", "npc.vetion", "npc.vetion_2",
        "npc.vetion_single", "npc.vetion_2_single", "npc.chaoselemental",
        "npc.chaos_fanatic", "npc.crazy_archaeologist", "npc.scorpia",
        "npc.rat_boss_normal", "npc.rat_boss_instance", "npc.amoxliatl",
        "npc.dagcave_magic_boss", "npc.dagcave_melee_boss", "npc.dagcave_ranged_boss",
        "npc.corp_beast", "npc.mole_giant", "npc.fossil_crazy_archaeologist",
        "npc.cerberus_sitting", "npc.cerberus_attacking", "npc.smoke_devil_boss",
        "npc.slayer_kraken_boss", "npc.kalphite_queen", "npc.kalphite_flyingqueen",
        "npc.gargboss_dusk_spawn", "npc.gargboss_dusk_phase1_defensive",
        "npc.gargboss_dusk_phase2_attacking", "npc.gargboss_dusk_phase4",
        "npc.gargboss_dusk_death", "npc.hydraboss",
        "npc.hydraboss_2", "npc.hydraboss_3", "npc.hydraboss_4", "npc.hydraboss_finaldeath",
        "npc.sarachnis", "npc.zalcano", "npc.zalcano_weak",
        "npc.muspah", "npc.muspah_melee", "npc.muspah_teleport",
        "npc.muspah_soulsplit", "npc.muspah_final", "npc.leviathan", "npc.vardorvis",
        "npc.whisperer_spawn", "npc.whisperer", "npc.whisperer_melee",
        "npc.duke_sucellus_awake", "npc.duke_sucellus_asleep", "npc.duke_sucellus_dead",
        "npc.snakeboss_boss_ranged", "npc.snakeboss_boss_melee", "npc.snakeboss_boss_magic",
        "npc.vorkath", "npc.vorkath_sleeping", "npc.vorkath_sleeping_noop",
        "npc.araxxor", "npc.araxxor_dead",
        "npc.huey_head", "npc.huey_head_defeated", "npc.gryphon_boss", "npc.cowboss",
        "npc.mad_angel", "npc.mad_angel_dead",
        "npc.hillgiant_boss", "npc.gb_mossgiant", "npc.dom_boss", "npc.yama",
        "npc.abyssalsire_sire_stasis_sleeping", "npc.abyssalsire_sire_stasis_awake",
        "npc.abyssalsire_sire_stasis_stunned", "npc.abyssalsire_sire_puppet",
        "npc.abyssalsire_sire_wandering", "npc.abyssalsire_sire_panicking",
        "npc.abyssalsire_sire_apocalypse",
        "npc.nightmare_initial", "npc.nightmare_phase_01", "npc.nightmare_phase_02",
        "npc.nightmare_phase_03", "npc.nightmare_dying",
        "npc.nightmare_challenge_initial", "npc.nightmare_challenge_phase_01",
        "npc.nightmare_challenge_phase_02", "npc.nightmare_challenge_phase_03",
        "npc.nightmare_challenge_phase_04", "npc.nightmare_challenge_phase_05",
        "npc.nightmare_challenge_dying",
    )
    private val resolved: Map<Int, Int> by lazy {
        buildMap {
            for (name in godWars + other) {
                // Revision-specific/unfinished content may have no definition in this cache.
                val id = RSCM.getRSCMOrNull(name, RSCMType.NPC) ?: continue
                if (id >= 0) put(id, requireNotNull(ticksForSymbol(name)))
            }
        }
    }

    public fun ticksForSymbol(symbol: String): Int? = when (symbol) {
        in godWars -> GOD_WARS_TICKS
        in other -> OTHER_BOSS_TICKS
        else -> null
    }

    public fun ticksFor(npcTypeId: Int): Int? = resolved[npcTypeId]
}
