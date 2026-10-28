import com.gtnewhorizons.gtnhgradle.modules.ToolchainModule
import com.gtnewhorizons.retrofuturagradle.mcp.ReobfuscatedJar
import org.gradle.language.jvm.tasks.ProcessResources

data class ModuleSpec(
    val classifier: String,
    val packagePaths: List<String>,
    val mcmodInfo: String,
    val excludePaths: List<String> = emptyList(),
    val reobfClasspath: List<String> = emptyList(),
    val assetsPath: String? = null,
)

val extraModules = listOf(
    ModuleSpec(
        "core", listOf("mekanism", "com", "cofh"), "mcmod-core.info",
        excludePaths = listOf("mekanism/generators", "mekanism/tools", "mekanism/gametest"),
        assetsPath = "assets/mekanism"
    ),
    ModuleSpec(
        "generators", listOf("mekanism/generators"), "mcmod-generators.info",
        reobfClasspath = listOf("core"),
        assetsPath = null
    ),
    ModuleSpec(
        "tools", listOf("mekanism/tools"), "mcmod-tools.info",
        reobfClasspath = listOf("core"),
        assetsPath = null
    ),
)



val mainSourceSet = the<JavaPluginExtension>().sourceSets.getByName("main")

val mcmodInfoProperties: MapProperty<String, String> =
    (extensions.getByName("gtnhGradle") as ExtensionAware)
        .extensions.getByType(ToolchainModule::class.java)
        .mcmodInfoProperties

val expandTokens: Provider<Map<String, String>> = mcmodInfoProperties

val allMcmodInfoFiles = extraModules.map { it.mcmodInfo } + "mcmod.info"

// exclude all per-module resources from the shared copy so they don't leak into other modules
val allExcludedResources = allMcmodInfoFiles +
    extraModules.mapNotNull { it.assetsPath }.map { "$it/**" }

val devJarTasks = mutableMapOf<String, TaskProvider<Jar>>()
for (mod in extraModules) {
    val capName = mod.classifier.replaceFirstChar { it.uppercaseChar() }

    val processResTask = tasks.register("process${capName}Resources", ProcessResources::class) {
        from(mainSourceSet.resources) { exclude(allExcludedResources) }
        from(mainSourceSet.resources) {
            include(mod.mcmodInfo)
            rename { "mcmod.info" }
            expand(expandTokens.get())
        }
        if (mod.assetsPath != null) {
            from(mainSourceSet.resources) { include("${mod.assetsPath}/**") }
        }
        into(layout.buildDirectory.dir("resources-${mod.classifier}-processed"))
    }

    devJarTasks[mod.classifier] = tasks.register("${mod.classifier}Jar", Jar::class) {
        dependsOn(processResTask)
        from(mainSourceSet.output) {
            mod.packagePaths.forEach { include("$it/**") }
            exclude(mod.excludePaths.map { "$it/**" })
        }
        from(processResTask.map { it.destinationDir })
        archiveClassifier.set("${mod.classifier}-dev")
    }
}

for (mod in extraModules) {
    val capName = mod.classifier.replaceFirstChar { it.uppercaseChar() }

    val reobfJarTask = tasks.named("reobf${capName}Jar", ReobfuscatedJar::class) {
        archiveClassifier.set(mod.classifier)
        for (depClassifier in mod.reobfClasspath) {
            // use the compiled classes instead of the dev jar so we get an obfuscated version again
            getReferenceClasspath().from(mainSourceSet.output.classesDirs)
        }
    }

    val sourcesJarTask = tasks.register("${mod.classifier}SourcesJar", Jar::class) {
        from(mainSourceSet.allSource) {
            mod.packagePaths.forEach { include("$it/**") }
            exclude(mod.excludePaths.map { "$it/**" })
        }
        archiveClassifier.set("${mod.classifier}-sources")
    }

    tasks.named("assemble") { dependsOn(devJarTasks[mod.classifier]!!, reobfJarTask, sourcesJarTask) }
}

// the gametest classes are excluded from every release jar, so dev runs get them from their own jar
val gametestJar = tasks.register("gametestJar", Jar::class) {
    from(mainSourceSet.output) { include("mekanism/gametest/**") }
    archiveClassifier.set("gametest-dev")
}
dependencies {
    add("runtimeOnlyNonPublishable", files(gametestJar))
}

// change the default jar to have an -all suffix
tasks.matching { it.name == "sourcesJar" }.configureEach { (this as Jar).exclude("mekanism/gametest/**") }
tasks.named("jar", Jar::class) {
    exclude("mekanism/gametest/**")
    archiveClassifier.set("all-dev")
}
tasks.named("reobfJar", ReobfuscatedJar::class) {
    archiveClassifier.set("all")
}
