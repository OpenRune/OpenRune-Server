package org.rsmod.api.combat.formulas.accuracy.magic

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import dev.openrune.definition.type.VarBitType
import dev.openrune.types.ItemServerType
import dev.openrune.types.npc.UnpackedNpcType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatStance
import org.rsmod.api.combat.formulas.test_npcs
import org.rsmod.api.combat.weapon.scripts.WeaponAttackStylesScript
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.back
import org.rsmod.api.player.feet
import org.rsmod.api.player.front
import org.rsmod.api.player.hands
import org.rsmod.api.player.hat
import org.rsmod.api.player.lefthand
import org.rsmod.api.player.legs
import org.rsmod.api.player.righthand
import org.rsmod.api.player.ring
import org.rsmod.api.player.torso
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.params.TestArgs
import org.rsmod.api.testing.params.TestArgsProvider
import org.rsmod.api.testing.params.TestWithArgs
import org.rsmod.api.testing.params.testArgsOfSingleParam
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj

class NvPMagicAccuracyTest {
    @TestWithArgs(MatchupProvider::class)
    fun `calculate matchup hit chance`(matchup: Matchup, state: GameTestState) =
        state.runInjectedGameTest(
            MagicAccuracyTestDependencies::class,
            MagicAccuracyTestModule,
            WeaponAttackStylesScript::class,
        ) {
            val accuracy = it.accuracy

            player.setCurrentLevel(stats.defence, matchup.defenceLvl)
            player.setBaseLevel(stats.defence, matchup.baseDefenceLvl)
            player.setCurrentLevel(stats.hitpoints, matchup.hitpoints)
            player.setBaseLevel(stats.hitpoints, matchup.baseHitpointsLvl)
            player.setCurrentLevel(stats.magic, matchup.magicLvl)
            player.setBaseLevel(stats.magic, matchup.baseMagicLvl)
            player.setVarp("varp.com_mode", matchup.blockStance.varValue)

            player.hat = matchup.hat
            player.back = matchup.back
            player.front = matchup.front
            player.righthand = matchup.righthand
            player.torso = matchup.torso
            player.lefthand = matchup.lefthand
            player.legs = matchup.legs
            player.hands = matchup.hands
            player.feet = matchup.feet
            player.ring = matchup.ring

            for (prayer in matchup.prayers) {
                player.setVarBit(prayer, 1)
            }

            val npc = Npc(matchup.npc)
            val accuracyRoll = accuracy.getHitChance(npc = npc, target = player)
            assertEquals(matchup.expectedAccuracy, accuracyRoll / 100.0)
        }

