package org.alter.skills.slayer

import org.alter.api.ext.*
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onItemOption
import org.alter.skills.slayer.SlayerDefinitions.SlayerMaster

/**
 * Enchanted Gem plugin - allows players to check their slayer task status
 * and communicate with their slayer master.
 */
class EnchantedGemPlugin : PluginEvent() {

    override fun init() {
        // Activate option on enchanted gem
        onItemOption("items.slayer_gem", "Activate") {
            player.queue { gemDialogue(player) }
        }

        // Also handle slayer ring and slayer helmet variants
        val slayerItems = listOf(
            "items.slayer_ring_1",
            "items.slayer_ring_2",
            "items.slayer_ring_3",
            "items.slayer_ring_4",
            "items.slayer_ring_5",
            "items.slayer_ring_6",
            "items.slayer_ring_7",
            "items.slayer_ring_8",
            "items.slayer_helm",
            "items.slayer_helm_i",
            "items.slayer_helm_black",
            "items.slayer_helm_black_i",
            "items.slayer_helm_green",
            "items.slayer_helm_green_i",
            "items.slayer_helm_red",
            "items.slayer_helm_red_i",
            "items.slayer_helm_purple",
            "items.slayer_helm_purple_i",
            "items.slayer_helm_turquoise",
            "items.slayer_helm_turquoise_i",
            "items.slayer_helm_hydra",
            "items.slayer_helm_hydra_i",
            "items.slayer_helm_twisted",
            "items.slayer_helm_twisted_i",
        )

        slayerItems.forEach { item ->
            try {
                onItemOption(item, "Check") {
                    showTaskStatus(player)
                }
            } catch (_: Exception) {
                // Item doesn't exist
            }
        }
    }

    /**
     * Main gem dialogue interface.
     */
    private suspend fun QueueTask.gemDialogue(player: Player) {
        val hasTask = SlayerManager.hasTask(player)
        val masterId = SlayerManager.getCurrentMaster(player)
        val master = SlayerMaster.fromId(masterId)

        if (!hasTask) {
            messageBox(player, "The gem is\"silent. You don't have a slayer task assigned.")
            return
        }

        val currentTask = SlayerManager.getCurrentTask(player)
        val remaining = SlayerManager.getRemainingKills(player)
        val taskName = currentTask?.nameUppercase ?: "Unknown"

        // Show task info
        messageBox(player, "Your current assignment is: $taskName<br>Remaining: $remaining")

        // Options
        val option = options(
            player,
            "How many do I have left?",
            "Who are you?",
            "Where can I find them?",
            "Got any tips?",
            "Nothing.",
        )

        when (option) {
            1 -> remainingDialogue(player, taskName, remaining)
            2 -> masterInfoDialogue(player, master)
            3 -> locationDialogue(player, currentTask)
            4 -> tipsDialogue(player, currentTask)
            5 -> {} // Exit
        }
    }

    /**
     * Show task status without dialogue.
     */
    private fun showTaskStatus(player: Player) {
        if (!SlayerManager.hasTask(player)) {
            player.message("You don't have a slayer task assigned.")
            return
        }

        val currentTask = SlayerManager.getCurrentTask(player)
        val remaining = SlayerManager.getRemainingKills(player)
        val taskName = currentTask?.nameLowercase ?: "unknown creatures"

        player.message("You're assigned to kill $taskName; only $remaining more to go.")
    }

    /**
     * Show remaining kills dialogue.
     */
    private suspend fun QueueTask.remainingDialogue(player: Player, taskName: String, remaining: Int) {
        messageBox(player, "You're assigned to kill $taskName.<br><br>Only $remaining more to go.")
    }

