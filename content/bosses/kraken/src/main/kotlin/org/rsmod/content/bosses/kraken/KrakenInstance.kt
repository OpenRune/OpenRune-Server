package org.rsmod.content.bosses.kraken

import jakarta.inject.Inject
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceAccess
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceNpc
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.instances.RegionLocal
import org.rsmod.api.instances.enterLocObjects
import org.rsmod.api.invtx.FeeSplit
import org.rsmod.api.invtx.feeSplit
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.utils.format.formatAmount
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class KrakenInstance @Inject constructor(
    registry: BossInstanceRegistry,
    private val playerList: PlayerList,
) : InstanceScript(registry) {

    override fun settingsRow(): String = "dbrow.instance_kraken"

    override fun area(): InstanceArea = INSTANCE

    override fun recordsKillTime(player: Player): Boolean = false

    override fun ScriptContext.configure() {
        onEnterObject { telejump(LAIR_ENTRY, TeleportType.Exempt) }
        onExitObject { leaveLair() }
        for (loc in settingsRowData().enterLocObjects()) {
            onOpLoc2(loc) { enterPrivate() }
            onOpLoc3(loc) { peek() }
        }
    }

    private suspend fun ProtectedAccess.leaveLair() {
        if (manager.sessionForPlayer(player) != null) {
            defaultLeaveFlow()
        } else {
            telejump(COVE_EXIT, TeleportType.Exempt)
        }
    }

    private suspend fun ProtectedAccess.peek() {
        mes("You peek through the crevice...")
        delay(PEEK_DELAY)
        val count = playerList.count { it.coords.x in LAIR_X && it.coords.z in LAIR_Z }
        val text =
            when (count) {
                0 -> "No adventurers."
                1 -> "1 adventurer."
                else -> "$count adventurers."
            }
        mes("Standard cave: $text")
    }

    private suspend fun ProtectedAccess.enterPrivate() {
        if (manager.sessionForPlayer(player) != null) {
            mes("You are already inside an instance.")
            return
        }
        val owned = player.uuid?.let { manager.sessionOwnedBy(key, it) }
        if (owned != null) {
            completeInstanceEntry(
                manager.join(player, owned, worldClock.cycle, code = null, forceAccess = true)
            )
            return
        }
        val spec = buildSpec()
        val fee = spec.fee.formatAmount
        val pay =
            choice2(
                "Yes, pay $fee x Coins.",
                true,
                "No, don't pay.",
                false,
                title = "Pay $fee x Coins?",
            )
        if (!pay) return
        val split = player.feeSplit(spec.fee)
        val result = manager.create(player, key, spec, InstanceAccess.Private, worldClock.cycle)
        if (result is InstanceManager.Result.Created) {
            mes(paymentMessage(split), ChatType.Spam)
            mesbox("<col=7f0000>Any items you leave in here will be lost.</col>")
        }
        completeInstanceEntry(result)
    }

    private fun paymentMessage(split: FeeSplit): String {
        val inventory = "${split.fromInventory.formatAmount} x Coins"
        val bank = "${split.fromBank.formatAmount} x Coins"
        return when {
            split.fromBank == 0 -> "Payment has been taken from your inventory: $inventory"
            split.fromInventory == 0 -> "Payment has been taken from your bank: $bank"
            else -> "Payment has been taken from your inventory ($inventory) and bank ($bank)"
        }
    }

    private companion object {
        private const val PEEK_DELAY = 2
        private const val LAIR_REGION = 9116

        private val LAIR_ENTRY = CoordGrid(2280, 10022, 0)
        private val COVE_EXIT = CoordGrid(2280, 10016, 0)
        private val LAIR_X = 2270..2291
        private val LAIR_Z = 10022..10045

        private val INSTANCE =
            InstanceArea.copyRegions(
                regionIds = listOf(LAIR_REGION),
                npcSpawns =
                    listOf(
                        spawn("npc.slayer_kraken_boss_whirlpool", 38, 50),
                        spawn("npc.slayer_kraken_boss_tentacle_whirlpool", 35, 50),
                        spawn("npc.slayer_kraken_boss_tentacle_whirlpool", 35, 54),
                        spawn("npc.slayer_kraken_boss_tentacle_whirlpool", 44, 50),
                        spawn("npc.slayer_kraken_boss_tentacle_whirlpool", 44, 54),
                    ),
            )

        private fun spawn(npc: String, localX: Int, localZ: Int) =
            InstanceNpc(npc, RegionLocal(0, 35, 156, localX, localZ))
    }
}
