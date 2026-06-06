package org.rsmod.tools.wiki.dumping

import java.nio.file.Path

/** Output paths for wiki-generated drop table JSON under `tables/`. */
object DropTableJsonOutputLayout {
    const val TABLES_DIR = "tables"
    const val MANIFEST_FILE = "manifest.json"

    fun resolveTableFile(jsonRoot: Path, tableVarName: String): Path =
        jsonRoot.resolve(TABLES_DIR).resolve(jsonFileName(tableVarName))

    fun resolveManifestFile(jsonRoot: Path): Path = jsonRoot.resolve(MANIFEST_FILE)

    fun jsonFileName(tableVarName: String): String =
        "${tableVarName.replaceFirstChar { it.uppercase() }}.json"

    fun relativeTablePath(jsonRoot: Path, tableVarName: String): String =
        jsonRoot.relativize(resolveTableFile(jsonRoot, tableVarName)).toString().replace('\\', '/')
}
