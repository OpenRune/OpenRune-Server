@file:Suppress("SpellCheckingInspection", "unused")

package org.rsmod.api.config.refs

import org.rsmod.api.type.refs.jingle.JingleReferences

typealias jingles = BaseJingles

object BaseJingles : JingleReferences() {
    val death_jingle = jingle("death_jingle")
    val death_jingle_2 = jingle("death_jingle_2")
    val emote_air_guitar = jingle("emote_air_guitar")
}
