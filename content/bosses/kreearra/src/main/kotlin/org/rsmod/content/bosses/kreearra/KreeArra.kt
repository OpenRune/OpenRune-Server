package org.rsmod.content.bosses.kreearra

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.combat.weapon.types.AttackTypes
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.player.righthand
import org.rsmod.api.script.onEvent
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.type.getOrNull
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.module.PluginModule
import org.rsmod.plugin.scripts.ScriptContext

class KreeArra @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {

    private val avatarId by lazy { AVATAR.asRSCM(RSCMType.NPC) }

    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps)

        onEvent<NpcStateEvents.Respawn> { if (npc.id == avatarId) respawnDeadBodyguards(npc) }
    }

    private fun respawnDeadBodyguards(avatar: Npc) {
        deps.npcRepo
            .findAll(ZoneKey.from(avatar.coords), zoneRadius = BODYGUARD_SEARCH_RADIUS)
            .filter { it.visType.isCategoryType(BODYGUARD_CATEGORY) && it.hitpoints == 0 }
            .forEach { bodyguard -> bodyguard.lifecycleRespawnCycle = deps.mapClock.cycle + 1 }
    }

    override val spec =
        boss(AVATAR) {
            stats(attackRate = 3, aggressionRadius = 8)

            val windRanged =
                ability("wind_ranged") {
                    anim("seq.godwars_armadyl_avatar_wind_attack")
                    include(
                        onEach(
                            AllInRadius(radius = ROOM_RADIUS),
                            Effect.Projectile(
                                spotanim = "spotanim.godwars_armadyl_avatar_wind_attack_spotanim",
                                hit = Effect.Hit(damage = Roll(0..69), type = Ranged),
                            ),
                        )
                    )
                }

            val magic =
                ability("magic") {
                    anim("seq.godwars_armadyl_avatar_wind_attack")
                    projectile(
                        spotanim = "spotanim.godwars_armadyl_avatar_magic_attack_spotanim",
                        hit = Effect.Hit(damage = Roll(0..21), type = Magic),
                    )
                }

            val claw =
                ability("claw") {
                    anim("seq.godwars_armadyl_avatar_claw_attack")
                    hit {
                        damage(0..25).roll()
                        type(Melee)
                    }
                }

            phase("combat") {
                weightedSelectorRandom {
                    +random(windRanged, weight = 6)
                    +random(magic, weight = 2)
                    +random(claw, weight = 2, requires = WithinMeleeRange)
                }
            }
        }

    private companion object {
        private const val AVATAR = "npc.godwars_armadyl_avatar"
        private const val BODYGUARD_CATEGORY = "category.godwars_armadyl_bodyguard"
        private const val BODYGUARD_SEARCH_RADIUS = 10

        private const val ROOM_RADIUS = 15
    }
}

public class KreeArraModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcAttackValidateHook>(KreeArraMeleeBlockHook::class.java)
    }
}

internal class KreeArraMeleeBlockHook @Inject constructor(private val types: AttackTypes) :
    NpcAttackValidateHook {
    private val avatarId by lazy { AVATAR.asRSCM(RSCMType.NPC) }

    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        val isUnreachable = npc.id == avatarId || npc.visType.isCategoryType(BODYGUARD_CATEGORY)
        if (!isUnreachable) {
            return NpcAttackValidateResult.Pass
        }

        val type = types.get(player)
        if (type != null && !type.isMelee) {
            return NpcAttackValidateResult.Pass
        }

        val weapon = getOrNull(player.righthand)
        if (weapon != null && weapon.isCategoryType("category.halberd")) {
            return NpcAttackValidateResult.Pass
        }

        return NpcAttackValidateResult.Deny(
            "${npc.name} is flying too high for you to reach with melee."
        )
    }

    private companion object {
        private const val AVATAR = "npc.godwars_armadyl_avatar"
        private const val BODYGUARD_CATEGORY = "category.godwars_armadyl_bodyguard"
    }
}
