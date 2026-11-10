package org.rsmod.content.other.bots

class BotPerf {
    private val cycleNanos = LongArray(WINDOW)
    private val botNanos = LongArray(WINDOW)
    private var cycleStart = 0L
    private var botSpent = 0L
    private var index = 0
    private var samples = 0

    fun startCycle() {
        cycleStart = System.nanoTime()
        botSpent = 0L
    }

    inline fun <T> timeBots(block: () -> T): T {
        val start = System.nanoTime()
        try {
            return block()
        } finally {
            addBotTime(System.nanoTime() - start)
        }
    }

    fun addBotTime(nanos: Long) {
        botSpent += nanos
    }

    fun endCycle() {
        if (cycleStart == 0L) {
            return
        }
        cycleNanos[index] = System.nanoTime() - cycleStart
        botNanos[index] = botSpent
        index = (index + 1) % WINDOW
        samples = minOf(samples + 1, WINDOW)
    }

    fun summary(): String {
        if (samples == 0) {
            return "No cycles measured yet."
        }
        val cycles = cycleNanos.take(samples)
        val bots = botNanos.take(samples)
        return "Last $samples ticks: cycle avg ${ms(cycles.average())}ms, " +
            "max ${ms(cycles.max().toDouble())}ms; bot logic avg ${ms(bots.average())}ms"
    }

    private fun ms(nanos: Double): String = "%.2f".format(nanos / NANOS_PER_MILLI)

    private companion object {
        private const val WINDOW = 100
        private const val NANOS_PER_MILLI = 1_000_000.0
    }
}
