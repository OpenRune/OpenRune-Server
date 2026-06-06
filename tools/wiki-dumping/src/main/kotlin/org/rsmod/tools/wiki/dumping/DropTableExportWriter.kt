package org.rsmod.tools.wiki.dumping

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText

/** Writes wiki dump output: TOML for simple tables, Kotlin only for tables that need it. */
object DropTableExportWriter {
    data class SplitSpecs(
        val toml: List<GeneratedDropTableSpec>,
        val kotlin: List<GeneratedDropTableSpec>,
    )

    fun splitSpecs(
        specs: List<GeneratedDropTableSpec>,
        tomlExport: TomlExportConfig?,
    ): SplitSpecs {
        if (tomlExport == null) {
            return SplitSpecs(toml = emptyList(), kotlin = specs)
        }
        val toml = specs.filter { DropTableTomlExporter.isSimpleForTomlExport(it) }
        val kotlin = specs.filterNot { DropTableTomlExporter.isSimpleForTomlExport(it) }
        return SplitSpecs(toml = toml, kotlin = kotlin)
    }

    data class PageExportResult(
        val kotlinFile: Path?,
        val tomlFiles: Set<Path>,
    )

    fun exportPage(
        wikiPage: String,
        specs: List<GeneratedDropTableSpec>,
        kotlinRoot: Path,
        tomlExport: TomlExportConfig?,
        kotlinOutput: Boolean,
        log: DropDumpLog,
        onKotlinWritten: ((Path) -> Unit)? = null,
    ): PageExportResult {
        val split = splitSpecs(specs, tomlExport)
        for (spec in split.kotlin) {
            val blockers = DropTableTomlExporter.tomlExportBlockers(spec)
            if (blockers.isNotEmpty()) {
                log.verbose("${spec.tableIdentifier} → Kotlin (${blockers.joinToString("; ")})")
            }
        }
        val tomlFiles = exportToml(split.toml, tomlExport, log)
        val kotlinFile =
            exportKotlin(
                wikiPage = wikiPage,
                specs = split.kotlin,
                kotlinRoot = kotlinRoot,
                kotlinOutput = kotlinOutput,
                log = log,
                onWritten = onKotlinWritten,
            )
        removeKotlinIfFullyMigrated(
            wikiPage = wikiPage,
            kotlinRoot = kotlinRoot,
            tomlExport = tomlExport,
            split = split,
            log = log,
        )
        return PageExportResult(kotlinFile = kotlinFile, tomlFiles = tomlFiles)
    }

    private fun exportToml(
        specs: List<GeneratedDropTableSpec>,
        tomlExport: TomlExportConfig?,
        log: DropDumpLog,
    ): Set<Path> {
        if (tomlExport == null || specs.isEmpty()) {
            return emptySet()
        }
        val written = mutableSetOf<Path>()
        for (spec in specs) {
            val table = DropTableTomlExporter.exportTable(spec)
            DropTableTomlExporter.writeTable(tomlExport.tomlRoot, spec.tableVarName, table)
            val tomlPath =
                DropTableTomlOutputLayout.resolveTableFile(tomlExport.tomlRoot, spec.tableVarName)
            written.add(tomlPath)
            log.info("wrote toml ${tomlExport.tomlRoot.relativize(tomlPath)} → ${tomlPath.toAbsolutePath()}")
        }
        return written
    }

    private fun exportKotlin(
        wikiPage: String,
        specs: List<GeneratedDropTableSpec>,
        kotlinRoot: Path,
        kotlinOutput: Boolean,
        log: DropDumpLog,
        onWritten: ((Path) -> Unit)?,
    ): Path? {
        if (!kotlinOutput || specs.isEmpty()) {
            return null
        }
        val output = DropTableOutputLayout.resolveMonsterOutputFile(kotlinRoot, wikiPage)
        output.parent.createDirectories()
        output.writeText(
            DropTableCodeGenerator.generateFile(
                specs,
                packageName = DropTableOutputLayout.MONSTERS_PACKAGE,
            ),
        )
        onWritten?.invoke(output)
        log.info("wrote ${output.fileName} → ${output.toAbsolutePath()}")
        return output
    }

    private fun removeKotlinIfFullyMigrated(
        wikiPage: String,
        kotlinRoot: Path,
        tomlExport: TomlExportConfig?,
        split: SplitSpecs,
        log: DropDumpLog,
    ) {
        if (tomlExport == null || split.toml.isEmpty() || split.kotlin.isNotEmpty()) {
            return
        }
        val kotlinFile = DropTableOutputLayout.resolveMonsterOutputFile(kotlinRoot, wikiPage)
        if (kotlinFile.deleteIfExists()) {
            log.info("removed ${kotlinFile.fileName} (migrated to TOML)")
        } else {
            log.verbose("$wikiPage → TOML only (${split.toml.size} table(s))")
        }
    }
}
