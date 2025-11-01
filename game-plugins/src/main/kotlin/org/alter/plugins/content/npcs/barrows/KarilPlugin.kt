package org.alter.plugins.content.npcs.barrows

import org.alter.api.*
import org.alter.api.cfg.*
import org.alter.api.dsl.*
import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.model.attr.*
import org.alter.game.model.container.*
import org.alter.game.model.container.key.*
import org.alter.game.model.entity.*
import org.alter.game.model.item.*
import org.alter.game.model.queue.*
import org.alter.game.model.shop.*
import org.alter.game.model.timer.*
import org.alter.game.plugin.*

class KarilPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
        
    init {
        spawnNpc("npcs.barrows_karil", 3565, 3275, 0, 2)
        spawnNpc("npcs.barrows_karil", 3563, 3272, 0, 2)
        spawnNpc("npcs.barrows_karil", 3563, 3278, 0, 2)
        spawnNpc("npcs.barrows_karil", 3567, 3272, 0, 2)
        spawnNpc("npcs.barrows_karil", 3567, 3278, 0, 2)

        setCombatDef("npcs.barrows_karil") {
            configs {
                attackSpeed = 6
                respawnDelay = 50
            }

            stats {
                hitpoints = 100
                magic = 100
                defence = 100
            }

            anims {
                attack = "sequences.human_caststun"
                block = "sequences.barrows_quarterstaff_defend"
                death = "sequences.champions_zombie_death"
            }
        }
    }
}
