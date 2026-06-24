package org.rsmod.game.damage

import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.npc.NpcUid

public sealed class DamageContributor {
    public abstract val damage: Int

    public data class ByPlayer(val uuid: Long, override var damage: Int) : DamageContributor() {
        public fun resolve(playerList: PlayerList): Player? =
            playerList.firstOrNull { it.uuid == uuid }
    }

    public data class ByNpc(val uid: NpcUid, override var damage: Int) : DamageContributor() {
        public fun resolve(npcList: NpcList): Npc? = uid.resolve(npcList)
    }
}

internal sealed interface DamageContributorKey {
    data class Player(val uuid: Long) : DamageContributorKey

    data class Npc(val uid: NpcUid) : DamageContributorKey
}
