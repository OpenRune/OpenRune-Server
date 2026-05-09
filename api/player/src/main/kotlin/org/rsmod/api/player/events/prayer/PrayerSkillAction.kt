package org.rsmod.api.player.events.prayer

/**
 * Prayer skill outcomes published inside [org.rsmod.api.player.events.skilling.SkillingActionContext.Prayer].
 */
public sealed class PrayerSkillAction {
    /**
     * @param catacombsBonePrayerRestore Prayer point restore from burying this bone in the
     * Catacombs of Kourend (from [org.rsmod.api.table.prayer.SkillPrayerRow.prayerRestore]); `0`
     * for ashes or bones with no restore tier.
     */
    public data class BuryComplete(
        public val itemInternal: String,
        public val ashes: Boolean,
        public val experienceGranted: Double,
        public val catacombsBonePrayerRestore: Int,
    ) : PrayerSkillAction()

    /**
     * @param catacombsBonePrayerRestore Same restore tier as manual burial for this bone type.
     */
    public data class BonecrusherCrushComplete(
        public val boneItemInternal: String,
        public val experienceGranted: Double,
        public val catacombsBonePrayerRestore: Int,
    ) : PrayerSkillAction()
}
