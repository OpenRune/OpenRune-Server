package org.rsmod.content.skills.agility.werewolf

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.hunt.HuntVis
import jakarta.inject.Inject
import kotlin.math.sign
import org.rsmod.api.hunt.NpcSearch
import org.rsmod.api.npc.owner.assignSpawnOwner
import org.rsmod.api.npc.owner.isSpawnOwnedBy
import org.rsmod.api.npc.owner.ownedBy
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.midiJingle
import org.rsmod.api.player.output.MiscOutput
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.subjectPronoun
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onAiTimer
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onOpNpc5
import org.rsmod.api.table.agility.AgilitySkullballRow
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.StepValidator
import org.rsmod.routefinder.collision.CollisionFlagMap

/**
 * Werewolf Skullball (https://oldschool.runescape.wiki/w/Werewolf_Skullball). The Skullball Boss
 * gives a player wearing a ring of Charos a ball of their own, which rolls out of a spawnhole. Tap,
 * Kick and Shoot roll it 1, 4 or 9 tiles away from the player, a tile a tick, and the ball's ai
 * timer moves it on. The ten goals count in order and the final shot into the hole ends the game,
 * paying xp by the time since the first goal. The game lives in the owner's temporary varbits; a
 * ball whose owner has gone or left the area is removed.
 */
