package org.rsmod.content.slayer.dialogue

data class HighCombatRedirect(
    val minimumCombat: Int,
    val suggestedMaster: String,
    val location: String,
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

data class SlayerMasterProfile(
    val npcId: Int,
    val supportsCape: Boolean = false,
    val supportsTaskSkip: Boolean = false,
    val highCombatRedirect: HighCombatRedirect? = null,
    val activeTaskStyle: ActiveTaskMessageStyle = ActiveTaskMessageStyle.Short,
    val nearContactMessage: String? = null,
)

object SlayerMasterProfiles {
    private val chaeldarRedirect = HighCombatRedirect(minimumCombat = 70, suggestedMaster = "Chaeldar", location = "Zanaris")

    private val vannakaRedirect = HighCombatRedirect(minimumCombat = 40, suggestedMaster = "Vannaka", location = "Edgeville")

    private val profilesByNpcId: Map<Int, SlayerMasterProfile> =
        mapOf(SlayerMasters.Npc.turael to
                SlayerMasterProfile(
                    npcId = SlayerMasters.Npc.turael,
                    supportsTaskSkip = true,
                    highCombatRedirect = chaeldarRedirect,
                    activeTaskStyle = ActiveTaskMessageStyle.Short,
                ),
            SlayerMasters.Npc.aya to
                SlayerMasterProfile(
                    npcId = SlayerMasters.Npc.aya,
                    supportsTaskSkip = true,
                    highCombatRedirect = chaeldarRedirect,
                    activeTaskStyle = ActiveTaskMessageStyle.Short,
                ),
            SlayerMasters.Npc.spriaActive to
                SlayerMasterProfile(
                    npcId = SlayerMasters.Npc.spriaActive,
                    supportsTaskSkip = true,
                    highCombatRedirect = chaeldarRedirect,
                    activeTaskStyle = ActiveTaskMessageStyle.Short,
                ),
            SlayerMasters.Npc.spria to
                SlayerMasterProfile(
                    npcId = SlayerMasters.Npc.spria,
                    supportsTaskSkip = true,
                    highCombatRedirect = chaeldarRedirect,
                    activeTaskStyle = ActiveTaskMessageStyle.Short,
                ),
            SlayerMasters.Npc.mazchna to
                SlayerMasterProfile(
                    npcId = SlayerMasters.Npc.mazchna,
                    highCombatRedirect = chaeldarRedirect,
                    activeTaskStyle = ActiveTaskMessageStyle.WithComebackComma,
                ),
            SlayerMasters.Npc.achtryn to
                SlayerMasterProfile(
                    npcId = SlayerMasters.Npc.achtryn,
                    highCombatRedirect = vannakaRedirect,
                    activeTaskStyle = ActiveTaskMessageStyle.WithComebackComma,
                ),
            SlayerMasters.Npc.vannaka to
                SlayerMasterProfile(
                    npcId = SlayerMasters.Npc.vannaka,
                    activeTaskStyle = ActiveTaskMessageStyle.WithComebackSemicolon,
                ),
            SlayerMasters.Npc.chaeldar to
                SlayerMasterProfile(
                    npcId = SlayerMasters.Npc.chaeldar,
                    activeTaskStyle = ActiveTaskMessageStyle.WithComebackSemicolon,
                ),
            SlayerMasters.Npc.nieve to
                SlayerMasterProfile(
                    npcId = SlayerMasters.Npc.nieve,
                    activeTaskStyle = ActiveTaskMessageStyle.WithComebackSemicolon,
                ),
            SlayerMasters.Npc.steve to
                SlayerMasterProfile(
                    npcId = SlayerMasters.Npc.steve,
                    activeTaskStyle = ActiveTaskMessageStyle.SemicolonOnly,
                ),
            SlayerMasters.Npc.kuradal to
                SlayerMasterProfile(
                    npcId = SlayerMasters.Npc.kuradal,
                    supportsCape = true,
                    activeTaskStyle = ActiveTaskMessageStyle.WithComebackSemicolon,
                ),
            SlayerMasters.Npc.duradel to
                SlayerMasterProfile(
                    npcId = SlayerMasters.Npc.duradel,
                    supportsCape = true,
                    activeTaskStyle = ActiveTaskMessageStyle.WithComebackSemicolon,
                    nearContactMessage =
                        "You could just talk to me, you know - I am standing right here.",
                ),
            SlayerMasters.Npc.konar to
                SlayerMasterProfile(
                    npcId = SlayerMasters.Npc.konar,
                    nearContactMessage =
                        "Bringer of death, why do you insist on using such magic when I'm right next to you? Come and talk to me.",
                ),
        )

    fun forNpc(npcId: Int): SlayerMasterProfile? = profilesByNpcId[npcId]

    fun forNpcOrDefault(npcId: Int): SlayerMasterProfile =
        forNpc(npcId) ?: SlayerMasterProfile(npcId = npcId)
}
