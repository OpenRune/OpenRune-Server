# Files

- [External and hot-loaded plugins](external-plugins.md) - How ExternalPluginLoader discovers PluginModule and PluginScript classes from jars or class directories under plugins/, the plugin.properties manifest, boot-time merging into the main injector, hot load/reload/unload with child injectors and classloader-based handler removal, enable/disable state, and the example-plugin template.
- [Writing content plugins](plugin-scripts.md) - How gameplay is added to OpenRune as PluginScript and PluginModule classes that are discovered by a shared classpath scan and constructed by Guice, the handler DSL in api/script and api/script-advanced, module layout with -pack siblings, and where to store state.