    /**
     * Show slayer master info.
     */
    private suspend fun QueueTask.masterInfoDialogue(player: Player, master: SlayerMaster?) {
        val masterName = master?.displayName ?: "a slayer master"
        val description = when (master) {
            SlayerMaster.TURAEL -> "I'm Turael, the easiest Slayer master. I give simple tasks for beginners."
            SlayerMaster.SPRIA -> "I'm Spria, located in Draynor Village. I give tasks similar to Turael."
            SlayerMaster.MAZCHNA -> "I'm Mazchna, found in Canifis. I require combat level 20 for assignments."
            SlayerMaster.VANNAKA -> "I'm Vannaka, the greatest swordsman alive! Find me in Edgeville Dungeon."
            SlayerMaster.CHAELDAR -> "I'm Chaeldar, the fairy Slayer master in Zanaris. Combat level 70 required."
            SlayerMaster.KONAR -> "I'm Konar quo Maten on Mount Karuulm. I assign tasks to specific locations."
            SlayerMaster.NIEVE -> "I'm Nieve, found in the Tree Gnome Stronghold. Combat level 85 required."
            SlayerMaster.STEVE -> "I'm Steve, Nieve's replacement. Find me in the Tree Gnome Stronghold."
            SlayerMaster.DURADEL -> "I'm Duradel, the highest-level Slayer master in Shilo Village. Combat 100, Slayer 50 required."
            SlayerMaster.KRYSTILIA -> "I'm Krystilia, the Wilderness Slayer master in Edgeville. All tasks are in the Wilderness."
            null -> "I am your Slayer master."
        }
        messageBox(player, description)
    }

    /**
     * Show task location hints.
     */
    private suspend fun QueueTask.locationDialogue(player: Player, taskRow: org.generated.tables.slayer.SlayerTaskRow?) {
        if (taskRow == null) {
            messageBox(player, "I'm not sure where you can find those creatures.")
            return
        }

        val taskName = taskRow.nameLowercase
        val location = getTaskLocation(taskName)
        messageBox(player, location)
    }

    /**
     * Get location hints for a task.
     */
    private fun getTaskLocation(taskName: String): String {
        return when {
            taskName.contains("crawling hand") -> "Crawling hands can be found in the Slayer Tower basement."
            taskName.contains("cave bug") -> "Cave bugs are found in the Lumbridge Swamp Caves."
            taskName.contains("cave crawler") -> "Cave crawlers are in the Fremennik Slayer Dungeon and Lumbridge Swamp Caves."
            taskName.contains("banshee") -> "Banshees can be found in the Slayer Tower."
            taskName.contains("cave slime") -> "Cave slimes are in the Lumbridge Swamp Caves."
            taskName.contains("rockslug") -> "Rockslugs are in the Fremennik Slayer Dungeon."
            taskName.contains("cockatrice") -> "Cockatrices are in the Fremennik Slayer Dungeon."
            taskName.contains("pyrefiend") -> "Pyrefiends can be found in the Fremennik Slayer Dungeon and God Wars Dungeon."
            taskName.contains("mogre") -> "Mogres are found at Mudskipper Point."
            taskName.contains("basilisk") -> "Basilisks are in the Fremennik Slayer Dungeon and Jormungand's Prison."
            taskName.contains("infernal mage") -> "Infernal mages are in the Slayer Tower."
            taskName.contains("bloodveld") -> "Bloodvelds are in the Slayer Tower and God Wars Dungeon."
            taskName.contains("turoth") -> "Turoths are found in the Fremennik Slayer Dungeon."
            taskName.contains("kurask") -> "Kurasks are in the Fremennik Slayer Dungeon."
            taskName.contains("aberrant spectre") -> "Aberrant spectres are in the Slayer Tower and Catacombs of Kourend."
            taskName.contains("gargoyle") -> "Gargoyles are on the top floor of the Slayer Tower."
            taskName.contains("nechryael") -> "Nechryaels are in the Slayer Tower and Catacombs of Kourend."
            taskName.contains("abyssal demon") -> "Abyssal demons are in the Slayer Tower and Catacombs of Kourend."
            taskName.contains("dark beast") -> "Dark beasts are in the Mourner Tunnels and Catacombs of Kourend."
            taskName.contains("smoke devil") -> "Smoke devils are found in the Smoke Devil Dungeon."
            taskName.contains("cave horror") -> "Cave horrors are on Mos Le'Harmless."
            taskName.contains("dust devil") -> "Dust devils are in the Smoke Dungeon and Catacombs of Kourend."
            taskName.contains("hellhound") -> "Hellhounds can be found in Taverley Dungeon and the Wilderness."
            taskName.contains("black demon") -> "Black demons are in Taverley Dungeon and the Catacombs of Kourend."
            taskName.contains("greater demon") -> "Greater demons are in the Brimhaven Dungeon and Wilderness."
            taskName.contains("blue dragon") -> "Blue dragons are in Taverley Dungeon and the Ogre Enclave."
            taskName.contains("black dragon") -> "Black dragons are in Taverley Dungeon and the Wilderness."
            taskName.contains("fire giant") -> "Fire giants are in Waterfall Dungeon and Catacombs of Kourend."
            taskName.contains("kalphite") -> "Kalphites are in the Kalphite Lair and Kalphite Cave."
            taskName.contains("dagannoth") -> "Dagannoths are in Waterbirth Island Dungeon."
            taskName.contains("troll") -> "Trolls can be found in the Troll Stronghold and Keldagrim."
            taskName.contains("aviansie") -> "Aviansies are in the God Wars Dungeon."
            taskName.contains("wyrm") -> "Wyrms are in the Karuulm Slayer Dungeon."
            taskName.contains("drake") -> "Drakes are in the Karuulm Slayer Dungeon."
            taskName.contains("hydra") -> "Hydras are in the Karuulm Slayer Dungeon."
            taskName.contains("kraken") -> "Krakens are in the Kraken Cove."
            else -> "Try searching around Gielinor for ${taskName}. Your Slayer master may have more specific advice."
        }
    }

