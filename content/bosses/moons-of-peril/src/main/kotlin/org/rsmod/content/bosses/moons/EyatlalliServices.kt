package org.rsmod.content.bosses.moons

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.MesAnimType
import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.content.bosses.moons.MoonsFights.Companion.set
import org.rsmod.content.bosses.moons.blue.FrozenWeapons
import org.rsmod.content.interfaces.collectionlog.CollectionLog
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class EyatlalliServices
@Inject
constructor(private val random: GameRandom, private val frozenWeapons: FrozenWeapons) :
    PluginScript() {

    override fun ScriptContext.startup() {
        for (npc in NPCS) {
            onOpNpc1(npc) { talk(it.npc) }
            val npcType = checkNotNull(ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC)))
            for (obj in MoonsUniques.ALL) {
                val objType = checkNotNull(ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ)))
                onOpNpcU(npcType, objType) { sacrifice(it.npc, obj) }
            }
        }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) {
        startDialogue(npc) {
            while (true) {
                when (pick(options())) {
                    Topic.Weapon -> reclaimWeapon()
                    Topic.Essence -> handInEssence()
                    Topic.Tired -> tired()
                    Topic.Beautiful -> beautiful()
                    Topic.Goodbye -> {
                        chatPlayer(neutral, "Goodbye.")
                        return@startDialogue
                    }
                }
            }
        }
    }

    private fun Dialogue.options(): List<Pair<String, Topic>> {
        val options = mutableListOf<Pair<String, Topic>>()
        if (frozenWeapons.held(player) != null) {
            options += "I lost my weapon fighting the Blue Moon. Could I have it back?" to Topic.Weapon
        }
        if (player.inv.count(ESSENCE) > 0) {
            options += "I found some essence whilst fighting the lesser nagua." to Topic.Essence
        }
        options += "Do you ever get tired of guarding this place?" to Topic.Tired
        options += "This place is beautiful. I thought it was supposed to be a prison?" to
            Topic.Beautiful
        options += "Goodbye." to Topic.Goodbye
        return options
    }

    private suspend fun Dialogue.pick(options: List<Pair<String, Topic>>): Topic =
        when (options.size) {
            3 ->
                choice3(
                    options[0].first,
                    options[0].second,
                    options[1].first,
                    options[1].second,
                    options[2].first,
                    options[2].second,
                )
            4 ->
                choice4(
                    options[0].first,
                    options[0].second,
                    options[1].first,
                    options[1].second,
                    options[2].first,
                    options[2].second,
                    options[3].first,
                    options[3].second,
                )
            else ->
                choice5(
                    options[0].first,
                    options[0].second,
                    options[1].first,
                    options[1].second,
                    options[2].first,
                    options[2].second,
                    options[3].first,
                    options[3].second,
                    options[4].first,
                    options[4].second,
                )
        }

    private suspend fun Dialogue.reclaimWeapon() {
        chatPlayer(quiz, "I lost my weapon fighting the Blue Moon. Could I have it back?")
        if (frozenWeapons.reclaim(player) == FrozenWeapons.ReleaseResult.Kept) {
            chatEyatlalli(neutral, "You have no room to carry it, iknil. Make some space and ask again.")
            return
        }
        player.set("varbit.pmoon_boss_blue_wearprevent", 0)
        chatEyatlalli(neutral, "Here you go, iknil.")
        mesbox("Eyatlalli returns your lost weapon to you.")
    }

    private suspend fun Dialogue.handInEssence() {
        chatPlayer(neutral, "I found some essence whilst fighting the lesser nagua.")
        val count = player.inv.count(ESSENCE)
        if (count == 0 || access.invDel(access.inv, ESSENCE, count).failure) return
        access.statAdvance("stat.runecrafting", ESSENCE_XP * count)
        chatEyatlalli(neutral, "Thank you, iknil. I shall use it to reinforce the seal.")
    }

    private suspend fun Dialogue.tired() {
        chatPlayer(quiz, "Do you ever get tired of guarding this place?")
        chatEyatlalli(neutral, "No.")
        chatPlayer(quiz, "Care to explain further?")
        chatEyatlalli(
            neutral,
            "Does the stream tire of eroding the earth? Does the earth tire of forming the mountain?",
        )
        chatPlayer(neutral, "Not that I can tell.")
        chatEyatlalli(
            neutral,
            "I have more in common with them than I do with most of your kind, iknil. Think of me " +
                "as you do them.",
        )
    }

    private suspend fun Dialogue.beautiful() {
        chatPlayer(quiz, "This place is beautiful. I thought it was supposed to be a prison?")
        chatEyatlalli(neutral, "It is a prison. Everything around us serves that purpose.")
        chatPlayer(quiz, "Even the plants and the streams?")
        chatEyatlalli(
            neutral,
            "I am a nagua of earth and stream. Being close to both empowers me to do my task.",
        )
        chatEyatlalli(neutral, "Just as being far from moonlight weakens the Moons of Peril.")
    }

    private suspend fun ProtectedAccess.sacrifice(npc: Npc, obj: String) {
        startDialogue(npc) {
            chatEyatlalli(
                neutral,
                "Congratulations on your reward, iknil. That is a treasure from ages past.",
            )
            chatPlayer(
                neutral,
                "Yeah, about that... I don't really like this one. Could I have a different one?",
            )
            chatEyatlalli(neutral, "...")
            chatPlayer(quiz, "Was that rude?")
            chatEyatlalli(
                neutral,
                "I was just... Processing. I suppose I could allow you another chance to obtain a " +
                    "piece of equipment, but it won't be guaranteed.",
            )
            val first =
                choice2(
                    "Yes.",
                    true,
                    "No.",
                    false,
                    title = "Sacrifice this equipment for another chance at a different piece?",
                )
            if (!first) return@startDialogue declined()
            chatPlayer(happy, "Yes, that sounds good.")
            chatEyatlalli(neutral, "You're certain, iknil? You could end up with nothing.")
            val second =
                choice2(
                    "Yes.",
                    true,
                    "No.",
                    false,
                    title = "Give up this equipment for possibly nothing in return?",
                )
            if (!second) return@startDialogue declined()
            chatEyatlalli(neutral, "Very well.")
            exchange(obj)
        }
    }

    private suspend fun Dialogue.declined() {
        chatPlayer(neutral, "Actually, I don't dislike it that much. I think I'll keep it.")
        chatEyatlalli(neutral, "Very well.")
    }

    private suspend fun Dialogue.exchange(obj: String) {
        if (access.invDel(access.inv, obj, 1).failure) return
        if (random.of(MoonsUniques.SACRIFICE_CHANCE) != 0) {
            chatEyatlalli(neutral, "No luck this time, iknil.")
            chatPlayer(sad, "Oh... Can I have it back?")
            chatEyatlalli(neutral, "No.")
            return
        }
        val reward = MoonsUniques.ALL[random.of(MoonsUniques.ALL.size)]
        access.invAdd(access.inv, reward, 1)
        CollectionLog.grant(player, reward)
        objbox(reward, "Eyatlalli hands you a new ${itemName(reward)}.")
        if (reward == obj) {
            chatEyatlalli(neutral, "Congratulations, ik-")
            chatPlayer(shocked, "This is the same thing I just gave you!")
            chatEyatlalli(neutral, "So it seems. You're welcome to try again.")
        } else {
            chatEyatlalli(neutral, "Congratulations, iknil. Enjoy your new equipment.")
        }
    }

    private fun itemName(obj: String): String =
        checkNotNull(ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ))).name

    private suspend fun Dialogue.chatEyatlalli(anim: MesAnimType, text: String) =
        chatNpcSpecific("Eyatlalli", HEAD, anim, text)

    private enum class Topic {
        Weapon,
        Essence,
        Tired,
        Beautiful,
        Goodbye,
    }

    private companion object {
        private val NPCS = listOf("npc.pmoon_eyatlalli_postquest", "npc.pmoon_eyatlalli_vis_postquest")
        private const val HEAD = "npc.pmoon_eyatlalli_vis"
        private const val ESSENCE = "obj.sulphurous_essence"
        private const val ESSENCE_XP = 50.0
    }
}
