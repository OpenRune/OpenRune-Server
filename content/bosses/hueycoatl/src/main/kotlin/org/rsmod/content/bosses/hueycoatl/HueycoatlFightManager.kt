package org.rsmod.content.bosses.hueycoatl

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.bossbar.plugin.BossHpBarScript
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.instances.InstanceId
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.instances.currentInstanceId
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

@Singleton
internal class HueycoatlFightManager
@Inject
constructor(
    private val deps: BossDeps,
    private val instances: InstanceManager,
    private val loot: HueycoatlLoot,
    private val hpBar: BossHpBarScript,
    private val wornBonuses: WornBonuses,
    private val worldStats: HueycoatlWorldStats,
) {
    private val logger = InlineLogger()

    val publicFight: HueycoatlFight =
        HueycoatlFight(
            key = HueyFightKey.Public,
            session = null,
            deps = deps,
            instances = instances,
            loot = loot,
            hpBar = hpBar,
            wornBonuses = wornBonuses,
            worldStats = worldStats,
        )

    private val instanceFights = linkedMapOf<InstanceId, HueycoatlFight>()

    fun initializePublic() {
        publicFight.initialize()
        logger.info {
            "Hueycoatl public encounter ready: " +
                "head=${publicFight.head?.isSlotAssigned == true}, " +
                "body=${publicFight.bodyParts.count { it.isSlotAssigned }}, " +
                "seer=${publicFight.seer?.isSlotAssigned == true}"
        }
    }

    fun ensureInstance(session: InstanceSession): HueycoatlFight {
        val fight =
            instanceFights.getOrPut(session.id) {
                HueycoatlFight(
                    key = HueyFightKey.Instance(session.id),
                    session = session,
                    deps = deps,
                    instances = instances,
                    loot = loot,
                    hpBar = hpBar,
                    wornBonuses = wornBonuses,
                    worldStats = worldStats,
                )
            }
        fight.initialize()
        return fight
    }

    fun fightForInstance(id: InstanceId): HueycoatlFight? = instanceFights[id]

    fun removeInstance(id: InstanceId) {
        instanceFights.remove(id)?.dispose()
    }

    fun fightForPlayer(player: Player): HueycoatlFight {
        val instance = player.currentInstanceId()
        return if (instance != null) {
            instanceFights[instance] ?: ensureInstance(instances.sessionForId(instance) ?: return publicFight)
        } else {
            publicFight
        }
    }

    fun existingFightForPlayer(player: Player): HueycoatlFight? {
        val instance = player.currentInstanceId() ?: return publicFight
        return instanceFights[instance]
    }

    fun fightForNpc(npc: Npc): HueycoatlFight? {
        val instance = instances.instanceForNpc(npc)
        if (instance != null) return instanceFights[instance]
        return publicFight.takeIf {
            npc === it.head || npc === it.tail || it.bodyParts.any { body -> body === npc }
        }
    }

    fun onLogout(player: Player) {
        existingFightForPlayer(player)?.removeParticipant(player)
        VarPlayerIntMapSetter.set(player, "varbit.huey_in_area", 0)
    }
}
