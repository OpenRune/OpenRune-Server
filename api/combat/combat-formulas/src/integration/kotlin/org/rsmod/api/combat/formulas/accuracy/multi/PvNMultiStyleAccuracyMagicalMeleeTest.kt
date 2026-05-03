package org.rsmod.api.combat.formulas.accuracy.multi

import com.google.inject.Inject
import dev.openrune.definition.type.VarBitType
import dev.openrune.types.ItemServerType
import dev.openrune.types.NpcServerType
import org.rsmod.api.combat.commons.styles.MeleeAttackStyle
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.test_npcs
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

class PvNMultiStyleAccuracyMagicalMeleeTest {
    @TestWithArgs(MatchupProvider::class)
    fun `calculate matchup hit chance`(matchup: Matchup, state: GameTestState) =
        state.runInjectedGameTest(MultiStyleAccuracyTestDependencies::class) {
            val accuracy = it.accuracy

            player.setCurrentLevel(stats.attack, matchup.attackLvl)
            player.setBaseLevel(stats.attack, matchup.baseAttackLvl)
            player.setCurrentLevel(stats.hitpoints, matchup.hitpoints)
            player.setBaseLevel(stats.hitpoints, matchup.baseHitpointsLvl)

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
            npc.hitpoints = matchup.npcCurrHp
            npc.baseHitpointsLvl = matchup.npcMaxHp

            val accuracyRoll =
                accuracy.getMagicalMeleeHitChance(
                    player = player,
                    target = npc,
                    attackType = matchup.attackType,
                    attackStyle = matchup.attackStyle,
                    specialMultiplier = matchup.specMultiplier,
                )
            assertEquals(matchup.expectedAccuracy, accuracyRoll / 100.0)
        }

    data class Matchup(
        val expectedAccuracy: Double,
        val npc: NpcServerType = test_npcs.man,
        val npcCurrHp: Int = 1,
        val npcMaxHp: Int = 1,
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
        val attackLvl: Int = 99,
        val baseAttackLvl: Int = 99,
        val hitpoints: Int = 99,
        val baseHitpointsLvl: Int = 99,
        val prayers: Set<VarBitType> = emptySet(),
        val attackType: MeleeAttackType? = null,
        val attackStyle: MeleeAttackStyle? = null,
        val specMultiplier: Double = 1.0,
    ) {
        fun withNpcTarget(npc: NpcServerType) = copy(npc = npc)

        fun withHelm(obj: ItemServerType?) = copy(hat = obj?.let(::InvObj))

        fun withCape(obj: ItemServerType?) = copy(back = obj?.let(::InvObj))

        fun withAmulet(obj: ItemServerType?) = copy(front = obj?.let(::InvObj))

        fun withWeapon(obj: ItemServerType?) = copy(righthand = obj?.let(::InvObj))

        fun withBody(obj: ItemServerType?) = copy(torso = obj?.let(::InvObj))

        fun withLegs(obj: ItemServerType?) = copy(legs = obj?.let(::InvObj))

        fun withGloves(obj: ItemServerType?) = copy(hands = obj?.let(::InvObj))

        fun withFeet(obj: ItemServerType?) = copy(feet = obj?.let(::InvObj))

        fun withRing(obj: ItemServerType?) = copy(ring = obj?.let(::InvObj))

        fun withPrayers(vararg prayers: VarBitType) = copy(prayers = prayers.toSet())

        fun withAttackType(attackType: MeleeAttackType?) = copy(attackType = attackType)

        fun withAttackStyle(attackStyle: MeleeAttackStyle?) = copy(attackStyle = attackStyle)

        override fun toString(): String =
            "Matchup(" +
                "expectedAccuracy=$expectedAccuracy, " +
                "npc=${NpcStatFormat()}, " +
                "player=${PlayerStatFormat()}" +
                ")"

        private inner class NpcStatFormat {
            override fun toString(): String =
                "Npc(" + "name=${npc.name}, " + "hitpoints=$npcCurrHp / $npcMaxHp" + ")"
        }

        private inner class PlayerStatFormat {
            override fun toString(): String {
                return "Player(" +
                    "attackType=$attackType, " +
                    "attackStyle=$attackStyle, " +
                    "specMultiplier=$specMultiplier, " +
                    "attackLevel=$attackLvl / $baseAttackLvl, " +
                    "hitpoints=$hitpoints / $baseHitpointsLvl, " +
                    "prayers=${concatenatePrayers()}, " +
                    "worn=[${concatenateWorn()}]" +
                    ")"
            }

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
                Matchup(expectedAccuracy = 98.80)
                    .withAttackType(MeleeAttackType.Slash)
                    .withAttackStyle(MeleeAttackStyle.Accurate)
                    .withWeapon("obj.blessed_saradomin_sword")
                    .withNpcTarget(test_npcs.man),
                Matchup(expectedAccuracy = 41.26)
                    .withAttackType(MeleeAttackType.Slash)
                    .withAttackStyle(MeleeAttackStyle.Aggressive)
                    .withHelm("obj.torva_helm")
                    .withBody("obj.torva_chest")
                    .withLegs("obj.torva_legs")
                    .withCape("obj.infernal_cape")
                    .withAmulet("obj.amulet_of_rancour")
                    .withWeapon("obj.blessed_saradomin_sword")
                    .withGloves("obj.ferocious_gloves")
                    .withFeet("obj.primordial_boots")
                    .withRing("obj.ultor_ring")
                    .withPrayers("varbit.prayer_piety")
                    .withNpcTarget(test_npcs.general_graardor),
            )
        }
    }

    private class MultiStyleAccuracyTestDependencies
    @Inject
    constructor(val accuracy: PvNMultiStyleAccuracy)
}
