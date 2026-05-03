package org.rsmod.api.combat.formulas.accuracy.magic

import com.google.inject.Inject
import dev.openrune.definition.type.VarBitType
import dev.openrune.types.ItemServerType
import dev.openrune.types.npc.UnpackedNpcType
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.combat.formulas.test_npcs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.back
import org.rsmod.api.player.feet
import org.rsmod.api.player.front
import org.rsmod.api.player.hands
import org.rsmod.api.player.hat
import org.rsmod.api.player.lefthand
import org.rsmod.api.player.legs
import org.rsmod.api.player.quiver
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

class PvNMagicSpellAccuracyTest {
    @TestWithArgs(MatchupProvider::class)
    fun `calculate matchup hit chance`(matchup: Matchup, state: GameTestState) =
        state.runInjectedGameTest(SpellAccuracyTestDependencies::class) {
            val accuracy = it.accuracy

            player.setCurrentLevel(stats.magic, matchup.magicLvl)
            player.setBaseLevel(stats.magic, matchup.baseMagicLvl)
            player.setCurrentLevel(stats.hitpoints, matchup.hitpoints)
            player.setBaseLevel(stats.hitpoints, matchup.baseHitpointsLvl)

            player.hat = matchup.hat
            player.back = matchup.back
            player.front = matchup.front
            player.quiver = matchup.quiver
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
                accuracy.getSpellHitChance(
                    player = player,
                    target = npc,
                    spell = matchup.spell,
                    spellbook = matchup.spellbook,
                    usedSunfireRune = false,
                )
            assertEquals(matchup.expectedAccuracy, accuracyRoll / 100.0)
        }

    data class Matchup(
        val expectedAccuracy: Double,
        val spell: ItemServerType = "obj.01_wind_strike",
        val spellbook: Spellbook = Spellbook.Standard,
        val npc: UnpackedNpcType = test_npcs.man,
        val npcCurrHp: Int = 1,
        val npcMaxHp: Int = 1,
        val hat: InvObj? = null,
        val back: InvObj? = null,
        val front: InvObj? = null,
        val quiver: InvObj? = null,
        val righthand: InvObj? = null,
        val torso: InvObj? = null,
        val lefthand: InvObj? = null,
        val legs: InvObj? = null,
        val hands: InvObj? = null,
        val feet: InvObj? = null,
        val ring: InvObj? = null,
        val magicLvl: Int = 99,
        val baseMagicLvl: Int = 99,
        val hitpoints: Int = 99,
        val baseHitpointsLvl: Int = 99,
        val prayers: Set<VarBitType> = emptySet(),
    ) {
        fun withSpell(spell: ItemServerType): Matchup = copy(spell = spell)

        fun withNpcTarget(npc: UnpackedNpcType) = copy(npc = npc)

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

        fun withPrayers(vararg prayers: VarBitType) = copy(prayers = prayers.toSet())

        fun withSaturatedHeart(): Matchup {
            val add = 4 + (baseMagicLvl * 0.1).toInt()
            return copy(magicLvl = baseMagicLvl + add)
        }

        fun withSmellingSalts(): Matchup {
            val add = 11 + (baseMagicLvl * 0.16).toInt()
            return copy(magicLvl = baseMagicLvl + add)
        }

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
            override fun toString(): String =
                "Player(" +
                    "magicLevel=$magicLvl / $baseMagicLvl, " +
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
                Matchup(expectedAccuracy = 97.30)
                    .withSpell("obj.01_wind_strike")
                    .withWeapon("obj.staff_of_air")
                    .withNpcTarget(test_npcs.man),
                Matchup(expectedAccuracy = 98.57)
                    .withSpell("obj.01_wind_strike")
                    .withWeapon("obj.staff_of_air")
                    .withHelm("obj.game_pest_mage_helm")
                    .withBody("obj.elite_void_knight_top")
                    .withLegs("obj.elite_void_knight_robes")
                    .withGloves("obj.pest_void_knight_gloves")
                    .withFeet("obj.eternal_boots")
                    .withRing("obj.magus_ring")
                    .withNpcTarget(test_npcs.man),
                Matchup(expectedAccuracy = 98.87)
                    .withSpell("obj.05_water_strike")
                    .withSmellingSalts()
                    .withWeapon("obj.staff_of_light")
                    .withHelm("obj.barrows_ahrim_head_100")
                    .withCape("obj.ma2_saradomin_cape")
                    .withAmulet("obj.occult_necklace")
                    .withBody("obj.barrows_ahrim_body_100")
                    .withLegs("obj.barrows_ahrim_legs_100")
                    .withGloves("obj.hundred_gauntlets_level_10")
                    .withFeet("obj.magictraining_infinityboots")
                    .withRing("obj.nzone_seer_ring")
                    .withPrayers("varbit.prayer_mystic_vigour_unlocked", "varbit.prayer_mysticmight")
                    .withNpcTarget(test_npcs.dagannoth_rex),
                Matchup(expectedAccuracy = 14.76)
                    .withSpell("obj.01_wind_strike")
                    .withWeapon("obj.staff_of_light")
                    .withHelm("obj.ancestral_hat")
                    .withCape("obj.ma2_saradomin_cape")
                    .withAmulet("obj.occult_necklace")
                    .withBody("obj.ancestral_robe_top")
                    .withLegs("obj.ancestral_robe_bottom")
                    .withGloves("obj.zenyte_bracelet_enchanted")
                    .withFeet("obj.eternal_boots")
                    .withRing("obj.magus_ring")
                    .withNpcTarget(test_npcs.corporeal_beast),
                Matchup(expectedAccuracy = 20.36)
                    .withSpell("obj.95_fire_surge")
                    .withSaturatedHeart()
                    .withWeapon("obj.staff_of_light")
                    .withHelm("obj.ancestral_hat")
                    .withCape("obj.ma2_saradomin_cape")
                    .withAmulet("obj.occult_necklace")
                    .withBody("obj.ancestral_robe_top")
                    .withLegs("obj.ancestral_robe_bottom")
                    .withGloves("obj.zenyte_bracelet_enchanted")
                    .withFeet("obj.eternal_boots")
                    .withRing("obj.magus_ring")
                    .withPrayers("varbit.prayer_augury")
                    .withNpcTarget(test_npcs.corporeal_beast),
            )
        }
    }

    private class SpellAccuracyTestDependencies @Inject constructor(val accuracy: PvNMagicAccuracy)
}
