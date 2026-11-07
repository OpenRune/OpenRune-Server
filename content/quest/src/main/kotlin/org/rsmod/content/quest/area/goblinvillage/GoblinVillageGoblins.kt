package org.rsmod.content.quest.area.goblinvillage

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import dev.openrune.types.hunt.HuntVis
import jakarta.inject.Inject
import kotlin.math.abs
import org.rsmod.api.config.refs.params
import org.rsmod.api.hunt.NpcSearch
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onAiTimer
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.StepValidator
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

class GoblinVillageGoblins
@Inject
constructor(
    private val aiPlayerInteractions: AiPlayerInteractions,
    private val search: NpcSearch,
    private val npcList: NpcList,
    private val random: GameRandom,
    private val worldRepo: WorldRepository,
    private val hitModifier: NpcHitModifier,
    collision: CollisionFlagMap,
) : PluginScript() {
    private val steps = StepValidator(collision)

    override fun ScriptContext.startup() {
        for (id in SOLDIERS) {
            for (red in listOf(true, false)) {
                val soldier = soldier(red, id)
                onOpNpc1(soldier) { talk(it.npc, red) }
                onAiTimer(soldier) { fight(npc, red) }
            }
        }
    }

    private fun fight(npc: Npc, red: Boolean) {
        val enemy = enemy(npc, red)
        if (enemy != null) {
            if (
                enemy(enemy, !red) != npc ||
                    !npc.coords.isCardinal(enemy.coords) ||
                    random.of(maxExclusive = END_CHANCE) == 0
            ) {
                npc.defaultMode()
                npc.aiTimer(npc.type.timer)
                return
            }
            swing(npc, enemy, red)
            return
        }
        if (!npc.isIdle() || random.of(maxExclusive = START_CHANCE) != 0) return
        val (target, tile) =
            search
                .findAllAny(npc.coords, FIGHT_RANGE, HuntVis.LineOfSight)
                .filter { isSoldier(it, !red) && it.isIdle() && it.isValidTarget() }
                .mapNotNull { enemy -> approach(npc, enemy)?.let { enemy to it } }
                .toList()
                .let { random.pickOrNull(it) } ?: return
        for ((from, to) in listOf(npc to target, target to npc)) {
            from.noneMode()
            from.faceNpc(to)
        }
        if (tile != npc.coords) npc.walk(tile)
        target.aiTimer(ATTACK_RATE / 2 + target.laterThan(npc))
        swing(npc, target, red)
    }

    private fun approach(npc: Npc, enemy: Npc): CoordGrid? {
        val from = npc.coords
        if (from.isCardinal(enemy.coords)) return from
        return STEPS.filter { (dx, dz) ->
                from.translate(dx, dz).isCardinal(enemy.coords) &&
                    steps.canTravel(from.level, from.x, from.z, dx, dz, extraFlag = CollisionFlag.BLOCK_NPCS)
            }
            .let { random.pickOrNull(it) }
            ?.let { (dx, dz) -> from.translate(dx, dz) }
    }

    private fun CoordGrid.isCardinal(other: CoordGrid): Boolean =
        level == other.level && abs(x - other.x) + abs(z - other.z) == 1

    private fun swing(npc: Npc, enemy: Npc, red: Boolean) {
        npc.aiTimer(ATTACK_RATE)
        npc.visType.paramOrNull(params.attack_anim)?.let { npc.anim(RSCM.getReverseMapping(RSCMType.SEQ, it.id)) }
        enemy.visType.paramOrNull(params.defend_anim)?.let { enemy.anim(RSCM.getReverseMapping(RSCMType.SEQ, it.id)) }
        npc.visType.paramOrNull(params.attack_sound)?.let {
            worldRepo.soundArea(npc, RSCM.getReverseMapping(RSCMType.SYNTH, it.id), radius = SOUND_RANGE)
        }
        enemy.visType.paramOrNull(params.defend_sound)?.let {
            worldRepo.soundArea(
                enemy,
                RSCM.getReverseMapping(RSCMType.SYNTH, it.id),
                delay = HIT_SOUND_DELAY,
                radius = SOUND_RANGE,
            )
        }
        val damage = if (enemy.hitpoints > 1) random.of(maxExclusive = 2) else 0
        enemy.queueHit(
            npc,
            delay = 1 + enemy.laterThan(npc),
            type = HitType.Melee,
            damage = damage,
            modifier = hitModifier,
        )
        if (random.of(maxExclusive = SHOUT_CHANCE) < SHOUTS_PER_CHANCE) {
            npc.say(random.pick(if (red) RED_SHOUTS else GREEN_SHOUTS))
        }
    }

    private fun Npc.laterThan(other: Npc): Int = if (slotId > other.slotId) 1 else 0

    private fun Npc.isIdle(): Boolean =
        interaction == null && !isFacingNpc && (mode ?: defaultMode) == defaultMode

    private fun enemy(npc: Npc, red: Boolean): Npc? =
        npc.facingTarget(npcList)?.takeIf { npc.mode == NpcMode.None && isSoldier(it, !red) }

    private suspend fun ProtectedAccess.talk(npc: Npc, red: Boolean) {
        val questDone = vars["varbit.gobdip_main"] >= QUEST_COMPLETE
        val enemy = enemy(npc, red)
        startDialogue(npc) {
            when {
                questDone -> afterQuest(npc, red)
                enemy != null -> whyFighting(npc, red, enemy)
                else -> armourBest(red)
            }
        }
    }

    private suspend fun Dialogue.whyFighting(npc: Npc, red: Boolean, enemy: Npc) {
        val enemyType = RSCM.getReverseMapping(RSCMType.NPC, enemy.id)
        if (access.random.of(maxExclusive = 2) == 0) {
            chatNpc(angry, if (red) "Red armour best!" else "Green armour best.")
            chatNpcSpecific(GOBLIN, enemyType, angry, if (red) "Green armour best." else "Red armour best!")
            return
        }
        chatPlayer(quiz, "Why are you fighting?")
        if (red) {
            chatNpc(angry, "He wearing green armour! General Bentnoze tell us wear red!")
            chatNpcSpecific(GOBLIN, enemyType, angry, "But General Wartface say we must wear green!")
        } else {
            chatNpc(angry, "He wearing red armour! General Wartface tell us wear green!")
            chatNpcSpecific(GOBLIN, enemyType, angry, "But General Bentnoze say we must wear red!")
        }
    }

    private suspend fun Dialogue.armourBest(red: Boolean) {
        val colour = if (red) "red" else "green"
        chatNpc(angry, if (red) "Red armour best!" else "Green armour best!")
        when (choice2("Why is $colour best?", 1, "Err, okay.", 2)) {
            1 -> {
                chatPlayer(quiz, "Why is red best?")
                chatNpc(neutral, "Cos General Bentnoze says so, and he bigger than me.")
            }
            else -> chatPlayer(confused, "Err, okay.")
        }
    }

    private suspend fun Dialogue.afterQuest(npc: Npc, red: Boolean) {
        when (access.random.of(maxExclusive = 5)) {
            0 -> attack(npc, "I kill you human!")
            1 -> attack(npc, "Go away smelly human!")
            2 -> {
                chatNpc(quiz, "What you doing here?")
                when (
                    choice2("I'm here to kill all you goblins!", 1, "I'm just looking around.", 2)
                ) {
                    1 -> {
                        chatPlayer(angry, "I'm here to kill all you goblins!")
                        attack(npc, "I kill you!")
                    }
                    else -> {
                        chatPlayer(neutral, "I'm just looking around.")
                        chatNpc(neutral, "Me not sure that allowed. You have to check with generals.")
                    }
                }
            }
            3 -> newCentury(npc)
            else -> brownBest(red)
        }
    }

    private suspend fun Dialogue.newCentury(npc: Npc) {
        chatNpc(happy, "Happy goblin new century!")
        when (choice2("Happy new century!", 1, "What is the goblin new century?", 2)) {
            1 -> {
                chatPlayer(happy, "Happy new century!")
                return
            }
            else -> chatPlayer(quiz, "What is the goblin new century?")
        }
        chatNpc(
            neutral,
            "Goblin century mark year of big battle on Plain of Mud. That when Big High War God " +
                "give us commandments.",
        )
        when (
            choice4(
                "Who is the Big High War God?",
                1,
                "What are the goblin commandments?",
                2,
                "Where is the Plain of Mud?",
                3,
                "I need to go.",
                4,
            )
        ) {
            1 -> {
                chatPlayer(quiz, "Who is the Big High War God?")
                chatNpc(
                    neutral,
                    "Big High War God take goblins and make them strong! Without him, we small " +
                        "and weak and stupid. But thanks to Big High War God, we most powerful " +
                        "race in Gielinor!",
                )
                chatPlayer(neutral, "Umm... you're clearly not the most powerful race in Gielinor.")
                attack(npc, "Not to doubt word of Big High War God! I kill you!")
            }
            2 -> {
                chatPlayer(quiz, "What are the goblin commandments?")
                chatNpc(
                    neutral,
                    "Slay enemies of Big High War God! Not show mercy! Not run from battle! Not " +
                        "doubt word of Big High War God!",
                )
                attack(
                    npc,
                    "Without commandments we not know right from wrong. We be like other races " +
                        "who not know war is good!",
                )
            }
            3 -> {
                chatPlayer(quiz, "Where is the Plain of Mud?")
                attack(npc, "That goblin secret! No human ever find Plain of Mud!")
            }
            else -> chatPlayer(neutral, "I need to go.")
        }
    }

    private suspend fun Dialogue.brownBest(red: Boolean) {
        val wearing = if (red) "red" else "green"
        chatNpc(happy, "Brown armour best!")
        when (
            choice3("Why is brown best?", 1, "But you're still wearing $wearing!", 2, "Err, okay.", 3)
        ) {
            1 -> {
                chatPlayer(quiz, "Why is brown best?")
                chatNpc(
                    neutral,
                    "General Wartface and General Bentnoze both say it is. And normally they " +
                        "never agree!",
                )
            }
            2 -> {
                chatPlayer(quiz, "But you're still wearing $wearing armour!")
                if (red) {
                    chatNpc(neutral, "It not red, it just red-ish brown!")
                } else {
                    chatNpc(neutral, "Um... my brown armour getting wash.")
                }
            }
            else -> chatPlayer(confused, "Err, okay.")
        }
    }

    private suspend fun Dialogue.attack(npc: Npc, text: String) {
        chatNpc(angry, text)
        npc.opPlayer2(player, aiPlayerInteractions)
    }

    private fun soldier(red: Boolean, id: Int): String =
        if (red) "npc.goblin_red_soldier_$id" else "npc.goblin_green_soldier_$id"

    private fun isSoldier(npc: Npc, red: Boolean): Boolean =
        SOLDIERS.any { npc.id == soldier(red, it).asRSCM(RSCMType.NPC) }

    private companion object {
        private const val QUEST_COMPLETE = 6
        private const val GOBLIN = "Goblin"
        private const val START_CHANCE = 300
        private const val END_CHANCE = 600
        private const val ATTACK_RATE = 6
        private const val SHOUT_CHANCE = 10
        private const val SHOUTS_PER_CHANCE = 6
        private const val SOUND_RANGE = 10
        private const val HIT_SOUND_DELAY = 30
        private const val FIGHT_RANGE = 2
        private val STEPS = (-1..1).flatMap { dx -> (-1..1).map { dz -> dx to dz } } - (0 to 0)
        private val SOLDIERS = 2..8
        private val RED_SHOUTS =
            listOf("Red armour best!", "Green armour stupid!", "Red not green!", "Red red red!", "Stupid greenie!")
        private val GREEN_SHOUTS =
            listOf("Green!", "Green armour best!", "Red armour stupid!", "Green not red!", "Stupid reddie!")
    }
}
