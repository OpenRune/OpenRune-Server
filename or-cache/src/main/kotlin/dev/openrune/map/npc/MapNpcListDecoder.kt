package dev.openrune.map.npc

import dev.openrune.map.util.InlineByteBuf
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntLists

public object MapNpcListDecoder {
    public fun decode(buf: InlineByteBuf): MapNpcListDefinition {
        var cursor = buf.newCursor()

        cursor = buf.readShort(cursor)
        val count = cursor.value
        if (count == 0) {
            return MapNpcListDefinition(IntLists.EMPTY_LIST)
        }

        val npcs = IntArrayList(count)
        repeat(count) {
            cursor = buf.readInt(cursor)
            npcs.add(cursor.value)
        }
        return MapNpcListDefinition(npcs)
    }
}
