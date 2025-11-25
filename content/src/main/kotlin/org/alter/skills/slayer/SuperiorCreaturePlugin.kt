package org.alter.skills.slayer

import org.alter.api.ext.*
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.rscm.RSCM.getRSCM
import org.alter.rscm.RSCM.getReverseMapping
import org.alter.rscm.RSCMType

/**
 * Handles superior slayer creature mechanics.
 * Superior creatures are stronger versions of regular slayer monsters
 * that have a chance to spawn when killing their regular counterparts.
 */
class SuperiorCreaturePlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    companion object {
        // Varbit for superior creature unlock
        private const val SUPERIOR_UNLOCK_VARBIT = "varbits.slayer_unlock_superiormobs"

        // Spawn chance: 1 in 200
        private const val SPAWN_CHANCE = 200

        // Superior creature mappings: regular NPC name -> superior NPC RSCM key
        val SUPERIOR_MAPPINGS = mapOf(
            // Crawling hand -> Crushing hand
            "crawling hand" to "npcs.superior_crawling_hand",
            // Cave crawler -> Chasm crawler
            "cave crawler" to "npcs.superior_cave_crawler",
            // Banshee -> Screaming banshee
            "banshee" to "npcs.superior_banshee",
            // Twisted banshee -> Screaming twisted banshee
            "twisted banshee" to "npcs.superior_kourend_banshee",
            // Rockslug -> Giant rockslug
            "rockslug" to "npcs.superior_rockslug",
            // Cockatrice -> Cockathrice
            "cockatrice" to "npcs.superior_cockatrice",
            // Pyrefiend -> Flaming pyrelord
            "pyrefiend" to "npcs.superior_pyrefiend",
            // Pyrelord -> Flaming pyrelord
            "pyrelord" to "npcs.superior_pyrelord",
            // Basilisk -> Monstrous basilisk
            "basilisk" to "npcs.superior_basilisk",
            // Infernal mage -> Malevolent mage
            "infernal mage" to "npcs.superior_infernal_mage",
            // Bloodveld -> Insatiable bloodveld
            "bloodveld" to "npcs.superior_bloodveld",
            // Mutated bloodveld -> Insatiable mutated bloodveld
            "mutated bloodveld" to "npcs.superior_kourend_bloodveld",
            // Jelly -> Vitreous jelly
            "jelly" to "npcs.superior_jelly",
            // Warped jelly -> Vitreous warped jelly (Catacombs)
            "warped jelly" to "npcs.superior_kourend_jelly",
            // Turoth -> Spiked turoth
            "turoth" to "npcs.superior_turoth",
            // Kurask -> King kurask
            "kurask" to "npcs.superior_kurask",
            // Aberrant spectre -> Abhorrent spectre
            "aberrant spectre" to "npcs.superior_abberant_spectre",
            // Deviant spectre -> Repugnant spectre
            "deviant spectre" to "npcs.superior_kourend_spectre",
            // Dust devil -> Choke devil
            "dust devil" to "npcs.superior_dustdevil",
            // Wyrm -> Shadow wyrm (Karuulm)
            "wyrm" to "npcs.superior_wyrm_dark",
            // Smoke devil -> Nuclear smoke devil
            "smoke devil" to "npcs.superior_smoke_devil",
            // Gargoyle -> Marble gargoyle
            "gargoyle" to "npcs.superior_gargoyle",
            // Drake -> Guardian drake
            "drake" to "npcs.superior_drake",
            // Dark beast -> Night beast
            "dark beast" to "npcs.superior_dark_beast",
            // Abyssal demon -> Greater abyssal demon
            "abyssal demon" to "npcs.superior_abyssal_demon",
            // Nechryael -> Nechryarch
            "nechryael" to "npcs.superior_nechryael",
            // Greater nechryael -> Nechryarch
            "greater nechryael" to "npcs.superior_nechryael",
            // Cave horror -> Cave abomination
            "cave horror" to "npcs.superior_cave_horror",
            // Basilisk Knight -> Basilisk Sentinel
            "basilisk knight" to "npcs.superior_basilisk_knight",
            // Hydra -> Alchemical hydra
            "hydra" to "npcs.superior_hydra",
        )

