package org.rsmod.content.slayer.dialogue

data class HighCombatRedirect(
    val minimumCombat: Int,
    val suggestedMaster: String,
    val place: String,
)

enum class ActiveTaskMessageStyle {
    /** "You're still hunting X; you have N to go. Come back when you've finished your task." */
    WithComebackSemicolon,

    /** "You're still hunting X, you have N to go. Come back when you've finished your task." */
    WithComebackComma,

    /** "You're still hunting X, you have N to go." */
    Short,

    /** "You're still hunting X; you have N to go." (no comeback line) */
    SemicolonOnly,
}

class SlayerMasterProfile(
    val npcId: Int,
    vararg highCombatRedirects: HighCombatRedirect,
    val supportsCape: Boolean = false,
    val supportsTaskSkip: Boolean = false,
    val activeTaskStyle: ActiveTaskMessageStyle = ActiveTaskMessageStyle.Short,
    val nearContactMessage: String? = null,
) {
    val highCombatRedirects: List<HighCombatRedirect> = highCombatRedirects.toList()
}

object SlayerMasterProfiles {
    private val mazchnaRedirect = HighCombatRedirect(20, "Mazchna", "in Morytania")

    private val vannakaRedirect = HighCombatRedirect(40, "Vannaka", "under Edgeville")

    private val chaeldarRedirect = HighCombatRedirect(70, "Chaeldar", "in Zanaris")

    private val profilesByNpcId: Map<Int, SlayerMasterProfile> =
        mapOf(SlayerMasters.Npc.turael to
                SlayerMasterProfile(
                    SlayerMasters.Npc.turael, chaeldarRedirect,
                    supportsTaskSkip = true,
                    activeTaskStyle = ActiveTaskMessageStyle.Short,
                ),
            SlayerMasters.Npc.aya to
                SlayerMasterProfile(
                    SlayerMasters.Npc.aya, chaeldarRedirect,
                    supportsTaskSkip = true,
                    activeTaskStyle = ActiveTaskMessageStyle.Short,
                ),
            SlayerMasters.Npc.spriaActive to
                SlayerMasterProfile(
                    SlayerMasters.Npc.spriaActive, mazchnaRedirect, vannakaRedirect, chaeldarRedirect,
                    activeTaskStyle = ActiveTaskMessageStyle.Short,
                ),
            SlayerMasters.Npc.spria to
                SlayerMasterProfile(
                    SlayerMasters.Npc.spria, mazchnaRedirect, vannakaRedirect, chaeldarRedirect,
                    activeTaskStyle = ActiveTaskMessageStyle.Short,
                ),
            SlayerMasters.Npc.mazchna to
                SlayerMasterProfile(
                    SlayerMasters.Npc.mazchna, chaeldarRedirect,
                    activeTaskStyle = ActiveTaskMessageStyle.WithComebackComma,
                ),
            SlayerMasters.Npc.achtryn to
                SlayerMasterProfile(
                    SlayerMasters.Npc.achtryn, vannakaRedirect,
                    activeTaskStyle = ActiveTaskMessageStyle.WithComebackComma,
                ),
            SlayerMasters.Npc.vannaka to
                SlayerMasterProfile(
                    SlayerMasters.Npc.vannaka,
                    activeTaskStyle = ActiveTaskMessageStyle.WithComebackSemicolon,
                ),
            SlayerMasters.Npc.chaeldar to
                SlayerMasterProfile(
                    SlayerMasters.Npc.chaeldar,
                    activeTaskStyle = ActiveTaskMessageStyle.WithComebackSemicolon,
                ),
            SlayerMasters.Npc.nieve to
                SlayerMasterProfile(
                    SlayerMasters.Npc.nieve,
                    activeTaskStyle = ActiveTaskMessageStyle.WithComebackSemicolon,
                ),
            SlayerMasters.Npc.steve to
                SlayerMasterProfile(
                    SlayerMasters.Npc.steve,
                    activeTaskStyle = ActiveTaskMessageStyle.SemicolonOnly,
                ),
            SlayerMasters.Npc.kuradal to
                SlayerMasterProfile(
                    SlayerMasters.Npc.kuradal,
                    supportsCape = true,
                    activeTaskStyle = ActiveTaskMessageStyle.WithComebackSemicolon,
                ),
            SlayerMasters.Npc.duradel to
                SlayerMasterProfile(
                    SlayerMasters.Npc.duradel,
                    supportsCape = true,
                    activeTaskStyle = ActiveTaskMessageStyle.WithComebackSemicolon,
                    nearContactMessage =
                        "You could just talk to me, you know - I am standing right here.",
                ),
            SlayerMasters.Npc.konar to
                SlayerMasterProfile(
                    SlayerMasters.Npc.konar,
                    nearContactMessage =
                        "Bringer of death, why do you insist on using such magic when I'm right next to you? Come and talk to me.",
                ),
            SlayerMasters.Npc.mortimer to
                SlayerMasterProfile(
                    SlayerMasters.Npc.mortimer,
                    activeTaskStyle = ActiveTaskMessageStyle.WithComebackSemicolon,
                    nearContactMessage =
                        "I may be a skeleton, but I still have feelings! Come talk to me face to... face.",
                ),
        )

    fun forNpc(npcId: Int): SlayerMasterProfile? = profilesByNpcId[npcId]

    fun forNpcOrDefault(npcId: Int): SlayerMasterProfile =
        forNpc(npcId) ?: SlayerMasterProfile(npcId = npcId)
}
