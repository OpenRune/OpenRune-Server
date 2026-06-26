package org.rsmod.api.bosses.spec

public data class Odds(val chance: Int, val outOf: Int) {
    init {
        require(outOf > 0) { "outOf must be > 0, was $outOf" }
        require(chance in 0..outOf) { "chance must be in 0..$outOf, was $chance" }
    }
}

public infix fun Int.chancesIn(outOf: Int): Odds = Odds(this, outOf)
