package org.rsmod.content.skills.hunter

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.MiscOutput
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onApNpc1
import org.rsmod.api.script.onAreaExit
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpWorn1
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.table.hunter.HunterFalconryRow
import org.rsmod.api.utils.skills.SkillingSuccessRate
import org.rsmod.content.skills.hunter.HunterPouch.Companion.pouchHunted
import org.rsmod.content.skills.hunter.HunterTrapScript.Companion.distance
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.proj.ProjAnim
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

@Singleton
class FalconryScript
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val worldRepo: WorldRepository,
    private val areas: AreaChecker,
    private val random: GameRandom,
    private val clock: MapClock,
    private val rumours: HunterRumours,
) : PluginScript() {
    private val kebbits: List<Kebbit> by lazy { HunterFalconryRow.all().map(::Kebbit) }
    private val flights = HashMap<Player, Flight>()

    override fun ScriptContext.startup() {
        for (matthias in MATTHIAS) {
            onOpNpc1(matthias) { startDialogue(it.npc) { matthias() } }
            onOpNpc3(matthias) { quickFalcon() }
        }
        for (kebbit in kebbits) {
            onOpNpc1(kebbit.npc) { send(it.npc, kebbit) }
            onApNpc1(kebbit.npc) {
                if (isWithinDistance(it.npc, RANGE) && lineOfSight(player.coords, it.npc.coords)) {
                    send(it.npc, kebbit)
                } else {
                    apRange(RANGE)
                }
            }
            onOpNpc1(kebbit.falcon) { retrieve(it.npc) }
        }
        for (glove in GLOVES) onOpWorn1(glove) { mes("You should speak to Matthias to get this removed safely.") }
        onOpLoc1(STILE) { climbOver(it.loc) }
        onOpLoc3(STILE) { leave(it.loc) }
        onAreaExit(AREA) { if (wearsGlove(player)) flyBack() }
        onPlayerSoftTimer(TIMER) { player.flightTick() }
        onPlayerLogout { flights.remove(player)?.falcon?.let { npcRepo.del(it, Int.MAX_VALUE) } }
    }

    private fun wearsGlove(player: Player): Boolean = GLOVES.any { it in player.worn }

    private fun hasFalcon(player: Player): Boolean = FALCON_GLOVE in player.worn

    private suspend fun Dialogue.matthias() {
        val flight = flights[player]
        if (wearsGlove(player)) {
            if (flight?.state == FlightState.OnPrey) {
                chatPlayer(neutral, "Hello again.")
                chatNpc(
                    neutral,
                    "Hmm, well it looks like you've got the hang of setting falcons on creatures. However, I " +
                        "suggest you keep track of it; left to its own devices it'll be more than happy to fly off " +
                        "on its own to find food.",
                )
                return
            }
            if (!hasFalcon(player) && flight == null) return falconLost()
            chatPlayer(neutral, "Hello again.")
            chatNpc(neutral, "Ah, you're back. How are you getting along with him then?")
            chatPlayer(neutral, "It's certainly harder than it looks.")
            chatNpc(
                neutral,
                "Sorry, but I was talking to the falcon, not you. But yes it is; have you had enough yet?",
            )
            val leave =
                choice2("Actually, I'd like to keep trying a little longer.", false, "I think I'll leave it for now.", true)
            if (!leave) {
                chatPlayer(neutral, "Actually, I'd like to keep trying a little longer.")
                chatNpc(neutral, "Ok then, just come talk to me when you're done.")
                return
            }
            chatPlayer(neutral, "I think I'll leave it for now.")
            access.returnGlove()
            mesbox("You give the falcon and glove back to Matthias.")
            return
        }
        chatPlayer(neutral, "Hello there.")
        chatNpc(neutral, "Greetings. Can I help you at all?")
        mainOptions()
    }

    private suspend fun Dialogue.mainOptions() {
        when (
            choice5(
                "Do you have any quests I could do?", 1,
                "What is this place?", 2,
                "What's this falconry thing all about then?", 3,
                "Could I have a go with your bird?", 4,
                "Could I invest in your setup here?", 5,
            )
        ) {
            1 -> quests()
            2 -> {
                chatPlayer(quiz, "What is this place?")
                chatNpc(neutral, "A good question; straight and to the point.")
                chatNpc(neutral, "My name is Matthias, I am a falconer, and this is where I train my birds.")
                when (
                    choice3(
                        "Do you have any quests I could do?", 1,
                        "That sounds like fun; could I have a go?", 2,
                        "That doesn't sound like my sort of thing.", 3,
                    )
                ) {
                    1 -> quests()
                    2 -> {
                        chatPlayer(happy, "That sounds like fun; could I have a go?")
                        haveAGo()
                    }
                    else -> {
                        chatPlayer(neutral, "That doesn't sound like my sort of thing.")
                        chatNpc(
                            neutral,
                            "Fair enough; it does require a great deal of patience and skill, so I can understand " +
                                "if you might feel intimidated.",
                        )
                    }
                }
            }
            3 -> {
                chatPlayer(quiz, "What's this falconry thing all about then?")
                chatNpc(
                    neutral,
                    "Well, some people see it as a sport, although such a term does not really convey the amount " +
                        "of patience and dedication required to be proficient at the task.",
                )
                chatNpc(neutral, "Putting it simply, it is the training and use of birds of prey in hunting quarry.")
                chatPlayer(quiz, "So it's like keeping a pet then?")
                chatNpc(
                    neutral,
                    "Not exactly, no. Such a bird can never really be considered tame in the same way that a dog can.",
                )
                chatNpc(
                    neutral,
                    "They can be trained to associate people or places with food though, and, as such, a good " +
                        "falconer can get a trained bird to do as he wishes.",
                )
                mainOptions()
            }
            4 -> {
                chatPlayer(quiz, "Could I have a go with your bird?")
                haveAGo()
            }
            else -> invest()
        }
    }

    private suspend fun Dialogue.quests() {
        chatPlayer(quiz, "Do you have any quests I could do?")
        chatNpc(
            neutral,
            "A quest? What a strange notion. Do you normally go around asking complete strangers for quests?",
        )
        chatPlayer(neutral, "Er, yes, now you come to mention it.")
        chatNpc(neutral, "Oh, ok then. Well, no, I don't; sorry.")
    }

    private suspend fun Dialogue.haveAGo() {
        if (player.statBase(HUNTER) < MIN_LEVEL) {
            chatNpc(neutral, "I'm sorry, but training falcons is a lot of work and I doubt you're up to the task.")
            chatNpc(
                neutral,
                "I might occasionally lend some of my better trained birds to professionals to hunt with, that " +
                    "being what the birds are trained for after all, but I can't really lend them to anyone else.",
            )
            return
        }
        if (vars[INVESTED] == 1) {
            chatNpc(neutral, "Absolutely, are you ready?")
            chatPlayer(happy, "I am!")
            if (!access.takeFalcon(fee = 0)) return handsBusy()
            mesbox("The falconer gives you a large leather glove and brings one of the smaller birds over to land on it.")
            chatNpc(neutral, "Don't worry; I'll keep an eye on you to make sure you don't upset it too much.")
            return
        }
        chatNpc(
            neutral,
            "Training falcons is a lot of work and I doubt you're up to the task. However, I suppose I could let " +
                "you try hunting with one.",
        )
        chatNpc(
            neutral,
            "I have some tamer birds that I occasionally lend to rich noblemen who consider it a sufficiently " +
                "refined sport for their tastes, and you look like the kind who might appreciate a good hunt.",
        )
        chatNpc(neutral, "I'd have to request a small fee, mind you; how does 500 gold pieces sound?")
        if (player.inv.count(COINS) < FEE) {
            chatPlayer(sad, "I'm afraid I don't have that much with me.")
            return
        }
        if (!choice2("Ok, that seems reasonable.", true, "I'm not interested then, thanks.", false)) {
            chatPlayer(neutral, "I'm not interested then, thanks.")
            chatNpc(neutral, "Well, you're welcome to come back if you change your mind.")
            return
        }
        chatPlayer(neutral, "Ok, that seems reasonable.")
        if (!access.takeFalcon(fee = FEE)) return handsBusy()
        mesbox("The falconer gives you a large leather glove and brings one of the smaller birds over to land on it.")
        chatNpc(neutral, "Don't worry; I'll keep an eye on you to make sure you don't upset it too much.")
    }

    private suspend fun Dialogue.handsBusy() {
        val hands = player.worn[Wearpos.RightHand.slot] != null || player.worn[Wearpos.Hands.slot] != null
        if (hands) {
            chatNpc(
                neutral,
                "Good, however you really need both hands free. I'd suggest that you put away your weapons and " +
                    "gloves before we start.",
            )
        } else {
            chatNpc(
                neutral,
                "Good, however you really need both hands free. I'd suggest that you take off some of that kit " +
                    "you're wearing before we start.",
            )
        }
    }

    private suspend fun Dialogue.invest() {
        chatPlayer(quiz, "Could I invest in your setup here?")
        if (vars[INVESTED] == 1) {
            chatNpc(neutral, "I appreciate that you want to invest more, but I really don't need it right now!")
            return
        }
        chatNpc(neutral, "If you'd like to put money forward for training more falcons, be my guest!")
        chatPlayer(quiz, "Would I get any benefit from this?")
        chatNpc(
            neutral,
            "I suppose I could let you use my falcons for free if you stump up enough cash. How does 500,000 " +
                "coins sound?",
        )
        if (player.inv.count(COINS) < INVESTMENT) {
            chatPlayer(sad, "Ah, I don't have that much on me right now.")
            return
        }
        if (!choice2("Yes", true, "No", false, title = "Unlock permanent access for 500,000 coins?")) {
            chatPlayer(neutral, "Hmm, that seems a little steep. I'll think on it.")
            return
        }
        if (access.invDel(player.inv, COINS, INVESTMENT).failure) return
        VarPlayerIntMapSetter.set(player, INVESTED, 1)
    }

    private suspend fun Dialogue.falconLost() {
        chatPlayer(sad, "Hi, er, your falcon ran away.")
        chatNpc(
            neutral,
            "Falcons aren't domesticated you know. You can't expect them to just automatically return to you. " +
                "Fortunately for you, they can learn to associate areas with food. He'll probably be back on his post.",
        )
        chatNpc(neutral, "Do you want me to fetch him for you?")
        if (choice2("Yes, please.", true, "Actually, I think I'll leave it for now.", false)) {
            chatPlayer(happy, "Yes, please.")
            access.swapGlove(FALCON_GLOVE)
            mesbox("The falconer brings a bird over to land on your glove.")
            chatNpc(
                neutral,
                "Right, try to be a bit more careful this time; these birds take a long time to raise. I'd rather " +
                    "not lose one.",
            )
        } else {
            chatPlayer(neutral, "Actually, I think I'll leave it for now.")
            access.returnGlove()
            mesbox("You give the falcon glove back to Matthias.")
        }
    }

    private fun ProtectedAccess.quickFalcon() {
        if (wearsGlove(player)) return
        val fee = if (vars[INVESTED] == 1) 0 else FEE
        if (inv.count(COINS) < fee) {
            mes("You need 500 coins to hire a falcon.")
            return
        }
        if (!takeFalcon(fee)) {
            mes("You need to free your hands before you can take a falcon.")
            return
        }
        mes("The falconer gives you a large leather glove and brings one of the smaller birds over to land on it.")
    }

    private fun ProtectedAccess.takeFalcon(fee: Int): Boolean {
        val slots = HAND_SLOTS.filter { player.worn[it] != null }
        if (inv.freeSpace() < slots.size) return false
        for (slot in slots) wornUnequip(slot)
        if (HAND_SLOTS.any { player.worn[it] != null }) return false
        if (fee > 0 && invDel(inv, COINS, fee).failure) return false
        invAdd(player.worn, FALCON_GLOVE, slot = Wearpos.RightHand.slot)
        rebuildAppearance()
        return true
    }

    private fun ProtectedAccess.swapGlove(into: String) {
        val current = GLOVES.firstOrNull { it in player.worn } ?: return
        invReplace(player.worn, current, 1, into)
        rebuildAppearance()
    }

    private fun ProtectedAccess.returnGlove() {
        flights.remove(player)?.falcon?.let { despawnFalcon(player, it) }
        for (glove in GLOVES) if (glove in player.worn) invDel(player.worn, glove)
        rebuildAppearance()
    }

    private fun ProtectedAccess.flyBack() {
        returnGlove()
        mes("As you leave, Matthias' falcon flies back to him.")
    }

    private fun ProtectedAccess.send(npc: Npc, kebbit: Kebbit) {
        if (!hasFalcon(player) || flights.containsKey(player)) return
        if (!npc.isSlotAssigned || !npc.isVisible) return
        faceEntitySquare(npc)
        anim(SEND_SEQ)
        invReplace(player.worn, FALCON_GLOVE, 1, EMPTY_GLOVE)
        rebuildAppearance()
        val d = distance(player.coords, npc.coords).coerceAtLeast(1)
        val level = statBase(HUNTER)
        val hit =
            level >= kebbit.level &&
                random.of(256) < HunterOutfit.applyTo((SkillingSuccessRate.successRate(kebbit.low, kebbit.high, level.coerceIn(1, 99), 99) * 256).toInt(), player)
        val arrive = FLIGHT_START + FLIGHT_STEP * d
        if (hit) {
            worldRepo.projAnim(
                ProjAnim(FALCON_SPOTANIM.asRSCM(RSCMType.SPOTANIM), 150, 5, FLIGHT_START, arrive, 10, 0,
                    -(player.slotId + 1), npc.slotId + 1, player.coords, npc.coords)
            )
        } else {
            worldRepo.projAnim(
                ProjAnim(FALCON_SPOTANIM.asRSCM(RSCMType.SPOTANIM), 150, 50, FLIGHT_START, arrive, 0, 0,
                    0, 0, player.coords, npc.coords)
            )
            worldRepo.projAnim(
                ProjAnim(FALCON_SPOTANIM.asRSCM(RSCMType.SPOTANIM), 50, 100, arrive, arrive + RETURN_STEP * d, 0, 0,
                    0, -(player.slotId + 1), npc.coords, player.coords)
            )
        }
        val returnCycles = if (hit) arrive else arrive + RETURN_STEP * d
        flights[player] = Flight(kebbit, npc, hit, clock.cycle, clock.cycle + cyclesToTicks(returnCycles))
        player.softTimer(TIMER, 1)
    }

    private fun ProtectedAccess.retrieve(falcon: Npc) {
        val flight = flights[player]
        if (flight == null || flight.falcon !== falcon) {
            mes("This isn't your falcon.")
            return
        }
        if (inv.freeSpace() < flight.kebbit.loot.size) {
            mes("You don't have enough inventory space.")
            return
        }
        flights.remove(player)
        despawnFalcon(player, falcon)
        for (obj in flight.kebbit.loot) if (!pouchHunted(obj)) invAdd(inv, obj)
        statAdvance(HUNTER, flight.kebbit.xp.toDouble())
        swapGlove(FALCON_GLOVE)
        rumours.onCatch(player, flight.kebbit.npc)
    }

    private fun despawnFalcon(player: Player, falcon: Npc) {
        if (falcon.isSlotAssigned) npcRepo.del(falcon, Int.MAX_VALUE)
        MiscOutput.hintArrowReset(player)
    }

    private fun Player.flightTick() {
        val flight = flights[this]
        if (flight == null) {
            clearSoftTimer(TIMER)
            return
        }
        val now = clock.cycle
        when (flight.state) {
            FlightState.Flying -> {
                if (now < flight.landCycle) return
                val target = flight.target
                if (!flight.hit || !target.isSlotAssigned || !target.isVisible) {
                    flights.remove(this)
                    if (EMPTY_GLOVE in worn) {
                        invReplaceWorn(EMPTY_GLOVE, FALCON_GLOVE)
                    }
                    return
                }
                val coords = target.coords
                npcRepo.despawn(target, KEBBIT_RESPAWN_TICKS)
                val falcon = Npc(flight.kebbit.falcon, coords)
                npcRepo.add(falcon, LEAVE_PREY_TICKS + 1)
                MiscOutput.hintArrowNpc(this, falcon.slotId)
                flight.falcon = falcon
                flight.state = FlightState.OnPrey
                flight.landCycle = now
            }
            FlightState.OnPrey ->
                if (now - flight.landCycle >= LEAVE_PREY_TICKS) {
                    flights.remove(this)
                    flight.falcon?.let { despawnFalcon(this, it) }
                    mes("Your falcon has left its prey. You see it heading back toward the falconer.")
                }
        }
    }

    private fun Player.invReplaceWorn(from: String, into: String) {
        val slot = Wearpos.RightHand.slot
        if (worn[slot]?.id != from.asRSCM(RSCMType.OBJ)) return
        worn[slot] = org.rsmod.game.inv.InvObj(into)
        rebuildAppearance()
    }

    private suspend fun ProtectedAccess.climbOver(stile: BoundLocInfo) {
        if (wearsGlove(player)) {
            startDialogue { chatPlayer(neutral, "I should return the glove I borrowed from the falconer before leaving.") }
            return
        }
        cross(stile)
    }

    private suspend fun ProtectedAccess.leave(stile: BoundLocInfo) {
        if (!inArea(AREA, player.coords)) {
            startDialogue { chatPlayer(quiz, "Why am I trying to leave when I am not even inside?") }
            return
        }
        if (wearsGlove(player)) returnGlove()
        cross(stile)
    }

    private suspend fun ProtectedAccess.cross(stile: BoundLocInfo) {
        val south = stile.coords
        val north = south.translate(stile.adjustedWidth - 1, stile.adjustedLength - 1)
        val fromSouth = player.coords.z <= south.z
        val (start, dest) = if (fromSouth) south to north else north to south
        val step = if (dest.z > start.z) 1 else -1
        val approach = start.translate(0, -step)
        if (player.coords != approach) {
            playerWalk(approach)
            if (player.coords != approach) return
        }
        teleport(start, TeleportType.Exempt)
        delay(1)
        exactMove(start, dest, CLIMB_STEP_DELAY, CLIMB_CROSS_DELAY, if (step > 0) FACE_NORTH else FACE_SOUTH, TeleportType.Exempt)
        anim(CLIMB_SEQ, delay = CLIMB_STEP_DELAY)
        delay(CLIMB_TICKS)
        teleport(dest.translate(0, step), TeleportType.Exempt)
    }

    private fun cyclesToTicks(cycles: Int): Int = (cycles + 29) / 30

    enum class FlightState {
        Flying,
        OnPrey,
    }

    class Flight(val kebbit: Kebbit, val target: Npc, val hit: Boolean, val sentCycle: Int, var landCycle: Int) {
        var state: FlightState = FlightState.Flying
        var falcon: Npc? = null
    }

    class Kebbit(row: HunterFalconryRow) {
        val npc: String = RSCM.getReverseMapping(RSCMType.NPC, row.npc.id)
        val falcon: String = RSCM.getReverseMapping(RSCMType.NPC, row.falcon.id)
        val level: Int = row.level
        val xp: Int = row.xp
        val loot: List<String> = row.loot.map { it.internalName }
        val low: Int = row.low
        val high: Int = row.high
    }

    companion object {
        const val HUNTER = "stat.hunter"
        const val COINS = "obj.coins"
        const val AREA = "area.piscatoris_falconry"
        const val TIMER = "timer.hunter_falconry"
        const val INVESTED = "varp.falconry_invested"
        const val FALCON_GLOVE = "obj.falcon_on_gloves"
        const val EMPTY_GLOVE = "obj.falcon_gloves"
        const val STILE = "loc.hunting_fence_style"
        const val SEND_SEQ = "seq.human_falconer_attack"
        const val CLIMB_SEQ = "seq.human_walk_style"
        const val FALCON_SPOTANIM = "spotanim.falcon_travel_anim"
        val MATTHIAS = listOf("npc.hunting_npc_falconer", "npc.hunting_npc_falconer_nobird")
        val GLOVES = listOf(FALCON_GLOVE, EMPTY_GLOVE)
        val HAND_SLOTS = listOf(Wearpos.RightHand.slot, Wearpos.LeftHand.slot, Wearpos.Hands.slot)
        const val FEE = 500
        const val INVESTMENT = 500_000
        const val MIN_LEVEL = 43
        const val RANGE = 7
        const val FLIGHT_START = 24
        const val FLIGHT_STEP = 8
        const val RETURN_STEP = 16
        const val LEAVE_PREY_TICKS = 100
        const val KEBBIT_RESPAWN_TICKS = 15
        const val CLIMB_STEP_DELAY = 30
        const val CLIMB_CROSS_DELAY = 94
        const val CLIMB_TICKS = 3
        const val FACE_NORTH = 1024
        const val FACE_SOUTH = 0
    }
}
