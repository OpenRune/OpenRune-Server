package org.rsmod.content.raids.toa.party

class ToaPartySettings(
    var raidLevel: Int = 0,
    var activeInvocations: Int = 0,
    var kcRequirement: Int = 0,
    val invocationBitmaps: IntArray = IntArray(BITMAP_COUNT),
) {

    fun isActive(invocation: ToaInvocation): Boolean {
        val slot = bitmapSlot(invocation.index)
        val bit = bitMask(invocation.index)
        return invocationBitmaps[slot] and bit != 0
    }

    fun flag(invocation: ToaInvocation) {
        val slot = bitmapSlot(invocation.index)
        val bit = bitMask(invocation.index)
        invocationBitmaps[slot] = invocationBitmaps[slot] or bit
        raidLevel += invocation.levelModifier
        activeInvocations++
    }

    fun unflag(invocation: ToaInvocation) {
        val slot = bitmapSlot(invocation.index)
        val bit = bitMask(invocation.index)
        invocationBitmaps[slot] = invocationBitmaps[slot] and bit.inv()
        raidLevel -= invocation.levelModifier
        activeInvocations--
    }

    fun unflagCategory(category: ToaInvocationCategory) {
        for (invocation in ToaInvocation.ALL) {
            if (invocation.category == category && isActive(invocation)) {
                unflag(invocation)
            }
        }
    }

    fun allActive(category: ToaInvocationCategory): Boolean {
        return ToaInvocation.ALL
            .filter { it.category == category }
            .all { isActive(it) }
    }

    fun loadPreset(preset: IntArray) {
        require(preset.size == BITMAP_COUNT) { "Preset must have $BITMAP_COUNT bitmaps" }
        preset.copyInto(invocationBitmaps)
        recalculate()
        for (invocation in ToaInvocation.ALL) {
            if (invocation.eventOnly && isActive(invocation)) unflag(invocation)
        }
    }

    fun clear() {
        invocationBitmaps.fill(0)
        raidLevel = 0
        activeInvocations = 0
    }

    fun copy(): ToaPartySettings {
        return ToaPartySettings(
            raidLevel = raidLevel,
            activeInvocations = activeInvocations,
            kcRequirement = kcRequirement,
            invocationBitmaps = invocationBitmaps.copyOf(),
        )
    }

    val mode: String
        get() = when {
            raidLevel >= 300 -> "Expert"
            raidLevel >= 150 -> "Normal"
            else -> "Entry"
        }

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
        private const val BITMAP_COUNT = 3
        private const val BITS_PER_SLOT = 31

        private fun bitmapSlot(index: Int): Int = when {
            index > 61 -> 2
            index > 30 -> 1
            else -> 0
        }

        private fun bitMask(index: Int): Int = 1 shl (index % BITS_PER_SLOT)
    }
}
