package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import java.nio.file.Files
import java.nio.file.Path
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

internal fun loadFarmingTestCache(): Int {
    val root = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
        .first { Files.isRegularFile(it.resolve("game.yml")) }
    val revision = Files.readAllLines(root.resolve("game.yml"))
        .first { it.startsWith("revision:") }.substringAfter(':').trim().substringBefore('.').toInt()
    ServerCacheManager.init(revision).close()
    return revision
}

@OptIn(InternalApi::class)
internal fun installFarmingTestInventory(restored: MutableList<() -> Unit>) {
    for ((owner, name) in listOf(
        "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
        "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
    )) {
        val field = Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
        val old = field.get(null)
        restored += { field.set(null, old) }
    }
    val oldStorage = InvVirtualStorageHolder.instance
    restored += { InvVirtualStorageHolder.instance = oldStorage }
    with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
        ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
    }
}

internal class SilentClient : Client<Any, Any> {
    override fun write(message: Any) {}
    override fun close() {}
    override fun read(player: Player) {}
    override fun flush() {}
    override fun flushHighPriority() {}
    override fun unregister(service: Any, player: Player) {}
}
