package org.rsmod.content.quest.area.varrock.childrenofthesun

import dev.openrune.types.MesAnimType
import org.rsmod.api.player.dialogue.Dialogue

internal object CotsStage {
    const val NotStarted = 0
    const val Started = 2
    const val Tailing = 6
    const val Eavesdropping = 8
    const val ReportToTobyn = 10
    const val Marking = 12
    const val OnRoof = 16
    const val Interrogating = 18
    const val Interrogated = 20
    const val ItzlaLeft = 22
    const val Complete = 24
}

internal object CotsNpc {
    const val Alina = "npc.vmq1_alina_vis"
    const val Noah = "npc.vmq1_noah_vis"
    const val Tobyn = "npc.vmq1_guard_sergeant_vis"
    const val Itzla = "npc.vmq1_itzla_vis"
    const val BagGuard = "npc.vmq1_bag_guard_vis"
    const val RedHood = "npc.vmq1_bandit_1_vis"
    const val Woman = "npc.vmq1_bandit_2_vis"
    const val Tanned = "npc.vmq1_bandit_3_vis"
    const val Bearded = "npc.vmq1_bandit_4_vis"

    const val GuardCount = 10
    const val BanditGuards = 4

    fun guardVarbit(guard: Int): String = "varbit.vmq1_guard_$guard"

    fun markableGuard(guard: Int): String = "npc.vmq1_guard_${guard}_unmarked"

    fun markedGuard(guard: Int): String = "npc.vmq1_guard_${guard}_marked"
}

internal suspend fun Dialogue.alina(anim: MesAnimType, text: String) =
    chatNpcSpecific("Alina", CotsNpc.Alina, anim, text)

internal suspend fun Dialogue.noah(anim: MesAnimType, text: String) =
    chatNpcSpecific("Noah", CotsNpc.Noah, anim, text)

internal suspend fun Dialogue.tobyn(anim: MesAnimType, text: String) =
    chatNpcSpecific("Sergeant Tobyn", CotsNpc.Tobyn, anim, text)

internal suspend fun Dialogue.itzla(anim: MesAnimType, text: String) =
    chatNpcSpecific("Prince Itzla Arkan", CotsNpc.Itzla, anim, text)

internal suspend fun Dialogue.bagGuard(anim: MesAnimType, text: String) =
    chatNpcSpecific("Guard", CotsNpc.BagGuard, anim, text)

internal suspend fun Dialogue.bandit(type: String, anim: MesAnimType, text: String) =
    chatNpcSpecific("Bandit", type, anim, text)
