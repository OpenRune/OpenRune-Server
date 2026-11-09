package org.rsmod.content.bosses.chaoselemental

import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossExtensionContext
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.HitType
import org.rsmod.api.bosses.spec.ProjectileConfig
import org.rsmod.api.combat.commons.player.combatPlayDefendAnim
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.worn.WornUnequipOp
import org.rsmod.api.player.worn.WornUnequipResult
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.plugin.scripts.ScriptContext

class ChaosElemental
@Inject
constructor(
    deps: BossDeps,
    private val wornUnequipOp: WornUnequipOp,
    private val protectedAccess: ProtectedAccessLauncher,
) : BossPluginScript(deps) {

    private val hasFreeInventorySpace = Condition.Custom { _, target ->
        target != null && target.inv.hasFreeSpace()
    }

    override val spec =
        boss(BOSS_NPC) {
            stats(attackRate = ATTACK_RATE)

            val discordMagic = ability("discord_magic", discord(Magic))
            val discordRanged = ability("discord_ranged", discord(Ranged))
            val discordMelee = ability("discord_melee", discord(Melee, MeleeAttackType.Crush))
            val madness = ability("madness", madness())
            val confusion = ability("confusion", confusion())

            phase(PHASE_FIGHT) {
                weightedSelectorRandom {
                    +random(discordMagic, weight = DISCORD_MAGIC_WEIGHT)
                    +random(discordRanged, weight = DISCORD_RANGED_WEIGHT)
                    +random(discordMelee, weight = DISCORD_MELEE_WEIGHT)
                    +random(madness, weight = MADNESS_WEIGHT, requires = hasFreeInventorySpace)
                    +random(confusion, weight = CONFUSION_WEIGHT)
                }
            }
        }

    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            spec,
            deps,
            onHit = {
                val player =
                    if (hit.isFromPlayer) hit.resolvePlayerSource(deps.playerList) else null
                if (player != null && player.isValidTarget()) player.soundSynth(HIT_SOUND)
            },
        )
        deps.extensionRegistry.register(DISCORD_IMPACT) { context -> onDiscordImpact(context) }
        deps.extensionRegistry.register(CONFUSION_IMPACT) { context -> onConfusionImpact(context) }
        deps.extensionRegistry.register(MADNESS_IMPACT) { context -> onMadnessImpact(context) }
    }

    private fun discord(hitType: HitType, meleeAttackType: MeleeAttackType? = null): Effect =
        sequence(
            anim(ATTACK_ANIM),
            spotanim(DISCORD_CAST, height = CAST_HEIGHT),
            sound(DISCORD_CAST_SOUND, radius = CAST_SOUND_RADIUS),
            projectile(
                spotanim = DISCORD_PROJECTILE,
                config = PROJECTILE,
                onImpact = external(DISCORD_IMPACT),
                resolveOnImpact = true,
                hit =
                    hit {
                        damage(Accuracy(Roll(0..MAX_HIT), meleeAttackType = meleeAttackType))
                        type(hitType)
                    },
            ),
        )

    private fun confusion(): Effect =
        sequence(
            anim(ATTACK_ANIM),
            spotanim(CONFUSION_CAST, height = CAST_HEIGHT),
            sound(CONFUSION_CAST_SOUND, radius = CAST_SOUND_RADIUS),
            projectile(
                spotanim = CONFUSION_PROJECTILE,
                config = PROJECTILE,
                onImpact = external(CONFUSION_IMPACT),
            ),
        )

    private fun madness(): Effect =
        sequence(
            anim(ATTACK_ANIM),
            spotanim(MADNESS_CAST, height = CAST_HEIGHT),
            sound(MADNESS_CAST_SOUND, radius = CAST_SOUND_RADIUS),
            projectile(
                spotanim = MADNESS_PROJECTILE,
                config = PROJECTILE,
                onImpact = external(MADNESS_IMPACT),
            ),
        )

    private fun onDiscordImpact(context: BossExtensionContext) {
        val player = context.target
        if (!player.isValidTarget()) return
        player.spotanim(DISCORD_IMPACT_GFX, height = IMPACT_HEIGHT)
        deps.worldRepo.soundArea(player, DISCORD_IMPACT_SOUND, radius = IMPACT_SOUND_RADIUS)
    }

    @OptIn(InternalApi::class)
    private fun onConfusionImpact(context: BossExtensionContext) {
        val player = context.target
        if (!player.isValidTarget()) return
        player.combatPlayDefendAnim()
        player.spotanim(CONFUSION_IMPACT_GFX, height = IMPACT_HEIGHT)
        deps.worldRepo.soundArea(player, CONFUSE_HIT_SOUND, radius = IMPACT_SOUND_RADIUS)
        deps.worldQueues.add(CONFUSION_TELEPORT_DELAY) {
            if (!player.isValidTarget()) return@add
            protectedAccess.launchLenient(player) {
                val destination =
                    mapFindSquareLineOfWalk(
                        player.coords,
                        minRadius = 1,
                        maxRadius = TELEPORT_RADIUS,
                    ) ?: return@launchLenient
                player.soundSynth(CONFUSION_IMPACT_SOUND)
                teleport(destination, TeleportType.Exempt)
            }
        }
    }

    private fun onMadnessImpact(context: BossExtensionContext) {
        val player = context.target
        if (!player.isValidTarget()) return
        player.combatPlayDefendAnim()
        player.spotanim(MADNESS_IMPACT_GFX, height = IMPACT_HEIGHT)
        deps.worldRepo.soundArea(player, MADNESS_IMPACT_SOUND, radius = IMPACT_SOUND_RADIUS)
        disarm(player)
    }

    private fun disarm(player: Player) {
        val maxItems = minOf(MAX_DISARMED_ITEMS, player.inv.freeSpace())
        for (slot in madnessUnequipSlots(player.worn, deps.random, maxItems)) {
            val result = wornUnequipOp.unequip(player, slot, player.worn, player.inv)
            if (result is WornUnequipResult.Fail) return
            if (slot == Wearpos.RightHand.slot) player.clearInteraction()
        }
    }

    private companion object {
        private const val BOSS_NPC = "npc.chaoselemental"
        private const val PHASE_FIGHT = "fight"

        private const val ATTACK_RATE = 4
        private const val MAX_HIT = 28

        private const val ATTACK_ANIM = "seq.chaoselemental_casting"
        private const val CAST_HEIGHT = 320
        private const val CAST_SOUND_RADIUS = 10
        private const val IMPACT_HEIGHT = 128
        private const val IMPACT_SOUND_RADIUS = 5

        private const val DISCORD_CAST = "spotanim.chaoselemental_spotanim_discord_casting"
        private const val DISCORD_PROJECTILE = "spotanim.chaoselemental_spotanim_discord_travel"
        private const val DISCORD_IMPACT_GFX = "spotanim.chaoselemental_spotanim_discord_impact"
        private const val DISCORD_CAST_SOUND = "synth.chaos_elemental_discord_cast"
        private const val DISCORD_IMPACT_SOUND = "synth.chaos_elemental_discord_impact"

        private const val CONFUSION_CAST = "spotanim.chaoselemental_spotanim_confusion_casting"
        private const val CONFUSION_PROJECTILE = "spotanim.chaoselemental_spotanim_confusion_travel"
        private const val CONFUSION_IMPACT_GFX = "spotanim.chaoselemental_spotanim_confusion_impact"
        private const val CONFUSION_CAST_SOUND = "synth.chaos_elemental_confusion_cast"
        private const val CONFUSION_IMPACT_SOUND = "synth.chaos_elemental_confusion_impact"
        private const val CONFUSE_HIT_SOUND = "synth.confuse_hit"

        private const val MADNESS_CAST = "spotanim.chaoselemental_spotanim_madness_casting"
        private const val MADNESS_PROJECTILE = "spotanim.chaoselemental_spotanim_madness_travel"
        private const val MADNESS_IMPACT_GFX = "spotanim.chaoselemental_spotanim_madness_impact"
        private const val MADNESS_CAST_SOUND = "synth.chaos_elemental_madness_cast"
        private const val MADNESS_IMPACT_SOUND = "synth.chaos_elemental_madness_impact"

        private const val HIT_SOUND = "synth.chaos_elemental_hit"

        private const val DISCORD_IMPACT = "chaoselemental.discord_impact"
        private const val CONFUSION_IMPACT = "chaoselemental.confusion_impact"
        private const val MADNESS_IMPACT = "chaoselemental.madness_impact"

        // 7:2:1 discord/madness/confusion; magic is the most frequent Discord style (2:1:1).
        private const val DISCORD_MAGIC_WEIGHT = 14
        private const val DISCORD_RANGED_WEIGHT = 7
        private const val DISCORD_MELEE_WEIGHT = 7
        private const val MADNESS_WEIGHT = 8
        private const val CONFUSION_WEIGHT = 4

        private const val CONFUSION_TELEPORT_DELAY = 1
        private const val TELEPORT_RADIUS = 10

        private const val MAX_DISARMED_ITEMS = 4

        private val PROJECTILE =
            ProjectileConfig(
                startHeight = 320,
                endHeight = 128,
                startDelay = 40,
                travelTime = 0,
                angle = 16,
                progress = 64,
                stepMultiplier = 10,
            )
    }
}

internal fun madnessUnequipSlots(
    worn: Inventory,
    random: GameRandom,
    maxItems: Int = 4,
): List<Int> {
    val equipped =
        Wearpos.entries
            .filter { !it.isClientOnly && worn[it.slot] != null }
            .mapTo(mutableListOf()) { it.slot }
    if (equipped.isEmpty() || maxItems <= 0) return emptyList()

    val count = random.of(1, minOf(maxItems, equipped.size))
    val slots = mutableListOf<Int>()
    if (equipped.remove(Wearpos.RightHand.slot)) slots += Wearpos.RightHand.slot

    while (slots.size < count && equipped.isNotEmpty()) {
        slots += equipped.removeAt(random.of(equipped.size))
    }
    return slots
}
