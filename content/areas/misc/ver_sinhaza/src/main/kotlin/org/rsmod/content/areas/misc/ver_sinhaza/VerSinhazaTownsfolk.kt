package org.rsmod.content.areas.misc.ver_sinhaza

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class VerSinhazaTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        for (citizen in CITIZENS) {
            onOpNpc1(citizen) { startDialogue(it.npc) { citizen() } }
        }
        for (citizen in FREED_CITIZENS) {
            onOpNpc1(citizen) { startDialogue(it.npc) { freedCitizen() } }
        }
        onOpNpc1("npc.myq4_harpert_vis") { startDialogue(it.npc) { harpert() } }
        for (noble in NOBLES) {
            onOpNpc1(noble) { startDialogue(it.npc) { noble() } }
        }
        onOpNpc1("npc.tob_door_guard") { startDialogue(it.npc) { doorGuard() } }
        onOpNpc1("npc.tob_door_guard_inner") { startDialogue(it.npc) { doorGuard() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Meiyerditch_citizen (Theatre of Blood)
    private suspend fun Dialogue.citizen() {
        chatNpc(neutral, access.random.pick(THEATRE_HOPEFULS))
    }

    // https://oldschool.runescape.wiki/w/Transcript:Meiyerditch_citizen (Theatre of Blood, after
    // The Blood Moon Rises)
    private suspend fun Dialogue.freedCitizen() {
        val line =
            access.random.pick(
                "I often thought about trying out the Theatre myself before Drakan was dealt " +
                    "with. Glad I never ended up doing that!",
                "It's nice that they let us watch the performances in the Theatre now. It's fun " +
                    "seeing how good some of those warriors from Misthalin are!",
                "Going to give the Theatre a go? You look like you've got what it takes.",
            )
        chatNpc(happy, line)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Harpert
    private suspend fun Dialogue.harpert() {
        chatNpc(neutral, access.random.pick(THEATRE_HOPEFULS))
    }

    // https://oldschool.runescape.wiki/w/Transcript:Vyrelord
    // https://oldschool.runescape.wiki/w/Transcript:Vyrelady
    // TODO(content): [system:vyre_disguise] the friendly lines for players wearing a full vyre
    //  noble disguise
    private suspend fun Dialogue.noble() {
        if (QuestRequirements.hasCompleted(player, BLOOD_MOON_RISES)) {
            val line =
                access.random.pick(
                    "I do enjoy a day out at the Theatre.",
                    "Come to see this show? I bet it's going to be a good one.",
                )
            chatNpc(happy, line)
            return
        }
        val line =
            access.random.pick(
                "I do enjoy a day out at the Theatre.",
                "Will you be entering the Theatre? It will be fun to watch you die.",
                "Get lost!",
                "Leave me alone food sack!",
            )
        chatNpc(angry, line)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Vampyre_Juvenile (outside Ver Sinhaza)
    // TODO(content): [system:vyre_disguise] "Leave me alone. I'm busy." while disguised
    private suspend fun Dialogue.doorGuard() {
        chatNpc(
            angry,
            "What's your problem, food sack? Better move on before I perforate you real good!",
        )
    }

    private companion object {
        const val BLOOD_MOON_RISES = "quest_bloodmoonrises"

        val THEATRE_HOPEFULS =
            arrayOf(
                "It can't be that hard can it?",
                "I guess this will be my last day of blood tithes, one way or another.",
                "I will survive the Theatre! Just like Serafina did!",
                "Soon I will be free!",
                "I'm starting to think I made a mistake coming here.",
                "Can't talk. I need to prepare myself.",
            )

        val CITIZENS =
            listOf(
                "npc.tob_citizen_1_vis",
                "npc.tob_citizen_2_vis",
                "npc.tob_citizen_3_vis",
                "npc.tob_citizen_4_vis",
            )

        val FREED_CITIZENS =
            listOf(
                "npc.tob_citizen_1_post_myq6",
                "npc.tob_citizen_2_post_myq6",
                "npc.tob_citizen_3_post_myq6",
                "npc.tob_citizen_4_post_myq6",
            )

        val NOBLES =
            listOf("npc.tob_noble_1", "npc.tob_noble_2", "npc.tob_noble_3", "npc.tob_noble_4")
    }
}
