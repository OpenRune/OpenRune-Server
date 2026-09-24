package org.rsmod.content.raids.toa.party

/**
 * Manages the invocation state for a TOA party.
 *
 * Invocations are stored as three bitmaps (31 bits each) to cover
 * all ~46 invocations. Raid level and active count are kept in sync
 * as invocations are toggled.
 */
class ToaPartySettings(
    var raidLevel: Int = 0,
    var activeInvocations: Int = 0,
    var kcRequirement: Int = 0,
    val invocationBitmaps: IntArray = IntArray(BITMAP_COUNT),
) {
    /**
     * Returns `true` if the given invocation is currently enabled.
     */
    fun isActive(invocation: ToaInvocation): Boolean {
        val slot = bitmapSlot(invocation.index)
        val bit = bitMask(invocation.index)
        return invocationBitmaps[slot] and bit != 0
    }

    /**
     * Enables an invocation. Adds its level modifier to the raid level.
     */
    fun flag(invocation: ToaInvocation) {
        val slot = bitmapSlot(invocation.index)
        val bit = bitMask(invocation.index)
        invocationBitmaps[slot] = invocationBitmaps[slot] or bit
        raidLevel += invocation.levelModifier
        activeInvocations++
    }

    /**
     * Disables an invocation. Subtracts its level modifier from the raid level.
     */
    fun unflag(invocation: ToaInvocation) {
        val slot = bitmapSlot(invocation.index)
        val bit = bitMask(invocation.index)
        invocationBitmaps[slot] = invocationBitmaps[slot] and bit.inv()
        raidLevel -= invocation.levelModifier
        activeInvocations--
    }

    /**
     * Disables every invocation in the given category.
     */
    fun unflagCategory(category: ToaInvocationCategory) {
        for (invocation in ToaInvocation.ALL) {
            if (invocation.category == category && isActive(invocation)) {
                unflag(invocation)
            }
        }
    }

    /**
     * Returns `true` if every invocation in the category is active.
     */
    fun allActive(category: ToaInvocationCategory): Boolean {
        return ToaInvocation.ALL
            .filter { it.category == category }
            .all { isActive(it) }
    }

    /**
     * Replaces bitmaps from a preset and recalculates raid level + count.
     */
    fun loadPreset(preset: IntArray) {
        require(preset.size == BITMAP_COUNT) { "Preset must have $BITMAP_COUNT bitmaps" }
        preset.copyInto(invocationBitmaps)
        recalculate()
    }

    /**
     * Resets all invocations to off.
     */
    fun clear() {
        invocationBitmaps.fill(0)
        raidLevel = 0
        activeInvocations = 0
    }

    /**
     * Creates an independent copy of these settings.
     */
    fun copy(): ToaPartySettings {
        return ToaPartySettings(
            raidLevel = raidLevel,
            activeInvocations = activeInvocations,
            kcRequirement = kcRequirement,
            invocationBitmaps = invocationBitmaps.copyOf(),
        )
    }

    /**
     * The raid mode string, derived from the current raid level.
     */
    val mode: String
        get() = when {
            raidLevel >= 300 -> "Expert"
            raidLevel >= 150 -> "Normal"
            else -> "Entry"
        }

    /**
     * Recalculates raid level and active count from the bitmaps.
     * Used after loading a preset or copying settings.
     */
    private fun recalculate() {
        raidLevel = 0
        activeInvocations = 0
        for (invocation in ToaInvocation.ALL) {
            if (isActive(invocation)) {
                raidLevel += invocation.levelModifier
                activeInvocations++
            }
        }
    }

    companion object {
        /** Three 31-bit ints to cover all invocation indices. */
        private const val BITMAP_COUNT = 3

        /** Bits per bitmap slot. */
        private const val BITS_PER_SLOT = 31

        /**
         * Which of the 3 bitmap ints this index falls in.
         * 0..30 → slot 0, 31..61 → slot 1, 62+ → slot 2
         */
        private fun bitmapSlot(index: Int): Int = when {
            index > 61 -> 2
            index > 30 -> 1
            else -> 0
        }

        /** The single-bit mask for the given index within its slot. */
        private fun bitMask(index: Int): Int = 1 shl (index % BITS_PER_SLOT)
    }
}
