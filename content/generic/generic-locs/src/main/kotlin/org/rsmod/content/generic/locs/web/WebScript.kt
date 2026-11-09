package org.rsmod.content.generic.locs.web

import jakarta.inject.Inject
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class WebScript @Inject constructor(private val cutter: WebCutter) : PluginScript() {
    override fun ScriptContext.startup() {
        for (web in WEBS) {
            onOpLoc1(web.base) { cutter.cut(this, it.loc, web.slashed) }
            onOpLocU(web.base) { cutter.useItem(this, it.loc, it.objType, web.slashed) }
            if (web.passThrough) {
                onOpLoc1(web.slashed) { cutter.passThrough(this, it.loc) }
            }
        }
    }

    private data class WebDef(
        val base: String,
        val slashed: String,
        val passThrough: Boolean = false,
    )

    private companion object {
        private val WEBS =
            listOf(
                WebDef("loc.webwall", "loc.sliced_web"),
                WebDef("loc.bigweb_slashable", "loc.bigweb_slashed"),
                WebDef("loc.bigweb_slashable_shape1", "loc.bigweb_slashed_shape1"),
                WebDef("loc.mm2_web", "loc.mm2_sliced_web", passThrough = true),
                WebDef("loc.sarachnis_web_peek", "loc.sarachnis_web_peek_slashed", passThrough = true),
            )
    }
}