    data class Matchup(
        val expectedAccuracy: Double,
        val npc: UnpackedNpcType = test_npcs.man,
        val blockStance: CombatStance = CombatStance.Stance1,
        val hat: InvObj? = null,
        val back: InvObj? = null,
        val front: InvObj? = null,
        val righthand: InvObj? = null,
        val torso: InvObj? = null,
        val lefthand: InvObj? = null,
        val legs: InvObj? = null,
        val hands: InvObj? = null,
        val feet: InvObj? = null,
        val ring: InvObj? = null,
        val defenceLvl: Int = 99,
        val baseDefenceLvl: Int = 99,
        val hitpoints: Int = 99,
        val baseHitpointsLvl: Int = 99,
        val magicLvl: Int = 99,
        val baseMagicLvl: Int = 99,
        val prayers: Set<VarBitType> = emptySet(),
    ) {
        fun withNpcSource(npc: UnpackedNpcType) = copy(npc = npc)

        fun withBlockStance(stance: CombatStance) = copy(blockStance = stance)

        fun withHelm(obj: ItemServerType?) = copy(hat = obj?.let(::InvObj))

        fun withCape(obj: ItemServerType?) = copy(back = obj?.let(::InvObj))

        fun withAmulet(obj: ItemServerType?) = copy(front = obj?.let(::InvObj))

        fun withWeapon(obj: ItemServerType?) = copy(righthand = obj?.let(::InvObj))

        fun withBody(obj: ItemServerType?) = copy(torso = obj?.let(::InvObj))

        fun withShield(obj: ItemServerType?) = copy(lefthand = obj?.let(::InvObj))

        fun withLegs(obj: ItemServerType?) = copy(legs = obj?.let(::InvObj))

        fun withGloves(obj: ItemServerType?) = copy(hands = obj?.let(::InvObj))

        fun withFeet(obj: ItemServerType?) = copy(feet = obj?.let(::InvObj))

        fun withRing(obj: ItemServerType?) = copy(ring = obj?.let(::InvObj))

        fun withDefenceLevel(defenceLvl: Int) = copy(defenceLvl = defenceLvl)

        fun withMagicLevel(magicLvl: Int) = copy(magicLvl = magicLvl)

        fun withPrayers(vararg prayers: VarBitType) = copy(prayers = prayers.toSet())

        fun withSaturatedHeart(): Matchup {
            val add = 4 + (baseMagicLvl * 0.1).toInt()
            return copy(magicLvl = baseMagicLvl + add)
        }

        override fun toString(): String =
            "Matchup(" +
                "expectedAccuracy=$expectedAccuracy, " +
                "npc=${NpcStatFormat()}, " +
                "player=${PlayerStatFormat()}" +
                ")"

        private inner class NpcStatFormat {
            override fun toString(): String = "Npc(name=${npc.name})"
        }

        private inner class PlayerStatFormat {
            override fun toString(): String =
                "Player(" +
                    "blockStance=$blockStance, " +
                    "defenceLevel=$defenceLvl / $baseDefenceLvl, " +
                    "hitpoints=$hitpoints / $baseHitpointsLvl, " +
                    "prayers=${concatenatePrayers()}, " +
                    "worn=[${concatenateWorn()}]" +
                    ")"

            private fun concatenateWorn(): String {
                val filteredWorn =
                    listOfNotNull(
                        hat?.let { "helm=${it.id}" },
                        back?.let { "cape=${it.id}" },
                        front?.let { "amulet=${it.id}" },
                        righthand?.let { "weapon=${it.id}" },
                        torso?.let { "chest=${it.id}" },
                        lefthand?.let { "shield=${it.id}" },
                        legs?.let { "legs=${it.id}" },
                        hands?.let { "gloves=${it.id}" },
                        feet?.let { "feet=${it.id}" },
                        ring?.let { "ring=${it.id}" },
                    )
                return filteredWorn.joinToString(", ")
            }

            private fun concatenatePrayers(): String =
                if (prayers.isEmpty()) {
                    "None"
                } else {
                    prayers.joinToString(transform = VarBitType::internalNameValue)
                }
        }
    }

    // Each matchup runs a full integrated test scope, meaning it initializes an entire game
    // environment. Avoid excessive matchups to prevent unnecessary overhead.
    private object MatchupProvider : TestArgsProvider {
        override fun args(): List<TestArgs> {
            return testArgsOfSingleParam(
                Matchup(expectedAccuracy = 79.92).withNpcSource(test_npcs.dagannoth_prime),
                Matchup(expectedAccuracy = 82.95)
                    .withNpcSource(test_npcs.dagannoth_prime)
                    .withBlockStance(CombatStance.Stance4) // Unarmed "Block" stance.
                    .withMagicLevel(magicLvl = 50)
                    .withPrayers("varbit.prayer_augury"),
                Matchup(expectedAccuracy = 51.11)
                    .withNpcSource(test_npcs.dagannoth_prime)
                    .withSaturatedHeart()
                    .withHelm("obj.ancestral_hat")
                    .withCape("obj.ma2_saradomin_cape")
                    .withAmulet("obj.occult_necklace")
                    .withBody("obj.ancestral_robe_top")
                    .withLegs("obj.ancestral_robe_bottom")
                    .withGloves("obj.zenyte_bracelet_enchanted")
                    .withFeet("obj.eternal_boots")
                    .withRing("obj.magus_ring")
                    .withPrayers("varbit.prayer_mysticwill"),
                Matchup(expectedAccuracy = 83.38)
                    .withNpcSource(test_npcs.dagannoth_prime)
                    .withHelm("obj.torva_helm")
                    .withBody("obj.torva_chest")
                    .withLegs("obj.torva_legs")
                    .withCape("obj.infernal_cape")
                    .withAmulet("obj.amulet_of_rancour")
                    .withGloves("obj.ferocious_gloves")
                    .withFeet("obj.primordial_boots")
                    .withRing("obj.ultor_ring")
                    .withPrayers("varbit.prayer_piety")
                    .withDefenceLevel(defenceLvl = 75),
            )
        }
    }

    private class MagicAccuracyTestDependencies @Inject constructor(val accuracy: NvPMagicAccuracy)

    private object MagicAccuracyTestModule : AbstractModule() {
        override fun configure() {
            bind(AttackStyles::class.java).`in`(Scopes.SINGLETON)
        }
    }
}
