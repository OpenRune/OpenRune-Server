package org.rsmod.content.travel.jewellery

import jakarta.inject.Inject
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.table.SpiritTreeDestinationsRow
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SpiritTreeScript @Inject constructor(private val caster: ItemTeleportCaster) :
    PluginScript() {
    private val trees: List<SpiritTreeDestinationsRow> =
        SpiritTreeDestinationsRow.all().sortedBy { it.rowId }

    override fun ScriptContext.startup() {
        for (type in TALK_ONLY_TYPES + TRAVEL_TYPES) {
            onOpLoc2(type) { talk() }
        }
        for (type in TRAVEL_TYPES) {
            onOpLoc1(type) { travelMenu() }
            onOpLoc3(type) { lastDestination() }
        }
    }

    private suspend fun ProtectedAccess.talk() {
        if (!QuestRequirements.hasCompleted(player, TREE_GNOME_VILLAGE)) {
            startDialogue {
                chatPlayer(happy, "Hello tree.")
                chatNpcSpecific(
                    TREE_NAME,
                    TREE_HEAD,
                    neutral,
                    "Hello, young one. I am a spirit tree, grown from the seeds of an ancient line.",
                )
                chatPlayer(quiz, "Can you take me somewhere?")
                chatNpcSpecific(
                    TREE_NAME,
                    TREE_HEAD,
                    neutral,
                    "Only friends of the gnome people may travel by my branches.",
                )
            }
            return
        }
        startDialogue {
            chatPlayer(happy, "Hello tree.")
            chatNpcSpecific(
                TREE_NAME,
                TREE_HEAD,
                happy,
                "Hello, friend of the gnomes. Would you like to travel by my branches?",
            )
            when (choice2("Yes please.", 1, "No thanks.", 2)) {
                1 -> {
                    chatPlayer(happy, "Yes please.")
                    access.travelMenu()
                }
                2 -> chatPlayer(neutral, "No thanks.")
            }
        }
    }

    private suspend fun ProtectedAccess.travelMenu() {
        if (!canTravel()) {
            return
        }
        val here = trees.indexOfFirst { player.coords.chebyshevDistance(it.coords) <= HERE_RADIUS }
        val available = trees.indices.filter { it != here }
        val chosen =
            menu(MENU_TITLE, hotkeys = true, choices = available.map { trees[it].title })
        travel(available[chosen])
    }

    private suspend fun ProtectedAccess.lastDestination() {
        if (!canTravel()) {
            return
        }
        val previous = vars[PREVIOUS_VARBIT] - 1
        val tree = trees.getOrNull(previous)
        if (tree == null) {
            mes("You haven't travelled by spirit tree yet.")
            travelMenu()
            return
        }
        if (player.coords.chebyshevDistance(tree.coords) <= HERE_RADIUS) {
            mes("You are already at ${tree.title}.")
            return
        }
        travel(previous)
    }

    private suspend fun ProtectedAccess.travel(index: Int) {
        val tree = trees[index]
        ifClose()
        with(caster) {
            cast(TeleportType.Standard, tree.coords) {
                vars[PREVIOUS_VARBIT] = index + 1
                mes("The spirit tree carries you to ${tree.title}.")
                true
            }
        }
    }

    private fun ProtectedAccess.canTravel(): Boolean {
        if (!QuestRequirements.hasCompleted(player, TREE_GNOME_VILLAGE)) {
            mes("You need to be a friend of the gnome people before the spirit trees will carry you.")
            return false
        }
        val atStronghold = player.coords.chebyshevDistance(trees[STRONGHOLD].coords) <= HERE_RADIUS
        if (atStronghold && !QuestRequirements.hasCompleted(player, GRAND_TREE)) {
            mes("This tree will not carry you until you have proved yourself to King Narnode.")
            return false
        }
        return true
    }

    private companion object {
        private const val MENU_TITLE = "Spirit Tree Locations"
        private const val TREE_NAME = "Spirit tree"
        private const val TREE_HEAD = "npc.treevillage_spirittree"
        private const val TREE_GNOME_VILLAGE = "quest_treegnomevillage"
        private const val GRAND_TREE = "quest_grandtree"
        private const val PREVIOUS_VARBIT = "varbit.spirit_tree_previous"
        private const val HERE_RADIUS = 6
        private const val STRONGHOLD = 1

        private val TALK_ONLY_TYPES = listOf("loc.spirittree_big_1op", "loc.spirittree_small_1op")
        private val TRAVEL_TYPES =
            listOf(
                "loc.spirittree_big_2ops",
                "loc.spirittree_big_2ops_orbs",
                "loc.spirittree_small_2ops",
            )
    }
}
