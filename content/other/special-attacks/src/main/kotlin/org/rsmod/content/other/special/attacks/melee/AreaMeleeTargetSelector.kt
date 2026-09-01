package org.rsmod.content.other.special.attacks.melee

import jakarta.inject.Inject
import org.rsmod.api.combat.player.PvPAreaAttackManager
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.map.CardinalDirection
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

/**
 * Selects valid nearby targets on the exact tiles occupied by a melee area special.
 *
 * The primary target is always preserved. Secondary NPCs and players are capped independently,
 * matching the cache-era weapon rules.
 */
class AreaMeleeTargetSelector
@Inject
constructor(
    private val npcs: NpcRepository,
    private val players: PlayerRepository,
    private val npcAttackValidateHooks: Set<NpcAttackValidateHook>,
    private val pvp: PvPAreaAttackManager,
) {
    fun square(centre: CoordGrid, radius: Int): Set<CoordGrid> {
        require(radius >= 0)
        val tiles = mutableSetOf<CoordGrid>()
        for (x in -radius..radius) {
            for (z in -radius..radius) {
                tiles += centre.translate(x, z)
            }
        }
        return tiles
    }

    /**
     * Builds the three-tile-wide line through the primary target that a halberd sweep affects.
     */
    fun halberdSweep(source: Player, primary: PathingEntity): Set<CoordGrid> {
        val centre = primary.coords
        return when (Direction.cardinalBetween(source.bounds(), primary.bounds())) {
            CardinalDirection.North, CardinalDirection.South ->
                setOf(centre.translate(-1, 0), centre, centre.translate(1, 0))

            CardinalDirection.East, CardinalDirection.West ->
                setOf(centre.translate(0, -1), centre, centre.translate(0, 1))
        }
    }

    fun select(
        source: ProtectedAccess,
        primary: PathingEntity,
        tiles: Set<CoordGrid>,
        npcLimit: Int,
        playerLimit: Int,
        totalLimit: Int = npcLimit + playerLimit,
        searchZoneRadius: Int = DEFAULT_SEARCH_ZONE_RADIUS,
    ): List<PathingEntity> {
        require(npcLimit > 0)
        require(playerLimit > 0)
        require(totalLimit > 0)
        require(searchZoneRadius >= 0)

        val targets = mutableListOf(primary)
        var npcCount = if (primary is Npc) 1 else 0
        var playerCount = if (primary is Player) 1 else 0
        val zone = ZoneKey.from(primary.coords)

        for (npc in npcs.findAll(zone, zoneRadius = searchZoneRadius)) {
            if (targets.size >= totalLimit || npcCount >= npcLimit) {
                break
            }
            if (npc === primary || !npc.occupies(tiles) || !canAttack(source.player, npc)) {
                continue
            }
            targets += npc
            npcCount++
        }

        for (player in players.findAll(zone, zoneRadius = searchZoneRadius)) {
            if (targets.size >= totalLimit || playerCount >= playerLimit) {
                break
            }
            if (
                player === source.player ||
                    player === primary ||
                    !player.occupies(tiles) ||
                    !pvp.canAttack(source.player, player)
            ) {
                continue
            }
            targets += player
            playerCount++
        }
        return targets
    }

    private fun canAttack(source: Player, target: Npc): Boolean {
        for (hook in npcAttackValidateHooks) {
            if (hook.validate(source, target) is NpcAttackValidateResult.Deny) {
                return false
            }
        }
        if (!target.isValidTarget()) {
            return false
        }
        return target.visType.hasOp(InteractionOp.Op2.slot)
    }

    private fun PathingEntity.occupies(tiles: Set<CoordGrid>): Boolean =
        bounds().asSequence().any { it in tiles }

    private companion object {
        /**
         * All affected tiles are at most two tiles from the source due to the weapon's attack
         * range, but a radius of one also safely covers a zone boundary. Larger specials can
         * opt into their exact search radius.
         */
        private const val DEFAULT_SEARCH_ZONE_RADIUS: Int = 1
    }
}
