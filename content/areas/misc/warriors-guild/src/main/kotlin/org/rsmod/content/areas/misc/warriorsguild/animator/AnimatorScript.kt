package org.rsmod.content.areas.misc.warriorsguild.animator

import jakarta.inject.Inject
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.npc.owner.assignSpawnOwner
import org.rsmod.api.npc.owner.clearSpawnOwner
import org.rsmod.api.npc.owner.ownedBy
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.MiscOutput
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.table.WarguildAnimatorSetsRow
import org.rsmod.content.areas.misc.warriorsguild.lostArmour
import org.rsmod.content.areas.misc.warriorsguild.scripts.TokenClaimScript.Companion.shanomiClaimTokens
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class AnimatorScript
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val npcList: NpcList,
    private val mapClock: MapClock,
    private val aiPlayerInteractions: AiPlayerInteractions,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(ANIMATOR) { animate(it.loc.coords) }
        onOpLocU(ANIMATOR) {
            if (AnimatorSets.isPiece(it.objType.id)) {
                animate(it.loc.coords)
            } else {
                startDialogue { wrongItem() }
            }
        }
        onOpNpc1(SHANOMI) { startDialogue(it.npc) { shanomi() } }
        onPlayerLogout { despawnArmour(player) }
    }

    private suspend fun ProtectedAccess.animate(animator: CoordGrid) {
        if (armourOf(player) != null) {
            mes("You've already summoned an animated armour.")
            return
        }
        if (player.lostArmour != 0) {
            mesbox("Shanomi is still looking after your last set of armour. Speak to her first.")
            return
        }
        if (AnimatorSets.carried(player) == null) {
            mesbox(
                "You need a suitable platebody, legs and full helm of the same type to activate " +
                    "the armour animator.",
            )
            return
        }
        val set = AnimatorSets.take(player) ?: return
        anim("seq.human_pickupfloor")
        soundSynth("synth.equip_body", delay = 10)
        mesboxNp(PLACED_TEXT)
        delay(1)
        mesboxNp("$PLACED_TEXT<br>The animator hums; something appears to be working...")
        soundSynth("synth.warguild_animate")
        walk(coords.translateZ(STEP_BACK_TILES))
        delay(1)
        soundSynth("synth.warguild_animate_alive")
        delay(1)
        val npc = spawn(player, set, animator)
        delay(1)
        ifCloseChat()
        delay(ATTACK_DELAY - 1)
        if (npc.isSlotAssigned && npc.hitpoints > 0) npc.opPlayer2(player, aiPlayerInteractions)
    }

    private fun spawn(player: Player, set: WarguildAnimatorSetsRow, at: CoordGrid): Npc {
        val npc = Npc(set.npc, at)
        npc.respawns = false
        npcRepo.add(npc, LIFETIME_TICKS)
        npc.assignSpawnOwner(player, mapClock.cycle)
        npc.anim("seq.warguild_animate")
        npc.say("I'm ALIVE!")
        MiscOutput.hintArrowNpc(player, npc.slotId)
        return npc
    }

    private fun armourOf(player: Player): Npc? =
        npcList.ownedBy(player).firstOrNull { AnimatorSets.byNpc(it.id) != null }

    private fun despawnArmour(player: Player) {
        val npc = armourOf(player) ?: return
        if (npc.hitpoints <= 0) return
        npc.clearSpawnOwner()
        npcRepo.del(npc, Int.MAX_VALUE)
    }

    private suspend fun Dialogue.wrongItem() {
        chatNpcSpecific(
            "Shanomi",
            SHANOMI,
            neutral,
            "Plain full plate armour in those machines only you may use, of appropriate " +
                "materials. When other items used, bad things happen, yes!",
        )
        chatPlayer(
            quiz,
            "Okay, so I can only use plain full plate armour of appropriate materials, no trimmed " +
                "armour or unusual kit?",
        )
        chatNpcSpecific("Shanomi", SHANOMI, neutral, "It is as you say.")
    }

    private suspend fun Dialogue.shanomi() {
        chatNpc(neutral, "Greetings ${player.displayName}. Welcome you are in the test of combat.")
        shanomiOptions()
    }

    private suspend fun Dialogue.shanomiOptions() {
        val topic =
            if (player.lostArmour != 0) {
                choice5(
                    "My armour disappeared!",
                    Topic.Lost,
                    "What do I do here?",
                    Topic.WhatToDo,
                    "Where do the machines come from?",
                    Topic.Machines,
                    "May I claim my tokens please?",
                    Topic.Tokens,
                    "Bye!",
                    Topic.Bye,
                )
            } else {
                choice4(
                "What do I do here?",
                Topic.WhatToDo,
                "Where do the machines come from?",
                Topic.Machines,
                "May I claim my tokens please?",
                Topic.Tokens,
                    "Bye!",
                    Topic.Bye,
                )
            }
        shanomiTopic(topic)
    }

    private suspend fun Dialogue.shanomiTopic(topic: Topic) {
        when (topic) {
            Topic.WhatToDo -> {
                whatToDo()
                shanomiOptions()
            }
            Topic.Machines -> {
                machines()
                shanomiOptions()
            }
            Topic.Tokens -> shanomiClaimTokens()
            Topic.Bye -> {
                chatPlayer(neutral, "Bye!")
                chatNpc(neutral, "Health be with you travelling.")
            }
            Topic.Lost -> lostArmour()
        }
    }

    private suspend fun Dialogue.lostArmour() {
        chatPlayer(sad, "My armour disappeared!")
        chatNpc(neutral, "Correct you are. Not killed it was. Collected your armour I did.")
        chatPlayer(quiz, "Excellent, may I have it so I can try the machine again?")
        if (armourOf(player) != null) {
            chatNpc(neutral, "Still alive your armour is! Defeat it you must.")
            return
        }
        when (AnimatorSets.returnHeld(player)) {
            AnimatorSets.Return.NoSpace -> {
                chatNpc(neutral, "Space you will need in your pack.")
                chatPlayer(neutral, "Okay, I'll go make some room in my inventory and come back.")
            }
            AnimatorSets.Return.Returned -> {
                chatNpc(neutral, "It is as you say.")
                chatPlayer(happy, "Thanks!")
            }
            AnimatorSets.Return.NothingHeld -> Unit
        }
    }

    private suspend fun Dialogue.whatToDo() {
        chatPlayer(quiz, "What do I do here?")
        chatNpc(
            neutral,
            "A spare suit of plate armour need you will. Full helm, plate leggings and platebody " +
                "yes? Placing it in the centre of the magical machines you will be doing. KA- " +
                "POOF! The armour, it attacks most furiously as if",
        )
        chatNpc(neutral, "alive! Kill it you must, yes.")
        chatPlayer(
            confused,
            "So I use a full set of plate armour on the centre plate of the machines and it will " +
                "animate it? Then I have to kill my own armour... how bizarre!",
        )
        chatNpc(
            neutral,
            "Yes. It is as you are saying. For this earn tokens you will. Also gain experience in " +
                "combat you will. Trained long and hard here have I.",
        )
        chatPlayer(quiz, "You're not from around here are you...?")
        chatNpc(neutral, "It is as you say.")
        chatPlayer(quiz, "So will I lose my armour?")
        chatNpc(
            neutral,
            "Lose armour you will if damaged too much it becomes. Rare this is, but still " +
                "possible. If kill you the armour does, also lose armour you will.",
        )
        chatPlayer(
            neutral,
            "So, occasionally I might lose a bit because it's being bashed about and I'll " +
                "obviously lose it if I die... that it?",
        )
        chatNpc(neutral, "It is as you say.")
    }

    private suspend fun Dialogue.machines() {
        chatPlayer(quiz, "Where do the machines come from?")
        chatNpc(neutral, "Make them I did, with magics.")
        chatPlayer(confused, "Magic, in the Warrior's Guild?")
        chatNpc(
            neutral,
            "A skilled warrior also am I. Harrallak mistakes does not make. Potential in my " +
                "invention he sees and opportunity grasps.",
        )
        chatPlayer(
            neutral,
            "I see, so you made the magical machines and Harrallak saw how they could be used in " +
                "the guild to train warrior's combat... interesting. Harrallak certainly is an " +
                "intelligent guy.",
        )
        chatNpc(neutral, "It is as you say.")
    }

    private enum class Topic {
        Lost,
        WhatToDo,
        Machines,
        Tokens,
        Bye,
    }

    private companion object {
        const val ANIMATOR = "loc.warguild_animator_cent"
        const val SHANOMI = "npc.warguild_shanomi"
        const val PLACED_TEXT = "You place your armour on the platform where it disappears..."
        const val STEP_BACK_TILES = 2
        const val ATTACK_DELAY = 2
        const val LIFETIME_TICKS = 500
    }
}
