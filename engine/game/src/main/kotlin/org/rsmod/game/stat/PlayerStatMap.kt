package org.rsmod.game.stat

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.StatType
import it.unimi.dsi.fastutil.bytes.Byte2ByteOpenHashMap
import it.unimi.dsi.fastutil.bytes.Byte2IntOpenHashMap
import org.rsmod.annotations.InternalApi

public class PlayerStatMap(
    private val xp: Byte2IntOpenHashMap = Byte2IntOpenHashMap(),
    private val baseLevels: Byte2ByteOpenHashMap = Byte2ByteOpenHashMap(),
    private val currLevels: Byte2ByteOpenHashMap = Byte2ByteOpenHashMap(),
) {
    public fun getXP(stat: String): Int = getFineXP(stat) / XP_FINE_PRECISION

    public fun setXP(stat: String, xp: Int) {
        setFineXP(stat, xp * XP_FINE_PRECISION)
    }

    public fun getFineXP(stat: String): Int = xp.getOrDefault(stat.asRSCM(RSCMType.STAT).toByte(), 0)

    public fun setFineXP(stat: String, xp: Int) {
        require(xp in 0..MAX_FINE_XP) {
            "`xp` must be within range [0..$MAX_FINE_XP]. (stat=$stat, xp=$xp)"
        }
        this.xp[stat.asRSCM(RSCMType.STAT).toByte()] = xp
    }

    @InternalApi
    public fun getBaseLevel(stat: String): Byte = baseLevels.getOrDefault(stat.asRSCM(RSCMType.STAT).toByte(), 1)

    public fun setBaseLevel(stat: String, level: Byte) {
        this.baseLevels[stat.asRSCM(RSCMType.STAT).toByte()] = level
    }

    @InternalApi
    public fun getCurrentLevel(stat: String): Byte = currLevels.getOrDefault(stat.asRSCM(RSCMType.STAT).toByte(), 1)

    public fun setCurrentLevel(stat: String, level: Byte) {
        this.currLevels[stat.asRSCM(RSCMType.STAT).toByte()] = level
    }

    public companion object {
        public const val MAX_XP: Int = 200_000_000
        public const val XP_FINE_PRECISION: Int = 10
        public const val MAX_FINE_XP: Int = MAX_XP * XP_FINE_PRECISION

        public fun toFineXP(xp: Double): Double = xp * XP_FINE_PRECISION

        public fun normalizeFineXP(fineXp: Int): Int = fineXp / XP_FINE_PRECISION
    }
}
