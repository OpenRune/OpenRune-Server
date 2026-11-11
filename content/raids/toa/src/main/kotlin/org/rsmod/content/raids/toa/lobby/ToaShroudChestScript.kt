package org.rsmod.content.raids.toa.lobby

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.interfaces.collectionlog.CollectionLog
import org.rsmod.content.raids.toa.raid.ToaKillCount
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ToaShroudChestScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(LobbyLocs.SHROUD_CHEST) { searchChest() }
    }

    private suspend fun ProtectedAccess.searchChest() {
        val completions = ToaKillCount.normalAndExpert(player)
        val unlocked = LobbyShrouds.REWARDS.filter { completions >= it.completions }
        if (unlocked.isEmpty()) {
            mesbox(LobbyShrouds.NONE_UNLOCKED)
            return
        }
        startDialogue {
            val reward = if (unlocked.size == 1) unlocked.single() else choose(unlocked)
            take(reward)
        }
    }

    private suspend fun Dialogue.choose(rewards: List<ShroudReward>): ShroudReward {
        if (rewards.size <= LobbyShrouds.MAX_CHOICES) return menu(rewards.map { it.option to it })
        return choosePage(rewards.chunked(LobbyShrouds.MAX_CHOICES - 1), page = 0)
    }

    private suspend fun Dialogue.choosePage(
        pages: List<List<ShroudReward>>,
        page: Int,
    ): ShroudReward {
        val options: List<Pair<String, ShroudReward?>> =
            pages[page].map { it.option to it } + (LobbyShrouds.MORE to null)
        return menu(options) ?: choosePage(pages, (page + 1) % pages.size)
    }

    private suspend fun Dialogue.take(reward: ShroudReward) {
        if (!access.inv.hasFreeSpace()) {
            mesbox(LobbyShrouds.NO_SPACE)
            return
        }
        access.invAdd(access.inv, reward.obj)
        CollectionLog.grant(player, reward.obj)
        objbox(reward.obj, LobbyShrouds.TAKEN)
    }

    private suspend fun <T> Dialogue.menu(o: List<Pair<String, T>>): T =
        when (o.size) {
            2 -> choice2(o[0].first, o[0].second, o[1].first, o[1].second)
            3 -> choice3(o[0].first, o[0].second, o[1].first, o[1].second, o[2].first, o[2].second)
            4 ->
                choice4(
                    o[0].first,
                    o[0].second,
                    o[1].first,
                    o[1].second,
                    o[2].first,
                    o[2].second,
                    o[3].first,
                    o[3].second,
                )
            else ->
                choice5(
                    o[0].first,
                    o[0].second,
                    o[1].first,
                    o[1].second,
                    o[2].first,
                    o[2].second,
                    o[3].first,
                    o[3].second,
                    o[4].first,
                    o[4].second,
                )
        }
}