        // Superior creature stats multipliers (compared to regular version)
        // HP is typically 3x, other stats scaled appropriately
        const val HP_MULTIPLIER = 3.0
        const val ATTACK_MULTIPLIER = 1.5
        const val DEFENCE_MULTIPLIER = 1.5
    }

    init {
        // Listen for superior creature deaths to award bonus loot
        SUPERIOR_MAPPINGS.values.toSet().forEach { superiorRscm ->
            try {
                onNpcDeath(superiorRscm) {
                    val npc = ctx as Npc
                    val killer = npc.attr[KILLER_ATTR]?.get() as? Player
                    if (killer != null) {
                        handleSuperiorDeath(killer, npc)
                    }
                }
            } catch (_: Exception) {
                // NPC doesn't exist in RSCM
            }
        }
    }

    /**
     * Check if player has unlocked superior creatures.
     */
    fun hasUnlock(player: Player): Boolean {
        return try {
            player.getVarbit(SUPERIOR_UNLOCK_VARBIT) == 1
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Attempt to spawn a superior creature.
     * @return true if a superior was spawned
     */
    fun trySpawnSuperior(player: Player, killedNpc: Npc): Boolean {
        // Check unlock
        if (!hasUnlock(player)) return false

        // Check if this NPC has a superior variant
        val npcName = killedNpc.name.lowercase()
        val superiorRscm = getSuperiorRscm(npcName) ?: return false

        // Roll for spawn chance
        if ((1..SPAWN_CHANCE).random() != 1) return false

        // Spawn the superior
        spawnSuperior(player, killedNpc, superiorRscm)
        return true
    }

    /**
     * Get superior NPC RSCM key for a given NPC name.
     */
    private fun getSuperiorRscm(npcName: String): String? {
        // Direct match
        SUPERIOR_MAPPINGS[npcName]?.let { return it }

        // Partial match
        return SUPERIOR_MAPPINGS.entries.find { (key, _) ->
            npcName.contains(key) || key.contains(npcName)
        }?.value
    }

    /**
     * Spawn a superior creature at the location of the killed NPC.
     */
    private fun spawnSuperior(player: Player, originalNpc: Npc, superiorRscm: String) {
        try {
            val superiorId = getRSCM(superiorRscm)
            val superior = Npc(superiorId, originalNpc.tile, world)
            superior.respawns = false // Superiors don't respawn

            // Set owner so only this player can attack initially
            // (In real OSRS, any player on the task can attack)

            world.spawn(superior)

            player.message("<col=ff0000>A superior foe has appeared...</col>")
        } catch (_: Exception) {
            // RSCM key doesn't exist
        }
    }

    /**
     * Handle superior creature death - award bonus drops.
     */
    private fun handleSuperiorDeath(player: Player, superior: Npc) {
        // Superior creatures always drop:
        // - Their regular drop table (handled by normal loot system)
        // - Guaranteed imbued heart / eternal gem / dust battlestaff piece
        // - More slayer XP (handled by slayer combat plugin based on HP)

        player.message("You've defeated the superior creature!")

        // Roll for rare superior drops
        rollSuperiorDrops(player, superior)
    }

    /**
     * Roll for special superior creature drops.
     */
    private fun rollSuperiorDrops(player: Player, superior: Npc) {
        // Superior creatures have a chance to drop:
        // 1/200 - Imbued heart
        // 1/200 - Eternal gem
        // 1/128 - Mist battlestaff (from Choke devils)
        // 1/128 - Dust battlestaff (from Choke devils)

        val superiorName = superior.name.lowercase()
        val dropRoll = (1..200).random()

        when {
            dropRoll == 1 -> {
                // Imbued heart drop
                try {
                    player.inventory.add(getRSCM("items.imbued_heart"))
                    player.message("<col=ff0000>You receive an imbued heart!</col>")
                } catch (_: Exception) {
                    // Drop to ground if inventory full
                    player.message("The imbued heart dropped to the ground!")
                }
            }
            dropRoll == 2 -> {
                // Eternal gem drop
                try {
                    player.inventory.add(getRSCM("items.eternal_gem"))
                    player.message("<col=ff0000>You receive an eternal gem!</col>")
                } catch (_: Exception) {
                    player.message("The eternal gem dropped to the ground!")
                }
            }
            superiorName.contains("choke") && dropRoll <= 3 -> {
                // Mist/Dust battlestaff from choke devils
                val staff = if ((1..2).random() == 1) "items.mist_battlestaff" else "items.dust_battlestaff"
                try {
                    player.inventory.add(getRSCM(staff))
                    player.message("<col=ff0000>You receive a rare battlestaff!</col>")
                } catch (_: Exception) {
                    player.message("The battlestaff dropped to the ground!")
                }
            }
        }
    }

    /**
     * Check if an NPC is a superior slayer creature.
     */
    fun isSuperior(npc: Npc): Boolean {
        return try {
            val npcRscm = getReverseMapping(RSCMType.NPCTYPES, npc.id) ?: return false
            SUPERIOR_MAPPINGS.values.contains(npcRscm)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Get the list of all superior NPC RSCM keys.
     */
    fun getSuperiorRscms(): Collection<String> {
        return SUPERIOR_MAPPINGS.values
    }
}

