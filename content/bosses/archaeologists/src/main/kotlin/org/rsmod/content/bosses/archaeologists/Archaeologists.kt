package org.rsmod.content.bosses.archaeologists

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.ProjectileConfig
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private const val CRAZY_ARCHAEOLOGIST = "npc.crazy_archaeologist"
private const val DERANGED_ARCHAEOLOGIST = "npc.fossil_crazy_archaeologist"

private const val CRAZY_MAX_HIT = 14
private const val CRAZY_SPECIAL_MAX_HIT = 24
private const val CRAZY_SPECIAL_SPLASH_HIT = 8
private const val DERANGED_MAX_HIT = 25
private const val DERANGED_SPECIAL_MAX_HIT = 56
private const val DERANGED_SPECIAL_SPLASH_HIT = 18

private const val ATTACK_RATE = 3
private const val SPECIAL_COOLDOWN = 25
private const val NORMAL_WEIGHT = 2
private const val ADJACENT_WEIGHT = 1
private const val SPECIAL_WEIGHT = 1

private const val BOOK_THROW_SEQ = "seq.book_chuck"
private const val BOOK_MELEE_SEQ = "seq.human_unarmedpunch"
private const val BOOK_PROJECTILE = "spotanim.crazy_archaeologist_book"
private const val BOOK_IMPACT_SPOTANIM = "spotanim.castlewars_catapult_splash"
private const val BOOK_THROW_SYNTH = "synth.thrown"
private const val BOOK_IMPACT_SYNTH = "synth.chinchompa_explode"

private val BOOK_PROJECTILE_CONFIG =
    ProjectileConfig(
        startHeight = 40,
        endHeight = 36,
        startDelay = 41,
        travelTime = 5,
        angle = 15,
        progress = 11,
        stepMultiplier = 5,
    )

private const val CRAZY_SPECIAL_QUOTE = "Rain of knowledge!"
private const val CRAZY_DEATH_QUOTE = "Ow!"
private const val DERANGED_SPECIAL_QUOTE = "Learn to Read!"
private const val DERANGED_DEATH_QUOTE = "Oh!"

private val CRAZY_QUOTES =
    listOf(
        "You belong in a museum!",
        "I'm Bellock - respect me!",
        "Get off my site!",
        "These ruins are mine!",
        "Taste my knowledge!",
        "No-one messes with Bellock's dig!",
    )

private val DERANGED_QUOTES =
    listOf(
        "The plants! They're alive!",
        "The doors won't stay closed forever!",
        "They came from the ground! They came from the ground!!!",
        "Round and round and round and round!",
        "Time is running out! She will rise again!!!",
        "They're cheering! Why are they cheering?",
    )

class Archaeologists @Inject constructor(private val deps: BossDeps) : PluginScript() {
    private val crazySpec =
        archaeologistSpec(
            npc = CRAZY_ARCHAEOLOGIST,
            maxHit = CRAZY_MAX_HIT,
            specialMaxHit = CRAZY_SPECIAL_MAX_HIT,
            specialSplashHit = CRAZY_SPECIAL_SPLASH_HIT,
            quotes = CRAZY_QUOTES,
            specialQuote = CRAZY_SPECIAL_QUOTE,
        )

    private val derangedSpec =
        archaeologistSpec(
            npc = DERANGED_ARCHAEOLOGIST,
            maxHit = DERANGED_MAX_HIT,
            specialMaxHit = DERANGED_SPECIAL_MAX_HIT,
            specialSplashHit = DERANGED_SPECIAL_SPLASH_HIT,
            quotes = DERANGED_QUOTES,
            specialQuote = DERANGED_SPECIAL_QUOTE,
        )

    override fun ScriptContext.startup() {
        deps.extensionRegistry.register(BookRain.EXTENSION) { _, npc, target, params ->
            BookRain.launch(deps, npc, target, params)
        }
        BossCombat.register(this, crazySpec, deps, onLethal = { npc -> npc.say(CRAZY_DEATH_QUOTE) })
        BossCombat.register(
            this,
            derangedSpec,
            deps,
            onLethal = { npc -> npc.say(DERANGED_DEATH_QUOTE) },
        )
    }
}

private fun archaeologistSpec(
    npc: String,
    maxHit: Int,
    specialMaxHit: Int,
    specialSplashHit: Int,
    quotes: List<String>,
    specialQuote: String,
): BossSpec =
    boss(npc) {
        stats(attackRate = ATTACK_RATE)

        val combatQuote = oneOf(quotes.map { say(it) })

        val melee =
            ability("melee") {
                anim(BOOK_MELEE_SEQ)
                include(combatQuote)
                hit {
                    damage(Accuracy(Roll(0..maxHit), meleeAttackType = MeleeAttackType.Crush))
                    type(Melee)
                }
            }

        val ranged =
            ability("ranged") {
                anim(BOOK_THROW_SEQ)
                include(combatQuote)
                sound(BOOK_THROW_SYNTH, radius = 8)
                projectile(
                    spotanim = BOOK_PROJECTILE,
                    config = BOOK_PROJECTILE_CONFIG,
                    onImpact = sound(BOOK_IMPACT_SYNTH, radius = 1, at = ImpactTile),
                    resolveOnImpact = true,
                    hit =
                        Effect.Hit(
                            damage = Accuracy(Roll(0..maxHit)),
                            type = Ranged,
                            spotanim = BOOK_IMPACT_SPOTANIM,
                            missSpotanim = BOOK_IMPACT_SPOTANIM,
                        ),
                )
            }

        val special =
            ability("special") {
                anim(BOOK_THROW_SEQ)
                say(specialQuote)
                include(
                    external(
                        BookRain.EXTENSION,
                        params = BookRainConfig(specialMaxHit, specialSplashHit),
                    )
                )
            }

        phase("fight") {
            weightedSelectorRandom {
                +random(melee, weight = ADJACENT_WEIGHT, requires = WithinMeleeRange)
                +random(ranged, weight = ADJACENT_WEIGHT, requires = WithinMeleeRange)
                +random(ranged, weight = NORMAL_WEIGHT, requires = !WithinMeleeRange)
                +random(special, weight = SPECIAL_WEIGHT, cooldown = SPECIAL_COOLDOWN)
            }
        }
    }
