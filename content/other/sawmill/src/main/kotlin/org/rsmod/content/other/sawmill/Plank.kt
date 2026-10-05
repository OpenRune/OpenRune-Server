package org.rsmod.content.other.sawmill

enum class Plank(val logs: String, val plank: String, val price: Int) {
    Wood("obj.logs", "obj.woodplank", 100),
    Oak("obj.oak_logs", "obj.plank_oak", 250),
    Teak("obj.teak_logs", "obj.plank_teak", 500),
    Mahogany("obj.mahogany_logs", "obj.plank_mahogany", 1500),
    Camphor("obj.camphor_logs", "obj.plank_camphor", 2500),
    Ironwood("obj.ironwood_logs", "obj.plank_ironwood", 5000),
    Rosewood("obj.rosewood_logs", "obj.plank_rosewood", 7500);

    companion object {
        fun forPlank(plank: String): Plank = entries.first { it.plank == plank }
    }
}
