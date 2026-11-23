package org.rsmod.content.travel.spirittrees

import dev.openrune.ServerCacheManager
import jakarta.inject.Inject
import org.rsmod.api.player.hook.ItemTeleportCaster
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
        for (type in TALK_ONLY_TYPES) {
            onOpLoc2(type) { talk(ancient = type in ANCIENT_TYPES) }
        }
        for (type in TRAVEL_TYPES) {
            onOpLoc1(type) { travelMenu() }
            onOpLoc2(type) { talk(ancient = type in ANCIENT_TYPES) }
            onOpLoc3(type) { lastDestination() }
        }
    }

    private suspend fun ProtectedAccess.talk(ancient: Boolean) {
        if (!QuestRequirements.hasCompleted(player, TREE_GNOME_VILLAGE)) {
            mes("The tree doesn't feel like talking.")
            return
        }
        if (!ancient) {
            treeSays("Hello gnome friend. Where would you like to go?")
            travelMenu()
            return
        }
        var wantsToTravel = false
        startDialogue {
            chatNpcSpecific(
                TREE_NAME,
                TREE_HEAD,
                neutral,
                "You friend of gnome people, you friend of mine. Would you like me to take you " +
                    "somewhere?",
            )
            wantsToTravel = choice2("No thanks, old tree.", false, "Where can I go?", true)
            if (!wantsToTravel) {
                chatPlayer(neutral, "No thanks, old tree.")
                return@startDialogue
            }
            chatPlayer(quiz, "Where can I go?")
            chatNpcSpecific(
                TREE_NAME,
                TREE_HEAD,
                neutral,
                "You can travel to the trees which are related to me.",
            )
        }
        if (wantsToTravel) {
            travelMenu()
        }
    }

    private suspend fun ProtectedAccess.travelMenu() {
        if (!canDepart()) {
            return
        }
        val choices =
            trees.map { if (isAvailable(it)) it.title else "<col=$GREY>${it.title}</col>" }
        val chosen = menu(MENU_TITLE, hotkeys = true, choices = choices + CANCEL)
        val tree = trees.getOrNull(chosen) ?: return
        travel(tree)
    }

    private suspend fun ProtectedAccess.lastDestination() {
        if (!canDepart()) {
            return
        }
        val tree = trees.getOrNull(vars[PREVIOUS_VARBIT] - 1)
        if (tree == null) {
            treeSays(
                "This once, you will have to tell me where you wish to go, for I do not yet know " +
                    "where to take you back."
            )
            travelMenu()
            return
        }
        travel(tree)
    }

    private suspend fun ProtectedAccess.travel(tree: SpiritTreeDestinationsRow) {
        val refusal = refusal(tree)
        if (refusal != null) {
            treeSays(refusal)
            travelMenu()
            return
        }
        ifClose()
        mesbox(
            "You place your hands on the dry tough bark of the spirit tree, and feel a surge of " +
                "energy run through your veins."
        )
        with(caster) {
            cast(TeleportType.Standard, tree.coords) {
                vars[PREVIOUS_VARBIT] = trees.indexOf(tree) + 1
                true
            }
        }
    }

    private fun ProtectedAccess.refusal(tree: SpiritTreeDestinationsRow): String? =
        when {
            isHere(tree) -> "You're already here."
            tree.house == true -> "None of my brethren are in your house."
            tree.planted == true || !questDone(tree) -> "None of my brethren are at that location."
            !varbitSet(tree) ->
                "You cannot travel to that land at this time. You must have visited " +
                    "${tree.title} before you can travel there."
            else -> null
        }

    private fun ProtectedAccess.isAvailable(tree: SpiritTreeDestinationsRow): Boolean =
        tree.house != true && tree.planted != true && questDone(tree) && varbitSet(tree)

    private fun ProtectedAccess.questDone(tree: SpiritTreeDestinationsRow): Boolean {
        val quest = tree.quest ?: return true
        if (QuestRequirements.hasCompleted(player, quest)) {
            return true
        }
        return tree.questStarted == true && QuestRequirements.isOnQuest(player, quest)
    }

    private fun ProtectedAccess.varbitSet(tree: SpiritTreeDestinationsRow): Boolean {
        val varbit = tree.unlockVarbit?.let { ServerCacheManager.getVarbit(it) } ?: return true
        return player.vars[varbit] != 0
    }

    private fun ProtectedAccess.isHere(tree: SpiritTreeDestinationsRow): Boolean =
        player.coords.chebyshevDistance(tree.coords) <= HERE_RADIUS

    private fun ProtectedAccess.canDepart(): Boolean {
        if (!QuestRequirements.hasCompleted(player, TREE_GNOME_VILLAGE)) {
            mes("The tree doesn't feel like talking.")
            return false
        }
        val here = trees.firstOrNull { isHere(it) } ?: return true
        val quest = here.departureQuest ?: return true
        if (QuestRequirements.hasCompleted(player, quest)) {
            return true
        }
        mes("This tree will not carry you until you have proved yourself to King Narnode.")
        return false
    }

    private suspend fun ProtectedAccess.treeSays(text: String) {
        startDialogue { chatNpcSpecific(TREE_NAME, TREE_HEAD, neutral, text) }
    }

    private companion object {
        private const val MENU_TITLE = "Spirit Tree Locations"
        private const val CANCEL = "Cancel"
        private const val GREY = "5f5f5f"
        private const val TREE_NAME = "Spirit Tree"
        private const val TREE_HEAD = "npc.treevillage_spirittree"
        private const val TREE_GNOME_VILLAGE = "quest_treegnomevillage"
        private const val PREVIOUS_VARBIT = "varbit.spirit_tree_previous"
        private const val HERE_RADIUS = 6

        private val ANCIENT_TYPES =
            listOf(
                "loc.spirittree_big_1op",
                "loc.spirittree_big_2ops",
                "loc.spirittree_big_2ops_orbs",
            )
        private val TALK_ONLY_TYPES = listOf("loc.spirittree_big_1op", "loc.spirittree_small_1op")
        private val TRAVEL_TYPES =
            listOf(
                "loc.spirittree_big_2ops",
                "loc.spirittree_big_2ops_orbs",
                "loc.spirittree_small_2ops",
                "loc.spirittree_prif_2ops",
            )
    }
}
