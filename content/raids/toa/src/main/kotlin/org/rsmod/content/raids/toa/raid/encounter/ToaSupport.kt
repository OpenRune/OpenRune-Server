package org.rsmod.content.raids.toa.raid.encounter

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcServerType
import dev.openrune.types.aconverted.SpotanimType
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType

internal fun npcType(name: String): NpcServerType =
    ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC))!!

internal fun spotanim(name: String): SpotanimType = SpotanimType(name.asRSCM(RSCMType.SPOTANIM))

internal fun Player.hitTypeless(damage: Int) {
    queueHit(delay = 1, type = HitType.Typeless, damage = damage, modifier = NoopPlayerHitModifier)
}
