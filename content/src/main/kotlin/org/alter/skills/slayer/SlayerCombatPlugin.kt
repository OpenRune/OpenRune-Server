package org.alter.skills.slayer

import org.alter.api.Skills
import org.alter.api.ext.*
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.game.Server
import org.alter.game.model.World

/**
 * Handles slayer-related combat events:
 * - Tracking kills for slayer tasks
 * - Awarding slayer XP
 * - Superior creature spawning
 */
class SlayerCombatPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {
        // Hook into any NPC death to check for slayer task progress
        onAnyNpcDeath {
            val npc = ctx as Npc
            val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return@onAnyNpcDeath

            handleSlayerKill(killer, npc)
        }
    }

    /**
     * Handle a potential slayer kill.
     */
    private fun handleSlayerKill(player: Player, npc: Npc) {
        // Check if player has an active slayer task
        if (!SlayerManager.hasTask(player)) return

        // Get current task
        val taskRow = SlayerManager.getCurrentTask(player) ?: return

        // Check if this NPC counts towards the task
        if (!isValidSlayerTarget(npc, taskRow.id)) return

        // Check area restriction for Konar tasks
        val areaId = SlayerManager.getTaskArea(player)
        if (areaId > 0 && !isInTaskArea(player, npc, areaId)) {
            return
        }

        // Award slayer XP (equal to NPC's hitpoints)
        val slayerXp = npc.combatDef.hitpoints.toDouble()
        if (slayerXp > 0) {
            player.addXp(Skills.SLAYER, slayerXp)
        }

        // Decrement task count
        val remaining = SlayerManager.getRemainingKills(player) - 1
        player.setVarp("varp.slayer_count", remaining.coerceAtLeast(0))

        // Check for task completion
        if (remaining <= 0) {
            completeSlayerTask(player)
        } else {
            // Notify every 10 kills
            if (remaining % 10 == 0 || remaining <= 10) {
                player.filterableMessage("You still need to kill $remaining more ${taskRow.nameLowercase}.")
            }
        }

        // Chance to spawn superior creature (if unlocked)
        // Handled by SuperiorCreaturePlugin
    }

    /**
     * Check if an NPC is a valid target for the given task ID.
     * This maps NPC IDs to slayer task categories.
     */
    private fun isValidSlayerTarget(npc: Npc, taskId: Int): Boolean {
        // Get the task row to find what monsters count
        val taskRow = SlayerDefinitions.tasks.find { it.id == taskId } ?: return false

        // Check task sublists for variant monsters
        val sublists = SlayerDefinitions.taskSublists.filter { it.task == taskId }

        // If there are sublists, check if NPC matches any
        if (sublists.isNotEmpty()) {
            // For now, we do a name-based check as a fallback
            // Real implementation would need NPC ID to task sublist mapping
            return npcMatchesTaskByName(npc, taskRow.nameLowercase)
        }

        // No sublists - check by name matching
        return npcMatchesTaskByName(npc, taskRow.nameLowercase)
    }

    /**
     * Check if an NPC matches a task by name.
     * This is a fallback for when proper NPC-to-task mapping isn't available.
     */
    private fun npcMatchesTaskByName(npc: Npc, taskName: String): Boolean {
        val npcName = npc.name.lowercase()
        val taskLower = taskName.lowercase()

        // Handle pluralization and common variations
        val singularTask = taskLower.removeSuffix("s").removeSuffix("ie").removeSuffix("e")

        return npcName.contains(singularTask) ||
               singularTask.contains(npcName) ||
               matchesTaskCategory(npcName, taskLower)
    }

    /**
     * Check if NPC matches a task category (for grouped tasks like "demons").
     */
    private fun matchesTaskCategory(npcName: String, taskName: String): Boolean {
        // Map task categories to NPC names that count
        val categoryMappings = mapOf(
            "demons" to listOf("demon", "imp", "lesser", "greater", "black demon", "abyssal"),
            "dragons" to listOf("dragon", "drake", "wyrm", "wyvern"),
            "kalphite" to listOf("kalphite", "kq"),
            "dagannoth" to listOf("dagannoth", "dag"),
            "tzhaar" to listOf("tzhaar", "tz-", "jad"),
            "trolls" to listOf("troll", "ice troll", "mountain troll"),
            "vampyres" to listOf("vampyre", "vyrewatch", "feral"),
            "undead" to listOf("zombie", "skeleton", "ghost", "shade", "revenant"),
            "bears" to listOf("bear", "grizzly", "black bear"),
            "dogs" to listOf("dog", "jackal", "hound", "hellhound"),
            "wolves" to listOf("wolf", "white wolf"),
            "birds" to listOf("chicken", "seagull", "bird", "terrorbird"),
            "goblins" to listOf("goblin", "hobgoblin"),
            "spiders" to listOf("spider", "venenatis", "sarachnis"),
            "bats" to listOf("bat", "giant bat"),
            "ghosts" to listOf("ghost", "tortured soul"),
            "skeletons" to listOf("skeleton"),
            "zombies" to listOf("zombie"),
            "giants" to listOf("giant", "hill giant", "moss giant", "fire giant", "ice giant"),
            "hellhounds" to listOf("hellhound", "cerberus"),
            "black demons" to listOf("black demon", "demonic gorilla", "skotizo"),
            "greater demons" to listOf("greater demon", "k'ril", "zammy"),
            "abyssal demons" to listOf("abyssal demon", "sire"),
            "dark beasts" to listOf("dark beast"),
            "gargoyles" to listOf("gargoyle", "grotesque"),
            "nechryael" to listOf("nechryael", "nech"),
            "dust devils" to listOf("dust devil"),
            "kurasks" to listOf("kurask"),
            "turoths" to listOf("turoth"),
            "cave horrors" to listOf("cave horror"),
            "aberrant spectres" to listOf("aberrant spectre", "deviant spectre"),
            "basilisks" to listOf("basilisk"),
            "cockatrices" to listOf("cockatrice"),
            "cave crawlers" to listOf("cave crawler"),
            "banshees" to listOf("banshee", "twisted banshee"),
            "bloodvelds" to listOf("bloodveld", "mutated bloodveld"),
            "fire giants" to listOf("fire giant"),
            "ice giants" to listOf("ice giant"),
            "moss giants" to listOf("moss giant", "bryophyta"),
            "hill giants" to listOf("hill giant", "obor"),
            "blue dragons" to listOf("blue dragon", "baby blue dragon", "vorkath"),
            "black dragons" to listOf("black dragon", "baby black dragon", "kbd", "king black"),
            "red dragons" to listOf("red dragon", "baby red dragon"),
            "green dragons" to listOf("green dragon", "baby green dragon"),
            "bronze dragons" to listOf("bronze dragon"),
            "iron dragons" to listOf("iron dragon"),
            "steel dragons" to listOf("steel dragon"),
            "mithril dragons" to listOf("mithril dragon"),
            "adamant dragons" to listOf("adamant dragon"),
            "rune dragons" to listOf("rune dragon"),
            "aviansies" to listOf("aviansie", "kree", "flight"),
            "spiritual creatures" to listOf("spiritual mage", "spiritual ranger", "spiritual warrior"),
            "suqahs" to listOf("suqah"),
            "cave kraken" to listOf("kraken", "cave kraken"),
            "smoke devils" to listOf("smoke devil", "thermonuclear"),
            "fossil island wyverns" to listOf("spitting wyvern", "taloned wyvern", "long-tailed wyvern", "ancient wyvern"),
            "skeletal wyverns" to listOf("skeletal wyvern"),
            "wyrms" to listOf("wyrm"),
            "drakes" to listOf("drake"),
            "hydras" to listOf("hydra", "alchemical"),
        )

        categoryMappings[taskName]?.let { validNames ->
            return validNames.any { npcName.contains(it) }
        }

        return false
    }

    /**
     * Check if the player is in the correct area for Konar tasks.
     */
    private fun isInTaskArea(player: Player, npc: Npc, areaId: Int): Boolean {
        val area = SlayerDefinitions.getAreaById(areaId) ?: return false

        // Area checking would require coordinate/region validation
        // For now, we'll allow it (needs proper implementation)
        // TODO: Implement proper area coordinate checking
        return true
    }

    /**
     * Complete the slayer task and award rewards.
     */
    private fun completeSlayerTask(player: Player) {
        val masterId = SlayerManager.getCurrentMaster(player)
        val streak = SlayerManager.getTaskStreak(player) + 1
        SlayerManager.setTaskStreak(player, streak)

        // Calculate and award points
        val points = SlayerDefinitions.calculatePoints(masterId, streak)

        if (points > 0) {
            SlayerManager.addPoints(player, points)
            player.message("You've completed your task! You've been awarded $points Slayer reward points.")
        } else {
            player.message("You've completed your Slayer task!")
        }

        // Clear task
        SlayerManager.clearTask(player)

        // Announce streak milestones
        when {
            streak % 1000 == 0 -> player.message("You've completed $streak Slayer tasks in a row!")
            streak % 250 == 0 -> player.message("You've completed $streak Slayer tasks in a row!")
            streak % 100 == 0 -> player.message("You've completed $streak Slayer tasks in a row!")
            streak % 50 == 0 -> player.message("You've completed $streak Slayer tasks in a row!")
            streak % 10 == 0 -> player.message("You've completed $streak Slayer tasks in a row!")
        }

        player.message("You now have ${SlayerManager.getPoints(player)} Slayer reward points.")
    }

}

