package org.rsmod.game.damage

import kotlin.math.min
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.npc.NpcUid

public class DamageContributions {
    private val entries: LinkedHashMap<DamageContributorKey, DamageContributor> = linkedMapOf()

    public val isEmpty: Boolean
        get() = entries.isEmpty()

    public val size: Int
        get() = entries.size

    public fun clear() {
        entries.clear()
    }

    public fun record(source: Player, damage: Int) {
        if (damage <= 0) {
            return
        }
        val uuid = source.uuid ?: error("Unexpected null uuid for player: $source")
        recordPlayer(uuid, damage)
    }

    public fun record(source: Npc, damage: Int) {
        if (damage <= 0) {
            return
        }
        recordNpc(source.uid, damage)
    }

    public fun damageBy(source: Player): Int {
        val uuid = source.uuid ?: return 0
        return (entries[DamageContributorKey.Player(uuid)] as? DamageContributor.ByPlayer)?.damage
            ?: 0
    }

    public fun damageBy(source: Npc): Int =
        (entries[DamageContributorKey.Npc(source.uid)] as? DamageContributor.ByNpc)?.damage ?: 0

    public fun entries(): List<DamageContributor> = entries.values.toList()

    public fun sortedByDamageDescending(): List<DamageContributor> =
        entries.values.sortedByDescending { it.damage }

    public fun sortedByDamageAscending(): List<DamageContributor> =
        entries.values.sortedBy { it.damage }

    public fun mostDamage(): DamageContributor? = entries.values.maxByOrNull { it.damage }

    public fun leastDamage(): DamageContributor? = entries.values.minByOrNull { it.damage }

    public fun totalDamage(): Int = entries.values.sumOf { it.damage }

    public fun absorbFrom(other: DamageContributions) {
        for (contributor in other.entries().filterIsInstance<DamageContributor.ByPlayer>()) {
            recordPlayer(contributor.uuid, contributor.damage)
        }
    }

    public fun topPlayer(): DamageContributor.ByPlayer? =
        mostDamage() as? DamageContributor.ByPlayer

    public fun leastPlayer(): DamageContributor.ByPlayer? {
        val players = entries.values.filterIsInstance<DamageContributor.ByPlayer>()
        return players.minByOrNull { it.damage }
    }

    public fun topNpc(): DamageContributor.ByNpc? = mostDamage() as? DamageContributor.ByNpc

    public fun leastNpc(): DamageContributor.ByNpc? {
        val npcs = entries.values.filterIsInstance<DamageContributor.ByNpc>()
        return npcs.minByOrNull { it.damage }
    }

    public fun topPlayer(playerList: PlayerList): Player? = topPlayer()?.resolve(playerList)

    public fun leastPlayer(playerList: PlayerList): Player? = leastPlayer()?.resolve(playerList)

    public fun topNpc(npcList: NpcList): Npc? = topNpc()?.resolve(npcList)

    public fun leastNpc(npcList: NpcList): Npc? = leastNpc()?.resolve(npcList)

    private fun recordPlayer(uuid: Long, damage: Int) {
        val key = DamageContributorKey.Player(uuid)
        val existing = entries[key] as? DamageContributor.ByPlayer
        if (existing != null) {
            existing.damage = min(Int.MAX_VALUE.toLong(), existing.damage + damage.toLong()).toInt()
            return
        }
        entries[key] = DamageContributor.ByPlayer(uuid, damage)
    }

    private fun recordNpc(uid: NpcUid, damage: Int) {
        val key = DamageContributorKey.Npc(uid)
        val existing = entries[key] as? DamageContributor.ByNpc
        if (existing != null) {
            existing.damage = min(Int.MAX_VALUE.toLong(), existing.damage + damage.toLong()).toInt()
            return
        }
        entries[key] = DamageContributor.ByNpc(uid, damage)
    }
}
