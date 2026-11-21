package org.rsmod.content.skills.agility.arena

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.dialogue.mesanims
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.MiscOutput
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.player.stat.baseAgilityLvl
import org.rsmod.api.registry.player.PlayerRegistry
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onPlayerCoordsChanged
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.table.agility.AgilityCoursesRow
import org.rsmod.content.skills.agility.AgilityPets
import org.rsmod.content.skills.agility.LapCounter
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

/**
 * The Brimhaven Agility Arena (https://oldschool.runescape.wiki/w/Brimhaven_Agility_Arena): Cap'n
 * Izzy takes the 200 coin fee, the ladder lets a paying player down, and tagging the dispenser
 * under the flashing arrow pays a ticket, a voucher and Agility xp once per move of the arrow.
 * Leaving keeps the player out for a minute, and tagging too soon after a world hop locks the
 * dispensers for one. The climbing ropes below the pillars lead back up after a fall.
 */
class BrimhavenArenaScript
@Inject
constructor(
    private val dispensers: ArenaDispensers,
    private val planks: ArenaPlanks,
    private val players: PlayerRegistry,
    private val worldClock: MapClock,
    private val collision: CollisionFlagMap,
    private val objRepo: ObjRepository,
    private val pets: AgilityPets,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<GameLifecycle.LateCycle> {
            if (dispensers.tick(worldClock.cycle)) {
                moveArrow()
                planks.reroll()
            }
        }
        onPlayerLogin { returnToHut(player) }
        onPlayerCoordsChanged {
            if (lastKnownCoords.inArena() && !player.coords.inArena()) {
                leave(player)
            }
        }
        onOpNpc1(IZZY) { startDialogue(it.npc) { izzy() } }
        onOpNpc3(IZZY) { payFee(it.npc) }
        onOpLoc1("loc.agilityarena_ladderdown") { climbDown() }
        onOpLoc1("loc.agilityarena_ladderup") { climbUp() }
        onOpLoc1("loc.agility_ticketpillar") { tag(it.loc.coords) }
        onOpLoc1("loc.agilityarena_poisondarts") { tag(it.loc.coords) }
        onOpLoc1("loc.agilityarena_climbingrope") { climbRope(it.loc.coords) }
    }

    private fun moveArrow() {
        for (player in players.findAll(ARENA_ZONES)) {
            if (!player.coords.inArena()) {
                continue
            }
            player.currentHintArrow = 0
            player.currentHintArrow = HINT_ARROW
            player.ticketAvailable = true
            MiscOutput.hintArrowReset(player)
            MiscOutput.hintArrowTile(player, dispensers.current, HINT_ARROW_HEIGHT)
        }
    }

    private fun returnToHut(player: Player) {
        if (!player.coords.inArena()) {
            return
        }
        PathingEntityCommon.telejump(player, collision, ENTRANCE_HUT)
        leave(player)
    }

    private suspend fun Dialogue.izzy() {
        val inv = access.inv
        if (CHALLENGE in inv) {
            answerChallenge()
            return
        }
        if (CLUE in inv) {
            treasureTrail()
            return
        }
        chatPlayer(happy, "Ahoy Cap'n!")
        chatNpc(happy, "Ahoy there!")
        // TODO(content): live plays seq.lore_bird_chathead_quizical, which needs its own mesanim.
        chatNpcSpecificNp("Parrot", PARROT, quiz, "Avast ye scurvy swabs!")
        chatPlayer(confused, "Huh?")
        chatNpc(happy, "Don't mind me parrot, he's Cracked Jenny's Tea Cup!")
        val topic =
            choice5(
                "What is this place?",
                IzzyTopic.Place,
                "What do I do in the arena?",
                IzzyTopic.Arena,
                "I'd like to use the Agility Arena, please.",
                IzzyTopic.Enter,
                "Is it true you sell Skillcapes of Agility?",
                IzzyTopic.Skillcape,
                "See you later.",
                IzzyTopic.Leave,
            )
        when (topic) {
            // TODO(content): Izzy's answers; no capture asked either question.
            IzzyTopic.Place -> chatPlayer(quiz, "What is this place?")
            IzzyTopic.Arena -> chatPlayer(quiz, "What do I do in the arena?")
            IzzyTopic.Enter -> useArena()
            IzzyTopic.Skillcape -> skillcape()
            // UNVERIFIED: the mesanim.
            IzzyTopic.Leave -> chatPlayer(neutral, "See you later.")
        }
    }

    private suspend fun Dialogue.useArena() {
        // UNVERIFIED: the wiki gives these lines only as a paraphrase, bar the quoted ones.
        chatPlayer(quiz, "I'd like to use the Agility Arena, please.")
        chatNpc(happy, "Aye, entrance be 200 coins.")
        if (player.canEnterArena) {
            chatNpc(happy, "Ye've already paid, matey. Off ye go!")
            return
        }
        if (access.invTotal(access.inv, COINS) < FEE) {
            chatPlayer(sad, "I don't have the money on me at the moment.")
            chatNpc(angry, "No coins, no entrance!")
            return
        }
        val pay = choice2("Ok, here's 200 coins.", true, "Never mind.", false)
        if (!pay) {
            chatPlayer(neutral, "Never mind.")
            return
        }
        chatPlayer(happy, "Ok, here's 200 coins.")
        if (!access.takeFee()) {
            return
        }
        chatNpc(happy, "May the wind be in ye sails!")
    }

    private suspend fun Dialogue.skillcape() {
        if (player.baseAgilityLvl < MAX_LEVEL) {
            chatPlayer(quiz, "Is it true you sell Skillcapes of Agility?")
            // TODO(content): Izzy's answer below 99 Agility; no capture asked.
            return
        }
        chatPlayer(quiz, "May I buy a Skillcape of Agility, please?")
        chatNpc(
            neutral,
            "Most certainly, having it equipped reduces your weight and I think it will really " +
                "suit you. All that remains to be done is pay me 99000 gold!",
        )
        val buy =
            choice2("I'm afraid I can't afford that.", false, "Certainly, here you go.", true)
        if (!buy) {
            // UNVERIFIED: the mesanim; no capture turned it down.
            chatPlayer(sad, "I'm afraid I can't afford that.")
            return
        }
        chatPlayer(happy, "Certainly, here you go.")
        // UNVERIFIED: both refusals; they are the other skillcape sellers' lines.
        if (access.invTotal(access.inv, COINS) < CAPE_PRICE) {
            chatPlayer(sad, "But, unfortunately, I don't have enough money with me.")
            chatNpc(neutral, "Well, come back and see me when you do.")
            return
        }
        if (!access.buySkillcape()) {
            chatNpc(
                neutral,
                "Unfortunately all Skillcapes are only available with a free hood, it's part of " +
                    "a skill promotion deal; buy one get one free, you know. So you'll need to " +
                    "free up some inventory space before I can sell you one.",
            )
            return
        }
        access.midiJingle(SKILLCAPE_JINGLE)
        chatNpc(happy, "Excellent! That cape really does suit you.")
    }

    private fun ProtectedAccess.buySkillcape(): Boolean {
        val maxed =
            ServerCacheManager.getStats().keys.count { id ->
                statBase(RSCM.getReverseMapping(RSCMType.STAT, id)) >= MAX_LEVEL
            }
        val cape = if (maxed > 1) TRIMMED_CAPE else CAPE
        return player
            .invTransaction(inv) {
                val into = select(inv)
                delete(into, COINS.asRSCM(RSCMType.OBJ), CAPE_PRICE)
                add(into, cape.asRSCM(RSCMType.OBJ), 1)
                add(into, HOOD.asRSCM(RSCMType.OBJ), 1)
            }
            .success
    }

    private suspend fun Dialogue.treasureTrail() {
        chatNpc(quiz, "Can I help you with something?")
        chatPlayer(quiz, "Well, I'm here about a treasure trail...")
        chatNpc(neutral, "Ah, you'll be wanting to talk to him then.")
        chatPlayer(quiz, "Him?")
        chatNpcSpecific("Parrot", PARROT, quiz, "Avast!")
        chatNpcSpecific("Parrot", PARROT, quiz, "Answer this and I'll give you treasure!")
        // UNVERIFIED: a full inventory; no capture had one.
        access.invAddOrDrop(objRepo, CHALLENGE)
        objbox(CHALLENGE, 400, "The parrot has given you a challenge scroll!")
    }

    private suspend fun Dialogue.answerChallenge() {
        chatNpcSpecific("Parrot", PARROT, quiz, "Please enter the answer to the question.")
        if (access.countDialog() != CHALLENGE_ANSWER) {
            chatNpcSpecific("Parrot", PARROT, quiz, "Try again! Try again!")
            return
        }
        chatNpcSpecific("Parrot", PARROT, quiz, "Squawk! Pieces of eight!")
        // TODO(content): swap the clue and the challenge scroll for the trail's next hard clue
        // ("The parrot has given you another clue scroll!") once treasure trails roll one.
    }

    private suspend fun ProtectedAccess.payFee(npc: Npc) {
        if (player.canEnterArena) {
            // UNVERIFIED: no capture paid twice.
            chatNpc(npc, mesanims.happy, "Ye've already paid, matey. Off ye go!")
            return
        }
        if (invTotal(inv, COINS) < FEE) {
            // UNVERIFIED: the Pay option's refusal; the wiki quotes this line for the dialogue.
            chatNpc(npc, mesanims.angry, "No coins, no entrance!")
            return
        }
        if (takeFee()) {
            objbox(COINS, 400, FEE_MESSAGE)
        }
    }

    private fun ProtectedAccess.takeFee(): Boolean {
        if (invDel(inv, COINS, FEE).failure) {
            return false
        }
        player.canEnterArena = true
        spam(FEE_MESSAGE)
        return true
    }

    private suspend fun ProtectedAccess.climbDown() {
        if (player.teleportedOut) {
            if (worldClock.cycle - player.arenaLeftClock < REENTRY_TICKS) {
                mesbox(
                    "You've only just left the arena. You need a wait at least a minute after " +
                        "leaving before re-entering."
                )
                return
            }
            player.teleportedOut = false
        }
        if (!player.canEnterArena) {
            // UNVERIFIED: the mesanim; live plays the bird's lore_bird_chathead_quizical.
            startDialogue { chatNpcSpecific("Parrot", PARROT, quiz, "Clap 'em in irons!") }
            return
        }
        anim("seq.human_pickupfloor")
        delay(1)
        teleport(ARENA_START, TeleportType.Exempt)
        enter()
    }

    private fun ProtectedAccess.enter() {
        player.highlightedCourse = HELPER_COURSE
        player.currentHintArrow = HINT_ARROW
        player.ticketAvailable = player.taggedRotation != dispensers.rotation
        runClientScript(START_LAP.asRSCM(RSCMType.CLIENTSCRIPT), player.currentMapClock)
        MiscOutput.hintArrowReset(player)
        MiscOutput.hintArrowTile(player, dispensers.current, HINT_ARROW_HEIGHT)
    }

    private suspend fun ProtectedAccess.climbUp() {
        anim("seq.human_reachforladder")
        delay(1)
        teleport(ARENA_EXIT, TeleportType.Exempt)
    }

    // UNVERIFIED: the climb's seq, timing and landing; the wiki only says the rope leads back up.
    private suspend fun ProtectedAccess.climbRope(rope: CoordGrid) {
        anim("seq.human_reachforladder")
        delay(2)
        telejump(CoordGrid(rope.x, rope.z, ARENA_LEVEL), TeleportType.Exempt)
        resetAnim()
    }

    private fun leave(player: Player) {
        player.currentHintArrow = 0
        player.canEnterArena = false
        player.highlightedCourse = 0
        player.teleportedOut = true
        player.arenaLeftClock = worldClock.cycle
        MiscOutput.hintArrowReset(player)
    }

    private suspend fun ProtectedAccess.tag(dispenser: CoordGrid) {
        if (dispenser != dispensers.current) {
            mes("You can only get a ticket when the flashing arrow is above the pillar.")
            return
        }
        if (player.taggedRotation == dispensers.rotation) {
            mes("You can only get one ticket at a time, wait till the arrow moves again.")
            return
        }
        if (lockedAfterHop()) {
            // UNVERIFIED: wording guessed.
            mes("You can't get another ticket so soon after changing worlds.")
            return
        }
        player.taggedRotation = dispensers.rotation
        player.ticketAvailable = false
        player.arenaTags++
        LapCounter.announce(this, AgilityCoursesRow.getRow(ARENA_COURSE), player.arenaTags)
        if (dispensers.claimFirstTag()) {
            announceFirstTag(player)
        }
        // UNVERIFIED: the elite diary's double reward and its message; the wiki gives the chance.
        val count = if (player.vars[KARAMJA_ELITE] != 0 && random.of(10) == 0) 2 else 1
        invAddOrDrop(objRepo, TICKET, count)
        invAddOrDrop(objRepo, VOUCHER, count)
        val reward = "You have received $count Agility Arena Tickets and $count Brimhaven Vouchers!"
        spam(reward)
        midiSong("midi.silence")
        midiJingle("jingle.agility_complete")
        val clock = player.currentMapClock
        runClientScript(START_LAP.asRSCM(RSCMType.CLIENTSCRIPT), clock)
        runClientScript(END_LAP.asRSCM(RSCMType.CLIENTSCRIPT), clock, 0)
        statAdvance(AGILITY, tagXp(player.agilityLvl).toDouble())
        pets.roll(this, AgilityCoursesRow.getRow(ARENA_COURSE))
        objbox(TICKET_MODEL, 400, reward)
    }

    private fun ProtectedAccess.lockedAfterHop(): Boolean {
        val now = currentSecond()
        if (now < player.tagLockedUntil) {
            return true
        }
        if (!player.taggedThisSession && now - player.lastTagSecond < QUICK_TAG_SECONDS) {
            player.tagLockedUntil = now + LOCK_SECONDS
            return true
        }
        player.lastTagSecond = now
        player.taggedThisSession = true
        return false
    }

    private fun currentSecond(): Int = (System.currentTimeMillis() / SECOND_MILLIS).toInt()

    private fun announceFirstTag(tagger: Player) {
        val text = "<col=ef1020>${tagger.displayName}</col> was the first to tag the pillar!"
        for (player in players.findAll(ARENA_ZONES)) {
            if (player.coords.inArena()) {
                player.mes(text, ChatType.GameMessage)
            }
        }
    }

    private companion object {
        const val ARENA_COURSE = "dbrow.agility_course_brimhaven_arena"
        const val IZZY = "npc.agilityarena_clerk"
        const val PARROT = "npc.lunar_captains_parrot"
        const val CLUE = "obj.trail_clue_hard_anagram003"
        const val CHALLENGE = "obj.trail_clue_hard_anagram003_challenge"
        const val CHALLENGE_ANSWER = 33
        const val CAPE = "obj.skillcape_agility"
        const val TRIMMED_CAPE = "obj.skillcape_agility_trimmed"
        const val HOOD = "obj.skillcape_agility_hood"
        const val CAPE_PRICE = 99_000
        const val MAX_LEVEL = 99
        const val SKILLCAPE_JINGLE = "jingle.skillcape_jingle"
        const val COINS = "obj.coins"
        const val FEE = 200
        const val FEE_MESSAGE = "You give Cap'n Izzy the 200 coin entrance fee."
        const val TICKET = "obj.agilityarena_ticket_new"
        const val TICKET_MODEL = "obj.agilityarena_ticket"
        const val VOUCHER = "obj.agilityarena_voucher"
        const val KARAMJA_ELITE = "varbit.karamja_diary_elite_complete"
        const val AGILITY = "stat.agility"
        const val START_LAP = "clientscript.[clientscript,helper_agility_start_lap]"
        const val END_LAP = "clientscript.[clientscript,helper_agility_end_lap]"
        const val SECOND_MILLIS = 1000L

        // UNVERIFIED: the wiki says only "too quickly"; one arrow's minute is a guess.
        const val QUICK_TAG_SECONDS = 60
        const val LOCK_SECONDS = 60
    }
}

private enum class IzzyTopic {
    Place,
    Arena,
    Enter,
    Skillcape,
    Leave,
}

internal fun tagXp(level: Int): Int = level / 10 * 30