class SkullballScript
@Inject
constructor(
    private val mapClock: MapClock,
    private val collision: CollisionFlagMap,
    private val npcRepo: NpcRepository,
    private val npcs: NpcList,
    private val players: PlayerList,
    private val locRepo: LocRepository,
    private val worldRepo: WorldRepository,
    private val search: NpcSearch,
    private val random: GameRandom,
) : PluginScript() {
    private lateinit var course: AgilitySkullballRow
    private lateinit var pitch: SkullballPitch

    private val Dialogue.werewolf
        get() = mesanim(WEREWOLF_MESANIM)

    override fun ScriptContext.startup() {
        course = AgilitySkullballRow.getRow(COURSE)
        val steps = StepValidator(collision)
        pitch =
            SkullballPitch(
                goals = course.goals.map { (centre, way) -> SkullballGoal.of(centre, way) },
                hole = course.hole.first().let { (tile, way) -> SkullballGoal.of(tile, way) },
                blocked = { from, dx, dz -> !steps.canTravel(from.level, from.x, from.z, dx, dz) },
            )
        onOpNpc1(BOSS) { startDialogue(it.npc) { boss() } }
        onOpNpc1(TRAINER) { startDialogue(it.npc) { trainer() } }
        onOpNpc1(BALL) { strike(it.npc, TAP_TILES) }
        onOpNpc3(BALL) { strike(it.npc, KICK_TILES) }
        onOpNpc4(BALL) { strike(it.npc, SHOOT_TILES) }
        onOpNpc5(BALL) { showGoal(it.npc) }
        onAiTimer(BALL) { roll(npc) }
    }

    private suspend fun Dialogue.boss() {
        if (!player.wearsRing()) {
            chatNpc(werewolf, "Grrr - you don't belong in here, human!")
            return
        }
        val ball = ballOf(player)
        if (ball == null) {
            val play =
                choice2(
                    "I would like to do the skullball course.",
                    true,
                    "What are the instructions for the skullball course?",
                    false,
                )
            if (!play) {
                chatPlayer(quiz, INSTRUCTIONS)
                instructions()
                return
            }
            chatPlayer(happy, "I think I'll give the Skullball Course a go.")
            if (canPlay()) {
                newBall(player)
            }
            return
        }
        when (choice3(INSTRUCTIONS, 1, LOST_BALL, 2, GIVE_UP, 3)) {
            1 -> {
                chatPlayer(quiz, INSTRUCTIONS)
                instructions()
            }
            2 -> {
                chatPlayer(sad, LOST_BALL)
                chatNpc(
                    werewolf,
                    "No problem, here's another one. You'll have to start from the beginning " +
                        "again, but the timer will be restarted too.",
                )
                remove(ball, player)
                newBall(player)
            }
            else -> {
                chatPlayer(sad, GIVE_UP)
                chatNpc(werewolf, "Oh dear, such a defeatist.")
                remove(ball, player)
            }
        }
    }

    private suspend fun Dialogue.instructions() {
        chatNpc(
            werewolf,
            "The skullball comes out of one of these four spawnholes. Just kick the ball through " +
                "the middle of each goal, through the skeleton's feet.",
        )
        chatNpc(
            werewolf,
            "There are 10 goals, which you must complete in order, and one final goal.",
        )
        chatNpc(
            werewolf,
            "An arrow will point to your ball, just in case lots of people are using the course " +
                "at the same time as yourself.",
        )
        chatNpc(
            werewolf,
            "The better your time, the more agillity XP you will be awarded. The timer starts " +
                "when you score your first goal.",
        )
    }

    private suspend fun Dialogue.canPlay(): Boolean {
        if (player.agilityLvl < LEVEL) {
            // UNVERIFIED: wording guessed; the wiki has no transcript for it.
            chatNpc(
                werewolf,
                "You're not agile enough for skullball, human. Come back when you are.",
            )
            return false
        }
        if (!QuestRequirements.hasCompleted(player, QUEST)) {
            // UNVERIFIED: wording guessed.
            player.mes("You need to complete Creature of Fenkenstrain to play skullball.")
            return false
        }
        return true
    }

    private suspend fun Dialogue.trainer() {
        chatPlayer(quiz, "What is this place?")
        chatNpc(werewolf, "This is the Skullball Course.")
        chatNpc(
            werewolf,
            "Go and talk to the boss at the beginning of the course if you'd like a go.",
        )
    }

    private fun newBall(player: Player) {
        val ball = Npc(BALL, random.pick(course.spawns))
        npcRepo.add(ball, Int.MAX_VALUE)
        ball.assignSpawnOwner(player, mapClock.cycle)
        ball.aiTimer(1)
        player.resetGame()
        MiscOutput.hintArrowNpc(player, ball.slotId)
    }

    private fun remove(ball: Npc, owner: Player?) {
        npcRepo.del(ball, Int.MAX_VALUE)
        if (owner != null) {
            owner.resetGame()
            MiscOutput.hintArrowReset(owner)
        }
    }

    private fun ballOf(player: Player): Npc? =
        npcs.ownedBy(player).firstOrNull { it.id == BALL.asRSCM(RSCMType.NPC) }

    private fun ProtectedAccess.strike(ball: Npc, tiles: Int) {
        if (!ball.isSpawnOwnedBy(player)) {
            // UNVERIFIED: wording guessed.
            mes("That isn't your skullball.")
            return
        }
        val dx = (ball.coords.x - coords.x).sign
        val dz = (ball.coords.z - coords.z).sign
        if (dx == 0 && dz == 0) {
            return
        }
        // UNVERIFIED: the kick's animation and sound for each of the three strokes.
        anim(KICK)
        soundSynth(TAP_SYNTH)
        player.ballHeading = heading(dx, dz)
        player.ballSteps = tiles
        MiscOutput.hintArrowNpc(player, ball.slotId)
    }

    // UNVERIFIED: the wiki names Show-Goal without saying what it shows.
    private fun ProtectedAccess.showGoal(ball: Npc) {
        if (!ball.isSpawnOwnedBy(player)) {
            mes("That isn't your skullball.")
            return
        }
        val next = pitch.goals.getOrNull(player.goalsScored) ?: pitch.hole
        MiscOutput.hintArrowTile(player, next.centre, HINT_ARROW_HEIGHT)
    }

    private fun roll(ball: Npc) {
        val owner = ball.spawnOwner.resolve(players)
        if (owner == null || !owner.coords.inWerewolfArea()) {
            remove(ball, owner)
            return
        }
        val steps = owner.ballSteps
        if (steps == 0) {
            return
        }
        val heading = owner.ballHeading
        val dx = heading / HEADINGS - 1
        val dz = heading % HEADINGS - 1
        when (val roll = pitch.roll(ball.coords, dx, dz, steps, owner.goalsScored)) {
            BallRoll.Holed -> finish(ball, owner)
            is BallRoll.Stopped -> {
                owner.ballSteps = 0
                if (roll.bounced) {
                    worldRepo.soundArea(ball.coords, COLLIDE_SYNTH)
                }
            }
            is BallRoll.Rolled -> {
                if (roll.goal >= 0) {
                    ball.teleport(collision, roll.to)
                } else {
                    ball.walk(roll.to)
                }
                owner.ballHeading = heading(roll.dx, roll.dz)
                owner.ballSteps = roll.steps
                if (roll.bounced) {
                    worldRepo.soundArea(ball.coords, COLLIDE_SYNTH)
                }
                if (roll.forward && roll.goal == owner.goalsScored) {
                    scored(owner, roll.goal)
                }
            }
        }
    }

    private fun scored(owner: Player, goal: Int) {
        owner.goalsScored = goal + 1
        if (goal == 0) {
            owner.startClock = mapClock.cycle
        }
        val centre = pitch.goals[goal].centre
        locRepo.findExact(centre, LocShape.CentrepieceStraight)?.let {
            worldRepo.locAnim(it, GOAL_ANIM)
        }
        celebrate(owner, centre, goalShout(owner))
        // TODO(content): the elite clue skill challenge that asks for a skullball goal.
    }

    private fun finish(ball: Npc, owner: Player) {
        val penalty = course.penalty
        val xp =
            SkullballPitch.xp(
                elapsed = mapClock.cycle - owner.startClock,
                xp = course.xp,
                parTicks = course.parTicks,
                penaltyTicks = penalty[0],
                penaltyXp = penalty[1],
            )
        celebrate(owner, pitch.hole.centre, finalShout(owner))
        remove(ball, owner)
        owner.statAdvance(AGILITY, xp.toDouble())
        // TODO(content): the scoreboard shown for the final goal.
    }

    private fun celebrate(owner: Player, near: CoordGrid, shout: String) {
        owner.soundSynth(GOAL_SYNTH)
        owner.midiJingle(GOAL_JINGLE)
        search.find(near, TRAINER, TRAINER_RANGE, HuntVis.Off)?.say(shout)
    }

    private fun goalShout(owner: Player): String {
        if (!owner.wearsRing()) {
            return "Humans play skullball worse than puppies!"
        }
        val them =
            when (owner.subjectPronoun()) {
                "He" -> "him"
                "She" -> "her"
                else -> "them"
            }
        return random.pick(
            listOf(
                "You have truly gifted paws!",
                "I've never seen anything like it!",
                "You need a few more skullball lessons.",
                "Keep it up!",
                "Don't give up the day job!",
                "Look at $them go!",
                "Pathetic!",
                "What - a - goal !!!",
                "That was just plain lucky.",
            )
        )
    }

    private fun finalShout(owner: Player): String {
        if (!owner.wearsRing()) {
            return "The human's scored!"
        }
        val they = owner.subjectPronoun()
        return if (they == "They") {
            "They shoot - They score!!!!!"
        } else {
            "$they shoots - $they scores!!!!!"
        }
    }

    private fun Player.wearsRing(): Boolean = RINGS.any { worn.count(it) > 0 }

    private fun Player.resetGame() {
        goalsScored = 0
        ballHeading = 0
        ballSteps = 0
        startClock = 0
    }

    private fun heading(dx: Int, dz: Int): Int = (dx + 1) * HEADINGS + (dz + 1)

    private companion object {
        const val COURSE = "dbrow.agility_skullball"
        const val BOSS = "npc.werewolf_skullballboss"
        const val TRAINER = "npc.werewolf_trainer_1"
        const val BALL = "npc.waa_skullball"
        const val QUEST = "quest_creatureoffenkenstrain"
        const val WEREWOLF_MESANIM = "mesanim.skullball_werewolf"
        const val AGILITY = "stat.agility"
        const val LEVEL = 25

        const val TAP_TILES = 1
        const val KICK_TILES = 4
        const val SHOOT_TILES = 9
        const val HEADINGS = 3
        const val TRAINER_RANGE = 16
        const val HINT_ARROW_HEIGHT = 60

        const val KICK = "seq.kick_skull_ball"
        const val TAP_SYNTH = "synth.skullball_tap"
        const val COLLIDE_SYNTH = "synth.skullball_collide"
        const val GOAL_SYNTH = "synth.skeletongoal"
        const val GOAL_JINGLE = "jingle.skullball_win"
        const val GOAL_ANIM = "seq.waa_goal_forward"

        const val INSTRUCTIONS = "What are the instructions for using the skullball course?"
        const val LOST_BALL = "I seem to have lost my ball - can I have another one?"
        const val GIVE_UP = "I give up, I can't do it - take my ball away."

        val RINGS = listOf("obj.ring_of_charos", "obj.ring_of_charos_unlocked")

        var Player.goalsScored by intVarBit("varbit.agility_skullball_goals")
        var Player.ballHeading by intVarBit("varbit.agility_skullball_heading")
        var Player.ballSteps by intVarBit("varbit.agility_skullball_steps")
        var Player.startClock by intVarp("varp.agility_skullball_start")
    }
}
