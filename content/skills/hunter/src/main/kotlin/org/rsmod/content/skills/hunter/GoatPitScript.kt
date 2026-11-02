package org.rsmod.content.skills.hunter

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import kotlin.math.sign
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.enums.HunterEnums.hunter_goat_pit_capacity
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.worn.HeldEquipResult
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.route.RouteFactory
import org.rsmod.api.route.walkTo
import org.rsmod.api.script.onApNpcT
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.api.table.hunter.HunterGoatLuresRow
import org.rsmod.content.other.pets.PetRewards
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.skills.hunter.HunterPouch.Companion.pouchHunted
import org.rsmod.game.entity.Npc
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

class GoatPitScript
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val objRepo: ObjRepository,
    private val routes: RouteFactory,
    private val collision: CollisionFlagMap,
    private val rumours: HunterRumours,
    private val pets: PetRewards,
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val worldRepo: WorldRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(PIT) { pitOp(it.loc.coords) }
        onOpLoc2(PIT) { inspect() }
        onOpNpc1(GOAT) { prod(it.npc) }
        onOpLoc1(SPIKES_SUPPLY) { takeSpikes(1) }
        onOpLoc2(SPIKES_SUPPLY) { takeSpikes(5) }
        onOpLoc3(SPIKES_SUPPLY) { takeSpikes(10) }
        onOpLoc4(SPIKES_SUPPLY) { takeSpikes(countDialog("Enter amount:")) }
        onOpLoc1(PROD_SUPPLY) { takeProd() }
        onOpHeld2(PROD) { invEquip(it.slot).let { result -> if (result is HeldEquipResult.Fail) result.messages.forEach(::mes) } }
        onOpNpc1(GEOFF) { startDialogue(it.npc) { geoff() } }
        val goat = checkNotNull(ServerCacheManager.getNpc(GOAT.asRSCM(RSCMType.NPC)))
        for (lure in HunterGoatLuresRow.all()) {
            val spell = spells.allSpells().first { it.component.packed == lure.component.packed }
            onApNpcT(goat, spell.component) { lure(it.npc, spell, lure) }
        }
    }

    private suspend fun Dialogue.geoff() {
        when {
            vars[INTRODUCTION] == INTRO_NONE && vars[TRAPPED] == 0 -> geoffFirst()
            vars[INTRODUCTION] == INTRO_NONE -> {
                chatNpc(angry, "Have ye been pittin me goats without permission?!")
                chatPlayer(neutral, "Sorry, I didn't realise that I-")
                chatNpc(laugh, "Ha! I'm just pullin' yer leg, outsider. Ye've got nothin' t'worry 'bout!")
                chatNpc(
                    neutral,
                    "Glad t'see ye've figured out how t'use the pit, at least. If ye need help with anythin' " +
                        "else, we can talk later, else I'll leave ye to it.",
                )
                VarPlayerIntMapSetter.set(player, INTRODUCTION, INTRO_DONE)
            }
            else -> {
                chatNpc(neutral, "Outsider.")
                chatPlayer(quiz, "What am I meant to be doing here?")
                chatNpc(neutral, "Ye wanna grab a set of spikes from this pile o'er 'ere and line the pit wit' 'em.")
                chatNpc(
                    neutral,
                    "Then take a prod and shove the goats in t'pit. Me pal reckons you could use a 'telly knee " +
                        "rectric grab' or a 'dark le hure' to do it, but I don't go fer that magic nonsense.",
                )
                chatNpc(
                    neutral,
                    "Ye can probably fit more goats in t'pit if ye get better at herdin' animals, but eventually " +
                        "it'll get full regardless.",
                )
                chatNpc(
                    neutral,
                    "Once it is, ye'll need to sort the carcasses. Ye can keep any furs or horns ye salvage as ye " +
                        "clear 'em out. Then ye can just re-line the pit and keep at it.",
                )
                chatPlayer(happy, "Thanks!")
            }
        }
    }

    private suspend fun Dialogue.geoffFirst() {
        chatNpc(sad, "Ah, what'm'ah gonna do with these damn goats...")
        chatPlayer(quiz, "Is there anything I can do to help?")
        chatNpc(
            quiz,
            "Hrm... ye are an outsider... but ye do have a look about ye. Tell me, d'y'ave experience herdin' goats?",
        )
        if (!QuestRequirements.hasCompleted(player, SHEEP_HERDER)) {
            chatPlayer(confused, "Uh... no?")
            chatNpc(
                sad,
                "Ah, a damn shame. If ye get some experience under yer belt, come back, but these goats aren't for " +
                    "the weak handed.",
            )
            mesbox("You need to have completed the Sheep Herder quest to hunt goats.")
            return
        }
        chatPlayer(neutral, "I have experience herding sheep, if that counts for anything?")
        chatNpc(neutral, "Aye? Well, I s'pose I should take what I can get. Feel like helpin' out with the goat problem?")
        chatPlayer(confused, "How can goats be a problem?")
        chatNpc(angry, "When there's too bleedin' many of 'em!")
        chatNpc(
            neutral,
            "As yer can see, on this hill, there's a... large goat herd. We think the wyrms used to eat 'em and " +
                "keep the numbers down, but since we moved in, we think they've just kept spawnin' and... well, " +
                "you can see what happens.",
        )
        chatNpc(
            neutral,
            "Best plan o' action we came up with is the goat pit. It ain't pretty, but the goats are dumb enough " +
                "for it t'work.",
        )
        chatPlayer(quiz, "So how does goat herding come into this?")
        chatNpc(neutral, "You herd the goats into the pit and they're dumb enough t' jump right in.")
        chatNpc(
            neutral,
            "There's a pile o' spikes right 'ere. You'll wanna grab yerself a set of them and line the pit with " +
                "'em first.",
        )
        chatNpc(
            neutral,
            "The other pile 'ere is a bunch o' cattleprods. You can use one of them, or if ye fancy yerself one " +
                "of them mages, me pal Jeff reckons ye might be able to pull 'em around wit' a spell or two.",
        )
        chatNpc(
            confused,
            "Telly knee rectric grab? Dark le hure? I don't remember. If those names mean anything to ye, don't " +
                "tell me. I'm better off not knowing.",
        )
        chatPlayer(quiz, "So I just prod the goats into the pit?")
        chatNpc(
            neutral,
            "Aye, but the pit can only hold so many goats at a time. I reckon ye could fit more in if ye get " +
                "better at animal handlin', but ye'd need to inspect the pit to tell, I reckon.",
        )
        chatNpc(
            neutral,
            "Once ye've got goats in t'pit, ye'll need t'sort out the carcasses. Feel free t'keep any horns or fur " +
                "ye can salvage. Just remember t'line the pit with spikes again once yer done.",
        )
        chatPlayer(
            happy,
            "So, line the pit with spikes, herd the goats into the pit, sort the carcasses and repeat. Got it, " +
                "thank you!",
        )
        chatNpc(angry, "Aye, ye could reduce me speech into a single line, ye bloody outsider...")
        VarPlayerIntMapSetter.set(player, INTRODUCTION, INTRO_DONE)
    }

    private fun ProtectedAccess.questDone(): Boolean {
        if (QuestRequirements.hasCompleted(player, SHEEP_HERDER)) return true
        mes("You must complete the Sheep Herder quest to use the Wyrmscraig Goat Pit.")
        return false
    }

    private fun ProtectedAccess.takeSpikes(count: Int) {
        if (count <= 0 || !questDone()) return
        invAdd(inv, SPIKES, count)
    }

    private fun ProtectedAccess.takeProd() {
        if (!questDone()) return
        when {
            PROD in inv || PROD in player.worn -> mes("You don't feel like you need another cattleprod.")
            inv.freeSpace() == 0 -> mes("You don't have room to take a cattleprod.")
            else -> {
                invAdd(inv, PROD)
                mes("You take a cattleprod.")
            }
        }
    }

    private suspend fun ProtectedAccess.pitOp(coords: CoordGrid) {
        when (vars[STATE]) {
            EMPTY -> line()
            SPIKED -> inspect()
            else -> clear()
        }
    }

    private suspend fun ProtectedAccess.line() {
        if (!questDone()) return
        if (stat(HUNTER) < LEVEL) {
            mes("You need a Hunter level of $LEVEL to use the goat pit.")
            return
        }
        if (SPIKES !in inv) {
            mes("You need some wooden spikes to line the pit.")
            return
        }
        anim(LAY_SEQ)
        delay(1)
        if (vars[STATE] != EMPTY || invDel(inv, SPIKES).failure) return
        VarPlayerIntMapSetter.set(player, SPIKES_VARBIT, 1)
        VarPlayerIntMapSetter.set(player, STATE, SPIKED)
        mes("You line the pit with wooden spikes.")
    }

    private fun ProtectedAccess.inspect() {
        if (vars[STATE] == EMPTY) {
            mes("The pit has not been lined with spikes.")
            return
        }
        val spikes = if (vars[SPIKES_VARBIT] != 0) "The spikes are still sharp." else "The spikes are broken."
        mes("The pit contains ${vars[IN_PIT]} goat remains, out of ${capacity(stat(HUNTER))}. $spikes")
    }

    private suspend fun ProtectedAccess.clear() {
        VarPlayerIntMapSetter.set(player, SPIKES_VARBIT, 0)
        while (vars[IN_PIT] > 0) {
            if (inv.freeSpace() == 0) {
                mes("You don't have enough inventory space.")
                return
            }
            anim(CLEAR_SEQ)
            delay(1)
            if (vars[IN_PIT] <= 0) return
            val loot = if (random.of(4) == 0) FUR else HORN
            if (!pouchHunted(loot) && invAdd(inv, loot).failure) {
                mes("You don't have enough inventory space.")
                return
            }
            statAdvance(HUNTER, harvestXp(statBase(HUNTER)).toDouble())
            rumours.onCatch(player, GOAT)
            pets.rollSkillingPet(player, PET, HUNTER, PET_BASE)
            setInPit(vars[IN_PIT] - 1)
        }
        VarPlayerIntMapSetter.set(player, STATE, EMPTY)
    }

    private suspend fun ProtectedAccess.prod(goat: Npc) {
        val weapon = player.righthand
        if (weapon == null) {
            mes("You should use a cattleprod if you want to prod the goats.")
            return
        }
        if (PROD !in player.worn) {
            mes("You don't think your current weapon would work as a cattleprod.")
            return
        }
        if (!questDone()) return
        if (stat(HUNTER) < LEVEL) {
            mes("You need a Hunter level of $LEVEL to herd the goats.")
            return
        }
        faceEntitySquare(goat)
        anim(PROD_SEQ)
        val dx = (goat.coords.x - player.coords.x).sign
        val dz = (goat.coords.z - player.coords.z).sign
        drive(goat, dx, dz, PUSH_TILES, JUMP_SEQ, PROD_XP)
    }

    private suspend fun ProtectedAccess.lure(goat: Npc, spell: MagicSpell, lure: HunterGoatLuresRow) {
        if (!questDone()) return
        if (stat(HUNTER) < LEVEL) {
            mes("You need a Hunter level of $LEVEL to herd the goats.")
            return
        }
        if (lure.component.packed == DARK_LURE.asRSCM(RSCMType.COMPONENT) && vars[DARK_LURE_COOLDOWN] != 0) {
            mes("You can only cast Dark Lure every 10 seconds.")
            return
        }
        if (runes.attemptCast(player, spell).isFailure()) return
        faceEntitySquare(goat)
        anim(lure.castSeq)
        spotanim(lure.castSpotanim, height = CAST_SPOTANIM_HEIGHT)
        lure.synth?.let { soundSynth(it) }
        val proj = worldRepo.projAnim(player, goat, SpotanimType(lure.travel.asRSCM(RSCMType.SPOTANIM)), PROJANIM)
        statAdvance(MAGIC, spell.castXp)
        if (lure.component.packed == DARK_LURE.asRSCM(RSCMType.COMPONENT)) {
            VarPlayerIntMapSetter.set(player, DARK_LURE_COOLDOWN, 1)
            clearQueue(DARK_LURE_COOLDOWN_QUEUE)
            queue(DARK_LURE_COOLDOWN_QUEUE, DARK_LURE_COOLDOWN_TICKS)
        }
        delay(maxOf(1, proj.durations.serverDelay))
        if (!goat.isSlotAssigned) return
        goat.spotanim(lure.impact, height = lure.impactHeight)
        val dx = (player.coords.x - goat.coords.x).sign
        val dz = (player.coords.z - goat.coords.z).sign
        val tiles = distance(goat.coords, player.coords) - 1
        drive(goat, dx, dz, tiles, LURE_DEATH_SEQ, LURE_XP)
    }

    private suspend fun ProtectedAccess.drive(goat: Npc, dx: Int, dz: Int, tiles: Int, jumpSeq: String, xp: Double) {
        if (dx == 0 && dz == 0) return
        var target = goat.coords
        var intoPit = false
        for (step in 1..tiles) {
            val next = goat.coords.translate(dx * step, dz * step)
            if (inPit(next)) {
                intoPit = true
                break
            }
            if (collision.isWalkBlocked(next)) break
            target = next
        }
        if (target != goat.coords) goat.walkTo(routes, target)
        if (!intoPit) return
        delay(maxOf(1, distance(goat.coords, target)))
        if (!goat.isSlotAssigned || !canTakeGoat()) return
        goat.anim(jumpSeq)
        npcRepo.despawn(goat, GOAT_RESPAWN_TICKS)
        setInPit(vars[IN_PIT] + 1)
        VarPlayerIntMapSetter.set(player, TRAPPED, 1)
        statAdvance(HUNTER, xp)
        if (vars[STATE] == FULL) mes("The pit is now filled with goats.", ChatType.Spam)
    }

    private fun ProtectedAccess.canTakeGoat(): Boolean =
        vars[SPIKES_VARBIT] != 0 && vars[STATE] in SPIKED until FULL && vars[IN_PIT] < capacity(stat(HUNTER))

    private fun ProtectedAccess.setInPit(count: Int) {
        VarPlayerIntMapSetter.set(player, IN_PIT, count)
        val cap = capacity(stat(HUNTER))
        val state =
            when {
                count <= 0 -> if (vars[SPIKES_VARBIT] != 0) SPIKED else EMPTY
                count >= cap -> FULL
                count == 1 -> ONE
                else -> SOME
            }
        VarPlayerIntMapSetter.set(player, STATE, state)
    }

    private fun inPit(coords: CoordGrid): Boolean =
        coords.level == PIT_SW.level && coords.x in PIT_SW.x until PIT_SW.x + PIT_SIZE &&
            coords.z in PIT_SW.z until PIT_SW.z + PIT_SIZE

    private fun distance(a: CoordGrid, b: CoordGrid): Int = maxOf(kotlin.math.abs(a.x - b.x), kotlin.math.abs(a.z - b.z))

    companion object {
        const val HUNTER = "stat.hunter"
        const val LEVEL = 60
        const val SHEEP_HERDER = "quest_sheepherder"
        const val PIT = "loc.goat_pit"
        const val SPIKES_SUPPLY = "loc.goat_pit_spikes"
        const val PROD_SUPPLY = "loc.goat_pit_prod"
        const val GOAT = "npc.goat_pit_goat"
        const val GEOFF = "npc.goat_pit_helper"
        const val INTRODUCTION = "varbit.goat_pit_introduction"
        const val INTRO_NONE = 0
        const val INTRO_DONE = 1
        const val TRAPPED = "varp.goat_pit_trapped"
        const val SPIKES = "obj.goat_pit_spikes"
        const val PROD = "obj.cattleprod"
        const val HORN = "obj.desert_goat_horn"
        const val FUR = "obj.goat_pit_fur"
        const val PET = "obj.goatpitpet"
        const val PET_BASE = 40000
        const val STATE = "varbit.goat_pit_state"
        const val IN_PIT = "varbit.goat_pit_inpit"
        const val SPIKES_VARBIT = "varbit.goat_pit_spikes"
        const val LAY_SEQ = "seq.goat_hunting_lay_spikes"
        const val PROD_SEQ = "seq.cattleprod"
        const val JUMP_SEQ = "seq.goat_hunting_jump_death"
        const val CLEAR_SEQ = "seq.human_pickupfloor"
        const val EMPTY = 0
        const val SPIKED = 1
        const val ONE = 2
        const val SOME = 3
        const val FULL = 4
        const val PROD_XP = 20.0
        const val PUSH_TILES = 5
        const val GOAT_RESPAWN_TICKS = 10
        const val PIT_SIZE = 3
        val PIT_SW = CoordGrid(2571, 2194, 0)
        const val MAGIC = "stat.magic"
        const val LURE_XP = 10.0
        const val LURE_DEATH_SEQ = "seq.goat_hunting_telegrab_end"
        const val PROJANIM = "projanim.magic_spell"
        const val CAST_SPOTANIM_HEIGHT = 92
        const val DARK_LURE = "component.magic_spellbook:dark_lure"
        const val DARK_LURE_COOLDOWN = "varbit.arceuus_dark_lure_cooldown"
        const val DARK_LURE_COOLDOWN_QUEUE = "queue.dark_lure_cooldown"
        const val DARK_LURE_COOLDOWN_TICKS = 17

        fun capacity(level: Int): Int = HunterTrapScript.byLevel(hunter_goat_pit_capacity, level)

        fun harvestXp(level: Int): Int {
            val l = level.coerceIn(LEVEL, 99)
            return if (l <= 80) 100 + (l - 60) * 3 else 160 + (l - 80)
        }
    }
}
