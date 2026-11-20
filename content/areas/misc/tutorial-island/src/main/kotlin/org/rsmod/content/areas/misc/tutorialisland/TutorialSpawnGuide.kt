package org.rsmod.content.areas.misc.tutorialisland

import jakarta.inject.Inject
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.game.entity.NpcList
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class TutorialSpawnGuide @Inject constructor(private val npcList: NpcList) :
    PluginScript() {

    override fun ScriptContext.startup() {
        onPlayerLogin {
            if (!player.inTutorial) {
                return@onPlayerLogin
            }
            apply()
        }
    }

    private fun apply() {
        for (npc in npcList) {
            val type = npc.type.internalName
            val visible = npc.visType.internalName
            val spec =
                SPAWNS[type]?.get(npc.spawnCoords) ?: SPAWNS[visible]?.get(npc.spawnCoords) ?: continue
            npc.respawnDir = spec.facing
            spec.wander?.let { npc.wanderRange = it }
        }
    }

    private data class SpawnSpec(val facing: Direction, val wander: Int? = null)

    private companion object {
        private fun spawn(
            x: Int,
            z: Int,
            facing: Direction,
            wander: Int? = null,
        ): Pair<CoordGrid, SpawnSpec> = CoordGrid(x, z, 0) to SpawnSpec(facing, wander)

        private val SPAWNS: Map<String, Map<CoordGrid, SpawnSpec>> =
            mapOf(
                "npc.0_48_48_newbiefishing" to
                    mapOf(
                        spawn(3099, 3090, Direction.South, 0),
                        spawn(3101, 3092, Direction.South, 0),
                        spawn(3103, 3092, Direction.South, 0),
                    ),
                "npc.newbie_basics_instructor" to
                    mapOf(spawn(3095, 3107, Direction.SouthEast, 2)),
                "npc.newbie_survival_instructor" to
                    mapOf(spawn(3103, 3095, Direction.South, 2)),
                "npc.newbie_cook_instructor" to
                    mapOf(spawn(3075, 3085, Direction.East, 2)),
                "npc.newbie_quest_instructor" to
                    mapOf(spawn(3087, 3123, Direction.North, 0)),
                "npc.newbie_mining_instructor" to
                    mapOf(spawn(3081, 9504, Direction.West, 2)),
                "npc.newbie_combat_instructor" to
                    mapOf(spawn(3106, 9509, Direction.North, 2)),
                "npc.newbie_account_instructor" to
                    mapOf(spawn(3127, 3124, Direction.South, 1)),
                "npc.newbie_magic_instructor" to
                    mapOf(spawn(3141, 3088, Direction.West, 1)),
                "npc.brother_noob" to
                    mapOf(spawn(3125, 3106, Direction.East, 2)),
                "npc.ironman_tutor_2" to
                    mapOf(spawn(3131, 3086, Direction.NorthWest, 1)),
                "npc.ironman_tutor_2_visible" to
                    mapOf(spawn(3131, 3086, Direction.NorthWest, 1)),
                "npc.noobbanker" to
                    mapOf(
                        spawn(3120, 3125, Direction.South, 0),
                        spawn(3122, 3125, Direction.SouthEast, 0),
                    ),
                "npc.newbiegiantrat" to
                    mapOf(
                        spawn(3098, 9519, Direction.South),
                        spawn(3099, 9514, Direction.North),
                        spawn(3100, 9517, Direction.West),
                        spawn(3100, 9521, Direction.East),
                        spawn(3102, 9513, Direction.East),
                        spawn(3103, 9518, Direction.North),
                        spawn(3103, 9521, Direction.South),
                        spawn(3104, 9512, Direction.North),
                        spawn(3105, 9514, Direction.East),
                        spawn(3105, 9518, Direction.East),
                        spawn(3107, 9516, Direction.South),
                        spawn(3107, 9521, Direction.East),
                        spawn(3109, 9518, Direction.East),
                    ),
                "npc.newbiechicken" to
                    mapOf(
                        spawn(3138, 3093, Direction.West),
                        spawn(3138, 3095, Direction.South),
                        spawn(3140, 3093, Direction.East),
                        spawn(3140, 3094, Direction.South),
                        spawn(3140, 3096, Direction.North),
                    ),
                "npc.butterfly" to
                    mapOf(
                        spawn(3082, 3077, Direction.South),
                        spawn(3092, 3082, Direction.East),
                        spawn(3109, 3090, Direction.NorthEast),
                        spawn(3112, 3093, Direction.East),
                    ),
                "npc.butterfly2" to
                    mapOf(spawn(3125, 3084, Direction.SouthWest)),
                "npc.butterfly_bluemorpo" to
                    mapOf(
                        spawn(3093, 3086, Direction.North),
                        spawn(3097, 3085, Direction.West),
                        spawn(3129, 3091, Direction.South),
                        spawn(3135, 3084, Direction.North),
                    ),
            )
    }
}
