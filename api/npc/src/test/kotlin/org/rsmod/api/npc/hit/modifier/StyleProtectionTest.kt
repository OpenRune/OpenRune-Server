package org.rsmod.api.npc.hit.modifier

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.InventoryServerType
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.npc.hit.InternalNpcHits
import org.rsmod.api.npc.hit.visHitmark
import org.rsmod.api.player.events.AttackKind
import org.rsmod.api.player.events.PlayerAttackEvents
import org.rsmod.api.player.events.beginAttackRecord
import org.rsmod.api.player.events.finishAttackRecord
import org.rsmod.api.player.events.prepareAttackRecord
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.Inventory
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class StyleProtectionTest {
    private val npc = npc()

    @Test
    fun `an unprotected style takes full damage, whatever the penetration`() {
        assertEquals(20, hit(HitType.Melee, 20))
        assertEquals(20, hit(HitType.Melee, 20, penetration = 50))
    }

    @Test
    fun `a full prayer blocks only its own style`() {
        npc.vars[PROTECT_MELEE] = 100

        assertEquals(0, hit(HitType.Melee, 20))
        assertEquals(20, hit(HitType.Ranged, 20))
        assertEquals(20, hit(HitType.Magic, 20))
    }

    @Test
    fun `penetration takes away from a full prayer`() {
        npc.vars[PROTECT_RANGED] = 100

        assertEquals(5, hit(HitType.Ranged, 20, penetration = 25))
        assertEquals(15, hit(HitType.Ranged, 20, penetration = 75))
        assertEquals(20, hit(HitType.Ranged, 20, penetration = 100))
        assertEquals(20, hit(HitType.Ranged, 20, penetration = 150))
    }

    @Test
    fun `a partial prayer reduces damage and penetration takes away from it`() {
        npc.vars[PROTECT_MAGIC] = 50

        assertEquals(10, hit(HitType.Magic, 20))
        assertEquals(15, hit(HitType.Magic, 20, penetration = 25))
        assertEquals(20, hit(HitType.Magic, 20, penetration = 50))
    }

    @Test
    fun `reduced damage rounds down`() {
        npc.vars[PROTECT_MELEE] = 100

        assertEquals(1, hit(HitType.Melee, 7, penetration = 25))
        assertEquals(0, hit(HitType.Melee, 1, penetration = 75))
    }

    @Test
    fun `no penetration gets through immunity`() {
        npc.vars[IMMUNE_MELEE] = 1

        assertEquals(0, hit(HitType.Melee, 20, penetration = 100))
    }

    @Test
    fun `immunity wins over a prayer the hit could penetrate`() {
        npc.vars[IMMUNE_MELEE] = 1
        npc.vars[PROTECT_MELEE] = 100

        assertEquals(0, hit(HitType.Melee, 20, penetration = 100))
    }

    @Test
    fun `typeless ignores both`() {
        npc.vars[IMMUNE_MELEE] = 1
        npc.vars[PROTECT_MELEE] = 100
        npc.vars[PROTECT_RANGED] = 100
        npc.vars[PROTECT_MAGIC] = 100

        assertEquals(20, hit(HitType.Typeless, 20))
    }

    @Test
    fun `a hit takes its penetration from the attack and not after it`() {
        val events = EventBus()
        val player = player()
        events.subscribeUnbound(PlayerAttackEvents.AttackPrepare::class.java) {
            prayerPenetration += 25
        }
        events.subscribeUnbound(PlayerAttackEvents.AttackPrepare::class.java) {
            prayerPenetration += 50
        }

        assertEquals(0, builderFrom(player).penetration)

        events.beginAttackRecord(player, npc, AttackKind.Melee, weapon = null, spell = null)
        events.prepareAttackRecord(player, baseCycles = 4)
        assertEquals(75, builderFrom(player).penetration)

        events.finishAttackRecord(player)
        assertEquals(0, builderFrom(player).penetration)
    }

    private fun hit(type: HitType, damage: Int, penetration: Int = 0): Int {
        val builder =
            InternalNpcHits.createBuilder(
                type = type,
                damage = damage,
                righthand = null,
                secondaryObj = null,
                hitmark = npc.visHitmark(),
                clientDelay = 0,
            )
        builder.penetration = penetration
        with(StandardNpcHitModifier(EventBus())) { builder.modify(npc) }
        return builder.damage
    }

    private fun builderFrom(player: Player) =
        InternalNpcHits.createBuilder(
            source = player,
            type = HitType.Melee,
            damage = 20,
            secondaryObj = null,
            hitmark = npc.visHitmark(),
            clientDelay = 0,
            specific = false,
        )

    @OptIn(InternalApi::class)
    private fun npc(): Npc =
        Npc(ServerCacheManager.getNpc("npc.bob".asRSCM(RSCMType.NPC))!!, CoordGrid.ZERO).apply {
            slotId = 1
            assignUid()
        }

    @OptIn(InternalApi::class)
    private fun player(): Player =
        Player().apply {
            slotId = 1
            uuid = 100L
            assignUid()
            worn = Inventory(InventoryServerType(size = 14), arrayOfNulls(14))
        }

    private companion object {
        const val IMMUNE_MELEE = "varn.immune_melee"
        const val PROTECT_MELEE = "varn.protect_melee"
        const val PROTECT_RANGED = "varn.protect_ranged"
        const val PROTECT_MAGIC = "varn.protect_magic"

        @JvmStatic
        @BeforeAll
        fun cache() {
            val root =
                generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                    .first { Files.isRegularFile(it.resolve("game.yml")) }
            val revision =
                Files.readAllLines(root.resolve("game.yml"))
                    .first { it.startsWith("revision:") }
                    .substringAfter(':')
                    .trim()
                    .substringBefore('.')
                    .toInt()
            ServerCacheManager.init(revision).close()
        }
    }
}
