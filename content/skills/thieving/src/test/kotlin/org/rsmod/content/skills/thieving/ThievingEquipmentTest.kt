package org.rsmod.content.skills.thieving

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory

private var Player.glovesFailures by intVarp("varp.gloves_of_silence_failures")
private var Player.dodgyUses by intVarp("varp.dodgy_necklace_uses")
private var Player.mediumDiary by boolVarBit("varbit.ardougne_diary_medium_complete")
private var Player.hardDiary by boolVarBit("varbit.ardougne_diary_hard_complete")
private var Player.eliteDiary by boolVarBit("varbit.ardougne_diary_elite_complete")

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class ThievingEquipmentTest {
    @Test
    fun `no bonuses leave the rates untouched`() {
        assertEquals(PickpocketRates(180, 240, glovesApplied = false), player().rates())
    }

    @Test
    fun `gloves of silence add five percent`() {
        assertEquals(PickpocketRates(189, 252, glovesApplied = true), player(GLOVES).rates())
    }

    @Test
    fun `the hard diary replaces the gloves bonus and stops them degrading`() {
        val player = player(GLOVES).apply { hardDiary = true }
        assertEquals(PickpocketRates(198, 264, glovesApplied = false), player.rates())
    }

    @Test
    fun `both thieving capes add ten percent and stack with the hard diary`() {
        assertEquals(PickpocketRates(198, 264, false), player(CAPE).rates())
        assertEquals(PickpocketRates(198, 264, false), player(TRIMMED_CAPE).rates())
        val player = player(CAPE).apply { hardDiary = true }
        assertEquals(PickpocketRates(217, 290, glovesApplied = false), player.rates())
    }

    @Test
    fun `pouch limit follows the highest ardougne diary`() {
        val player = player()
        assertEquals(28, ThievingEquipment.pouchLimit(player))
        player.mediumDiary = true
        assertEquals(56, ThievingEquipment.pouchLimit(player))
        player.hardDiary = true
        assertEquals(84, ThievingEquipment.pouchLimit(player))
        player.eliteDiary = true
        assertEquals(140, ThievingEquipment.pouchLimit(player))
    }

    @Test
    fun `each rogue piece adds fifteen percent and the full set always doubles`() {
        for (pieces in 0 until ROGUE.size) {
            val player = player(*ROGUE.take(pieces).toTypedArray())
            assertEquals(pieces * 15, ThievingEquipment.rogueDoubleChance(player))
        }
        assertEquals(100, ThievingEquipment.rogueDoubleChance(player(*ROGUE.toTypedArray())))
    }

    @Test
    fun `gloves condition follows the failures used`() {
        val expected =
            mapOf(
                0 to "Your gloves are new.",
                1 to "Your gloves are in good condition.",
                28 to "Your gloves are in good condition.",
                29 to "Your gloves are starting to look quite shabby.",
                42 to "Your gloves are starting to look quite shabby.",
                43 to "Your gloves are starting to need repair.",
                52 to "Your gloves are starting to need repair.",
                53 to "Your gloves are in need of repair!",
                60 to "Your gloves are in need of repair!",
                61 to "Your gloves are about to fall apart!",
            )
        val player = player()
        for ((failures, message) in expected) {
            player.glovesFailures = failures
            assertEquals(message, ThievingEquipment.glovesCondition(player), "at $failures")
        }
    }

    @Test
    fun `dodgy necklace counts down its charges`() {
        val player = player()
        assertEquals(
            "Your dodgy necklace has 10 charges left.",
            ThievingEquipment.dodgyCondition(player),
        )
        player.dodgyUses = 9
        assertEquals(
            "Your dodgy necklace has 1 charge left.",
            ThievingEquipment.dodgyCondition(player),
        )
    }

    private fun Player.rates(): PickpocketRates = ThievingEquipment.rates(this, 180, 240)

    private fun player(vararg worn: String): Player =
        Player().apply {
            val type = checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM()))
            this.worn = Inventory(type, arrayOfNulls(type.size))
            worn.forEachIndexed { slot, obj -> this.worn[slot] = InvObj(obj, 1) }
        }

    companion object {
        private const val GLOVES = ThievingEquipment.GLOVES_OF_SILENCE
        private const val CAPE = "obj.skillcape_thieving"
        private const val TRIMMED_CAPE = "obj.skillcape_thieving_trimmed"
        private val ROGUE =
            listOf(
                "obj.roguesden_helm",
                "obj.roguesden_body",
                "obj.roguesden_legs",
                "obj.roguesden_gloves",
                "obj.roguesden_boots",
            )

        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(241).close()
        }
    }
}
