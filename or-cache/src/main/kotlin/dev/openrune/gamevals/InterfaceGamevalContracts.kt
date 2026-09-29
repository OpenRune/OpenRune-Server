package dev.openrune.gamevals

import java.io.File

/**
 * Build-time contract between packed `.if3` interface sources and generated component gamevals.
 *
 * A cache build is not considered valid when a component declared in a pack cannot be resolved
 * through `component.<interface>:<component>` at runtime. This turns a late server-startup crash
 * into an immediate cache-build failure with the missing keys listed explicitly.
 */
internal object InterfaceGamevalContracts {
    private enum class Section {
        Interface,
        Component,
        Other,
    }

    private val nameLine = Regex("""^name\s*=\s*"([^"]+)"\s*$""")

    fun expectedComponents(contentRoot: File): Set<String> {
        if (!contentRoot.isDirectory) return emptySet()

        val expected = linkedSetOf<String>()
        contentRoot
            .walkTopDown()
            .filter { it.isFile && it.extension.equals("if3", ignoreCase = true) }
            .forEach { file ->
                var section = Section.Other
                var interfaceName: String? = null

                file.forEachLine { rawLine ->
                    val line = rawLine.substringBefore('#').trim()
                    when {
                        line == "[[interface]]" -> section = Section.Interface
                        line == "[[component]]" -> section = Section.Component
                        line.startsWith("[[") && line.endsWith("]]") -> section = Section.Other
                        else -> {
                            val name =
                                nameLine.matchEntire(line)?.groupValues?.get(1)
                                    ?: return@forEachLine
                            when (section) {
                                Section.Interface -> {
                                    interfaceName =
                                        name
                                            .takeIf { it.startsWith("interface.") }
                                            ?.removePrefix("interface.")
                                            ?.takeIf { it.isNotBlank() }
                                }

                                Section.Component -> {
                                    val currentInterface = interfaceName ?: return@forEachLine
                                    expected += "$currentInterface:$name"
                                }

                                Section.Other -> Unit
                            }
                        }
                    }
                }
            }

        return expected
    }

    fun verify(contentRoot: File, generatedComponents: List<String>) {
        val expected = expectedComponents(contentRoot)
        if (expected.isEmpty()) return

        val generatedKeys =
            generatedComponents
                .asSequence()
                .map { it.substringBefore('=').trim() }
                .filter { it.isNotEmpty() }
                .toSet()

        val missing = expected.filterNot(generatedKeys::contains)
        check(missing.isEmpty()) {
            buildString {
                appendLine("Packed interface component gamevals are incomplete.")
                appendLine(
                    "The following source components would be unavailable through component.* at runtime:",
                )
                missing.take(50).forEach { appendLine("  - component.$it") }
                if (missing.size > 50) {
                    appendLine("  - ... and ${missing.size - 50} more")
                }
                append(
                    "Fix the interface packing/id mapping before starting the server; " +
                        "continuing would cause a runtime Missing mapping failure.",
                )
            }
        }
    }
}
