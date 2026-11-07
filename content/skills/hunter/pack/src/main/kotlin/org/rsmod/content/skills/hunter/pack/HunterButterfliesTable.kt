package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object HunterButterfliesTable {
    const val NPC = 0
    const val CREATURE = 1
    const val JAR = 2
    const val KIND = 3
    const val LEVEL = 4
    const val XP = 5
    const val NET_LOW = 6
    const val NET_HIGH = 7
    const val BARE_LOW = 8
    const val BARE_HIGH = 9
    const val MAGIC_LOW = 10
    const val MAGIC_HIGH = 11
    const val EFFECT = 12
    const val EFFECT_STAT = 13
    const val EFFECT_CONSTANT = 14
    const val EFFECT_PERCENT = 15
    const val HITPOINTS_HEAL = 16

    const val EFFECT_BOOST = 0
    const val EFFECT_HEAL = 1
    const val EFFECT_RESTORE = 2

    private data class Row(
        val key: String,
        val npc: String,
        val creature: String,
        val jar: String,
        val kind: String,
        val level: Int,
        val xp: Int,
        val chances: IntArray,
        val effect: Int,
        val stat: String,
        val constant: Int,
        val percent: Int,
        val hitpoints: Int,
    )

    private val BUTTERFLY = intArrayOf(20, 296, 40, 316, 40, 316)
    private val SUNLIGHT = intArrayOf(20, 296, 20, 296, 40, 316)
    private val MOONLIGHT = intArrayOf(0, 276, 0, 276, 20, 286)

    private val ROWS =
        listOf(
            Row("ruby_harvest", "npc.butterfly_ruby", "npc.butterfly_ruby", "obj.butterfly_jar_ruby", "butterfly", 15, 24, BUTTERFLY, EFFECT_BOOST, "stat.attack", 4, 15, 0),
            Row("sapphire_glacialis", "npc.butterfly_glacialis", "npc.butterfly_glacialis", "obj.butterfly_jar_glacialis", "butterfly", 25, 34, BUTTERFLY, EFFECT_BOOST, "stat.defence", 4, 15, 0),
            Row("snowy_knight", "npc.butterfly_snowy", "npc.butterfly_snowy", "obj.butterfly_jar_snowy", "butterfly", 35, 44, BUTTERFLY, EFFECT_HEAL, "stat.hitpoints", 15, 0, 0),
            Row("black_warlock", "npc.butterfly_warlock", "npc.butterfly_warlock", "obj.butterfly_jar_warlock", "butterfly", 45, 54, BUTTERFLY, EFFECT_BOOST, "stat.strength", 4, 15, 0),
            Row("sunlight_moth", "npc.moth_sunlight", "npc.moth_sunlight", "obj.butterfly_jar_sunmoth", "moth", 65, 74, SUNLIGHT, EFFECT_RESTORE, "stat.hitpoints", 6, 20, 8),
            Row("moonlight_moth", "npc.moth_moonlight", "npc.moth_moonlight", "obj.butterfly_jar_moonmoth", "moth", 75, 84, MOONLIGHT, EFFECT_HEAL, "stat.prayer", 22, 0, 0),
            Row("moonlight_moth_low_wander", "npc.moth_moonlight_low_wander", "npc.moth_moonlight", "obj.butterfly_jar_moonmoth", "moth", 75, 84, MOONLIGHT, EFFECT_HEAL, "stat.prayer", 22, 0, 0),
        )

    fun hunterButterflies() =
        dbTable("dbtable.hunter_butterflies", serverOnly = true) {
            column("npc", NPC, VarType.NPC)
            column("creature", CREATURE, VarType.NPC)
            column("jar", JAR, VarType.OBJ)
            column("kind", KIND, VarType.STRING)
            column("level", LEVEL, VarType.INT)
            column("xp", XP, VarType.INT)
            column("net_low", NET_LOW, VarType.INT)
            column("net_high", NET_HIGH, VarType.INT)
            column("bare_low", BARE_LOW, VarType.INT)
            column("bare_high", BARE_HIGH, VarType.INT)
            column("magic_low", MAGIC_LOW, VarType.INT)
            column("magic_high", MAGIC_HIGH, VarType.INT)
            column("effect", EFFECT, VarType.INT)
            column("effect_stat", EFFECT_STAT, VarType.STAT)
            column("effect_constant", EFFECT_CONSTANT, VarType.INT)
            column("effect_percent", EFFECT_PERCENT, VarType.INT)
            column("hitpoints_heal", HITPOINTS_HEAL, VarType.INT)

            for (r in ROWS) {
                row("dbrow.hunter_butterfly_${r.key}") {
                    columnRSCM(NPC, r.npc)
                    columnRSCM(CREATURE, r.creature)
                    columnRSCM(JAR, r.jar)
                    column(KIND, r.kind)
                    column(LEVEL, r.level)
                    column(XP, r.xp)
                    column(NET_LOW, r.chances[0])
                    column(NET_HIGH, r.chances[1])
                    column(BARE_LOW, r.chances[2])
                    column(BARE_HIGH, r.chances[3])
                    column(MAGIC_LOW, r.chances[4])
                    column(MAGIC_HIGH, r.chances[5])
                    column(EFFECT, r.effect)
                    columnRSCM(EFFECT_STAT, r.stat)
                    column(EFFECT_CONSTANT, r.constant)
                    column(EFFECT_PERCENT, r.percent)
                    column(HITPOINTS_HEAL, r.hitpoints)
                }
            }
        }
}