    /**
     * Show task tips.
     */
    private suspend fun QueueTask.tipsDialogue(player: Player, taskRow: org.generated.tables.slayer.SlayerTaskRow?) {
        if (taskRow == null) {
            messageBox(player, "Good luck with your task!")
            return
        }

        val taskName = taskRow.nameLowercase
        val tip = getTaskTip(taskName)
        messageBox(player, tip)
    }

    /**
     * Get tips for a task.
     */
    private fun getTaskTip(taskName: String): String {
        return when {
            taskName.contains("gargoyle") -> "Gargoyles must be finished off with a rock hammer when they're low on health."
            taskName.contains("rockslug") -> "Rock slugs must be finished off with a bag of salt when they're low on health."
            taskName.contains("desert lizard") -> "Desert lizards must be finished off with ice coolers when they're low on health."
            taskName.contains("zygomite") -> "Zygomites must be finished off with fungicide spray when they're low on health."
            taskName.contains("aberrant spectre") -> "Wear a nosepeg or Slayer helmet to protect against their attacks."
            taskName.contains("banshee") -> "Wear earmuffs or a Slayer helmet to protect against their screams."
            taskName.contains("dust devil") -> "Wear a face mask or Slayer helmet to protect against the dust."
            taskName.contains("kurask") || taskName.contains("turoth") -> "They can only be damaged with leaf-bladed weapons or broad bolts/arrows."
            taskName.contains("basilisk") -> "Wear a mirror shield to protect against their gaze."
            taskName.contains("cockatrice") -> "Wear a mirror shield to protect against their gaze."
            taskName.contains("cave horror") -> "Wear a witchwood icon to protect against their attacks."
            taskName.contains("dragon") -> "Make sure you have an anti-dragon shield or dragonfire protection!"
            taskName.contains("demon") -> "Demonbane weapons are very effective against demons."
            else -> "Good luck with your assignment!"
        }
    }
}

